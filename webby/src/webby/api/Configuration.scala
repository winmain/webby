package webby.api

import java.nio.file.Path
import java.util.concurrent.TimeUnit

import com.google.common.io.ByteStreams
import com.typesafe.config._

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

/**
  * This object provides a set of operations to create `Configuration` values.
  *
  * For example, to load a `Configuration` in a running application:
  * {{{
  * val config = Configuration.load()
  * val foo = config.getString("foo").getOrElse("boo")
  * }}}
  *
  * The underlying implementation is provided by https://github.com/typesafehub/config.
  */
object Configuration {

  def load(appPath: Path): Configuration = {
    try {
      Configuration(ConfigFactory.load(
        ConfigParseOptions.defaults()
          .setAllowMissing(false)))
    } catch {
      case e: ConfigException => throw configError(e.origin, e.getMessage, Some(e))
      case e: Throwable => throw e
    }
  }

  /**
    * Returns an empty Configuration object.
    */
  def empty = Configuration(ConfigFactory.empty())

  /**
    * Create a ConfigFactory object from the data passed as a Map.
    */
  def from(data: Map[String, AnyRef]) = {
    Configuration(ConfigFactory.parseMap(data.asJava))
  }

  private def configError(origin: ConfigOrigin, message: String, e: Option[Throwable] = None): WebbyException = {
    new WebbyException.ExceptionSource("Configuration error", message, e.orNull) {
      def line = Option(origin.lineNumber: java.lang.Integer).orNull
      def position = null
      def input: String = Option(origin.url).fold(null: String)(url => new String(ByteStreams.toByteArray(url.openStream())))
      def sourceName = Option(origin.filename).orNull
      override def toString = "Configuration error: " + getMessage
    }
  }

}

/**
  * A full configuration set.
  *
  * The underlying implementation is provided by https://github.com/typesafehub/config.
  *
  * @param underlying the underlying Config implementation
  */
