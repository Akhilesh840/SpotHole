# SpotHole (Khadda / खड्डा) — Documentation Index

Welcome to the comprehensive technical documentation and research knowledge base for **SpotHole** (Edge-AI & Multi-Modal Passive Pothole Detection System).

This directory contains modular, detailed guides covering every layer of the system, mathematically derived and strictly grounded in the project codebase and research paper (*Final Passive Pothole Detection System using Smartphone Accelerometer and Gyroscope*).

---

## 📚 Documentation Table of Contents

| Document | Description | Target Audience |
| :--- | :--- | :--- |
| **[`01_PROJECT_OVERVIEW.md`](./01_PROJECT_OVERVIEW.md)** | Executive synopsis, problem statement, core capabilities, academic context, and quick-start instructions. | Evaluators, Researchers, New Contributors |
| **[`02_SYSTEM_ARCHITECTURE.md`](./02_SYSTEM_ARCHITECTURE.md)** | Detailed 4-tier MVVM architecture (Sensing, Detection, Persistence, Presentation) and component interaction. | System Architects, Engineers |
| **[`03_ALGORITHM_MATHEMATICS.md`](./03_ALGORITHM_MATHEMATICS.md)** | Mathematical formulations, physical derivations (gravity projection, heuristic shock thresholds, kinematic filters, Haversine formula). | Researchers, Algorithm Designers |
| **[`04_DATASET_AND_BENCHMARKS.md`](./04_DATASET_AND_BENCHMARKS.md)** | Dataset composition, audit findings (data leakage, duplicate analysis), and optical perturbation robustness stress tests. | Data Scientists, ML Engineers |
| **[`05_ANDROID_APP_DEVELOPMENT.md`](./05_ANDROID_APP_DEVELOPMENT.md)** | Complete Android Kotlin app guide (Foreground Service, CameraX ring buffer, TFLite YOLOv8n runtime, Room DB, osmdroid, Unit Testing). | Mobile Developers |

---

## 🔍 Quick System Summary

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

* **Target Device:** Android smartphones (API 26+) mounted on motorcycle handlebars.
* **Stage 1 (Inertial):** Orientation-independent vertical acceleration via gravity projection + 2-of-4 consensus voting (`Z-DIFF`, `Z-THRESH`, `STDEV(Z)`, `G-ZERO`).
* **Stage 2 (Vision Veto):** On-demand YOLOv8n ($640 \times 640$, 3.01M parameters) running locally via TensorFlow Lite on buffered camera frames.
* **Database & Mapping:** Local Room SQLite database + OpenStreetMap (`osmdroid`) color-coded markers (High/Medium/Low).
