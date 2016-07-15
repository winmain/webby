package webby.commons.html
import javax.annotation.Nullable

import webby.commons.text.SB

import scala.collection.mutable.ArrayBuffer

//
// Benchmark. По тесту, эти шаблоны в 2-3 раза быстрее, чем стандарные play шаблоны.
//
class HtmlBuffer(capacity: Int = 1024) extends SB(capacity) {
  /** Текущий открытый тег. Пока этот тег открыт, в него можно добавлять атрибуты. */
  @Nullable protected var curTag: BaseTag = null

  /**
    * Специальный флаг, сигнализирующий о том что следующие записи в буфер будут атрибутами открытого тега.
    * Если же этот флаг не стоит, то следующие записи в буфер будут вне тело тега.
    * Этот флаг нужен для автоматического закрытия тега перед записью в буфер.
    */
  protected var writingAttr: Boolean = false

  /** Иерархия вложенных тегов. */
  protected var tags: ArrayBuffer[BaseTag] = new ArrayBuffer[BaseTag](8)

  /** Список классов (атрибут class), которые следует добавить в декларацию тега. */
  protected var classes: ArrayBuffer[String] = new ArrayBuffer[String](8)

  override protected def beforeAppend(): Unit = {
    if (writingAttr) {
      if (curTag eq null) sys.error("Cannot write attr: no open tag")
    } else if (curTag ne null) {
      closeTag(curTag) // Автозакрытие тега
    }
  }

  override protected def beforeResult(): Unit = {
    if (curTag ne null) closeTag0(curTag)
    while (tags.nonEmpty) closeTag(tags.last)
  }

  /** Директива, показывающая что мы сейчас будем писать атрибуты тега. */
  private[html] def beginAttr(tag: BaseTag): this.type = {
    if (writingAttr) sys.error("Already writing attributes")
    if (curTag ne tag) sys.error("Cannot add attr when not in tag declaration")
    writingAttr = true
    this
  }
  /** Директива, закрывающая запись в атрибуты тега. */
  private[html] def endAttr(): Unit = writingAttr = false

  /** Открытие нового тега */
  private[html] def newTag(tag: BaseTag) {
    if (curTag ne null) closeTag0(curTag)
    curTag = tag
    sb append '<' append tag.tag
  }

  /** Добавление класса в список классов для заданного тега */
  private[html] def addClass(tag: BaseTag, cls: String) {
    if (curTag ne tag) sys.error("Cannot addClass to tag " + tag + ": not in tag")
    classes += cls
  }

  /** Записать все накопленные классы в открытый тег, и очистить накопленный список классов */
  private def appendAndCleanClasses() {
    val length: Int = classes.length
    if (length > 0) {
      sb append " class=\"" append classes(0)
      var i = 1
      while (i < length) {
        sb append ' ' append classes(i)
        i += 1
      }
      sb append '"'
    }
    classes.clear()
  }

  /** Войти в тег. Декларация тега завершается здесь и начинается тело тега. */
  private[html] def goInTag(tag: BaseTag) {
    if (curTag ne tag) sys.error("Not in tag " + tag)
    appendAndCleanClasses()
    sb append '>'
    tags += curTag
    curTag = null
  }

  /** Закрыть открытый тег, либо завершить тело тега. */
  private[html] def closeTag(tag: BaseTag) {
    if (curTag eq tag) {
      closeTag0(tag)
      curTag = null
    } else {
      if (curTag ne null) {
        closeTag0(curTag)
        curTag = null
      }
      if (tags.isEmpty) sys.error("Cannot close tag " + tag + ". No tags are open.")
      if (tags.last != tag) sys.error("Trying to close tag " + tag + ", but current tag is " + tags.last)
      closeTag0(tag)
      tags.reduceToSize(tags.length - 1)
    }
  }

  /** Реальное закрытие тега, без проверок. */
  private def closeTag0(tag: BaseTag): Unit = {
    writingAttr = true
    if (curTag eq tag) {
      appendAndCleanClasses()
      if (tag.shortClose) sb append "/>"
      else sb append "></" append tag.tag append '>'
    } else {
      sb append "</" append tag.tag append '>'
    }
    writingAttr = false
  }

  /** Вернуть html результат в виде строки. */
  def result: String = toString
}
