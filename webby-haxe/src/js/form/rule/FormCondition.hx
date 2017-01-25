package js.form.rule;

import js.form.field.Field;

/*
Form condition base class
 */
@:autoBuild(macros.KeepConstructorMacro.build())
class FormCondition {
  public function getFields(): Array<Field> return [];

  public function check(form: Form): Bool return null;

  // ------------------------------- Static methods -------------------------------

  private static var registry: JMap<String, Class<FormCondition>> = JMap.create();

  public static function regCond(cls: Class<FormCondition>) {
    FormRule.internalReg('condition', registry, cls);
  }

  /*
  Register all common conditions
   */
  public static function regCommonConditions(): Void {
    // TODO:
  }

  /*
  Create new condition for specified properties.
   */
  public static function createCond(props: External): FormCondition return FormRule.internalCreate('condition', registry, props);
}

// TODO:

/*
###
  Условие not
###
class rr.form.rule.Not extends rr.form.rule.Condition
  constructor: (props) ->
    @cond = rr.form.rule.Condition.createCond(props['cond'])

  getFields: -> @cond.getFields()
  check: (form) -> !@cond.check(form)


###
  Условие and
###
class rr.form.rule.And extends rr.form.rule.Condition
  constructor: (props) ->
    @c1 = rr.form.rule.Condition.createCond(props['c1'])
    @c2 = rr.form.rule.Condition.createCond(props['c2'])

  getFields: -> @c1.getFields().concat(@c2.getFields())
  check: (form) -> @c1.check(form) && @c2.check(form)


###
Условие or
###
class rr.form.rule.Or extends rr.form.rule.Condition
  constructor: (props) ->
    @c1 = rr.form.rule.Condition.createCond(props['c1'])
    @c2 = rr.form.rule.Condition.createCond(props['c2'])

  getFields: -> @c1.getFields().concat(@c2.getFields())
  check: (form) -> @c1.check(form) || @c2.check(form)


###
  Условие выполняется, если указанное поле имеет заданное значение
###
class rr.form.rule.FieldEquals extends rr.form.rule.Condition
  constructor: (props) ->
    @fieldName = props['field']
    @value = props['value']

  getFields: -> [@fieldName]
  check: (form) -> form.fields[@fieldName].value() == @value


###
  Условие выполняется, если указанное поле имеет одно из заданных значений
###
class rr.form.rule.FieldIn extends rr.form.rule.Condition
  constructor: (props) ->
    @fieldName = props['field']
    @values = props['values']

  getFields: -> [@fieldName]
  check: (form) ->
    value = form.fields[@fieldName].value()
    for v in @values
      if v == value then return true
    false


###
  Условие выполняется, если указанное поле пусто (не забыть проверить работоспособность метода isEmpty у поля)
###
class rr.form.rule.FieldEmpty extends rr.form.rule.Condition
  constructor: (props) ->
    @fieldName = props['field']

  getFields: -> [@fieldName]
  check: (form) -> form.fields[@fieldName].isEmpty()


###
  Условие выполняется, если указанное поле целиком совпадает с регуляркой
###
class rr.form.rule.FieldRegex extends rr.form.rule.Condition
  constructor: (props) ->
    @fieldName = props['field']
    @regex = new RegExp('^' + props['regex'] + '$')

  getFields: -> [@fieldName]
  check: (form) -> @regex.test(form.fields[@fieldName].value())


###
  Создать новое условие по заданным параметрам
###
rr.form.rule.Condition.createCond = (props) ->
  classes = rr.form.rule.Condition.classes()
  cls = classes[props['cls']]
  if !cls then throw "Form condition class '" + props['cls'] + "' not found"
  new cls(props)

###
  Сопоставление класса условия
  (функция здесь нужна потому, что google closure не успевает проинициализировать нужные классы на момент создания объекта)
###
rr.form.rule.Condition.classes = ->
  'not': rr.form.rule.Not
  'and': rr.form.rule.And
  'or': rr.form.rule.Or
  'fieldEquals': rr.form.rule.FieldEquals
  'fieldIn': rr.form.rule.FieldIn
  'fieldEmpty': rr.form.rule.FieldEmpty
  'fieldRegex': rr.form.rule.FieldRegex
 */