package com.pothole.khadda.test.tests;

import com.pothole.khadda.test.base.BaseTest;
import com.pothole.khadda.test.pages.HistoryPage;
import com.pothole.khadda.test.pages.MainShellPage;
import com.pothole.khadda.test.pages.MonitorPage;
import io.appium.java_client.AppiumBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SimulationModeTest extends BaseTest {

    private static final By BTN_NAVIGATE_APP = AppiumBy.id("com.pothole.khadda:id/btnNavigateApp");
    private static final By BTN_DIALOG_CLOSE = AppiumBy.id("com.pothole.khadda:id/btnDialogClose");

    @Test
    @DisplayName("TC04 - Simulation mode produces stored anomaly event visible in History tab")
    public void testSimulationModeProducesStoredEventInHistory() {
        MainShellPage shell = new MainShellPage(getDriver(), getWait());
        MonitorPage monitor = new MonitorPage(getDriver(), getWait());
        HistoryPage history = new HistoryPage(getDriver(), getWait());

        // 1. Enable Demo Mode
        monitor.setDemoMode(true);

        // 2. Start Monitoring
        monitor.clickToggleMonitoring();
        assertThat(monitor.getStatusText()).containsIgnoringCase("Detecting");

        // 3. Trigger simulated/manual anomaly
        monitor.clickMarkPothole();

        // 4. Stop Monitoring to trigger verification and persistence
        monitor.clickToggleMonitoring();

        // 5. Dismiss verification popup if visible
        dismissValidationDialogIfPresent();

        // 6. Navigate to History screen
        shell.navigateToHistory();
        assertThat(history.isLoaded()).isTrue();

        // 7. Wait for at least one pothole event card in the history list
        getWait().until(driver -> history.getItemCount() > 0 || !history.isEmptyStateDisplayed());

        assertThat(history.getItemCount())
                .as("History list should contain at least one detected event card")
                .isGreaterThanOrEqualTo(1);
    }

    private void dismissValidationDialogIfPresent() {
        try {
            List<WebElement> navButtons = getDriver().findElements(BTN_NAVIGATE_APP);
            if (!navButtons.isEmpty() && navButtons.get(0).isDisplayed()) {
                navButtons.get(0).click();
                return;
            }
            List<WebElement> closeButtons = getDriver().findElements(BTN_DIALOG_CLOSE);
            if (!closeButtons.isEmpty() && closeButtons.get(0).isDisplayed()) {
                closeButtons.get(0).click();
            }
        } catch (Exception ignored) {
            // Popup may have auto-dismissed
        }
    }
}
