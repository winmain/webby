package webby.route

/**
 * Разделитель пути на basePath и всё остальное.
 * Используется для получения basePath из пути. По нему составляется оптимизированная карта маршрутов (see RequestHandler).
 */
trait BasePathSplitter {

  /**
   * Получить basePath, и остальную часть пути. Работает в двух режимах:
   *
   * Режим обучения (#learning = true).
   * В path передаётся не весь путь, а только первая часть пути до переменной. #hasVar выставляется в true,
   * если после path должна идти переменная.
   * Хорошая реализация этого метода должна отслеживать ситуацию, когда при наличии #hasVar невозможно точно разграничить путь -
   * в таких случаях должен бросаться exception.
   *
   * Основной режим (#learning = false).
   * Флаг #hasVar игнорируется. В path передаётся весь путь, а не только первая его часть.
   * Здесь проверку на ошибки устраивать нет смысла.
   *
   * @return (basePath, otherPath)
   */
  def split(path: String, learning: Boolean, hasVar: Boolean): (String, String)
}


/**
 * Простой разделитель пути, который берёт только первую часть пути до первого слеша '/'
 */
object SimpleBasePathSplitter extends BasePathSplitter {

  def split(path: String, learning: Boolean, hasVar: Boolean): (String, String) = {
    def error = throw sys.error("Cannot split basePath (potentially unreachable route): " + path)
    path match {
      case "/" => if (learning && hasVar) error else ("/", "")
      case p => p.indexOf('/', 1) match {
        case -1 => if (learning && hasVar) error else (p, "")
        case idx => (p.substring(0, idx), p.substring(idx + 1))
      }
    }
  }
}


/**
 * Расширение простого разделителя пути, который берёт только первую часть пути до первого слеша '/', если это не админка.
 * Админка имеет "/=/" в начале, поэтому для неё мы разделяем до второго слеша '/'
 */
object SimpleBasePathSplitterWithAdmEqual extends BasePathSplitter {

  override def split(path: String, learning: Boolean, hasVar: Boolean): (String, String) = {
    def error = throw sys.error("Cannot split basePath (potentially unreachable route): " + path)
    path match {
      case "/" => if (learning && hasVar) error else ("/", "")
      case p => p.indexOf('/', if (p(1) == '=') 3 else 1) match {
        case -1 => if (learning && hasVar) error else (p, "")
        case idx => (p.substring(0, idx), p.substring(idx + 1))
      }
    }
  }
}