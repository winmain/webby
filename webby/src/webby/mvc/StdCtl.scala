package webby.mvc

import webby.api.mvc.Results

/**
  * Defines utility methods to generate `Action` and `Results` types.
  *
  * For example:
  * {{{
  * object HelloCtl extends StdCtl {
  *
  *   def hello(name: String) = SimpleAction { request =>
  *     Ok("Hello " + name)
  *   }
  *
  * }
  * }}}
  */
abstract class StdCtl extends Results
