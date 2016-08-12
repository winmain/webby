package webby.commons.system.cron

import java.nio.file.{Files, Path}
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Date

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import org.apache.commons.lang3.StringUtils
import org.quartz.impl.matchers.GroupMatcher
import org.quartz.impl.{JobDetailImpl, StdSchedulerFactory}
import org.quartz.spi.{JobFactory, MutableTrigger, TriggerFiredBundle}
import org.quartz.{CronTrigger, TriggerKey, _}
import webby.api.{App, Plugin, Profile}
import webby.commons.io.{IOUtils, StdJs}
import webby.commons.system.mbean.{Description, MBeans, PName}
import webby.commons.text.StringWrapper.wrapper

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
  * Базовый класс для плагина Quartz - крон для явы.
  *
  * Пример использования:
  * {{{
  * class Quartz(testMode: Boolean = false) extends BaseQuartzPlugin(testMode) {
  *   override def initJobs(): Unit = {
  *     //
  *     // Действия каждые N минут
  *     //
  *     s(minutes(15, 5), job("subscribe", "ResSubscribeSender.sendAll", c => ResSubscribeSender.sendAll(c)))
  *     s(minutes(30, 3), job("stat", "flush deferred stat counters", c => DeferredStatCounters.plugin.get.flushCounters()))
  *
  *     //
  *     // Ежемесячные действия
  *     //
  *     s(monthly(1, 0, 6), job("stat", "StatCompanyModel.collectLastMonthStat", c => StatCompanyModel.collectLastMonthStat()))
  *   }
  *
  *   override protected def statePath: Path = Paths.state.resolve("quartz")
  *   override protected def jsMapper: ObjectMapper = Js.mapper
  * }
  * }}}
  *
  * Requires sbt dependency
  * {{{
  *   deps += "org.quartz-scheduler" % "quartz" % "2.2.3" exclude("c3p0", "c3p0")
  * }}}
  *
  * @param testMode Quartz запущен для тестирования? Не для продакшна. В этом режиме задания сами не выполняются.
  */
abstract class BaseQuartzPlugin(val testMode: Boolean) extends Plugin {self =>
  // ------------------------------- Abstract methods -------------------------------

  /**
    * Заполнение списка крон-заданий
    */
  def initJobs()

  /**
    * Путь к файлу, в который записывается состояние планировщика при перезагрузке сервера.
    * При старте планировщика, он читает этот файл и запускает остановленные задания.
    */
  protected def statePath: Path

  /**
    * Jackson [[ObjectMapper]] для сериализации состояния задаий.
    */
  protected def jsMapper: ObjectMapper = StdJs.get.mapper

  protected def createCronLog(group: String, name: String): CronLog = CronLogFactory.get.forQuartz(group, name)

  protected def canRunInProfile(profile: Profile): Boolean = profile.isProd

  // ------------------------------- Public methods -------------------------------

  def runJob(group: String, name: String): Unit = scheduler.triggerJob(new JobKey(name, group))
  def runJobData(group: String, name: String, data: AnyRef): Unit = scheduler.triggerJob(new JobKey(name, group), makeJobDataMap(jsMapper.valueToTree(data)))
  def runJobJsonString(group: String, name: String, json: String): Unit = scheduler.triggerJob(new JobKey(name, group), makeJobDataMap(jsMapper.readTree(json)))

  // ------------------------------- Inner methods -------------------------------

  protected val log = webby.api.Logger(getClass)

  protected[cron] var scheduler: Scheduler = _

  val jobMap = mutable.Map[JobKey, Job]()

  override def onStart() {
    if (!testMode) require(canRunInProfile(App.profile), "Quartz cannot run in profile '" + App.profile + "'")

    scheduler = StdSchedulerFactory.getDefaultScheduler
    scheduler.setJobFactory(new JobFactory {
      def newJob(bundle: TriggerFiredBundle, scheduler: Scheduler): Job = jobMap(bundle.getJobDetail.getKey)
    })
    val stPath: Path = statePath
    val jsonState: QuartzJsonState = {
      if (Files.exists(stPath)) jsMapper.readValue(IOUtils.readString(stPath), classOf[QuartzJsonState])
      else QuartzJsonState(time = System.currentTimeMillis(), jobStates = Map.empty)
    }
    triggerInitDate = new Date(jsonState.time)
    initJobs()
    scheduler.start()

    // Перезапустить все задания, у которых было сохранено состояние
    for ((jobFullName, jobJsonState) <- jsonState.jobStates) {
      val jobKey: JobKey = jobFullName.splitChar(':') match {
        case Array(group, name) => new JobKey(name, group)
      }
      log.info("Restoring " + jobFullName + " with data: " + jobJsonState)
      scheduler.triggerJob(jobKey, makeJobDataMap(jobJsonState))
    }

    Files.deleteIfExists(stPath)
  }

