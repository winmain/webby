package webby.form.field

trait PlaceholderField[T] extends Field[T] {
  var placeholder: String = defaultPlaceholder

  protected def defaultPlaceholder: String = null

  // ------------------------------- Builder & validations -------------------------------

  def placeholder(ph: String): this.type = {placeholder = ph; this}
}
