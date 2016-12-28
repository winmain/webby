package webby.form
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonAutoDetect, JsonInclude, JsonProperty}
import webby.api.mvc.{PlainResult, Results}

import scala.collection.mutable

trait FormResult {
  def plainResult: PlainResult = Results.JsonOk(this)
  def &&(next: => FormResult): FormResult
  def ++(another: FormErrors): FormErrors
  def ++(another: FormResult): FormResult
}

// ------------------------------- Form success classes -------------------------------

trait FormSuccess extends FormResult {
  override def &&(next: => FormResult): FormResult = next
  override def ++(another: FormErrors): FormErrors = another
  override def ++(another: FormResult): FormResult = another
}

case object FormSuccess extends FormResult with FormSuccess {@JsonProperty val success = true}

//
// Дополнительные методы (форма с сообщением, редирект) описаны в объекте JsActionResult
//

// ------------------------------- Form error classes -------------------------------

/**
 * Ошибки формы
 *
 * @param name Имя подформы (только для ошибок в подформах)
 * @param index Индекс подформы в списке (только для ошибок в подформах)
 * @param errors Ошибки полей этой формы/подформы
 * @param required Список пустых полей, обязательных для заполнения
 * @param selfErrors Ошибки самой формы (сюда не входят ошибки полей)
 * @param sub Ошибки в подформах. Каждая запись в sub должна иметь установленные поля name, index.
 */
@JsonInclude(Include.NON_EMPTY)
@JsonAutoDetect(isGetterVisibility = Visibility.NONE, getterVisibility = Visibility.NONE)
case class FormErrors(@JsonProperty var name: String = null,
                      @JsonProperty var index: Option[Int] = None,
                      @JsonProperty errors: mutable.Map[String, String] = mutable.Map.empty,
                      @JsonProperty required: mutable.Buffer[String] = mutable.Buffer.empty,
                      @JsonProperty selfErrors: mutable.Buffer[String] = mutable.Buffer.empty,
                      @JsonProperty sub: mutable.Buffer[FormErrors] = mutable.Buffer.empty) extends FormResult {
  override def &&(next: => FormResult): FormResult = this
  override def ++(another: FormErrors): FormErrors = { this ++= another; this }
  override def ++(another: FormResult): FormResult = { this ++= another; this }

  def ++=(another: FormErrors) {
    errors ++= another.errors
    required ++= another.required
    sub ++= another.sub
    selfErrors ++= another.selfErrors
  }
  def ++=(another: FormResult) {
    another match {
      case e: FormErrors => ++=(e)
      case _ => ()
    }
  }

  def addSub(another: FormResult, name: String, index: Int) {
    another match {
      case FormSuccess => ()
      case e: FormErrors =>
        e.name = name
        e.index = Some(index)
        sub += e
    }
  }

  def isEmpty: Boolean = errors.isEmpty && required.isEmpty && sub.isEmpty && selfErrors.isEmpty
  def orSuccess: FormResult = if (isEmpty) FormSuccess else this
}
