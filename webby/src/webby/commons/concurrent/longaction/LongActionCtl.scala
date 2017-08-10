package webby.commons.concurrent.longaction
import webby.api.mvc.{Action, SimpleAction}
import webby.commons.io.StdJs

/**
  * Common controller to retrieve LongAction status and process cancel requests.
  */
object LongActionCtl {
  case class ActionNotFound(notFound: Boolean = true)
  case class ActionStatus(name: String, progress: Int, maxProgress: Int, message: String, error: String, state: String, cancelable: Boolean)
  case class ActionCancelRequested(cancelRequested: Boolean)

  def status(id: Int) = withAction(id) {action =>
    val status = action.status.updated
    ActionStatus(action.name, status.progress, status.maxProgress, status.message, status.error, status.state.toString, cancelable = action.cancelable)
  }

  def requestCancel(id: Int) = withAction(id) {action =>
    try {
      if (action.cancelable && action.status.state == LongActionState.started) {
        action.status.requestCancel()
        ActionCancelRequested(cancelRequested = true)
      } else ActionCancelRequested(cancelRequested = false)
    } catch {
      case e: Exception => ActionCancelRequested(cancelRequested = false)
    }
  }

  // ------------------------------- Private & protected methods -------------------------------

  private def withAction(id: Int)(block: LongAction[_] => Any): Action = SimpleAction {rh =>
    StdJs.get.result(
      LongActions.getAction(id) match {
        case Some(action) => block(action)
        case _ => ActionNotFound()
      })
  }
}
