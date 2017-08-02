package webby.api.db.evolutions
import java.io.File

import webby.api.App
import webby.html.{HtmlBuffer, StdHtmlView, WebbyPage}
import webby.mvc.StdCtl

import scala.collection.immutable.IntMap

/**
  * DbEvolutions admin controller
  */
class AdmEvolutionsCtl(ev: WebbyDbEvolutions, admConf: DbEvolutionsAdmConf) extends StdCtl {
  def evolutionsBlock(implicit buf: HtmlBuffer, page: WebbyPage) = new StdHtmlView(buf) {
    val vm: VersionManager = ev.vm
    val currentVersion: Int = vm.getCurrentVersion
    val newEvolutions: IntMap[File] = ev.getNewEvolutions(currentVersion)
    div.style("line-height: 100%") {
      if (newEvolutions.isEmpty) p {+"Db evolution: " + currentVersion + span.style("margin-left: 10px; color:#ccc") ~ "latest"}
      else {
        p {+"Db evolution: " + currentVersion}
        p.style("margin: 5px 0") ~ "New evolutions:"
        ul.cls("disc orange").style("margin-bottom: 10px") {
          newEvolutions.foreachValue(file => li ~ file.getName)
        }
        a.cls("btn btn-orange").href(admConf.evolutionApplyNewUrl) ~ "Apply new evolutions"
      }
    }
  }

  def applyNew = admConf.applyWrapper {implicit page =>
    if (App.isProd) Ok("Evolutions not supported in production")
    else {
      ev.updateEvolutions()
      Redirect(admConf.admMainUrl)
    }
  }
}
