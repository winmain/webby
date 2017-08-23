package js.form.field.upload;

import js.html.Image;

/*
Managing previews of uploaded files and pictures for UploadField
 */
interface UploadFieldPreview {
  function managePreviews(field: UploadField, findTags: String -> Array<Tag>, fileName: String): Void;
}


class CommonUploadFieldPreview implements UploadFieldPreview {

  // ------------------------------- Config and styles -------------------------------

  public var uploadPreviewCls = 'upload-preview';
  public var uploadPreviewTypeCls = 'upload-preview__type';
  public var uploadPreviewImgCls = 'upload-preview__img';

  // ------------------------------- Vars -------------------------------

  public var fileType: UploadFileType;

  public function new(fileType: UploadFileType) {
    this.fileType = fileType;
  }

  public function managePreviews(field: UploadField, findTags: String -> Array<Tag>, fileName: String): Void {
    for (t in findTags(uploadPreviewCls)) {
      managePreview(field, t, fileName);
    }
  }

  function managePreview(field: UploadField, previewBlockTag: Tag, fileName: String): Void {
    var typeTag = previewBlockTag.fndByCls(uploadPreviewTypeCls);
    var tnParams = previewBlockTag.getAttr('tn-params');
    var tnAsJpeg = G.toBool(previewBlockTag.getAttr('[tn-asjpeg]'));
    var imgTag: Tag = G.require(previewBlockTag.fndByCls(uploadPreviewImgCls), "No ." + uploadPreviewImgCls);
    var imgEl: Image = cast imgTag.el;
    var ext = fileType.fromName(fileName);
    var imgSrc = fileType.getImageForType(ext);

    function setImgSize(w: String, h: String) {
      imgEl.style.width = w;
      imgEl.style.height = h;
    }

    if (imgSrc != null) {
      // Мы определили, что загруженный файл не является растровой картинкой, поэтому нужно сделать заглушку для него.
      var splitted = tnParams.split('~')[0].split('x');
      setImgSize(splitted[0] + 'px', splitted[1] + 'px');
    } else {
      // Загруженный файл является растровой картинкой. Показать превью.
      var previewPath = fileName.replace(new RegExp('(\\.[^.]+)$'), '~' + tnParams + (tnAsJpeg ? '.jpg' : '$1'));
      imgSrc = field.fileBaseUrl + previewPath;
      if (!imgTag.hasAttr('width')) {
        setImgSize('inherit', 'inherit');
      }
    }
    imgEl.src = imgSrc;
    previewBlockTag.href(field.fileBaseUrl + fileName);

    if (field.showType) {
      typeTag.setHtml('.' + ext).style('margin: inherit').clsOff(field.form.config.hiddenClass);
    } else {
      typeTag.cls(field.form.config.hiddenClass);
    }
  }
}
