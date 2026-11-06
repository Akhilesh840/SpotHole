package com.pothole.khadda.test.base;

import io.appium.java_client.android.AndroidDriver;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/**
 * JUnit 5 Extension that intercepts test failures and automatically saves
 * a screenshot to build/reports/screenshots/.
 */
public class TestFailureListener implements TestWatcher {

    private static final String SCREENSHOTS_DIR = "build/reports/screenshots";

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        AndroidDriver driver = BaseTest.getDriver();
        if (driver == null) {
            System.err.println("WARN: Cannot capture screenshot because AndroidDriver is null.");
            return;
        }

        String className = context.getRequiredTestClass().getSimpleName();
        String methodName = context.getRequiredTestMethod().getName();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        String fileName = String.format("%s_%s_%s.png", className, methodName, timestamp);

        try {
            Path directory = Paths.get(SCREENSHOTS_DIR);
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            File screenshotFile = driver.getScreenshotAs(OutputType.FILE);
            Path targetPath = directory.resolve(fileName);
            Files.copy(screenshotFile.toPath(), targetPath);

            System.out.printf("[FAILURE SCREENSHOT SAVED] -> %s%n", targetPath.toAbsolutePath());
        } catch (IOException e) {
            System.err.printf("ERROR: Failed to save failure screenshot: %s%n", e.getMessage());
        }
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {}

    @Override
    public void testSuccessful(ExtensionContext context) {}

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {}
}
