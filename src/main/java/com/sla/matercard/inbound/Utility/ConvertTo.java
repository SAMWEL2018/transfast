package com.sla.matercard.inbound.Utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Joseph Kibe
 * Created on April 17, 2023.
 * Time 8:37 AM
 */

public class ConvertTo {

    public static JsonNode jsonNode(Object objectToConvert) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(mapper.writeValueAsString(objectToConvert));

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JsonNode jsonNodeFromStr(String data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(data);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String jsonString(Object objectToConvert) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(objectToConvert);

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> T objectOfType(Object objectToConvert) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(objectToConvert);
            return (T) mapper.convertValue(json, objectToConvert.getClass());

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }

    }
}
