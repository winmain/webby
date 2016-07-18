package webby.route.v1

import java.util.regex.{Matcher, Pattern}

import io.netty.handler.codec.http.HttpMethod
import webby.api.mvc.Handler
import webby.commons.io.Url
import webby.route._

import scala.annotation.tailrec
import scala.language.experimental.macros
import scala.reflect.runtime.universe._

trait RouteConfigV1 {
  type ActionHandler = Handler

  def basePathSplitter(path: String): (String, String)

  def createVar(name: String, tag: TypeTag[_], pattern: Option[String]): Var[_] = tag match {
    case TypeTag.Int => new IntVar(name, pattern)
    case a if a.tpe == typeOf[String] => new StringVar(name, pattern)
    case _ => sys.error("Unknown type " + tag)
  }

  class RouteDefs[D, DP <: DomainProvider[D]](dp: DP, domain: D) {
    implicit protected def $domain: D = domain

    /** Метод для мета-описания пути. Также, он служит для reverse-resolving */
    def route(path: String, handler: => ActionHandler): Url = {
      val i1 = path.indexOf(' ')
      val pathNoMethod = path.substring(i1 + 1)
      // Вырезать вставки регулярок "<...>"
      @tailrec
      def cropRegexp(p: String): String = p.indexOf('<') match {
        case -1 => p
        case i2 => cropRegexp(p.substring(0, i2) + p.substring(p.indexOf('>', i2 + 1) + 1))
      }
      val localUrl = cropRegexp(pathNoMethod)
      dp.toDomain(domain) match {
        case Some(d) => Url.common(d, localUrl)
        case None => Url.local(localUrl)
      }
    }
  }

  class SimpleRouteDefs[DP <: DomainProvider[Any]](dp: DP = null) extends RouteDefs[Any, DP](dp, null)

  private var _routes = Seq[LinkedRouteV1]()

  private def addRoute(route: LinkedRouteV1) {
    _routes = _routes :+ route
  }
  def routes = _routes

  /**
   * Класс, создаваемый и вызываемый макросом RouteMacros для регистрации пути
   * @param name            Имя метода, задающее имя пути
   * @param domainProvider  Обработчик домена для этого пути. Он определяет подходит ли заданный домен к этому пути.
   * @param parts           Константные пути
   * @param varNames        Имена переменных, которые подставляются в эти части путей
   * @param varTypes        Типы переменных
   */
  class _DefineRoute[DD](name: String, domainProvider: DomainProvider[DD], parts: List[String], varNames: List[String], varTypes: List[TypeTag[_]]) {
    val (method, part0_noMethod) = {
      val p0 = parts(0)
      val idx = p0.indexOf(' ')
      val method = HttpMethod.valueOf(p0.substring(0, idx))
      (method, p0.substring(idx + 1).trim)
    }
    val (basePath, part0) = basePathSplitter(part0_noMethod)

    private val (varsIterable, cleanSubParts) = (varNames, varTypes, parts.drop(1)).zipped.map {
      (name, tpe, part) =>
        val (newPart, pattern) =
          if (part.startsWith("<")) {
            val idx = part.indexOf('>')
            (part.substring(idx + 1), Some(part.substring(1, idx)))
          } else (part, None)
        (createVar(name, tpe, pattern), newPart)
    }.unzip[Var[_], String]
    private val vars = varsIterable.toArray

    val cp: Array[String] = (Seq(part0) ++ cleanSubParts).toArray

    val patternString: String =
      Pattern.quote(part0) + vars.view.zip(cleanSubParts).map(a => "(" + a._1.pattern + ")" + Pattern.quote(a._2)).mkString
    val pattern: Pattern = Pattern.compile(patternString)

