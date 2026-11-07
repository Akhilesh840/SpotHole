# SpotHole (Khadda / खड्डा) — Comprehensive System Architecture
**Edge-AI & Multi-Modal Passive Sensing Platform for Smartphone Handlebar Mounts**

---

## 1. Executive Architectural Summary

**SpotHole** (internally designated **Khadda**) is an Android-based dual-modal sensing system engineered to detect, verify, geolocate, and log road surface cavities (potholes) and road anomalies while operating passively in the background on motorcycle handlebars or vehicle dashboards.

The architecture solves two classical failure modes in mobile road sensing:
1. **Inertial False Alarms:** Speed breakers, hard braking, sharp cornering, and engine resonance often produce vertical g-force spikes that trick single-point threshold detectors.
2. **Vision Thermal/Battery Throttling:** Running deep learning computer vision continuously on live camera frames depletes phone batteries in under an hour and induces severe hardware thermal throttling.

**The Solution:** SpotHole implements a **hierarchical two-stage funnel**:
* **Stage 1 (Continuous 24/7 Sensing):** Lightweight 67.7 Hz (100 Hz requested) IMU inertial sensing with gravity vector projection, 2-of-4 consensus voting, and kinematic rejection gates.
* **Stage 2 (On-Demand Vision Veto):** A lightweight YOLOv8n model running via TensorFlow Lite (`pothole_model.tflite`) that processes buffered CameraX frames **only when Stage 1 flags a candidate shock**.

```
[ Accelerometer & Gyroscope (100Hz req / 67.7Hz meas) ]
                       │
                       ▼
        [ Gravity Vector Projection (a_v) ]
                       │
                       ▼
          [ 2-of-4 Consensus Voting ] ──(Fails)──> [ Discarded (77.1% Noise) ]
                       │ (Passes)
                       ▼
        [ Kinematic Rejection Gates ] ──(Fails)──> [ Discarded (Braking/Turning) ]
                       │ (Passes)
                       ▼
         [ Stage 2: On-Demand Vision Veto ]
             (YOLOv8n TFLite @ 640x640)
                       │
         ┌─────────────┴─────────────┐
         ▼                           ▼
[ Confidence < 0.25 ]      [ Confidence >= 0.25 ]
 (Vetoed: Speed Breaker)     (Confirmed Pothole)
                                     │
                                     ▼
                     [ Room DB & OpenStreetMap Pin ]
```

---

## 2. Four-Tier Architecture Specification

The application follows the **Model-View-ViewModel (MVVM)** design pattern partitioned into four decoupled layers:

### A. Sensing Tier (`SensorCollector`, `LocationTracker`)
* **`DetectionForegroundService`:** Hosts the persistent sensing lifecycle with an ongoing Android status bar notification. Prevents the Android Low Memory Killer (LMK) or Doze mode from killing sensor acquisition when the screen is locked.
* **Hardware Callbacks (`SensorCollector.kt`):** Registers callbacks for:
  * `Sensor.TYPE_ACCELEROMETER` (Raw triaxial acceleration including gravity, used for `G-ZERO`).
  * `Sensor.TYPE_LINEAR_ACCELERATION` (Triaxial acceleration with gravity removed).
  * `Sensor.TYPE_GRAVITY` (Earth gravity direction vector).
  * `Sensor.TYPE_GYROSCOPE` (Angular rotational velocity in rad/s).
  * Target sampling rate: $10,000\ \mu\text{s}$ (100 Hz requested); empirical delivery rate: **67.7 Hz (14.77 ms/sample)** due to Android kernel HAL dispatch.
* **GPS Geolocation (`LocationTracker.kt`):** High-precision `FusedLocationProvider` stream capturing WGS84 Latitude, Longitude, Altitude, Bearing, and GPS Speed in m/s.
* **Camera Ring Buffer (`DetectionForegroundService.kt`):** Android `CameraX` captures low-overhead road imagery at **5 FPS** into a **25-frame FIFO circular buffer**, maintaining a rolling 5.0-second visual history immediately preceding any physical shock.

