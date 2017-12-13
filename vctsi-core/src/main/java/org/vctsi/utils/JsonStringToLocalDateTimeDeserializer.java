package org.vctsi.utils;

/*-
 * #%L
 * vctsi-core
 * %%
 * Copyright (C) 2016 - 2017 Michael Pietsch (aka. Skywalker-11)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

public class JsonStringToLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    /**
     * takes a string from the parser and parses it to a {@link LocalDateTime} object
     *
     * @param p the parser where the value is take from
     * @return the LocalDateTime object representing the parsed string
     */
    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        try {
            String datetime = p.readValueAs(String.class);
            LocalDateTime time;
            try {
                time = LocalDateTime.ofEpochSecond(Long.parseLong(datetime), 0, ZoneOffset.ofHours(2));
            } catch (NumberFormatException e) {
                time = LocalDateTime.parse(datetime);
            }
            return time;
        } catch (DateTimeParseException e) {
            throw new JsonParseException(p, "Couldn't parse the value to a LocalDateTime", e);
        }
    }
}
