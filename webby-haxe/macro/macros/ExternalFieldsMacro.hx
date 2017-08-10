package macros;

import haxe.macro.Context;
import haxe.macro.Expr;
import haxe.macro.Type;

/*
Этот макрос предназначен для автоматического создания геттеров и сеттеров для обычных полей (var) в классе.
Только, при каждом обращении к этому полю, оно будет записано так: obj.fieldname => obj["fieldname"].
Аналогично и при записи в поле.

Суть этого макроса в том, чтобы сохранить названия полей в этом классе после прохода Google Closure Compiler
в режиме Advanced.

---
Example usage:
---
@:build(macros.ExternalFieldsMacro.build())
class FieldProps {
  public var shortId: String;
  public var jsField: String;
  public var field: String;
  public var name: Null<String>;
  public var required: Null<Bool>;
  public var enabled: Null<Bool>;
  public var enterKeySubmit: Null<Bool>;
  public var hideWithRow: Null<Bool>;
}
 */
class ExternalFieldsMacro {
  public static function build(): Array<Field> {
    // get existing fields from the context from where build() is called
    var fields = Context.getBuildFields();

    var pos = Context.currentPos();

    var hasConstructor: Bool = false;

    for (field_ in fields) {
      var field: Field = field_;

      switch (field.kind) {
        case FieldType.FVar(varType, varExpr):
          field.kind = FieldType.FProp("get", "set", varType);

          // make getter
          var getterFn: Function = {
            args: [],
            expr: macro return untyped __js__('{0}[{1}]', this, $v{field.name}),
            ret: varType
          }
          var getterField: Field = {
            name: "get_" + field.name,
            access: [Access.APrivate, Access.AInline],
            kind: FieldType.FFun(getterFn),
            pos: pos
          };
          fields.push(getterField);

          // make setter
          var setterFn: Function = {
            args: [{
              name: "value",
              type: varType
            }],
            expr: macro return untyped __js__('{0}[{1}] = {2}', this, $v{field.name}, value),
            ret: varType
          }
          var setterField: Field = {
            name: "set_" + field.name,
            access: [Access.APrivate, Access.AInline],
            kind: FieldType.FFun(setterFn),
            pos: pos
          };
          fields.push(setterField);


        case FieldType.FFun(fn):
          if (field.name == 'new' || field.name == '_new') {
            hasConstructor = true;
          }

        case _:
      }
    }

    // Добавить конструктор, если его нет в классе. Только для abstract классов.
    if (!hasConstructor && Context.getLocalClass().get().kind.match(ClassKind.KAbstractImpl(_))) {
      var constructorFn: Function = {
        args: [],
        expr: macro this = {},
        ret: null
      }
      var constructorField: Field = {
        name: "new",
        access: [Access.APublic, Access.AInline],
        kind: FieldType.FFun(constructorFn),
        pos: pos
      };
      fields.push(constructorField);
    }

    return fields;
  }
}
