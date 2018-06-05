package;

import haxe.unit.TestCase;
import js.form.field.FormListFieldTest;

using js.lib.ArrayUtils;
using js.lib.StrUtils;

@globalPrepend("// scriptEntryPoint")
class HaxeTests {
  static function allTests(): Array<Class<TestCase>> return [
    FormListFieldTest
  ];

  static function main() {
    test.HaxeTestRunner.main(allTests());
  }
}
