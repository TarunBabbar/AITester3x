// FILE: src/test/java/com/apitests/get/GetPostsTest.java
package com.apitests.get;

import com.apitests.base.BaseTest;
import com.apitests.constants.ApiConstants;
import com.apitests.constants.StatusCodes;
import com.apitests.models.Post;
import com.apitests.utils.ReportManager;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class GetPostsTest extends BaseTest {

    @DataProvider(name = "validPostIds")
    public Object[][] validPostIds() {
        return new Object[][]{{1}, {5}, {10}};
    }

    @Test(groups = {"smoke", "regression"}, priority = 1, dataProvider = "validPostIds")
    public void test_GET_Post_ValidId_Returns200(int postId) {
        ReportManager.createTest("test_GET_Post_ValidId_Returns200");
        try {
            Response response = given().spec(requestSpec)
                    .pathParam("id", postId)
                    .when()
                    .get(ApiConstants.POST_BY_ID)
                    .then()
                    .spec(responseSpec)
                    .statusCode(StatusCodes.OK)
                    .body("id", equalTo(postId))
                    .body("title", not(emptyString()))
                    .extract()
                    .response();

            Post post = response.as(Post.class);
            SoftAssertions soft = new SoftAssertions();
            soft.assertThat(post.getId()).isEqualTo(postId);
            soft.assertThat(post.getTitle()).isNotBlank();
            soft.assertThat(post.getUserId()).isPositive();
            soft.assertAll();
            ReportManager.log("PASS", "Valid GET returned 200 for id=" + postId);
        } catch (AssertionError e) {
            ReportManager.log("FAIL", e.getMessage());
            throw e;
        }
    }

    @Test(groups = {"negative"}, priority = 2)
    public void test_GET_Post_InvalidId_Returns404() {
        ReportManager.createTest("test_GET_Post_InvalidId_Returns404");
        int invalidId = 0;
        try {
            given().spec(requestSpec)
                    .pathParam("id", invalidId)
                    .when()
                    .get(ApiConstants.POST_BY_ID)
                    .then()
                    .statusCode(StatusCodes.NOT_FOUND);
            ReportManager.log("PASS", "Invalid GET returned 404 for id=" + invalidId);
        } catch (AssertionError e) {
            ReportManager.log("FAIL", e.getMessage());
            throw e;
        }
    }
}
