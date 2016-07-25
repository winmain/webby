package webby.route.v2

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{FunSuite, Inspectors, Matchers}
import webby.api.mvc.Handler
import webby.route._

class RouteV2ParserTest extends FunSuite with Matchers with TableDrivenPropertyChecks with Inspectors {

  //  def is = "test routeCommonParams" ! routeCommonParams ^
  //    "test routeTestInput" ! routeTestInput ^
  //    "var not in url should cause exception while parsing route" ! varNotInUrlTest

  val routes: Iterable[LinkedRouteV2] = RouteV2Parser.parse(
    RoutePack[Any](_ => _RouteTestHandlers, null, _RouteTest, EmptyDomainProvider), SimpleBasePathSplitter)

  test("routeCommonParams") {
    Table[String, String, String, Seq[TestVar]](("route", "base path", "compiled route", "vars")
      , ("simple", "/simple", "", Seq[TestVar]())
      , ("intAndStr", "/ias", "(-?[0-9]+),([^/]*)", Seq(intVar(), strVar()))
      , ("customStr", "/customs", "(.*)", Seq(strVar(".*")))
      , ("multipleInts", "/r", "(-?[0-9]+)/and\\.(-?[0-9]+)/another(-?[0-9]+)", Seq(intVar()))
      , ("shuffleStr", "/sh", "([a-z]+)/(qwe)/(.*)", Seq(strVar(".*"), strVar("qwe"), strVar("[a-z]+")))
      , ("escapes", "/e", "\\?param=value&another=(-?[0-9]+)", Seq(intVar()))
      , ("customRegexp", "/rr", "<\\.\\*>", Seq[TestVar]())
    ).forEvery {case (routeName, basePath, compiledRoute, vars) =>
      val route = findRoute(routeName)
      route.pattern.toString shouldEqual compiledRoute
      route.basePath shouldEqual basePath
      forAll(route.vars.zip(vars)) {
        case (vr, testVar) => testVar.testCompareTo(vr)
      }
      route.name shouldEqual routeName
    }
  }

  test("routeTestInput") {
    Table[String, String, String](("route", "test input", "test result")
      , ("simple", "/simple", "simple")
      , ("intAndStr", "/ias/5,qwe", "a:5,b:qwe")
      , ("customStr", "/customs/foo/bar", "s:foo/bar")
      , ("multipleInts", "/r/1/and.-2/another5", "a:1")
      , ("shuffleStr", "/sh/aaa/qwe/1", "a:1,b:qwe,c:aaa")
    ).forEvery {case (routeName, testInput, testResult) =>
      val route = findRoute(routeName)
      val (_, otherPath) = SimpleBasePathSplitter.split(testInput, learning = false, hasVar = false)
      val matcher = route.pattern.matcher(otherPath)
      matcher.matches() shouldEqual true
      route.resolve(null, matcher).asInstanceOf[Option[_TestHandler]].get.msg shouldEqual testResult
    }
  }

  private def findRoute(name: String): LinkedRouteV2 =
    routes.find(_.name == name).getOrElse(sys.error(s"Route $name not found"))


  test("var not in url should cause exception while parsing route") {
    ((the[Exception] thrownBy RouteV2Parser.parse(RoutePack[Any](_ => _VarNotInUrlHandlers, null, _VarNotInUrl, EmptyDomainProvider), SimpleBasePathSplitter)).getMessage
      should include ("All variables must be used"))
  }

  // ---------------------- routes ----------------------

  trait _RouteTestTrait[R] extends BaseRoute[R] {
    protected def toDomain: String = "test"

    def simple: R = get"/simple"
    def intAndStr(a: Int, b: String): R = get"/ias/$a,$b"
    def customStr(s: String = "default"): R = get"/customs/$s<.*>"
    def multipleInts(a: Int): R = get"/r/$a/and.$a/another$a"
    def shuffleStr(a: String, b: String, c: String) = get"/sh/$c<[a-z]+>/$b<qwe>/$a<.*>"
    def escapes(a: Int): R = get"/e/?param=value&another=$a"
    /** Такие регулярки не поддерживаются */
    def customRegexp: R = get"/rr/<.*>"
  }

  object _RouteTest extends RouteRoute with _RouteTestTrait[Route]

  object _RouteTestHandlers extends RouteHandlers with _RouteTestTrait[Handler] {
    override def simple: Handler = _TestHandler("simple")
    override def intAndStr(a: Int, b: String): Handler = _TestHandler(s"a:$a,b:$b")
    override def customStr(s: String): Handler = _TestHandler(s"s:$s")
    override def multipleInts(a: Int): Handler = _TestHandler(s"a:$a")
    override def shuffleStr(a: String, b: String, c: String): Handler = _TestHandler(s"a:$a,b:$b,c:$c")
    override def escapes(a: Int): Handler = _TestHandler(s"a:$a")
    override def customRegexp: Handler = _TestHandler("rr")
  }

  case class _TestHandler(msg: String) extends Handler

  // Var not in url route

  trait _VarNotInUrlTrait[R] extends BaseRoute[R] {
    protected def toDomain: String = "test"
    def varNotInUrl(v: Int): R = get"/var/notinurl"
  }
  object _VarNotInUrl extends RouteRoute with _VarNotInUrlTrait[Route]
  object _VarNotInUrlHandlers extends RouteHandlers with _VarNotInUrlTrait[Handler] {
    override def varNotInUrl(v: Int): Handler = _TestHandler("var")
  }

  // ---------------------- test vars ----------------------

  trait TestVar {
    def testCompareTo(vr: Var[_])
  }
  case class intVar(pat: String = "-?[0-9]+") extends TestVar {
    def testCompareTo(vr: Var[_]) {
      vr shouldBe a[IntVar]
      vr.asInstanceOf[IntVar].pattern shouldEqual pat
    }
  }
  case class strVar(pat: String = "[^/]*") extends TestVar {
    def testCompareTo(vr: Var[_]) {
      vr shouldBe a[StringVar]
      vr.asInstanceOf[StringVar].pattern shouldEqual pat
    }
  }
}
