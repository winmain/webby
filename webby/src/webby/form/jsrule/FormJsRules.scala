package webby.form.jsrule
import javax.annotation.Nullable

import webby.form.Form
import webby.form.field.{Field, FormListField}

import scala.collection.mutable

trait FormJsRules {
  def addRule0(@Nullable clientRule: JsRule, @Nullable serverRule: JsRule): Unit
  def addRule(ruleMaker: JsRuleBuilder.type => JsWhenBuilder): Unit = {
    val maker: JsWhenBuilder = ruleMaker(JsRuleBuilder)
    addRule0(maker.toJsRuleForClient, maker.toJsRuleForServer)
  }
}

object JsRuleBuilder {
  def when(cond: JsCondition): JsWhenBuilder = new JsWhenBuilder(cond)
}

class JsWhenBuilder(cond: JsCondition, actions: mutable.Buffer[JsAction] = mutable.Buffer.empty) {
  @Nullable def toJsRuleForClient: JsRule = JsRule.makeOrNull(cond, actions.filter(!_.serverOnly))
  @Nullable def toJsRuleForServer: JsRule = JsRule.makeOrNull(cond, actions.filter(!_.jsOnly))
  def add(action: JsAction): this.type = { actions += action; this }
  def addIf(condition: Boolean, action: JsAction): this.type = if (condition) add(action) else this
  def addIf2(condition: Boolean, trueAction: JsAction, falseAction: JsAction): this.type = if (condition) add(trueAction) else add(falseAction)

  def show(field: Field[_], focus: Boolean = false, withParent: Boolean = false) = add(Visible(field, vis = true, focus = focus, withParent = withParent))
  def hide(field: Field[_], focus: Boolean = false, withParent: Boolean = false) = add(Visible(field, vis = false, focus = focus, withParent = withParent))

  def enable(field: Field[_], focus: Boolean = false) = add(Enable(field, enable = true, focus = focus))
  def disable(field: Field[_], focus: Boolean = false) = add(Enable(field, enable = false, focus = focus))

  def require(field: Field[_], focus: Boolean = false) = add(Require(field, require = true, focus = focus))
  def optional(field: Field[_], focus: Boolean = false) = add(Require(field, require = false, focus = focus))

  def ignore(field: Field[_]) = add(Ignore(field, ignore = true))
  def unIgnore(field: Field[_]) = add(Ignore(field, ignore = false))

  def showRequire(field: Field[_], focus: Boolean = false, withParent: Boolean = false) =
    add(Visible(field, vis = true, focus = focus, withParent = withParent))
      .add(Require(field, require = true, focus = focus))
  def hideOptional(field: Field[_], focus: Boolean = false, withParent: Boolean = false) =
    add(Visible(field, vis = false, focus = focus, withParent = withParent))
      .add(Require(field, require = false, focus = focus))
  def hideIgnore(field: Field[_], withParent: Boolean = false) =
    add(Visible(field, vis = false, focus = false, withParent = withParent))
      .add(Ignore(field, ignore = true))

  def setValue[T](field: Field[T], value: T) = add(SetValue(field, value))
  def setValue2[T](field: Field[T], valueOn: T, valueOff: T) = add(SetValue2(field, valueOn, valueOff))

  def addSubform(field: FormListField[_ <: Form]) = add(AddSubform(field))
}
