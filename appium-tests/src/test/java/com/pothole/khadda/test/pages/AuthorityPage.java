package com.pothole.khadda.test.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Page object for the Authority Dashboard screen (fragment_authority.xml).
 */
public class AuthorityPage extends BasePage {

    public static final By TV_AUTHORITY_TITLE = AppiumBy.id("com.pothole.khadda:id/tvAuthorityTitle");
    public static final By CHIP_GROUP_STATUS = AppiumBy.id("com.pothole.khadda:id/chipGroupStatus");
    public static final By CHIP_STATUS_ALL = AppiumBy.id("com.pothole.khadda:id/chipStatusAll");
    public static final By CHIP_STATUS_REPORTED = AppiumBy.id("com.pothole.khadda:id/chipStatusReported");
    public static final By CHIP_STATUS_REVIEW = AppiumBy.id("com.pothole.khadda:id/chipStatusReview");
    public static final By CHIP_STATUS_REPAIRED = AppiumBy.id("com.pothole.khadda:id/chipStatusRepaired");
    public static final By RECYCLER_VIEW_AUTHORITY = AppiumBy.id("com.pothole.khadda:id/recyclerViewAuthority");
    public static final By TV_EMPTY_AUTHORITY = AppiumBy.id("com.pothole.khadda:id/tvEmptyAuthority");

    public AuthorityPage(AndroidDriver driver, WebDriverWait wait) {
        super(driver, wait);
    }

    public boolean isLoaded() {
        return isElementDisplayed(TV_AUTHORITY_TITLE) && isElementDisplayed(CHIP_GROUP_STATUS);
    }

    public String getTitleText() {
        return getText(TV_AUTHORITY_TITLE);
    }

    public void selectChipReported() {
        click(CHIP_STATUS_REPORTED);
    }

    public void selectChipRepaired() {
        click(CHIP_STATUS_REPAIRED);
    }
}
