package js.form;

import goog.array.GoogArray;
import goog.events.EventTarget;
import js.form.field.Field;
import js.html.Element;
import js.html.Event;
import js.html.FormElement;
import js.lib.ArrayUtils;

class Form extends EventTarget {
  // ------------------------------- Events -------------------------------

  public static inline var FieldsReadyEvent = 'fields-ready';
  public static inline var FillEvent = 'fill';
  public static inline var AfterFirstFillEvent = 'after-first-fill';
  public static inline var BeforeSubmitEvent = 'before-submit';

  // ------------------------------- Class -------------------------------

  // Массив ошибок самой формы (это ошибки именно формы, а не её полей)
  public var selfErrors(default, null) = [];

  public var tag(default, null): Tag;
  public var props(default, null): FormProps;
  public var style(default, null): FormStyle;
  public var subId(default, null): Null<Int>;
  public var parentField(default, null): Null<Field>;
  public var parentForm(default, null): Null<Form>;

  public var formEl(get, never): FormElement;

  inline private function get_formEl(): FormElement return cast tag.el;

  public var controller(default, null): Null<Dynamic>;
  public var formErrorsTag(default, null): Null<Tag>;
  public var errorBlock(default, null): FormErrorBlock;

  public var blocks(default, null): Array<FormBlock>;
  public var fields(default, null): JMap<String, Field>;
  public var rules(default, null): Array<String>;

  public function new(tag: Tag, props: FormProps, ?subId: Null<Int>, ?parentField: Null<Field>) {
    super();
    this.tag = tag;
    this.props = props;
    style = G.or(props.style, function() return new FormStyle());
    this.subId = subId;
    this.parentField = parentField;
    parentForm = parentField != null ? parentField.form : null;
  }

  @:keep
  @:expose('Form.create')
  static public function create(tag: Tag, props: FormProps): Form return new Form(tag, props);

  @:keep
  @:expose('Form.init')
  public function init() {
    registerForm(this);
    if (formEl.method != 'get') { // Для method="get" форма не работает через js - делаем простой сабмит (чтобы можно было делать простые формы)
      tag.on('submit', function(event: Event) {
        event.preventDefault();
        submit();
      });
    }

    controller = props.controller != null ? props.controller(this) : null;

    formErrorsTag = getFormErrorsTag();
    if (formErrorsTag == null) throw new Error("." + style.formErrorsClass + " not found");
    errorBlock = new FormErrorBlock(parentForm != null ? parentForm.errorBlock : null, tag, formErrorsTag);
    if (tag.hasCls(style.formBlockClass)) {
      blocks = [new FormBlock(this, tag)]; // Сама форма является блоком, поэтому блок тут один
    } else {
      blocks = [for (subTag in tag.fnd('.' + style.formBlockClass)) new FormBlock(this, subTag)];
    }

    // this['fields'] нужен для доступа к полям формы для внешних скриптов
    (this:External).set('fields', fields = JMap.create());
    for (fieldProps in props.fields) {
      initField(fieldProps);
    }
    dispatchEvent(FieldsReadyEvent);

    rules = [];
    if (props.rules != null) {
      for (ruleProp in props.rules) {
//        var rule = new Rule
      }
    }
//    for ruleProp in (@props['rules'] || [])
//      rule = new rr.form.rule.Rule(@, ruleProp)
//      rule.addListeners()
//      @rules.push(rule)
//
//    @fill(@props['values'] || {})
//    @dispatchEvent({type: @afterFirstFillEvent})
//    if @props['initialFilled']
//      @initialFilled = true
//
//    if !@parentForm
//      if @props['onUnloadConfirm']
//        $(window).on 'beforeunload', ->
//          if !self.finished && self.hasChanges()
//            'У вас есть несохранённые изменения'
//      # По-дефолту основная форма скрыта (чтобы не показывать поля до инициализации). Поэтому, здесь мы её и показываем.
//      if !@props['hidden']
//        @$el.show()
//        @onFormShown()
//        # Вызвать ресайз окна, потому что появление формы скорее всего вызовет появление скролла.
//        # Если этого не делать, то возникнет горизонтальный скролл.
//        rr.windowSize.onResize()
//
//      if @props['submitAfterInit']
//        @submit()
  }

  public override function disposeInternal() {
    deregisterForm(this);
  }

  public function getFormErrorsTag(): Null<Tag> return tag.fnd('.' + style.formErrorsClass);


