package webby.form.html
import webby.form.field.upload.UploadField
import webby.html._

trait UploadFormHtml {self: StdFormHtml =>
  /*
// Пример расположения элементов для аплоада файлов в элемент f.photo:

// Контейнер для загрузки нового файла .newContainer
f.photo.newContainer() < {
  // Обязательно наличие .inputFile (именно в блоке newContainer, который имеет класс drop-container)
  f.photo.inputFile

  // Зона, показываемая только при драг-н-дропе
  div.cls("drop-zone") ~~ "Поместите сюда вашу фотографию"

  // Зона, показываемая только в отсутствии драг-н-дропе (клик на эту зону открывает диалог загрузки файла)
  div.cls("drop-idle") ~~ "Загрузите фотографию или перетащите файл сюда"
}

// Контейнер, который показывается, когда файл уже есть (либо он только что загружен)
f.photo.editContainer() < {
  // Превью изображения
  f.photo.previewImg(f.photo.token.thumbnails.head)
  // Имя загруженного файла (заполняется только сразу после загрузки)
  f.photo.uploadFilename()

  // Ссылки загрузки нового файла и удаления файла
  f.photo.uploadLink ~~ "Изменить фотографию"
  f.photo.clearLink ~~ "Удалить фотографию"
}

// Контейнер, который показывается только во время загрузки файла. Он автоматически заполняется тегами в js UploadField.
f.photo.progressContainer().style("height:200px")

// Примечание: из всех контейнеров [newContainer, editContainer, progressContainer] всегда показывается только один.
   */

  // ------------------------------- CSS styles -------------------------------

  def formUploadNewCls = "upload-new"
  def formUploadEditCls = "upload-edit"
  def formUploadFilenameCls = "upload-filename"
  def formUploadOpenCls = "upload-open"
  def formUploadClearCls = "upload-clear"
  def formUploadProgressCls = "upload-progress"

  def formDropContainerCls = "drop-container"
  def formDropZoneFillCls = "drop-zone-fill"
  def formDropZoneCls = "drop-zone"
  def formDropIdleCls = "drop-idle"

  // ------------------------------- Html methods -------------------------------

  /** Тег input[type=file]. Его наличие обязательно. */
  def uploadInputFile(field: UploadField): StdInputFileTag = {
    val inp: StdInputFileTag = view.inputFile.id(field.htmlId)
    if (field.storageApi.acceptOnlyImage) inp.accept("image/*")
    inp
  }

  /** Контейнер для загрузки нового файла */
  def uploadNewContainer(field: UploadField, tag: String = "div", dropContainer: Boolean = true, dropZoneFill: Boolean = false): CommonTag =
    view.tag(tag)
      .cls(formUploadNewCls)
      .cls(form.base.hiddenCls)
      .dataTarget(field.htmlId)
      .clsIf(dropContainer, formDropContainerCls)
      .clsIf(dropZoneFill, formDropZoneFillCls)

  /** Контейнер, который показывается, когда файл уже есть (либо он только что загружен) */
  def uploadEditContainer(field: UploadField, tag: String = "div"): CommonTag =
    view.tag(tag)
      .cls(formUploadEditCls)
      .cls(form.base.hiddenCls)
      .dataTarget(field.htmlId)

  /** Блок, содержащий имя загруженного файла */
  def uploadFilename(field: UploadField, tag: String = "span"): CommonTag =
    view.tag(tag)
      .cls(formUploadFilenameCls)
      .dataTarget(field.htmlId)

  def uploadLink(field: UploadField): StdATag =
    view.a.hrefAnchor
      .cls(formUploadOpenCls)
      .dataTarget(field.htmlId)

  def uploadClearLink(field: UploadField): StdATag =
    view.a.hrefAnchor
      .cls(formUploadClearCls)
      .dataTarget(field.htmlId)

  /** Контейнер, который показывается только во время загрузки файла. Он автоматически заполняется тегами в js UploadField. */
  def uploadProgressContainer(field: UploadField, tag: String = "div"): CommonTag =
    view.tag(tag)
      .cls(formUploadProgressCls)
      .cls(form.base.hiddenCls)
      .dataTarget(field.htmlId)
}
