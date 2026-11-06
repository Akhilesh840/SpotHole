package com.pothole.khadda.test.tests;

import com.pothole.khadda.test.base.BaseTest;
import com.pothole.khadda.test.pages.MonitorPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MonitorLifecycleTest extends BaseTest {

    @Test
    @DisplayName("TC03 - Start monitoring changes visible state and posts foreground notification; stop restores idle state")
    public void testStartAndStopMonitoringLifecycle() {
        MonitorPage monitor = new MonitorPage(getDriver(), getWait());

        // Ensure initially in Standby
        assertThat(monitor.getStatusText()).containsIgnoringCase("Standby");

        // 1. Enable Demo mode so hardware GPS/sensors aren't strictly required
        monitor.setDemoMode(true);

        // 2. Click Start Monitoring
        monitor.clickToggleMonitoring();

        // 3. Verify visible UI changes to active state
        assertThat(monitor.getStatusText())
                .as("Status should change to Detecting Potholes")
                .containsIgnoringCase("Detecting");

        assertThat(monitor.getToggleMonitoringButtonText())
                .as("Button should change to Stop Monitoring")
                .containsIgnoringCase("Stop Monitoring");

        // 4. Open Notification shade and verify Foreground Service notification exists
        getDriver().openNotifications();
        try {
            List<WebElement> notifications = getDriver().findElements(
                    AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Pothole Detection Active\")")
            );
            assertThat(notifications)
                    .as("Foreground notification 'Pothole Detection Active' should exist in notification shade")
                    .isNotEmpty();
        } finally {
            // Close notification shade
            getDriver().navigate().back();
        }

        // 5. Click Stop Monitoring
        monitor.clickToggleMonitoring();

        // 6. Verify UI restores to Standby state
        assertThat(monitor.getStatusText())
                .as("Status should return to Standby after stopping")
                .containsIgnoringCase("Standby");

        assertThat(monitor.getToggleMonitoringButtonText())
                .as("Button should return to Start Monitoring")
                .containsIgnoringCase("Start Monitoring");
    }
}
