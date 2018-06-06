package webby.form.field
import enumeratum.values.{IntEnum, IntEnumEntry}
import querio._
import webby.api.mvc.RequestHeader
import webby.commons.text.Plural
import webby.form.field.autocomplete.{AutocompleteField, AutocompleteListField, AutocompleteTextField}
import webby.form.field.recaptcha.{ReCaptcha, ReCaptchaField}
import webby.form.{Form, FormWithDb, SubForm}

/**
  * Trait, включающий в себя объявления всех типов полей для удобного описания формы через DSL.
  */
trait StdFormFields {self: Form =>
  protected def addField[F <: Field[_]](field: F): F
  protected def addFormFieldWithDb[F <: FormListFieldWithDb[_, _, _]](field: F): F

  // ----------- !!! Все методы, возвращающие конструкторы полей должны быть обрамлены в addField() !!!

  protected def formList[F <: SubForm](id: String, factory: => F) = addField(new FormListField(this, id, () => factory, strings.recordRPlural))

  protected def formListWithDbLinked[F <: FormWithDb[TR, MTR] with SubForm, TR <: TableRecord, MTR <: MutableTableRecord[TR]]
  (id: String,
   factory: => F,
   parentField: Table[TR, MTR]#Field[Int, Int],
   sortRecords: Vector[TR] => Vector[TR] = null) =
    addFormFieldWithDb(new FormListFieldWithDbLinked[F, TR, MTR, Table[TR, MTR]#Field[Int, Int]](
      this, id, () => factory, parentField, _.set(_, _), strings.recordRPlural, sortRecords))

  protected def formListWithDbLinkedOpt[F <: FormWithDb[TR, MTR] with SubForm, TR <: TableRecord, MTR <: MutableTableRecord[TR]]
  (id: String,
   factory: => F,
   parentField: Table[TR, MTR]#Field[Int, Option[Int]],
   sortRecords: Vector[TR] => Vector[TR] = null) =
    addFormFieldWithDb(new FormListFieldWithDbLinked[F, TR, MTR, Table[TR, MTR]#Field[Int, Option[Int]]](
      this, id, () => factory, parentField, (f, r, p) => f.set(r, Some(p)), strings.recordRPlural, sortRecords))

  protected def formListWithDbStandalone[F <: FormWithDb[TR, MTR] with SubForm, TR <: TableRecord, MTR <: MutableTableRecord[TR]]
  (table: Table[TR, MTR])
  (id: String, factory: => F) =
    addFormFieldWithDb(new FormListFieldWithDbStandalone[F, TR, MTR](
      this, id, () => factory, strings.recordRPlural))

  protected def textField(id: String) = addField(new TextField(this, id))
  protected def checkField(name: String, makeUniqueInputId: Boolean = false) = addField(new CheckField(this, name, makeUniqueInputId))
  protected def intField(id: String) = addField(new IntField(this, id))
  protected def longField(id: String) = addField(new LongField(this, id))
  protected def floatField(id: String) = addField(new FloatField(this, id))
  protected def dateField(id: String) = addField(new RuDateField(this, id))
  protected def ruMonthYearField(id: String) = addField(new RuMonthYearField(this, id))
  protected def maskedField(id: String, mask: String) = addField(new MaskedField(this, id, mask))
  protected def emailField(id: String) = addField(new EmailField(this, id))
  protected def passwordField(id: String, hashFn: String => String) = addField(new PasswordField(this, id, hashFn))
  protected def urlField(id: String, allowedDomains: Vector[String] = Vector.empty) = addField(new UrlField(this, id, allowedDomains))

  protected def hiddenField(id: String) = addField(new HiddenField(this, id))
  protected def hiddenIntField(id: String) = addField(new HiddenIntField(this, id))
  protected def hiddenBooleanField(id: String) = addField(new HiddenBooleanField(this, id))

  protected def pagerField(step: Int, nearRadius: Int = 2, id: String = "page"): PagerField = addField(new PagerField(this, step, nearRadius, id))

