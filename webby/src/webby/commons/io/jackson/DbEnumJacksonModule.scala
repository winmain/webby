package webby.commons.io.jackson
import java.io.IOException

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.ser.Serializers
import com.fasterxml.jackson.module.scala.JacksonModule
import querio._

// ------------------------------- Serializers -------------------------------

private object DbEnumSerializers extends Serializers.Base {
  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription): JsonSerializer[_] = {
    if (classOf[DbEnum#Cls].isAssignableFrom(javaType.getRawClass)) DbEnumSerializer
    else null
  }
}

private object DbEnumSerializer extends JsonSerializer[DbEnum#Cls] {
  def serialize(value: DbEnum#Cls, jgen: JsonGenerator, provider: SerializerProvider) {
    jgen.writeNumber(value.getId)
  }
}


// ------------------------------- Deserializers -------------------------------

private object DbEnumDeserializers extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription): JsonDeserializer[_] = {
    if (classOf[DbEnum#Cls].isAssignableFrom(javaType.getRawClass)) {
      DbEnums.getEnum(javaType.getRawClass.asInstanceOf[Class[DbEnum#Cls]]) match {
        case Some(enum) => new DbEnumDeserializer(enum)
        case None => null
      }
    } else null
  }
}

private class DbEnumDeserializer(enum: DbEnum) extends JsonDeserializer[DbEnum#Cls] {
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): DbEnum#Cls = {
    enum.getValue(jp.getValueAsInt) match {
      case Some(e) => e
      case None => throw new IOException(s"Invalid enum value '${jp.getValueAsInt}' for " + enum.getClass.getName)
    }
  }
}

// ------------------------------- Module -------------------------------

trait DbEnumJacksonModule extends JacksonModule {
  this += DbEnumSerializers
  this += DbEnumDeserializers
}
