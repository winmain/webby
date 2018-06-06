package webby.form.field

import java.lang
import java.lang.Boolean.{FALSE, TRUE}
import javax.annotation.Nullable

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import querio._
import webby.api.mvc.{PlainResult, Results}
import webby.form._
import webby.form.jsrule._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

/**
 * Базовый trait поля формы
 *
 * @tparam T Тип значения, которое хранится в поле
 */
trait Field[T] {self =>
  def form: Form
  def shortId: String

  val htmlId: String = form.base.makeFieldHtmlId(this)

  private var _value: T = nullValue
  def get: T = _value
  def set(v: T): Unit = setValue(v)
  def set(v: Option[T]): Unit = v match {
    case Some(value) => setValue(value)
    case None | null => setValue(nullValue)
  }
  def :=(v: T): Unit = set(v)
  def :=(v: Option[T]): Unit = set(v)

  def setNull: Unit = setValue(nullValue)
  def getOpt: Option[T] = if (isEmpty) None else Some(get)

  protected def setValue(value: T) {
    val v = convertValue(value)
    if (_value != v) {
      _value = v
      changed = true
    }
  }
  protected final def silentlySetValue(v: T): Unit = _value = convertValue(v)

  /**
   * Конвертирует внешнее значение во внутренне значение поля. Вызывается в setValue, silentlySetValue.
   * Этот метод специально сделан для переопределения в потомках.
   */
  protected def convertValue(v: T): T = v

  var changed: Boolean = false
  def prepareBeforePost(): Unit = changed = false

  def nullValue: T

  var hasDbConnector: Boolean = false

  // ------------------------------- Builder & validations -------------------------------

  /**
   * Поле обязательно для заполнения?
   */
  var required: Boolean = false
  def require: this.type = require(v = true)
  def require(v: Boolean): this.type = { required = v; this }

  /**
    * Поле включено?
    * Примечание: этот флаг никак не влияет на запись поля в БД.
    * Т.е., выключенное поле будет записано в БД как и обычное.
    * Но, выключенные поля не принимают значения из POST-запроса.
    *
    * В отличие от [[ignored]], выключенное поле всё же иниализируется в javascript форме.
    */
  var enabled: Boolean = true
  def disable: this.type = enable(v = false)
  def enable: this.type = enable(v = true)
  def enable(v: Boolean): this.type = { enabled = v; this }

  /**
   * Поле пустое? Пустота поля определяется методом nullValue.
   */
  final def isEmpty: Boolean = isEmpty(_value)
  def isEmpty(v: T): Boolean = v == nullValue
  def nonEmpty: Boolean = !isEmpty

  /**
   * Поле игнорируется формой?
   * Игнорируемое поле не должно выводиться, оно не инициализируется js'ом, и игнорируется сервером при чтении POST запроса,
   * а также не проверяется сервером.
   * Но! Это поле читается и записывается в БД.
   * Т.е., оно просто не видимо в форме.
   */
  var ignored: Boolean = false
  def ignore: this.type = ignore(v = true)
  def ignore(v: Boolean): this.type = { ignored = v; this }
  def used: Boolean = !ignored
  def use: this.type = use(v = true)
  def use(v: Boolean): this.type = ignore(!v)

  /**
   * При нажатии на enter в этом поле форма сабмитится?
   * Работает только для input-полей (для textarea не работает).
   */
  var _enterKeySubmit: Boolean = false
  def enterKeySubmit: this.type = { _enterKeySubmit = true; this }

  /**
    * Показывать/скрывать не только само поле, но и его родительский [[BaseForms.formRowCls]].
    */
  var _hideWithRow: Option[Boolean] = None
  def hideWithRow(v: Option[Boolean]): this.type = {_hideWithRow = v; this}
  def hideWithRow(v: Boolean = true): this.type = hideWithRow(Some(v))

  /**
    * Список ограничений и проверок, накладываемых на это поле.
    */
  val constraints: mutable.Buffer[Constraint[T]] = mutable.Buffer.empty[Constraint[T]]

