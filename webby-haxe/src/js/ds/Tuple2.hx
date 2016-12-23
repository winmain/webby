package js.ds;

class Tuple2<A, B> {
  public var a(default, null): A;
  public var b(default, null): B;

  public function new(a: A, b: B) {
    this.a = a;
    this.b = b;
  }

  public function toString() return '(${a}, ${b})';
}