    // ----------- route registrars -----------
    def reg(handler: (DD) => ActionHandler) {
      assert(cp.length == 1, "Inconsistent path parts")
      addRoute(new Route0(name, domainProvider, method, basePath, pattern, handler, cp(0)))
    }
    def reg[A](handler: (DD, A) => ActionHandler) {
      assert(cp.length == 2, "Inconsistent path parts")
      addRoute(new Route1(name, domainProvider, method, basePath, pattern, handler, cp(0), vars(0).asInstanceOf[Var[A]], cp(1)))
    }
    def reg[A, B](handler: (DD, A, B) => ActionHandler) {
      assert(cp.length == 3, "Inconsistent path parts")
      addRoute(new Route2(name, domainProvider, method, basePath, pattern, handler,
        cp(0), vars(0).asInstanceOf[Var[A]], cp(1), vars(1).asInstanceOf[Var[B]], cp(2)))
    }
    def reg[A, B, C](handler: (DD, A, B, C) => ActionHandler) {
      assert(cp.length == 4, "Inconsistent path parts")
      addRoute(new Route3(name, domainProvider, method, basePath, pattern, handler,
        cp(0), vars(0).asInstanceOf[Var[A]], cp(1), vars(1).asInstanceOf[Var[B]], cp(2), vars(2).asInstanceOf[Var[C]], cp(3)))
    }
    def reg[A, B, C, D](handler: (DD, A, B, C, D) => ActionHandler) {
      assert(cp.length == 5, "Inconsistent path parts")
      addRoute(new Route4(name, domainProvider, method, basePath, pattern, handler,
        cp(0), vars(0).asInstanceOf[Var[A]], cp(1), vars(1).asInstanceOf[Var[B]], cp(2), vars(2).asInstanceOf[Var[C]],
        cp(3), vars(3).asInstanceOf[Var[D]], cp(4)))
    }
    // -----------------------------------------
  }

  // -------- Route --------

  class Route0[DD](val name: String, val domainProvider: DomainProvider[DD], val method: HttpMethod, val basePath: String, val pattern: Pattern,
                   val handler: (DD) => ActionHandler, val p0: String) extends LinkedRouteV1 {
    override def toString = s"$method $basePath $p0 - $name: $pattern"

    def resolve(domain: Any, m: Matcher): ActionHandler = handler(domain.asInstanceOf[DD])
  }

  class Route1[DD, A](val name: String, val domainProvider: DomainProvider[DD], val method: HttpMethod, val basePath: String, val pattern: Pattern,
                      val handler: (DD, A) => ActionHandler, val p0: String, val v0: Var[A], val p1: String) extends LinkedRouteV1 {
    override def toString = s"$method $basePath $p0{${v0.name}}$p1 - $name: $pattern"

    def resolve(domain: Any, m: Matcher): ActionHandler = handler(domain.asInstanceOf[DD],
      v0.fromString(m.group(1)))
  }

  class Route2[DD, A, B](val name: String, val domainProvider: DomainProvider[DD], val method: HttpMethod, val basePath: String, val pattern: Pattern,
                         val handler: (DD, A, B) => ActionHandler,
                         val p0: String, val v0: Var[A], val p1: String, val v1: Var[B], val p2: String) extends LinkedRouteV1 {
    override def toString = s"$method $basePath $p0{${v0.name}}$p1{${v1.name}}$p2 - $name: $pattern"

    def resolve(domain: Any, m: Matcher): ActionHandler = handler(domain.asInstanceOf[DD],
      v0.fromString(m.group(1)), v1.fromString(m.group(2)))
  }

  class Route3[DD, A, B, C](val name: String, val domainProvider: DomainProvider[DD], val method: HttpMethod, val basePath: String, val pattern: Pattern,
                            val handler: (DD, A, B, C) => ActionHandler,
                            val p0: String, val v0: Var[A], val p1: String, val v1: Var[B], val p2: String,
                            val v2: Var[C], val p3: String) extends LinkedRouteV1 {
    override def toString = method + " " + basePath + " " + p0 + v0.name + p1 + v1.name + p2 + v2.name + p3 + " - " + name + ": " + pattern

    def resolve(domain: Any, m: Matcher): ActionHandler = handler(domain.asInstanceOf[DD],
      v0.fromString(m.group(1)), v1.fromString(m.group(2)), v2.fromString(m.group(3)))
  }

  class Route4[DD, A, B, C, D](val name: String, val domainProvider: DomainProvider[DD], val method: HttpMethod, val basePath: String, val pattern: Pattern,
                               val handler: (DD, A, B, C, D) => ActionHandler,
                               val p0: String, val v0: Var[A], val p1: String, val v1: Var[B], val p2: String,
                               val v2: Var[C], val p3: String, val v3: Var[D], val p4: String) extends LinkedRouteV1 {
    override def toString = method + " " + basePath + " " + p0 + v0.name + p1 + v1.name + p2 + v2.name + p3 + v3.name + p4 + " - " + name + ": " + pattern

    def resolve(domain: Any, m: Matcher): ActionHandler = handler(domain.asInstanceOf[DD],
      v0.fromString(m.group(1)), v1.fromString(m.group(2)), v2.fromString(m.group(3)), v3.fromString(m.group(4)))
  }

}