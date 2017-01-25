package macros;

import haxe.macro.Context;
import haxe.macro.Expr;

/*
Simple macro adds @:keep annotation to class constructor
 */
class KeepConstructorMacro {
  public static function build(): Array<Field> {
    var fields = Context.getBuildFields();

    for (field_ in fields) {
      var field: Field = field_;

      switch (field.kind) {
        case FieldType.FFun(fn):
          if (field.name == 'new' || field.name == '_new') {
            field.meta.push({
              name: ':keep',
              pos: field.pos
            });
          }

        case _:
      }
    }

    return fields;
  }
}
