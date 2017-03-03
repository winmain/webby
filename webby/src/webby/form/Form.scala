package webby.form

import java.sql.SQLException
import java.util
import javax.annotation.Nullable

import com.fasterxml.jackson.annotation.{JsonInclude, JsonRawValue}
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import webby.api.mvc.{PlainResult, ResultException, Results}
import webby.commons.io.StdJs
import webby.form.field.{Field, StdFormFields, FormListField, FormListFieldWithDb}
import webby.form.jsrule.{StdFormJsRules, JsRule}
import webby.html.{CommonTag, StdFormTag, StdHtmlView, WebbyPage}

import scala.collection.mutable
import scala.util.control.ControlThrowable

/**
  * Trait для описания форм
  */
trait Form extends StdFormFields with StdFormJsRules {self =>
  type B <: BaseForms
  def base: B

  /**
    * Ключ подформы. Нужен для связи подформы с подтаблицей. По сути, это id подтаблицы.
    * Если подформа новая, то ключ должен быть равен 0.
    */
  var key: Int = 0

  def isNew: Boolean = key == 0

  /** Подтверждать закрытие/смену страницы специальным js-оповещением? */
  var onUnloadConfirm = true

  /** Не показывать форму в случае успешной инициализации? */
  var hidden = false

  /** Флаг, устанавливающий у js формы Form.initialFilled = true после заполнения полей.
    * Этот флаг нужно сбрасывать, чтобы при простом сохранении без изменений форма считала, что изменения были.
    * Т.е., после заполнения полей форма не считает это состояние первоначальным. А считает его изменённым.
    * Это применяется для копирования вакансий.
    */
  var initialFilled = true

  /** Поле, которое будет в фокусе после инициализации формы. */
  @Nullable
  var focusField: Field[_] = null

  /** Вызывать form.submit() после первой инициализации формы? Применяется, когда форма является фильтром в выборке. */
  var submitAfterInit = false

  /**
    * Действия, выполняемые после успешного прохождения post'а и валидации формы, но до applyValues().
    */
  val afterValidPost: mutable.Buffer[() => FormResult] = mutable.Buffer.empty

  /**
    * Список ограничений и проверок формы
    */
  val constraints = mutable.Buffer[Form => FormResult]()

  def addConstraint(c: Form => FormResult): Unit = constraints += c
  def addConstraint(c: => FormResult): Unit = constraints += {_ => c}
  def addErrorIf(errorCondition: => Boolean, onConditionFailed: => FormErrors): Unit =
    constraints += {_ => if (errorCondition) onConditionFailed else FormSuccess}
  def addValidIf(validCondition: => Boolean, onConditionFailed: => FormErrors): Unit =
    constraints += {_ => if (validCondition) FormSuccess else onConditionFailed}

  /** Список полей формы (поля подформ здесь не указываются) */
  val fields = mutable.Buffer.empty[Field[_]]
  /** Список полей-подформ. Все эти поля также входят в список fields. */
  val formFieldsWithDb = mutable.Buffer.empty[FormListFieldWithDb[_, _, _]]
  override protected def addField[F <: Field[_]](field: F): F = {fields += field; field}
  override protected def addFormFieldWithDb[F <: FormListFieldWithDb[_, _, _]](field: F): F
  = {fields += field; formFieldsWithDb += field; field}

  /**
    * Набор полей для вычисления данных формы. Данные вычисляются как только пройдут
    * все проверки полей и constraints формы во время поста.
    * Результат вычисленных данных записывается в переменную data.
    * Т.е., это проверки полей + вычисление и хранение полученных значений. В случае ошибки метод
    * error() бросает ErrorStopException и вычисление прерывается.
    * Пример использования:
    * {{{
    *  class Data extends BaseData {
    *    val sum = myField.getOpt.map(_ + 10).getOrElse(error(myField.error("Значение не указано"))))
    *  }
    *  override def makeData = new Data
    *  // Результат будет вычислен и сохранён в переменной data
    * }}}
    */
  type Compute <: BaseCompute
  class BaseCompute {
    protected def error(err: FormErrors): Nothing = throw new ErrorStopException(err)
  }
  class ErrorStopException(val errors: FormErrors) extends ControlThrowable
  var computed: Compute = null.asInstanceOf[Compute]
  def makeComputed: Compute = null.asInstanceOf[Compute]

  def jsMapper: ObjectMapper = StdJs.get.mapper

