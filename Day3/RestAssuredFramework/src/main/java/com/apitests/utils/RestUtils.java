// FILE: src/main/java/com/apitests/utils/RestUtils.java
package com.apitests.utils;

import com.apitests.constants.ApiConstants;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class RestUtils {

    public static Response createPost(Object body) throws Exception {
        String json = JsonUtils.toJson(body);
        return given()
                .body(json)
                .when()
                .post(ApiConstants.POSTS)
                .andReturn();
    }

    public static Response getPostById(int id) {
        return given()
                .pathParam("id", id)
                .when()
                .get(ApiConstants.POST_BY_ID)
                .andReturn();
    }

    public static Response updatePost(int id, Object body) throws Exception {
        String json = JsonUtils.toJson(body);
        return given()
                .pathParam("id", id)
                .body(json)
                .when()
                .put(ApiConstants.POST_BY_ID)
                .andReturn();
    }

    public static Response deletePost(int id) {
        return given()
                .pathParam("id", id)
                .when()
                .delete(ApiConstants.POST_BY_ID)
                .andReturn();
    }
}
