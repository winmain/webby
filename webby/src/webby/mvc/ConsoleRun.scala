package webby.mvc

import java.lang.reflect.Method

import webby.api.Profile

/**
  * Класс для запуска object методов из консоли в Console режиме (Profile.Console).
  * Usage: rosrabota webby.mvc.ConsoleRun module.Class methodName
  */
object ConsoleRun {

  def main(args: Array[String]) {
    if (args.length < 1 || args.length > 2) {
      println("Usage: rosrabota webby.mvc.ConsoleRun module.Class methodName")
      sys.exit(-1)
    }
    val className = args(0)
    val methodName = args(1)

    val classLoader: ClassLoader = Thread.currentThread().getContextClassLoader
    val cls: Class[_] = classLoader.loadClass(className)
    val method: Method = cls.getMethod(methodName)
    AppStub.withApp(Profile.Console) {
      val result: AnyRef = method.invoke(cls)
      if (result != null) {
        println(result)
      }
    }
  }
}
