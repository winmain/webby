package js.form.field;

import goog.events.EventTarget;
import js.form.Form;

class Field extends EventTarget {

  public var form(default, null): Form;
  private var props: FieldProps;

  public var error(default, null) = null;
  public var emptyError(default, null) = false;
  public var vis(default, null) = true;

  public var field(default, null): String;
  public var name(default, null): String;
  public var id(default, null): String;
  public var required(default, null): Bool;

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
//    @$el = @initEl().prop('field', @)
//    @$box = @initBoxEl()
//    @$box.addClass('field')
//    @updateRequired()
//    @$error = @createErrorEl()
//    @initElEvents()
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
//  setValue: (value) ->
//    oldValue = @value()
//    @setValueEl(value)
//    # for test only # console.log(@name + '.setValue: ' + value)
//    @dispatchEvent({type: @setValueEvent, value: value})
//    if JSON.stringify(@value()) != JSON.stringify(oldValue)
//      @onChange()
//      @dispatchEvent({type: @changeEvent, setValue: true})
//
//  setValueEl: (value) ->
//    @$el.val(value || null)
//
//  value: -> @$el.val()
//
//  isEmpty: -> !@value()
//
//  updateRequired: -> @$box.toggleClass('required', !!@required)
//
//  initEl: -> $('#' + @id, @form.$el)
//
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

//  show: (vis, withParent) ->
//    @vis = !!vis
//    (if withParent then @$box.parent() else @$box).toggle(!!vis)
//
//  # Вызывается сразу после показа формы, независимо от видимости самого элемента
//  onFormShown: -> @updateErrorEl()
//
  public function enable(en: Bool) {
    tag.attr('disabled', !en);
  }

//  initElEvents: ->
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
//
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
//  ###
//  Это поле нужно реинициализировать каждый раз после сабмита формы?
//  (этот метод перегружается в наследниках)
//  ###
//  reInitAfterSubmit: -> false
//
//  # --------------- Error & event handling methods ---------------
//
//  ###
//  Вернуть элемент, задающий "коробку" всего поля.
//  Под коробкой показывается сообщение об ошибке.
//  ###
//  initBoxEl: -> @$el
//
//  ###
//  Создать и вернуть, или просто вернуть элемент, который будет показывать ошибку этого поля.
//  ###
//  createErrorEl: ->
//    $('<label class="field-error"/>').prop('for', @id).hide().insertAfter(@$box)
//
//  resetError: ->
//    @emptyError = false
//    @error = null
//    @updateErrorEl()
//    @block?.error.clearError(@)
//
//  onChange: ->
//    @resetError()
//
//  onFocus: ->
//    @block?.error.clearError(@)
//
//  setError: (@error) ->
//    @updateErrorEl()
//    @block?.error.setError(@)
//
//  ###
//  Специальная вариация setError(), вызываемая яваскриптом во время заполнения блока.
//  Основная цель - не показывать ошибку под блоком.
//  ###
//  setJsError: (@error) ->
//    @updateErrorEl()
//
//  setEmptyError: ->
//    @emptyError = true
//    @updateErrorEl()
//    @block?.error.setError(@)
//
//  updateErrorEl: ->
//    @$box.toggleClass('error', !!(@error || @emptyError)).toggleClass('with-msg', !!@error)
//    if @error
//      @$error.html(@error).show()
//      @positionErrorEl()
//    else
//      @$error.hide()
//
//  positionErrorEl: ->
//    @$error.css('left', @$box[0].offsetLeft)
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
