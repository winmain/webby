package js.ui;

import js.lib.ArrayUtils;
import goog.events.EventTarget;
import js.html.Element;
import js.html.Event;

/*
Вспомогательный класс для реализации механизма drag-and-drop upload.
 */
class Drag extends EventTarget {
  // ------------------------------- Styles -------------------------------

  public static inline var DocumentHoverCls = 'drag';

  // ------------------------------- Events -------------------------------

  public static inline var EnterEvent = 'enter';
  public static inline var LeaveEvent = 'leave';
  public static inline var DropEvent = 'drop';

  // ------------------------------- Class -------------------------------

  var tag: Tag;
  var dragDepth = 0;

  public function new(tag: Tag) {
    super();
    this.tag = G.require(tag, "Drag tag is null");
    initOnce();
    var timeout = -1;
    all.push(this);

    tag.on('dragenter', onlyFilesDecorator(function(e: Event) {
      dragDepth++;
      dispatchEvent({type: EnterEvent, dataTransfer: untyped e.dataTransfer});
    }));

    tag.on('dragover', function(e: Event) {if (untyped e.preventDefault) e.preventDefault();});

    tag.on('dragleave', onlyFilesDecorator(function(e: Event) {
      dragDepth--;
      G.window.clearTimeout(timeout);
      timeout = G.window.setTimeout(function() {
        if (dragDepth == 0) dispatchEvent({type: LeaveEvent, dataTransfer: untyped e.dataTransfer});
      }, 100);
    }));
  }

  public function hoverClass(tag: Tag, cls: String) {
    listen(EnterEvent, function() {tag.cls(cls);});
    listen(LeaveEvent, function() {tag.clsOff(cls);});
  }

  public function onCancelDrag(): Void {
    htmlDrag.dispatchEvent({type: LeaveEvent});
  }

  // ------------------------------- Static vars & methods -------------------------------

  private static var htmlDrag: Drag;

  private static var initialized = false;

  // Список всех объектов DragAndDrop
  private static var all: Array<Drag> = [];

  /*
  Инициализация событий документа (html) один раз, при первом создании объекта Drag
   */
  static function initOnce() {
    if (initialized) return;
    initialized = true;

    var docTag = Tag.getHtml();
    htmlDrag = new Drag(docTag);
    htmlDrag.hoverClass(docTag, DocumentHoverCls);

    docTag.on('drop', onlyFilesDecorator(function(e: Event) {
      var el: Element = cast e.target;
      var drag: Drag = null;
      while (drag == null) {
        for (d in all) {
          if (d.tag.el == el) {
            drag = d;
            break;
          }
        }
        el = el.parentElement;
      }
      drag.dispatchEvent({type: DropEvent, dataTransfer: untyped e.dataTransfer});
      drag.dispatchEvent({type: LeaveEvent, dataTransfer: untyped e.dataTransfer});
      htmlDrag.dispatchEvent({type: LeaveEvent});
      for (d in all) d.dragDepth = 0;
    }));
  }

  static function onlyFilesDecorator(bodyFn: Event -> Void) return function(e: Event) {
    if (untyped e.preventDefault) e.preventDefault();
    if (ArrayUtils.contains(untyped e.dataTransfer.types, 'Files')) {
      bodyFn(e);
    }
  }
}
