package webby.commons.system.log

import java.util.concurrent.LinkedBlockingQueue

import io.sentry.Sentry
import io.sentry.event.EventBuilder

class SafeSentryThread extends Thread {
  private val queue = new LinkedBlockingQueue[EventBuilder](queueCapacity)

  override def run(): Unit = {
    while (true) {
      val eventBuilder = queue.take()
      Sentry.capture(eventBuilder)
    }
  }

  def gracefullyStop(): Unit = {
    interrupt()
  }

  @throws(classOf[IllegalStateException])
  def enqueue(eventBuilder: EventBuilder): Unit = {
    queue.add(eventBuilder)
  }

  def queueCapacity = 1000
}