  def parseJsonTree(bytes: Array[Byte]): JsonNode = {
    val tree: JsonNode =
      try jsMapper.readTree(bytes)
      catch {case e: Exception => throw ResultException(Results.BadRequest(e.toString))}
    if (!tree.isObject) throw ResultException(Results.BadRequest("Not a json object"))
    tree
  }

  private def runAfterValidPosts(): FormResult = {
    for (fn <- afterValidPost) {
      fn() match {
        case _@FormSuccess => // ok, just move next
        case fail => return fail
      }
    }
    FormSuccess
  }

  def onPostUnsafe(request: Array[Byte])(onSuccess: => PlainResult): PlainResult = {
    val tree: JsonNode = parseJsonTree(request)
    // Пост формы
    if (tree.has("post")) {
      val postNode: JsonNode = tree.get("post")
      if (postNode != null && !postNode.isNull && !postNode.isObject) throw ResultException(Results.BadRequest("Post not a json object"))
      prepareBeforePost()
      setJsValuesAndValidate(postNode, FormErrors()) && runAfterValidPosts() match {
        case _@FormSuccess =>
          applyValues(formRemoved = false) match {
            case FormSuccess => onSuccess
            case r => r.plainResult
          }
        case fail => fail.plainResult
      }
    } else {
      // Дополнительное взаимодействие с полями (connectedAction)
      val fieldPath = tree.get("field")
      resolveField(fieldPath) match {
        case Some(f) => f.connectedAction(tree)
        case None => Results.BadRequest("Invalid field")
      }
    }
  }

  def onPostPlainResult(request: Array[Byte])(onSuccess: => PlainResult): PlainResult = {
    try onPostUnsafe(request)(onSuccess)
    catch {
      case e: SQLException => throw e // пробросить далее, чтобы её поймал блок Db.transaction
      case e: Throwable => wrapPostException(e)
    }
  }
  def onPost(request: Array[Byte])(onSuccess: => FormResult): PlainResult =
    onPostPlainResult(request)(onSuccess.plainResult)

  def wrapPostException(e: Throwable): Nothing = {
    ResultException.wrap(e)(selfError("Ошибка сервера. Пожалуйста, обратитесь в службу поддержки.").plainResult).raise
  }

  /**
    * Чтение присланных данных, разборка и проверка полей
    *
    * @param tree       Присланные значения от формы. Если null или NullNode, то значения не изменились.
    * @param formErrors Ошибки, которые будут накоплены при чтении данных.
    */
  def setJsValuesAndValidate(tree: JsonNode, formErrors: FormErrors): FormResult = {
    val nullTree = tree == null || tree.isNull
    // Прочитать и проверить все поля
    for (field <- fields if !field.ignored && field.enabled) {
      (if (nullTree) field.validate else field.setJsValueAndValidate(tree.get(field.id))) match {
        case e: FormErrors => formErrors ++= e
        case _ => ()
      }
    }
    // Применить правила rules и ещё раз перепроверить все поля, если что-то поменяется.
    if (formErrors.isEmpty) {
      var needRevalidate = false
      for {rule <- serverRules
           turnOn = rule.cond.check
           action <- rule.actions} {
        action.execute(turnOn)
        needRevalidate = true
      }
      if (needRevalidate)
        for (field <- fields if !field.ignored && field.enabled)
          formErrors ++= field.validate
    }

    // Проверить все ограничения формы. Проверки идут только если других ошибок в форме нет.
    if (formErrors.isEmpty) constraints.foreach(constraint => formErrors ++= constraint(this))
    // Вычислить и проверить дополнительные значения.
    if (formErrors.isEmpty) try computed = makeComputed catch {case e: ErrorStopException => formErrors ++= e.errors}
    formErrors.orSuccess
  }

  /**
    * Вернуть поле по пути, указанному в js.
    * Примеры пути: "fio"; {"phones": "phone"}
    */
  def resolveField(tree: JsonNode): Option[Field[_]] = {
    if (tree == null) None
    else if (tree.isObject) {
      // Подформа
      val names: util.Iterator[String] = tree.fieldNames()
      if (!names.hasNext) return None
      val name: String = names.next()
      fields.find(_.id == name) match {
        case Some(listField: FormListField[_]) =>
          val form = listField.formStub
          form.resolveField(tree.get(name))
        case _ => None
      }
    } else if (tree.isTextual) {
      // Обычное поле
      val name: String = tree.asText()
      fields.find(_.id == name)
    } else None
  }

