package webby.core.server

import java.lang.management.ManagementFactory
import java.net.InetSocketAddress
import java.nio.file.{Files, Path, Paths}
import java.util.TimeZone
import java.util.concurrent._
import javax.management.{MBeanServer, ObjectName}

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.{NioEventLoop, NioEventLoopGroup}
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{Channel, ChannelFuture, ChannelInitializer, ChannelOption}
import io.netty.handler.codec.http.{HttpObjectAggregator, HttpRequestDecoder, HttpResponseEncoder}
import io.netty.util.concurrent
import io.netty.util.concurrent.DefaultThreadFactory
import org.apache.commons.lang3.StringUtils
import webby.api._
import webby.commons.concurrent.{ThreadUtils, Threads}
import webby.commons.system.mbean.AnnotatedStandardMBean
import webby.core._
import webby.core.server.netty._
import webby.core.system.{ApplicationProvider, ReloadableAppProvider, StaticAppProvider}

import scala.annotation.tailrec
import scala.util.control.NonFatal

/**
  * creates a Server implementation based Netty
  */
abstract class NettyServer(appProvider: ApplicationProvider, port: Int, address: String)
  extends Server with ServerWithStop {

  def applicationProvider = appProvider

  class ServerChannelInitializer extends ChannelInitializer[SocketChannel] {

    def initChannel(ch: SocketChannel) {
      val pipeline = ch.pipeline()
      pipeline.addLast("decoder", new HttpRequestDecoder())
      pipeline.addLast("aggregator", new HttpObjectAggregator(1024 * 1024))
      pipeline.addLast("encoder", new HttpResponseEncoder())
      pipeline.addLast("handler", defaultUpStreamHandler)
    }
  }
  // Initialize MBean
  initMbean()

  def defaultUpStreamHandler: UpstreamHandler
  def workerThreadNum: Int

  // The HTTP server channel
  val bossGroup = new NioEventLoopGroup(0, new DefaultThreadFactory("netty-boss"))
  val workerGroup = new NioEventLoopGroup(workerThreadNum, new DefaultThreadFactory("netty-worker"))

  val httpChannel: Channel =
    try {
      val bootstrap = new ServerBootstrap()
        .group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .childHandler(new ServerChannelInitializer)
        .option(ChannelOption.SO_BACKLOG, 1024.asInstanceOf[Integer]) // Длина очереди на соединение с сервером, see ServerSocket.bind()
        .childOption(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.TRUE)
      val f: ChannelFuture = bootstrap.bind(new InetSocketAddress(address, port)).sync()
      f.channel()
    } catch {
      case e: Throwable =>
        bossGroup.shutdownGracefully(0, 10, TimeUnit.SECONDS)
        workerGroup.shutdownGracefully(0, 60, TimeUnit.SECONDS)
        throw e
    }

  override val mainAddress: InetSocketAddress = httpChannel.localAddress().asInstanceOf[InetSocketAddress]

  if (!profile.isTest) Logger.webby.warn("--------- Started " + profile.name + " server on " + mainAddress + "---------")

  /**
    * Gracefully stop the server
    */
  override def stop() {
    if (!profile.isTest) Logger.webby.info("Server preparing to shutdown")
    try {
      App.prepareToShutdown()
    } catch {
      case NonFatal(e) => Logger.webby.error("Error while preparing to shutdown the application", e)
    }

    if (!profile.isTest) Logger.webby.info("Shutting down Netty channels")
    // Close all opened sockets
    val bgFuture: concurrent.Future[_] = bossGroup.shutdownGracefully(0, 10, TimeUnit.SECONDS)
    val wgFuture: concurrent.Future[_] = workerGroup.shutdownGracefully(0, 60, TimeUnit.SECONDS)
    httpChannel.closeFuture().sync()
    wgFuture.awaitUninterruptibly()

    Logger.webby.info("All channels closed. Stopping application.")

    try {
      App.stop()
    } catch {
      case NonFatal(e) => Logger.webby.error("Error while stopping the application", e)
    }
    // Deinitialize MBean
    deinitMbean()

    Logger.webby.warn("--------- Server stopped ---------") // Следующая команда super.stop() закрывает логгер, поэтому сообщение стопа сервера здесь.
    try {
      super.stop()
    } catch {
      case NonFatal(e) => Logger.webby.error("Error while stopping akka", e)
    }
  }

  //
  // ------------------------- MBean classes and methods -------------------------
  //
  trait NettyServerMBean {
    def getAddress: String
    def getWorkerThreadNum: Int
    def getPendingTaskNum: Int
    def stopServer(): Unit

    def threadInterrupt(id: Long): String
    def threadStop(id: Long): String
  }

  object NettyServerMBeanImpl extends NettyServerMBean {
    override def getAddress: String = mainAddress.toString
    override def getWorkerThreadNum: Int = workerGroup.executorCount()
    override def getPendingTaskNum: Int = {
      import scala.collection.JavaConversions._
      workerGroup.foldLeft(0)((sum, executor) => sum + executor.asInstanceOf[NioEventLoop].pendingTasks())
    }
    override def stopServer(): Unit = stop()

    override def threadInterrupt(id: Long): String = findThread(id).fold("Thread not found") {t => t.interrupt(); "ok"}
    override def threadStop(id: Long): String = findThread(id).fold("Thread not found") {t => ThreadUtils.stop(t); "ok"}

    def findThread(id: Long): Option[Thread] = {
      @tailrec def findRootGroup(group: ThreadGroup): ThreadGroup = group.getParent match {
        case null => group
        case parent => findRootGroup(parent)
      }
      val rootGroup: ThreadGroup = findRootGroup(Thread.currentThread().getThreadGroup)
      val allActiveThreads = rootGroup.activeCount()
      val allThreads = new Array[Thread](allActiveThreads)
      rootGroup.enumerate(allThreads)
      allThreads.find(_.getId == id)
    }
  }

  private def getMBeanObjectName = new ObjectName("webby:type=NettyServer")

  private def initMbean(): Unit = {
    ManagementFactory.getPlatformMBeanServer.registerMBean(
      new AnnotatedStandardMBean(NettyServerMBeanImpl, classOf[NettyServerMBean]), getMBeanObjectName)
  }

  private def deinitMbean(): Unit = {
    val mBeanServer: MBeanServer = ManagementFactory.getPlatformMBeanServer
    val objectName: ObjectName = getMBeanObjectName
    if (mBeanServer.isRegistered(objectName)) mBeanServer.unregisterMBean(objectName)
  }
}