  /*
  Инициализация одного поля и добавление/обновление его в словаре this.fields.
  fieldClasses - значение из Form.fieldClasses()
  fieldProps - свойства поля из props.fields
   */
  private function initField(fieldProps: FieldProps) {
    var fieldReg = fieldRegistry.get(fieldProps.jsField);
    if (fieldReg == null) throw new Error('Field "${fieldProps.jsField}" not registered');

    var field: Field = fieldReg.constructor(this, fieldProps);
    field.init(fieldProps);
    fields.set(field.name, field);
  }

//  ###
//  Переинициализация одного или нескольких полей по критериям фильтра.
//  propFilterFn на вход получает fieldProps, на выходе должен вернуть boolean - запускать или нет
//  переинициализацию поля.
//
//  @export
//  ###
//  reInitFields: (propFilterFn) ->
//    fieldClasses = rr.form.Form.fieldClasses()
//    for fieldProps in @props['fields']
//      if propFilterFn(fieldProps)
//        @initField(fieldClasses, fieldProps)
//
//  ### @export ###
//  fill: (valueMap) ->
//    rule.allowFocusChange(false) for rule in @rules
//    @key = valueMap['_key']
//    for name, field of @fields
//      field.setValue(valueMap[name])
//    @dispatchEvent({type: @fillEvent})
//    rule2.allowFocusChange(true) for rule2 in @rules
//    @originalValue = @value()
//    @initialFilled = false
//    @triggerRules()
//
//  ###
//  Вызвать trigger у всех правил формы.
//  ###
//  triggerRules: ->
//    for rule in @rules
//      rule.triggerNoFocus()
//
//  ###
//  Показать/скрыть опциональные поля, размещённые в блоке class="optional-fields".
//  Ссылка на раскрытие полей должна иметь class="show-optional".
//  Актуально только для подформ.
//  ###
//  showOptionalFields: (show) ->
//    $showOpt = @$el.find('.show-optional')
//    $optFields = @$el.find('.optional-fields')
//    showFields = ->
//      $showOpt.hide()
//      $optFields.show()
//      false
//    if show then showFields()
//    else
//      $optFields.hide()
//      $showOpt.click(showFields)
//
//  formAction: -> @$el.attr('action')
//
//  ###
//  Вернуть форму наивысшей иерархии
//  ###
//  topForm: -> @parentForm or @
//
//  fieldPath: (subPath) ->
//    if @parentField
//      path = {}
//      path[@parentField.name] = subPath
//      @parentForm.fieldPath(path)
//    else
//      subPath

  @:keep
  @:expose('Form.submit')
  public function submit() {
//  ### @export ###
//  submit: ->
//    self = @
//    if @finished then return
//    @dispatchEvent({type: @beforeSubmitEvent})
//    # TODO: довольно кривая конструкция определения кнопки. Но фокус на кнопку надо ставить, т.к. если он останется на инпуте, то ошибка под этим инпутом сразу же исчезает.
//    buttons = @$el.find('button[type=submit]')
//    buttonLocker = rr.util.ButtonLocker.lock(buttons)
//    buttons.focus()
//    loaderGif = rr.util.LoaderGif.forButton(buttons)
//    $.jsonPost(@formAction(), {'post': if @isInitialEmpty() then null else @value()}, ((result) ->
//      # --- Успешный submit. Т.е., запрос прошёл, но форма может иметь ошибки. ---
//      loaderGif.remove()
//      buttonLocker.unlock() # Разлочить кнопки
//      self.resetErrors()
//      if result['success'] then self.onPostSuccess(result)
//      else self.onPostErrors(result)
//    ), ((xhr, text, err) ->
//      # --- Произошла при отправке запроса ---
//      loaderGif.remove()
//      buttonLocker.unlock() # Разлочить кнопки
//      if xhr.status == 500
//        rr.window.Message.show({cls: 'error-message', text: 'Внутренняя ошибка сервера. Не волнуйтесь, мы уже работаем над этим.'})
//      else
//        footer = $(rr.window.Message.makeFooter('Повторить', 'btn-orange'))
//        footer.find('button').click(-> self.submit())
//        rr.window.Message.show(
//          cls: 'error-message'
//          text: 'Произошла ошибка соединения с сервером.<p class="tech">' + xhr.status + ': ' + err + '</p>'
//          footer: footer
//        )
//    ))
  }

//  ### @export ###
//  value: ->
//    data = {'_key': @key}
//    for _, field of @fields
//      data[field.name] = field.value()
//    data
//
//  hasChanges: -> JSON.stringify(@originalValue) != JSON.stringify(@value())
//
//  ###
//  Эта форма изначально пустая? Эта проверка нужна, чтобы определить свежесозданную подформу,
//  которую юзер не заполнял. Но она, в то же время, может быть заполнена изначальными значениями, которые не всегда пусты.
//  Также, эта проверка хорошо работает для главной формы, чтобы определить, делал ли юзер какие-либо изменения над полями
//  (просто вызов метода hasChanges() не работает для формы, которая была заполнена методом fill()).
//  ###
//  isInitialEmpty: -> @initialFilled && !@hasChanges()
//
//  resetErrors: ->
//    for _, field of @fields
//      field.resetError()
//    @selfErrors = []
//    @errorBlock.resetErrors()
//
//  setSelfErrors: (selfErrors) ->
//    @selfErrors = selfErrors
//    @errorBlock.setSelfErrors(selfErrors)
//
//  onPostErrors: (post) ->
//    # Сортировать полученные ошибки по полям, как они заданы в форме
//    errors = post['errors'] || {}
//    required = post['required'] || []
//    subs = {}
//    if post['sub']
//      for sub in post['sub']
//        name = sub['name']
//        if name not of subs then subs[name] = []
//        subs[name].push(sub)
//      for _, subList of subs
//        subList.sort((a, b) -> a.index - b.index)
//    for name, field of @fields
//      if name of errors then field.setError(errors[name])
//      if name in required then field.setEmptyError()
//      if name of subs
//        for sub in subs[name]
//          field.forms[sub['index']].onPostErrors(sub)
//    @setSelfErrors(post['selfErrors'] || [])
//
//  onPostSuccess: (post) ->
//    @finished = !(post['clearFinished'] || (post['message'] && post['message']['clearFinished']))
//    if @onPostSuccessFn then @onPostSuccessFn(post)
//    else rr.util.Actions.onResult(post, [])
//
//    # Переинициализировать некоторые поля
//    fieldClasses = rr.form.Form.fieldClasses()
//    for _, field of @fields
//      if field.reInitAfterSubmit()
//        @initField(fieldClasses, field.props)
//
//
//  setOnPostSuccess: (fn) -> @onPostSuccessFn = fn
//
//  # Вызывается сразу после показа формы, независимо от видимости самой формы
//  onFormShown: ->
//    for _, field of @fields
//      field.onFormShown()
//
//    # Установить фокус на заданное поле, если в форму передан параметр focusField
//    if @props['focusField'] then @fields[@props['focusField']].focus()
//

