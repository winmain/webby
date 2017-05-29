package webby.mvc.script

import webby.api.{App, Profile}
import webby.commons.system.OverridableObject

/**
  * Простенький держатель ресурса, который работает по-разному для локалки и для всего остального:
  * 1. Для локалки ресурс каждый раз загружается вызовом loader
  * 2. Для всего остального ресурс загружается только один раз
  */
class ResourceHolder(loader: => String) {
  private val _value: String = if (App.isDevOrTest) null else loader
  def get: String = App.profile match {
    case Profile.Prod | Profile.Jenkins | Profile.Console => _value
    case Profile.Dev => loader
    case _ => "unavailable in test mode"
  }
}

/**
  * Класс для работы с минификацией ресурсов.
  * В одном проекте по-умолчанию используется только один instance этого класса, объявленный в одном объекте.
  */
object StdResourceHolder extends OverridableObject {
  class Value extends Base {
    val jsMinifier: ScriptMinifier = StdJsMinifier
    def jsMin(pathFromApp: String): ResourceHolder = new ResourceHolder(jsMinifier.load(pathFromApp))

    val lessMinifier: ScriptMinifier = new StdLessMinifier(App.profile).minifier
    def lessMin(pathFromApp: String): ResourceHolder = new ResourceHolder(lessMinifier.load(pathFromApp))
  }

  override protected def default = new Value
}
