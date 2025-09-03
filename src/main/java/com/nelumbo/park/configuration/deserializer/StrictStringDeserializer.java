package com.nelumbo.park.configuration.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;

public class StrictStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.getCurrentToken();

        if (token == JsonToken.VALUE_STRING) {
            return parser.getText();
        }

        throw new InvalidFormatException(parser, 
            "El valor debe ser una cadena de texto (string), no " + token.name().toLowerCase(), 
            parser.getCurrentValue(), 
            String.class);
    }
}
