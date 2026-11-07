# SpotHole (Khadda / खड्डा) — Android Application
### Real-Time Edge-AI & Multi-Modal Passive Pothole Detection Client

An Android smartphone application for automated road-surface anomaly detection using embedded accelerometer, gyroscope, gravity vector projection, CameraX on-demand Edge-AI (YOLOv8n via TensorFlow Lite), GPS geolocation, and participatory sensing algorithms.

Developed for **PBL III: Software Engineering and Modelling (CSE20140)** — Semester V, Department of Computer Science & Engineering.

---

## 👥 Authors & Academic Context
| Sr. No. | Student Name | PRN / Student ID |
| :--- | :--- | :--- |
| 1 | **Akhilesh Munishwar** | 1262242269 |
| 2 | **Ishaan Dhore** | 1262242310 |
| 3 | **Neeraj Padghan** | 1262242219 |
| 4 | **Samruddhi Patil** | 1262242202 |

- **Specification Reference**: Software Requirements Specification (SRS) Version 1.0 approved
- **Process Model**: Agile Scrum Methodology
- **Design Specification**: Structured Systems Analysis and Design (SSAD) with Level-0 & Level-1 DFDs, UML Class, Activity, and Sequence Diagrams.

---

## 🔬 Theoretical Foundation & Architecture
This application implements a **two-stage detection funnel**:
1. **Stage 1 (Continuous 67.7 Hz Inertial Sensing):**
   - Projects 3-axis linear acceleration onto the normalized gravity vector: $a_v = \mathbf{a}_{\text{linear}} \cdot \hat{\mathbf{g}}$ to eliminate arbitrary phone mounting tilt ($30^\circ\text{--}60^\circ$).
   - 5-sample moving average smoothing.
   - **2-of-4 Consensus Voting:** Requires at least two algorithms to fire simultaneously:
     - **Z-DIFF:** $|a_{\text{smooth}}[t] - a_{\text{smooth}}[t-9]| \ge 5.0\text{ m/s}^2$ (~148 ms impulse).
     - **Z-THRESH:** $|a_{\text{smooth}}[t]| \ge 3.92\text{ m/s}^2$ ($0.40g$).
     - **STDEV(Z):** $\sigma(z_{t-19 \dots t}) \ge 5.0\text{ m/s}^2$ (~295 ms window).
     - **G-ZERO:** Total magnitude $M \le 2.5\text{ m/s}^2$ for $\ge 3$ samples (cavity free-fall drop).
   - **Kinematic Rejection Gates:** Eliminates braking ($> 4.5\text{ m/s}^2$), turning ($|\text{yaw}| > 0.3\text{ rad/s}$), wobble ($> 1.2\text{ rad/s}$), and enforces a 1.5s cooldown / 15m radius deduplication.
2. **Stage 2 (On-Demand Edge Vision Veto):**
   - CameraX captures preview frames at 5 FPS into an in-memory **25-frame ring buffer** (5.0s pre-impact history).
   - On an accepted Stage 1 shock candidate, the frame is preprocessed with **Letterbox aspect-ratio preservation** to $640 \times 640$ with RGB(114, 114, 114) padding.
   - Evaluated by **`pothole_model.tflite`** (YOLOv8n, 3.01M parameters).
   - If confidence is $< 0.25$, the shock is vetoed (e.g., speed breaker heave); if $\ge 0.25$, it is committed as a confirmed pothole.

---

## 📱 Application Modules & Features

### 1. Live Monitor Screen (`MonitorFragment`)
- Prominent Start/Stop control designed for zero distraction while driving.
- Custom Canvas-based `LiveWaveformView` plotting triaxial acceleration and vertical shock at 60 FPS.
- Live vehicle speed (km/h) and GPS coordinates.
- **Simulation / Demo Mode Toggle:** Allows instant lab demonstration without needing a vehicle.

### 2. Road-Anomaly Map (`MapFragment`)
- **OpenStreetMap (`osmdroid`):** Free and open-source, zero Google API keys required.
- **Severity-Coded Markers:** 🔴 High ($\ge 6.0\text{ m/s}^2$), 🟡 Medium ($3.0\text{--}6.0\text{ m/s}^2$), 🟢 Low ($< 3.0\text{ m/s}^2$).
- Tap any marker to inspect timestamp, coordinates, and current repair status.

### 3. Road Maintenance Authority Dashboard (`AuthorityFragment`)
- Tracks potholes through municipal lifecycle:
  $$\text{REPORTED} \longrightarrow \text{UNDER\_REVIEW} \longrightarrow \text{REPAIRED}$$

### 4. Detection History & Analytics (`HistoryFragment`, `ReportFragment`)
- RecyclerView list displaying all recorded events.
- **CSV & JSON Export:** Packages database records and raw CSV telemetry logs into a compressed `.zip` archive via the Android Share Sheet.

### 5. Debug Visual Gallery (`DebugGalleryActivity`)
- Visual verification gallery displaying candidate camera frames with bounding box overlays and confidence scores.

### 6. Background Sensing (`DetectionForegroundService`)
- Android Foreground Service with persistent status bar notification, ensuring continuous acquisition when the screen is turned off or locked.

---

## 🛠️ How to Build and Run in Android Studio

1. Launch **Android Studio**.
2. Click **File $\to$ Open...** and select the `KhaddaApp` folder.
3. Android Studio will automatically sync the Gradle configuration using the included Gradle 8.7 wrapper.
4. Connect an Android smartphone via USB with **Developer Options & USB Debugging** enabled.
5. Click **Run ▶** (or press `Shift + F10`).
6. To demonstrate in a lab: Toggle **"Demo"** mode at the top right of the Monitor screen and tap **START MONITORING**!

---

## 🧪 Running Unit Tests
```bash
./gradlew test
```
- `PotholeDetectorTest`: Tests Z-DIFF, Z-THRESH, STDEV(Z), and G-ZERO algorithms.
- `FalsePositiveFilterTest`: Tests braking rejection, cornering rejection, and duplicate suppression.
- `LetterboxUtilTest`: Tests letterbox aspect ratio scaling and padding.
- `CandidateTransitionTest`: Tests candidate state machine lifecycle.
