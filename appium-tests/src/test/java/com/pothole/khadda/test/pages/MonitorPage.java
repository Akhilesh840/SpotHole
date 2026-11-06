package com.pothole.khadda.test.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page object for the Live Monitor screen (fragment_monitor.xml).
 */
public class MonitorPage extends BasePage {

    public static final By CARD_STATUS = AppiumBy.id("com.pothole.khadda:id/cardStatus");
    public static final By TV_STATUS = AppiumBy.id("com.pothole.khadda:id/tvStatus");
    public static final By SWITCH_TORCH = AppiumBy.id("com.pothole.khadda:id/switchTorch");
    public static final By SWITCH_DEMO_MODE = AppiumBy.id("com.pothole.khadda:id/switchDemoMode");
    public static final By TV_SPEED = AppiumBy.id("com.pothole.khadda:id/tvSpeed");
    public static final By TV_GPS = AppiumBy.id("com.pothole.khadda:id/tvGps");

    public static final By BTN_TOGGLE_MONITORING = AppiumBy.id("com.pothole.khadda:id/btnToggleMonitoring");
    public static final By BTN_MARK_POTHOLE = AppiumBy.id("com.pothole.khadda:id/btnMarkPothole");
    public static final By BTN_MARK_SPEED_BREAKER = AppiumBy.id("com.pothole.khadda:id/btnMarkSpeedBreaker");

    public static final By WAVEFORM_VIEW = AppiumBy.id("com.pothole.khadda:id/waveformView");
    public static final By TV_COUNT = AppiumBy.id("com.pothole.khadda:id/tvCount");
    public static final By CARD_LAST_ALERT = AppiumBy.id("com.pothole.khadda:id/cardLastAlert");
    public static final By TV_ALERT_SEVERITY = AppiumBy.id("com.pothole.khadda:id/tvAlertSeverity");
    public static final By TV_ALERT_DETAIL = AppiumBy.id("com.pothole.khadda:id/tvAlertDetail");

    public MonitorPage(AndroidDriver driver, WebDriverWait wait) {
        super(driver, wait);
    }

    public boolean isLoaded() {
        return isElementDisplayed(CARD_STATUS) && isElementDisplayed(BTN_TOGGLE_MONITORING);
    }

    public String getStatusText() {
        return getText(TV_STATUS);
    }

    public String getToggleMonitoringButtonText() {
        return getText(BTN_TOGGLE_MONITORING);
    }

    public void clickToggleMonitoring() {
        click(BTN_TOGGLE_MONITORING);
    }

    public void clickMarkPothole() {
        click(BTN_MARK_POTHOLE);
    }

    public void clickMarkSpeedBreaker() {
        click(BTN_MARK_SPEED_BREAKER);
    }

    public boolean isDemoModeChecked() {
        WebElement switchEl = waitForVisible(SWITCH_DEMO_MODE);
        String checkedAttr = switchEl.getAttribute("checked");
        return "true".equalsIgnoreCase(checkedAttr);
    }

    public void setDemoMode(boolean enable) {
        if (isDemoModeChecked() != enable) {
            click(SWITCH_DEMO_MODE);
        }
    }

    public String getPotholeCount() {
        return getText(TV_COUNT);
    }

    public String getSpeedText() {
        return getText(TV_SPEED);
    }

    public String getGpsText() {
        return getText(TV_GPS);
    }

    public boolean isWaveformDisplayed() {
        return isElementDisplayed(WAVEFORM_VIEW);
    }
}
