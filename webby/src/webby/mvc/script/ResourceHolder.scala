package webby.mvc.script

import webby.api.{App, Profile}

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

object StdResourceHolder {
  def jsMin(pathFromApp: String): ResourceHolder = new ResourceHolder(StdJsMinifier.load(pathFromApp))

  val lessMinifier = new StdLessMinifier(App.profile)
  def lessMin(pathFromApp: String): ResourceHolder = new ResourceHolder(lessMinifier.minifier.load(pathFromApp))
}
