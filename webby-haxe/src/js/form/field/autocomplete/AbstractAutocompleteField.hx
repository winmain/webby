package js.form.field.autocomplete;

import js.form.field.Field.FieldProps;
import goog.ui.ac.InputHandler;
import goog.ui.ac.RusRenderer;
import goog.ui.ac.Renderer;
import goog.ui.ac.AutoComplete;

class AbstractAutocompleteField extends Field {
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

  function makeRenderer(): Renderer return new RusRenderer(null, null, null, true);

  function makeInputHandler(): InputHandler return new InputHandler(null, null, false);

  function onUpdate(e: RowEvent) {}

  function onBlur() {}
}


@:build(macros.ExternalFieldsMacro.build())
class AutocompleteFieldProps extends FieldProps {
  public var sourceFn: String;
  @:optional public var sourceArg: Dynamic;
  @:optional public var addRendererCls: String;
}
