package com.pothole.khadda.test.base;

import com.pothole.khadda.test.config.TestConfig;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;

/**
 * Base test class managing the Appium AndroidDriver lifecycle, explicit wait configuration,
 * and UiAutomator2Options initialization. Uses no Thread.sleep and captures screenshots on failure.
 */
@ExtendWith(TestFailureListener.class)
public abstract class BaseTest {

    private static final ThreadLocal<AndroidDriver> DRIVER = new ThreadLocal<>();
    private static final ThreadLocal<WebDriverWait> WAIT = new ThreadLocal<>();

    public static AndroidDriver getDriver() {
        return DRIVER.get();
    }

    public static WebDriverWait getWait() {
        return WAIT.get();
    }

    @BeforeEach
    public void setUp() {
        String apkPath = TestConfig.getApkPath();
        File apkFile = new File(apkPath);
        if (!apkFile.exists()) {
            throw(new IllegalStateException(
                "Debug APK not found at: " + apkPath +
                ". Please build the APK first via './gradlew assembleDebug' in the KhaddaApp project."
            ));
        }

        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName("Android")
                .setAutomationName("UiAutomator2")
                .setUdid(TestConfig.getDeviceUdid())
                .setApp(apkFile.getAbsolutePath())
                .setAppPackage(TestConfig.getAppPackage())
                .setAppActivity(TestConfig.getAppActivity())
                .setAutoGrantPermissions(TestConfig.isAutoGrantPermissions())
                .setNewCommandTimeout(Duration.ofSeconds(TestConfig.getNewCommandTimeout()))
                .setNoReset(TestConfig.isNoReset())
                .setFullReset(TestConfig.isFullReset())
                .setAndroidInstallTimeout(Duration.ofMinutes(2));

        AndroidDriver driver = new AndroidDriver(TestConfig.getServerUrl(), options);
        DRIVER.set(driver);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(TestConfig.getExplicitWaitSeconds()));
        WAIT.set(wait);
    }

    @AfterEach
    public void tearDown() {
        AndroidDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                DRIVER.remove();
                WAIT.remove();
            }
        }
    }
}
