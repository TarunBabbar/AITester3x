// FILE: src/test/java/com/apitests/put/UpdatePostTest.java
package com.apitests.put;

import com.apitests.base.BaseTest;
import com.apitests.constants.ApiConstants;
import com.apitests.constants.StatusCodes;
import com.apitests.models.Post;
import com.apitests.utils.ReportManager;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class UpdatePostTest extends BaseTest {
    private int createdId = -1;

    @BeforeClass
    public void setup() {
        Post payload = new Post();
        payload.setUserId(1);
        payload.setTitle("Initial Title");
        payload.setBody("Initial body");
        Response r = given().spec(requestSpec).body(payload).when().post(ApiConstants.POSTS).andReturn();
        createdId = r.jsonPath().getInt("id");
    }

    @Test(groups = {"regression"}, priority = 1)
    public void test_PUT_Post_ValidId_Returns200() {
        ReportManager.createTest("test_PUT_Post_ValidId_Returns200");
        Post update = new Post();
        update.setUserId(1);
        update.setTitle("Updated Title");
        update.setBody("Updated body");
        try {
            Response response = given().spec(requestSpec)
                    .pathParam("id", createdId)
                    .body(update)
                    .when()
                    .put(ApiConstants.POST_BY_ID)
                    .then()
                    .spec(responseSpec)
                    .statusCode(StatusCodes.OK)
                    .body("title", equalTo("Updated Title"))
                    .extract()
                    .response();

            Post p = response.as(Post.class);
            SoftAssertions soft = new SoftAssertions();
            soft.assertThat(p.getId()).isEqualTo(createdId);
            soft.assertThat(p.getTitle()).isEqualTo("Updated Title");
            soft.assertAll();
            ReportManager.log("PASS", "PUT updated id=" + createdId);
        } catch (AssertionError e) {
            ReportManager.log("FAIL", e.getMessage());
            throw e;
        }
    }

    @Test(groups = {"negative"}, priority = 2)
    public void test_PUT_Post_InvalidPayload_Returns400() {
        ReportManager.createTest("test_PUT_Post_InvalidPayload_Returns400");
        String invalid = "{\"invalid\":\"data\"}";
        try {
            given().spec(requestSpec)
                    .pathParam("id", createdId)
                    .body(invalid)
                    .when()
                    .put(ApiConstants.POST_BY_ID)
                    .then()
                    .statusCode(StatusCodes.BAD_REQUEST);
            ReportManager.log("PASS", "PUT with invalid payload returned 400");
        } catch (AssertionError e) {
            ReportManager.log("FAIL", e.getMessage());
            throw e;
        }
    }

    @AfterClass
    public void cleanup() {
        if (createdId > 0) {
            given().spec(requestSpec).pathParam("id", createdId).when().delete(ApiConstants.POST_BY_ID);
        }
    }
}
