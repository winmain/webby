package webby.commons.io

object Using {
  /**
    * Automatic resource management for [[AutoCloseable]] types
    *
    * Example:
    * {{{
    *   Using(new BufferedReader(new FileReader("file"))) { r =>
    *     var count = 0
    *     while (r.readLine != null) count += 1
    *     println(count)
    *   }
    * }}}
    */
  def apply[T <: AutoCloseable, R]
  (resource: T)
  (block: T => R): R = {
    try {
      block(resource)
    } finally {
      if (resource != null) resource.close()
    }
  }
}
