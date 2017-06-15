package js.form.field.upload;

import js.form.field.upload.UploadField.UploadFieldApiAccess;
import js.html.FileList;
import js.html.FormData;

/*
Upload server API for UploadField
 */
interface UploadFieldApi {
  // TODO
  function startUpload(access: UploadFieldApiAccess, fileList: FileList): Void;
}


class StorageServerUploadFieldApi implements UploadFieldApi {
  public function new() {
  }

  public function startUpload(access: UploadFieldApiAccess, fileList: FileList): Void {
    access.showProgressContainer();
    access.valueBeforeUpload = access.field.value();
    var formData = new FormData();
    formData.append('file', fileList[0]);
    getToken(access, function(tokenId: String) {
      uploadFile(formData, tokenId, function(filename: String) {
        waitFinish(tokenId, function(path: String) {
          access.field.setValue(path);
          access.uploadedFilename = filename;
          for (t in access.uploadFilenameBlockTags) t.setHtml(filename);
          access.startTempTimer();
        });
      });
    });
  }

  function getToken(access: UploadFieldApiAccess, tokenIdCallback: String -> Void) {
    access.field.connectedAction({'getToken': true}, function(result: External) {
      if (result.get('error') != null) access.field.showError(result.get('error'));
      else if (result.get('id') == null) access.field.showUnexpectedError();
      else tokenIdCallback(result.get('id'));
    });
  }

  function uploadFile(formData: FormData, tokenId: String, onSuccess: String -> Void) {
    // TODO
  }

  function waitFinish(tokenId: String, onComplete: String -> Void) {
    // TODO
  }
}
