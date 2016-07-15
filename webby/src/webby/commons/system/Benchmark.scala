package webby.commons.system

object Benchmark {
  def time[A](a: => A): A = {
    val now = System.currentTimeMillis
    val result = a
    val micros = System.currentTimeMillis - now
    println("::: " + micros + " ms, result: " + result)
    result
  }

  def time[A](title: String, a: => A): A = {
    println(title)
    time(a)
  }
}
