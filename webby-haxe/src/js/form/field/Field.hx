package js.form.field;

import goog.events.EventTarget;
import js.form.Form;
import js.html.Event;
import js.html.InputElement;
import js.html.KeyboardEvent;

@:autoBuild(macros.KeepConstructorMacro.build())
class Field extends EventTarget {
  // ------------------------------- Events -------------------------------

  public static inline var SetValueEvent = 'set-value';
  public static inline var ChangeEvent = 'change';

  // ------------------------------- Class -------------------------------

  public var form(default, null): Form;
  public var props(default, null): FieldProps;

  /* Error message or null if no error */
  public var error(default, null): Null<String> = null;
  public var emptyError(default, null): Bool = false;
  /* Field visible? */
  public var vis(default, null): Bool = true;

  public var field(default, null): String;
  public var shortId(default, null): String;
  public var htmlId(default, null): String;
  public var required: Bool;

  public var tag(default, null): Tag;
  public var box(default, null): Tag;
  public var errorTag(default, null): Tag;
  public var block(default, null): Null<FormGroup>;

  public function new(form: Form, props: FieldProps) {
    super();
    this.form = form;
    this.props = props;
    field = props.field;
    shortId = props.shortId;
    htmlId = form.htmlId + '-' + props.shortId;
    tag = initTag(); // TODO: неплохо бы добавить свойство элемента 'field', которое будет ссылаться на this
    if (tag == null) throw new Error('Field node #${htmlId} not found');
//    @$el = @initEl().prop('field', @)
    box = initBoxTag().cls(form.config.fieldBoxClass);

    required = G.toBool(props.required);
    updateRequired();
    errorTag = createErrorTag();
    initElEvents();
    block = findFormBlock();
    if (!props.enterKeySubmit && canSuppressEnterKey()) {
      tag.on('keypress', function(e: KeyboardEvent) {
        if (e.which == 13) {
          e.preventDefault();
          return false;
        }
        return null;
      });
    }
  }

  /*
  Дополнительная инициализация, которая не может работать в конструкторе из-за неполностью
  проинициализированных переменных.
   */
  public function init(props: FieldProps) {
    enable(props.enabled == null ? true : props.enabled);
  }

  /*
  Установить значение полю. Если нужно прописать другой код для установки этого значения элементу, то следует переопределить setValueEl.
  Если же требуется предварительная конвертация значения перед установкой, тогда этот метод можно переопределить (например, конвертировать дату в текст).
   */
  @:keep
  @:expose('setValue')
  public function setValue(v: Null<Dynamic>) {
    var oldValue = value();
    setValueEl(v);
    // for test only // trace(name + '.setValue: ' + v);
    dispatchEvent({
      type: SetValueEvent,
      value: v
    });
    if (JSON.stringify(value()) != JSON.stringify(oldValue)) {
      onChange();
      dispatchEvent({
        type: ChangeEvent,
        setValue: true
      });
    }
  }

  public function setValueEl(value: Null<Dynamic>) {
    tag.setVal(isEmptyValue(value) ? null : value);
  }

  public function value(): Dynamic return tag.val();

  @:final public function isEmpty(): Bool return isEmptyValue(value());

  public function isEmptyValue(v: Dynamic): Bool return !v;

  public function updateRequired() {
    box.setCls(form.config.fieldBoxRequiredClass, required);
  }

  public function initTag(): Tag return form.tag.fnd('#' + htmlId);

  /*
  Найти и вернуть rr.form.FormBlock, в который вложено это поле; либо null, если такого не существует.
   */
  public function findFormBlock(): Null<FormGroup> {
    var formBlockTag: Tag = tag.fndParent(function(t: Tag) return t.hasCls(form.config.formGroupClass));
    if (formBlockTag != null) return form.findBlock(formBlockTag);
    return null;
  }

  /*
  Перейти к этому полю (например, при клике на бирку блока/формы с ошибкой)
   */
  public function focus() {
    tag.el.focus();
  }

  public function show(v: Bool) {
    vis = v;
    var t: Tag;
    if (hideWithRow()) {
      t = getRow();
      if (t == null) t = box;
    } else {
      t = box;
    }
    t.setCls(form.config.hiddenClass, !v);
  }

  /*
  Вызывается сразу после показа формы, независимо от видимости самого элемента
   */
  public function onFormShown() {
    updateErrorTag();
  }

  public function enable(en: Bool) {
    tag.attr('disabled', !en);
  }

