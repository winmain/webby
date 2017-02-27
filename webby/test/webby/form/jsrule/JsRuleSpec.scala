package webby.form.jsrule

import com.fasterxml.jackson.databind.{MapperFeature, ObjectMapper}
import org.scalatest.{FunSuite, Matchers}
import webby.commons.io.StdJs
import webby.form.StubForms

class JsRuleSpec extends FunSuite with Matchers {
  // Этот тест проверяет, что все поля JsRule просериализованы
  // Он будет падать для jackson версии 2.5 и выше (пока мы не придумаем как это пофиксить)
  test("test json serialization") {
    class MyForm extends StubForms.Common {
      val field = checkField("tt")
      addRule(_ when field.isEmptyRule show field)
    }
    val f = new MyForm
    val mapper: ObjectMapper = StdJs.get.newMapper
    mapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
    val result = mapper.writeValueAsString(f.clientRules)
    result shouldEqual
      """[{"actions":[{"focus":false,"vis":true,"withParent":false,"cls":"visible","field":"tt"}],"cond":{"cls":"fieldEmpty","field":"tt"}}]"""
  }
}
