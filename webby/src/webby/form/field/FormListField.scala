package webby.form.field
import javax.annotation.Nullable

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import webby.commons.text.Plural
import webby.form._
import webby.html.{CommonTag, HtmlBase, StdHtmlView}

import scala.collection.JavaConversions._
import scala.collection.mutable

class FormListField[F <: Form](val form: Form, val id: String, var factory: () => F, var recordPlural: Plural)
  extends Field[Vector[F]] {self =>

  var defaultItems: Int = 0
  var minItems: Option[Int] = None
  var maxItems: Option[Int] = None
  var uniqueBy: Option[(F => Field[_], String)] = None

  /** Специальная карта старых подформ, которые оказались неиспользованы после получения новых значений. Их следует удалить из БД. */
  var removeOldForms: mutable.Map[Int, F] = mutable.Map.empty[Int, F]

  /**
    * Ключ, который был выставлен у последней подформы с нулевым key.
    * Т.е., при вызове метода add() для у FormListField (но не FormListFieldWithDb), если у формы нулевой key,
    * то ей выставляется lastNewKey + 1, и сам lastNewKey увеличивается на 1.
    */
  private var lastNewKey: Int = 0

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
    val uniqueBy = self.uniqueBy.map {case (fn, msg) => fn(f).id -> msg}
    val sub = f.jsProps
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
      FormErrors(errors = mutable.Map(id -> "Некорректное значение формы"))
    } else {
      val formErrors = FormErrors()
      removeOldForms = super.get.map(f => f.key -> f)(scala.collection.breakOut)
      removeOldForms.remove(0) // Нет смысла хранить значения без ключей - их всё равно нет в базе.
      val processedKeys = mutable.Buffer.empty[Int]

      val valueBuilder = Vector.newBuilder[F]
      for ((jsForm, index) <- node.asInstanceOf[ArrayNode].elements().zipWithIndex) {
        val key = {val k = jsForm.get("_key"); if (k == null) 0 else k.asInt(0)}
        // Попытаться найти уже готовую подформу по ключу. Если такой нет, то создать новую.
        val subForm: F =
        if (key != 0) {
          if (processedKeys.contains(key)) return FormErrors(errors = mutable.Map(id -> "Дублирующий ключ формы"))
          processedKeys += key
          removeOldForms.get(key) match {
            case Some(oldForm) => removeOldForms.remove(key); oldForm
            case None => factory()
          }
        } else factory()

        subForm.setJsValuesAndValidate(jsForm, FormErrors(name = id, index = Some(index))) match {
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
    if (v.key == 0) {lastNewKey += 1; v.key = lastNewKey}
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

  /**
    * Проверки, специфичные для конкретной реализации Field.
    * Эти проверки не включают в себя список constraints, и не должны их вызывать или дублировать.
    */
  override def validateFieldOnly: ValidationResult = {
    if (minItems.exists(get.size < _)) return Invalid("Не менее " + recordPlural(minItems.get).str)
    if (maxItems.exists(get.size > _)) return Invalid("Не более " + recordPlural(maxItems.get).str)

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
      val set = mutable.Set[Int]()
      if (!get.forall(form => if (form.key != 0) set.add(form.key) else true))
        return Invalid("Неуникальный ключ подформ")
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
        result.addSub(values(idx).applyValues(formRemoved), name = id, index = idx)
        idx += 1
      }
    }
    removeOldForms.values.foreach(v => result.addSub(v.applyValues(formRemoved = true), name = id, index = 0))
    result.orSuccess
  }

  // ------------------------------- Html helpers -------------------------------

  /**
    * Шаблон подформы, который клонируется при добавлении нового элемента.
    *
    * @param tag         Обрамляющий тег шаблона. Клонируется именно этот тег.
    * @param cls         Класс обрамляющего тега.
    * @param mobileMulti Флаг, который должен быть установлен, если подформа на десктопной версии однострочная,
    *                    а на мобильной она превращается в многострочную. Добавляет дополнительные паддинги в моб. версии.
    * @param body        Тело шаблона,
    */
  def template(tag: String = "p", cls: String = null, mobileMulti: Boolean = false)(body: F => Any)(implicit view: StdHtmlView): HtmlBase =
    view.tag(tag).id(id + "-template").cls("hide").cls(cls).clsIf(mobileMulti, "mobile-multi") < body(formStub)
  def blockTemplate(tag: String = "section", cls: String = "form-block subform")(body: F => Any)(implicit view: StdHtmlView): HtmlBase =
    view.tag(tag).id(id + "-template").cls("hide").cls(cls) {blockRemoveTag; body(formStub)}

  def listPlaceholder(implicit view: StdHtmlView): CommonTag = view.div.id(id + "-list")
  def smallAddTag(implicit view: StdHtmlView): CommonTag = view.a.hrefAnchor.cls("dotted").id(id + "-add")
  def smallRemoveTag(implicit view: StdHtmlView): CommonTag = view.a.hrefAnchor.cls("small-remove").id(removeId)
  def blockAddTag(implicit view: StdHtmlView): CommonTag = view.a.hrefAnchor.cls("block-add").id(id + "-add")
  def blockRemoveTag(implicit view: StdHtmlView): CommonTag = view.a.hrefAnchor.cls("block-remove").id(removeId).title("Удалить блок")

  /** id для html элемента удаления записи */
  def removeId: String = "remove"
}