class ProdNettyServer(appProvider: ApplicationProvider, port: Int, address: String)
  extends NettyServer(appProvider, port, address) {

  override def workerThreadNum = 50
  // Our upStream handler is stateless. Let's use this instance for every new connection
  override val defaultUpStreamHandler: UpstreamHandler = new UpstreamHandler(this)
}


class DevNettyServer(appProvider: ApplicationProvider, port: Int, address: String = "0.0.0.0")
  extends NettyServer(appProvider, port, address) {

  appProvider match {
    case reloadable: ReloadableAppProvider =>
      reloadable.onReloadAfterStartApp += {newApp =>
        Threads.withContextClassLoader(newApp.classloader) {
          upStreamHandler = new UpstreamHandler(this)
        }
      }
    case _ =>
  }

  override def workerThreadNum = 4
  override def defaultUpStreamHandler: UpstreamHandler = upStreamHandler
  protected var upStreamHandler: UpstreamHandler = new UpstreamHandler(this)
}


/**
  * Bootstraps Webby application with a NettyServer backend.
  * Используется для запуска сервера на Production и Jenkins.
  */
object NettyServer {

  def init(): Unit = {
  }

  /**
    * Глобальная инициализация состояния при старте production сервера
    */
  def initProd(): Unit = {
    // Принудительно поставить московское время (иначе, часы будут показывать на час вперёд).
    // Время нужно установить до инициализации логов.
    TimeZone.setDefault(TimeZone.getTimeZone("GMT+0300"))
  }

  /**
    * creates a NettyServer based on the application represented by applicationPath
    * @param appPath path to application
    * @param profile app profile
    */
  def createServer(appPath: Path, profile: Profile): Option[ProdNettyServer] = {
    try {
      // Manage RUNNING_PID file
      val pid = StringUtils.split(java.lang.management.ManagementFactory.getRuntimeMXBean.getName, '@')(0)
      val pidPath = Option(System.getProperty("pidfile.path")).fold(appPath.resolve("RUNNING_PID"))(Paths.get(_))

      // The Logger is not initialized yet, we print the Process ID on STDOUT
      println("App server process ID is " + pid)

      if (pidPath.toString != "/dev/null" && Files.exists(pidPath)) {
        println("This application is already running (Or delete " + pidPath.toAbsolutePath + " file).")
        System.exit(-1)
      }

      val server = new ProdNettyServer(
        new StaticAppProvider(appPath, profile),
        Option(System.getProperty("http.port")).fold(9000)(Integer.parseInt),
        Option(System.getProperty("http.address")).getOrElse("0.0.0.0")
      )

      if (pidPath.toString != "/dev/null") {
        Files.write(pidPath, pid.getBytes)
        Runtime.getRuntime.addShutdownHook(new Thread {
          override def run() {
            Files.deleteIfExists(pidPath)
          }
        })
      }

      Runtime.getRuntime.addShutdownHook(new Thread {
        override def run() {
          server.stop()
        }
      })

      Some(server)
    } catch {
      case NonFatal(e) =>
        println("Oops, cannot start the server.")
        e.printStackTrace()
        None
    }
  }

  /**
    * attempts to create a NettyServer based on either
    * passed in argument or `user.dir` System property or current directory.
    *
    * Точка входа для Production/Jenkins сервера.
    */
  def main(args: Array[String]) {
    var appPath: Path = null
    args match {
      case Array(appPathStr) => appPath = Paths.get(appPathStr)
      case Array() => appPath = Paths.get(".")
      case _ => sys.error("Invalid number of arguments, must be 1.")
    }
    require(Files.exists(appPath) && Files.isDirectory(appPath), "Invalid home directory")
    init()
    initProd()
    val profile: Profile = System.getProperty("profile") match {
      case null => Profile.Prod // default profile
      case profileStr => Profile.fromString(profileStr).getOrElse(sys.error("Invalid profile"))
    }
    createServer(appPath, profile).getOrElse {System.exit(-1); sys.error("")}
  }

  /**
    * provides a NettyServer for the dev environment.
    *
    * Точка входа для Dev сервера, который запущен специальным sbt плагином, передающим [[SBTLink]].
    */
  def mainDev(port: Int, sbtLink: Option[SBTLink]): DevNettyServer = {
    Threads.withContextClassLoader(this.getClass.getClassLoader) {
      try {
        init()
        val appProvider: ApplicationProvider = sbtLink match {
          case Some(link) => new ReloadableAppProvider(link)
          case None => new StaticAppProvider(Paths.get("."), Profile.Dev)
        }
        new DevNettyServer(appProvider, port)
      } catch {
        case e: ExceptionInInitializerError => throw e.getCause
      }
    }
  }
}
