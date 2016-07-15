package webby
import sbt.ConsoleLogger

object Colors {

  import scala.Console._

  def red(str: String): String = if (ConsoleLogger.formatEnabled) RED + str + RESET else str
  def blue(str: String): String = if (ConsoleLogger.formatEnabled) BLUE + str + RESET else str
  def cyan(str: String): String = if (ConsoleLogger.formatEnabled) CYAN + str + RESET else str
  def green(str: String): String = if (ConsoleLogger.formatEnabled) GREEN + str + RESET else str
  def magenta(str: String): String = if (ConsoleLogger.formatEnabled) MAGENTA + str + RESET else str
  def white(str: String): String = if (ConsoleLogger.formatEnabled) WHITE + str + RESET else str
  def black(str: String): String = if (ConsoleLogger.formatEnabled) BLACK + str + RESET else str
  def yellow(str: String): String = if (ConsoleLogger.formatEnabled) YELLOW + str + RESET else str
}
