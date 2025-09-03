package com.nelumbo.park.configuration.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;

public class StrictIntegerDeserializer extends JsonDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.getCurrentToken();

        if (token == JsonToken.VALUE_NUMBER_INT) {
            return parser.getIntValue();
        }

        throw new InvalidFormatException(parser, 
            "El valor debe ser un número entero, no " + token.name().toLowerCase(), 
            parser.getCurrentValue(), 
            Integer.class);
    }
}
