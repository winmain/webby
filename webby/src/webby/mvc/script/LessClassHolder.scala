package webby.mvc.script

import webby.commons.io.Resources
import webby.mvc.script.minifier.StdLessMinifierResourceHolder

/**
  * Подключение CSS файла с таким же именем, как и сам контроллер.
  *
  * Для использования:
  * 1. Контроллер должен наследовать LessClassHolder
  * 2. Подключить на страницу: page.addClassLess()
  */
trait LessClassHolder {
  implicit protected def _lessClassHolder: LessClassHolder = this

  val lessResHolder = StdLessMinifierResourceHolder.get.lessMin(Resources.nameForClass(getClass, ".less"))
}
