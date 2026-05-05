package org.anta.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {

    private static final ObjectMapper M = new ObjectMapper();

    public static String toJson(Object o) {
        try {
            return M.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return M.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T fromJson(
            String json,
            com.fasterxml.jackson.core.type.TypeReference<T> typeRef
    ) {
        try {
            return M.readValue(json, typeRef);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}