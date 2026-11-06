package com.pothole.khadda.test.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page object for the Debug Vision Gallery screen (activity_debug_gallery.xml).
 */
public class DebugGalleryPage extends BasePage {

    public static final By BTN_SHOW_CONFIRMED = AppiumBy.id("com.pothole.khadda:id/btnShowConfirmed");
    public static final By BTN_SHOW_REJECTED = AppiumBy.id("com.pothole.khadda:id/btnShowRejected");
    public static final By IMAGE_CONTAINER = AppiumBy.id("com.pothole.khadda:id/imageContainer");

    public DebugGalleryPage(AndroidDriver driver, WebDriverWait wait) {
        super(driver, wait);
    }

    public boolean isLoaded() {
        return isElementDisplayed(BTN_SHOW_CONFIRMED) && isElementDisplayed(BTN_SHOW_REJECTED);
    }

    public void clickConfirmed() {
        click(BTN_SHOW_CONFIRMED);
    }

    public void clickRejected() {
        click(BTN_SHOW_REJECTED);
    }
}
