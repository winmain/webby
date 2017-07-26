package webby.commons.text
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist

/**
  * Методы для работы с html в строках, требующие библиотеку Jsoup для своей работы.
  *
  * Библиотека Jsoup подключается sbt настройкой
  * {{{
  * deps += "org.jsoup" % "jsoup" % "1.6.3"
  * }}}
  */
trait StdStrHtmlJsoup extends StdStrHtml {
  /**
    * Вырезать все html теги, убрав переносы строк. Html entities остаются как есть.
    * Т.е., это подготовка строки, в которой будет храниться html.
    */
  def stripHtmlTags(str: String): String = Jsoup.clean(str, Whitelist.none())

  /**
    * Вырезать все html теги, сохранив переносы строк (которые определяются по тегам <br>).
    * Html entities остаются как есть.
    * Также, тег <li> конвертируется в "<br>• ".
    * Т.е., это подготовка строки, в которой будет храниться html.
    */
  def stripHtmlTagsPreserveLineBreaks(str: String): String = {
    val replacedLi = StringUtils.replace(str, "<li>", "<br>• ") // Добавить точки к <li>, и заменить их на <br>
    StringUtils.replace(
      StringUtils.replace(
        StringUtils.replace(Jsoup.clean(replacedLi, StdStrHtmlJsoup.WhitelistBr),
          "<br />", ""),
        "<br>", ""),
      "\n ", "\n")
  }

  /**
    * Вырезать все html теги, убрав переносы строк, и преобразовать html entities, заменив опасные символы '<', '>'.
    * Применяется, чтобы полностью убрать html из строки (например, сконвертировать html-строку из старого сайта).
    * Внутри вызывается unescapeAndCleanHtmlEntities
    */
  def cleanHtml(str: String): String = if (str == null) null else unescapeAndCleanHtmlEntities(stripHtmlTags(str))

  /**
    * Вырезать все html теги, сохранив переносы строк (которые определяются по тегам <br>),
    * и преобразовать html entities, заменив опасные символы '<', '>'
    * Внутри вызывается unescapeAndCleanHtmlEntities
    */
  def cleanHtmlPreserveLineBreaks(str: String): String = if (str == null) null else unescapeAndCleanHtmlEntities(stripHtmlTagsPreserveLineBreaks(str))
}

object StdStrHtmlJsoup extends StdStrHtmlJsoup {
  val WhitelistBr = Whitelist.none().addTags("br")
}


trait StdStrHtmlJsoupWrapper extends StdStrHtmlWrapper {

  /** Вырезать все html теги, сохранив переносы строк (которые определяются по тегам <br>). Html entities остаются как есть. */
  def stripHtmlTagsPreserveLineBreaks: String = StdStrHtmlJsoup.stripHtmlTagsPreserveLineBreaks(s)

  /**
    * Вырезать все html теги, убрав переносы строк, и преобразовать html entities, заменив опасные символы '<', '>'.
    * Применяется, чтобы полностью убрать html из строки (например, сконвертировать html-строку из старого сайта).
    * Внутри вызывается unescapeAndCleanHtmlEntities
    */
  def cleanHtml: String = StdStrHtmlJsoup.cleanHtml(s)

  /**
    * Вырезать все html теги, сохранив переносы строк (которые определяются по тегам <br>),
    * и преобразовать html entities, заменив опасные символы '<', '>'
    * Внутри вызывается unescapeAndCleanHtmlEntities
    */
  def cleanHtmlPreserveLineBreaks: String = StdStrHtmlJsoup.cleanHtmlPreserveLineBreaks(s)
}
