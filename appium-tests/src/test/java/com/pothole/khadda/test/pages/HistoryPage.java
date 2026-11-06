package com.pothole.khadda.test.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

/**
 * Page object for the Detection Log / History screen (fragment_history.xml).
 */
public class HistoryPage extends BasePage {

    public static final By TV_TOTAL_COUNT = AppiumBy.id("com.pothole.khadda:id/tvTotalCount");
    public static final By BTN_EXPORT = AppiumBy.id("com.pothole.khadda:id/btnExport");
    public static final By BTN_CLEAR_ALL = AppiumBy.id("com.pothole.khadda:id/btnClearAll");
    public static final By RECYCLER_VIEW_POTHOLES = AppiumBy.id("com.pothole.khadda:id/recyclerViewPotholes");
    public static final By TV_EMPTY_STATE = AppiumBy.id("com.pothole.khadda:id/tvEmptyState");

    // Item card locators
    public static final By CARD_SEVERITY_BADGE = AppiumBy.id("com.pothole.khadda:id/tvSeverityBadge");
    public static final By CARD_ALGORITHM = AppiumBy.id("com.pothole.khadda:id/tvAlgorithm");
    public static final By CARD_IMPACT = AppiumBy.id("com.pothole.khadda:id/tvImpact");
    public static final By CARD_LOCATION = AppiumBy.id("com.pothole.khadda:id/tvLocation");

    public HistoryPage(AndroidDriver driver, WebDriverWait wait) {
        super(driver, wait);
    }

    public boolean isLoaded() {
        return isElementDisplayed(TV_TOTAL_COUNT);
    }

    public String getTotalCountHeader() {
        return getText(TV_TOTAL_COUNT);
    }

    public void clickClearAll() {
        click(BTN_CLEAR_ALL);
    }

    public void clickExport() {
        click(BTN_EXPORT);
    }

    public boolean isEmptyStateDisplayed() {
        return isElementDisplayedQuick(TV_EMPTY_STATE, 3);
    }

    public int getItemCount() {
        List<WebElement> items = driver.findElements(CARD_SEVERITY_BADGE);
        return items.size();
    }

    public String getFirstItemSeverity() {
        return getText(CARD_SEVERITY_BADGE);
    }

    public String getFirstItemAlgorithm() {
        return getText(CARD_ALGORITHM);
    }
}
