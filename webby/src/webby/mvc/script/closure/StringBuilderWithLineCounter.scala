package webby.mvc.script.closure
import java.lang.StringBuilder

import org.apache.commons.lang3.StringUtils

class StringBuilderWithLineCounter(capacity: Int) {
  private val sb = new StringBuilder(capacity)
  private var line = 0

  def getLine: Int = line
  def str: String = sb.toString
  override def toString: String = str

  def nl(): this.type = {
    sb.append('\n')
    line += 1
    this
  }

  def add(s: String): this.type = {
    addRaw(s, StringUtils.countMatches(s, '\n'))
  }

  def +(s: String): this.type = add(s)

  def addLine(lineWithoutNL: String): this.type = {
    add(lineWithoutNL)
    nl()
  }


  def addRaw(s: String, addLines: Int): this.type = {
    sb.append(s)
    line += addLines
    this
  }
}
