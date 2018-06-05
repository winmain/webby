package webby.form.field
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import javax.annotation.Nullable
import webby.commons.text.Plural
import webby.form._

import scala.collection.JavaConverters._
import scala.collection.mutable

class FormListField[F <: SubForm](val form: Form,
                                  val shortId: String,
                                  var factory: () => F,
                                  var recordPlural: Plural,
                                  keyOps: FormListKeyOps[F#Key])
  extends Field[Vector[F]] {self =>
  final type Key = F#Key

  def htmlTemplateId: String = customTemplateId.getOrElse(htmlId + "-template")
  def htmlListId: String = htmlId + "-list"
  def htmlAddId: String = htmlId + "-add"

  var defaultItems: Int = 0
  var minItems: Option[Int] = None
  var maxItems: Option[Int] = None
  var uniqueBy: Option[(F => Field[_], String)] = None
  var customTemplateId: Option[String] = None

  /** Специальная карта старых подформ, которые оказались неиспользованы после получения новых значений. Их следует удалить из БД. */
  var removeOldForms: mutable.Map[Key, F] = mutable.Map.empty[Key, F]

  /**
    * Ключ, который был выставлен у последней подформы с нулевым key.
    * Т.е., при вызове метода add() для у FormListField (но не FormListFieldWithDb), если у формы нулевой key,
    * то ей выставляется lastNewKey + 1, и сам lastNewKey увеличивается на 1.
    */
  private var lastNewKey: F#Key = keyOps.zeroKey

  silentlySetValue(nullValue)

  def valueGet(index: Int): F = get.apply(index)
  override protected def setValue(v: Vector[F]): Unit = {
    if (v.size != get.size) {
      changed = true
      silentlySetValue(v)
    } else {
      silentlySetValue(v)
      changed = v.exists(_.changed)
    }
  }
  override def prepareBeforePost(): Unit = {
    super.prepareBeforePost()
    get.foreach(_.prepareBeforePost())
  }

  // ------------------------------- Reading data & js properties -------------------------------

  /** Короткое название для js-класса обработчика этого поля (см. rr.form.Form.fieldClasses) */
  class JsProps extends BaseJsProps {
    private val f = factory()

    val defaultItems = self.defaultItems
    val minItems = self.minItems
    val maxItems = self.maxItems
    val uniqueBy = self.uniqueBy.map {case (fn, msg) => fn(f).shortId -> msg}
    val sub = f.jsProps
    val templateId = customTemplateId
  }
  override def jsProps = new JsProps
  override def jsField: String = "formList"
  override def nullValue: Vector[F] = Vector.empty[F]

  override def toJsValue(v: Vector[F]): AnyRef = v.map(_.jsValues)

  override def setJsValueAndValidate(@Nullable node: JsonNode): FormResult = {
    if (node == null) {
      setNull
      FormSuccess
    } else if (!node.isArray || !node.isInstanceOf[ArrayNode]) {
      FormErrors(errors = mutable.Map(shortId -> "Invalid form value"))
    } else {
      val formErrors = FormErrors()
      removeOldForms = super.get.map(f => f.key -> f)(scala.collection.breakOut)
      removeOldForms.remove(keyOps.zeroKey)
      // Нет смысла хранить значения без ключей - их всё равно нет в базе.
      val processedKeys = mutable.Buffer.empty[Key]

      val valueBuilder = Vector.newBuilder[F]
      for ((jsForm, index) <- node.asInstanceOf[ArrayNode].elements().asScala.zipWithIndex) {
        val key: Key = keyOps.fromJsonNode(jsForm.get("_key"))
        // Попытаться найти уже готовую подформу по ключу. Если такой нет, то создать новую.
        val subForm: F =
          if (key != keyOps.zeroKey) {
            if (processedKeys.contains(key)) return FormErrors(errors = mutable.Map(shortId -> "Duplicate form key"))
            processedKeys += key
            removeOldForms.get(key) match {
              case Some(oldForm) => removeOldForms.remove(key); oldForm
              case None => factory()
            }
          } else factory()

        subForm.setJsValuesAndValidate(jsForm, FormErrors(name = shortId, index = Some(index))) match {
          case e: FormErrors => formErrors.sub += e
          case _ => ()
        }
        valueBuilder += subForm
      }
      setValue(valueBuilder.result())
      formErrors ++= validate
      formErrors.orSuccess
    }
  }

  // ------------------------------- Form methods -------------------------------

  def formStub: F = factory()

  def add(v: F) {
    checkAndSetNewKey(v)
    setValue(get :+ v)
  }
  def add(itemInit: F => Any) {
    val item = factory()
    itemInit(item)
    add(item)
  }

  protected def checkAndSetNewKey(v: F) {
    if (v.isNew) {
      lastNewKey = keyOps.increment(lastNewKey)
      v.key = lastNewKey.asInstanceOf[v.Key]
    }
  }

  // ------------------------------- Builder & validations -------------------------------

  /** Стандартное количество элементов при пустом значении этого поля. Т.е., сколько пустых подформ нужно создать по-дефолту */
  def defaultItems(v: Int): this.type = {defaultItems = v; this}
  /** Проверка на минимальное количество элементов (подформ) */
  def minItems(v: Int): this.type = {minItems = Some(v); this}
  /** Проверка на максимальное количество элементов (подформ) */
  def maxItems(v: Int): this.type = {maxItems = Some(v); this}
  /** Установить фиксированное количество элементов. */
  def fixedItems(v: Int): this.type = {defaultItems = v; minItems = Some(v); maxItems = Some(v); this}
  /** Каждая подформа должна иметь уникальное значение этого поля. */
  def uniqueBy(v: F => Field[_], errorMessage: String): this.type = {uniqueBy = Some((v, errorMessage)); this}
  /** Явно заданный templateId. Полезно для подформ в подформах. */
  def templateId(v: String): this.type = {customTemplateId = Some(v); this}

  /**
    * Проверки, специфичные для конкретной реализации Field.
    * Эти проверки не включают в себя список constraints, и не должны их вызывать или дублировать.
    */
  override def validateFieldOnly: ValidationResult = {
    if (minItems.exists(get.size < _)) return Invalid(form.strings.noLessThanError(recordPlural(minItems.get).str))
    if (maxItems.exists(get.size > _)) return Invalid(form.strings.noMoreThanError(recordPlural(maxItems.get).str))

    // Проверка на уникальное значение поля у всех заполненных подформ uniqueBy
    for ((fn, errorMsg) <- uniqueBy) {
      val set = mutable.Set[Any]()
      if (!get.forall {form =>
        val field: Field[_] = fn(form)
        if (field.isEmpty) true
        else set.add(field.get)
      }) return Invalid(errorMsg)
    }

    // Проверка на уникальные ключи форм (если они не 0).
    locally {
      val set = mutable.Set[Key]()
      if (!get.forall(form => if (form.key != 0) set.add(form.key) else true))
        return Invalid("Non-unique subform key")
    }

    Valid
  }

  /**
    * Применить или зафиксировать значение поля. Это действие вызывается после поста и валидации, и перед сохранением формы.
    * Пример действия - для полей UploadField старые файлы удаляются, а новые переносятся из временных в постоянные.
    * Также, это действие вызывается и после удаления формы для всех её полей (с установленным флагом formRemoved)
    *
    * @param formRemoved Флаг устанавливается, если этот метод был вызван для формы, которая удалена. Очень полезно при очистке полей за собой.
    */
  override def applyValues(formRemoved: Boolean): FormResult = {
    val result = FormErrors()
    locally {
      var idx = 0
      val values: Vector[F] = get
      while (idx < values.length) {
        result.addSub(values(idx).applyValues(formRemoved), name = shortId, index = idx)
        idx += 1
      }
    }
    removeOldForms.values.foreach(v => result.addSub(v.applyValues(formRemoved = true), name = shortId, index = 0))
    result.orSuccess
  }
}


trait FormListKeyOps[Key] {
  def zeroKey: Key
  def fromJsonNode(@Nullable node: JsonNode): Key
  def increment(v: Key): Key
}

object IntFormListKeyOps extends FormListKeyOps[Int] {
  override def zeroKey: Int = 0
  override def fromJsonNode(node: JsonNode): Int = if (node == null) 0 else node.asInt(0)
  override def increment(v: Int): Int = v + 1
}
