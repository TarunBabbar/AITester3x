// FILE: src/main/java/com/apitests/base/BaseTest.java
package com.apitests.base;

import com.apitests.config.ConfigReader;
import com.apitests.utils.ReportManager;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import static org.hamcrest.Matchers.lessThan;

public class BaseTest {

    protected static final Logger logger = LogManager.getLogger(BaseTest.class);
    public static RequestSpecification requestSpec;
    protected static ResponseSpecification responseSpec;

    @BeforeSuite
    public void globalSetup() {
        RestAssured.useRelaxedHTTPSValidation();

        var requestSpecBuilder = new RequestSpecBuilder()
                .setBaseUri(ConfigReader.get("base.uri"))
                .setContentType(ContentType.JSON);

        if (ConfigReader.getBoolean("log.enabled")) {
            requestSpecBuilder
                    .addFilter(new RequestLoggingFilter())
                    .addFilter(new ResponseLoggingFilter());
        }

        requestSpec = requestSpecBuilder.build();

        responseSpec = new ResponseSpecBuilder()
                .expectResponseTime(lessThan(ConfigReader.getLong("default.timeout.ms")))
                .expectContentType(ContentType.JSON)
                .build();

        ReportManager.initReport();
        logger.info("Global test suite setup complete. Base URI: {}", ConfigReader.get("base.uri"));
    }

    @AfterMethod
    public void afterEachTest(ITestResult result) {
        if (result.getStatus() == ITestResult.SUCCESS) {
            ReportManager.logPass("PASS: " + result.getName());
            logger.info("PASS: {}", result.getName());
        } else if (result.getStatus() == ITestResult.FAILURE) {
            ReportManager.logFail("FAIL: " + result.getName() + " — " + result.getThrowable().getMessage());
            logger.error("FAIL: {} — {}", result.getName(), result.getThrowable().getMessage());
        } else {
            ReportManager.logInfo("SKIP: " + result.getName());
            logger.warn("SKIP: {}", result.getName());
        }
    }

    @AfterSuite
    public void globalTeardown() {
        ReportManager.flushReport();
        logger.info("Global test suite teardown complete");
    }
}
