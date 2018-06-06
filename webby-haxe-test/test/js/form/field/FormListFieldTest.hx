package js.form.field;

import haxe.unit.TestCase;
import js.form.field.Field.FieldProps;
import js.form.field.FormListField.FormListFieldProps;
import js.form.Form.FormProps;

class FormListFieldTest extends TestCase {

  override public function setup(): Void {
    NodeGlobals.init();
  }

  /*
  Test simple subform creating with correct field id replaced in the subform template
   */
  public function testSubformIds() {
    // Prepare form & fields
    Form.regField(TextField);
    Form.regField(FormListField);

    // Make html form
    var formHtml = '
<form id="foo">
  <div id="foo-bar-list"></div>
  <div id="foo-bar-template" data-template="$$subform$$">
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


  /*
  Test subform in subform. Check correct field ids.
   */
  public function testSubformInSubformIds() {
    // Prepare form & fields
    Form.regField(TextField);
    Form.regField(FormListField);

    // Make html form
    var formHtml = '
<form id="foo">
  <div id="foo-bar-list"></div>
  <div id="foo-bar-template" data-template="$$subform$$">
    <div id="$$subform$$-cat-list"></div>
    <div id="$$subform$$-cat-template" data-template="$$subform$$">
      <input id="$$subform$$-txt">
    </div>
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

    // inner FormListField props
    var ff2p: FormListFieldProps = cast {};
    ff2p.shortId = 'cat';
    ff2p.jsField = FormListField.REG;
    ff2p.sub = cast {};
    ff2p.sub.htmlId = "$subform$";
    ffp.sub.fields = [ff2p];

    // TextField props
    var tfp: FieldProps = cast {};
    tfp.shortId = 'txt';
    tfp.jsField = TextField.REG;
    ff2p.sub.fields = [tfp];

    formProps.fields = [ffp];

    // Make js form
    var form = new Form(formProps);
    form.init();

    // Get FormListField
    var ff: FormListField = cast G.require(form.fields.get('bar'));

    // Add one subform & check id
    {
      var subForm: Form = G.require(ff.addForm(true));
      var catField: FormListField = cast subForm.fields.get('cat');
      assertEquals("foo-bar-1-cat", catField.htmlId);

      // Add one inner subform & check id
      var catForm: Form = G.require(catField.addForm(true));
      assertEquals("foo-bar-1-cat-1", catForm.htmlId);
      var txtField = catForm.fields.get('txt');
      assertEquals("foo-bar-1-cat-1-txt", txtField.htmlId);

      // Add second inner subform & check id
      var catForm2: Form = G.require(catField.addForm(true));
      assertEquals("foo-bar-1-cat-2", catForm2.htmlId);
      var txtField2 = catForm2.fields.get('txt');
      assertEquals("foo-bar-1-cat-2-txt", txtField2.htmlId);
    }
  }
}
