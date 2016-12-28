package webby
import querio.{MutableTableRecord, TableRecord}

package object form {
  type TrFormWithDb[TR <: TableRecord] = FormWithDb[TR, _ <: MutableTableRecord[TR]]
  type AnyFormWithDb = TrFormWithDb[_ <: TableRecord]
}
