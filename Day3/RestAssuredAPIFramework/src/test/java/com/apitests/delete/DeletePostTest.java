// FILE: src/test/java/com/apitests/delete/DeletePostTest.java
package com.apitests.delete;

import com.apitests.base.BaseTest;
import com.apitests.constants.ApiConstants;
import com.apitests.constants.StatusCodes;
import com.apitests.utils.ReportManager;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

public class DeletePostTest extends BaseTest {

    @BeforeClass
    public void classSetup() {
        logger.info("Initialising DeletePostTest class");
    }

    @Test(dataProvider = "postIdsToDelete", groups = {"smoke", "regression"}, priority = 1)
    public void test_DELETE_Post_ValidId_Returns200(int postId) {
        ReportManager.createTest("test_DELETE_Post_ValidId_Returns200_id" + postId);

        var response = given(requestSpec)
                .pathParam("id", postId)
                .when()
                .delete(ApiConstants.POST_BY_ID)
                .then()
                .statusCode(StatusCodes.OK)
                .extract()
                .response();

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(response.statusCode()).isEqualTo(StatusCodes.OK);
        soft.assertThat(response.time()).isLessThan(3000L);
        soft.assertAll();

        logger.info("Successfully deleted post id: {}", postId);
    }

    @Test(groups = {"negative", "regression"}, priority = 2)
    public void test_DELETE_Post_NonExistentId_Returns404() {
        ReportManager.createTest("test_DELETE_Post_NonExistentId_Returns404");

        given(requestSpec)
                .pathParam("id", 99999)
                .when()
                .delete(ApiConstants.POST_BY_ID)
                .then()
                .statusCode(StatusCodes.NOT_FOUND)
                .extract()
                .response();

        logger.info("Confirmed 404 for DELETE on non-existent post id: 99999");
    }

    @Test(groups = {"negative", "regression"}, priority = 3)
    public void test_DELETE_Post_ZeroId_Returns404() {
        ReportManager.createTest("test_DELETE_Post_ZeroId_Returns404");

        given(requestSpec)
                .pathParam("id", 0)
                .when()
                .delete(ApiConstants.POST_BY_ID)
                .then()
                .statusCode(StatusCodes.NOT_FOUND)
                .extract()
                .response();

        logger.info("Confirmed 404 for DELETE on post id: 0");
    }

    @Test(dataProvider = "postIdsToDelete", groups = {"regression"}, priority = 4)
    public void test_DELETE_Post_Idempotency_RepeatedDeleteStillReturns200(int postId) {
        ReportManager.createTest("test_DELETE_Post_Idempotency_RepeatedDeleteStillReturns200_id" + postId);

        given(requestSpec)
                .pathParam("id", postId)
                .when()
                .delete(ApiConstants.POST_BY_ID)
                .then()
                .statusCode(StatusCodes.OK);

        given(requestSpec)
                .pathParam("id", postId)
                .when()
                .delete(ApiConstants.POST_BY_ID)
                .then()
                .statusCode(StatusCodes.OK);

        logger.info("Idempotency verified for DELETE on post id: {}", postId);
    }

    @DataProvider(name = "postIdsToDelete")
    public Object[][] postIdsToDelete() {
        return new Object[][]{{2}, {3}};
    }
}