  public function initElEvents() {
    tag.on('change', onChangeWithEvent);
    tag.on('keyup', function(e: KeyboardEvent) {
      if ((e.which == 13 && !new RegExp('[\n\r]').test(tag.val())) || (e.which >= 33 && e.which <= 40)) {
        // Проверка для случая, если был нажат enter, который привёл к сабмиту.
        // onChange() вызывать не следует, т.к. это приведёт к сбросу ошибки в этом поле, хотя само значение на самом деле не менялось.
        // Также, нажатие на стрелки и клавишы навигации не должно приводить к onChange()
        return;
      }
      onChangeWithEvent(e);
    });
    tag.on('focus', onFocus);
  }

//  fieldPath: -> @form.fieldPath(@name)
//
//  ###
//  Выполнить специальное действие для этого поля на сервере.
//  В data передаются данные, которые будут прочитаны методом Field.connectedAction() на сервере.
//  В callback придёт json result.
//  ###
//  connectedAction: (data, callback) ->
//    topForm = @form.topForm()
//    data['field'] = @fieldPath()
//    $.jsonPost(topForm.formAction(), data, callback)

  /*
  Подавлять нажатие enter'а в этом поле, чтобы форма не делала submit?
   */
  public function canSuppressEnterKey(): Bool return untyped tag.el.length && tag.el.tagName == 'INPUT';

  /*
  Do this field needed to be reinitialized every time after form submit?
  (this method can be overloaded in descendants)
   */
  public function reInitAfterSubmit(): Bool return false;

  /*
  Default `hideWithRow` value. Can be overriden in subclasses.
   */
  function defaultHideWithRow(): Bool return false;

  /*
  Calculate `hideWithRow` value using `defaultHideWithRow` and `props.hideWithRow`.
  `props.hideWithRow` has a priority, so it will be used if it defined.
  Otherwise `defaultHideWithRow` will be used.
   */
  @:final public function hideWithRow(): Bool {
    var hws = props.hideWithRow;
    return hws == null ? defaultHideWithRow() : hws;
  }

  /*
  Find parent row tag. Used to hide tag with it's parent.
   */
  function getRow(): Null<Tag> return form.config.findFieldRow(this);


  // ------------------------------- Error & event handling methods -------------------------------

  /*
  Проинициализировать и вернуть элемент, задающий "коробку" всего поля.
  Под коробкой показывается сообщение об ошибке.
   */
  public function initBoxTag(): Tag return tag;

  /*
  Создать и вернуть, или просто вернуть элемент, который будет показывать ошибку этого поля.
   */
  public function createErrorTag(): Tag return
    Tag.labelFor(htmlId).cls(form.config.fieldErrorClass).cls(form.config.hiddenClass).addAfter(box);

  public function resetError() {
    emptyError = false;
    error = null;
    updateErrorTag();
    blockClearError();
  }

  function onChange() {
    resetError();
  }

  function onChangeWithEvent(e: Event) {
    onChange();
    dispatchEvent({
      type: ChangeEvent,
      parent: e
    });
  }

  function onFocus() {
    blockClearError();
  }

  public function setError(v: Null<String>) {
    setJsError(v);
    blockSetError();
  }

  /*
  Специальная вариация setError(), вызываемая яваскриптом во время заполнения блока.
  Основная цель - не показывать ошибку под блоком.
   */
  public function setJsError(v: String) {
    error = v;
    updateErrorTag();
  }

  public function setEmptyError() {
    emptyError = true;
    updateErrorTag();
    blockSetError();
  }

  public function updateErrorTag() {
    box.setCls(form.config.fieldBoxErrorClass, error != null || emptyError).setCls(form.config.fieldBoxWithMsgClass, error != null);
    if (error != null) {
      errorTag.setHtml(error).clsOff(form.config.hiddenClass);
      positionErrorTag();
    } else {
      errorTag.cls(form.config.hiddenClass);
    }
  }

  public function positionErrorTag() {
    errorTag.el.style.left = G.toString(box.el.offsetLeft) + 'px';
  }

  inline function tagInputEl(): InputElement return cast tag.el;

  // ------------------------------- Private & protected methods -------------------------------

  private function blockClearError() {
    if (block != null) block.error.clearError(this);
  }

  private function blockSetError() {
    if (block != null) block.error.setError(this);
  }
}

@:build(macros.ExternalFieldsMacro.build())
class FieldProps {
  public var shortId: String;
  public var jsField: String;
  public var field: String;
  public var name: Null<String>;
  public var required: Null<Bool>;
  public var enabled: Null<Bool>;
  public var enterKeySubmit: Null<Bool>;
  public var hideWithRow: Null<Bool>;
}
