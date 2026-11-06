package com.pothole.khadda.test.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page object for the Analytics & Report screen (fragment_report.xml).
 */
public class AnalyticsPage extends BasePage {

    public static final By TV_REPORT_HIGH = AppiumBy.id("com.pothole.khadda:id/tvReportHigh");
    public static final By TV_REPORT_MEDIUM = AppiumBy.id("com.pothole.khadda:id/tvReportMedium");
    public static final By TV_REPORT_LOW = AppiumBy.id("com.pothole.khadda:id/tvReportLow");
    public static final By TV_ALGO_STATS = AppiumBy.id("com.pothole.khadda:id/tvAlgoStats");
    public static final By BTN_EXPORT_CSV = AppiumBy.id("com.pothole.khadda:id/btnExportCsv");
    public static final By BTN_EXPORT_JSON = AppiumBy.id("com.pothole.khadda:id/btnExportJson");
    public static final By BTN_EXPORT_TELEMETRY = AppiumBy.id("com.pothole.khadda:id/btnExportTelemetry");

    public AnalyticsPage(AndroidDriver driver, WebDriverWait wait) {
        super(driver, wait);
    }

    public boolean isLoaded() {
        return isElementDisplayed(TV_REPORT_HIGH) && isElementDisplayed(BTN_EXPORT_TELEMETRY);
    }

    public String getHighSeverityCount() {
        return getText(TV_REPORT_HIGH);
    }

    public String getMediumSeverityCount() {
        return getText(TV_REPORT_MEDIUM);
    }

    public String getLowSeverityCount() {
        return getText(TV_REPORT_LOW);
    }

    public String getAlgorithmStatsText() {
        return getText(TV_ALGO_STATS);
    }

    public boolean isExportCsvButtonDisplayed() {
        return isElementDisplayed(BTN_EXPORT_CSV);
    }
}