  /**
    * Проверить значение поля, прогнав все ограничения constraints.
    */
  def validateConstraints: ValidationResult = {
    constraints.foreach(_.check(_value) match {
      case invalid: Invalid => return invalid
      case _ => ()
    })
    Valid
  }

  /**
   * Проверки, специфичные для конкретной реализации Field.
   * Эти проверки не включают в себя список constraints, и не должны их вызывать или дублировать.
   * Этот метод может быть вызван несколько раз, поэтому иной раз стоит кешировать
   * результат проверки внутри поля для заданного значения поля.
   */
  def validateFieldOnly: ValidationResult = Valid

  /**
   * Проверка поля. Может быть вызвана несколько раз, поэтому иной раз стоит кешировать
   * результат проверки внутри поля для заданного значения поля.
   */
  def validate: FormResult = {
    if (required && isEmpty) return requiredError
    if (!isEmpty) {
      // Все проверки делаются только если это поле не пустое
      validateFieldOnly match {
        case Invalid(msg) => return error(msg)
        case _ => ()
      }
      validateConstraints match {
        case Invalid(msg) => return error(msg)
        case _ => ()
      }
    }
    FormSuccess
  }

  def addErrorIf(errorCondition: T => Boolean, onConditionFailed: => String): this.type
  = { constraints += new Constraint[T] {override def check(v: T): ValidationResult = if (errorCondition(v)) Invalid(onConditionFailed) else Valid}; this }
  def addValidIf(validCondition: T => Boolean, onConditionFailed: => String): this.type
  = { constraints += new Constraint[T] {override def check(v: T): ValidationResult = if (validCondition(v)) Valid else Invalid(onConditionFailed)}; this }

  /** Создать и вернуть ошибку обязательного заполнения этого поля для формы */
  def requiredError: FormErrors = FormErrors(required = ArrayBuffer(shortId))
  /** Создать и вернуть ошибку этого поля для формы */
  def error(message: String): FormErrors = FormErrors(errors = mutable.Map(shortId -> message))

  /** Вызов специального действия для этого поля из js */
  def connectedAction(tree: JsonNode): PlainResult = Results.BadRequest("Unsupported")

  /**
   * Применить или зафиксировать значение поля. Это действие вызывается после поста и валидации, и перед сохранением формы.
   * Пример действия - для полей UploadField старые файлы удаляются, а новые переносятся из временных в постоянные.
   * Также, это действие вызывается и после удаления формы для всех её полей (с установленным флагом formRemoved)
   *
   * @param formRemoved Флаг устанавливается, если этот метод был вызван для формы, которая удалена. Очень полезно при очистке полей за собой.
   */
  def applyValues(formRemoved: Boolean): FormResult = FormSuccess

  // ------------------------------- Db connectors -------------------------------

  def canAddDbConnector: Boolean = true

  def dbConnector[TR <: TableRecord, MTR <: MutableTableRecord[TR]]
  (dbConnector: DbConnector[T, TR, MTR])
  (implicit form: FormWithDb[TR, MTR]): this.type = {
    if (!canAddDbConnector) sys.error("Cannot use dbConnector for field " + getClass)
    if (hasDbConnector) sys.error("Field already have DbConnector")
    form.addFieldDbConnector(dbConnector)
    this
  }

