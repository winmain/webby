package lib.form.jsrule
import javax.annotation.Nullable

import com.fasterxml.jackson.annotation.{JsonAutoDetect, JsonProperty}

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
  isGetterVisibility = JsonAutoDetect.Visibility.NONE,
  setterVisibility = JsonAutoDetect.Visibility.NONE,
  creatorVisibility = JsonAutoDetect.Visibility.NONE,
  fieldVisibility = JsonAutoDetect.Visibility.NONE)
case class JsRule(@JsonProperty cond: JsCondition, @JsonProperty actions: Iterable[JsAction])

object JsRule {
  @Nullable def makeOrNull(cond: JsCondition, actions: Iterable[JsAction]): JsRule =
    if (actions.isEmpty) null
    else new JsRule(cond, actions)
}
