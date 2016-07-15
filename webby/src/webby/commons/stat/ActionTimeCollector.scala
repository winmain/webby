package webby.commons.stat

/**
  * Хранит счётчики выполнения действий и их суммарное время (в миллисекундах) для одного измерения
  * за день поминутно.
  * Хранилище оптимизировано по потреблению памяти. При хранении одних нулей оно занимает минимум
  * памяти.
  */
class ActionTimeCollector {
  import ActionTimeCollector._

  private[stat] val actionsHourly: Array[ValueStorage] = new Array[ValueStorage](24)
  private[stat] val millisHourly: Array[ValueStorage] = new Array[ValueStorage](24)

  locally {
    var i = 0
    while (i < 24) {
      actionsHourly(i) = EmptyStorage
      millisHourly(i) = EmptyStorage
      i += 1
    }
  }

  def put(hour: Int, minute: Int, actions: Long, millis: Long): Unit = {
    require(hour >= 0 && hour < 24, "Invalid hour")
    require(minute >= 0 && minute < 60, "Invalid minute")
    require(actions >= 0, "Actions cannot be negative")
    require(millis >= 0, "Times cannot be negative")
    if (!actionsHourly(hour).put(minute, actions)) {
      val storage: ValueStorage = newStorage(actions, 60)
      storage.fillFrom(actionsHourly(hour), Math.max(60, minute + 1))
      require(storage.put(minute, actions))
      actionsHourly(hour) = storage
    }
    if (!millisHourly(hour).put(minute, millis)) {
      val storage: ValueStorage = newStorage(millis, 60)
      storage.fillFrom(millisHourly(hour), Math.max(60, minute + 1))
      require(storage.put(minute, millis))
      millisHourly(hour) = storage
    }
  }
  def add(hour: Int, minute: Int, actions: Long, millis: Long): Unit = {
    put(hour, minute, actionsHourly(hour).get(minute) + actions,
      millisHourly(hour).get(minute) + millis)
  }

  def getActions(hour: Int, minute: Int): Long = actionsHourly(hour).get(minute)
  def getMillis(hour: Int, minute: Int): Long = millisHourly(hour).get(minute)
}

object ActionTimeCollector {
  abstract class ValueStorage {
    def capacity: Int
    def get(index: Int): Long
    def put(index: Int, value: Long): Boolean

    def fillFrom(other: ValueStorage, count: Int): Unit = {
      var i = 0
      while (i < count) {
        if (!put(i, other.get(i)))
          sys.error("Error filling ValueStorage from " + other + " to " + this + " for value:" + other.get(i))
        i += 1
      }
    }
  }

  object EmptyStorage extends ValueStorage {
    override def capacity: Int = Int.MaxValue
    override def get(index: Int): Long = 0L
    override def put(index: Int, value: Long): Boolean = value == 0L
  }

  class ByteStorage(_capacity: Int) extends ValueStorage {
    private[stat] val values: Array[Byte] = new Array[Byte](_capacity)

    override def capacity: Int = values.length
    override def get(index: Int): Long = {
      val v = values(index)
      if (v < 0) v + 0x100L else v
    }
    override def put(index: Int, value: Long): Boolean =
      if (value < 0x100L) {values(index) = value.toByte; true}
      else false
  }

  class ShortStorage(_capacity: Int) extends ValueStorage {
    private[stat] val values: Array[Short] = new Array[Short](_capacity)

    override def capacity: Int = values.length
    override def get(index: Int): Long = {
      val v = values(index)
      if (v < 0) v + 0x10000L else v
    }
    override def put(index: Int, value: Long): Boolean =
      if (value < 0x10000L) {values(index) = value.toShort; true}
      else false
  }

  class IntStorage(_capacity: Int) extends ValueStorage {
    private[stat] val values: Array[Int] = new Array[Int](_capacity)

    override def capacity: Int = values.length
    override def get(index: Int): Long = {
      val v = values(index)
      if (v < 0) v + 0x100000000L else v
    }
    override def put(index: Int, value: Long): Boolean =
      if (value < 0x100000000L) {values(index) = value.toInt; true}
      else false
  }

  class LongStorage(_capacity: Int) extends ValueStorage {
    private[stat] val values: Array[Long] = new Array[Long](_capacity)

    override def capacity: Int = values.length
    override def get(index: Int): Long = values(index)
    override def put(index: Int, value: Long): Boolean = {values(index) = value; true}
  }

  def newStorage(forValue: Long, capacity: Int): ValueStorage = forValue match {
    case 0 => EmptyStorage
    case v if v < 0x100L => new ByteStorage(capacity)
    case v if v < 0x10000L => new ShortStorage(capacity)
    case v if v < 0x100000000L => new IntStorage(capacity)
    case _ => new LongStorage(capacity)
  }
}