  protected def radioGroupField[T](id: String, items: Iterable[T], valueFn: T => String, titleFn: T => String, emptyTitle: Option[String] = None) =
    addField(new RadioGroupField(this, id, items, valueFn = valueFn, titleFn = titleFn, emptyTitle = emptyTitle))
  protected def radioGroupEnumField[EE <: IntEnumEntry](id: String, enum: IntEnum[EE])(titleFn: EE => String, emptyTitle: Option[String] = None) =
    addField(new RadioGroupField[EE](this, id, enum.values, valueFn = _.value.toString, titleFn = titleFn, emptyTitle = emptyTitle))
  protected def radioGroupDbEnumField[E <: DbEnum](id: String, enum: E)(titleFn: E#V => String, emptyTitle: Option[String] = None) =
    addField(new RadioGroupField[E#V](this, id, enum.values, valueFn = _.getId.toString, titleFn = titleFn, emptyTitle = emptyTitle))
  protected def radioGroupOldEnumField[E <: ScalaDbEnumCls[E]](id: String, enum: ScalaDbEnum[E])(titleFn: E => String, emptyTitle: Option[String] = None) =
    addField(new RadioGroupField[E](this, id, enum.values, valueFn = _.getDbValue, titleFn = titleFn, emptyTitle = emptyTitle))

  protected def richSelectField[T](id: String, items: Iterable[T], valueFn: T => String, titleFn: T => String, emptyTitle: Option[String] = None) =
    addField(new RichSelectField[T](this, id, items, valueFn = valueFn, titleFn = titleFn, emptyTitle = emptyTitle))
  protected def richSelectEnumField[EE <: IntEnumEntry](id: String, enum: IntEnum[EE])(titleFn: EE => String, values: Iterable[EE] = enum.values, emptyTitle: Option[String] = None) =
    addField(new RichSelectField[EE](this, id, values, valueFn = _.value.toString, titleFn = titleFn, emptyTitle = emptyTitle))
  protected def richSelectDbEnumField[E <: DbEnum](id: String, enum: E)(titleFn: E#V => String, values: Iterable[E#V] = enum.values, emptyTitle: Option[String] = None) =
    addField(new RichSelectField[E#V](this, id, values, valueFn = _.getId.toString, titleFn = titleFn, emptyTitle = emptyTitle))

  protected def checkListField[T](id: String, items: Iterable[T], valueFn: T => String, titleFn: T => String, commentFn: T => String = null) =
    addField(new CheckListField[T](this, id, items, valueFn = valueFn, titleFn = titleFn, commentFn = commentFn))
  protected def checkListEnumField[EE <: IntEnumEntry](id: String, enum: IntEnum[EE])(titleFn: EE => String, commentFn: EE => String = null) =
    addField(new CheckListField[EE](this, id, enum.values, valueFn = _.value.toString, titleFn = titleFn, commentFn = commentFn))
  protected def checkListOldEnumField[E <: ScalaDbEnumCls[E]](id: String, enum: ScalaDbEnum[E])(titleFn: E => String, commentFn: E => String = null) =
    addField(new CheckListField[E](this, id, enum.values, valueFn = _.getDbValue, titleFn = titleFn, commentFn = commentFn))

  protected def autocompleteField[T](id: String, jsSourceFunction: String, jsSourceArg: Any = null, toJs: T => Int, fromJs: Int => Option[T], addRendererCls: String = null) =
    addField(new AutocompleteField[T](this, id, jsSourceFunction = jsSourceFunction, jsSourceArg = jsSourceArg, toJs = toJs, fromJs = fromJs, addRendererCls = addRendererCls))
  protected def autocompleteListField[T](id: String, jsSourceFunction: String, jsSourceArg: Any = null, toJs: T => Int, fromJs: Int => Option[T], recordPlural: Plural = strings.recordRPlural) =
    addField(new AutocompleteListField[T](this, id, jsSourceFunction = jsSourceFunction, jsSourceArg = jsSourceArg, toJs = toJs, fromJs = fromJs, recordPlural))
  protected def autocompleteTextField(id: String, jsSourceFunction: String, jsSourceArg: Any = null) =
    addField(new AutocompleteTextField(this, id, jsSourceFunction = jsSourceFunction, jsSourceArg = jsSourceArg))

  protected def reCaptchaField(reCaptcha: ReCaptcha)(implicit rh: RequestHeader) = addField(new ReCaptchaField(this, reCaptcha)(rh))
}
