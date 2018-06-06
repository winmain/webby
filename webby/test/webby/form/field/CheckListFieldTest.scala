package webby.form.field
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSuite
import webby.commons.io.StdJs
import webby.form.StubForms

class CheckListFieldTest extends FunSuite with MockFactory {
  test("should correctly serialize int js values") {
    val form = new StubForms.Common {}
    val items = Vector(1, 2, 3)
    val field = new CheckListField[Int](form, "id", items, _.toString, "v" + _)
    field := Seq(1, 2)

    assert(StdJs.defaultMapper.writeValueAsString(field.toJsVal) === """["1","2"]""")
  }

  test("should correctly serialize object js values") {
    case class Value(id: Int, title: String)
    val v3 = Value(3, "v3")
    val v5 = Value(5, "v5")

    val form = new StubForms.Common {}
    val items = Vector[Value](v3, v5)
    val field = new CheckListField[Value](form, "id", items, _.id.toString, _.title)
    field := Seq(v3, v5)

    assert(StdJs.defaultMapper.writeValueAsString(field.toJsVal) === """["3","5"]""")
  }

  test("should correctly serialize empty js value") {
    val form = new StubForms.Common {}
    val field = new CheckListField[String](form, "id", Nil, identity, identity)
    field.setNull

    assert(StdJs.defaultMapper.writeValueAsString(field.toJsVal) === """[]""")
  }
}
