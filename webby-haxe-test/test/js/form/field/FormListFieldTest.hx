package js.form.field;

import haxe.unit.TestCase;
import js.form.field.Field.FieldProps;
import js.form.field.FormListField.FormListFieldProps;
import js.form.Form.FormProps;

class FormListFieldTest extends TestCase {

  override public function setup(): Void {
    NodeGlobals.init();
  }

  public function testSubformIds() {
    // Prepare form & fields
    Form.regField(TextField);
    Form.regField(FormListField);

    // Make html form
    var formHtml = '
<form id="foo">
  <div id="foo-bar-list"></div>
  <div id="foo-bar-template">
    <input id="$$subform$$-txt">
  </div>
</form>
';
    var formT = Tag.fromHtml(formHtml);
    Tag.getBody().add(formT);

    // FormProps
    var formProps: FormProps = cast {};
    formProps.htmlId = 'foo';

    // FormListField props
    var ffp: FormListFieldProps = cast {};
    ffp.shortId = 'bar';
    ffp.jsField = FormListField.REG;
    ffp.sub = cast {};
    ffp.sub.htmlId = "$subform$";

    // TextField props
    var tfp: FieldProps = cast {};
    tfp.shortId = 'txt';
    tfp.jsField = TextField.REG;
    ffp.sub.fields = [tfp];

    formProps.fields = [ffp];

    // Make js form
    var form = new Form(formProps);
    form.init();

    // Get FormListField
    var ff: FormListField = cast G.require(form.fields.get('bar'));


    // Add one subform & check id
    {
      var subForm: Form = G.require(ff.addForm(true));
      var subTxtField: Field = G.require(subForm.fields.get('txt'));
      assertEquals("foo-bar-1-txt", subTxtField.htmlId);
    }

    // Add seconds subform & check id
    {
      var subForm: Form = G.require(ff.addForm(true));
      var subTxtField: Field = G.require(subForm.fields.get('txt'));
      assertEquals("foo-bar-2-txt", subTxtField.htmlId);
    }
  }
}
