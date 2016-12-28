package webby.form

import querio.{DbTrait, ModifyData, MutableTableRecord, TableRecord}
import webby.api.mvc._

class FormReq(val req: Either[Request[Unit], Request[Array[Byte]]],
              val id: Option[Int],
              db: DbTrait) {
  def isGet: Boolean = req.isLeft
  def isPost: Boolean = req.isRight
  def isNew: Boolean = id.isEmpty
  def isEdit: Boolean = id.nonEmpty

  /**
    * Класс для запуска цикла чтения данных из поста форм, сохранения в БД, и рендеринга результата.
    *
    * modifyData - Функция генерации ModifyData для сохранения формы
    * showForm - Рендеринг формы в случае, если поста не было
    * onSuccess - Вызывается в случае успешного поста.
    * > Если данные изменились и были сохранены, то на вход идёт Some(MTR).
    * > Если данные не изменились, то передаётся None
    */
  abstract class DbFormAction[TR <: TableRecord, MTR <: MutableTableRecord[TR]](form: FormWithDb[TR, MTR]) {
    private var _md: ModifyData = null
    protected def md: ModifyData = _md

    /** Если указан, то берём запись отсюда (вместо загрузки из БД) */
    def maybeRecord: Option[TR] = None

    def modifyData(req: Request[_]): ModifyData
    def showForm: PlainResult
    def onSuccess(maybeRecord: Option[MTR]): FormResult

    /**
      * Запустить цикл обработки формы
      */
    def process: PlainResult = {
      if (isNew && isGet) showForm
      else {
        db.dataTrSerializable(null) {implicit dt =>
          maybeRecord match {
            case None => id.foreach(form.loadOrNotFoundRaw(_)(dt))
            case Some(record) =>
              if (!form.checkLoadedRecord.forall(_ (record))) throw ResultException(Results.NotFoundRaw)
              form.load(record)
          }

          req match {
            case Left(r) =>
              showForm

            case Right(r) =>
              form.onPost(r.body) {
                if (form.changed) {
                  _md = modifyData(r)
                  dt.updateMd(md)
                  val record = form.save()
                  onSuccess(Some(record))
                } else
                  onSuccess(None)
              }
          }
        }
      }
    }
  }

  /**
    * Запустить цикл чтения данных из поста форм, сохранения в БД, и рендеринга результата.
    *
    * @param form         Форма
    * @param showFormFn   Рендеринг формы в случае, если поста не было
    * @param modifyDataFn Функция генерации ModifyData для сохранения формы
    * @param onSuccessFn  Вызывается в случае успешного поста.
    *                     Если данные изменились и были сохранены, то возвращается Some(MTR).
    *                     Если данные не изменились, то вернётся None(MTR)
    * @return Результат для Action
    */
  def dbFormAction[TR <: TableRecord, MTR <: MutableTableRecord[TR]]
  (form: FormWithDb[TR, MTR],
   showFormFn: => PlainResult,
   modifyDataFn: Request[_] => ModifyData)
  (onSuccessFn: Option[MTR] => FormResult): PlainResult =
    new DbFormAction[TR, MTR](form) {
      override def modifyData(req: Request[_]): ModifyData = modifyDataFn(req)
      override def showForm: PlainResult = showFormFn
      override def onSuccess(maybeRecord: Option[MTR]): FormResult = onSuccessFn(maybeRecord)
    }.process

  /**
    * Вариант [[dbFormAction()]], который работает только с POST-запросом.
    */
  def dbFormPost[TR <: TableRecord, MTR <: MutableTableRecord[TR]]
  (form: FormWithDb[TR, MTR],
   modifyDataFn: Request[_] => ModifyData)
  (onSuccessFn: Option[MTR] => FormResult): PlainResult =
    dbFormAction(form, sys.error("Unimplemented"), modifyDataFn)(onSuccessFn)


  abstract class SimpleFormActionPlainSuccess(form: Form) {
    def showForm: PlainResult
    def onPlainSuccess: PlainResult

    def process: PlainResult = {
      if (isNew && isGet) showForm
      else {
        req match {
          case Left(r) => showForm
          case Right(r) => form.onPostPlainResult(r.body)(onPlainSuccess)
        }
      }
    }
  }
  abstract class SimpleFormAction(form: Form) extends SimpleFormActionPlainSuccess(form) {
    override def onPlainSuccess: PlainResult = onSuccess.plainResult
    def onSuccess: FormResult
  }

  def simpleFormAction(form: Form, showFormFn: => PlainResult)(onSuccessFn: => FormResult): PlainResult =
    new SimpleFormAction(form) {
      override def showForm: PlainResult = showFormFn
      override def onSuccess: FormResult = onSuccessFn
    }.process

  def simpleFormActionPlainSuccess(form: Form, showFormFn: => PlainResult)(onSuccessFn: => PlainResult): PlainResult =
    new SimpleFormActionPlainSuccess(form) {
      override def showForm: PlainResult = showFormFn
      override def onPlainSuccess: PlainResult = onSuccessFn
    }.process
}
