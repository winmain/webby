package orm.elasticsearch

import scala.annotation.Annotation

/**
  * Аннотация-предупреждение. Ей помечаются enum-классы (как правило, ScalaDbEnum).
  * Индексы элементов в этом классе сохранены в базе ElasticSearch. Соответственно, при изменении индексов
  * (смена порядка элементов, удаление элемента, или добавление в середину), нужно переиндексировать таблицу ElasticSearch.
  */
class EsIndexedEnum extends Annotation
