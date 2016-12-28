package webby.form.field
import querio._
import webby.commons.text.Plural
import webby.form.{Form, FormWithDb}

/**
  * Trait, включающий в себя объявления всех типов полей для удобного описания формы через DSL.
  */
trait FormFields {self: Form =>
  protected def addField[F <: Field[_]](field: F): F
  protected def addFormFieldWithDb[F <: FormListFieldWithDb[_, _, _]](field: F): F

  // ----------- !!! Все методы, возвращающие конструкторы полей должны быть обрамлены в addField() !!!

  protected def formList[F <: Form](id: String, factory: () => F) = addField(new FormListField(id, factory, base.recordRPlural))

  protected def formListWithDbLinked[F <: FormWithDb[TR, MTR], TR <: TableRecord, MTR <: MutableTableRecord[TR]]
  (id: String, factory: => F, parentField: Table[TR, MTR]#Field[Int, Int]) =
    addFormFieldWithDb(new FormListFieldWithDbLinked[F, TR, MTR, Table[TR, MTR]#Field[Int, Int]](
      base, id, () => factory, parentField, _.set(_, _), base.recordRPlural))

  protected def formListWithDbLinkedOpt[F <: FormWithDb[TR, MTR], TR <: TableRecord, MTR <: MutableTableRecord[TR]]
  (id: String, factory: => F, parentField: Table[TR, MTR]#Field[Int, Option[Int]]) =
    addFormFieldWithDb(new FormListFieldWithDbLinked[F, TR, MTR, Table[TR, MTR]#Field[Int, Option[Int]]](
      base, id, () => factory, parentField, (f, r, p) => f.set(r, Some(p)), base.recordRPlural))

  protected def formListWithDbStandalone[F <: FormWithDb[TR, MTR], TR <: TableRecord, MTR <: MutableTableRecord[TR]]
  (table: Table[TR, MTR])(id: String, factory: => F) =
    addFormFieldWithDb(new FormListFieldWithDbStandalone[F, TR, MTR](
      base, id, () => factory, base.recordRPlural))

  protected def textField(id: String) = addField(new TextField(id))
  protected def longTextField(id: String) = addField(new TextField(id).maxLength(base.longTextFieldMaxLength))
  protected def checkField(name: String, makeUniqueInputId: Boolean = false) = addField(new CheckField(name, makeUniqueInputId))
  protected def intField(id: String) = addField(new IntField(id))
  protected def longField(id: String) = addField(new LongField(id))
  protected def dateField(id: String) = addField(new DateField(id))
  protected def monthYearField(id: String) = addField(new MonthYearField(id))
  protected def phoneField(id: String) = addField(new PhoneField(id))
  protected def maskedField(id: String, mask: String) = addField(new MaskedField(id, mask))
  protected def fioField(id: String) = addField(new RusFioField(id))
  protected def emailField(id: String) = addField(new EmailField(id))
  protected def passwordField(id: String, hashFn: String => String) = addField(new PasswordField(id, hashFn))
  protected def urlField(id: String, allowedDomains: Vector[String] = Vector.empty) = addField(new UrlField(id, allowedDomains))
  protected def hiddenField(id: String) = addField(new HiddenField(id))
  protected def hiddenIntField(id: String) = addField(new HiddenIntField(id))
  protected def hiddenBooleanField(id: String) = addField(new HiddenBooleanField(id))

  protected def pagerField(step: Int, nearRadius: Int = 2, id: String = "page"): PagerField = addField(new PagerField(step, nearRadius, id))

  protected def radioGroupField[T](id: String, items: Iterable[T], valueFn: T => String, titleFn: T => String, emptyTitle: Option[String] = None) =
    addField(new RadioGroupField(id, items, valueFn = valueFn, titleFn = titleFn, emptyTitle = emptyTitle))
  protected def radioGroupEnumField[E <: DbEnum](id: String, enum: E)(titleFn: E#V => String, emptyTitle: Option[String] = None) =
    addField(new RadioGroupField[E#V](id, enum.values, valueFn = _.getId.toString, titleFn = titleFn, emptyTitle = emptyTitle))
  protected def radioGroupOldEnumField[E <: ScalaDbEnumCls[E]](id: String, enum: ScalaDbEnum[E])(titleFn: E => String, emptyTitle: Option[String] = None) =
    addField(new RadioGroupField[E](id, enum.values, valueFn = _.getDbValue, titleFn = titleFn, emptyTitle = emptyTitle))

  protected def selectField[T](id: String, items: Iterable[T], valueFn: T => String, titleFn: T => String, emptyTitle: Option[String] = None) =
    addField(new SelectField[T](id, items, valueFn = valueFn, titleFn = titleFn, emptyTitle = emptyTitle))
  protected def selectEnumField[E <: DbEnum](id: String, enum: E)(titleFn: E#V => String, values: Iterable[E#V] = enum.values, emptyTitle: Option[String] = None) =
    addField(new SelectField[E#V](id, values, valueFn = _.getId.toString, titleFn = titleFn, emptyTitle = emptyTitle))
  protected def selectOldEnumField[E <: ScalaDbEnumCls[E]](id: String, enum: ScalaDbEnum[E])(titleFn: E => String, values: Iterable[E] = enum.values, emptyTitle: Option[String] = None) =
    addField(new SelectField[E](id, values, valueFn = _.getDbValue, titleFn = titleFn, emptyTitle = emptyTitle))

  protected def checkListField[T](id: String, items: Iterable[T], valueFn: T => String, titleFn: T => String, commentFn: T => String = null) =
    addField(new CheckListField[T](id, items, valueFn = valueFn, titleFn = titleFn, commentFn = commentFn))
  protected def checkListEnumField[E <: ScalaDbEnumCls[E]](id: String, enum: ScalaDbEnum[E])(titleFn: E => String, commentFn: E => String = null) =
    addField(new CheckListField[E](id, enum.values, valueFn = _.getDbValue, titleFn = titleFn, commentFn = commentFn))

  protected def autocompleteField[T](id: String, jsSourceFunction: String, jsSourceArg: Any = null, toJs: T => Int, fromJs: Int => Option[T], addRendererCls: String = null) =
    addField(new AutocompleteField[T](id, jsSourceFunction = jsSourceFunction, jsSourceArg = jsSourceArg, toJs = toJs, fromJs = fromJs, addRendererCls = addRendererCls))
  protected def autocompleteListField[T](id: String, jsSourceFunction: String, jsSourceArg: Any = null, toJs: T => Int, fromJs: Int => Option[T], plural: Plural = base.recordRPlural) =
    addField(new AutocompleteListField[T](id, jsSourceFunction = jsSourceFunction, jsSourceArg = jsSourceArg, toJs = toJs, fromJs = fromJs, plural))
  protected def autocompleteTextField(id: String, jsSourceFunction: String, jsSourceArg: Any = null) =
    addField(new AutocompleteTextField(id, jsSourceFunction = jsSourceFunction, jsSourceArg = jsSourceArg))
}