  // ------------------------------- Static methods -------------------------------

  private static var fieldRegistry: JMap<String, FormRegField> = JMap.create();

  public static function regField(cls: Class<Field>, ?name: String, ?constructor: Null<Form -> FieldProps -> Field>) {
    if (name == null) {
      name = untyped cls.REG;
    }
    if (name == null) throw new Error('Field class with empty REG: ${cls}');
    var oldReg = fieldRegistry.get(name);
    if (oldReg != null) {
      if (oldReg.cls == cls) return; // this field is already registered
      throw new Error('Collision field registry name "${name}"');
    }
    if (constructor == null) {
      constructor = function(a, b) return Type.createInstance(cls, [a, b]);
    }
    fieldRegistry.set(name, new FormRegField(cls, constructor));
  }

  /*
  Сопоставление типа поля с его классом
  (функция здесь нужна потому, что google closure не успевает проинициализировать нужные классы на момент создания объекта)
   */
//  'check': rr.form.field.CheckField
//  'date': rr.form.field.DateField
//  'monthYear': rr.form.field.MonthYearField
//  'radioGroup': rr.form.field.RadioGroupField
//  'pager': rr.form.field.PagerField
//  'select': rr.form.field.SelectField
//  'checkList': rr.form.field.CheckListField
//  'autocomplete': rr.form.field.AutocompleteField
//  'autocompleteList': rr.form.field.AutocompleteListField
//  'autocompleteText': rr.form.field.AutocompleteTextField
//  'phone': rr.form.field.PhoneField
//  'masked': rr.form.field.MaskedField
//  'fio': rr.form.field.FioField
//  'upload': rr.form.field.UploadField
//  'hidden': rr.form.field.BaseField
//  'reCaptcha': rr.form.field.ReCaptchaField
//

  private static var registeredForms: Array<Form> = [];

  private static function registerForm(form: Form) {
    ArrayUtils.pushUnique(registeredForms, form);
  }

  private static function deregisterForm(form: Form) {
    registeredForms.remove(form);
  }

  /*
  Получить зарегистрированную форму по тегу
   */
  public static function fromEl(el: Element): Null<Form> {
    for (form in registeredForms) {
      if (form.formEl == el) return form;
    }
    return null;
  }

  public inline static function fromTag(tag: Tag): Null<Form> return fromEl(tag.el);
}

/*
Свойства формы
 */
