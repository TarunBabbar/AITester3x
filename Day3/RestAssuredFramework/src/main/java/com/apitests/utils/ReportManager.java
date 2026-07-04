// FILE: src/main/java/com/apitests/utils/ReportManager.java
package com.apitests.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

public class ReportManager {
    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> CURRENT = new ThreadLocal<>();

    public static synchronized void initReport(String path) {
        ExtentSparkReporter reporter = new ExtentSparkReporter(path);
        extent = new ExtentReports();
        extent.attachReporter(reporter);
    }

    public static void createTest(String name) {
        if (extent == null) return;
        CURRENT.set(extent.createTest(name));
    }

    public static void log(String status, String message) {
        ExtentTest test = CURRENT.get();
        if (test == null) return;
        test.info(status + ": " + message);
    }

    public static synchronized void flushReport() {
        if (extent != null) extent.flush();
    }
}