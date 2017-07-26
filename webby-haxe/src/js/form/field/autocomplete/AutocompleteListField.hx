package js.form.field.autocomplete;

import goog.ui.ac.AutoComplete;
import goog.ui.ac.ArrayMatcher;
import js.form.field.autocomplete.AbstractAutocompleteField.AutocompleteFieldProps;
import js.html.Event;
import js.lib.ArrayUtils;

class AutocompleteListField extends AbstractAutocompleteField {
  static public var REG = 'autocompleteList';

  var arrayMatcher: ArrayMatcher<Dynamic>;
  var sourceFn: String;
  var sourceArg: Dynamic;

  public var itemsBox(default, null): Tag;

  public var maxItems: Null<Int>;
  public var values: JMap<String, Dynamic>;
  public var rows: Array<Dynamic>;
  public var allRows: Array<Dynamic>;

  public function new(form: Form, props: AutocompleteListFieldProps) {
    var ac = form.config.acListConfig;
    G.require(ac, "AutocompleteListConfig is not defined");
    super(form, props);
    setMatcher(matcher);
    sourceFn = props.sourceFn;
    sourceArg = props.sourceArg;
    maxItems = props.maxItems;

    itemsBox = G.require(box.fnd('.' + ac.autocompleteListItemsCls), "No autocomplete items found");

    readRows();
  }

  override public function initBoxTag(): Tag return
    tag.fndParent(function(t: Tag) return t.hasCls(form.config.acListConfig.autocompleteListFieldCls));

  override public function value(): Array<String> return values.keys();

  override public function setValueEl(value: Null<Dynamic>) {
    setValueEl2(value);
  }

  public function setValueEl2(value: Null<Array<String>>): Void {
    reset();
    if (value != null) {
      for (id in value) {
        for (row in rows) {
          if (source.getRowId(row) == id) {
            addItem(row, false);
            break;
          }
        }
      }
    }
    updateTagVisibility();
  }

  // Прочитать все строки, используя настройки source
  public function readRows() {
    allRows = arrayMatcher.rows_.copy();
    reset();
  }

  public function reset(): Void {
    values = JMap.create();
    itemsBox.removeChildren();
    tag.setVal(null);
    rows = allRows.copy();
    arrayMatcher.setRows(rows);
    dispatchEvent({type: Field.ChangeEvent});
  }

  public function updateTagVisibility(): Void {
    showHide(tag, canAdd());
  }

  public function canAdd(): Bool return maxItems == null || values.keys().length < maxItems;

  function createItem(row: Dynamic, closeCrossTag: Null<Tag>): Tag return
    Tag.div
    .cls(form.config.acListConfig.autocompleteListItemCls)
    .setHtml(source.getRowTitle(row))
    .add2(closeCrossTag);

  public function addItem(row: Dynamic, fireChangeEvent: Bool = true) {
    var rowId = source.getRowId(row);
    G.require(!values.contains(rowId), "Already has item id:" + rowId);
    if (!canAdd()) return;
    values.set(rowId, row);
    rows.splice(rows.indexOf(row), 1); // Remove selected row from autocomplete

    // Create item tag
    var closeCrossTag = form.config.acListConfig.createItemCloseCross();
    var itemTag = createItem(row, closeCrossTag);
    if (closeCrossTag != null) {
      closeCrossTag.onClick(function(e: Event) {removeItem(itemTag, row); e.preventDefault();});
    }

    // Add item and fire events
    itemsBox.add(itemTag);
    if (fireChangeEvent) dispatchEvent({type: Field.ChangeEvent});
    updateTagVisibility();
  }

  public function removeItem(itemTag: Tag, row: Dynamic): Void {
    itemTag.remove();
    values.remove(source.getRowId(row));
    // Restore removed row in the autocomplete list at its original position.
    var idx = 0;
    for (sourceRow in allRows) {
      if (sourceRow == row) {
        ArrayUtils.splice1(rows, idx, 0, row);
        break;
      }
      if (sourceRow == rows[idx]) {
        idx++;
      }
    }
    dispatchEvent({type: Field.ChangeEvent});
    updateTagVisibility();
  }


  public function setMatcher(matcher: Matcher) {
    G.require(matcher, 'No matcher defined');
    G.require(Std.is(matcher, ArrayMatcher), matcher + " is not ArrayMatcher");
    this.matcher = matcher;
    arrayMatcher = cast matcher;
  }

  /*
  Установить/сменить настройки исходных данных.
  Если новые настройки не совпадают со старыми, то происходит переинициализация автокомплита, и все выбранные значения очищаются.
  Если новые настройки совпадают со старыми, то ничего не происходит.
   */
  public function setSource(sourceFn: String, sourceArg: Dynamic) {
    if (this.sourceFn != sourceFn || this.sourceArg != sourceArg) {
      setMatcher(source.getMatcher(sourceFn, sourceArg));
      this.sourceFn = sourceFn;
      this.sourceArg = sourceArg;
      readRows();
    }
  }

  public function setSourceArg(sourceArg: Dynamic): Void {
    setSource(sourceFn, sourceArg);
  }

  override function onUpdate(e: RowEvent) {
    addItem(e.row);
    tag.setVal(null);
  }

  override function onBlur() {
    tag.setVal(null);
  }
}


class AutocompleteListFieldProps extends AutocompleteFieldProps {
  @:optional public var maxItems: Int;
}
