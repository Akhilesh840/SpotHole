package com.pothole.khadda.test.tests;

import com.pothole.khadda.test.base.BaseTest;
import com.pothole.khadda.test.pages.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BottomNavigationTest extends BaseTest {

    @Test
    @DisplayName("TC07 - Seamless bottom navigation switching across all 5 application sections")
    public void testBottomNavigationTabSwitching() {
        MainShellPage shell = new MainShellPage(getDriver(), getWait());
        MonitorPage monitorPage = new MonitorPage(getDriver(), getWait());
        MapPage mapPage = new MapPage(getDriver(), getWait());
        HistoryPage historyPage = new HistoryPage(getDriver(), getWait());
        AuthorityPage authorityPage = new AuthorityPage(getDriver(), getWait());
        AnalyticsPage analyticsPage = new AnalyticsPage(getDriver(), getWait());

        // 1. Initial tab: Monitor
        assertThat(monitorPage.isLoaded()).isTrue();

        // 2. Switch to Map
        shell.navigateToMap();
        assertThat(mapPage.isLoaded()).isTrue();

        // 3. Switch to History
        shell.navigateToHistory();
        assertThat(historyPage.isLoaded()).isTrue();

        // 4. Switch to Authority
        shell.navigateToAuthority();
        assertThat(authorityPage.isLoaded()).isTrue();

        // 5. Switch to Report / Analytics
        shell.navigateToReport();
        assertThat(analyticsPage.isLoaded()).isTrue();

        // 6. Return to Monitor
        shell.navigateToMonitor();
        assertThat(monitorPage.isLoaded()).isTrue();
    }
}
