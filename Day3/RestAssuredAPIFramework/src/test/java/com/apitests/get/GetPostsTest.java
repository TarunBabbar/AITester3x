// FILE: src/test/java/com/apitests/get/GetPostsTest.java
package com.apitests.get;

import com.apitests.base.BaseTest;
import com.apitests.constants.ApiConstants;
import com.apitests.constants.StatusCodes;
import com.apitests.models.Post;
import com.apitests.utils.ReportManager;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class GetPostsTest extends BaseTest {

    @BeforeClass
    public void classSetup() {
        logger.info("Initialising GetPostsTest class");
    }

    @Test(groups = {"smoke", "regression"}, priority = 1)
    public void test_GET_AllPosts_Returns200AndNonEmptyList() {
        ReportManager.createTest("test_GET_AllPosts_Returns200AndNonEmptyList");

        Response response = given(requestSpec)
                .when()
                .get(ApiConstants.POSTS_ENDPOINT)
                .then()
                .spec(responseSpec)
                .statusCode(StatusCodes.OK)
                .body("$", not(empty()))
                .body("[0].id", notNullValue())
                .body("[0].title", not(emptyString()))
                .extract()
                .response();

        List<Post> posts = response.jsonPath().getList("", Post.class);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(posts).isNotEmpty();
        soft.assertThat(posts.get(0).getId()).isPositive();
        soft.assertThat(posts.get(0).getTitle()).isNotBlank();
        soft.assertThat(posts.get(0).getUserId()).isPositive();
        soft.assertAll();
    }

    @Test(dataProvider = "validPostIds", groups = {"smoke", "regression"}, priority = 2)
    public void test_GET_Post_ValidId_Returns200(int postId) {
        ReportManager.createTest("test_GET_Post_ValidId_Returns200_id" + postId);

        Response response = given(requestSpec)
                .pathParam("id", postId)
                .when()
                .get(ApiConstants.POST_BY_ID)
                .then()
                .spec(responseSpec)
                .statusCode(StatusCodes.OK)
                .body("id", equalTo(postId))
                .body("title", not(emptyString()))
                .body("userId", notNullValue())
                .body("body", not(emptyString()))
                .extract()
                .response();

        Post post = response.as(Post.class);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(post.getId()).isEqualTo(postId);
        soft.assertThat(post.getTitle()).isNotBlank();
        soft.assertThat(post.getUserId()).isPositive();
        soft.assertThat(post.getBody()).isNotBlank();
        soft.assertAll();
    }

    @Test(dataProvider = "invalidPostIds", groups = {"negative", "regression"}, priority = 3)
    public void test_GET_Post_InvalidId_Returns404(int invalidId) {
        ReportManager.createTest("test_GET_Post_InvalidId_Returns404_id" + invalidId);

        given(requestSpec)
                .pathParam("id", invalidId)
                .when()
                .get(ApiConstants.POST_BY_ID)
                .then()
                .statusCode(StatusCodes.NOT_FOUND)
                .extract()
                .response();

        logger.info("Confirmed 404 for invalid post id: {}", invalidId);
    }

    @Test(groups = {"negative", "regression"}, priority = 4)
    public void test_GET_Post_ZeroId_Returns404() {
        ReportManager.createTest("test_GET_Post_ZeroId_Returns404");

        given(requestSpec)
                .pathParam("id", 0)
                .when()
                .get(ApiConstants.POST_BY_ID)
                .then()
                .statusCode(StatusCodes.NOT_FOUND)
                .extract()
                .response();

        logger.info("Confirmed 404 for post id: 0");
    }

    @DataProvider(name = "validPostIds")
    public Object[][] validPostIds() {
        return new Object[][]{{1}, {5}, {10}};
    }

    @DataProvider(name = "invalidPostIds")
    public Object[][] invalidPostIds() {
        return new Object[][]{{9999}, {99999}};
    }
}
