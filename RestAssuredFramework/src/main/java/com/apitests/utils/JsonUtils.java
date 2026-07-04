// FILE: src/main/java/com/apitests/utils/JsonUtils.java
package com.apitests.utils;

import com.apitests.models.Post;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Post[] readPostsData(String resourcePath) throws IOException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) throw new IOException("Resource not found: " + resourcePath);
            return MAPPER.readValue(is, Post[].class);
        }
    }

    public static String toJson(Object obj) throws IOException {
        return MAPPER.writeValueAsString(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) throws IOException {
        return MAPPER.readValue(json, clazz);
    }
}