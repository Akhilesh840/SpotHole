package com.pothole.khadda.test.tests;

import com.pothole.khadda.test.base.BaseTest;
import com.pothole.khadda.test.pages.MainShellPage;
import com.pothole.khadda.test.pages.MapPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MapScreenTest extends BaseTest {

    @Test
    @DisplayName("TC05 - Map screen loads with MapView container, filter chips, and location controls")
    public void testMapScreenLoadsAndControlsPresent() {
        MainShellPage shell = new MainShellPage(getDriver(), getWait());
        MapPage mapPage = new MapPage(getDriver(), getWait());

        // Navigate to Map
        shell.navigateToMap();

        // 1. Verify MapView container is present
        assertThat(mapPage.isMapViewDisplayed())
                .as("osmdroid MapView container should be visible")
                .isTrue();

        // 2. Verify controls are present
        assertThat(mapPage.isLoaded())
                .as("Map screen and FAB controls should be loaded")
                .isTrue();

        // 3. Test filter chip interaction
        mapPage.selectChipHigh();
        assertThat(mapPage.isChipHighChecked())
                .as("High severity filter chip should be selected after click")
                .isTrue();

        // 4. Return to All
        mapPage.selectChipAll();
    }
}
