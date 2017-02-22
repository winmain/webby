package js.form;

import js.html.XMLHttpRequestResponseType as XMLHttpRequestResponseType;
import goog.events.EventTarget;
import js.form.field.Field;
import js.form.field.FormListField;
import js.form.rule.FormRule;
import js.html.Element;
import js.html.Event;
import js.html.FormElement;
import js.html.XMLHttpRequest;
import js.lib.XhrUtils;

using js.lib.ArrayUtils;

class Form extends EventTarget {
  // ------------------------------- Events -------------------------------

  public static inline var FieldsReadyEvent = 'fields-ready';
  public static inline var FillEvent = 'fill';
  public static inline var AfterFirstFillEvent = 'after-first-fill';
  public static inline var BeforeSubmitEvent = 'before-submit';

  // ------------------------------- Class -------------------------------

  // Массив ошибок самой формы (это ошибки именно формы, а не её полей)
  public var selfErrors(default, null): Array<String> = [];

  public var tag(default, null): Tag;
  public var props(default, null): FormProps;
  public var config(default, null): FormConfig;
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
  public var rules(default, null): Array<FormRule>;

  public var key(default, null): Int;
  public var originalValue(default, null): External;
  public var initialFilled(default, null): Bool;
  public var finished(default, null): Bool;
  public var onPostSuccessFn(default, null): Null<External -> Void>;

  public function new(tag: Tag, props: FormProps, ?subId: Null<Int>, ?parentField: Null<Field>) {
    super();
    this.tag = tag;
    this.props = props;
    this.subId = subId;
    this.parentField = parentField;
    parentForm = parentField != null ? parentField.form : null;
    config = props.config;
    if (config == null) {
      config = if (parentForm != null) parentForm.config else G.or(defaultConfig, function() return new FormConfig());
    }
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
    if (formErrorsTag == null) throw new Error("." + config.formErrorsClass + " not found");
    errorBlock = new FormErrorBlock(config, parentForm != null ? parentForm.errorBlock : null, tag, formErrorsTag);
    if (tag.hasCls(config.formBlockClass)) {
      blocks = [new FormBlock(this, tag)]; // Сама форма является блоком, поэтому блок тут один
    } else {
      blocks = [for (subTag in tag.fnd('.' + config.formBlockClass)) new FormBlock(this, subTag)];
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
        var rule = new FormRule(this, ruleProp);
        rule.addListeners();
        rules.push(rule);
      }
    }

    fill(G.or(props.values, function() return {}));
    dispatchEvent(AfterFirstFillEvent);
    if (props.initialFilled) {
      initialFilled = true;
    }