  // Quartz останавливаем в самом начале, ещё до остановки сервера, потому что он
  // не влияет на приём запросов и может затормозить остановку сервера.
  override def onPrepareToShutdown(): Unit = {
    if (scheduler != null) {
      val now = System.currentTimeMillis()
      // Получить и вывести в лог все текущие задания, чтобы оперативно знать кого мы ждём
      val curJobs = scheduler.getCurrentlyExecutingJobs
      if (!curJobs.isEmpty) {
        log.info("Currently executing jobs: " + curJobs.map {jec =>
          val jobKey: JobKey = jec.getJobDetail.getKey
          jobKey.getGroup + ":" + jobKey.getName
        }.mkString(", "))
      }
      scheduler.shutdown(true)

      // Получить состояния преждевременно завершившихся заданий
      val jobStates: Map[String, JsonNode] = {
        val b = Map.newBuilder[String, JsonNode]
        curJobs.foreach {jec =>
          jec.getResult match {
            case null => // do nothing
            case JobSaveForRestartResult(savedState) =>
              val jobKey: JobKey = jec.getTrigger.getJobKey
              val jobFullName: String = jobKey.getGroup + ":" + jobKey.getName
              b += ((jobFullName, savedState))
              log.info("Job " + jobFullName + " savedState: " + savedState)
            case v => log.error("Unsupported JobExecutionContext result: " + v)
          }
        }
        b.result()
      }

      // Сохранить состояние Quartz'а
      val quartzJsonState = QuartzJsonState(time = now, jobStates = jobStates)
      IOUtils.writeToFile(statePath, jsMapper.writeValueAsBytes(quartzJsonState))
    }
  }

  // ------------------------------- Private & protected methods -------------------------------

  sealed trait StubJob extends Job

  /** Специальный результат выполнения в [[JobExecutionContext]], сигнализирующий о том,
    * что задание завершилось корректно при получении сигнала остановки сервера, и это задание
    * следует запустить заново после рестарта сервера.
    */
  case class JobSaveForRestartResult(savedState: JsonNode)

  protected def makeJobDataMap(jobJsonState: JsonNode): JobDataMap = {
    val dataMap: JobDataMap = new JobDataMap()
    dataMap.put("savedStateNode", jobJsonState)
    dataMap
  }

  protected def job(group: String, name: String, job: CronJobContext => Unit): JobDetailImpl = {
    require(isValidName(name), "Invalid name: " + name)
    require(isValidName(group), "Invalid group: " + group)
    val jd: JobDetailImpl = new JobDetailImpl()
    jd.setName(name)
    jd.setGroup(group)
    jd.setJobClass(classOf[StubJob])
    jobMap.put(new JobKey(name, group), new Job {
      def execute(jec: JobExecutionContext) {
        val startTime = LocalDateTime.now()
        val cronLog = createCronLog(group, name).start()
        val initStateNode: Option[JsonNode] =
          jec.getMergedJobDataMap.get("savedStateNode") match {
            case null => None
            case jsonState: JsonNode => Some(jsonState)
          }
        val ctx: QuartzCronJobContext = new QuartzCronJobContext(jec, initStateNode, jsMapper)
        try {
          // Выполняем задание
          job(ctx)

          // После выполнения проверяем, долго ли выполнялось задание, и поддерживает ли оно быстрое завершение?
          if (!ctx.isTaskShutdownChecked) {
            val endTime = LocalDateTime.now()
            if (!isInNightHours(startTime) || !isInNightHours(endTime)) {
              val durationMin = ChronoUnit.MINUTES.between(startTime, endTime).toInt
              if (durationMin > 1) {
                log.warn("Job " + jec.getJobDetail.getKey + " run for too long (" + durationMin + " minutes) without checking for server shutdown")
              }
            }
          } else if (ctx.isShutdown) {
            // Сервер останавливается, а задание поддерживает остановку.
            // Если задание нужно перезапустить, то его состояние не должно быть пустым.
            if (ctx.finishState != null) {
              jec.setResult(JobSaveForRestartResult(jsMapper.valueToTree(ctx.finishState)))
            }
          }

        } catch {
          case e: Throwable =>
            log.error("Job " + jec.getJobDetail.getKey + ": " + e.getClass.getSimpleName + ": " + e.getMessage, e)
        }
        finally cronLog.finish()
      }
    })
    jd
  }

