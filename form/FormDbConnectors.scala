package lib.form
import querio.{MutableTableRecord, TableRecord}

import scala.language.implicitConversions

trait FormDbConnectors[TR <: TableRecord, MTR <: MutableTableRecord[TR]] {self: FormWithDb[TR, MTR] =>

//  class CityCustomConnector(val field: Field[City], dbField: Table[TR, MTR]#Field[_, Int]) extends DbConnector[City, TR, MTR] {
//    override def load(r: TR): Unit = field := Cities(dbField.get(r))
//    override def save(r: MTR): Unit = dbField.set(r, field.get.id)
//  }
//
//  class CityOptionCustomConnector(val field: Field[City], dbField: Table[TR, MTR]#Field[_, Option[Int]]) extends DbConnector[City, TR, MTR] {
//    override def load(r: TR): Unit = field := dbField.get(r).map(Cities(_))
//    override def save(r: MTR): Unit = dbField.set(r, field.getOpt.map(_.id))
//  }
//
//  implicit class _CityFieldWrapper[F <: Field[City]](f: F) {
//    def connectCity(dbField: Table[TR, MTR]#Field[_, Int]): F = f.dbConnector[TR, MTR](new CityCustomConnector(f, dbField))
//    def connectCityOpt(dbField: Table[TR, MTR]#Field[_, Option[Int]]): F = f.dbConnector[TR, MTR](new CityOptionCustomConnector(f, dbField))
//  }
}
