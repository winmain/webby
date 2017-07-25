package webby.form

import com.fasterxml.jackson.databind.node.{NullNode, TextNode}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FreeSpec, Inside, Matchers}
import webby.form.field.upload.{PreparedStorageServerApi, StorageServerStoreResult, UploadField}

class FormUploadTest extends FreeSpec with Matchers with Inside with MockFactory {
  //  override def map(fs: => Fragments) = Step(AppStub.startTest) ^ fs ^ Step(AppStub.stop())

  class _TestForm extends StubForms.Common {
    val apiMock = stub[PreparedStorageServerApi]
    val upload = addField(new UploadField(this, "upload", apiMock))
  }
  case class _StoreResult(path: String, fileSize: Int) extends StorageServerStoreResult

  "UploadField" - {
    "check for invalid non-temp filenames" in new _TestForm {
      prepareBeforePost()
      apiMock.isTempFile _ when * returns false
      upload.setJsValueAndValidate(new TextNode("abc111.jpg")) shouldBe a[FormErrors]
    }

    "empty value" in new _TestForm {
      prepareBeforePost()
      upload.setJsValueAndValidate(NullNode.getInstance()) shouldBe a[FormSuccess.type]
      upload.changed shouldEqual false
    }

    "value not changed" in new _TestForm {
      upload.set("abc.jpg")
      prepareBeforePost()
      upload.setJsValueAndValidate(new TextNode("abc.jpg")) shouldBe a[FormSuccess.type]
      upload.changed shouldEqual false
      applyValues(formRemoved = false)
      apiMock.delete _ verify * never()
      apiMock.newToken _ verify() never()
      apiMock.store _ verify * never()
    }

    "delete value" in new _TestForm {
      upload.set("abc.jpg")
      prepareBeforePost()
      upload.setJsValueAndValidate(NullNode.getInstance()) shouldBe a[FormSuccess.type]
      applyValues(formRemoved = false)
      apiMock.delete _ verify "abc.jpg" once()
      apiMock.newToken _ verify() never()
      apiMock.store _ verify * never()
    }

    "store temp value" in new _TestForm {
      val tmpName = "tmp/abc.jpg"

      val storeResult = _StoreResult("stored.jpg", -1)
      apiMock.store _ when tmpName returns Some(storeResult) once()
      apiMock.store _ when * never()

      apiMock.isTempFile _ when tmpName returns true

      prepareBeforePost()
      upload.setJsValueAndValidate(new TextNode(tmpName)) shouldBe a[FormSuccess.type]
      applyValues(formRemoved = false)
      apiMock.delete _ verify * never()
      apiMock.newToken _ verify() never()
      upload.get shouldEqual storeResult.path
      upload.changed shouldEqual true
    }

    "store temp value with error on StorageServer" in new _TestForm {
      val tmpName = "tmp/abc.jpg"

      val storeResult = None
      apiMock.store _ when tmpName returns storeResult once()
      apiMock.store _ when * never()

      apiMock.isTempFile _ when tmpName returns true

      prepareBeforePost()
      upload.setJsValueAndValidate(new TextNode(tmpName)) shouldBe a[FormSuccess.type]
      applyValues(formRemoved = false)
      apiMock.delete _ verify * never()
      apiMock.newToken _ verify() never()
      upload.getOpt shouldEqual None
      upload.changed shouldEqual true
    }


    "reupload new value" in new _TestForm {
      val oldName = "old.jpg"
      val tmpNewName = "tmp/new.jpg"
      upload.set(oldName)

      val storeResult = _StoreResult(path = "stored.jpg", fileSize = -1)
      apiMock.store _ when tmpNewName returns Some(storeResult) once()
      apiMock.store _ when * never()

      apiMock.isTempFile _ when tmpNewName returns true

      prepareBeforePost()
      upload.setJsValueAndValidate(new TextNode(tmpNewName)) shouldBe a[FormSuccess.type]
      applyValues(formRemoved = false)
      apiMock.delete _ verify oldName once()
      apiMock.newToken _ verify() never()
      upload.get shouldEqual storeResult.path
      upload.changed shouldEqual true
    }

    "reupload new value with error on StorageServer" in new _TestForm {
      val oldName = "old.jpg"
      val tmpNewName = "tmp/new.jpg"
      upload.set(oldName)

      val storeResult = None
      apiMock.store _ when tmpNewName returns storeResult once()

      apiMock.isTempFile _ when tmpNewName returns true

      prepareBeforePost()
      upload.setJsValueAndValidate(new TextNode(tmpNewName)) shouldBe a[FormSuccess.type]
      applyValues(formRemoved = false)
      apiMock.delete _ verify oldName once()
      apiMock.newToken _ verify() never()
      apiMock.store _ verify tmpNewName once()
      upload.getOpt shouldEqual None
      upload.changed shouldEqual true
    }
  }
}
