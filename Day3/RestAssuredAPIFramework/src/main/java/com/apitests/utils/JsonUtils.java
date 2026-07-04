// FILE: src/main/java/com/apitests/utils/JsonUtils.java
package com.apitests.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public final class JsonUtils {

    private static final Logger logger = LogManager.getLogger(JsonUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private JsonUtils() {}

    public static <T> T fromJson(String json, Class<T> type) throws IOException {
        logger.debug("Deserialising JSON to type: {}", type.getSimpleName());
        return objectMapper.readValue(json, type);
    }

    public static String toJson(Object object) throws IOException {
        logger.debug("Serialising object of type: {}", object.getClass().getSimpleName());
        return objectMapper.writeValueAsString(object);
    }

    public static List<Map<String, Object>> readJsonArrayFromFile(String classpathResource) throws IOException {
        logger.debug("Reading JSON array from classpath resource: {}", classpathResource);
        try (InputStream is = JsonUtils.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (is == null) {
                throw new IOException("Resource not found on classpath: " + classpathResource);
            }
            return objectMapper.readValue(is, new TypeReference<List<Map<String, Object>>>() {});
        }
    }

    public static Object[][] toDataProviderArray(List<Map<String, Object>> data) {
        return data.stream()
                .map(entry -> new Object[]{entry})
                .toArray(Object[][]::new);
    }
}
