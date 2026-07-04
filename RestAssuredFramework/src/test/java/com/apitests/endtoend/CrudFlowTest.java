// FILE: src/test/java/com/apitests/endtoend/CrudFlowTest.java
package com.apitests.endtoend;

import com.apitests.base.BaseTest;
import com.apitests.constants.ApiConstants;
import com.apitests.constants.StatusCodes;
import com.apitests.models.Post;
import com.apitests.utils.ReportManager;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

public class CrudFlowTest extends BaseTest {

    @Test(groups = {"smoke", "regression"}, priority = 1)
    public void test_CRUD_Post_FullFlow() {
        ReportManager.createTest("test_CRUD_Post_FullFlow");
        try {
            Post payload = new Post();
            payload.setUserId(1);
            payload.setTitle("E2E Title");
            payload.setBody("E2E body");

            Response postResp = given().spec(requestSpec).body(payload).when().post(ApiConstants.POSTS).andReturn();
            postResp.then().statusCode(StatusCodes.CREATED);
            int id = postResp.jsonPath().getInt("id");

            Response getResp = given().spec(requestSpec).pathParam("id", id).when().get(ApiConstants.POST_BY_ID).andReturn();
            getResp.then().statusCode(StatusCodes.OK);
            Post got = getResp.as(Post.class);

            SoftAssertions soft = new SoftAssertions();
            soft.assertThat(got.getTitle()).isEqualTo(payload.getTitle());

            Post update = new Post();
            update.setUserId(got.getUserId());
            update.setTitle("E2E Updated");
            update.setBody(got.getBody());

            Response putResp = given().spec(requestSpec).pathParam("id", id).body(update).when().put(ApiConstants.POST_BY_ID).andReturn();
            putResp.then().statusCode(StatusCodes.OK);
            Post updated = putResp.as(Post.class);
            soft.assertThat(updated.getTitle()).isEqualTo("E2E Updated");

            Response delResp = given().spec(requestSpec).pathParam("id", id).when().delete(ApiConstants.POST_BY_ID).andReturn();
            int delCode = delResp.getStatusCode();
            soft.assertThat(delCode == StatusCodes.OK || delCode == StatusCodes.NO_CONTENT).isTrue();

            given().spec(requestSpec).pathParam("id", id).when().get(ApiConstants.POST_BY_ID).then().statusCode(StatusCodes.NOT_FOUND);

            soft.assertAll();
            ReportManager.log("PASS", "CRUD flow completed for id=" + id);
        } catch (AssertionError e) {
            ReportManager.log("FAIL", e.getMessage());
            throw e;
        }
    }
}
