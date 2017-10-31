package js.form.field.upload;

import haxe.Json;
import js.form.field.upload.UploadField.UploadFieldApiAccess;
import js.html.FileList;
import js.html.FormData;
import js.html.ProgressEvent;
import js.html.XMLHttpRequest;
import js.lib.TestUpload;
import js.lib.XhrUtils;

/*
Upload server API for UploadField
 */
interface UploadFieldApi {
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
      uploadFile(access, formData, tokenId, function(filename: String) {
        waitFinish(access, tokenId, function(path: String) {
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
      if (result.get('error') != null) access.showError(result.get('error'));
      else if (result.get('id') == null) access.showUnexpectedError();
      else tokenIdCallback(result.get('id'));
    });
  }

  function uploadFile(access: UploadFieldApiAccess, formData: FormData, tokenId: String, onSuccess: String -> Void) {
    var xhr = new XMLHttpRequest();
    xhr.open('POST', uploadTempUrl(access, tokenId), true);
    xhr.onreadystatechange = function() {
      if (xhr.readyState == XMLHttpRequest.DONE) {
        if (xhr.status == 200) {
          var resp: External = Json.parse(xhr.responseText);
          if (resp.get('error') != null) access.showError(resp.get('error'));
          else onSuccess(resp.get('filename'));
        } else {
          access.showUnexpectedError();
        }
      }
    }
    xhr.onerror = access.showUnexpectedError;

    if (TestUpload.testProgress()) {
      xhr.upload.onprogress = function(event: ProgressEvent) {
        if (event.lengthComputable) {
          access.progressValue(event.loaded / event.total * 100);
        }
      }
    }
    xhr.send(formData);
  }

  function waitFinish(access: UploadFieldApiAccess, tokenId: String, onComplete: String -> Void) {
    function check() {
      XhrUtils.jsonGet(infoUrl(access, tokenId), function(result: External) {
        if (result.get('error') != null) {
          access.showError(result.get('error'));
        } else if (!result.getBool('completed')) {
          G.window.setTimeout(check, 500);
        } else {
          onComplete(result.get('path'));
        }
      }, function(xhr) {
        access.showUnexpectedError();
      });
    }
    check();
  }

  function uploadTempUrl(access: UploadFieldApiAccess, tokenId: String): String return access.uploadServer + '/upload-temp?' + tokenId;

  function infoUrl(access: UploadFieldApiAccess, tokenId: String): String return access.uploadServer + '/info?' + tokenId;
}
