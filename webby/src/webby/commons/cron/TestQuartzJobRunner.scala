package webby.commons.cron

import org.apache.commons.lang3.StringUtils
import org.quartz.listeners.JobListenerSupport
import org.quartz.{JobExecutionContext, JobExecutionException}
import webby.commons.text.StringWrapper.wrapper
import webby.mvc.AppStub

/**
  * Класс для тестирования Quartz заданий.
  * Суть такая - задание должно уметь переживать рестарты сервера, это мы и тестируем здесь.
  *
  * Запускает Quartz в режиме тестирования, когда задния сами не выполняются по крону. Выполняется
  * только то задание, которое было указано при запуске (если указано вообще).
  *
  * Работает в две фазы. Сначала просто запускает задание, потом ждёт Enter, и перезапускается.
  * После перезапуска задание должно восстановиться и продолжить выполнение. Следующий Enter завершает
  * выполнение теста.
  *
  * Типичное применение класса - это создать объект для вызова из консоли:
  * {{{
  * object TestQuartzJob {
  *   def main(args: Array[String]) {
  *     new TestQuartzJobRunner(new Quartz(testMode = true))
  *   }
  * }
  * }}}
  */
class TestQuartzJobRunner(quartzInTestMode: => BaseQuartzPlugin) {

  def run(maybeJobFullName: Option[String] = None,
          maybeJsonData: Option[String] = None): Unit = {
    AppStub.withAppDev {
      def runPhase(secondPhase: Boolean) {
        val quartz = quartzInTestMode
        try {
          quartz.onStart()

          quartz.scheduler.getListenerManager.addJobListener(new JobListenerSupport {
            override def getName: String = "TestQuartzJob listener"
            override def jobToBeExecuted(context: JobExecutionContext): Unit = {
              println("::: Job started " + context.getJobDetail.getKey)
            }
            override def jobWasExecuted(context: JobExecutionContext, jobException: JobExecutionException): Unit = {
              println("::: Job finished " + context.getJobDetail.getKey)
            }
          })

          maybeJobFullName.foreach {jobFullName =>
            jobFullName.splitChar(':') match {
              case Array(group, name) =>
                maybeJsonData match {
                  case Some(jsonData) => quartz.runJobJsonString(group, name, jsonData)
                  case None => quartz.runJob(group, name)
                }
            }
          }

          if (!secondPhase) {
            println("=== Phase 1 === Press enter to restart quartz")
          } else {
            println("=== Phase 2 === Press enter to stop quartz")
          }
          System.in.read()

        } finally {
          quartz.onPrepareToShutdown()
          quartz.onStop()
        }
        if (!secondPhase) runPhase(secondPhase = true)
      }
      runPhase(secondPhase = false)
    }
  }

  def runFromConsole(args: Array[String]): Unit = {
    args match {
      case Array(jobFullName) => run(Some(jobFullName))
      case Array(jobFullName, jsonData) => run(Some(jobFullName), Some(jsonData))

      case _ =>
        println("Usage: runMain " + StringUtils.stripEnd(getClass.getName, "$") + " jobGroup:jobName [json-data]")
        println()
        run()
    }
  }
}