case class Configuration(underlying: Config) {

  /**
    * Merge 2 configurations.
    */
  def ++(other: Configuration): Configuration = {
    Configuration(other.underlying.withFallback(underlying))
  }

  /**
    * Read a value from the underlying implementation,
    * catching Errors and wrapping it in an Option value.
    */
  private def readValueOpt[T](path: String, v: => T): Option[T] = {
    try {
      Option(v)
    } catch {
      case e: ConfigException.Missing => None
      case NonFatal(e) => throw reportError(path, e.getMessage, Some(e))
    }
  }

  private def readValueDef[T](path: String, value: => T, default: T): T = {
    try {
      value match {
        case null => default
        case v => v
      }
    } catch {
      case e: ConfigException.Missing => default
      case NonFatal(e) => throw reportError(path, e.getMessage, Some(e))
    }
  }

  /**
    * Retrieves a configuration value as a `String`.
    *
    * This method supports an optional set of valid values:
    * {{{
    * val config = Configuration.load()
    * val mode = config.getString("engine.mode", Some(Set("dev","prod")))
    * }}}
    *
    * A configuration error will be thrown if the configuration value does not match any of the required values.
    *
    * @param path        the configuration key, relative to configuration root key
    * @param validValues valid values for this configuration
    * @return a configuration value
    */
  def getString(path: String, validValues: Option[Set[String]] = None): Option[String] = readValueOpt(path, underlying.getString(path)).map {value =>
    validValues match {
      case Some(values) if values.contains(value) => value
      case Some(values) if values.isEmpty => value
      case Some(values) => throw reportError(path, "Incorrect value, one of " + values.reduceLeft(_ + ", " + _) + " was expected.")
      case None => value
    }
  }

  def getString(path: String, default: String): String = readValueDef(path, underlying.getString(path), default)


  /**
    * Retrieves a configuration value as an `Int`.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val poolSize = configuration.getInt("engine.pool.size")
    * }}}
    *
    * A configuration error will be thrown if the configuration value is not a valid `Int`.
    *
    * @param path the configuration key, relative to the configuration root key
    * @return a configuration value
    */
  def getInt(path: String): Option[Int] = readValueOpt(path, underlying.getInt(path))

  def getInt(path: String, default: Int): Int = readValueDef(path, underlying.getInt(path), default)

  /**
    * Retrieves a configuration value as a `Boolean`.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val isEnabled = configuration.getString("engine.isEnabled")
    * }}}
    *
    * A configuration error will be thrown if the configuration value is not a valid `Boolean`.
    * Authorized vales are yes/no or true/false.
    *
    * @param path the configuration key, relative to the configuration root key
    * @return a configuration value
    */
  def getBoolean(path: String): Option[Boolean] = readValueOpt(path, underlying.getBoolean(path))

  def getBoolean(path: String, default: Boolean): Boolean = readValueDef(path, underlying.getBoolean(path), default)

  /**
    * Retrieves a configuration value as `Milliseconds`.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val timeout = configuration.getMilliseconds("engine.timeout")
    * }}}
    *
    * The configuration must be provided as:
    *
    * {{{
    * engine.timeout = 1 second
    * }}}
    */
  def getMilliseconds(path: String): Option[Long] = readValueOpt(path, underlying.getDuration(path, TimeUnit.MILLISECONDS))

  /**
    * Retrieves a configuration value as `Nanoseconds`.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val timeout = configuration.getNanoseconds("engine.timeout")
    * }}}
    *
    * The configuration must be provided as:
    *
    * {{{
    * engine.timeout = 1 second
    * }}}
    */
  def getNanoseconds(path: String): Option[Long] = readValueOpt(path, underlying.getDuration(path, TimeUnit.NANOSECONDS))

  /**
    * Retrieves a configuration value as `Bytes`.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val maxSize = configuration.getString("engine.maxSize")
    * }}}
    *
    * The configuration must be provided as:
    *
    * {{{
    * engine.maxSize = 512k
    * }}}
    */
  def getBytes(path: String): Option[Long] = readValueOpt(path, underlying.getBytes(path))

  def getBytes(path: String, default: Long): Long = readValueDef(path, underlying.getBytes(path), default)

  /**
    * Retrieves a sub-configuration, i.e. a configuration instance containing all keys starting with a given prefix.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val engineConfig = configuration.getSub("engine")
    * }}}
    *
    * The root key of this new configuration will be ‘engine’, and you can access any sub-keys relatively.
    *
    * @param path the root prefix for this sub-configuration
    * @return a new configuration
    */
  def getConfig(path: String): Option[Configuration] = readValueOpt(path, underlying.getConfig(path)).map(Configuration(_))

  /**
    * Retrieves a configuration value as a `Double`.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val population = configuration.getDouble("world.population")
    * }}}
    *
    * A configuration error will be thrown if the configuration value is not a valid `Double`.
    *
    * @param path the configuration key, relative to the configuration root key
    * @return a configuration value
    */
  def getDouble(path: String): Option[Double] = readValueOpt(path, underlying.getDouble(path))

  def getDouble(path: String, default: Double): Double = readValueDef(path, underlying.getDouble(path), default)


  /**
    * Retrieves a configuration value as a `Long`.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val duration = configuration.getLong("timeout.duration")
    * }}}
    *
    * A configuration error will be thrown if the configuration value is not a valid `Long`.
    *
    * @param path the configuration key, relative to the configuration root key
    * @return a configuration value
    */
  def getLong(path: String): Option[Long] = readValueOpt(path, underlying.getLong(path))

  def getLong(path: String, default: Long): Long = readValueDef(path, underlying.getLong(path), default)


  /**
    * Retrieves a configuration value as a `Number`.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val counter = configuration.getNumber("foo.counter")
    * }}}
    *
    * A configuration error will be thrown if the configuration value is not a valid `Number`.
    *
    * @param path the configuration key, relative to the configuration root key
    * @return a configuration value
    */
  def getNumber(path: String): Option[Number] = readValueOpt(path, underlying.getNumber(path))

  def getNumber(path: String, default: Number): Number = readValueDef(path, underlying.getNumber(path), default)


  /**
    * Retrieves a configuration value as a List of `Boolean`.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val switches = configuration.getBooleanList("board.switches")
    * }}}
    *
    * The configuration must be provided as:
    *
    * {{{
    * board.switches = [true, true, false]
    * }}}
    *
    * A configuration error will be thrown if the configuration value is not a valid `Boolean`.
    * Authorized vales are yes/no or true/false.
    */
  def getBooleanList(path: String): Option[java.util.List[java.lang.Boolean]] = readValueOpt(path, underlying.getBooleanList(path))

  /**
    * Retrieves a configuration value as a List of `Bytes`.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val maxSizes = configuration.getBytesList("engine.maxSizes")
    * }}}
    *
    * The configuration must be provided as:
    *
    * {{{
    * engine.maxSizes = [512k, 256k, 256k]
    * }}}
    */
  def getBytesList(path: String): Option[java.util.List[java.lang.Long]] = readValueOpt(path, underlying.getBytesList(path))

  /**
    * Retrieves a List of sub-configurations, i.e. a configuration instance for each key that matches the path.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val engineConfigs = configuration.getConfigList("engine")
    * }}}
    *
    * The root key of this new configuration will be "engine", and you can access any sub-keys relatively.
    */
  def getConfigList(path: String): Option[java.util.List[Configuration]] = readValueOpt[java.util.List[_ <: Config]](path, underlying.getConfigList(path)).map {configs => configs.asScala.map(Configuration(_)).asJava}

  /**
    * Retrieves a configuration value as a List of `Double`.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val maxSizes = configuration.getDoubleList("engine.maxSizes")
    * }}}
    *
    * The configuration must be provided as:
    *
    * {{{
    * engine.maxSizes = [5.0, 3.34, 2.6]
    * }}}
    */
  def getDoubleList(path: String): Option[java.util.List[java.lang.Double]] = readValueOpt(path, underlying.getDoubleList(path))

  /**
    * Retrieves a configuration value as a List of `Integer`.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val maxSizes = configuration.getIntList("engine.maxSizes")
    * }}}
    *
    * The configuration must be provided as:
    *
    * {{{
    * engine.maxSizes = [100, 500, 2]
    * }}}
    */
  def getIntList(path: String): Option[java.util.List[java.lang.Integer]] = readValueOpt(path, underlying.getIntList(path))

  /**
    * Gets a list value (with any element type) as a ConfigList, which implements java.util.List<ConfigValue>.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val maxSizes = configuration.getList("engine.maxSizes")
    * }}}
    *
    * The configuration must be provided as:
    *
    * {{{
    * engine.maxSizes = ["foo", "bar"]
    * }}}
    */
  def getList(path: String): Option[ConfigList] = readValueOpt(path, underlying.getList(path))

  /**
    * Retrieves a configuration value as a List of `Long`.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val maxSizes = configuration.getLongList("engine.maxSizes")
    * }}}
    *
    * The configuration must be provided as:
    *
    * {{{
    * engine.maxSizes = [10000000000000, 500, 2000]
    * }}}
    */
  def getLongList(path: String): Option[java.util.List[java.lang.Long]] = readValueOpt(path, underlying.getLongList(path))

  /**
    * Retrieves a configuration value as List of `Milliseconds`.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val timeouts = configuration.getMillisecondsList("engine.timeouts")
    * }}}
    *
    * The configuration must be provided as:
    *
    * {{{
    * engine.timeouts = [1 second, 1 second]
    * }}}
    */
  def getMillisecondsList(path: String): Option[java.util.List[java.lang.Long]] = readValueOpt(path, underlying.getDurationList(path, TimeUnit.MILLISECONDS))

  /**
    * Retrieves a configuration value as List of `Nanoseconds`.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val timeouts = configuration.getNanosecondsList("engine.timeouts")
    * }}}
    *
    * The configuration must be provided as:
    *
    * {{{
    * engine.timeouts = [1 second, 1 second]
    * }}}
    */
  def getNanosecondsList(path: String): Option[java.util.List[java.lang.Long]] = readValueOpt(path, underlying.getDurationList(path, TimeUnit.NANOSECONDS))

  /**
    * Retrieves a configuration value as a List of `Number`.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val maxSizes = configuration.getNumberList("engine.maxSizes")
    * }}}
    *
    * The configuration must be provided as:
    *
    * {{{
    * engine.maxSizes = [50, 500, 5000]
    * }}}
    */
  def getNumberList(path: String): Option[java.util.List[java.lang.Number]] = readValueOpt(path, underlying.getNumberList(path))

  /**
    * Retrieves a configuration value as a List of `ConfigObject`.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val engineProperties = configuration.getObjectList("engine.properties")
    * }}}
    *
    * The configuration must be provided as:
    *
    * {{{
    * engine.properties = [{id: 5, power: 3}, {id: 6, power: 20}]
    * }}}
    */
  def getObjectList(path: String): Option[java.util.List[_ <: ConfigObject]] = readValueOpt[java.util.List[_ <: ConfigObject]](path, underlying.getObjectList(path))

  /**
    * Retrieves a configuration value as a List of `String`.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val names = configuration.getStringList("names")
    * }}}
    *
    * The configuration must be provided as:
    *
    * {{{
    * names = ["Jim", "Bob", "Steve"]
    * }}}
    */
  def getStringList(path: String): Option[java.util.List[java.lang.String]] = readValueOpt(path, underlying.getStringList(path))

  /**
    * Retrieves a ConfigObject for this path, which implements Map<String,ConfigValue>
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val engineProperties = configuration.getObject("engine.properties")
    * }}}
    *
    * The configuration must be provided as:
    *
    * {{{
    * engine.properties = {id: 1, power: 5}
    * }}}
    */
  def getObject(path: String): Option[ConfigObject] = readValueOpt(path, underlying.getObject(path))

  /**
    * Returns available keys.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val keys = configuration.keys
    * }}}
    *
    * @return the set of keys available in this configuration
    */
  def keys: Set[String] = underlying.entrySet.asScala.map(_.getKey).toSet

  /**
    * Returns sub-keys.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val subKeys = configuration.subKeys
    * }}}
    * @return the set of direct sub-keys available in this configuration
    */
  def subKeys: Set[String] = underlying.root().keySet().asScala.toSet

  /**
    * Returns every path as a set of key to value pairs, by recursively iterating through the
    * config objects.
    */
  def entrySet: Set[(String, ConfigValue)] = underlying.entrySet().asScala.map(e => e.getKey -> e.getValue).toSet

  /**
    * Creates a configuration error for a specific configuration key.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * throw configuration.reportError("engine.connectionUrl", "Cannot connect!")
    * }}}
    *
    * @param path    the configuration key, related to this error
    * @param message the error message
    * @param e       the related exception
    * @return a configuration exception
    */
  def reportError(path: String, message: String, e: Option[Throwable] = None): WebbyException = {
    Configuration.configError(if (underlying.hasPath(path)) underlying.getValue(path).origin else underlying.root.origin, message, e)
  }

  /**
    * Creates a configuration error for this configuration.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * throw configuration.globalError("Missing configuration key: [yop.url]")
    * }}}
    *
    * @param message the error message
    * @param e       the related exception
    * @return a configuration exception
    */
  def globalError(message: String, e: Option[Throwable] = None): WebbyException = {
    Configuration.configError(underlying.root.origin, message, e)
  }

}
