package com.pothole.khadda.test.tests;

import com.pothole.khadda.test.base.BaseTest;
import com.pothole.khadda.test.config.TestConfig;
import com.pothole.khadda.test.pages.MainShellPage;
import com.pothole.khadda.test.pages.SettingsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SettingsPersistenceTest extends BaseTest {

    @Test
    @DisplayName("TC06 - Settings opens, a changed value persists after app restart")
    public void testSettingsChangedValuePersistsAfterRestart() {
        MainShellPage shell = new MainShellPage(getDriver(), getWait());
        SettingsPage settingsPage = new SettingsPage(getDriver(), getWait());

        // 1. Open Settings
        shell.openSettings();
        assertThat(settingsPage.isLoaded()).isTrue();

        // 2. Read initial state of audible sound switch and toggle it
        boolean originalSoundState = settingsPage.isSoundSwitchChecked();
        settingsPage.toggleSoundSwitch();
        boolean toggledState = settingsPage.isSoundSwitchChecked();
        assertThat(toggledState)
                .as("Switch state should have inverted after click")
                .isEqualTo(!originalSoundState);

        // 3. Restart the application process to test persistent storage (SharedPreferences)
        String appPackage = TestConfig.getAppPackage();
        getDriver().terminateApp(appPackage);
        getDriver().activateApp(appPackage);

        // 4. Re-open Settings screen
        shell.openSettings();
        assertThat(settingsPage.isLoaded()).isTrue();

        // 5. Verify the value persisted across restart
        boolean persistedState = settingsPage.isSoundSwitchChecked();
        assertThat(persistedState)
                .as("Sound switch state should persist after app termination and restart")
                .isEqualTo(toggledState);

        // 6. Restore original state for test cleanliness
        if (settingsPage.isSoundSwitchChecked() != originalSoundState) {
            settingsPage.toggleSoundSwitch();
        }
    }
}
