package js.form.field.upload;

import js.form.field.Field.FieldProps;
import js.html.Event;
import js.lib.TestUpload;
import js.ui.Drag;

class UploadField extends Field {
  static public var REG = 'upload';

  var uploadServer: String;
  var fileBaseUrl: String;
  var onUpload: Null<OnUploadProps>;
  var onUploadAddForm: Null<FormListField>;
  var onUploadToFieldId: Null<String>;
  var showType: Bool;

  // Таймаут хранения файла во временном хранилище файлового сервера
  var tempTimeout: Int;
  var tempTimer: Dynamic;////////////////

  var newContainerTag: Tag;
  var editContainerTags: Array<Tag>;
  var progressContainerTag: Null<Tag>;
  var previewBlockTags: Array<Tag>;
  var uploadFilenameBlockTags: Array<Tag>;

  var api: UploadFieldApi;
  var progress: Null<UploadFieldProgress>;
  var drag: Drag;

  var val: Dynamic;
  var valueBeforeUpload: Dynamic;

  public function new(form: Form, props: UploadFieldProps) {
    super(form, props);
    var uc: UploadConfig = G.require(form.config.uploadConfig, "UploadConfig is not configured");

    uploadServer = props.uploadServer[G.location.protocol == 'https:' ? 1 : 0];
    fileBaseUrl = props.fileBaseUrl;
    onUpload = props.onUpload;
    if (onUpload != null) {
      onUploadAddForm = G.require(cast form.fields.get(onUpload.addForm), 'OnUpload addForm "' + onUpload.addForm + '" not found');
//      onUploadAddForm.listen(FormListField.AddRemoveEvent, function() {newContainerTags.setCls(form.config.hiddenClass, onUploadAddForm.canAddForm());});
      onUploadToFieldId = onUpload.toField;
    }
    showType = props.showType;
    tempTimeout = initTempTimeout(uc);
    api = G.require(initApi(uc), "No UploadFieldApi configured");
    progress = initUploadFieldProgress(uc);

    function findTags(cls: String): Array<Tag> return form.tag.fndAll('.' + cls + '[data-target=' + htmlId + ']');
    newContainerTag = showHide(findTags(uc.uploadNewCls)[0], false);
    editContainerTags = showHideMany(findTags(uc.uploadEditCls), false);
    progressContainerTag = findTags(uc.uploadProgressCls)[0];
    if (progressContainerTag != null) showHide(progressContainerTag, false);
    previewBlockTags = findTags(uc.uploadPreviewBlockCls);
    uploadFilenameBlockTags = findTags(uc.uploadFilenameCls);

    if (initUploader()) {
      function openFileChooser(e: Event) {
        if (e.target != tag.el) { // Защита от циклических событий click, когда tag лежит в newContainerTag
          tag.trigger('click');
          e.preventDefault();
        }
      }
      newContainerTag.onClick(openFileChooser);
      for (t in findTags(uc.uploadOpenCls)) t.onClick(openFileChooser);
      for (t in findTags(uc.uploadClearCls)) t.onClick(function(e: Event) {setValue(null); e.preventDefault();});

      initProgressContainer();
    }
  }

  function initApi(c: UploadConfig): UploadFieldApi return c.api();

  function initTempTimeout(c: UploadConfig): Int return c.tempTimeout;

  function initUploadFieldProgress(c: UploadConfig): Null<UploadFieldProgress> return c.uploadFieldProgress();

  function initUploader(): Bool {
    if (!TestUpload.testUpload()) {
      onOldBrowser();
      return false;
    }
/*
    getToken = (tokenIdCallback) ->
      self.connectedAction {'getToken': true}, (result) ->
        if result['error'] then showError(result['error'])
        else if (!result['id']) then showUnexpectedError()
        else tokenIdCallback(result['id'])

    uploadFile = (formData, tokenId, onSuccess) ->
      xhr = new XMLHttpRequest()
      xhr.open('POST', self.uploadServer + '/upload-temp?' + tokenId, true)
      xhr.onreadystatechange = ->
        if xhr.readyState == 4
          if xhr.status == 200
            resp = JSON.parse(xhr.responseText)
            if resp['error'] then showError(resp['error'])
            else onSuccess(resp['filename'])
          else
            showUnexpectedError()

      if rr.util.testUpload.progress()
        xhr.upload.onprogress = (event) ->
          if event.lengthComputable
            self.progressValue(event.loaded / event.total * 100 | 0)
      xhr.send(formData)

    waitFinish = (tokenId, onComplete) ->
      check = ->
        $.jsonGet(self.uploadServer + '/info?' + tokenId, null, (result) ->
          if result['error']
            showError(result['error'])
          else if !result['completed']
            window.setTimeout(check, 500)
          else
            onComplete(result['path'])
        )
      check()

    startUpload = (files) ->
      self.showProgressContainer()
      self.valueBeforeUpload = self.value()
      formData = new FormData()
      formData.append('file', files[0])
      getToken (tokenId) ->
        uploadFile formData, tokenId, (filename) ->
          waitFinish tokenId, (path) ->
            self.setValue(path)
            self.uploadedFilename = filename
            self.$uploadFilenameBlocks.html(filename)
            self.startTempTimer()
*/
    drag = new Drag(newContainerTag);
    drag.hoverClass(newContainerTag, form.config.uploadConfig.dragOverCls);
//    drag.listen(Drag.DropEvent, function(e: Event) {startUpload(e.dataTransfer.files);});

//    tag.on('change', function() {startUpload(untyped tag.el.files)});
    return true;
  }

