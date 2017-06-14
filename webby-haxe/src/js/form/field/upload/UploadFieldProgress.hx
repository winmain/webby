package js.form.field.upload;

/*
Interface to progressbar component
 */
interface UploadFieldProgress {
  function initAndAddTo(tag: Tag): Void;

  function show(): Void;

  function progress(percent: Float): Void;
}


/*
Default implementation of UploadFieldProgress
 */
class CommonUploadFieldProgress implements UploadFieldProgress {
  public var progressBarCls = 'progress-bar';
  public var progressBarHeight = 17;
  public var valueCls = 'progress-bar__value';
  public var textCls = 'progress-bar__text';

  public var loadingText = 'Загрузка';

  public var containerTag: Tag;
  public var progressBarTag: Tag;
  public var valueTag: Tag;

  public function new() {
  }

  public function initAndAddTo(tag: Tag): Void {
    containerTag = tag;
    progressBarTag = Tag.div.cls(progressBarCls);
    valueTag = Tag.div.cls(valueCls);
    tag.add(progressBarTag.add(valueTag));
    progressBarTag.add(Tag.div.cls(textCls).setHtml(loadingText));
  }

  public function show(): Void {
    progressBarTag.el.style.marginTop = (containerTag.el.clientHeight / 2 - progressBarHeight) + "px";
  }

  public function progress(percent: Float): Void {
    valueTag.el.style.width = percent + '%';
  }
}
