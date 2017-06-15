package webby.form.html
import webby.form.field.upload.UploadField
import webby.html.{CommonTag, HtmlBase, StdImgTag}

trait UploadPreviewFormHtml {self: StdFormHtml =>

  // ------------------------------- CSS styles -------------------------------

  def formUploadPreviewCls = "upload-preview"
  def formUploadPreviewTypeCls = "upload-preview__type"
  def formUploadPreviewImgCls = "upload-preview__img"

  // ------------------------------- Html methods -------------------------------

  def uploadPreviewImg(field: UploadField,
                       tn: StorageServerThumbnailParams,
                       addSize: Boolean = false,
                       imgClass: String = null,
                       outerTag: HtmlBase => CommonTag = _.a.targetBlank): Unit = {
    outerTag(view)
      .cls(formUploadPreviewCls)
      .dataTarget(field.htmlId)
      .attr("tn-params", tn.toString)
      .attr("tn-asjpeg", tn.asJpeg) {
        view.div.cls(formUploadPreviewTypeCls)
        val img: StdImgTag = view.img.cls(formUploadPreviewImgCls).cls(imgClass)
        if (addSize) img.w(tn.sizeWidth).h(tn.sizeHeight)
      }
  }
}

trait StorageServerThumbnailParams {
  def asJpeg: Boolean
  def toString: String
  def sizeWidth: Int
  def sizeHeight: Int
}