  function onOldBrowser() {
    for (t in newContainerTag) t.setHtml(form.config.strings.oldBrowserHtml());
  }

  function showError(error:String) {
    form.config.uploadConfig.showErrorMessage(form.config, error);
    setValue(valueBeforeUpload);
    drag.onCancelDrag();
  }

//  function showUnexpectedError()  -> showError('Произошла непредвиденная ошибка')


  function initProgressContainer() {
    if (progressContainerTag != null && progress != null) {
      progress.initAndAddTo(progressContainerTag.removeChildren());
    }
  }

  function showProgressContainer() {
    showHide(newContainerTag, false);
    showHideMany(editContainerTags, false);
    showHide(progressContainerTag, true);
    if (progress != null) {
      progressValue(0);
      progress.show();
    }
  }

  function progressValue(percent: Float) {
    if (progress != null) progress.progress(percent);
  }

  override public function initBoxTag(): Tag {
    var dropContainer = tag.fndParent(function(t: Tag) return t.hasCls(form.config.uploadConfig.dropContainerCls));
    return dropContainer != null ? dropContainer : super.initBoxTag();
  }

  override public function setValueEl(value: Null<Dynamic>) {
    val = value;
    stopTempTimer();
    if (value != null) {
      ///////////
    }

    showHide(progressContainerTag, false);
    showHide(newContainerTag, onUploadAddForm != null ? onUploadAddForm.canAddForm() : !value);
    showHideMany(editContainerTags, G.toBool(value));
    tag.setVal(null);
  }

  // TODO: вынести логику @$previewBlocks.each в отдельный класс или функцию
/*
  setValueEl: (@val) ->
    self = @
    @stopTempTimer()
    if val
      if @onUploadAddForm
        @onUploadFormAdded = form = @onUploadAddForm.addForm(true)
        form.fields[@onUploadToFieldId].setValue(val)
        @val = val = null
      else
        @$previewBlocks.each (idx, block) ->
          $block = $(block)
          $typeEl = $block.find('.upload-preview-type')
          tnParams = $block.attr('tn-params')
          tnAsJpeg = $block.is('[tn-asjpeg]')
          $img = $block.find('.upload-preview')
          img = $img[0]
          ext = rr.util.FileType.fromName(val)
          imgSrc = rr.util.FileType.getImageForType(ext)
          if imgSrc
            # Мы определили, что загруженный файл не является растровой картинкой, поэтому нужно сделать заглушку для него.
            [width, height] = tnParams.split('~', 1)[0].split('x')
            img.style.width = width + 'px'
            img.style.height = height + 'px'
          else
            # Загруженный файл является растровой картинкой. Показать превью.
            previewPath = val.replace(/(\.[^.]+)$/, '~' + tnParams + (if tnAsJpeg then '.jpg' else '$1'))
            imgSrc = self.fileBaseUrl + previewPath
            if !$img.attr('width') then img.style.width = img.style.height = 'inherit'
          img.src = imgSrc
          $block[0].href = self.fileBaseUrl + val

          if self.showType
            $typeEl.html('.' + ext).css('margin', '').show()
          else
            $typeEl.hide()

    @$progressContainer.hide()
    @$newContainer.toggle(if @onUploadAddForm then @onUploadAddForm.canAddForm() else !val)
    @$editContainer.toggle(!!val)
    @$el.val(null)
*/

  function stopTempTimer() {
    // TODO:
  }

/*
  stopTempTimer: ->
    if @tempTimer then clearTimeout(@tempTimer)
    @tempTimer
*/

  function startTempTimer() {
    // TODO:
  }
/*
  startTempTimer: ->
    self = @
    addedForm = if @onUploadAddForm then @onUploadFormAdded else null
    @stopTempTimer()
    @tempTimer = setTimeout( ->
      if addedForm then self.onUploadAddForm.removeForm(addedForm)
      else self.setValue(null)
      self.tempTimer = null
      rr.window.Message.show({
        title: 'Внимание!',
        text: 'Нам не удалось принять файл ' + self.uploadedFilename + ', который вы пытались загрузить. Пожалуйста, попробуйте ещё раз.'
      })
    , @tempTimeout)
*/

  override public function value(): Dynamic return val;
}

@:build(macros.ExternalFieldsMacro.build())
class UploadFieldProps extends FieldProps {
  public var uploadServer: Array<String>;
  public var fileBaseUrl: String;
  @:optional public var onUpload: OnUploadProps;
  public var showType: Bool;
}

@:build(macros.ExternalFieldsMacro.build())
class OnUploadProps {
  public var addForm: String;
  public var toField: String;
}
