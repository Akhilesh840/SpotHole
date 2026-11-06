package com.pothole.khadda.test.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Loads configuration from config.properties, with priority given to
 * JVM system properties (-Dprop=val) and OS environment variables.
 * Resolves all paths dynamically to prevent hardcoded absolute paths.
 */
public final class TestConfig {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream is = TestConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                PROPERTIES.load(is);
            } else {
                System.err.println("WARN: config.properties not found in classpath. Relying on defaults/system properties.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    private TestConfig() {}

    public static String get(String key, String defaultValue) {
        // Priority: 1. System property, 2. Env variable, 3. Properties file, 4. Default
        String sysProp = System.getProperty(key);
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            return sysProp.trim();
        }

        String envKey = key.toUpperCase().replace('.', '_');
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.trim().isEmpty()) {
            return envVal.trim();
        }

        return PROPERTIES.getProperty(key, defaultValue);
    }

    public static String get(String key) {
        return get(key, "");
    }

    public static URL getServerUrl() {
        try {
            String urlStr = get("appium.server.url", "http://127.0.0.1:4723/");
            return URI.create(urlStr).toURL();
        } catch (Exception e) {
            throw new RuntimeException("Invalid Appium server URL: " + get("appium.server.url"), e);
        }
    }

    public static String getDeviceUdid() {
        return get("appium.device.udid", "ZA222L6CYV");
    }

    public static String getAppPackage() {
        return get("appium.app.package", "com.pothole.khadda");
    }

    public static String getAppActivity() {
        return get("appium.app.activity", "com.pothole.khadda.ui.MainActivity");
    }

    public static boolean isAutoGrantPermissions() {
        return Boolean.parseBoolean(get("appium.auto.grant.permissions", "true"));
    }

    public static int getNewCommandTimeout() {
        return Integer.parseInt(get("appium.new.command.timeout", "120"));
    }

    public static boolean isNoReset() {
        return Boolean.parseBoolean(get("appium.no.reset", "false"));
    }

    public static boolean isFullReset() {
        return Boolean.parseBoolean(get("appium.full.reset", "false"));
    }

    public static int getExplicitWaitSeconds() {
        return Integer.parseInt(get("explicit.wait.seconds", "15"));
    }

    /**
     * Resolves APK path dynamically without hardcoded machine paths.
     * Looks at user-configured path, resolving relative paths against the project root.
     */
    public static String getApkPath() {
        String configuredPath = get("app.apk.path", "../KhaddaApp/app/build/outputs/apk/debug/app-debug.apk");
        File file = new File(configuredPath);
        if (!file.isAbsolute()) {
            Path projectDir = Paths.get("").toAbsolutePath();
            file = projectDir.resolve(configuredPath).normalize().toFile();
        }

        if (!file.exists()) {
            // Check direct sibling fallback
            File fallback = new File("../KhaddaApp/app/build/outputs/apk/debug/app-debug.apk");
            if (fallback.exists()) {
                return fallback.getAbsolutePath();
            }
        }
        return file.getAbsolutePath();
    }
}
