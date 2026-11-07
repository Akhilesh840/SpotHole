# SpotHole (Khadda / खड्डा) — Comprehensive Project Documentation
**Edge-AI & Multi-Modal Passive Pothole Detection System**

---

## 1. Project Overview & Academic Context

**SpotHole** (internally codenamed **Khadda**) is an intelligent, low-cost road anomaly detection and mapping application engineered for standard Android smartphones mounted on motorcycle handlebars or vehicle dashboards. By synthesizing high-frequency inertial sensor sampling (**Accelerometer**, **Gyroscope**, and **Gravity Vector Projection**) with on-demand **Edge-AI Computer Vision** (**YOLOv8n** running locally via **TensorFlow Lite**), SpotHole passively identifies, verifies, and geolocates potholes without requiring dedicated vehicular sensors or expensive LIDAR infrastructure.

### Academic Context
* **Project:** PBL III: Software Engineering and Modelling (CSE20140) — Semester V, Department of Computer Science & Engineering.
* **Team Members:**
  1. **Akhilesh Munishwar** (PRN: 1262242269)
  2. **Ishaan Dhore** (PRN: 1262242310)
  3. **Neeraj Padghan** (PRN: 1262242219)
  4. **Samruddhi Patil** (PRN: 1262242202)
* **Methodology:** Agile Scrum Framework.
* **Specification Baseline:** Software Requirements Specification (SRS) Version 1.0 approved.

---

## 2. Product Requirements Document (PRD)

### 2.1 Problem Statement
Road surface cavities (potholes) and unmapped speed bumps cause severe vehicular damage, traffic congestion, and fatal road accidents. Municipal road authorities rely on slow, expensive, and manual visual road surveys. Traditional smartphone accelerometer apps produce excessive false alarms on speed breakers, sharp turns, and sudden braking, while pure camera systems drain phone batteries and overheat within minutes.

### 2.2 Core System Objectives
1. **Passive Background Operation:** Continuous sensor acquisition via an Android Foreground Service with zero user interaction required while driving.
2. **Handlebar-Orientation Independence:** Automatically resolve arbitrary phone mounting angles ($30^\circ\text{--}60^\circ$) by dynamically projecting linear acceleration onto the Earth's gravity vector.
3. **Robust False-Alarm Suppression:** A multi-tier filtering hierarchy that distinguishes true road cavities from hard braking, aggressive cornering, motorcycle vibration, and speed breakers.
4. **On-Demand Edge Vision Veto:** Execute on-device YOLOv8n object detection only when an inertial shock candidate occurs, saving battery and eliminating non-pothole shock triggers.
5. **Municipal Repair Lifecycle Tracking:** An interactive OpenStreetMap GIS interface and authority dashboard tracking road anomalies from `REPORTED` $\to$ `UNDER_REVIEW` $\to$ `REPAIRED`.
6. **Scientific Telemetry Logging:** Continuous, non-blocking 100Hz CSV logging for offline research and algorithmic benchmarking.

---

## 3. Technology Stack & Dependencies

* **Mobile Platform:** Android (Min SDK 26, Target SDK 34)
* **Programming Language:** Kotlin (with Jetpack Coroutines & StateFlow)
* **Software Architecture:** Model-View-ViewModel (MVVM)
* **Camera Pipeline:** Android Jetpack CameraX with in-memory FIFO 25-frame ring buffer (5 FPS)
* **Machine Learning Runtime:** TensorFlow Lite (`pothole_model.tflite` head head parser, $640 \times 640$ input, 8,400 anchors)
* **Local Persistence:** Room Persistence Library (SQLite abstraction layer)
* **Geographical Mapping:** `osmdroid` (OpenStreetMap Android SDK) — 100% free and open-source, zero proprietary API key requirements
* **Testing Suites:** 
  * Unit Tests: JUnit 4 & AndroidX Test (`PotholeDetectorTest`, `FalsePositiveFilterTest`, `CandidateTransitionTest`, `LetterboxUtilTest`)
  * End-to-End Mobile Testing: Appium Java test suite (`appium-tests/`)

---

## 4. Detection Pipeline & Algorithm Specifications

SpotHole processes road data through a **3-stage hierarchical pipeline**:

