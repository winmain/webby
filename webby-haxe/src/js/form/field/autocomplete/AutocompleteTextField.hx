package js.form.field.autocomplete;

import js.form.field.autocomplete.AbstractAutocompleteField.AutocompleteFieldProps;

/*
This autocomplete allows free text input.
 */
class AutocompleteTextField extends AbstractAutocompleteField {
  static public var REG = 'autocompleteText';

  public function new(form: Form, props: AutocompleteFieldProps) {
    super(form, props);
  }
}
