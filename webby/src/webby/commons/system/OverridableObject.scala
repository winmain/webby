package webby.commons.system

/**
  * Объект, унаследованный от этого класса, может иметь другую реализацию в самом приложении.
  * На самом деле, приложение наследует не этот объект, а тип [[OverridableObject#Value]].
  *
  * Этот объект содержит ссылку на единственную реализацию.
  * Она может быть проинициализирована только один раз одним из двух способов (какой случится раньше):
  * 1. Через вызов метода [[get]], тогда реализация будет дефолтной (то что возрватит метод [[default]])
  * 2. Через инициализацию наследника [[Value]]
  *
  * Пример кода в библиотеке webby:
  * {{{
  *   object StdJs extends OverridableObject {
  *     class Value extends Base {
  *       val mapper: ObjectMapper = newMapper
  *
  *       def newMapper = new ObjectMapper()
  *     }
  *
  *     override protected def default: Value = new Value
  *   }
  * }}}
  *
  * Пример кода в приложении:
  * {{{
  *   object Js extends StdJs.Value {
  *     override def newMapper = new ObjectMapper().registerModule(DefaultScalaModule)
  *   }
  * }}}
  *
  * Таким образом, если первым вызвать объект `Js`, то StdJs.get будет выдавать ссылку на Js,
  * и у нас по во всём проекте будет использоваться только один дефолтный mapper из `Js.mapper`.
  */
abstract class OverridableObject {
  type Value <: Base

  trait Base {self: Value =>
    require(_obj == null, "Object cannot be initialized twice: " + getClass.getName)
    _obj = this
  }

  private var _obj: Value = _

  def get: Value = {
    if (_obj == null) {
      default
      require(_obj != null, "Object not initialized properly")
    }
    _obj
  }

  protected def default: Value
}
