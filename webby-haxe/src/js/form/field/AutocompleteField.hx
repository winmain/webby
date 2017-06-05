package js.form.field;

import goog.ui.ac.AutoComplete;
import goog.ui.ac.InputHandler;
import goog.ui.ac.Renderer;
import goog.ui.ac.RusRenderer;
import js.form.field.Field.FieldProps;

class AutocompleteField extends Field {
  static public var REG = 'autocomplete';

  var valueRow: Null<Dynamic>;

  var source: AutocompleteSource;
  var autoComplete: AutoComplete;

  public function new(form: Form, props: AutocompleteFieldProps) {
    super(form, props);
    source = form.config.autocompleteSource;
    G.require(source != null, "AutocompleteSource not configured in FormConfig");
    var matcher = source.getMatcher(props.sourceFn, props.sourceArg);
    G.require(matcher != null, 'No matcher for ${props.sourceFn}, ${props.sourceArg}');
    var renderer = makeRenderer();
    var addRendererCls = props.addRendererCls;
    if (addRendererCls != null) {
      renderer.className += ' ' + addRendererCls;
    }
    var inputHandler = makeInputHandler();

    autoComplete = new AutoComplete(matcher, renderer, inputHandler);
    inputHandler.attachAutoComplete(autoComplete);
    inputHandler.attachInput(tag.el);

    autoComplete.listen(EventType.UPDATE, onUpdate);
    tag.on('blur', onBlur);
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

  function makeRenderer(): Renderer return new RusRenderer(null, null, null, true);

  function makeInputHandler(): InputHandler return new InputHandler(null, null, false);

  function onUpdate(e: RowEvent) {
    setValue(source.getRowId(e.row));
  }

  function onBlur() {
    // Если значение введено неверно, то просто сбросить его.
    setValue(G.toBool(tag.val()) ? source.getRowId(valueRow) : null);
  }
}


@:build(macros.ExternalFieldsMacro.build())
class AutocompleteFieldProps extends FieldProps {
  public var sourceFn: String;
  @:optional public var sourceArg: Dynamic;
  @:optional public var addRendererCls: String;
}
