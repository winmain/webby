package webby.mvc.script

import compiler.{ExternalCoffeeScriptCompiler, ExternalLessCompiler, LibSassCompiler}
import watcher.{SeparateWatcher, WideWatcher}
import webby.api.mvc.{Action, Results, SimpleAction}
import webby.api.{App, Profile}
import webby.commons.system.log.PageLog
import webby.mvc.StdPaths

/**
  * Стандартный набор серверов для статики: less, js, coffeescript, google-closure-compiler.
  * Эти сервера принимают на вход параметр path, а возвращают Action. Таким образом, они используются
  * как обработчики запросов.
  *
  * @param paths Реализация [[StdPaths]] в проекте, либо по-дефолту [[StdPaths.get]]
  * @param gccForProfile Фабрика [[GoogleClosureServer]] для заданного имени профайла. Этот профайл
  *                      необязательно должен совпадать с [[Profile]].
  *                      Например, это может быть метод GoogleClosureServers.create
  *                      из примера использования [[GoogleClosureServer]].
  */
class StdScriptRouteUtils(paths: StdPaths.Value, gccForProfile: String => GoogleClosureServer) {
  // lazy prefixes here to avoid creating unused servers that use unresolved optional dependencies
  // for example, sassServer uses jsass library

  lazy val sassServer: (String) => Action = magicServer(Some(paths.cssAssetType)) {
    ScriptServer(paths, paths.cssAssetType, List(
      LibSassCompiler(includePaths = Seq(paths.assetsProfile(App.profile.name).toString))), new WideWatcher(_))
  }

  lazy val lessServer: (String) => Action = magicServer(Some(paths.cssAssetType)) {
    ScriptServer(paths, paths.cssAssetType, List(
      ExternalLessCompiler(includePaths = Seq(paths.assetsProfile(App.profile.name).toString))), new WideWatcher(_))
  }

  lazy val jsServer: (String) => Action = magicServer(Some(StdPaths.get.jsAssetType)) {
    val googleClosureServer = gccForProfile("dev")
    (path: String) => SimpleAction {implicit req => PageLog.noLog(); googleClosureServer.serveDev(path)}
  }

  lazy val jsGccServer: (String) => Action = magicServer(None) {
    val googleClosureServer = gccForProfile("dev_closure")
    (path: String) => SimpleAction {implicit req => PageLog.noLog(); googleClosureServer.serveClosureCompiled(path)}
  }

  lazy val jsSimpleServer: (String) => Action = magicServer(Some(paths.jsSimpleAssetType)) {
    ScriptServer(paths, paths.jsSimpleAssetType, List(ExternalCoffeeScriptCompiler()), _ => SeparateWatcher)
  }

  // ------------------------------- Private & protected methods -------------------------------

  protected def magicServer(jenkinsAssetType: Option[StdPaths.AssetType])(localServer: => (String) => Action): (String) => Action =
    App.profile match {
      case Profile.Dev => localServer
      case Profile.Jenkins =>
        jenkinsAssetType match {
          case Some(assetType) => ScriptServer.simpleServer(assetType.sourcePath)
          case None => errorSimpleAction
        }
      case _ => errorSimpleAction
    }

  protected val errorSimpleAction = {path: String =>
    SimpleAction {_ => Results.InternalServerError("Cannot serve this file in " + App.profile)}
  }
}