  private def isValidName(name: String) = name.nonEmpty && !StringUtils.containsAny(name, "\n\t:")

  /** Проверяем, относится ли время к ночному периоду?
    * Ночь считается с 23:00 до 4:00.
    * В ночное время обновление сервера не происходит, поэтому нет смысла следить за долгими заданиями.
    */
  private def isInNightHours(time: LocalDateTime): Boolean = time.getHour >= 23 || time.getHour <= 3

  /**
    * Format: "sec min hour day-of-month month day-of-week [year]"
    *
    * @see [[org.quartz.CronExpression]]
    */
  protected def cron(cronExpression: String): MutableTrigger =
    CronScheduleBuilder.cronSchedule(cronExpression).build()

  protected def minutes(minutePeriod: Int, offset: Int = 0): MutableTrigger =
    CronScheduleBuilder.cronSchedule(s"0 $offset/$minutePeriod * ? * *").build()

  protected def hourly(minute: Int): MutableTrigger =
    CronScheduleBuilder.cronSchedule(s"0 $minute * ? * *").build()

  protected def daily(hour: Int, minute: Int): MutableTrigger =
    CronScheduleBuilder.dailyAtHourAndMinute(hour, minute).build()

  //  private def weekly(dayOfWeek: Int, hour: Int, minute: Int) =
  //    triggerName(CronScheduleBuilder.weeklyOnDayAndHourAndMinute(dayOfWeek, hour, minute).build(), "weekly:"+dayOfWeek+":"+

  protected def monthly(dayOfMonth: Int, hour: Int, minute: Int): MutableTrigger =
    CronScheduleBuilder.monthlyOnDayAndHourAndMinute(dayOfMonth, hour, minute).build()

  protected def s(trigger: MutableTrigger, jobDetail: JobDetailImpl,
                  misfireInstruction: Int = CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW) {
    if (testMode) {
      // в тестовом режиме крон задание не должно запускаться автоматом
      jobDetail.setDurability(true)
      scheduler.addJob(jobDetail, false)
    } else {
      trigger.setMisfireInstruction(misfireInstruction)
      val triggerKey = new TriggerKey(jobDetail.getName, jobDetail.getGroup)
      trigger.setKey(triggerKey)
      trigger.setStartTime(triggerInitDate)
      scheduler.scheduleJob(jobDetail, trigger)
    }
  }

  private var triggerInitDate: Date = null

  // ------------------------------- MBean -------------------------------
  trait MBean {
    @Description("All job groups and names")
    def getJobs: Array[String]

    @Description("All currently executing jobs")
    def getCurrentlyExecutingJobs: java.util.List[String]

    @Description("Run job task")
    def runJob(@PName("group.name") groupName: String)

    @Description("Run job task")
    def runJob(@PName("group") group: String, @PName("name") name: String)

    @Description("Run job task with JSON data")
    def runJobJsonString(@PName("group") group: String, @PName("name") name: String, @PName("jsonData") jsonData: String)
  }

  object MBeanImpl extends MBean {
    override def getJobs: Array[String] = {
      scheduler.getJobKeys(GroupMatcher.anyJobGroup()).toArray.map(_.toString).sorted
    }
    override def getCurrentlyExecutingJobs: java.util.List[String] = {
      val ret = new java.util.ArrayList[String]()
      scheduler.getCurrentlyExecutingJobs.foreach {jec =>
        ret.add(jec.getJobDetail.getKey + ": started " + jec.getFireTime)
      }
      ret
    }
    override def runJob(groupName: String): Unit = {
      val Array(group, name) = groupName.splitChars(".", 2)
      self.runJob(group, name)
    }
    override def runJob(group: String, name: String): Unit = self.runJob(group, name)
    override def runJobJsonString(group: String, name: String, jsonData: String): Unit = runJobJsonString(group, name, jsonData)
  }

  def mBeanName = "Quartz"
  MBeans.register(MBeanImpl, classOf[MBean]).withName(mBeanName)
}

/**
  * Сохраняемое между рестартами состояние Quartz'а.
  * @param time      Последнее время отработки крона. По этому показателю мы можем определить пропущенные задания, и запустить их.
  * @param jobStates Сохранённые состояния заданий, которые нужно продолжить после старта сервера.
  */
case class QuartzJsonState(time: Long, jobStates: Map[String, JsonNode])