    if (parentForm == null) {
      // For topmost form only

      if (props.onUnloadConfirm) {
        G.window.addEventListener('beforeunload', beforeUnloadHandler);
      }
      // По-дефолту основная форма скрыта (чтобы не показывать поля до инициализации). Поэтому, здесь мы её и показываем.
      if (!props.hidden) {
        tag.clsOff(config.hiddenClass);
        onFormShown();
        // Вызвать ресайз окна, потому что появление формы скорее всего вызовет появление скролла.
        // Если этого не делать, то возникнет горизонтальный скролл.
        config.onResizeAfterFormShown();
      }

      if (props.submitAfterInit) {
        submit();
      }
    }
  }

  private function beforeUnloadHandler(): String {
    return if (!finished && hasChanges()) config.onUnloadConfirmText; else null;
  }

  public override function disposeInternal() {
    if (props.onUnloadConfirm) G.window.removeEventListener('beforeunload', beforeUnloadHandler);
    deregisterForm(this);
  }

  public function getFormErrorsTag(): Null<Tag> return tag.fnd('.' + config.formErrorsClass);


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

  /*
  Reinitialization one or more field filtered by `propFielterFn`.
   */
  @:keep
  @:expose('Form.reInitFields')
  public function reInitFields(propFilterFn: FieldProps -> Bool) {
    for (fieldProps in props.fields) {
      if (propFilterFn(fieldProps)) {
        initField(fieldProps);
      }
    }
  }

  @:keep
  @:expose('Form.fill')
  public function fill(valueMap: External) {
    for (rule in rules) {
      rule.allowFocusChange(false);
    }
    key = valueMap['_key'];
    for (name in fields.keys()) {
      var field = fields.get(name);
      field.setValue(valueMap[name]);
    }
    dispatchEvent(FillEvent);
    for (rule in rules) {
      rule.allowFocusChange(true);
    }

    originalValue = value();
    initialFilled = false;
    triggerRules();
  }

  /*
  Call `trigger` on all form rules.
   */
  public function triggerRules() {
    for (rule in rules) {
      rule.triggerNoFocus();
    }
  }

  /*
  Показать/скрыть опциональные поля, размещённые в блоке class="optional-fields".
  Ссылка на раскрытие полей должна иметь class="show-optional".
  Актуально только для подформ.
  */
  public function showOptionalFields(show: Bool) {
    var showOptTags = tag.fndAll('.' + config.showOptionalClass);
    var optFieldsTags = tag.fndAll('.' + config.optionalFieldsClass);
    function showTags(tags: Array<Tag>, show: Bool) {
      for (tag in tags) tag.setCls(config.hiddenClass, !show);
    }
    function showFields(): Dynamic {
      showTags(showOptTags, false);
      showTags(optFieldsTags, true);
      return false;
    }
    if (show) showFields();
    else {
      showTags(optFieldsTags, false);
      // TODO: странное решение - навешивать onClick только при show==false, и делать это каждый раз при вызове showOptionalFields
      for (tag in showOptTags) {
        tag.onClick(showFields);
      }
    }
  }

  public function formAction(): String return tag.getAttr('action');

  /*
  Returns top-most form. Can return self for top-most form.
  */
  public function topForm(): Form return G.or(parentForm, function() return this);

  public function fieldPath(subPath: External): External {
    if (parentField != null) {
      var path: External = {};
      path[parentField.name] = subPath;
      return parentForm.fieldPath(path);
    } else {
      return subPath;
    }
  }

  @:keep
  @:expose('Form.submit')
  public function submit() {
    if (finished) return;
    dispatchEvent(BeforeSubmitEvent);
    // TODO: довольно кривая конструкция определения кнопки. Но фокус на кнопку надо ставить, т.к. если он останется на инпуте, то ошибка под этим инпутом сразу же исчезает.
    var buttons: Array<Tag> = config.findSubmitButtons(this);
    // TODO: var buttonLocker = rr.util.ButtonLocker.lock(buttons)
    if (buttons.nonEmpty()) buttons[0].el.focus();
    // TODO: loaderGif = rr.util.LoaderGif.forButton(buttons)

    var postData: External = {'post': isInitialEmpty() ? null : value()};

    var xhr = new XMLHttpRequest();
    XhrUtils.bind(
      xhr,
      function() { // onSuccess
        // --- Успешный submit. Т.е., запрос прошёл, но форма может иметь ошибки. ---
        var result: External = xhr.response;
        //if (untyped handler) handler(xhr.response);
        // TODO: loaderGif.remove()
        // TODO: buttonLocker.unlock() # Разлочить кнопки
        resetErrors();
        if (result['success']) onPostSuccess(result);
        else onPostErrors(result);
      },
      function() { // onFail
        // --- Произошла при отправке запроса ---
        // TODO: loaderGif.remove()
        // TODO: buttonLocker.unlock() # Разлочить кнопки
        config.onErrorSubmit(xhr);
      });
    xhr.open('POST', formAction(), true);
    xhr.responseType = XMLHttpRequestResponseType.JSON;
    xhr.send(JSON.stringify(postData));
  }

  @:keep
  @:expose('Form.value')
  public function value(): External {
    var data: External = {'_key': key};
    for (field_ in fields) {
      var field: Field = field_;
      data.set(field.name, field.value());
    }
    return data;
  }

  public function hasChanges(): Bool return JSON.stringify(originalValue) != JSON.stringify(value());

  /*
  Эта форма изначально пустая? Эта проверка нужна, чтобы определить свежесозданную подформу,
  которую юзер не заполнял. Но она, в то же время, может быть заполнена изначальными значениями, которые не всегда пусты.
  Также, эта проверка хорошо работает для главной формы, чтобы определить, делал ли юзер какие-либо изменения над полями
  (просто вызов метода hasChanges() не работает для формы, которая была заполнена методом fill()).
  */
  public function isInitialEmpty(): Bool return initialFilled && !hasChanges();

  public function resetErrors() {
    for (field_ in fields) {
      var field: Field = field_;
      field.resetError();
    }
    selfErrors = [];
    errorBlock.resetErrors();
  }

  public function setSelfErrors(selfErrors: Array<String>) {
    this.selfErrors = selfErrors;
    errorBlock.setSelfErrors(selfErrors);
  }

  function onPostErrors(post: FormErrors) {
    // Сортировать полученные ошибки по полям, как они заданы в форме
    var errors: JMap<String, String> = G.or(post.errors, function() return JMap.create());
    var required: Array<String> = G.or(post.required, function() return []);
    var subs: JMap<String, Array<FormErrors>> = JMap.create();
    if (post.sub != null) {
      for (sub_ in post.sub) {
        var sub: FormErrors = sub_;
        var name = sub.name;
        if (!subs.contains(name)) subs.set(name, []);
        subs.get(name).push(sub);
      }
      for (subList_ in subs) {
        var subList: Array<FormErrors> = subList_;
        subList.sort(function(a: FormErrors, b: FormErrors) return a.index - b.index);
      }
    }
    for (name in fields.keys()) {
      var field: Field = fields.get(name);
      if (errors.contains(name)) field.setError(errors.get(name));
      if (required.contains(name)) field.setEmptyError();

      var subArray: Array<FormErrors> = subs.get(name);
      if (subArray != null) {
        for (sub in subArray) {
          var formListField: FormListField = cast field;
          formListField.forms[sub.index].onPostErrors(sub);
        }
      }
    }
    setSelfErrors(G.or(post.selfErrors, function() return []));
  }

  function onPostSuccess(post: FormSuccess) {
    finished = !post.clearFinished;
    if (onPostSuccessFn != null) onPostSuccessFn(post);
    else config.onFormSuccess(post);

    // Переинициализировать некоторые поля
    for (field_ in fields) {
      var field: Field = field_;
      if (field.reInitAfterSubmit())
        initField(field.props);
    }
  }

  public function setOnPostSuccess(fn: Null<External -> Void>) {
    onPostSuccessFn = fn;
  }

  /*
  Вызывается сразу после показа формы, независимо от видимости самой формы
   */
  function onFormShown() {
    for (field_ in fields) {
      var field: Field = field_;
      field.onFormShown();
    }
    // Установить фокус на заданное поле, если в форму передан параметр focusField
    if (props.focusField != null) {
      fields.get(props.focusField).focus();
    }
  }

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

  private static var defaultConfig: Null<FormConfig>;

  public static function setDefaultConfig(config: FormConfig) {
    defaultConfig = config;
  }

  /*
  Сопоставление типа поля с его классом
  (функция здесь нужна потому, что google closure не успевает проинициализировать нужные классы на момент создания объекта)
   */
  // TODO: очистить список ниже по мере реализации перечисленных типов полей
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
    registeredForms.pushUnique(form);
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
  // Field definitions
  public var fields: Array<FieldProps>;

  // --- Optional fields ---

  // Form initial values
  public var values: Null<External>;

  // Form config describing css classes, texts, and some external behavoir.
  public var config: Null<FormConfig>;

  // Custom form controller. Calls in very beginning of initialization.
  public var controller: Null<Form -> Dynamic>;

  // Form rules (see FormRule class)
  public var rules: Null<Array<External>>;

  // Add `beforeunload` window listener which prevents closing a tab
  // if the form changed.
  public var onUnloadConfirm: Null<Bool>;

  // Form remains hidden after initialization.
  // In this case `submitAfterInit` and `focusField` properties will be ignored.
  public var hidden: Null<Bool>;

  // TODO:
  public var initialFilled: Null<Bool>;

  // Set focus on this field after form init and shown.
  // Will do nothing if `hidden` flag is set.
  public var focusField: Null<String>;

  // Do form submit after init and shown
  // Will do nothing if `hidden` flag is set.
  public var submitAfterInit: Null<Bool>;
}

/*
Form errors structure
 */
@:build(macros.ExternalFieldsMacro.build())
class FormErrors {
  // Имя подформы (только для ошибок в подформах)
  public var name: Null<String>;

  // Индекс подформы в списке (только для ошибок в подформах)
  public var index: Null<Int>;

  // Ошибки полей этой формы/подформы
  public var errors: Null<JMap<String, String>>;

  // Список пустых полей, обязательных для заполнения
  public var required: Null<Array<String>>;

  // Ошибки самой формы (сюда не входят ошибки полей)
  public var selfErrors: Null<Array<String>>;

  // Ошибки в подформах. Каждая запись в sub должна иметь установленные поля name, index.
  public var sub: Null<Array<FormErrors>>;
}

/*
Form success structure
 */
@:build(macros.ExternalFieldsMacro.build())
class FormSuccess {
  // Always true
  public var success: Bool;

  // Clear `Form.finished` flag on success
  public var clearFinished: Null<Bool>;
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
