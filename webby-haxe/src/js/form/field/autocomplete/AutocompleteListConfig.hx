package js.form.field.autocomplete;

class AutocompleteListConfig {
  public function new() {
  }

  // ------------------------------- Styles -------------------------------

  public var autocompleteListFieldCls = "autocomplete-list-field";
  public var autocompleteListItemsCls = "autocomplete-list__items";
  public var autocompleteListItemCls = "autocomplete-list__item";

  // ------------------------------- Defaults -------------------------------

  public function createItemCloseCross(): Null<Tag> return null;
}
