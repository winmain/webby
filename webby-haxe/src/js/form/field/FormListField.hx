package js.form.field;

import js.form.field.Field.FieldProps;

class FormListField extends Field {
  static public var REG = 'formList';

  public var forms(default, null): Array<Form> = [];

  public function new(form: Form, props: FormListFieldProps) {
    super(form, props);
    // TODO:
  }

  /*
  Add one or more default subforms.
  This method works only when no subform exists.
  */
  public function addDefaultForm(opt_count: Int = 1) {
    // TODO:
  }

  public function removeForm(form: Form) {
    // TODO:
  }
}
/*
class rr.form.field.FormListField extends rr.form.field.BaseField
  ### @const ###
  addFormEvent: 'add-form'
  ### @const ###
  addRemoveEvent: 'add-remove'

  constructor: (form, props) ->
    super(form, props)
    self = @
    @subProps = props['sub']
    @defaultItems = props['defaultItems'] || 0
    @minItems = props['minItems']
    @maxItems = props['maxItems']
    @uniqueBy = props['uniqueBy']
    @lastSubId = 1
    @forms = []

    @$template = $('#' + @templateId())
    topForm = @form # Вычислить самую верхнюю форму
    topForm = topForm.parentForm while topForm.parentForm
    if !@$template.length then throw "Template for subform '#{@id}' not found"
    @$list = $('#' + @listId(), topForm.$el)
    if !@$list.length then throw "List placeholder for subform '#{@id}' not found"

    @$adder = $('#' + @addId(), topForm.$el).click ->
      self.addForm(true, false)
      false

  # --------- Все id в одном месте ---------

  makeId: (suffix) -> @name + '-' + suffix + (if @form.subId then '-' + @form.subId else '')
  templateId: -> @name + '-template'
  listId: -> @makeId('list')
  addId: -> @makeId('add')
  removeId: (subId)-> 'remove-' + subId

  # --------------- Error handling methods ---------------

  resetError: ->
    super
    form.resetErrors() for form in @forms

  # ----------------------------------------

  setValueEl: (value) ->
    @forms = []
    @$list.empty()
    if value && value.length > 0
      for formValue in value
        form = @addForm(true, true, true)
        form.fill(formValue)
    else
      @addForm(true, false) for idx in [0...@defaultItems]

  value: ->
    ret = []
    for form, idx in @forms
      if idx < @minItems || !form.isInitialEmpty()
        # Условие непустой формы !form.isInitialEmpty() работает только для необязательных подформ (индекс которых больше чем @minItems).
        ret.push(form.value())
    ret

  initEl: -> $('#' + @listId(), @form.$el)

  ###
  Создать новую подформу через клонирование шаблона
  ###
  newFormEl: (subId, vis)->
    self = @
    $form = @$template.clone()
    if vis then $form.removeClass('hide')
    $form[0].id = @id + '-' + subId
    for el in $form.find('*')
      if el.id
        el.id = el.id + '-' + subId
        el.name = el.name + '-' + subId
        $(el).focus(-> rr.form.field.FormListField.superClass_.resetError.apply(self))
      if el.htmlFor
        el.htmlFor = el.htmlFor + '-' + subId
      if el.getAttribute('data-target')
        el.setAttribute('data-target', el.getAttribute('data-target') + '-' + subId)
    $form

  ###
  Создать и добавить подформу.
  Если указан @maxItems и количество подформ превысит это значение, то подформа не добавится.
  @return объект rr.form.Form, если подформа была добавлена, иначе null
  ###
  addForm: (vis, showOptionalFields, ignoreAddCheck) ->
    self = @
    if !ignoreAddCheck && !@canAddForm() then return null
    subId = @lastSubId++
    $el = @newFormEl(subId, vis)
    $el.appendTo(@$list) # appendTo() надо делать ДО создания объекта rr.form.Form (иначе не работает BaseField.findFormBlock)
    form = new rr.form.Form($el, @subProps, subId, @)
    @dispatchEvent({type: @addFormEvent, form: form})
    form.init()
    form.showOptionalFields(showOptionalFields)
    form.triggerRules()

    @getRemoveEl(form).click -> self.removeForm(form)
    @forms.push(form)
    @updateAddRemoveEls()
    form

  ###
  Добавить дефолтную одну или несколько дефолтных подформ.
  Этот метод работает только если нет ни одной подформы.
  ###
  addDefaultForm: (opt_count) ->
    for idx in [0...(opt_count || 1)]
      if @forms.length == 0 then @addForm(true, false)

  removeForm: (form) ->
    if !@canRemoveForm() then return false # Нельзя удалять элементы, если их количество меньше @minItems
    for curItem, i in @forms
      if form == curItem
        @forms.splice(i, 1)
        form.$el.remove()
        @updateAddRemoveEls()
        return false
    throw "Cannot remove subform"

  canAddForm: -> !@maxItems || @forms.length < @maxItems
  canRemoveForm: -> !@minItems || @forms.length > @minItems

  updateAddRemoveEls: ->
    @$adder.toggle(@canAddForm())
    canRemove = @canRemoveForm()
    @getRemoveEl(form).toggle(canRemove) for form in @forms
    @dispatchEvent({type: @addRemoveEvent})

  ###
  Вернуть элемент удаления для подформы
  ###
  getRemoveEl: (form) -> $('#' + @removeId(form.subId), form.$el)

*/

@:build(macros.ExternalFieldsMacro.build())
class FormListFieldProps extends FieldProps {
  // TODO:
}
