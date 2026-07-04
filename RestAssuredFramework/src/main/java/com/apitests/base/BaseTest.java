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
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

import static org.hamcrest.Matchers.lessThan;

public class BaseTest {
    protected static RequestSpecification requestSpec;
    protected static ResponseSpecification responseSpec;

    @BeforeSuite
    public void globalSetup() {
        RestAssured.useRelaxedHTTPSValidation();
        String baseUri = ConfigReader.get("base.uri");
        boolean logEnabled = Boolean.parseBoolean(ConfigReader.get("log.enabled"));

        RequestSpecBuilder reqBuilder = new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType(ContentType.JSON);
        if (logEnabled) {
            reqBuilder.addFilter(new RequestLoggingFilter()).addFilter(new ResponseLoggingFilter());
        }
        requestSpec = reqBuilder.build();

        responseSpec = new ResponseSpecBuilder()
                .expectResponseTime(lessThan((long) ConfigReader.getInt("default.timeout.ms", 3000)))
                .expectContentType(ContentType.JSON)
                .build();

        ReportManager.initReport(ConfigReader.get("report.path"));
    }

    @AfterSuite
    public void globalTeardown() {
        ReportManager.flushReport();
    }
}
