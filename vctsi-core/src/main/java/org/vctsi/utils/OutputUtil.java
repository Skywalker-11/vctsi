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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class OutputUtil {

    public static boolean DEBUG = false;

    private static ObjectMapper mapper;

    /* initializes the output util by settings the correct serializer and deserializer for non default data types */
    static {
        mapper = new ObjectMapper();
        SimpleModule serializerModule = new SimpleModule("MyModule", new Version(2, 7, 0, null, "com.fasterxml.jackson.core", "jackson-databind"));
        serializerModule.addSerializer(LocalDateTime.class, new LocalDateTimeToJsonSerializer());
        serializerModule.addSerializer(Date.class, new DateToJsonSerializer());
        serializerModule.addSerializer(DateTime.class, new JodaDateTimeToJsonSerializer());
        serializerModule.addDeserializer(LocalDateTime.class, new JsonStringToLocalDateTimeDeserializer());
        mapper.registerModule(serializerModule);
    }

    /**
     * prints an error message as json error object
     *
     * @param errorMessage the message to be printed
     */
    public static void printError(String errorMessage) {
        try {
            System.err.println(getErrorMessageAsJsonString(errorMessage));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("unhandled exception while printing output", e);
        }
    }

    /**
     * prints an info message as json info object
     *
     * @param infoMessage the message to be printed
     */
    public static void printInfo(String infoMessage) {
        try {
            System.out.println(getInfoMessageAsJsonString(infoMessage));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("unhandled exception while printing output", e);
        }
    }

    /**
     * prints a signle objects as json
     *
     * @param value the object to be printed
     */
    public static void printObject(Object value) {
        try {
            System.out.println(mapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("unhandled exception while printing output", e);
        }
    }

    /**
     * prints a list of objects as json
     *
     * @param objectList the list of objects to be printed
     * @param <T>        the object type
     */
    public static <T> void printObjectList(List<T> objectList) {
        try {
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectList));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("unhandled exception while printing output", e);
        }
    }

    /**
     * prints debug messages as plain text if debugging is enabled
     *
     * @param message the message to be printed
     */
    public static void debug(String message) {
        if (DEBUG) {
            System.out.println(message);
        }
    }

    /**
     * get the representation of an error message as json object
     *
     * @param errorMessage the error message
     * @return a json object representing the error message
     * @throws JsonProcessingException if the message could not be converted to a json object
     */
    public static String getErrorMessageAsJsonString(String errorMessage) throws JsonProcessingException {
        return mapper.writeValueAsString(new JSONError(errorMessage));
    }

    /**
     * get the representation of an info message as json object
     *
     * @param infoMessage the info message
     * @return a json object representing the info message
     * @throws JsonProcessingException if the message could not be converted to a json object
     */
    public static String getInfoMessageAsJsonString(String infoMessage) throws JsonProcessingException {
        return mapper.writeValueAsString(new JSONInfo(infoMessage));
    }

    /**
     * an object representing an error message in json
     */
    private static class JSONError {
        public String errorMessage;

        public JSONError(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    /**
     * an object representing an info message in json
     */
    private static class JSONInfo {
        public String infoMessage;

        public JSONInfo(String infoMessage) {
            this.infoMessage = infoMessage;
        }
    }
}
