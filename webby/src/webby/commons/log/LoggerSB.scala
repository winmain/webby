package webby.commons.log

import java.time.LocalDateTime

import org.apache.commons.lang3.StringUtils
import webby.api.mvc.RequestHeader
import webby.commons.text.SB

/**
 * Дополнительные методы для удобного составления строки лога
 */
class LoggerSB(capacity: Int = 128) extends SB(capacity) {

  // Формат: 2012-07-18T13:55:02
  def writeDateTime(v: LocalDateTime) {
    this + v.getYear + "-" + zpad2(v.getMonthValue) + "-" + zpad2(v.getDayOfMonth) + "T" +
      zpad2(v.getHour) + ":" + zpad2(v.getMinute) + ":" + zpad2(v.getSecond)
  }

  // Формат: "2012-07-18T13:55:02 [POST rosrabota.ru/=/moder/res~moder] "
  def writeCommonPrefix(v: LocalDateTime): Unit = {
    writeDateTime(v)
    this + " [" + Thread.currentThread().getName + "] "
  }

  // Формат: "2012-07-18T13:55:02 144.76.172.79 [POST rosrabota.ru/=/moder/res~moder] "
  def writeCommonPrefixWithIp(v: LocalDateTime)(implicit req: RequestHeader): Unit = {
    writeDateTime(v)
    this + " " + req.remoteAddress + " [" + Thread.currentThread().getName + "] "
  }

  private def zpad(value: Int, zeroes: Int): String =
    StringUtils.leftPad(String.valueOf(value), zeroes, '0')
  private def zpad2(value: Int) = zpad(value, 2)
}
