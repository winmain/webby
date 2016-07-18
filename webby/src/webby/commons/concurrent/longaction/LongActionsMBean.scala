package webby.commons.concurrent.longaction
import java.util

import webby.commons.system.mbean._

trait LongActionsMBean {
  MBeans.register(this, classOf[LongActionsMBean]).withName("LongActions")

  @Description("Информация обо всех запущенных действиях")
  def getActiveActionsInfo: util.List[String]

  @Description("Информация о прошедших действиях")
  def getFinishedActionsInfo: util.List[String]

  @Description("Послать запрос на отмену действия")
  def cancelAction(@PName("id") id: Int): String

  @Description("Аварийная остановка действия")
  def killAction(@PName("id") id: Int): String

  @Description("Прочитать состояние всех контроллеров из файлов и запустить их StatefulActions")
  def restoreAllStatefulActions()
}
