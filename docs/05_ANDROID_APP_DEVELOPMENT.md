# Document 05: Android Mobile Application & Development Guide
**SpotHole (Khadda / खड्डा) — Mobile Implementation Reference**

---

## 1. Android Project Structure

The complete Android Studio project is located in [`KhaddaApp/`](file:///d:/Khadda/Khadda/KhaddaApp):

```text
KhaddaApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/pothole/khadda/
│   │   │   ├── KhaddaApplication.kt        # Application Singleton
│   │   │   ├── data/
│   │   │   │   ├── local/                  # Room Database, Converters, DAOs
│   │   │   │   │   ├── AppDatabase.kt      # SQLite Database declaration
│   │   │   │   │   ├── PotholeDao.kt       # Pothole entity access queries
│   │   │   │   │   └── CandidateDao.kt     # Candidate shock audit queries
│   │   │   │   └── repository/             # Repository layer (MVVM)
│   │   │   ├── detector/
│   │   │   │   ├── DataProcessor.kt        # Gravity projection & smoothing
│   │   │   │   ├── PotholeDetector.kt      # 4 Mednis threshold algorithms
│   │   │   │   ├── FalsePositiveFilter.kt  # Kinematic rejection gates
│   │   │   │   ├── LetterboxUtil.kt        # 640x640 aspect-ratio letterboxing
│   │   │   │   ├── PotholeVisionAnalyzer.kt# TFLite YOLOv8n runtime interpreter
│   │   │   │   ├── PotholeOutputParser.kt  # 8,400 anchor prediction parser
│   │   │   │   └── CandidateVerifier.kt    # Dual-modal shock/vision correlator
│   │   │   ├── location/
│   │   │   │   └── LocationTracker.kt      # FusedLocationProvider GPS client
│   │   │   ├── model/
│   │   │   │   ├── PotholeEvent.kt         # Confirmed pothole entity
│   │   │   │   ├── CandidateEvent.kt       # Stage 1 shock candidate entity
│   │   │   │   ├── SensorData.kt           # Synchronized IMU sample data model
│   │   │   │   └── Enums.kt                # SeverityLevel, AlgorithmType, Status
│   │   │   ├── sensor/
│   │   │   │   ├── SensorCollector.kt      # 100Hz hardware callback manager
│   │   │   │   └── SensorStreamTracker.kt  # Rolling telemetry tracker
│   │   │   ├── service/
│   │   │   │   ├── DetectionForegroundService.kt # Core persistent coordinator
│   │   │   │   └── TelemetryLogger.kt      # Async non-blocking CSV blackbox
│   │   │   ├── simulation/
│   │   │   │   └── RoadSimulationManager.kt# In-lab synthetic telemetry engine
│   │   │   └── ui/
│   │   │       ├── MainActivity.kt         # Navigation shell & volume button overrides
│   │   │       ├── DebugGalleryActivity.kt # Candidate verification frame gallery
│   │   │       ├── monitor/                # Live HUD & waveform oscilloscope
│   │   │       ├── map/                    # osmdroid OpenStreetMap interface
│   │   │       ├── authority/              # Municipal road repair dashboard
│   │   │       ├── history/                # Logged events list & status editor
│   │   │       ├── report/                 # Statistical reports & ZIP exporter
│   │   │       ├── settings/               # Threshold sliders & preferences
│   │   │       └── custom/
│   │   │           └── LiveWaveformView.kt # 60 FPS Canvas acceleration plotter
│   │   ├── assets/
│   │   │   └── pothole_model.tflite        # Deployed YOLOv8n edge model (640x640)
│   │   └── res/                            # XML layouts, drawables, menus
│   └── src/test/                           # Unit test suites
├── build.gradle.kts                        # Module build configuration
└── settings.gradle.kts                     # Project settings
```

---

## 2. Core Service & Sensor Pipeline

### Persistent Background Execution (`DetectionForegroundService.kt`)
* Implements an Android `ForegroundService` with notification channel `khadda_sensing_channel`.
* Prevents process termination by Android Doze and battery managers.
* Instantiates `SensorCollector`, `LocationTracker`, `DataProcessor`, `PotholeDetector`, `FalsePositiveFilter`, `CandidateVerifier`, and `TelemetryLogger`.

### Hardware Callbacks (`SensorCollector.kt`)
* Registers listeners for Accelerometer, Gyroscope, Linear Acceleration, and Gravity sensors.
* Target rate: `10000` $\mu\text{s}$ ($100\text{ Hz}$).
* Synchronizes sensor readings into a unified `SensorData` data class with nanosecond timestamps (`timestampNs`).

### Dynamic Gravity Vector Projection (`DataProcessor.kt`)
* Projects 3-axis linear acceleration onto the unit gravity vector:
  $$a_v = \mathbf{a}_{\text{linear}} \cdot \hat{\mathbf{g}}$$
* Computes 5-sample smoothed vertical acceleration $a_{\text{smooth}}$.
* Computes $Z_{\text{diff}}$ over 10 samples ($148\text{ ms}$) and rolling standard deviation over 20 samples ($295\text{ ms}$).

---

## 3. On-Demand Edge Vision Implementation

### CameraX 25-Frame Ring Buffer (`DetectionForegroundService.kt`)
* Configures `CameraX` `ImageAnalysis` at **5 FPS**.
* Frames are held in an in-memory `ArrayDeque<Bitmap>` capped at 25 frames (~5.0 seconds of road imagery).
* Frames are discarded automatically unless promoted by an inertial trigger, saving flash storage and RAM.

### TFLite Interpreter (`PotholeVisionAnalyzer.kt`)
* Loads `pothole_model.tflite` from `app/src/main/assets/`.
* Supported tensor layouts:
  * Channels First: `[1, 5, 8400]`
  * Channels Last: `[1, 8400, 5]`
* **Preprocessing:**
  * Rotates frame by `rotationDegrees` to align with the road plane.
  * Applies **Letterboxing** via [`LetterboxUtil.kt`](file:///d:/Khadda/Khadda/KhaddaApp/app/src/main/java/com/pothole/khadda/detector/LetterboxUtil.kt) with aspect-ratio preservation to $640 \times 640$ with RGB(114, 114, 114) padding.
  * Normalizes pixels to $[0.0, 1.0]$.
* **Veto Decision:** Bounding box confidence $< 0.25$ rejects the candidate shock.

---

## 4. Local Database & Asynchronous Telemetry

### Room SQLite Database (`AppDatabase.kt`)
* **`PotholeEvent` Entity:** Stores confirmed potholes.
* **`CandidateEvent` Entity:** Stores all candidate shocks for verification analysis.
* **Database Migration & Safety:** Auto-managed with Room SQLite schema versioning.

### Non-Blocking Telemetry Logger (`TelemetryLogger.kt`)
* To ensure the 67.7 Hz sensing thread never drops frames or suffers UI stalls, log records are pushed into a thread-safe `ConcurrentLinkedQueue`.
* A background Kotlin Coroutine (`Dispatchers.IO`) flushes the queue into disk CSV files:
  * `sensor_log_*.csv`: Raw 100Hz IMU vectors, vertical acceleration, and speed.
  * `candidate_log_*.csv`: Full decision audit log with exact rejection reasons.
  * `gps_log_*.csv`: Geolocation tracking.

---

## 5. UI Features & Diagnostic Tools

### 1. Live Waveform Oscilloscope (`LiveWaveformView.kt`)
A custom hardware-accelerated Canvas component that plots:
* Triaxial Acceleration ($X$: Red, $Y$: Green, $Z$: Cyan).
* Projected Vertical Shock ($a_v$: White/Yellow).
* Live 60 FPS sweep line rendering.

### 2. OpenStreetMap Integration (`MapFragment.kt`)
* Powered by `osmdroid` (no Google Play Services or API keys required).
* Pins potholes with severity markers:
  * 🔴 Crimson ($\ge 6.0\text{ m/s}^2$)
  * 🟡 Amber ($3.0\text{--}6.0\text{ m/s}^2$)
  * 🟢 Emerald ($< 3.0\text{ m/s}^2$)
* Tap any marker to view coordinates, impact force, and current repair status.

### 3. Road Maintenance Authority Dashboard (`AuthorityFragment.kt`)
* Allows municipal evaluators to inspect detected potholes and transition repair states:
  $$\text{REPORTED} \longrightarrow \text{UNDER\_REVIEW} \longrightarrow \text{REPAIRED}$$

### 4. Classroom / Bench Demo Mode (`RoadSimulationManager.kt`)
* Toggleable via the "Demo" switch on the Monitor toolbar.
* Synthesizes realistic vehicle telemetry and pothole waveforms without needing a moving motorcycle.

### 5. Hardware Button Overrides (`MainActivity.kt`)
* Because interacting with a touchscreen while riding is hazardous:
  * **Volume UP:** Injects a `MANUAL_POTHOLE` ground-truth tag into the telemetry stream.
  * **Volume DOWN:** Injects a `MANUAL_SPEED_BREAKER` tag into the telemetry stream.

---

## 6. How to Run Tests

### Unit Tests
```bash
cd KhaddaApp
.\gradlew.bat test
```
* **`PotholeDetectorTest`:** Validates `Z-DIFF`, `Z-THRESH`, `STDEV(Z)`, and `G-ZERO` algorithms against synthetic g-force impulses.
* **`FalsePositiveFilterTest`:** Validates braking rejection, cornering rejection, and duplicate suppression.
* **`CandidateTransitionTest`:** Validates dual-modal candidate state transitions.
* **`LetterboxUtilTest`:** Validates camera letterboxing coordinate transformations.

### Appium Mobile Automation Tests (`appium-tests/`)
```bash
cd appium-tests
.\gradlew.bat test
```
Executes end-to-end UI tests across App Launch, Bottom Navigation, Map View, Monitor Lifecycle, and Settings persistence.
