package webby.form
import com.fasterxml.jackson.databind.JsonNode
import org.scalatest._
import webby.commons.io.StdJs

class FormTest extends FreeSpec with Matchers with Inside {
  val mapper = StdJs.defaultMapper

  "subform keys" - {
    case class _TestSubForm() extends StubForms.Common with SubForm
    class _TestForm extends StubForms.Common {
      val subForm = formList("subform", _TestSubForm())
    }

    "validate js setValue" in {
      def setJs(js: String) = new _TestForm().subForm.setJsValueAndValidate(mapper.readTree(js))
      setJs( """[]""") shouldBe a[FormSuccess.type]
      setJs( """[{}, {}, {"_key":0}, {"_key":0}]""") shouldBe a[FormSuccess.type]
      inside(setJs( """[{"_key":3}, {}, {"_key":3}]""")) {case e: FormErrors => e.errors.values.head.contains("Duplicate form key") shouldEqual true}
    }

    "check after setValue" in {
      val form = new _TestForm {
        subForm.add(s => s)
        subForm.add(s => s.key = 12)
        subForm.add(s => s.key = 9)
      }
      // "1" в первой записи появилась здесь потому, что formList автоматически присваивает ключи формам с нулевыми полями.
      // Это нужно для того, чтобы при сохранении, сервер смог сопоставить запись от клиента, и не помечать её как изменённую, если она не менялась.
      // Но formListWithDb так не делает.
      form.subForm.get.map(_.key) shouldEqual Seq(1, 12, 9)
      val subForm9 = form.subForm.get.apply(2)
      subForm9.key shouldEqual 9

      val tree: JsonNode = mapper.readTree( """[{}, {"_key":9}, {"_key":3}]""")
      form.subForm.setJsValueAndValidate(tree) shouldBe a[FormSuccess.type]

      val list: Vector[_TestSubForm] = form.subForm.get

      /* unknown key must be 0 (was 3) */ list.last.key shouldEqual 0
      list.map(_.key) shouldEqual Seq(0, 9, 0)
      /* subForm with key=9 should be the same (no copy) */ list(1) should be theSameInstanceAs subForm9
      form.subForm.removeOldForms.keys shouldEqual Set(1, 12)
    }
  }

  "test resetChanged" - {
    case class _TestSubForm() extends StubForms.Common with SubForm {
      val st = textField("st")
    }
    class _TestForm extends StubForms.Common {
      val t1 = textField("t1")
      val t2 = textField("t1")
      val subForm = formList("subform", _TestSubForm())
    }

    "default fields should be unchanged" in new _TestForm {
      Seq(t1, t2, subForm).foreach(_.changed shouldEqual false)
      changed shouldEqual false
    }

    "changed fields" in new _TestForm {
      t1.set("zzz")
      subForm.add(s => s.st.set("eee"))
      subForm.add(s => s)

      t1.get shouldEqual "zzz"
      t1.changed shouldEqual true
      t2.changed shouldEqual false
      subForm.valueGet(0).st.changed shouldEqual true
      changed shouldEqual true

      prepareBeforePost()
      Seq(t1, t2, subForm).foreach(_.changed shouldEqual false)
      changed shouldEqual false
    }
  }
}