---

### B. Detection Tier (`DataProcessor`, `PotholeDetector`, `FalsePositiveFilter`, `CandidateVerifier`)

#### 1. Orientation Reorientation & Preprocessing (`DataProcessor.kt`)
Because the smartphone is mounted at an arbitrary handlebar angle ($30^\circ\text{--}60^\circ$ toward the rider), raw X, Y, and Z axes do not align with Earth coordinates.
* **Dynamic Gravity Projection:**
  $$\hat{\mathbf{g}} = \frac{\mathbf{g}}{\|\mathbf{g}\|}, \quad a_v = \mathbf{a}_{\text{linear}} \cdot \hat{\mathbf{g}}$$
  The linear acceleration vector is projected onto the normalized unit gravity vector $\hat{\mathbf{g}}$ via dot product, extracting pure **earth-referenced vertical acceleration ($a_v$)** without gimbal lock or manual calibration.
* **Noise Smoothing:** A 5-sample moving average window filters out high-frequency motorcycle engine combustion vibrations.
* **Feature Extraction:** Calculates $Z_{\text{diff}}$ over a 10-sample window ($147.7\text{ ms}$), $Z_{\text{thresh}}$, rolling $\sigma(Z)$ over 20 samples ($295\text{ ms}$), and total vector magnitude $M = \sqrt{a_x^2 + a_y^2 + a_z^2}$.

#### 2. Consensus Voting Engine (`PotholeDetector.kt`)
Evaluates 4 algorithms derived from Mednis et al. (2011) and requires **at least 2 of 4 algorithms to fire simultaneously**:
1. **`Z-DIFF`:** $|a_{\text{smooth}}[t] - a_{\text{smooth}}[t-9]| \ge 5.0\text{ m/s}^2$ (suspension compression impulse).
2. **`Z-THRESH`:** $|a_{\text{smooth}}[t]| \ge 3.92\text{ m/s}^2$ ($0.40g$ deviation from baseline).
3. **`STDEV(Z)`:** $\sigma(z_{t-19 \dots t}) \ge 5.0\text{ m/s}^2$ (sustained corrugated road roughness).
4. **`G-ZERO`:** $M = \sqrt{a_x^2 + a_y^2 + a_z^2} \le 2.5\text{ m/s}^2$ for $\ge 3$ consecutive samples (momentary cavity free-fall drop).

#### 3. Kinematic Noise Rejection Gates (`FalsePositiveFilter.kt`)
Candidates that pass voting must survive three physical motion filters:
* **Hard Braking Gate:** Blocks triggers when horizontal deceleration $> 4.5\text{ m/s}^2$ while vertical impact $< 2.5\text{ m/s}^2$ (filters front-fork brake diving).
* **Sharp Turning Gate:** Blocks triggers when gyroscope $|\text{yaw}| > 0.3\text{ rad/s}$ ($17.2^\circ/\text{s}$ turn rate).
* **Roll/Pitch Gate:** Blocks triggers when total gyroscope magnitude $> 1.2\text{ rad/s}$ ($68.8^\circ/\text{s}$ bike lean or mount shake).
* **Deduplication & Cooldown:** Enforces a **1.5-second cooldown** and suppresses duplicate detections within a **15-meter GPS radius** for 3.0 seconds.

#### 4. Stage 2 Edge-AI Vision Verification (`PotholeVisionAnalyzer.kt`, `CandidateVerifier.kt`)
* Triggered asynchronously via Kotlin Coroutines upon an accepted inertial candidate.
* Fetches the relevant pre-impact frame from the 25-frame ring buffer.
* **Letterboxing Preprocessor (`LetterboxUtil.kt`):** Letterboxes image with aspect-ratio preservation to $640 \times 640$ with RGB(114, 114, 114) padding (prevents the 93.9% accuracy collapse caused by direct stretching).
* **TFLite Interpreter:** Executes single-class YOLOv8n (`pothole_model.tflite`, 3.01M parameters) producing 8,400 anchor predictions.
* **Veto Gate:** If maximum detected pothole confidence is $< 0.25$, the candidate is rejected as a visual false alarm (e.g., speed breaker heave or joint). If $\ge 0.25$, it is committed as a verified pothole.

