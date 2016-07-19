package webby.commons.io.jackson

import java.io.IOException

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.module.scala.JacksonModule
import querio._

private object ScalaDbEnumSerializers extends Serializers.Base {

  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription): JsonSerializer[_] = {
    if (classOf[AnyScalaDbEnumCls].isAssignableFrom(javaType.getRawClass)) ScalaDbEnumSerializer
    else null
  }
}

private object ScalaDbEnumSerializer extends JsonSerializer[AnyScalaDbEnumCls] {
  def serialize(value: AnyScalaDbEnumCls, jgen: JsonGenerator, provider: SerializerProvider) {
    jgen.writeString(value.getDbValue)
  }
}

private object ScalaDbEnumDeserializers extends Deserializers.Base {

  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): JsonDeserializer[_] = {
    if (classOf[AnyScalaDbEnumCls].isAssignableFrom(javaType.getRawClass)) {
      ScalaDbEnums.getEnum(javaType.getRawClass.asInstanceOf[Class[AnyScalaDbEnumCls]]) match {
        case Some(enum) => new ScalaDbEnumDeserializer(enum)
        case None => null
      }
    } else null
  }
}

private class ScalaDbEnumDeserializer(enum: AnyScalaDbEnum) extends JsonDeserializer[AnyScalaDbEnumCls] {

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): AnyScalaDbEnumCls = {
    enum.getValue(jp.getValueAsString) match {
      case Some(e) => e
      case None => throw new IOException(s"Invalid enum value '${jp.getValueAsString}'")
    }
  }
}

trait ScalaDbEnumJacksonModule extends JacksonModule {
  this += ScalaDbEnumSerializers
  this += ScalaDbEnumDeserializers
}
