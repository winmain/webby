import models.AppPaths
import webby.api.{Application, GlobalSettings}
import webby.api.mvc.{Handler, RequestHeader}

object Global extends GlobalSettings {

  override def initObjects(app: Application): Unit = {
    AppPaths
  }

  override def onRouteRequest(request: RequestHeader): Option[Handler] = ???
}
