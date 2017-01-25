package webby.route.v2

import java.lang.reflect.{Method, Modifier}
import java.util.regex.Pattern

import com.google.common.base.CharMatcher
import webby.api.mvc.Handler
import webby.route._

import scala.collection.{breakOut, mutable}

object RouteV2Parser {

  import scala.reflect.runtime.universe

  case class Meth(name: String, paramTypes: Seq[Class[_]]) {
    def this(m: Method) = this(m.getName, m.getParameterTypes.toSeq)
  }

  def parse[DD](rp: RoutePack[DD], basePathSplitter: BasePathSplitter): Iterable[LinkedRouteV2] = {
    val routeLinks = rp.routeHandlersForDomainData(rp.sampleDomainData)
    val mirror: universe.Mirror = universe.runtimeMirror(routeLinks.getClass.getClassLoader)
    val routeRoute: RouteRoute = rp.routeRoute
    val linkMethods: Array[Method] = routeLinks.getClass.getDeclaredMethods

    checkHandlerOverrides(routeLinks, linkMethods)

    val linkMethodMap: Map[String, Method] =
      (for (method <- linkMethods if method.getReturnType == classOf[Handler]) yield method.getName -> method)(breakOut)

    val routeRouteMirror = mirror.reflect(routeRoute)
    val cs = routeRouteMirror.symbol

    val ret = mutable.Buffer[LinkedRouteV2]()
    for (member <- cs.toType.members.sorted if member.isMethod;
         routeMethod = member.asMethod if routeMethod.returnType.typeSymbol.asType.isAbstract && !routeMethod.isFinal;
         linkMethod <- linkMethodMap.get(routeMethod.name.decodedName.toString)) {
      val routeParams: List[universe.Symbol] = routeMethod.paramLists match {
        case p if p.isEmpty => List.empty
        case p => p.head
      }
      val stubs: Vector[VarStub.Stub] = routeParams.map {
        p => VarStub.resolve(p.typeSignature, routeRoute.getClass.getSimpleName + "." + routeMethod.name.decodedName.toString)
      }(scala.collection.breakOut)

      val route: Route = routeRouteMirror.reflectMethod(routeMethod).
        apply(stubs.zipWithIndex.map(v => v._1.makeStub(v._2)): _*).asInstanceOf[Route]

      val pi = route.parts.iterator
      val ai = route.args.iterator
      val sb = new java.lang.StringBuilder(64)
      val varArray = Array.ofDim[Var[_]](stubs.size)
      val varIndices = Array.ofDim[Int](stubs.size)
      val (basePath, firstPartEnding) = {
        val firstPart = pi.next()
        basePathSplitter.split(firstPart, learning = true, hasVar = route.args.nonEmpty)
      }
      sb append sanitize(firstPartEnding)
      var i = 0
      while (pi.hasNext) {
        val (part, pattern) = splitRegexpPart(pi.next())
        val varIdx: Int = VarStub.indexFromStub(ai.next())
        val vr = stubs(varIdx).toVar("unknown", pattern)
        if (varArray(varIdx) == null) {
          // Непроинициализированная переменная записана как null (мы работаем с массивом, у которого заранее задан размер, и все значения изначально нулевые).
          // Когда одна и та же переменная записана в урле несколько раз ("/$a/qwe/$a"), то берём значение только из первой.
          varArray(varIdx) = vr
          varIndices(varIdx) = i
        }
        sb append '('
        sb append vr.pattern
        sb append ')'
        sb append sanitize(part)
        i += 1
      }
      val path: String = sb.toString

      for (method <- route.methods) {
        if (method == null) sys.error("Empty method")
        ret += new LinkedRouteV2(
          name = routeMethod.name.decodedName.toString,
          domainProvider = rp.domainProvider,
          method = method,
          basePath = basePath,
          pattern = Pattern.compile(path),
          routeLinksForDomainData = rp.routeHandlersForDomainData.asInstanceOf[(Any) => RouteHandlers],
          vars = varArray.toVector,
          varIndices = varIndices.toVector,
          linkMethod = linkMethod)
      }
    }
    ret
  }

  /**
   * Отделить регулярку от части пути. Регулярка всегда идёт в начале строки. Она заключена в угловые скобки "<>".
   * Например: "<.*>some/other/part/" => ("some/other/part/", Some(".*"))
   */
  @inline private[v2] def splitRegexpPart(part: String): (String, Option[String]) =
    if (part.length > 2 && part.charAt(0) == '<') {
      val idx = part.indexOf('>')
      (part.substring(idx + 1), Some(part.substring(1, idx)))
    } else (part, None)

  /**
   * Проверить, для всех ли путей указаны обработчики. Также, лишних обработчиков быть не должно.
   */
  def checkHandlerOverrides(routeLinks: RouteHandlers, methods: Array[Method]) {
    val map = new mutable.HashMap[Meth, Int] {
      override def default(key: Meth): Int = 0
    }
    for (method <- methods) {
      if (!method.getName.startsWith("$")) {
        if (method.getReturnType == classOf[Handler]) map(new Meth(method)) += 1
        else if (method.getReturnType == classOf[Object] && !Modifier.isFinal(method.getModifiers)) map(new Meth(method)) -= 1
        // Здесь, модификатор final у метода trait'а говорит о том, что этот метод не требует Handler'а.
        // Сделано это потому, что scala annotations, никак не видно из java reflections. Возможно, есть способ сделать это изящнее.
      }
    }
    val invalidMethods = map.filter(_._2 != 0)
    if (invalidMethods.nonEmpty) {
      val className = routeLinks.getClass.getCanonicalName.replaceFirst("\\$$", "")
      throw new RuntimeException(s"No methods or invalid signature in $className:\n" +
        invalidMethods.map(e => e._1.name + " - " + (if (e._2 == 1) "handler without path" else "no handler")).mkString("\n"))
    }
  }

  def sanitize(s: String): String = {
    val sb = new java.lang.StringBuilder(s.length + 4)
    var i = 0
    var continue = true
    while (continue && i < s.length) {
      sanitizeMatcher.indexIn(s, i) match {
        case -1 => continue = false
        case idx =>
          sb.append(s, i, idx).append('\\').append(s.charAt(idx))
          i = idx + 1
      }
    }
    sb.append(s, i, s.length)
    sb.toString
  }
  private val sanitizeMatcher = CharMatcher.anyOf(".?$^*()+[]")

  ////////////////////
  //  def tt(rl: RouteLinks) {
  //    Benchmark.time {
  //      RouteV2Parser.parse(rl, _ => rl)
  //    }
  //  }
}


////////////////////////////////////////// TODO: убрать
//object Benchmark {
//  def time[A](a: => A): A = {
//    val now = System.currentTimeMillis
//    val result = a
//    val micros = System.currentTimeMillis - now
//    println("::: " + micros + " ms")
//    result
//  }
//
//  def time[A](title: String, a: => A): A = {
//    println(title)
//    time(a)
//  }
//}
