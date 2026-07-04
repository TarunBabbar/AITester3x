// FILE: src/main/java/com/apitests/constants/ApiConstants.java
package com.apitests.constants;

public final class ApiConstants {

    private ApiConstants() {}

    public static final String POSTS_ENDPOINT = "/posts";
    public static final String POST_BY_ID = "/posts/{id}";
    public static final String USERS_ENDPOINT = "/users";
    public static final String USER_BY_ID = "/users/{id}";
    public static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";
    public static final long MAX_RESPONSE_TIME_MS = 3000L;
}