  def connect[TR <: TableRecord, MTR <: MutableTableRecord[TR]]
  (dbField: Table[TR, MTR]#Field[_, T])
  (implicit form: FormWithDb[TR, MTR]): this.type =
    dbConnector[TR, MTR](new DbFieldConnector[T, TR, MTR](self, dbField))

  def connect[TR <: TableRecord, MTR <: MutableTableRecord[TR]]
  (dbField: Table[TR, MTR]#Field[_, Option[T]])
  (implicit form: FormWithDb[TR, MTR], ct: ClassTag[TR]): this.type =
    dbConnector[TR, MTR](new DbOptionFieldConnector[T, TR, MTR](self, dbField))

  def ~:~[TR <: TableRecord, MTR <: MutableTableRecord[TR]]
  (dbField: Table[TR, MTR]#Field[_, T])
  (implicit form: FormWithDb[TR, MTR]): this.type = connect(dbField)

  def ~:~[TR <: TableRecord, MTR <: MutableTableRecord[TR]]
  (dbField: Table[TR, MTR]#Field[_, Option[T]])
  (implicit form: FormWithDb[TR, MTR], ct: ClassTag[TR]): this.type = connect(dbField)

  def ~:~[DbT, TR <: TableRecord, MTR <: MutableTableRecord[TR]]
  (connector: PreparedDbConnector[T, TR, MTR])
  (implicit form: FormWithDb[TR, MTR]): this.type =
    dbConnector(connector.forField(this))

  def connect[TR <: TableRecord, MTR <: MutableTableRecord[TR]]
  (implicit form: FormWithDb[TR, MTR]): FormWithDb[TR, MTR]#DbConnectorStart[T, this.type] =
    new form.DbConnectorBuilder[T, this.type](this)

  def connectCustom[TR <: TableRecord, MTR <: MutableTableRecord[TR]]
  (connectorFn: this.type => DbConnector[T, TR, MTR])
  (implicit form: FormWithDb[TR, MTR]): this.type =
    dbConnector[TR, MTR](connectorFn(this))

  def connectStub[TR <: TableRecord, MTR <: MutableTableRecord[TR]]
  (implicit form: FormWithDb[TR, MTR]): this.type =
    dbConnector[TR, MTR](new StubDbConnector[T, TR, MTR](this))

  def ~![TR <: TableRecord, MTR <: MutableTableRecord[TR]]
  (implicit form: FormWithDb[TR, MTR]): this.type = connectStub

  // ------------------------------- Reading data & js properties -------------------------------

  /** Короткое название для js-класса обработчика этого поля (см. rr.form.Form.fieldClasses) */
  def jsField: String

  /**
   * Преобразовать хранимое значение в примитив для яваскрипта. Полученное значение будет пройдено через ObjectMapper.
   * Также, хранимое значение может быть null.
   */
  def toJsValue(v: T): AnyRef = v.asInstanceOf[AnyRef]
  final def toJsVal: AnyRef = toJsValue(_value)
  def setJsValueAndValidate(@Nullable node: JsonNode): FormResult

  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  class BaseJsProps {
    val shortId: String = self.shortId
    val jsField: String = self.jsField
    val required = boolFalse(self.required)
    val enabled = boolTrue(self.enabled)
    val enterKeySubmit = boolFalse(self._enterKeySubmit)
    val hideWithRow = self._hideWithRow

    // Вспомогательные функции для записи булевой переменной с дефолтным значением.
    // Для boolFalse дефолтное значение false, поэтому если value == false, то boolFalse вернёт null.
    // А null уже не будет создавать запись в выходном JSON'е.
    protected def boolFalse(value: Boolean): lang.Boolean = if (value) TRUE else null
    protected def boolTrue(value: Boolean): lang.Boolean = if (!value) FALSE else null
    protected def int(value: Int, default: Int): lang.Integer = if (value == default) null else lang.Integer.valueOf(value)
    protected def long(value: Long, default: Long): lang.Long = if (value == default) null else lang.Long.valueOf(value)
    protected def float(value: Float, default: Float): lang.Float = if (value == default) null else lang.Float.valueOf(value)
  }
  def jsProps: BaseJsProps = new BaseJsProps

  // ------------------------------- Js helpers -------------------------------

  /** Используется для условий в методе FormJsRules.addRule() */
  def ===(value: T): JsCondition = FieldEquals(this, value)
  def !==(value: T): JsCondition = Not(FieldEquals(this, value))
  def ~==(regex: String): JsCondition = FieldRegex(this, regex)
  def !~=(regex: String): JsCondition = Not(FieldRegex(this, regex))
  def inRule(values: Iterable[T]): JsCondition = FieldIn(this, values)
  def isEmptyRule: JsCondition = FieldEmpty(this)
  def isNonEmptyRule: JsCondition = Not(FieldEmpty(this))
}