@:build(macros.ExternalFieldsMacro.build())
class FormProps {
  public var style: Null<FormStyle>;
  public var controller: Null<Form -> Dynamic>;
  public var fields: Array<FieldProps>;
  public var rules: Array<External>;
//  public var values;
//  public var initialFilled;
//  public var onUnloadConfirm;
//  public var hidden;
//  public var submitAfterInit;
}


/*
Регистрация типа поля в форме
 */
class FormRegField {
  public var cls(default, null): Class<Field>;
  public var constructor(default, null): Form -> FieldProps -> Field;

  public function new(cls: Class<Field>, constructor: Form -> FieldProps -> Field) {
    this.cls = cls;
    this.constructor = constructor;
  }
}


/*
Блок внутри формы (пример: section.cls("form-block")).
Этот блок связан с полями, которые вложены в него. Также, он имеет элемент вывода ошибки.
 */
class FormBlock {
  public var form(default, null): Form;
  public var tag(default, null): Tag;
  public var error(default, null): FormErrorBlock;

  public function new(form: Form, tag: Tag) {
    this.form = form;
    this.tag = tag;
//    @$el.data('block', @)
    error = new FormErrorBlock(form.errorBlock, tag, createErrorTag());
    error.resetErrors();
  }

  /*
  Создать и вернуть, или просто вернуть элемент, который будет показывать ошибку этого поля.
   */
  function createErrorTag(): Tag return Tag.label.cls(form.style.blockErrorClass).hide().addTo(tag);
}

/*
Блок с ошибками как для блока формы .form-block, так и для самой формы .form-errors
 */
class FormErrorBlock {
  private var parent(default, null): Null<FormErrorBlock>;
  private var parentTag(default, null): Tag;
  private var errorTag(default, null): Tag;

  private var errors(default, null): Array<Dynamic> = []; // Can be: Field, FormErrorBlock
  private var selfErrors(default, null): Array<String> = [];
  private var target(default, null): Null<Field> = null;

  private inline static var HoverClass = 'hover'; // TODO: вынести в FormStyle

  public function new(parent: Null<FormErrorBlock>, parentTag: Tag, errorTag: Tag) {
    this.parent = parent;
    this.parentTag = parentTag;
    this.errorTag = errorTag;
    // Это действие нужно, чтобы срабатывал "фокус" на поля без инпутов (пример: RadioGroupField)

    errorTag
    .on('mouseover', function() {if (target != null) target.box.cls(HoverClass); })
    .on('mouseout', function() {if (target != null) target.box.clsOff(HoverClass); })
    .onClick(function() {
      if (target != null) {
        target.box.clsOff(HoverClass);
        target.focus();
      }
    });
//    .click(->
//      if self.target
//        self.target.$box.removeClass('hover')
//        self.target.focus()
//    )
//    @target = null
  }

  public function resetErrors() {
    errors = [];
    selfErrors = [];
    errorTag.setHtml('');
    updateErrorTag();
    if (parent != null) parent.clearError(this);
  }

  public function setError(item: Dynamic) {
    if (!GoogArray.contains(errors, item)) {
      errors.push(item);
      updateErrorTag();
      if (parent != null) parent.setError(this);
    }
  }

  public function clearError(item: Dynamic) {
    if (GoogArray.remove(errors, item)) {
      if (ArrayUtils.isEmpty(errors) && parent != null) {
        parent.clearError(item);
      }
      updateErrorTag();
    }
  }

  public function setSelfErrors(v: Array<String>) {
    selfErrors = v;
    updateErrorTag();
  }

  public function updateErrorTag() {
//    hasErrors = @errors.length > 0
//    if @selfErrors.length > 0
//      text = @selfErrors.join('<br>')
//      if @$error.length
//        if @$error.html() != text then @$error.html(text).css('display', 'block').delay(3000).fadeOut(1000)
//      else # В некоторых формах бывает так, что нет блока с ошибками, а саму ошибку показать надо.
//        rr.window.Message.show({cls: 'error-message', text: text})
//    else
//      if hasErrors then @$error.html('Некоторые поля заполнены неправильно')
//      @$error.css('display', if hasErrors then 'block' else 'none')
//    if hasErrors then @target = @getFirstError()
//    @$parent.toggleClass('with-error', hasErrors)
//    @parent?.updateErrorEl()
  }

//  getFirstError: ->
//    err = @errors[0]
//    if err instanceof rr.form.FormErrorBlock
//      err.getFirstError()
//    else if err instanceof rr.form.field.FormListField # Если ошибка на самой подформе, то выбрать первое поле в ней
//      for _, first of err.forms[0].fields  # TODO: здесь может быть так, что нет ни одной подформы, и будет ошибка
//        return first
//    else
//      err
}