```
[ 100Hz IMU Callbacks (67.7Hz Meas) ]
                │
                ▼
  [ Gravity Vector Projection ]
                │
                ▼
 [ Stage 1: Consensus Voting ] ──(Fails)──> [ Discarded Noise (77.1%) ]
                │ (Passes)
                ▼
 [ Kinematic Rejection Gates ] ──(Fails)──> [ Discarded Braking/Turning ]
                │ (Passes)
                ▼
   [ Stage 2: Vision Veto ]
   (YOLOv8n TFLite @ 640x640)
                │
    ┌───────────┴───────────┐
    ▼                       ▼
[ Conf < 0.25 ]         [ Conf >= 0.25 ]
(Vetoed: Speed Breaker)  (Confirmed Pothole)
                            │
                            ▼
                [ Committed to Room DB ]
```

### Stage 1A: Orientation-Independent Acceleration (`DataProcessor.kt`)
* The phone's orientation on a handlebar mount is arbitrary.
* The system computes the normalized gravity unit vector $\hat{\mathbf{g}}$:
  $$\hat{\mathbf{g}} = \frac{\mathbf{g}}{\|\mathbf{g}\|}, \quad a_v = \mathbf{a}_{\text{linear}} \cdot \hat{\mathbf{g}}$$
* The vertical acceleration $a_v$ is smoothed using a 5-sample moving average to suppress motorcycle engine combustion frequencies:
  $$a_{\text{smooth}}[t] = \frac{1}{5} \sum_{i=0}^{4} a_v[t-i]$$

### Stage 1B: Consensus Voting (`PotholeDetector.kt`)
A candidate anomaly triggers **only when at least 2 of the 4 algorithms fire simultaneously**:

| Algorithm | Formula / Condition | Calibrated Threshold | Physical Phenomenon |
| :--- | :--- | :--- | :--- |
| **`Z-DIFF`** | $|a_{\text{smooth}}[t] - a_{\text{smooth}}[t-9]| \ge T_{\text{diff}}$ | $5.0\text{ m/s}^2$ | Rapid suspension compression over ~148 ms |
| **`Z-THRESH`** | $|a_{\text{smooth}}[t]| \ge T_{\text{thresh}}$ | $3.92\text{ m/s}^2$ ($0.40g$) | High-amplitude vertical deflection |
| **`STDEV(Z)`** | $\sigma(z_{t-19 \dots t}) \ge T_{\text{stdev}}$ | $5.0\text{ m/s}^2$ | Sustained surface roughness across ~295 ms |
| **`G-ZERO`** | $\sqrt{a_x^2 + a_y^2 + a_z^2} \le T_{\text{gzero}}$ ($\ge 3$ samples) | $2.5\text{ m/s}^2$ (~$0.25g$) | Temporary wheel cavity free-fall drop |

### Stage 1C: Kinematic Rejection Gates (`FalsePositiveFilter.kt`)
Even if consensus voting fires, candidates are vetoed if any vehicle maneuver gate is triggered:
1. **Hard Braking Gate:** Blocks front-fork brake diving when horizontal deceleration $> 4.5\text{ m/s}^2$ with vertical impact $< 2.5\text{ m/s}^2$.
2. **Sharp Turning Gate:** Blocks centripetal acceleration spikes when gyroscope $|\text{yaw}| > 0.3\text{ rad/s}$ ($17.2^\circ/\text{s}$).
3. **Roll/Pitch Tilt Gate:** Blocks body lean or mount vibration when total gyro magnitude $> 1.2\text{ rad/s}$ ($68.8^\circ/\text{s}$).
4. **Spatial & Temporal Cooldown:** A strict **1.5-second cooldown** and **15-meter GPS radius suppression** prevents duplicate entries for single large cavities.

### Stage 2: On-Demand Vision Verification (`PotholeVisionAnalyzer.kt`, `CandidateVerifier.kt`)
* Triggers asynchronously only upon an accepted Stage 1 candidate.
* Fetches the pre-impact road frame from the 25-frame CameraX ring buffer.
* Preprocesses frame using **Letterbox padding** to $640 \times 640$ with RGB(114, 114, 114).
* Evaluates using `pothole_model.tflite` (YOLOv8n).
* **Veto Decision:** If maximum detected bounding box confidence $< 0.25$, the candidate is vetoed as a non-pothole shock (e.g., speed breaker heave). If $\ge 0.25$, it is committed to the database.

---

## 5. Machine Learning & Vision Model Specifications

