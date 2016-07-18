package webby.commons.concurrent.longaction

/**
 * Trait, для хранения состояния StatefulAction.
 * Класс, реализующий этот trait, должен поддерживать сериализацию в json.
 */
trait StatefulState {
  def las: LongActionStatus
}