  /**
    * Форма считается изменённой, если у неё изменилось хотябы одно поле.
    */
  def changed: Boolean = fields.exists(_.changed)

  /**
    * Подготовиться к приёму POST данных: сбросить флаги changed у всех полей.
    */
  def prepareBeforePost(): Unit = fields.foreach(_.prepareBeforePost())

  /**
    * Применить или зафиксировать значение поля. Это действие вызывается после поста и валидации, и перед сохранением формы.
    * Пример действия - для полей UploadField старые файлы удаляются, а новые переносятся из временных в постоянные.
    * Также, это действие вызывается и после удаления формы для всех её полей (с установленным флагом formRemoved)
    *
    * @param formRemoved Флаг устанавливается, если этот метод был вызван для формы, которая удалена. Очень полезно при очистке полей за собой.
    */
  def applyValues(formRemoved: Boolean): FormResult = {
    var result: FormResult = FormSuccess
    for (field <- fields) result ++= field.applyValues(formRemoved)
    result
  }

  /** Создать и вернуть ошибку для этой формы */
  def selfError(message: String): FormErrors = FormErrors(selfErrors = mutable.Buffer(message))

  // ------------------------------- Js rules -------------------------------

  val clientRules = new mutable.ArrayBuffer[JsRule]()
  val serverRules = new mutable.ArrayBuffer[JsRule]()

  override def addRule0(@Nullable clientRule: JsRule, @Nullable serverRule: JsRule): Unit = {
    if (clientRule != null) clientRules += clientRule
    if (serverRule != null) serverRules += serverRule
  }

  // ------------------------------- Js properties -------------------------------

  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  sealed class BaseJsProps {
    val fields: Iterable[_] = self.fields.withFilter(!_.ignored).map(_.jsProps)

    /**
      * Значения для главной формы нужны, чтобы проинициализировать её при редактировании.
      * Значения для подформы (в структуре jsProps) нужны, для первичной инициализации полей в подформе при её создании.
      */
    val values: mutable.OpenHashMap[String, AnyRef] = self.jsValues

    @JsonRawValue val config: String = self.jsConfig
    @JsonRawValue val controller: String = self.jsController
    val rules: Iterable[JsRule] = self.clientRules

    val onUnloadConfirm: java.lang.Boolean = trueOrNull(self.onUnloadConfirm)
    val hidden: java.lang.Boolean = trueOrNull(self.hidden)
    val initialFilled: java.lang.Boolean = trueOrNull(self.initialFilled)
    val focusField: String = if (self.focusField == null) null else self.focusField.name
    val submitAfterInit: java.lang.Boolean = trueOrNull(self.submitAfterInit)

    private def trueOrNull(bool: Boolean): java.lang.Boolean = if (bool) java.lang.Boolean.TRUE else null
  }

  /**
    * Instance of js `FormConfig` class.
    * If not defined, default config will be used (can be overridden by Form.setDefaultConfig).
    * Raw js code.
    */
  def jsConfig: String = null

  /**
    * Js-контроллер форм, обеспечивающий динамику формы. Он должен находиться в package rr.form.controller
    * Пример: rr.form.controller.ResForm
    */
  def jsController: String = null

  def jsProps: Form#BaseJsProps = new BaseJsProps

  def jsValues: mutable.OpenHashMap[String, AnyRef] = {
    val ret = new mutable.OpenHashMap[String, AnyRef]
    ret.put("_key", key.asInstanceOf[AnyRef])
    for (field <- fields if !field.ignored) ret += field.id -> field.toJsValue
    ret
  }

  def changedJsValues: mutable.OpenHashMap[String, AnyRef] = {
    val ret = new mutable.OpenHashMap[String, AnyRef]
    if (key != 0) ret.put("_key", key.asInstanceOf[AnyRef])
    for (field <- fields if !field.ignored && field.changed) ret += field.id -> field.toJsValue
    ret
  }

  // ------------------------------- Html helpers -------------------------------

  def formTag(id: String = "form", method: String = "post")(implicit view: StdHtmlView, page: WebbyPage): StdFormTag =
    base.formTag(page.scripts, this, id, method)

  def group(implicit view: StdHtmlView): CommonTag = view.div.cls(base.formGroupCls)
  def row(implicit view: StdHtmlView): CommonTag = view.div.cls(base.formRowCls)
  def formErrorsBlock(implicit view: StdHtmlView): CommonTag = view.div.cls(base.formErrorsBlockCls)
}
