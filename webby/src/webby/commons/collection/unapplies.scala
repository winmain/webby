package webby.commons.collection

/**
 * Empty matches empty collection.
 * Example:
 * {{{
 *   Vector() matches { case Empty() => true }
 *   Vector(1) matches { case Empty() => false }
 *   null matches { case Empty() => true }
 * }}}
 */
object Empty {
  @inline def unapply(it: Iterable[_]): Boolean = it == null || it.isEmpty
}

/**
 * NonEmpty matches non-empty collection.
 * Example:
 * {{{
 *   Vector() matches { case NonEmpty(_) => false }
 *   Vector(1) matches { case NonEmpty(v) => true } // v == Vector(1)
 *   null matches { case NonEmpty(_) => false }
 * }}}
 */
object NonEmpty {
  @inline def unapply[A <: Iterable[_]](it: A): Option[A] = if (it == null || it.isEmpty) None else Some(it)
}

/**
 * One matches one-element collection.
 * Example:
 * {{{
 *   Vector(1) matches { case One(v) => true }
 *   Vector() matches { case One(v) => false }
 *   Vector(1, 2) matches { case One(v) => false }
 *   null matches { case One(v) => false }
 * }}}
 */
object One {
  @inline def unapply[A](it: Iterable[A]): Option[A] = if (it != null && it.size == 1) Some(it.head) else None
}

/**
 * Head matches head of collection.
 * Example:
 * {{{
 *   Vector(1) matches { case Head(v) => true } // v == 1
 *   Vector() matches { case Head(v) => false }
 *   Vector(2, 3) matches { case Head(v) => true } // v == 2
 *   null matches { case Head(v) => false }
 * }}}
 */
object Head {
  @inline def unapply[A](it: Iterable[A]): Option[A] = if (it == null) None else it.headOption
}
