package js.form.field;

import js.form.field.Field.FieldProps;
import js.html.Event;
import js.html.HTMLCollection;
import js.html.OptionElement;
import js.html.SelectElement;

class RichSelectField extends Field {
  static public var REG = 'richSelect';

  public var label(default, null): Tag;
  public var values(default, null): Array<String>;
  public var placeholder(default, null): Null<String>;

  public function new(form: Form, props: RichSelectFieldProps) {
    super(form, props);
    var selectConfig = form.config.selectConfig;
    G.require(selectConfig != null, "SelectConfig is not configured");
    label = box.fnd('.' + selectConfig.labelCls);
    G.require(label != null, "No label found");

    tag.onMulti(['change', 'keyup'], function(e: Event) {
      var select: SelectElement = cast e.target;
      setValue(select.value);
    })
    .on('focus', function() {box.cls(selectConfig.focusCls);})
    .on('blur', function() {box.clsOff(selectConfig.focusCls);});

    values = props.values;
    placeholder = G.or(props.placeholder, function() return '');
  }

  override public function setValueEl(value: Null<Dynamic>) {
    super.setValueEl(value);
    var html: String = placeholder;
    var selEl: SelectElement = cast tag.el;
    var selOpts: HTMLCollection = selEl.selectedOptions;
    if (selOpts.length != 0) {
      html = selOpts[0].innerHTML;
    } else {
      // If no option selected - try to find option with empty value manually
      for (opt in tag.fndAll('option')) {
        var o: OptionElement = cast opt.el;
        if (o.value == '') {
          html = o.innerHTML;
          break;
        }
      }
    }
    label.setHtml(html)
    .setCls(form.config.selectConfig.emptyLabelCls, isEmpty());
  }

  override public function enable(en: Bool) {
    super.enable(en);
    box.setCls(form.config.selectConfig.disabledCls, !en);
  }

  override public function initTag(): Tag return form.tag.fnd('#' + id + ' > select');

  override public function initBoxTag(): Tag return tag.parent();
}

@:build(macros.ExternalFieldsMacro.build())
class RichSelectFieldProps extends FieldProps {
  public var values: Array<String>;
  public var placeholder(default, null): Null<String>;
}
