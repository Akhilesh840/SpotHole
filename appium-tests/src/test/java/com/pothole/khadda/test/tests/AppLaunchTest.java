package com.pothole.khadda.test.tests;

import com.pothole.khadda.test.base.BaseTest;
import com.pothole.khadda.test.pages.MainShellPage;
import com.pothole.khadda.test.pages.MonitorPage;
import com.pothole.khadda.test.pages.SettingsPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;

import static org.assertj.core.api.Assertions.assertThat;

public class AppLaunchTest extends BaseTest {

    @Test
    @DisplayName("TC01 - App launches and the main screen shows with standby status")
    public void testAppLaunchesAndMainScreenShows() {
        MainShellPage shell = new MainShellPage(getDriver(), getWait());
        MonitorPage monitor = new MonitorPage(getDriver(), getWait());

        // Verify top toolbar is displayed
        assertThat(shell.isToolbarDisplayed())
                .as("Main top toolbar should be visible upon launch")
                .isTrue();

        // Verify monitor screen loaded in default state
        assertThat(monitor.isLoaded())
                .as("Live Monitor controls should be loaded upon launch")
                .isTrue();

        // Verify initial standby telemetry
        assertThat(monitor.getStatusText())
                .as("Initial status should be Standby")
                .containsIgnoringCase("Standby");

        assertThat(monitor.getToggleMonitoringButtonText())
                .as("Button should prompt user to start monitoring")
                .containsIgnoringCase("Start Monitoring");

        assertThat(monitor.getPotholeCount())
                .as("Initial session pothole count should be 0")
                .isEqualTo("0");
    }

    @Test
    @DisplayName("Sample Test - Find element by resource ID using AppiumBy.id and click it")
    public void testFindElementByResourceIdAndClickUsingAppiumById() {
        // Explicitly demonstrates finding by resource ID with AppiumBy.id as requested in prompt
        WebElement settingsButton = getDriver().findElement(AppiumBy.id("com.pothole.khadda:id/btnSettings"));
        assertThat(settingsButton).isNotNull();
        settingsButton.click();

        // Verify Settings screen opens
        SettingsPage settingsPage = new SettingsPage(getDriver(), getWait());
        assertThat(settingsPage.isLoaded())
                .as("Settings screen should load after clicking settings button")
                .isTrue();
    }
}
