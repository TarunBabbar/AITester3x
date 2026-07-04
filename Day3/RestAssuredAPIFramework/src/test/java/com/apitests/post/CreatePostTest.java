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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class CreatePostTest extends BaseTest {

    @BeforeClass
    public void classSetup() {
        logger.info("Initialising CreatePostTest class");
    }

    @Test(dataProvider = "validPostData", groups = {"smoke", "regression"}, priority = 1)
    public void test_POST_CreatePost_ValidPayload_Returns201(Map<String, Object> postData) {
        ReportManager.createTest("test_POST_CreatePost_ValidPayload_Returns201");

        var requestBody = new Post(
                (int) postData.get("userId"),
                (String) postData.get("title"),
                (String) postData.get("body")
        );

        Response response = given(requestSpec)
                .body(requestBody)
                .when()
                .post(ApiConstants.POSTS_ENDPOINT)
                .then()
                .statusCode(StatusCodes.CREATED)
                .body("id", notNullValue())
                .body("title", equalTo(requestBody.getTitle()))
                .body("userId", equalTo(requestBody.getUserId()))
                .extract()
                .response();

        Post created = response.as(Post.class);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(created.getId()).isPositive();
        soft.assertThat(created.getTitle()).isEqualTo(requestBody.getTitle());
        soft.assertThat(created.getUserId()).isEqualTo(requestBody.getUserId());
        soft.assertThat(created.getBody()).isEqualTo(requestBody.getBody());
        soft.assertAll();
    }

    @Test(groups = {"negative", "regression"}, priority = 2)
    public void test_POST_CreatePost_EmptyTitle_Returns201WithEmptyTitle() {
        ReportManager.createTest("test_POST_CreatePost_EmptyTitle_Returns201WithEmptyTitle");

        var requestBody = new Post(1, "", "Body content with no title provided");

        Response response = given(requestSpec)
                .body(requestBody)
                .when()
                .post(ApiConstants.POSTS_ENDPOINT)
                .then()
                .statusCode(StatusCodes.CREATED)
                .body("id", notNullValue())
                .extract()
                .response();

        Post created = response.as(Post.class);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(created.getId()).isPositive();
        soft.assertThat(created.getTitle()).isEmpty();
        soft.assertAll();
    }

    @Test(groups = {"negative", "regression"}, priority = 3)
    public void test_POST_CreatePost_MissingUserId_Returns201WithDefaultUserId() {
        ReportManager.createTest("test_POST_CreatePost_MissingUserId_Returns201WithDefaultUserId");

        String incompletePayload = "{\"title\": \"Post with no userId\", \"body\": \"Body text here\"}";

        Response response = given(requestSpec)
                .body(incompletePayload)
                .when()
                .post(ApiConstants.POSTS_ENDPOINT)
                .then()
                .statusCode(StatusCodes.CREATED)
                .body("id", notNullValue())
                .extract()
                .response();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(response.jsonPath().getInt("id")).isPositive();
        soft.assertAll();
    }

    @DataProvider(name = "validPostData")
    public Object[][] validPostData() throws IOException {
        List<Map<String, Object>> data = JsonUtils.readJsonArrayFromFile("testdata/posts_data.json");
        return JsonUtils.toDataProviderArray(data);
    }
}
