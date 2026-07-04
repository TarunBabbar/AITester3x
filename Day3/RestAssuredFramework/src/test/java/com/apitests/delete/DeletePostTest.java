// FILE: src/test/java/com/apitests/delete/DeletePostTest.java
package com.apitests.delete;

import com.apitests.base.BaseTest;
import com.apitests.constants.ApiConstants;
import com.apitests.constants.StatusCodes;
import com.apitests.models.Post;
import com.apitests.utils.ReportManager;
import io.restassured.response.Response;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

public class DeletePostTest extends BaseTest {
    private int createdId = -1;

    @BeforeClass
    public void setup() {
        Post payload = new Post();
        payload.setUserId(1);
        payload.setTitle("Delete Title");
        payload.setBody("Delete body");
        Response r = given().spec(requestSpec).body(payload).when().post(ApiConstants.POSTS).andReturn();
        createdId = r.jsonPath().getInt("id");
    }

    @Test(groups = {"regression"}, priority = 1)
    public void test_DELETE_Post_ValidId_Returns200Or204() {
        ReportManager.createTest("test_DELETE_Post_ValidId_Returns200Or204");
        try {
            Response r = given().spec(requestSpec).pathParam("id", createdId).when().delete(ApiConstants.POST_BY_ID).andReturn();
            int code = r.getStatusCode();
            if (!(code == StatusCodes.OK || code == StatusCodes.NO_CONTENT)) {
                throw new AssertionError("Unexpected status code: " + code);
            }
            ReportManager.log("PASS", "DELETE returned " + code + " for id=" + createdId);
        } catch (AssertionError e) {
            ReportManager.log("FAIL", e.getMessage());
            throw e;
        }
    }

    @Test(groups = {"negative"}, priority = 2)
    public void test_DELETE_Post_InvalidId_Returns404() {
        ReportManager.createTest("test_DELETE_Post_InvalidId_Returns404");
        int invalidId = 0;
        try {
            given().spec(requestSpec).pathParam("id", invalidId).when().delete(ApiConstants.POST_BY_ID).then().statusCode(StatusCodes.NOT_FOUND);
            ReportManager.log("PASS", "DELETE invalid id returned 404");
        } catch (AssertionError e) {
            ReportManager.log("FAIL", e.getMessage());
            throw e;
        }
    }

    @AfterClass
    public void cleanup() {
        // ensure idempotency
        if (createdId > 0) {
            given().spec(requestSpec).pathParam("id", createdId).when().delete(ApiConstants.POST_BY_ID);
        }
    }
}
