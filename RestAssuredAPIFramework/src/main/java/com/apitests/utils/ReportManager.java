// FILE: src/main/java/com/apitests/utils/ReportManager.java
package com.apitests.utils;

import com.apitests.config.ConfigReader;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ReportManager {

    private static final Logger logger = LogManager.getLogger(ReportManager.class);
    private static ExtentReports extentReports;
    private static final ThreadLocal<ExtentTest> currentTest = new ThreadLocal<>();

    private ReportManager() {}

    public static void initReport() {
        var reportPath = ConfigReader.get("report.path");
        var sparkReporter = new ExtentSparkReporter(reportPath);
        sparkReporter.config().setDocumentTitle("REST Assured API Test Report");
        sparkReporter.config().setReportName("API Automation Suite");

        extentReports = new ExtentReports();
        extentReports.attachReporter(sparkReporter);
        extentReports.setSystemInfo("Framework", "Rest Assured 5.x + TestNG 7.x");
        extentReports.setSystemInfo("Base URI", ConfigReader.get("base.uri"));
        extentReports.setSystemInfo("Java Version", System.getProperty("java.version"));

        logger.info("ExtentReports initialised at: {}", reportPath);
    }

    public static ExtentTest createTest(String testName) {
        var test = extentReports.createTest(testName);
        currentTest.set(test);
        return test;
    }

    public static ExtentTest getCurrentTest() {
        return currentTest.get();
    }

    public static void logPass(String message) {
        if (currentTest.get() != null) {
            currentTest.get().pass(message);
        }
    }

    public static void logFail(String message) {
        if (currentTest.get() != null) {
            currentTest.get().fail(message);
        }
    }

    public static void logInfo(String message) {
        if (currentTest.get() != null) {
            currentTest.get().info(message);
        }
    }

    public static void flushReport() {
        if (extentReports != null) {
            extentReports.flush();
            logger.info("ExtentReports flushed successfully");
        }
    }
}
