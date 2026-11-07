# Document 02: System Architecture Specification
**SpotHole (Khadda / खड्डा) — Comprehensive 4-Tier Architecture**

---

## 1. Architectural Overview & Design Pattern

SpotHole adopts the **Model-View-ViewModel (MVVM)** architectural pattern. The codebase is strictly partitioned into four functional tiers: **Sensing**, **Detection**, **Persistence**, and **Presentation**.

```
┌─────────────────────────────────────────────────────────────┐
│                      PRESENTATION TIER                      │
│   MapFragment        MonitorFragment      AuthorityFragment │
│   (osmdroid)         (Waveform Oscilloscope) (Municipal Lifecycle)
└──────────────────────────────▲──────────────────────────────┘
                               │ LiveData / StateFlow
┌──────────────────────────────┴──────────────────────────────┐
│                      PERSISTENCE TIER                       │
│     Room SQLite Database               TelemetryLogger      │
│  (PotholeDao / CandidateDao)      (Async 100Hz CSV Blackbox)│
└──────────────────────────────▲──────────────────────────────┘
                               │ Confirmed Events
┌──────────────────────────────┴──────────────────────────────┐
│                       DETECTION TIER                        │
│   DataProcessor       PotholeDetector     FalsePositiveFilter│
│(Gravity Projection)  (2-of-4 Voting)      (Kinematic Gates) │
│                              │                              │
│                    Candidate Verifier                       │
│               (Stage 2: YOLOv8n TFLite Veto)                │
└──────────────────────────────▲──────────────────────────────┘
                               │ Sensor & Frame Buffers
┌──────────────────────────────┴──────────────────────────────┐
│                        SENSING TIER                         │
│  SensorCollector    LocationTracker      CameraX RingBuffer │
│(100Hz req / 67.7Hz)  (Fused GPS)          (5 FPS, 25-frame) │
│                DetectionForegroundService                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Tier 1: Sensing Tier

### A. Lifecycle Management (`DetectionForegroundService.kt`)
* **Role:** Continuous background coordinator.
* **Why Foreground Service?** Android OS aggressively terminates background processes via Low Memory Killer (LMK) or Doze Mode. The Foreground Service runs with an ongoing persistent status bar notification, granting it elevated OS process priority (`Process.THREAD_PRIORITY_FOREGROUND`) so sensor acquisition never terminates when the phone screen is locked or navigating.

### B. High-Frequency Hardware Callbacks (`SensorCollector.kt`)
Registers listeners for four hardware sensors via `SensorManager`:
* `Sensor.TYPE_ACCELEROMETER`: Raw triaxial acceleration including gravity ($1g \approx 9.8\text{ m/s}^2$ at rest), used for `G-ZERO`.
* `Sensor.TYPE_LINEAR_ACCELERATION`: Triaxial acceleration with gravity mathematically subtracted by Android HAL.
* `Sensor.TYPE_GRAVITY`: Normalized Earth gravity direction vector.
* `Sensor.TYPE_GYROSCOPE`: 3-axis angular velocity in radians per second.
* **Sampling Rate:**
  * **Requested:** $10,000\ \mu\text{s}$ ($100\text{ Hz}$).
  * **Empirical Measured Delivery:** **$67.7\text{ Hz}$ ($14.77\text{ ms/sample}$)** due to non-deterministic Android HAL event dispatch overhead.

### C. GPS Location Tracking (`LocationTracker.kt`)
* Queries Android's `FusedLocationProviderClient` for high-accuracy WGS84 Latitude, Longitude, Altitude, Bearing, and GPS Velocity ($m/s$).

### D. CameraX Frame Ring Buffer (`DetectionForegroundService.kt`)
* Continuous video recording and live inference on every frame drains smartphone battery within ~45 minutes.
* Instead, Android **CameraX** captures low-overhead road preview frames at **5 FPS** into an in-memory **25-frame FIFO circular buffer**.
* This maintains a rolling 5.0-second visual window immediately preceding any physical shock impact without writing video to storage.

---

## 3. Tier 2: Detection Tier

### A. Orientation Reorientation & Preprocessing (`DataProcessor.kt`)
Because smartphones are mounted at arbitrary angles ($30^\circ\text{--}60^\circ$ toward the rider), raw X, Y, and Z axes do not align with Earth coordinates.
* **Euler-Free Gravity Projection:**
  $$\hat{\mathbf{g}} = \frac{\mathbf{g}}{\|\mathbf{g}\|}, \quad a_v = \mathbf{a}_{\text{linear}} \cdot \hat{\mathbf{g}}$$
  The dot product between linear acceleration $\mathbf{a}_{\text{linear}}$ and normalized unit gravity $\hat{\mathbf{g}}$ extracts pure **earth-referenced vertical acceleration ($a_v$)** without gimbal lock or manual calibration.
* **Moving Average Smoothing:** A 5-sample moving average window suppresses motorcycle engine vibration noise:
  $$a_{\text{smooth}}[t] = \frac{1}{5} \sum_{i=0}^{4} a_v[t-i]$$

### B. Stage 1: Consensus Voting Engine (`PotholeDetector.kt`)
Evaluates 4 algorithms derived from Mednis et al. (2011) and enforces a **2-of-4 majority voting rule**:
1. **`Z-DIFF`:** $|a_{\text{smooth}}[t] - a_{\text{smooth}}[t-9]| \ge 5.0\text{ m/s}^2$ (suspension compression impulse over 10 samples / ~148 ms).
2. **`Z-THRESH`:** $|a_{\text{smooth}}[t]| \ge 3.92\text{ m/s}^2$ ($0.40g$ vertical deflection).
3. **`STDEV(Z)`:** $\sigma(z_{t-19 \dots t}) \ge 5.0\text{ m/s}^2$ (sustained corrugated road roughness over 20 samples / ~295 ms).
4. **`G-ZERO`:** $M = \sqrt{a_x^2 + a_y^2 + a_z^2} \le 2.5\text{ m/s}^2$ for $\ge 3$ consecutive samples (momentary cavity free-fall drop).

### C. Kinematic Noise Rejection Gates (`FalsePositiveFilter.kt`)
Candidates that pass voting must survive three physical motion filters:
* **Hard Braking Gate:** Blocks triggers when horizontal deceleration $> 4.5\text{ m/s}^2$ with vertical impact $< 2.5\text{ m/s}^2$ (filters front-fork brake diving).
* **Sharp Turning Gate:** Blocks triggers when gyroscope $|\text{yaw}| > 0.3\text{ rad/s}$ ($17.2^\circ/\text{s}$ turn rate).
* **Roll/Pitch Gate:** Blocks triggers when total gyroscope magnitude $> 1.2\text{ rad/s}$ ($68.8^\circ/\text{s}$ bike lean or mount shake).
* **Deduplication & Cooldown:** Enforces a **1.5-second cooldown** and suppresses duplicate detections within a **15-meter GPS radius** for 3.0 seconds.

### D. Stage 2: On-Demand Vision Verification (`PotholeVisionAnalyzer.kt`, `CandidateVerifier.kt`)
* Triggered asynchronously via Kotlin Coroutines upon an accepted inertial candidate.
* Fetches the relevant pre-impact frame from the 25-frame ring buffer.
* **Letterboxing Preprocessor (`LetterboxUtil.kt`):** Letterboxes image with aspect-ratio preservation to $640 \times 640$ with RGB(114, 114, 114) padding (prevents the 93.9% accuracy collapse caused by direct stretching).
* **TFLite Interpreter:** Executes single-class YOLOv8n (`pothole_model.tflite`, 3.01M parameters) producing 8,400 anchor predictions.
* **Veto Decision:** If maximum detected pothole confidence is $< 0.25$, the candidate is rejected as a visual false alarm (e.g., speed breaker heave or joint). If $\ge 0.25$, it is committed as a verified pothole.

---

## 4. Tier 3: Persistence & Telemetry Tier

### A. Local SQLite Database (`Room`)
* **`PotholeEvent.kt`:** Entity storing confirmed potholes (`id`, `timestamp`, `latitude`, `longitude`, `altitude`, `severity`, `impactMagnitude`, `algorithmMask`, `imageUri`, `repairStatus`).
* **`CandidateEvent.kt`:** Entity storing all Stage 1 shock candidates for debugging and audit.
* **`PotholeDao.kt` / `CandidateDao.kt`:** Room DAOs offering reactive `Flow` and `LiveData` streams.

### B. High-Frequency Telemetry Blackbox (`TelemetryLogger.kt`)
Asynchronously logs session data to disk using a `ConcurrentLinkedQueue` on `Dispatchers.IO` without dropping 67.7 Hz sensor frames:
* **`sensor_log_*.csv`:** 100Hz timestamps, vertical acceleration, raw triaxial IMU vectors, gravity unit vectors, orientation angles, and speed.
* **`candidate_log_*.csv`:** Comprehensive audit trail recording every triggered shock and its exact rejection reason (e.g., *"Filtered: Sharp turning"*, *"Filtered: Speed below 10km/h"*, *"Vetoed: Vision confidence < 0.25"*).
* **`gps_log_*.csv`:** Geolocation traces.

---

## 5. Tier 4: Presentation Tier

### A. Live Monitor (`MonitorFragment.kt`, `LiveWaveformView.kt`)
* Real-time Canvas oscilloscope plotting vertical and triaxial acceleration at 60 FPS.
* Live telemetry HUD (GPS speed, detected count, hardware button indicators).
* Demo/Simulation toggle (`RoadSimulationManager.kt`) for bench testing without driving.

### B. Anomaly Mapping (`MapFragment.kt`)
* Integrated **OpenStreetMap via `osmdroid`** (no proprietary Google Maps API keys or billing quotas).
* Color-coded markers: 🔴 High ($\ge 6.0\text{ m/s}^2$), 🟡 Medium ($3.0\text{--}6.0\text{ m/s}^2$), 🟢 Low ($< 3.0\text{ m/s}^2$).

### C. Municipal Authority Workflow (`AuthorityFragment.kt`)
* Municipal road maintenance dashboard tracking potholes through lifecycle:
  $$\text{REPORTED} \longrightarrow \text{UNDER\_REVIEW} \longrightarrow \text{REPAIRED}$$

### D. Debug Visual Gallery (`DebugGalleryActivity.kt`)
* Visual inspection gallery displaying candidate camera frames with overlay bounding boxes and confidence scores.

### E. Session Export (`ReportExporter.kt`)
* Packages database records, CSV telemetry logs, and camera frames into a `.zip` archive via Android Share Sheet (`Intent.ACTION_SEND`).
