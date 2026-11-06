package com.pothole.khadda.test.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page object for the Road-Anomaly Map screen (fragment_map.xml).
 * Note: osmdroid map tiles render on a native canvas and are not individually
 * inspectable as DOM elements. The map view container and filter controls are tested.
 */
public class MapPage extends BasePage {

    public static final By MAP_VIEW = AppiumBy.id("com.pothole.khadda:id/mapView");
    public static final By CHIP_GROUP_SEVERITY = AppiumBy.id("com.pothole.khadda:id/chipGroupSeverity");
    public static final By CHIP_ALL = AppiumBy.id("com.pothole.khadda:id/chipAll");
    public static final By CHIP_HIGH = AppiumBy.id("com.pothole.khadda:id/chipHigh");
    public static final By CHIP_MEDIUM = AppiumBy.id("com.pothole.khadda:id/chipMedium");
    public static final By CHIP_LOW = AppiumBy.id("com.pothole.khadda:id/chipLow");
    public static final By FAB_MY_LOCATION = AppiumBy.id("com.pothole.khadda:id/fabMyLocation");

    public static final By CARD_MARKER_DETAIL = AppiumBy.id("com.pothole.khadda:id/cardMarkerDetail");
    public static final By TV_MARKER_SEVERITY = AppiumBy.id("com.pothole.khadda:id/tvMarkerSeverity");
    public static final By TV_MARKER_ALGORITHM = AppiumBy.id("com.pothole.khadda:id/tvMarkerAlgorithm");
    public static final By BTN_CLOSE_MARKER_DETAIL = AppiumBy.id("com.pothole.khadda:id/btnCloseMarkerDetail");

    public MapPage(AndroidDriver driver, WebDriverWait wait) {
        super(driver, wait);
    }

    public boolean isLoaded() {
        return isElementDisplayed(MAP_VIEW) && isElementDisplayed(FAB_MY_LOCATION);
    }

    public boolean isMapViewDisplayed() {
        return isElementDisplayed(MAP_VIEW);
    }

    public void selectChipAll() {
        click(CHIP_ALL);
    }

    public void selectChipHigh() {
        click(CHIP_HIGH);
    }

    public void selectChipMedium() {
        click(CHIP_MEDIUM);
    }

    public void selectChipLow() {
        click(CHIP_LOW);
    }

    public boolean isChipHighChecked() {
        WebElement chip = waitForVisible(CHIP_HIGH);
        return "true".equalsIgnoreCase(chip.getAttribute("checked"));
    }

    public void clickMyLocation() {
        click(FAB_MY_LOCATION);
    }

    public boolean isMarkerDetailDisplayed() {
        return isElementDisplayedQuick(CARD_MARKER_DETAIL, 2);
    }
}
