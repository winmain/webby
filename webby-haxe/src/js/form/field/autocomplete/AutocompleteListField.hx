package js.form.field.autocomplete;

import goog.ui.ac.AutoComplete.RowEvent;
import js.form.field.autocomplete.AbstractAutocompleteField.AutocompleteFieldProps;

class AutocompleteListField extends AbstractAutocompleteField {
  static public var REG = 'autocompleteList';

  public var itemsBox(default, null): Tag;

  public var maxItems: Null<Int>;
  public var values: Dynamic = {};
  public var rows: Dynamic = [];

  public function new(form: Form, props: AutocompleteListFieldProps) {
    super(form, props);
    maxItems = props.maxItems;

    itemsBox = box.fnd('.ac-items');

    readRows();
  }

  // Прочитать все строки, используя настройки source
  public function readRows() {
    // TODO:
  }

  /*
  readRows: ->
    @allRows = []
    rawRows = rr.form.field.AutocompleteSource.get(@source[0], @source[1])
    if rawRows
      for row in rawRows
        @allRows.push {id: row[0], title: row[1], toString: -> @title}
    @reset()

  reset: ->
    @values = {}
    @$itemsBox.empty()
    @$el.val(null)
    @rows = @allRows.slice(0)
    @matcher.setRows(@rows)
    @dispatchEvent({type: @changeEvent})

  canAdd: -> !@maxItems || Object.keys(@values).length < @maxItems

  addItem: (row, fireChangeEvent = true) ->
    self = @
    if @values[row.id] then throw "Already has item id:" + row.id
    if !@canAdd() then return
    @values[row.id] = row
    @rows.splice(@rows.indexOf(row), 1) # Удалить выбранную позицию из автокомплита
    $item = $('<div class="ac-item">').html(row.title)
    closeCross = $('<i class="ico">' + rr.iconGlyph.tinyCross + '</i>').click(-> self.removeItem($item, row))
    $item.append(closeCross).appendTo(@$itemsBox)
    if fireChangeEvent then @dispatchEvent({type: @changeEvent})
    if !@canAdd() then @$el.hide()

  removeItem: ($item, row) ->
    $item.remove()
    delete @values[row.id]
    # Восстановить удалённую позицию в автокомплите на том месте, где она была в изначальной позиции
    idx = 0
    for sourceRow in @allRows
      if sourceRow == row
        @rows.splice(idx, 0, row)
        break
      if sourceRow == @rows[idx]
        idx++
    @dispatchEvent({type: @changeEvent})
    if @canAdd() then @$el.show()

  setValueEl: (value) ->
    @reset()
    if value
      for id in value
        for row in @rows
          if row.id == id
            @addItem(row, false)
            break
    @$el.toggle(@canAdd())

  value: -> (id for id of @values)

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

  initBoxEl: -> @$el.parents('.autocomplete-list')
   */

  override function onUpdate(e: RowEvent) {
    addItem(e.row);
    tag.setVal(null);
  }

  override function onBlur() {
    tag.setVal(null);
  }

  public function addItem(row: Dynamic, fireChangeEvent: Bool = true) {
    // TODO:
  }
}


class AutocompleteListFieldProps extends AutocompleteFieldProps {
  @:optional public var maxItems: Int;
}
