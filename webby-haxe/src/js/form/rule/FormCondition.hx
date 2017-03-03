package js.form.rule;

/*
Form condition base class
 */
import js.form.rule.FormCondition;
@:autoBuild(macros.KeepConstructorMacro.build())
class FormCondition {
  public function getFieldNames(): Array<String> return [];

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
    regCond(Not);
    regCond(And);
    regCond(Or);
    regCond(FieldEquals);
    regCond(FieldIn);
    regCond(FieldEmpty);
    regCond(FieldRegex);
  }

  /*
  Create new condition for specified properties.
   */
  public static function createCond(props: External): FormCondition return FormRule.internalCreate('condition', registry, props);
}


/*
`Not` condition
 */
class Not extends FormCondition {
  static public var REG = 'not';

  public var cond(default, null): FormCondition;

  public function new(props: External) {
    cond = FormCondition.createCond(props.get('cond'));
  }

  override public function getFieldNames(): Array<String> return cond.getFieldNames();

  override public function check(form: Form): Bool return !cond.check(form);
}


/*
`And` condition
 */
class And extends FormCondition {
  static public var REG = 'and';

  public var c1(default, null): FormCondition;
  public var c2(default, null): FormCondition;

  public function new(props: External) {
    c1 = FormCondition.createCond(props.get('c1'));
    c2 = FormCondition.createCond(props.get('c2'));
  }

  override public function getFieldNames(): Array<String> return c1.getFieldNames().concat(c2.getFieldNames());

  override public function check(form: Form): Bool return c1.check(form) && c2.check(form);
}


/*
`Or` condition
 */
class Or extends FormCondition {
  static public var REG = 'or';

  public var c1(default, null): FormCondition;
  public var c2(default, null): FormCondition;

  public function new(props: External) {
    c1 = FormCondition.createCond(props.get('c1'));
    c2 = FormCondition.createCond(props.get('c2'));
  }

  override public function getFieldNames(): Array<String> return c1.getFieldNames().concat(c2.getFieldNames());

  override public function check(form: Form): Bool return c1.check(form) || c2.check(form);
}


/*
Condition satisfied when field value exactly equals specific value.
 */
class FieldEquals extends FormCondition {
  static public var REG = 'fieldEquals';

  public var fieldName(default, null): String;
  public var value(default, null): Dynamic;

  public function new(props: External) {
    fieldName = props.get('field');
    value = props.get('value');
  }

  override public function getFieldNames(): Array<String> return [fieldName];

  override public function check(form: Form): Bool return form.fields.get(fieldName).value() == value;
}


/*
Condition satisfied when field value equals one of specified values.
 */
class FieldIn extends FormCondition {
  static public var REG = 'fieldIn';

  public var fieldName(default, null): String;
  public var values(default, null): Array<Dynamic>;

  public function new(props: External) {
    fieldName = props.get('field');
    values = props.get('values');
  }

  override public function getFieldNames(): Array<String> return [fieldName];

  override public function check(form: Form): Bool {
    var value = form.fields.get(fieldName).value();
    for (v in values) {
      if (v == value) return true;
    }
    return false;
  }
}


/*
Condition satisfied when field is empty (don't forget to check method `Field.isEmpty`)
 */
class FieldEmpty extends FormCondition {
  static public var REG = 'fieldEmpty';

  public var fieldName(default, null): String;

  public function new(props: External) {
    fieldName = props.get('field');
  }

  override public function getFieldNames(): Array<String> return [fieldName];

  override public function check(form: Form): Bool return form.fields.get(fieldName).isEmpty();
}


/*
Condition satisfied when field fully matches specified regular expression.
 */
class FieldRegex extends FormCondition {
  static public var REG = 'fieldRegex';

  public var fieldName(default, null): String;
  public var regex(default, null): RegExp;

  public function new(props: External) {
    fieldName = props.get('field');
    regex = new RegExp('^' + props.get('regex') + '$');
  }

  override public function getFieldNames(): Array<String> return [fieldName];

  override public function check(form: Form): Bool return regex.test(form.fields.get(fieldName).value());
}
