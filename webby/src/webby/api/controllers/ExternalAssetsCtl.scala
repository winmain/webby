package webby.api.controllers

import java.nio.file.{Files, Path, Paths}

import com.google.common.net.HttpHeaders
import webby.api._
import webby.api.mvc._
import webby.mvc.StdCtl

/**
  * Controller that serves static resources from an external folder.
  * It useful in development mode if you want to serve static assets that shouldn't be part of the build process.
  *
  * Not that this controller is not intented to be used in production mode and can lead to security issues.
  * Therefore it is automatically disabled in production mode.
  *
  * All assets are served with max-age=3600 cache directive.
  *
  * You can use this controller in any application, just by declaring the appropriate route. For example:
  * {{{
  * GET     /assets/\uFEFF*file               controllers.ExternalAssets.at(path="/home/peter/myplayapp/external", file)
  * GET     /assets/\uFEFF*file               controllers.ExternalAssets.at(path="C:\external", file)
  * GET     /assets/\uFEFF*file               controllers.ExternalAssets.at(path="relativeToYourApp", file)
  * }}}
  *
  */
object ExternalAssetsCtl extends StdCtl {

  val AbsolutePath = """^(/|[a-zA-Z]:\\).*""".r

  /**
    * Generates an `Action` that serves a static resource from an external folder
    *
    * @param rootPath the root folder for searching the static resource files such as `"/home/peter/public"`, `C:\external` or `relativeToYourApp`
    * @param file     the file part extracted from the URL
    */
  def at(rootPath: String, file: String): Action = SimpleAction {request =>
    val app: Application = App.app
    if (app.profile.isProd) NotFoundRaw
    else {
      val fileToServe: Path = rootPath match {
        case AbsolutePath(_) => Paths.get(rootPath, file)
        case _ => app.path.resolve(rootPath).resolve(file)
      }

      if (Files.exists(fileToServe)) {
        Ok.sendFile(fileToServe, inline = true).withHeader(HttpHeaders.CACHE_CONTROL, "max-age=3600")
      } else {
        NotFoundRaw
      }
    }
  }

}