---

### C. Persistence & Telemetry Tier (`AppDatabase`, `TelemetryLogger`)

#### 1. SQLite Database (`Room`)
* **`PotholeEvent.kt`:** Entity storing confirmed potholes (`id`, `timestamp`, `latitude`, `longitude`, `altitude`, `severity`, `impactMagnitude`, `algorithmMask`, `imageUri`, `repairStatus`).
* **`CandidateEvent.kt`:** Entity storing all Stage 1 shock candidates for debugging and audit.
* **`PotholeDao.kt` / `CandidateDao.kt`:** Room DAOs offering reactive `Flow` and `LiveData` streams.

#### 2. High-Frequency Telemetry Blackbox (`TelemetryLogger.kt`)
Asynchronously logs session data to disk using a `ConcurrentLinkedQueue` on `Dispatchers.IO` without dropping 67.7 Hz sensor frames:
* **`sensor_log_*.csv`:** 100Hz timestamps, vertical acceleration, raw triaxial IMU vectors, gravity unit vectors, orientation angles, and speed.
* **`candidate_log_*.csv`:** Comprehensive audit trail recording every triggered shock and its exact rejection reason (e.g., *"Filtered: Sharp turning"*, *"Filtered: Speed below 10km/h"*, *"Vetoed: Vision confidence < 0.25"*).
* **`gps_log_*.csv`:** Geolocation traces.

---

### D. Presentation Tier (`MapFragment`, `MonitorFragment`, `AuthorityFragment`)

* **Live Monitor (`MonitorFragment.kt`, `LiveWaveformView.kt`):** 
  * Real-time Canvas oscilloscope plotting vertical and triaxial acceleration at 60 FPS.
  * Live telemetry HUD (GPS speed, detected count, hardware button indicators).
  * Demo/Simulation toggle (`RoadSimulationManager.kt`) for bench testing without driving.
* **Anomaly Mapping (`MapFragment.kt`):**
  * Integrated **OpenStreetMap via `osmdroid`** (no proprietary Google Maps API keys or billing quotas).
  * Color-coded markers: 🔴 High ($\ge 6.0\text{ m/s}^2$), 🟡 Medium ($3.0\text{--}6.0\text{ m/s}^2$), 🟢 Low ($< 3.0\text{ m/s}^2$).
* **Municipal Authority Workflow (`AuthorityFragment.kt`):**
  * Municipal road maintenance dashboard tracking potholes through lifecycle:
    $$\text{REPORTED} \longrightarrow \text{UNDER\_REVIEW} \longrightarrow \text{REPAIRED}$$
* **Debug Visual Gallery (`DebugGalleryActivity.kt`):**
  * Visual inspection gallery displaying candidate camera frames with overlay bounding boxes and confidence scores.
* **Session Export (`ReportExporter.kt`):**
  * Packages database records, CSV telemetry logs, and camera frames into a `.zip` archive via Android Share Sheet (`Intent.ACTION_SEND`).

---

## 3. Real-World Field Performance Benchmarks

Deployed and tested across 19 motorcycle handlebar ride sessions on a `moto g64 5G` smartphone:
* **Total Candidate Shocks Evaluated:** 3,635 events.
* **Stage 1 Consensus Voting Rejections:** 2,804 minor road vibrations filtered out (77.1%).
* **Kinematic Gate Rejections:** 609 non-pothole maneuvers eliminated (526 sharp turns, 71 cooldown duplicates, 12 hard brakes).
* **Stage 2 Vision Invocations:** 222 candidates checked by YOLOv8n.
* **Stage 2 Vision Vetoes:** **208 false triggers rejected (93.7% veto efficiency)**, including speed breaker heaves.
* **Confirmed Potholes Committed:** 14 genuine physical cavities.
