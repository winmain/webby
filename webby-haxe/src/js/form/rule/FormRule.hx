package js.form.rule;

class FormRule {
  public var form(default, null): Form;
  public var cond(default, null): FormCondition;
  public var actions(default, null): Array<FormAction>;
  public var allowFocus(default, null): Bool = false;

  public function new(form: Form, props: FormRuleProps) {
    this.form = form;
    cond = FormCondition.createCond(props.cond);
    actions = [for (actionProps in props.actions) FormAction.createAction(actionProps)];
  }

  // TODO: добавить методы

  // ------------------------------- Internal static methods -------------------------------

  @:allow(js.form.rule.FormCondition.regCond)
  @:allow(js.form.rule.FormAction.regAction)
  static function internalReg<T>(typeName: String, registry: JMap<String, Class<T>>, cls: Class<T>) {
    var name = untyped cls.REG;
    if (name == null) throw new Error('Form ${typeName} with empty REG: ${cls}');
    var oldReg = registry.get(name);
    if (oldReg != null) {
      if (oldReg == cls) return; // this class is already registered
      throw new Error('Collision ${typeName} registry name "${name}"');
    }
    registry.set(name, cls);
  }

  @:allow(js.form.rule.FormCondition.createCond)
  @:allow(js.form.rule.FormAction.createAction)
  static function internalCreate<T>(typeName: String, registry: JMap<String, Class<T>>, props: External): T {
    var cls = registry.get(props.get('cls'));
    if (cls == null) throw new Error('Form ${typeName} class "${props.get('cls')}" not found');
    return Type.createInstance(cls, [props]);
  }
}

/*
class rr.form.rule.Rule
  constructor: (@form, props) ->
    @cond = rr.form.rule.Condition.createCond(props['cond'])
    @actions = (rr.form.rule.Action.createAction(action) for action in props['actions'])
    @allowFocus = false

  addListeners: ->
    self = @
    for fieldName in @cond.getFields()
      field = @form.fields[fieldName]
      field.listen(field.changeEvent, -> self.trigger())

  ###
    Перепроверить условие и выполнить все действия. Вызывается из Condition
  ###
  trigger: ->
    turnOn = @cond.check(@form)
    action.execute(@form, turnOn, @allowFocus) for action in @actions

  triggerNoFocus: ->
    @allowFocus = false
    @trigger()
    @allowFocus = true

  allowFocusChange: (@allowFocus) ->

 */

@:build(macros.ExternalFieldsMacro.build())
class FormRuleProps {
  public var cond: External;
  public var actions: Array<External>;
}
