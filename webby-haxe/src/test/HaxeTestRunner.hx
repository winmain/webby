package test;

import goog.string.GoogString;
import haxe.CallStack;
import haxe.unit.TestCase;
import haxe.unit.TestRunner;

using js.lib.ArrayUtils;
using js.lib.StrUtils;

/*
Example usage:

Create `HaxeTests.hx` file contains:
------
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
------

Add some test case:
------
class ExampleHaxeModuleTest extends TestCase {
  public function testFoo(): Void {
  }
}
------
 */
class HaxeTestRunner {
  public static function main(allTestClasses: Array<Class<TestCase>>) {
    var args: Array<String> = untyped __js__('process.argv.slice(2)');
    setupSourceMapSupport();

    var runner = new TestRunner();

    if (args.length > 0) {
      // Run specified tests only
      var cases: Array<Class<TestCase>> = [];
      for (arg in args) {
        for (testClass in allTestClasses) {
          if (GoogString.startsWith(Type.getClassName(testClass), arg)) {
            cases.pushUnique(testClass);
          }
        }
      }
      if (cases.isEmpty()) {
        trace('No tests to run');
      } else {
        for (cls in cases) {
          runner.add(Type.createInstance(cls, []));
        }
      }

    } else {

      // Run all tests
      for (testClass in allTestClasses) {
        runner.add(Type.createInstance(testClass, []));
      }
    }

    var result = runner.run() ? 0 : -1;
    untyped __js__('process.exit({0})', result);
  }

  /*
  https://www.npmjs.com/package/source-map-support must be installed
   */
  public static function setupSourceMapSupport(): Void {
    try {
      untyped __js__("require('source-map-support').install();");

      // Workaround for HaxeUnit
      untyped CallStack.getStack = function(e: js.Error): Array<StackItem> {
        if (e == null) return [];
        return CallStack.makeStack(e.stack);
      }
    }
  }
}
