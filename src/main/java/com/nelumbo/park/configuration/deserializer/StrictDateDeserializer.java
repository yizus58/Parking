package com.nelumbo.park.configuration.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StrictDateDeserializer extends JsonDeserializer<Date> {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public Date deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonToken token = parser.getCurrentToken();

        if (token == JsonToken.VALUE_STRING) {
            try {
                return dateFormat.parse(parser.getText());
            } catch (ParseException e) {
                throw new InvalidFormatException(parser, 
                    "El formato de fecha debe ser yyyy-MM-dd'T'HH:mm:ss", 
                    parser.getCurrentValue(), 
                    Date.class);
            }
        }

        throw new InvalidFormatException(parser, 
            "El valor debe ser una fecha en formato string (yyyy-MM-dd'T'HH:mm:ss), no " + token.name().toLowerCase(), 
            parser.getCurrentValue(), 
            Date.class);
    }
}
