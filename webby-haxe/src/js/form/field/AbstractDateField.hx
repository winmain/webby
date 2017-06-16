package js.form.field;

import js.form.field.Field.FieldProps;
import js.ui.InputMask;
import Std;

class AbstractDateField extends Field {
  public var minDate: Null<Date>;
  public var maxDate: Null<Date>;

  public function new(form: Form, mask: String, props: DateFieldProps) {
    super(form, props);
    minDate = parseDateOrFail(props.minDate, 'minDate');
    maxDate = parseDateOrFail(props.maxDate, 'maxDate');
    inputMask().applyOn(tag, mask);

    tag.on('blur', function() {
      if (!isEmpty()) {
        var val = value();
        var date: Null<Date> = parseDate(val);
        if (date != null) {
          if (minDate != null && date.getTime() < minDate.getTime()) {
            setJsError(form.config.strings.notEarlierThanError(formatDate(minDate)));
          }
          if (maxDate != null && date.getTime() > maxDate.getTime()) {
            setJsError(form.config.strings.noLaterThanError(formatDate(maxDate)));
          }
          setValue(formatDate(date));
        }
      }
    });
  }

  override public function setValue(v: Null<Dynamic>) {
    if (Std.is(v, Date)) {
      v = formatDate(v);
    }
    super.setValue(v);
  }

  public function dateValue(): Null<Date> {
    return G.and2(value(), function(v) return parseDate(v));
  }

  @:protected function inputMask(): InputMask return new InputMask();

  @:protected @:final function parseDateOrFail(s: Null<String>, varName: String): Null<Date> {
    if (s == null) return null;
    var date = parseDate(s);
    if (date == null) throw "Invalid " + varName + " = " + s;
    return date;
  }

  // ------------------------------- Abstract methods -------------------------------

  function parseDate(s: String): Null<Date> return null;

  function formatDate(date: Date): String return null;
}


class DateFieldProps extends FieldProps {
  public var minDate: Null<String>;
  public var maxDate: Null<String>;
}
