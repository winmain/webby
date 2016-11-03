package webby.mvc.script
import java.nio.file.Path

import com.fasterxml.jackson.databind.ObjectMapper
import webby.api.Logger
import webby.api.mvc.{Result, Results}

trait ScriptErrorRenderer {
  def renderToClient(scriptPath: Path, multilineErrors: String): Result
  def renderToConsole(log: Logger, scriptPath: Path, multilineErrors: String)
}

class DefaultScriptErrorRenderer extends ScriptErrorRenderer {
  override def renderToClient(scriptPath: Path, multilineErrors: String): Result =
    Results.InternalServerError("Error compiling " + scriptPath + ":\n" + multilineErrors)

  override def renderToConsole(log: Logger, scriptPath: Path, multilineErrors: String): Unit = {
    log.error("Error compiling " + scriptPath + ":")
    System.err.println(multilineErrors)
  }
}

class JsScriptErrorRenderer extends DefaultScriptErrorRenderer {
  override def renderToClient(scriptPath: Path, multilineErrors: String): Result = {
    val mapper = new ObjectMapper()
    Results.Ok( """
if (!window.renderJsError) window.renderJsError = function(error) {
  console.error(error);

  var div = document.createElement('pre');
  div.style = "position: fixed; z-index: 999999; max-width: 80%; max-height: 100%; overflow: auto; background: #CA0E0E; color: #fff; font: 12px monospace; padding: 5px; box-shadow: 0 2px 10px 0 rgba(0,0,0,.5); line-height:150%"
  div.innerHTML = error.replace(/-+$/, '').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  document.body.appendChild(div);
}

renderJsError(""" + mapper.writeValueAsString(multilineErrors) + ")")
  }
}
