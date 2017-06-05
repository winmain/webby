package js.form.field;

import js.form.field.AbstractDateField.DateFieldProps;
import js.lib.ArrayUtils;
using js.lib.ArrayUtils;

class RuDateField extends AbstractDateField {
  static public var REG = 'ruDate';

  public function new(form: Form, props: DateFieldProps) {
    super(form, "99.99.9999", props);
  }

  override function formatDate(date: Date): String return
    zpad2(date.getDate()) + '.' + zpad2(date.getMonth() + 1) + '.' + zpad4(date.getFullYear());

  override function parseDate(str: String): Null<Date> {
    var parts: Array<String> = str.split('.');
    if (parts.length == 3) {
      var a: Array<Int> = [];
      for (part in parts) {
        var v: Int = G.toInt(part);
        if (Math.isNaN(v)) return null;
        a.push(v);
      }
      if (a[2] > 1900) {
        var d = new Date(a[2], a[1] - 1, a[0]);
        if (G.toInt(a[0]) == d.getDate() && G.toInt(a[1]) == (d.getMonth() + 1) && G.toInt(a[2]) == d.getFullYear()) {
          return d;
        }
      }
    }
    return null;
  }

  static function zpad2(v: Int): String return if (v < 10) '0' + v else '' + v;

  static function zpad4(v: Int): String return if (v < 1000) '0000' else '' + v;
}
