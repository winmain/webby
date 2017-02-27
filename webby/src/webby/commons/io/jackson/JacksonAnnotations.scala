package webby.commons.io.jackson

import com.fasterxml.jackson.annotation.JsonAutoDetect

import scala.annotation.meta.{field, getter, param}

object JacksonAnnotations {
  // @formatter:off
  type JsonDeserialize = com.fasterxml.jackson.databind.annotation.JsonDeserialize @param @field @getter
  type JsonFormat = com.fasterxml.jackson.annotation.JsonFormat @param @field @getter
  type JsonIgnore = com.fasterxml.jackson.annotation.JsonIgnore @param @field @getter
  type JsonIgnoreProperties = com.fasterxml.jackson.annotation.JsonIgnoreProperties @param @field @getter
  type JsonInclude = com.fasterxml.jackson.annotation.JsonInclude @param @field @getter
  type JsonProperty = com.fasterxml.jackson.annotation.JsonProperty @param @field @getter
  type JsonRawValue = com.fasterxml.jackson.annotation.JsonRawValue @param @field @getter
  type JsonRootName = com.fasterxml.jackson.annotation.JsonRootName @param @field @getter
  type JsonSerialize = com.fasterxml.jackson.databind.annotation.JsonSerialize @param @field @getter
  type JsonTypeInfo = com.fasterxml.jackson.annotation.JsonTypeInfo @param @field @getter
  type JsonUnwrapped = com.fasterxml.jackson.annotation.JsonUnwrapped @param @field @getter
  // @formatter:on

  @JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE)
  trait JsonDisableAutodetect
}
