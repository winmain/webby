package js.form.field.autocomplete;

import js.lib.ArrayUtils;
import goog.ui.ac.ArrayMatcher;
import goog.ui.ac.AutoComplete.RowEvent;
import js.form.field.autocomplete.AbstractAutocompleteField.AutocompleteFieldProps;
import js.html.Event;

class AutocompleteListField extends AbstractAutocompleteField {
  static public var REG = 'autocompleteList';

  var arrayMatcher: ArrayMatcher<Dynamic>;

  public var itemsBox(default, null): Tag;

  public var maxItems: Null<Int>;
  public var values: JMap<String, Dynamic>;
  public var rows: Array<Dynamic>;
  public var allRows: Array<Dynamic>;

  public function new(form: Form, props: AutocompleteListFieldProps) {
    var ac = form.config.acListConfig;
    G.require(ac, "AutocompleteListConfig is not defined");
    super(form, props);
    G.require(Std.is(matcher, ArrayMatcher), matcher + " is not ArrayMatcher");
    arrayMatcher = cast matcher;
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
      for (id in values) {
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
//    allRows = [];
//    var rawRows = source. rr.form.field.AutocompleteSource.get(@source[0], @source[1])
//    if rawRows
//      for row in rawRows
//        @allRows.push {id: row[0], title: row[1], toString: -> @title}
//    @reset()
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

/*
  ###
    Установить/сменить настройки исходных данных.
    Если новые настройки не совпадают со старыми, то происходит переинициализация автокомплита, и все выбранные значения очищаются.
    Если новые настройки совпадают со старыми, то ничего не происходит.
  ###
  setSource: (source, sourceArg) ->
    if @source[0] != source || @source[1] != sourceArg
      @source = [source, sourceArg]
      @readRows()

  setSourceArg: (sourceArg) -> @setSource(@source[0], sourceArg)

  # --------------- Error & event handling methods ---------------
   */

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
