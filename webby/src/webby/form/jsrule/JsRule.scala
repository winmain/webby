package webby.form.jsrule
import javax.annotation.Nullable

import webby.commons.io.jackson.JacksonAnnotations._

case class JsRule(@JsonProperty cond: JsCondition,
                  @JsonProperty actions: Iterable[JsAction]) extends JsonDisableAutodetect

object JsRule {
  @Nullable def makeOrNull(cond: JsCondition, actions: Iterable[JsAction]): JsRule =
    if (actions.isEmpty) null
    else new JsRule(cond, actions)
}
