package webby.commons.system

import java.lang.reflect.Constructor

import org.apache.commons.lang3.StringUtils
import org.zeroturnaround.javarebel.{ClassEventListener, ReloaderFactory}

import scala.collection.mutable.ArrayBuffer

/**
  * Методы для удобного использования JRebel.
  *
  * Для работы требуют подключения
  * {{{
  * libraryDependencies += "org.zeroturnaround" % "jr-sdk" % "6.4.6"
  * }}}
  * Эта либа не находится в стандартных репозиториях, поэтому, возможно потребуется прописать
  * дополнительный репозиторий в `plugins.sbt`:
  * {{{
  * resolvers += "zeroturnaround repository" at "https://repos.zeroturnaround.com/nexus/content/repositories/zt-public/"
  * }}}
  */
object JRebelUtils {
  val log = webby.api.Logger(getClass)

  var reloadListeners = new ArrayBuffer[Class[_] => Any]()

  ReloaderFactory.getInstance().addClassReloadListener(new ClassEventListener {
    override def onClassEvent(eventType: Int, klass: Class[_]): Unit = {
      if (eventType == ClassEventListener.EVENT_RELOADED) {
        fireReloadListeners(klass)

        val name: String = klass.getName
        if (name.endsWith("$class")) {
          // Это класс-спутник, который содержит методы трейта.
          // При обновлении этого класса, нам следует вызвать и обновление самого трейта.
          try {
            val cls = Class.forName(StringUtils.removeEnd(name, "$class"))
            fireReloadListeners(cls)
          } catch {case ignore: Exception =>}
        }
      }
    }
    override def priority(): Int = 1
  })

  private def fireReloadListeners(cls: Class[_]): Unit = {
    reloadListeners.foreach(_ (cls))
  }

  /**
    * Небольшой хак, улучшающий перезагрузку объектов JRebel'ом.
    * Теперь поля объекта (val's) тоже перегружаются.
    */
  def reloadObjectFields(): Unit = {
    reloadListeners += {cls =>
      val constructors: Array[Constructor[_]] = cls.getDeclaredConstructors
      if (constructors.length>0) {
        val constructor = constructors(0)
        if (constructor.getParameterCount == 0) {
          constructor.setAccessible(true)
          try {
            constructor.newInstance()
          } catch {
            case e: Exception => log.error("Error creating instance of " + cls.getName, e)
          }
          constructor.setAccessible(false)
        }
      }
    }
  }

  /**
    * Добавить обработчик в случае перезагрузки класса.
    */
  def onReload(handler: Class[_] => Any): Unit = reloadListeners += handler

  /**
    * Подождать пока JRebel обновит классы
    */
  def waitForReload(): Unit = {
    val t0 = System.currentTimeMillis()
    ReloaderFactory.getInstance().waitForReload()
    val time = System.currentTimeMillis() - t0
    if (time > 10) println("[info] JRebel reloaded classes in " + time + " ms")
  }
}
