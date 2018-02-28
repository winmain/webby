package webby.mvc.script.compiler

import java.io.File

import webby.commons.text.SB
import webby.commons.text.StringWrapper.wrapper

import scala.io.{Codec, Source}

class HaxeErrorBeautifier {
  def beautify(messages: String): String = {
    val lines = messages.splitChar('\n')
    new SB {
      var lastKey: ErrorKey = null
      var lastMessages: Seq[String] = Nil
      lines.foreach {
        case ErrorCharacterR(filePathStr, rowStr, colStr, message) =>
          val col = colStr.toInt
          val key = ErrorKeyCols(filePathStr, rowStr.toInt, col, col + 1)
          if (key == lastKey) {
            lastMessages :+= message
          } else {
            if (lastKey != null) printError(lastKey, lastMessages)
            lastKey = key
            lastMessages = Seq(message)
          }

        case ErrorCharactersR(filePathStr, rowStr, col1Str, col2Str, message) =>
          val key = ErrorKeyCols(filePathStr, rowStr.toInt, col1Str.toInt, col2Str.toInt)
          if (key == lastKey) {
            lastMessages :+= message
          } else {
            if (lastKey != null) printError(lastKey, lastMessages)
            lastKey = key
            lastMessages = Seq(message)
          }

        case ErrorLinesR(filePathStr, rowStr, line1Str, line2Str, message) =>
          if (lastKey != null) printError(lastKey, lastMessages)
          printError(ErrorKeyLines(filePathStr, rowStr.toInt, line1Str.toInt, line2Str.toInt), Seq(message))
          lastKey = null
          lastMessages = Nil


        case line => +line + '\n'
      }

      def printError(anyKey: ErrorKey, messages: Seq[String]): Unit = {
        anyKey match {
          case key: ErrorKeyCols =>
            +key.filePathStr + ":" + key.row + " : " + messages.head + '\n'
            messages.drop(1).foreach(msg => +"  " + msg + '\n')

            // Вставить код из файла
            val file = new File(key.filePathStr)
            if (file.exists()) {
              val lines = Source.fromFile(file)(Codec.UTF8).getLines()
              lines.drop(key.row - 1)
              +lines.next() + '\n'
              +' '.repeat(key.col1) + '^'.repeat(key.col2 - key.col1) + '\n'
            } else {
              +"File not found: " + key.filePathStr + '\n'
            }

          case key: ErrorKeyLines =>
            +key.filePathStr + ":" + key.row + " : " + messages.head + '\n'
            messages.drop(1).foreach(msg => +"  " + msg + '\n')

            // Вставить код из файла
            val file = new File(key.filePathStr)
            if (file.exists()) {
              val lines = Source.fromFile(file)(Codec.UTF8).getLines()
              lines.drop(key.line1 - 1)
              val lineCount = math.min(10, key.line2 - key.line1 + 1)
              for (i <- 0.until(lineCount)) {
                +lines.next() + '\n'
              }
            } else {
              +"File not found: " + key.filePathStr + '\n'
            }
        }
      }

      if (lastKey != null) printError(lastKey, lastMessages)

      +"------------------------------------"
    }.str
  }

  private val ErrorCharacterR = "^([^:]+):(\\d+): character (\\d+) : (.*)".r
  private val ErrorCharactersR = "^([^:]+):(\\d+): characters (\\d+)-(\\d+) : (.*)".r
  private val ErrorLinesR = "^([^:]+):(\\d+): lines (\\d+)-(\\d+) : (.*)".r

  sealed private trait ErrorKey
  private case class ErrorKeyCols(filePathStr: String, row: Int, col1: Int, col2: Int) extends ErrorKey
  private case class ErrorKeyLines(filePathStr: String, row: Int, line1: Int, line2: Int) extends ErrorKey
}
