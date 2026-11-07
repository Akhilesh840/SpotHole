# Document 01: Project Overview & Getting Started
**SpotHole (Khadda / खड्डा) — Edge-AI & Multi-Modal Passive Pothole Detection System**

---

## 1. Executive Summary

**SpotHole** is an Android-based road anomaly sensing platform designed to operate passively on motorcycle handlebars or vehicle dashboards. By combining continuous high-frequency inertial sensor sampling (**Accelerometer**, **Gyroscope**, and **Gravity Vector Projection**) with on-demand **Edge-AI Computer Vision** (**YOLOv8n** running locally via **TensorFlow Lite**), SpotHole detects, verifies, and geolocates potholes without requiring expensive dedicated LIDAR or specialized vehicular hardware.

The platform functions as a crowdsensing participatory client: drivers or motorcyclists mount their smartphone on a vehicle handlebar. The application operates continuously in the background via a persistent foreground service, suppressing environmental road noise (turning, braking, engine vibrations), and presenting verified road hazards on an interactive OpenStreetMap interface and municipal maintenance dashboard.

---

## 2. Academic Context & Team

Developed as part of **PBL III: Software Engineering and Modelling (CSE20140)** — Semester V, Department of Computer Science & Engineering.

### Core Contributors
| Sr. No. | Student Name | Role / PRN |
| :--- | :--- | :--- |
| 1 | **Akhilesh Munishwar** | 1262242269 |
| 2 | **Ishaan Dhore** | 1262242310 |
| 3 | **Neeraj Padghan** | 1262242219 |
| 4 | **Samruddhi Patil** | 1262242202 |

* **Methodology:** Agile Scrum Framework.
* **Specification Baseline:** Software Requirements Specification (SRS) Version 1.0 approved.
* **Research Paper:** *Final Passive Pothole Detection System using Smartphone Accelerometer and Gyroscope*.

---

## 3. The Core Challenge & Two-Stage Innovation

### The Limitations of Prior Systems
1. **Pure Accelerometer Systems:** Suffer from high false-positive rates because speed bumps, bridge joints, hard braking, and sharp cornering generate vertical g-force spikes that fool single-point threshold detectors.
2. **Pure Camera Systems:** Running continuous deep learning computer vision on live camera feeds exhausts smartphone batteries in under 45 minutes and induces thermal throttling and camera frame-drops.

### SpotHole's Two-Stage Solution
SpotHole implements a **hierarchical two-stage filter**:
* **Stage 1 (Continuous Low-Power Sensing):** 67.7 Hz inertial sampling with Euler-free gravity projection, 2-of-4 consensus voting, and kinematic rejection gates eliminates 77.1% of routine road noise.
* **Stage 2 (On-Demand Vision Veto):** A lightweight YOLOv8n model ($640 \times 640$ RGB, 3.01M parameters) running locally via TensorFlow Lite processes buffered CameraX frames **only when Stage 1 flags a candidate shock**.

In real-world motorcycle testing across 19 ride sessions (3,635 candidate shocks):
* Stage 1 filtered out 2,804 minor vibrations (77.1%).
* Kinematic gates eliminated 609 vehicle maneuvers.
* Stage 2 Vision checked 222 shocks and **vetoed 208 false alarms (93.7% veto rate)**, confirming 14 genuine road potholes.

---

## 4. How to Clone & Run Locally

### Prerequisites
* **Git** (version 2.30+)
* **Android Studio** (Ladybug / Iguana or later) with Android SDK (API 26 to 34)
* **JDK 17** (or Android Studio bundled OpenJDK)
* **Python 3.10+** (Optional: only needed if running local ML training or benchmark scripts)

### Step 1: Clone the Repository
```bash
git clone https://github.com/Akhilesh840/SpotHole.git
cd SpotHole
```

### Step 2: Open and Build the Android Application
1. Launch Android Studio.
2. Click **File** $\to$ **Open...** and select the **`KhaddaApp`** directory.
3. Wait for Gradle sync to complete using the included Gradle 8.7 wrapper.
4. Build via terminal:
   ```bash
   cd KhaddaApp
   # Windows:
   .\gradlew.bat assembleDebug
   # Linux / macOS:
   ./gradlew assembleDebug
   ```

### Step 3: Run the Application
* **On Physical Device:** Enable **Developer Options** & **USB Debugging** on an Android phone, connect via USB, and click **Run ▶** (Shift + F10).
* **In-Lab Demo Mode (No Vehicle Required):** Open the app, toggle the **"Demo"** switch on the Monitor toolbar, and tap **START MONITORING**. The built-in `RoadSimulationManager` synthesizes realistic telemetry and pothole alerts on-screen.

### Step 4: Run Unit Tests
```bash
cd KhaddaApp
.\gradlew.bat test
```
Tests cover:
* `PotholeDetectorTest`: Algorithmic threshold triggers for Z-DIFF, Z-THRESH, STDEV, G-ZERO.
* `FalsePositiveFilterTest`: Braking, turning, and duplicate suppression.
* `LetterboxUtilTest`: Letterboxing coordinate transformations.
