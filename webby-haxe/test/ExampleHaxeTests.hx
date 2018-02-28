package ;

import haxe.unit.TestCase;

@globalPrepend("// scriptEntryPoint")
class ExampleHaxeTests {
  static function allTests(): Array<Class<TestCase>> return [
    ExampleHaxeModuleTest
    // add all TestCases here
  ];

  static function main() {
    test.HaxeTestRunner.main(allTests());
  }
}


class ExampleHaxeModuleTest extends TestCase {
  public function testFoo(): Void {
  }
}
