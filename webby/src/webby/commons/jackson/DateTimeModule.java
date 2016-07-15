package webby.commons.jackson;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import javax.annotation.Nullable;

import com.fasterxml.jackson.databind.module.SimpleModule;
import scala.Function1;

public class DateTimeModule extends SimpleModule {
    public DateTimeModule(DateTimeFormatter dateTimeFormatter, @Nullable Function1<String, String> dateTimePrepareFn) {
        addDeserializer(OffsetDateTime.class, new OffsetDateTimeDeserializer(dateTimeFormatter, dateTimePrepareFn));
    }
}
