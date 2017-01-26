package js.form.field;

import goog.events.EventTarget;
import js.form.Form;

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
  public var name(default, null): String;
  public var id(default, null): String;
  public var required: Bool;

  public var tag(default, null): Tag;
  public var box(default, null): Tag;
  public var errorTag(default, null): Tag;
  public var block(default, null): Null<FormBlock>;

  public function new(form: Form, props: FieldProps) {
    super();
    this.form = form;
    this.props = props;
    field = props.field;
    // TODO: не очень хорошая логика с name и id. Надо бы сделать более простую/прямолинейную логику.
    name = G.or(props.name, function() return props.id);
    id = form.subId != null ? '${props.id}-${form.subId}' : props.id;
    required = props.required || false;
    tag = getTag(); // TODO: неплохо бы добавить свойство элемента 'field', которое будет ссылаться на this
    if (tag == null) throw new Error('Field node #${id} not found');
//    @$el = @initEl().prop('field', @)
    box = getBoxTag().cls(form.config.fieldBoxClass);
    updateRequired();
    errorTag = createErrorTag();
    initElEvents();
//    @block = @findFormBlock()
//    if !props['enterKeySubmit'] && @suppressEnterKey()
//      @$el.keypress (e) ->
//        if e.which == 13
//          e.preventDefault()
//          return false
  }

  // TODO: это пока что рыба

  /*
  Дополнительная инициализация, которая не может работать в конструкторе из-за неполностью
  проинициализированных переменных.
   */
  public function init(props: FieldProps) {
    enable(props.enabled == null ? true : props.enabled);
  }

//  ###
//  Установить значение полю. Если нужно прописать другой код для установки этого значения элементу, то следует переопределить setValueEl.
//  Если же требуется предварительная конвертация значения перед установкой, тогда этот метод можно переопределить (например, конвертировать дату в текст).
//  @export
//  ###
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
    tag.setVal(value);
  }

  public function value(): Dynamic return tag.val();

  public function isEmpty(): Bool return !value();

  public function updateRequired() {
    box.setCls(form.config.fieldBoxRequiredClass, required);
  }

  public function getTag(): Tag return form.tag.fnd('#' + id);

//  # Найти и вернуть rr.form.FormBlock, в который вложено это поле; либо null, если такого не существует.
//  findFormBlock: ->
//    blockEl = @$el.parents('.form-block')[0]
//    if blockEl then $(blockEl).data('block') else null
//
  /*
  Перейти к этому полю (например, при клике на бирку блока/формы с ошибкой)
   */
  public function focus() {
    tag.el.focus();
  }

  public function show(v: Bool, withParent: Bool = false) {
    vis = v;
    var t: Tag = withParent ? box.parent() : box;
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
//    self = @
//    @$el.bind('change keyup', (e) ->
//      if e.type == 'keyup' && ((e.which == 13 && !/[\n\r]/.test(self.$el.val())) || (e.which >= 33 && e.which <= 40))
//        # Проверка для случая, если был нажат enter, который привёл к сабмиту.
//        # self.onChange() вызывать не следует, т.к. это приведёт к сбросу ошибки в этом поле, хотя само значение на самом деле не менялось.
//        # Также, нажатие на стрелки и клавишы навигации не должно приводить к self.onChange()
//        return
//      self.onChange()
//      self.dispatchEvent({type: self.changeEvent, parent: e})
//    )
//    .bind('focus', -> self.onFocus())
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
//
//  ###
//  Подавлять нажатие enter'а в этом поле, чтобы форма не делала submit?
//  ###
//  suppressEnterKey: -> @$el.length && @$el[0].tagName == 'INPUT'
//
  /*
  Do this field needed to be reinitialized every time after form submit?
  (this method can be overloaded in descendants)
   */
  public function reInitAfterSubmit(): Bool return false;


  // ------------------------------- Error & event handling methods -------------------------------

  /*
  Вернуть элемент, задающий "коробку" всего поля.
  Под коробкой показывается сообщение об ошибке.
   */
  public function getBoxTag(): Tag return tag;

  /*
  Создать и вернуть, или просто вернуть элемент, который будет показывать ошибку этого поля.
   */
  public function createErrorTag(): Tag return
    Tag.labelFor(id).cls(form.config.fieldErrorClass).cls(form.config.hiddenClass).addAfter(box);

  public function resetError() {
    emptyError = false;
    error = null;
    updateErrorTag();
    if (block != null) block.error.clearError(this);
  }

  public function onChange() {
    resetError();
  }

//  onFocus: ->
//    @block?.error.clearError(@)

  public function setError(v: Null<String>) {
    error = v;
    updateErrorTag();
    if (block != null) block.error.setError(this);
  }

//  ###
//  Специальная вариация setError(), вызываемая яваскриптом во время заполнения блока.
//  Основная цель - не показывать ошибку под блоком.
//  ###
//  setJsError: (@error) ->
//    @updateErrorEl()

  public function setEmptyError() {
    emptyError = true;
    updateErrorTag();
    if (block != null) block.error.setError(this);
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
    errorTag.el.style.left = G.toString(box.el.offsetLeft);
  }
}

@:build(macros.ExternalFieldsMacro.build())
class FieldProps {
  public var jsField: String;
  public var field: String;
  public var name: Null<String>;
  public var id: String;
  public var required: Null<Bool>;
  public var enabled: Null<Bool>;
  public var enterKeySubmit: Null<Bool>;
}
