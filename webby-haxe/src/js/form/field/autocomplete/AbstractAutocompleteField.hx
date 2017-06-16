package js.form.field.autocomplete;

import goog.ui.ac.AutoComplete;
import goog.ui.ac.InputHandler;
import goog.ui.ac.Renderer;
import goog.ui.ac.RusRenderer;
import js.form.field.Field.FieldProps;

class AbstractAutocompleteField extends Field {
  var source: AutocompleteSource;
  var matcher: Matcher;
  var autoComplete: AutoComplete;

  public function new(form: Form, props: AutocompleteFieldProps) {
    super(form, props);
    source = G.require(form.config.autocompleteSource, "AutocompleteSource not configured in FormConfig");
    matcher = G.require(source.getMatcher(props.sourceFn, props.sourceArg), 'No matcher for ${props.sourceFn}, ${props.sourceArg}');
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


class AutocompleteFieldProps extends FieldProps {
  public var sourceFn: String;
  @:optional public var sourceArg: Dynamic;
  @:optional public var addRendererCls: String;
}
