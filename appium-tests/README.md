# Khadda Appium UI Automation Test Suite

Standalone UI test automation suite for the **Khadda Android App** built in **Java 17**, **JUnit 5**, and **Appium Java-Client 9.4.0** with **UiAutomator2**.

---

## 1. Technical Architecture & Constraints

- **Scope**: Tests user interface, navigation flows, permission dialogs, simulation mode toggles, and data persistence.
- **Explicit Scope Limitation**: Appium does **not** test real accelerometer vibrations or camera ML detection quality. In accordance with system specifications, all test flows requiring events trigger the internal simulation mode (`RoadSimulationManager`) or manual event injectors.
- **Framework & Standards**:
  - Host runtime: Java 17 / OpenJDK 21.
  - Test framework: JUnit 5 (`org.junit.jupiter:junit-jupiter:5.11.4`).
  - Appium Client: `io.appium:java-client:9.4.0` (using `UiAutomator2Options`; deprecated `DesiredCapabilities` is strictly excluded).
  - Explicit waits: Managed via `WebDriverWait` (`Thread.sleep` is disallowed).
  - Automated failure screenshots: Handled via `TestFailureListener` implementing JUnit 5's `TestWatcher`.

---

## 2. Directory Structure

```
D:\Khadda\Khadda\appium-tests\
├── build.gradle                   # Gradle dependencies, Java 17 compatibility, JUnit Platform
├── settings.gradle                # Project root definition: khadda-appium-tests
├── run_gradlew.ps1                # PowerShell runner ensuring JAVA_HOME and PATH are set
├── README.md                      # Setup, execution instructions, and capabilities guide
└── src/test/
    ├── resources/
    │   └── config.properties      # Default device UDID, APK path, server URL, timeouts
    └── java/com/pothole/khadda/test/
        ├── config/
        │   └── TestConfig.java    # Dynamic config reader (System props -> Env -> Properties)
        ├── base/
        │   ├── BaseTest.java      # Driver lifecycle with UiAutomator2Options & WebDriverWait
        │   └── TestFailureListener.java # Automated screenshot capture on test failure
        ├── pages/                 # Page Object Model classes
        │   ├── BasePage.java
        │   ├── MainShellPage.java
        │   ├── MonitorPage.java
        │   ├── MapPage.java
        │   ├── HistoryPage.java
        │   ├── SettingsPage.java
        │   ├── AuthorityPage.java
        │   ├── AnalyticsPage.java
        │   └── DebugGalleryPage.java
        └── tests/                 # Automated test specifications
            ├── AppLaunchTest.java
            ├── PermissionFlowTest.java
            ├── MonitorLifecycleTest.java
            ├── SimulationModeTest.java
            ├── MapScreenTest.java
            ├── SettingsPersistenceTest.java
            └── BottomNavigationTest.java
```

---

## 3. Windows 11 Prerequisites & Setup

### Environment Variables
Run PowerShell as Administrator or User to persist the environment:
```powershell
[Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Android\Android Studio\jbr', 'User')
[Environment]::SetEnvironmentVariable('ANDROID_HOME', 'C:\Users\ASUS TUF\AppData\Local\Android\Sdk', 'User')

$userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
$entries = @(
    'C:\Program Files\Android\Android Studio\jbr\bin',
    'C:\Users\ASUS TUF\AppData\Local\Android\Sdk\platform-tools',
    'C:\Users\ASUS TUF\AppData\Roaming\npm'
)
foreach ($e in $entries) {
    if ($userPath -notlike "*$e*") { $userPath = "$e;$userPath" }
}
[Environment]::SetEnvironmentVariable('Path', $userPath, 'User')
```

### Appium Server & Driver Installation
```powershell
# 1. Install Appium v3 globally
npm install -g appium

# 2. Install UiAutomator2 driver
appium driver install uiautomator2

# 3. Verify driver diagnostics
appium driver doctor uiautomator2
```

---

## 4. Running the Tests

### Step 1: Start the Appium Server
In a terminal, start Appium listening on port 4723:
```powershell
appium --address 127.0.0.1 --port 4723
```
*Note: In Appium 2.x/3.x, the default base path is `/` (http://127.0.0.1:4723/).*

### Step 2: Verify Device Connection
Ensure your target device (moto g64 5G or connected test device) is visible:
```powershell
adb devices -l
```

### Step 3: Execute Tests
Run all tests via Gradle:
```powershell
cd D:\Khadda\Khadda\appium-tests
.\run_gradlew.ps1 test
```

To target a specific device UDID via command line override:
```powershell
.\run_gradlew.ps1 test -Dappium.device.udid=ZA222L6CYV
```

To run an individual test class (e.g., AppLaunchTest):
```powershell
.\run_gradlew.ps1 test --tests "com.pothole.khadda.test.tests.AppLaunchTest"
```

---

## 5. Reports & Artifacts

- **HTML Test Report**: `build/reports/tests/test/index.html`
- **JUnit XML Report**: `build/test-results/test/`
- **Failure Screenshots**: `build/reports/screenshots/<TestClass>_<method>_<timestamp>.png`
