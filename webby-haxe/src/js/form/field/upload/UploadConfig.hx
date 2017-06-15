package js.form.field.upload;

class UploadConfig {
  public function new() {
  }

  // ------------------------------- Styles -------------------------------

  public var uploadNewCls = 'upload-new';
  public var uploadEditCls = 'upload-edit';
  public var uploadProgressCls = 'upload-progress';
  public var uploadPreviewBlockCls = 'upload-preview-block';
  public var uploadPreviewTypeCls = 'upload-preview-type';
  public var uploadFilenameCls = 'upload-filename';
  public var uploadOpenCls = 'upload-open';
  public var uploadClearCls = 'upload-clear';

  public var dropContainerCls = 'drop-container';

  public var dragOverCls = 'drag-over';

  // ------------------------------- Defaults -------------------------------

  public function api(): UploadFieldApi return new UploadFieldApi.StorageServerUploadFieldApi();

  public var tempTimeout: Int = 3 * 3600 * 1000; // Таймаут хранения файла во временном хранилище файлового сервера

  public function uploadFieldProgress(): Null<UploadFieldProgress> return null;

  public function uploadFieldPreview(): Null<UploadFieldPreview> return null;

  public function showErrorMessage(formConfig: FormConfig, text: String): Void {
    formConfig.showFormErrorDialog(formConfig.strings.errorUploadTitle() + "<br>" + text);
  }
}
