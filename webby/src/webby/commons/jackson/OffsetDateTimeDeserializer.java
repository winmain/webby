/*
 * Copyright 2013 FasterXML.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package webby.commons.jackson;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import javax.annotation.Nullable;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import scala.Function1;

/**
 * Deserializer for Java 8 temporal {@link OffsetDateTime}s with custom formatter and prepareFunction
 *
 * @author Nick Williams
 * @since 2.2.0
 */
public class OffsetDateTimeDeserializer extends StdScalarDeserializer<OffsetDateTime> {
    private static final long serialVersionUID = 1L;
    private final DateTimeFormatter formatter;
    private final Function1<String, String> prepareFn;

    public OffsetDateTimeDeserializer(DateTimeFormatter formatter, @Nullable Function1<String, String> prepareFn) {
        super(LocalDate.class);
        this.formatter = formatter;
        this.prepareFn = prepareFn;
    }

    @Override
    public OffsetDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        switch (parser.getCurrentToken()) {
            case VALUE_STRING:
                String string = parser.getText().trim();
                if (prepareFn != null)
                    string = prepareFn.apply(string);
                if (string == null || string.length() == 0)
                    return null;
                return OffsetDateTime.parse(string, formatter);
        }

        throw context.wrongTokenException(parser, JsonToken.START_ARRAY, "Expected array or string.");
    }

    @Override
    public Object deserializeWithType(JsonParser parser, DeserializationContext context, TypeDeserializer deserializer)
         throws IOException {
        return deserializer.deserializeTypedFromAny(parser, context);
    }
}
