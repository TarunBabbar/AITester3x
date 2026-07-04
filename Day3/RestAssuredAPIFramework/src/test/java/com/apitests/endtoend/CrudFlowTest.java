// FILE: src/test/java/com/apitests/endtoend/CrudFlowTest.java
package com.apitests.endtoend;

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
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class CrudFlowTest extends BaseTest {

    private int createdPostId;
    private static final String ORIGINAL_TITLE = "CRUD Flow Test Original Title";
    private static final String UPDATED_TITLE = "CRUD Flow Test Updated Title";
    private static final int TEST_USER_ID = 1;
    private static final int KNOWN_EXISTING_POST_ID = 1;

    @BeforeClass
    public void classSetup() {
        logger.info("Starting end-to-end CRUD flow test sequence");
        ReportManager.createTest("CrudFlowTest E2E Suite");
    }

    @Test(groups = {"smoke", "regression"}, priority = 1)
    public void test_CRUD_Step1_POST_CreateNewPost_Returns201AndCapturesId() {
        var requestBody = new Post(TEST_USER_ID, ORIGINAL_TITLE, "CRUD flow test body content for post");

        Response response = given(requestSpec)
                .body(requestBody)
                .when()
                .post(ApiConstants.POSTS_ENDPOINT)
                .then()
                .statusCode(StatusCodes.CREATED)
                .body("id", notNullValue())
                .body("title", equalTo(ORIGINAL_TITLE))
                .body("userId", equalTo(TEST_USER_ID))
                .extract()
                .response();

        Post created = response.as(Post.class);
        createdPostId = created.getId();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(createdPostId).isPositive();
        soft.assertThat(created.getTitle()).isEqualTo(ORIGINAL_TITLE);
        soft.assertThat(created.getUserId()).isEqualTo(TEST_USER_ID);
        soft.assertThat(created.getBody()).isNotBlank();
        soft.assertAll();

        logger.info("CRUD Step 1 PASS - Created post with simulated id: {}", createdPostId);
    }

    @Test(groups = {"smoke", "regression"}, priority = 2, dependsOnMethods = "test_CRUD_Step1_POST_CreateNewPost_Returns201AndCapturesId")
    public void test_CRUD_Step2_GET_ExistingPost_Returns200AndValidBody() {
        Response response = given(requestSpec)
                .pathParam("id", KNOWN_EXISTING_POST_ID)
                .when()
                .get(ApiConstants.POST_BY_ID)
                .then()
                .spec(responseSpec)
                .statusCode(StatusCodes.OK)
                .body("id", equalTo(KNOWN_EXISTING_POST_ID))
                .body("title", not(emptyString()))
                .body("userId", notNullValue())
                .extract()
                .response();

        Post fetched = response.as(Post.class);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(fetched.getId()).isEqualTo(KNOWN_EXISTING_POST_ID);
        soft.assertThat(fetched.getTitle()).isNotBlank();
        soft.assertThat(fetched.getUserId()).isPositive();
        soft.assertThat(fetched.getBody()).isNotBlank();
        soft.assertAll();

        logger.info("CRUD Step 2 PASS - Fetched post id: {}", KNOWN_EXISTING_POST_ID);
    }

    @Test(groups = {"smoke", "regression"}, priority = 3, dependsOnMethods = "test_CRUD_Step2_GET_ExistingPost_Returns200AndValidBody")
    public void test_CRUD_Step3_PUT_UpdatePost_Returns200WithUpdatedTitle() {
        var updatePayload = new Post(TEST_USER_ID, UPDATED_TITLE, "CRUD flow updated body content");

        Response response = given(requestSpec)
                .pathParam("id", KNOWN_EXISTING_POST_ID)
                .body(updatePayload)
                .when()
                .put(ApiConstants.POST_BY_ID)
                .then()
                .spec(responseSpec)
                .statusCode(StatusCodes.OK)
                .body("title", equalTo(UPDATED_TITLE))
                .body("userId", equalTo(TEST_USER_ID))
                .extract()
                .response();

        Post updated = response.as(Post.class);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(updated.getTitle()).isEqualTo(UPDATED_TITLE);
        soft.assertThat(updated.getUserId()).isEqualTo(TEST_USER_ID);
        soft.assertAll();

        logger.info("CRUD Step 3 PASS - Updated post title to: {}", UPDATED_TITLE);
    }

    @Test(groups = {"smoke", "regression"}, priority = 4, dependsOnMethods = "test_CRUD_Step3_PUT_UpdatePost_Returns200WithUpdatedTitle")
    public void test_CRUD_Step4_DELETE_Post_Returns200() {
        given(requestSpec)
                .pathParam("id", KNOWN_EXISTING_POST_ID)
                .when()
                .delete(ApiConstants.POST_BY_ID)
                .then()
                .statusCode(StatusCodes.OK)
                .extract()
                .response();

        logger.info("CRUD Step 4 PASS - Deleted post id: {}", KNOWN_EXISTING_POST_ID);
    }

    @Test(groups = {"smoke", "regression"}, priority = 5, dependsOnMethods = "test_CRUD_Step4_DELETE_Post_Returns200")
    public void test_CRUD_Step5_GET_NonExistentPost_Returns404() {
        given(requestSpec)
                .pathParam("id", 99999)
                .when()
                .get(ApiConstants.POST_BY_ID)
                .then()
                .statusCode(StatusCodes.NOT_FOUND)
                .extract()
                .response();

        logger.info("CRUD Step 5 PASS - Confirmed 404 for non-existent post id: 99999");
    }

    @AfterClass
    public void classTeardown() {
        given(requestSpec)
                .pathParam("id", createdPostId)
                .when()
                .delete(ApiConstants.POST_BY_ID)
                .then()
                .extract()
                .response();

        logger.info("CRUD flow cleanup complete - simulated deletion of created post id: {}", createdPostId);
    }
}
