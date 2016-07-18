package webby.commons.system.log

import java.time.ZoneId

import webby.commons.time.StdDates

import scala.collection.mutable.ArrayBuffer

class PageLog(val ip: String, val host: String, val method: String, val path: String) {
  val started = System.currentTimeMillis()
  val zoneId = ZoneId.systemDefault()

  var noLog = false

  private var _dbQueries: ArrayBuffer[QueryAndTime] = null
  def dbQueries: Iterable[QueryAndTime] = _dbQueries

  def setRecordQueries(): Unit = {
    if (_dbQueries == null) _dbQueries = new ArrayBuffer[QueryAndTime]()
  }

  private var _dbQueryCount = 0
  def dbQueryCount = _dbQueryCount
  private var _dbTotalTimeMs = 0L
  def addDbQuery(sql: String, queryTimeMs: Long) {
    _dbQueryCount += 1
    _dbTotalTimeMs += queryTimeMs
    if (_dbQueries != null) _dbQueries += QueryAndTime(sql, queryTimeMs)
  }

  private var _esQueries = 0
  private var _esTotalTimeMs = 0L
  def addEsQuery(queryTimeMs: Long) {
    _esQueries += 1
    _esTotalTimeMs += queryTimeMs
  }

  private var _cabanCookieGenCounter = 0
  def setCabanCookieGenCounter(v: Int): Unit = _cabanCookieGenCounter = v
  private var _cabanShortCounter = 0
  def setCabanShortCounter(v: Int): Unit = _cabanShortCounter = v
  private var _cabanLongCounter = 0
  def setCabanLongCounter(v: Int): Unit = _cabanLongCounter = v
  private var _cabanResolution: String = null
  def setCabanResolution(v: String): Unit = _cabanResolution = v

  private var _userId: Int = 0
  private var _userEmail: String = null
  def setUserSess(userId: Int, userEmail: String): Unit = {_userId = userId; _userEmail = userEmail}

  private var _resultStatus: Int = 0
  def setResultStatus(resultStatus: Int): Unit = _resultStatus = resultStatus

  private var _finishedTime: Long = 0L
  def setFinishedTime(finished: Long): Unit = _finishedTime = finished

  /**
   * Вернуть path, очищенный от escape символов типа %D0. Они не содержат никакой ценной информации, а лог из-за них раздувается.
   */
  protected def printCleanPath(sb: java.lang.StringBuilder) {
    var prev = 0
    var cur = 0
    while ( {cur = path.indexOf('%', prev); cur != -1}) {
      if (cur != prev) sb.append(path, prev, cur)
      prev = cur + 3
    }
    if (prev < path.length) sb.append(path, prev, path.length)
  }

  def toLogString: String = new LoggerSB() {
    // Формат: 2012-07-18T13:55:02
    writeDateTime(StdDates.toLocalDateTime(started, zoneId))

    +" ip:" + ip
    val totalTime: Long = _finishedTime - started
    +" t:" + totalTime
    if (_dbQueryCount > 0) +" db:" + _dbTotalTimeMs + "/" + _dbQueryCount else +" db:-"
    if (_esQueries > 0) +" es:" + _esTotalTimeMs + "/" + _esQueries else +" es:-"
    if (_userId != 0) {
      +" user:" + _userId
      if (_userEmail != null) +":" + _userEmail
    } else +" user:-"
    if (_resultStatus != 0) +" st:" + _resultStatus else +" st:-"
    +" cab:" + _cabanCookieGenCounter + "/" + _cabanShortCounter + "/" + _cabanLongCounter
    if (_cabanResolution != null) +" cr:" + _cabanResolution

    +" url:" + method + ":" + host
    printCleanPath(sb)
  }.toString
}

object PageLog extends ThreadLocal[PageLog] {
  def noLog() {
    val log: PageLog = get()
    if (log != null) log.noLog = true
  }

  def addDbQuery(sql: String, queryTimeMs: Long) {
    val log: PageLog = get()
    if (log != null) log.addDbQuery(sql: String, queryTimeMs)
  }

  def addEsQuery(queryTimeMs: Long) {
    val log: PageLog = get()
    if (log != null) log.addEsQuery(queryTimeMs)
  }
}

case class QueryAndTime(sql: String, time: Long)