package com.pothole.khadda.test.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page object for the Algorithm Settings screen (fragment_settings.xml).
 */
public class SettingsPage extends BasePage {

    public static final By TV_LABEL_ZDIFF = AppiumBy.id("com.pothole.khadda:id/tvLabelZDiff");
    public static final By SLIDER_ZDIFF = AppiumBy.id("com.pothole.khadda:id/sliderZDiff");
    public static final By TV_LABEL_ZTHRESH = AppiumBy.id("com.pothole.khadda:id/tvLabelZThresh");
    public static final By SLIDER_ZTHRESH = AppiumBy.id("com.pothole.khadda:id/sliderZThresh");
    public static final By TV_LABEL_STDEV = AppiumBy.id("com.pothole.khadda:id/tvLabelStdev");
    public static final By SLIDER_STDEV = AppiumBy.id("com.pothole.khadda:id/sliderStdev");
    public static final By TV_LABEL_GZERO = AppiumBy.id("com.pothole.khadda:id/tvLabelGZero");
    public static final By SLIDER_GZERO = AppiumBy.id("com.pothole.khadda:id/sliderGZero");

    public static final By SWITCH_SOUND = AppiumBy.id("com.pothole.khadda:id/switchSound");
    public static final By SWITCH_VIBRATE = AppiumBy.id("com.pothole.khadda:id/switchVibrate");
    public static final By SWITCH_SETTINGS_DEMO = AppiumBy.id("com.pothole.khadda:id/switchSettingsDemo");
    public static final By BTN_RESET_DEFAULTS = AppiumBy.id("com.pothole.khadda:id/btnResetDefaults");

    public SettingsPage(AndroidDriver driver, WebDriverWait wait) {
        super(driver, wait);
    }

    public boolean isLoaded() {
        return isElementDisplayed(SLIDER_ZDIFF) && isElementDisplayed(BTN_RESET_DEFAULTS);
    }

    public String getZDiffLabelText() {
        return getText(TV_LABEL_ZDIFF);
    }

    public boolean isSoundSwitchChecked() {
        WebElement el = waitForVisible(SWITCH_SOUND);
        return "true".equalsIgnoreCase(el.getAttribute("checked"));
    }

    public void toggleSoundSwitch() {
        click(SWITCH_SOUND);
    }

    public boolean isVibrationSwitchChecked() {
        WebElement el = waitForVisible(SWITCH_VIBRATE);
        return "true".equalsIgnoreCase(el.getAttribute("checked"));
    }

    public void toggleVibrationSwitch() {
        click(SWITCH_VIBRATE);
    }

    public boolean isDemoSwitchChecked() {
        WebElement el = waitForVisible(SWITCH_SETTINGS_DEMO);
        return "true".equalsIgnoreCase(el.getAttribute("checked"));
    }

    public void toggleDemoSwitch() {
        click(SWITCH_SETTINGS_DEMO);
    }

    public void clickResetDefaults() {
        click(BTN_RESET_DEFAULTS);
    }
}
