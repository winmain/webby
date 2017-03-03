package js.form.rule;

import js.form.field.Field;
import js.form.field.FormListField;


@:autoBuild(macros.KeepConstructorMacro.build())
class FormAction {

  public function execute(form: Form, turnOn: Bool, allowFocus: Bool): Void {
    throw new Error("Not implemented");
  }

  // ------------------------------- Static methods -------------------------------

  private static var registry: JMap<String, Class<FormAction>> = JMap.create();

  public static function regAction(cls: Class<FormAction>) {
    FormRule.internalReg('action', registry, cls);
  }

  /*
  Register all common actions
   */
  public static function regCommonActions(): Void {
    regAction(Visible);
    regAction(Enable);
    regAction(Require);
    regAction(SetValue);
    regAction(SetValue2);
    regAction(AddSubform);
  }

  /*
  Create new action for specified properties.
   */
  public static function createAction(props: External): FormAction return FormRule.internalCreate('action', registry, props);
}


/*
Base class for field actions.
 */
class FieldAction extends FormAction {
  public var fieldName(default, null): String;
  public var focus(default, null): Bool;

  public function new(props: External) {
    fieldName = props.get('field');
    focus = G.toBool(props.get('focus'));
  }

  override public function execute(form: Form, turnOn: Bool, allowFocus: Bool): Void {
    var field = form.fields.get(fieldName);
    var result = execField(field, turnOn);
    if (allowFocus && result && focus) {
      field.focus();
    }
  }

  // Returns true if field is enabled and can receive focus.
  // Can be overriden in descendants.
  public function execField(field: Field, turnOn: Bool): Bool return false;
}


/*
Show/hide field action.
 */
class Visible extends FieldAction {
  static public var REG = 'visible';

  public var vis(default, null): Bool;

  public function new(props: External) {
    super(props);
    vis = G.toBool(props.get('vis'));
  }

  override public function execField(field: Field, turnOn: Bool): Bool {
    var v: Bool = untyped vis ^ (!turnOn);
    field.show(v);
    return v;
  }
}


/*
Enable/disable field action.
 */
class Enable extends FieldAction {
  static public var REG = 'enable';

  public var enable(default, null): Bool;

  public function new(props: External) {
    super(props);
    enable = G.toBool(props.get('enable'));
  }

  override public function execField(field: Field, turnOn: Bool): Bool {
    var en: Bool = untyped enable ^ (!turnOn);
    field.enable(en);
    return en;
  }
}


/*
Set required/optional field property.
 */
class Require extends FieldAction {
  static public var REG = 'require';

  public var require(default, null): Bool;

  public function new(props: External) {
    super(props);
    require = G.toBool(props.get('require'));
  }

  override public function execField(field: Field, turnOn: Bool): Bool {
    var req: Bool = untyped require ^ (!turnOn);
    field.required = req;
    field.updateRequired();
    return req;
  }
}


/*
Set field value action.
Executes only when turnOn == true.
 */
class SetValue extends FieldAction {
  static public var REG = 'setValue';

  public var value(default, null): Dynamic;

  public function new(props: External) {
    super(props);
    value = props.get('value');
  }

  override public function execField(field: Field, turnOn: Bool): Bool {
    if (turnOn) field.setValue(value);
    return true;
  }
}


/*
Set field value action for every state of `turnOn` flag (one value for turnOn==false, other for turnOn==true).
 */
class SetValue2 extends FieldAction {
  static public var REG = 'setValue2';

  public var valueOn(default, null): Dynamic;
  public var valueOff(default, null): Dynamic;

  public function new(props: External) {
    super(props);
    valueOn = props.get('valueOn');
    valueOff = props.get('valueOff');
  }

  override public function execField(field: Field, turnOn: Bool): Bool {
    field.setValue(turnOn ? valueOn : valueOff);
    return true;
  }
}


/*
Add/remove subform action.
 */
class AddSubform extends FieldAction {
  static public var REG = 'addSubform';

  public function new(props: External) {
    super(props);
  }

  override public function execField(field: Field, turnOn: Bool): Bool {
    var flf: FormListField = cast field;
    if (turnOn) {
      flf.addDefaultForm();
    } else {
      var ln = flf.forms.length;
      if (ln > 0) flf.removeForm(flf.forms[ln - 1]);
    }
    return turnOn;
  }
}
