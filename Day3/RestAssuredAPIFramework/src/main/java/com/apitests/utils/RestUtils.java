// FILE: src/main/java/com/apitests/utils/RestUtils.java
package com.apitests.utils;

import com.apitests.base.BaseTest;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static io.restassured.RestAssured.given;

public final class RestUtils {

    private static final Logger logger = LogManager.getLogger(RestUtils.class);

    private RestUtils() {}

    public static Response get(String endpoint) {
        logger.debug("Executing GET: {}", endpoint);
        return given(BaseTest.requestSpec)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();
    }

    public static Response getById(String endpoint, int id) {
        logger.debug("Executing GET by id={}: {}", id, endpoint);
        return given(BaseTest.requestSpec)
                .pathParam("id", id)
                .when()
                .get(endpoint)
                .then()
                .extract()
                .response();
    }

    public static Response post(String endpoint, Object body) {
        logger.debug("Executing POST to: {}", endpoint);
        return given(BaseTest.requestSpec)
                .body(body)
                .when()
                .post(endpoint)
                .then()
                .extract()
                .response();
    }

    public static Response put(String endpoint, int id, Object body) {
        logger.debug("Executing PUT id={} to: {}", id, endpoint);
        return given(BaseTest.requestSpec)
                .pathParam("id", id)
                .body(body)
                .when()
                .put(endpoint)
                .then()
                .extract()
                .response();
    }

    public static Response delete(String endpoint, int id) {
        logger.debug("Executing DELETE id={} at: {}", id, endpoint);
        return given(BaseTest.requestSpec)
                .pathParam("id", id)
                .when()
                .delete(endpoint)
                .then()
                .extract()
                .response();
    }
}
