package webby.mvc.script

import webby.commons.io.Resources
import webby.mvc.script.minifier.StdSassMinifierResourceHolder

/**
  * Include SASS-CSS file with the same name as the controller.
  *
  * Usage:
  * 1. Controller should extends SassClassHolder
  * 2. To add on a page: page.addClassSass()
  */
trait SassClassHolder {
  implicit protected def _sassClassHolder: SassClassHolder = this

  val sassResHolder = StdSassMinifierResourceHolder.get.sassMin(Resources.nameForClass(getClass, ".sass"))
}