* **Model Architecture:** YOLOv8 Nano (`yolov8n.pt`, 3.01M parameters, 8.2 GFLOPs).
* **Training Dataset:** Aggregated from Kaggle Potholes Dataset and Roboflow `pothole-voxrl`:
  * **Training Split:** 2,046 images with 7,198 annotated pothole bounding boxes.
  * **Validation Split:** 529 images with 1,725 annotated potholes.
* **Accuracy Metrics:**
  * Precision: $0.825$
  * Recall: $0.699$
  * F1-Score: $0.756$
  * $mAP@0.5$: **$0.799$** ($0.8145$ under clean per-image protocol)
  * $mAP@0.5:0.95$: $0.516$

---

## 6. Real-World Field Telemetry & Speed-Breaker Discrimination

During prototype testing across **19 motorcycle ride sessions (3,635 candidate evaluations)** on a `moto g64 5G`:
* **Stage 1 Consensus Voting:** Filtered 2,804 minor vibrations (**77.1% noise rejection**).
* **Kinematic Rejection Gates:** Filtered 609 vehicle maneuvers (526 turns, 71 duplicates, 12 hard brakes).
* **Stage 2 Vision Verification:** 222 candidates checked; **208 false triggers vetoed (93.7% visual veto rate)**.
* **Confirmed Potholes:** 14 genuine physical potholes confirmed and mapped.

### Physical Waveform Divergence: Pothole vs. Speed Breaker
* **Pothole:** Produces an instantaneous downward drop: vertical acceleration plunges to **$-5.63\text{ m/s}^2$** with vector magnitude collapsing to $5.52\text{ m/s}^2$ (suspension decompression into cavity).
* **Speed Breaker:** Produces an upward heave surging to **$+9.86\text{ m/s}^2$** with magnitude peaking at **$19.90\text{ m/s}^2$**. Although crossing the vertical threshold ($Z_{\text{diff}} = 11.94\text{ m/s}^2$), the on-demand vision model identifies no surface cavity in the camera frame, successfully vetoing the event.

---

## 7. Known Research Limitations & Future Roadmap

Based on the empirical audits performed in [`benchmark_runner.py`](file:///d:/Khadda/Khadda/benchmark_runner.py) and documented in the project research paper:

1. **Dataset Split Leakage:**
   * A perceptual hash audit revealed **242 near-duplicate pairs** and **113 exact bit-for-bit duplicate images** between the training and validation sets (due to video frame extraction without sequence-based splitting). Reported validation metrics are an optimistic upper bound.
2. **Zero Negative Background Samples:**
   * The training dataset contains **0 pothole-free images** (`zero_label_images: 0`). Future datasets must incorporate 15%–20% clean asphalt, manhole covers, and tree shadows.
3. **Motion Blur Sensitivity:**
   * Synthetic perturbation testing proved that horizontal motion blur (kernel 15) causes a **56% relative accuracy drop** ($mAP@0.5$ falls from $0.8145$ to $0.3576$). Future training must include vibration and motion blur augmentations.
4. **Future Enhancements:**
   * Speed-adaptive dynamic correlation windows ($\Delta t = \frac{d}{v}$) between camera bounding boxes and wheel shock timing.
   * INT8 model quantization with Android NNAPI / NPU hardware delegation.
   * Multi-user crowdsourced spatial clustering (DBSCAN) on a central GIS server.

---

## 8. How to Build, Test & Run

### 8.1 Build the Application
```bash
cd KhaddaApp
# Windows
.\gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```

### 8.2 Run Unit Test Suites
```bash
.\gradlew.bat test
```
Tests cover:
* `PotholeDetectorTest`: Algorithmic threshold triggers for Z-DIFF, Z-THRESH, STDEV, G-ZERO.
* `FalsePositiveFilterTest`: Braking, turning, and duplicate suppression.
* `LetterboxUtilTest`: Letterboxing coordinate math.

### 8.3 In-Lab Demonstration Mode
If testing in a classroom or lab without a moving vehicle:
1. Open the app on an Android device or emulator.
2. In the **Monitor** screen, toggle the **"Demo"** switch in the top toolbar.
3. Tap **START MONITORING**.
4. The built-in `RoadSimulationManager` will inject real physical telemetry waveforms, demonstrating live oscilloscope updates and pothole alert triggers on demand.
