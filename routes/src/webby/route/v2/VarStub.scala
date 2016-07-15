package webby.route.v2

import webby.route.{BigDecimalVar, IntVar, StringVar, Var}

object VarStub {

  import scala.reflect.runtime.universe

  trait Stub {
    def makeStub(index: Int): AnyRef
    def toVar(name: String, pat: Option[String]): Var[_]
  }

  object IntStub extends Stub {
    override def makeStub(index: Int): AnyRef = index.asInstanceOf[AnyRef]
    override def toVar(name: String, pat: Option[String]): Var[_] = new IntVar(name, pat)
  }

  object StringStub extends Stub {
    override def makeStub(index: Int): AnyRef = index.toString
    override def toVar(name: String, pat: Option[String]): Var[_] = new StringVar(name, pat)
  }

  object BigDecimalStub extends Stub {
    override def makeStub(index: Int): AnyRef = BigDecimal(index)
    override def toVar(name: String, pat: Option[String]): Var[_] = new BigDecimalVar(name, pat)
  }

  private val intType = universe.typeOf[scala.Int]
  private val stringType = universe.typeOf[java.lang.String]
  private val bigDecimalType = universe.typeOf[scala.BigDecimal]

  def resolve(tpe: universe.Type, method: => String): Stub = tpe match {
    case s if s =:= intType => IntStub
    case s if s =:= stringType => StringStub
    case s if s =:= bigDecimalType => BigDecimalStub
    case t => sys.error("Unknown variable type for route handler " + t + " in method " + method)
  }

  def indexFromStub(stub: Any): Int = stub match {
    case int: Int => int
    case str: String => str.toInt
    case bd: BigDecimal => bd.toInt
    case s => sys.error("Cannot recognize stub value: " + s)
  }
}
