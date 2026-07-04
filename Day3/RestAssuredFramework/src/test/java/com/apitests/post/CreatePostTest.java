// FILE: src/test/java/com/apitests/post/CreatePostTest.java
package com.apitests.post;

import com.apitests.base.BaseTest;
import com.apitests.constants.ApiConstants;
import com.apitests.constants.StatusCodes;
import com.apitests.models.Post;
import com.apitests.utils.JsonUtils;
import com.apitests.utils.ReportManager;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class CreatePostTest extends BaseTest {

    @DataProvider(name = "postPayloads")
    public Object[][] postPayloads() throws IOException {
        Post[] posts = JsonUtils.readPostsData("testdata/posts_data.json");
        Object[][] out = new Object[posts.length][1];
        for (int i = 0; i < posts.length; i++) out[i][0] = posts[i];
        return out;
    }

    @Test(groups = {"regression"}, priority = 1, dataProvider = "postPayloads")
    public void test_POST_Post_Create_Returns201(Post payload) {
        ReportManager.createTest("test_POST_Post_Create_Returns201");
        try {
            Response response = given().spec(requestSpec)
                    .body(payload)
                    .when()
                    .post(ApiConstants.POSTS)
                    .then()
                    .spec(responseSpec)
                    .statusCode(StatusCodes.CREATED)
                    .body("id", notNullValue())
                    .extract()
                    .response();

            Post created = response.as(Post.class);
            SoftAssertions soft = new SoftAssertions();
            soft.assertThat(created.getId()).isPositive();
            soft.assertThat(created.getTitle()).isEqualTo(payload.getTitle());
            soft.assertAll();
            ReportManager.log("PASS", "POST created id=" + created.getId());
        } catch (AssertionError e) {
            ReportManager.log("FAIL", e.getMessage());
            throw e;
        }
    }

    @Test(groups = {"negative"}, priority = 2)
    public void test_POST_Post_MissingFields_Returns400() {
        ReportManager.createTest("test_POST_Post_MissingFields_Returns400");
        Post payload = new Post();
        try {
            given().spec(requestSpec)
                    .body(payload)
                    .when()
                    .post(ApiConstants.POSTS)
                    .then()
                    .statusCode(StatusCodes.BAD_REQUEST);
            ReportManager.log("PASS", "POST with missing fields returned 400");
        } catch (AssertionError e) {
            ReportManager.log("FAIL", e.getMessage());
            throw e;
        }
    }
}
