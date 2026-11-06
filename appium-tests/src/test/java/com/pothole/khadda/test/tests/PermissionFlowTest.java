package com.pothole.khadda.test.tests;

import com.pothole.khadda.test.base.BaseTest;
import com.pothole.khadda.test.config.TestConfig;
import com.pothole.khadda.test.pages.MonitorPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PermissionFlowTest extends BaseTest {

    private static final By BTN_ALLOW_FOREGROUND = AppiumBy.id("com.android.permissioncontroller:id/permission_allow_foreground_only_button");
    private static final By BTN_ALLOW_NOTIFICATION = AppiumBy.id("com.android.permissioncontroller:id/permission_allow_button");

    @Test
    @DisplayName("TC02 - Permission flow completes upon granting system dialogs and reaches active monitoring")
    public void testPermissionFlowGrantAndMonitoringStart() {
        MonitorPage monitor = new MonitorPage(getDriver(), getWait());
        String pkg = TestConfig.getAppPackage();

        // 1. Explicitly revoke permissions via ADB to trigger system dialogs
        try {
            getDriver().executeScript("mobile: shell", java.util.Map.of("command", "pm revoke " + pkg + " android.permission.ACCESS_FINE_LOCATION"));
            getDriver().executeScript("mobile: shell", java.util.Map.of("command", "pm revoke " + pkg + " android.permission.CAMERA"));
            getDriver().executeScript("mobile: shell", java.util.Map.of("command", "pm revoke " + pkg + " android.permission.POST_NOTIFICATIONS"));
        } catch (Exception e) {
            // Some device configurations may restrict shell pm revoke; proceed gracefully
        }

        // 2. Enable Demo mode and click Start Monitoring
        monitor.setDemoMode(true);
        monitor.clickToggleMonitoring();

        // 3. Handle Android 15 permission prompts if visible
        handlePermissionDialogs();

        // 4. Verify app reaches active monitoring state
        assertThat(monitor.getStatusText())
                .as("Status should transition to active Detecting state upon permission grant")
                .containsIgnoringCase("Detecting");

        // 5. Clean up
        monitor.clickToggleMonitoring();
    }

    private void handlePermissionDialogs() {
        // Handle sequential permission dialogs (Location -> Camera -> Notification)
        for (int i = 0; i < 3; i++) {
            try {
                List<WebElement> fgButtons = getDriver().findElements(BTN_ALLOW_FOREGROUND);
                if (!fgButtons.isEmpty() && fgButtons.get(0).isDisplayed()) {
                    fgButtons.get(0).click();
                    continue;
                }
                List<WebElement> notifButtons = getDriver().findElements(BTN_ALLOW_NOTIFICATION);
                if (!notifButtons.isEmpty() && notifButtons.get(0).isDisplayed()) {
                    notifButtons.get(0).click();
                }
            } catch (Exception ignored) {
                break;
            }
        }
    }
}
