package com.pothole.khadda.test.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page object for the MainActivity chrome: top toolbar, settings/gallery buttons,
 * bottom navigation bar, and persistent background validation banner.
 */
public class MainShellPage extends BasePage {

    public static final By TOOLBAR = AppiumBy.id("com.pothole.khadda:id/toolbar");
    public static final By BTN_SETTINGS = AppiumBy.id("com.pothole.khadda:id/btnSettings");
    public static final By BTN_GALLERY = AppiumBy.id("com.pothole.khadda:id/btnGallery");

    public static final By BOTTOM_NAV = AppiumBy.id("com.pothole.khadda:id/bottom_navigation");
    public static final By NAV_MONITOR = AppiumBy.id("com.pothole.khadda:id/nav_monitor");
    public static final By NAV_MAP = AppiumBy.id("com.pothole.khadda:id/nav_map");
    public static final By NAV_HISTORY = AppiumBy.id("com.pothole.khadda:id/nav_history");
    public static final By NAV_AUTHORITY = AppiumBy.id("com.pothole.khadda:id/nav_authority");
    public static final By NAV_REPORT = AppiumBy.id("com.pothole.khadda:id/nav_report");

    public static final By CARD_VERIFICATION_BANNER = AppiumBy.id("com.pothole.khadda:id/cardVerificationBanner");
    public static final By TV_BANNER_TITLE = AppiumBy.id("com.pothole.khadda:id/tvBannerTitle");
    public static final By TV_BANNER_PERCENT = AppiumBy.id("com.pothole.khadda:id/tvBannerPercent");

    public MainShellPage(AndroidDriver driver, WebDriverWait wait) {
        super(driver, wait);
    }

    public boolean isToolbarDisplayed() {
        return isElementDisplayed(TOOLBAR);
    }

    public void openSettings() {
        click(BTN_SETTINGS);
    }

    public void openDebugGallery() {
        click(BTN_GALLERY);
    }

    public void navigateToMonitor() {
        click(NAV_MONITOR);
    }

    public void navigateToMap() {
        click(NAV_MAP);
    }

    public void navigateToHistory() {
        click(NAV_HISTORY);
    }

    public void navigateToAuthority() {
        click(NAV_AUTHORITY);
    }

    public void navigateToReport() {
        click(NAV_REPORT);
    }

    public boolean isVerificationBannerDisplayed() {
        return isElementDisplayedQuick(CARD_VERIFICATION_BANNER, 2);
    }
}
