package js.form.field.autocomplete;

import goog.ui.ac.AutoComplete;
import js.form.field.autocomplete.AbstractAutocompleteField.AutocompleteFieldProps;

/*
Common autocomplete field.
Only values from the source allowed.
 */
class AutocompleteField extends AbstractAutocompleteField {
  static public var REG = 'autocomplete';

  var valueRow: Null<Dynamic>;

  public function new(form: Form, props: AutocompleteFieldProps) {
    super(form, props);
  }

  override public function setValueEl(value: Null<Dynamic>) {
    setValueRow(
      if (value == null) null;
      else source.findRowById(autoComplete.getMatcher(), value));
  }

  override public function value(): Dynamic return G.and(valueRow, function() return source.getRowId(valueRow));

  function setValueRow(row: Null<Dynamic>) {
    valueRow = row;
    tag.setVal(G.and(row, function() return source.getRowTitle(row)));
  }

  override function onUpdate(e: RowEvent) {
    setValue(source.getRowId(e.row));
  }

  override function onBlur() {
    // Если значение введено неверно, то просто сбросить его.
    setValue(G.toBool(tag.getVal()) ? source.getRowId(valueRow) : null);
  }
}
