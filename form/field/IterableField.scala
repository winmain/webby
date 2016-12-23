package lib.form.field
import lib.form.FormWithDb
import querio.{SubTableList, _}

trait IterableField[T] extends Field[Iterable[T]] {

  def connectIntSubTable[TR <: TableRecord, MTR <: MutableTableRecord[TR], STR <: TableRecord, SMTR <: MutableTableRecord[STR]]
  (_parentTable: Table[TR, MTR], updater: SubTableUpdater[STR, SMTR, Int])
  (getSubTable: TR => SubTableList[STR, SMTR], getT: STR => T, toS: T => Int)
  (implicit form: FormWithDb[TR, MTR]): this.type =
    dbConnector[TR, MTR](new DbSetSubTableConnector[T, TR, MTR, Int, STR, SMTR](this, getSubTable, getT, updater, toS))

}
