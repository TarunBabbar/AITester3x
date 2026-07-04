// FILE: src/test/java/com/apitests/put/UpdatePostTest.java
package com.apitests.put;

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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class UpdatePostTest extends BaseTest {

    private static final int EXISTING_POST_ID = 1;

    @BeforeClass
    public void classSetup() {
        logger.info("Initialising UpdatePostTest class");
    }

    @Test(dataProvider = "updateTitles", groups = {"smoke", "regression"}, priority = 1)
    public void test_PUT_UpdatePost_ValidPayload_Returns200WithUpdatedTitle(String newTitle) {
        ReportManager.createTest("test_PUT_UpdatePost_ValidPayload_Returns200_" + newTitle.replace(" ", "_"));

        var updatePayload = new Post(1, newTitle, "Updated body content for put test");

        Response response = given(requestSpec)
                .pathParam("id", EXISTING_POST_ID)
                .body(updatePayload)
                .when()
                .put(ApiConstants.POST_BY_ID)
                .then()
                .spec(responseSpec)
                .statusCode(StatusCodes.OK)
                .body("id", equalTo(EXISTING_POST_ID))
                .body("title", equalTo(newTitle))
                .extract()
                .response();

        Post updated = response.as(Post.class);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(updated.getId()).isEqualTo(EXISTING_POST_ID);
        soft.assertThat(updated.getTitle()).isEqualTo(newTitle);
        soft.assertThat(updated.getUserId()).isPositive();
        soft.assertAll();
    }

    @Test(groups = {"negative", "regression"}, priority = 2)
    public void test_PUT_UpdatePost_NonExistentId_Returns404() {
        ReportManager.createTest("test_PUT_UpdatePost_NonExistentId_Returns404");

        var updatePayload = new Post(1, "Non-existent post update title", "Body for non-existent post");

        given(requestSpec)
                .pathParam("id", 99999)
                .body(updatePayload)
                .when()
                .put(ApiConstants.POST_BY_ID)
                .then()
                .statusCode(StatusCodes.NOT_FOUND)
                .extract()
                .response();

        logger.info("Confirmed 404 for PUT on non-existent post id: 99999");
    }

    @Test(groups = {"negative", "regression"}, priority = 3)
    public void test_PUT_UpdatePost_EmptyBody_Returns200WithIdPreserved() {
        ReportManager.createTest("test_PUT_UpdatePost_EmptyBody_Returns200WithIdPreserved");

        given(requestSpec)
                .pathParam("id", EXISTING_POST_ID)
                .body("{}")
                .when()
                .put(ApiConstants.POST_BY_ID)
                .then()
                .statusCode(StatusCodes.OK)
                .body("id", notNullValue())
                .extract()
                .response();

        logger.info("Empty body PUT handled gracefully for post id: {}", EXISTING_POST_ID);
    }

    @DataProvider(name = "updateTitles")
    public Object[][] updateTitles() {
        return new Object[][]{
                {"Updated Title Alpha"},
                {"Updated Title Beta"}
        };
    }
}
