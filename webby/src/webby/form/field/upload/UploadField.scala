package webby.form.field.upload

import java.net.{ConnectException, SocketTimeoutException}

import com.fasterxml.jackson.databind.JsonNode
import webby.api.mvc.{PlainResult, Results}
import webby.form.field.{FormListField, ValueField}
import webby.form.{Form, FormResult}


/**
  * Поле, отвечающиее за аплоад файлов, а также за показ текущих загруженных превью.
  *
  * Поведение поля в специфичных ситуациях:
  * При загрузке нового файла взамен старого, старый удаляется через [[PreparedStorageServerApi]].
  * При ошибке загрузке нового файла, поле обнуляется, а файл отбрасывается.
  * При этом, старый файл (если он был) удаляется через [[PreparedStorageServerApi]].
  *
  * @param shortId    id (имя) поля
  * @param storageApi Подготовленный API для работы с файловым сервером.
  *                   Уже содержит в себе token (правила загрузки файлов) и suffix (суффикс для всех загружаемых файлов).
  */
class UploadField(val form: Form,
                  val shortId: String,
                  val storageApi: PreparedStorageServerApi,
                  val showType: Boolean = false)
  extends ValueField[String] {self =>

  case class OnUpload(addForm: String, toField: String)
  var onUpload: OnUpload = null

  /**
    * Размер загруженного файла. Может устанавливать своё значение из формы.
    * После загрузки файла и вызова applyValue() обновляет своё значение (также, сбрасывается в 0 при удалении файла из формы).
    */
  var fileSize: Int = 0

  def set(v: String, fileSize: Int) {set(v); this.fileSize = fileSize}

  /** Значение этого поля после инициализации формы. Оно нужно, чтобы определить что делать со старым файлом, если его заменили или удалили. */
  protected var originalValue: String = null

  override def prepareBeforePost(): Unit = {
    super.prepareBeforePost()
    originalValue = get
  }

  // ------------------------------- Reading data & js properties -------------------------------

  class JsProps extends BaseJsProps {
    val onUpload = self.onUpload
    val showType = self.showType
    val uploadServer = (storageApi.externalUploadBaseUrlHttp, storageApi.externalUploadBaseUrlHttps)
    val fileBaseUrl = storageApi.externalFileBaseUrl
  }

  override def jsField: String = "upload"
  override def jsProps: BaseJsProps = new JsProps
  override def parseJsValue(node: JsonNode): Either[String, String] = parseJsString(node) {v =>
    if (v == originalValue) Right(v)
    else if (storageApi.isTempFile(v)) Right(v)
    else Left(form.strings.invalidValue)
  }
  override def nullValue: String = null

  case class GetTokenResult(id: String)
  case class GetTokenError(error: String)

  /** Вызов специального действия для этого поля из js */
  override def connectedAction(tree: JsonNode): PlainResult = {
    if (tree.hasNonNull("getToken")) {
      try Results.JsonOk(GetTokenResult(storageApi.newToken()))
      catch {
        case _: SocketTimeoutException | _: ConnectException =>
          Results.JsonOk(GetTokenError(form.strings.storageServerIsUnavailable))
      }
    }
    else Results.BadRequest("Invalid action")
  }

  // ------------------------------- Builder & validations -------------------------------

  /**
    * Назначить специальное действие после загрузки файла - добавить подформу formField
    * и переместить загруженный файл в поле добавленной формы toFormField.
    * Внимание! Это поле и formField должны иметь одного и того же форму-родителя.
    * Также, поле toFormField должно быть в форме formField.
    */
  def onUploadAddSubForm(formField: FormListField[_], toFormField: UploadField): this.type = {
    onUpload = OnUpload(formField.shortId, toFormField.shortId)
    this
  }

  /**
    * Применить или зафиксировать значение поля. Это действие вызывается после поста и валидации, и перед сохранением формы.
    */
  override def applyValues(formRemoved: Boolean): FormResult = {
    def delete() {
      storageApi.delete(originalValue)
      fileSize = 0
    }
    if (formRemoved && originalValue != null) delete()
    else {
      get match {
        case v if v == originalValue => ()
        case null => delete()
        case v if storageApi.isTempFile(v) =>
          storageApi.store(v) match {
            case Some(result) =>
              setValue(result.path)
              fileSize = result.fileSize
              // При загрузке нового файла нужно удалить старый, если он есть.
              if (originalValue != null) storageApi.delete(originalValue)
            case None =>
              // В случае неудачной загрузки обнуляем значение нашего поля.
              // Если originalValue != null, то мы также удаляем originalValue.
              // Получается, независимо от предыдущего состояния поля, в случае ошибки оно будет очищено.
              setNull
              applyValues(formRemoved)
          }
        case v => sys.error(s"Invalid value:'$v', original:'$originalValue'")
      }
    }
    validate
  }
}
