package webby.api

import scala.collection.mutable

/**
  * Профили приложения. Описывают режим работы.
  */
object Profile {
  private[api] val _values = new mutable.ArrayBuffer[Profile](5)
  def values: Seq[Profile] = _values

  def fromString(name: String): Option[Profile] = values.find(_.name == name)

  // ------------------------------- Default profiles -------------------------------

  /** Запуск сервера на локале через sbt (development режим) */
  val Dev = new Profile("dev", isDev = true)

  /** Запуск сервера на dev сервере через jenkins */
  val Jenkins = new Profile("jenkins", isJenkins = true)

  /** Запуск действий из консоли на боевом сервере (например, системные cron задания) */
  val Console = new Profile("console", isConsole = true)

  /** Боевой production */
  val Prod = new Profile("prod", isProd = true)

  /** Профайл для запуска тестов */
  val Test = new Profile("test", isTest = true)

  // Если понадобятся дополнительные профили в приложении, можно дописать сюда код,
  // который будет подгружать объект с профилями и выполнять его.
}

class Profile(val name: String,
              val isDev: Boolean = false,
              val isJenkins: Boolean = false,
              val isConsole: Boolean = false,
              val isProd: Boolean = false,
              val isTest: Boolean = false) {
  Profile._values += this

  def in(profiles: Profile*): Boolean = profiles.contains(this)

  def isDevOrTest: Boolean = isDev || isTest
  def isDevOrJenkins: Boolean = isDev || isJenkins
  def isDevOrJenkinsOrTest: Boolean = isDev || isJenkins || isTest
  def isDevOrConsole: Boolean = isDev || isConsole
  def isJenkinsOrProd: Boolean = isJenkins || isProd
  def isProdOrConsole: Boolean = isProd || isConsole

  override def toString: String = name
}
