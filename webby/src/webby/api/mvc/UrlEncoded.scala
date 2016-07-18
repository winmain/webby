package webby.api.mvc

import java.{util => ju}

import io.netty.handler.codec.http.QueryStringDecoder

/**
  * Доступ к распарсенным данным application/x-www-form-urlencoded, либо к query string.
  */
trait UrlEncoded {
  def get(key: String): Option[String]
  def get(key: String, default: String): String
  def getAll(key: String): Iterable[String]
  def contains(key: String): Boolean
  def toMap: Map[String, String]
  def asJavaMultiMap: ju.Map[String, ju.List[String]]

  /**
    * Оригинальная строка, из которой были разобраны параметры.
    * Например: "a=123&b=foo". Если параметров не было, то здесь будет пустая строка "".
    */
  def original: String

  def isEmpty: Boolean
}

object UrlEncoded {
  def fromQuery(query: String): UrlEncoded = new UrlEncodedFromDecoder(new QueryStringDecoder(query, false), query)
}

object EmptyUrlEncoded extends UrlEncoded {
  override def get(key: String): Option[String] = None
  override def get(key: String, default: String): String = default
  override def getAll(key: String): Iterable[String] = Seq.empty
  override def contains(key: String): Boolean = false
  override def toMap: Map[String, String] = Map.empty
  override def asJavaMultiMap: ju.Map[String, ju.List[String]] = ju.Collections.emptyMap()
  override def original: String = ""
  override def isEmpty: Boolean = true
}

/**
  * Легковесная обёртка над QueryStringDecoder
  */
class UrlEncodedFromDecoder(decoder: QueryStringDecoder, val original: String) extends UrlEncoded {

  import scala.collection.JavaConverters._

  override def get(key: String): Option[String] = try {
    decoder.parameters().get(key) match {
      case null => None
      case e if e.size() > 0 => Some(e.get(0))
      case _ => None
    }
  } catch {
    case _: IllegalArgumentException => None
  }

  override def get(key: String, default: String): String = try {
    decoder.parameters().get(key) match {
      case null => default
      case e if e.size() > 0 => e.get(0)
      case _ => default
    }
  } catch {
    case _: IllegalArgumentException => default
  }

  override def getAll(key: String): Iterable[String] =
    try {decoder.parameters().get(key).asScala}
    catch {case _: IllegalArgumentException => Nil}

  override def contains(key: String): Boolean = decoder.parameters().containsKey(key)

  override lazy val toMap: Map[String, String] = {
    val b = Map.newBuilder[String, String]
    for (entry <- decoder.parameters().entrySet().asScala) b += ((entry.getKey, entry.getValue.get(0)))
    b.result()
  }

  override def asJavaMultiMap: ju.Map[String, ju.List[String]] = decoder.parameters()

  override def isEmpty: Boolean = original.isEmpty
}
