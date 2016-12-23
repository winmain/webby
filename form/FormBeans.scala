package lib.form
import lib.form.field.Field

/**
  * Bean контекст, используемый в бинах для форм.
  * Бины используются как транспортные объекты, для генерации урлов в формах.
  *
  * Пример использования:
  * {{{
  * class FilterForm extends Form {
  *   val vacId = intField("vacId").enterKeySubmit
  *   val pager = pagerField(10)
  * }
  *
  * case class FilterBean(@JsonIgnore @JsonProperty("_") ctx: FormBeanCtx[FilterForm, FilterBean] = StubFormBeanCtx())(
  *   @JsonProperty("vacId") var vacId: Int = ctx(_.vacId, _.vacId),
  *   @JsonProperty("page") var page: Int = ctx(_.pager, _.page))
  * }}}
  *
  * Чтобы создать `FilterBean`: `FilterBean()(vacId = 5)`.
  * Преобразование формы в бин: `FilterBean(ReadFormBeanCtx(form))()`
  * Заполнение формы бином: `FilterBean(FillFormBeanCtx(form, bean))()`
  *
  * @tparam F Тип формы
  * @tparam B Тип bean
  */
trait FormBeanCtx[F <: Form, B] {
  def apply[T](fieldFn: F => Field[T], fromData: B => T): T
}

case class StubFormBeanCtx[F <: Form, B]() extends FormBeanCtx[F, B] {
  override def apply[T](fieldFn: (F) => Field[T], fromData: (B) => T): T = null.asInstanceOf[T]
}

case class ReadFormBeanCtx[F <: Form, B](form: F) extends FormBeanCtx[F, B] {
  override def apply[T](fieldFn: (F) => Field[T], fromData: B => T): T = fieldFn(form).get
}

case class FillFormBeanCtx[F <: Form, B](form: F, data: B) extends FormBeanCtx[F, B] {
  override def apply[T](fieldFn: (F) => Field[T], fromData: (B) => T): T = {fieldFn(form).set(fromData(data)); null.asInstanceOf[T]}
}
