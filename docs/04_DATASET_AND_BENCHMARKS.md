# Document 04: Dataset Composition & Benchmark Audit
**SpotHole (Khadda / खड्डा) — Empirical Machine Learning Evaluation**

---

## 1. Dataset Composition & Sources

The machine learning vision model was trained on an aggregated single-class pothole dataset sourced from two public computer vision repositories:
1. **Kaggle Potholes Detection YOLOv8 Dataset** (Bhambhania, 2023): 1,581 training and 396 validation images.
2. **Roboflow Pothole v1 Dataset** (Dwyer, 2020): 466 training, 133 validation, and 67 test images (`Pothole.v1/`).

### Dataset Split Distribution (`Dataset/data.yaml`)
* **Training Set (`Dataset/train/images`):** **2,046 images**, containing **7,198 annotated pothole bounding boxes** ($3.52\text{ boxes/image}$).
* **Validation Set (`Dataset/valid/images`):** **529 files** (528 usable validation images after discarding 1 corrupt GIF), containing **1,725 annotated potholes** ($3.27\text{ boxes/image}$).
* **Annotation Format:** Single-class YOLO bounding boxes:
  ```text
  0 <x_center> <y_center> <width> <height>
  ```
  *(All normalized between $0.0$ and $1.0$).*

---

## 2. Empirical Quality Audit & Structural Flaws

An exhaustive computational audit was executed via [`benchmark_runner.py`](file:///d:/Khadda/Khadda/benchmark_runner.py) and logged in [`results.json`](file:///d:/Khadda/Khadda/runs/detect/runs/detect/train_local_50epochs/benchmarks/results.json):

### A. Data Leakage & Duplicate Pairs
* **Exact MD5 Duplicates in Training:**
  * Out of 2,046 files in `Dataset/train/images`, there are only **1,791 unique MD5 hashes**.
  * **241 duplicate groups** contain **255 redundant, bit-for-bit identical image files** with different names (e.g., `pothole_1944.jpg` and `pothole_4.jpg` have a mean pixel difference of $0.0$).
* **Train-to-Validation Leakage:**
  * **113 validation images (21.3%)** in `Dataset/valid/images` are exact bit-for-bit duplicates of training images.
* **Perceptual Hash Near-Duplicates:**
  * A perceptual hash audit ($p\text{Hash}$ with Hamming distance $\le 4$) identified **242 near-duplicate pairs** between training and validation splits.
* **Root Cause:** Sourced video frames were extracted sequentially without sequence-aware grouped splitting. Consequently, standard validation accuracy represents an **optimistic upper bound**.

### B. Lack of Negative / Background Images
* **`zero_label_images: 0`** in both train and validation splits.
* Every single image in the dataset contains at least one pothole.
* Real-world roads are 99% clean asphalt, manhole covers, shadows, and road markings. The absence of empty background images means pure standalone vision models trigger false alarms on dark road patches.

### C. Resolution Inconsistency
* **Minimum Resolution:** $135 \times 129\text{ px}$.
* **Maximum Resolution:** $5760 \times 3840\text{ px}$.
* **Median Resolution:** $600 \times 400\text{ px}$.
* Small images ($135 \times 129$) lose edge texture when upscaled to the model's $640 \times 640$ input grid.

### D. Bounding Box Scale Distribution
* **Small Boxes ($< 32^2\text{ px}$):** $27.04\%$ (1,946 boxes)
* **Medium Boxes ($32^2\text{--}96^2\text{ px}$):** $39.69\%$ (2,857 boxes)
* **Large Boxes ($> 96^2\text{ px}$):** $33.27\%$ (2,395 boxes)

---

## 3. YOLOv8n Model Architecture & Training Specification

* **Architecture:** YOLOv8 Nano (`YOLOv8n`) single-class object detection.
* **Parameters:** $3,011,043$ (fused: $3,005,843$).
* **Computational Complexity:** $8.2\text{ GFLOPs}$ at $640 \times 640$ input resolution.
* **Training Hardware:** NVIDIA GeForce RTX 4050 Laptop GPU ($6\text{ GB}$ VRAM), AMD64, 16 GB RAM.
* **Software Stack:** PyTorch 2.5.1 + CUDA 12.1, Ultralytics 8.4.173.

### Hyperparameters (`args.yaml`)
| Parameter | Value | Description |
| :--- | :--- | :--- |
| **Epochs** | 50 | Fine-tuned from 5-epoch baseline (55 total) |
| **Batch Size** | 16 | |
| **Image Size** | 640 | Resized / letterboxed |
| **Optimizer** | Auto (resolved to SGD) | Momentum: 0.937, Weight Decay: 0.0005 |
| **Learning Rate** | $\text{lr}_0 = 0.01 \to \text{lrf} = 0.01$ | Linear decay |
| **Mosaic Augmentation** | 1.0 (prob) | Disabled for the final 10 epochs (`close_mosaic: 10`) |
| **Patience** | 100 | Early stopping not triggered |

---

## 4. Detection Accuracy Results

Evaluated on the 528 usable validation images at IoU 0.6:

| Metric | 5-Epoch Baseline | 50-Epoch Model | Gain |
| :--- | :--- | :--- | :--- |
| **Precision** | 0.7408 | **0.8247** | $+0.0839$ |
| **Recall** | 0.6250 | **0.6986** | $+0.0736$ |
| **F1-Score** | 0.6780 | **0.7564** | $+0.0784$ |
| **mAP@0.5** | 0.7072 | **0.7992** | $+0.0920$ |
| **mAP@0.5:0.95** | 0.4129 | **0.5162** | $+0.1033$ |

### Confusion Matrix at Deployed Confidence ($0.25$):
* **True Positives (TP):** 1,324
* **False Positives (FP):** 449
* **False Negatives (FN):** 400

---

## 5. Optical Degradation & Robustness Stress Tests

To simulate motorcycle riding conditions (vibrations, canopy shadows, lens defocusing), synthetic perturbations were applied to the validation images in [`benchmark_runner.py`](file:///d:/Khadda/Khadda/benchmark_runner.py):

| Perturbation Condition | Real-World Phenomenon | mAP@0.5 | $\Delta \text{mAP}$ | Relative Impact |
| :--- | :--- | :--- | :--- | :--- |
| **Clean Baseline** | Optimal daytime conditions | **0.8145** | — | Baseline |
| **JPEG Quality 30** | Cellular telemetry upload compression | 0.7967 | $-0.0178$ | Tolerable ($-2.2\%$) |
| **Gaussian Noise ($\sigma=10$)** | Sensor noise under tree shade | 0.7748 | $-0.0397$ | Mild ($-4.9\%$) |
| **$5 \times 5$ Gaussian Blur** | Mild lens defocusing / water droplets | 0.7685 | $-0.0460$ | Minor ($-5.6\%$) |
| **$9 \times 9$ Gaussian Blur** | Heavy defocusing | 0.7023 | $-0.1122$ | Moderate ($-13.8\%$) |
| **Motion Blur (Kernel 9)** | Handlebar vibration at 25–40 km/h | 0.6298 | $-0.1847$ | Significant ($-22.7\%$) |
| **Motion Blur (Kernel 15)** | High-speed cruising jitter (50+ km/h) | **0.3576** | **$-0.4569$** | **Critical collapse ($-56.1\%$)** |
| **Brightness $+40\%$** | Direct noon solar glare | 0.6179 | $-0.1966$ | Significant ($-24.1\%$) |
| **Brightness $-40\%$** | Evening twilight / overcast tunnels | 0.5546 | $-0.2599$ | Severe ($-31.9\%$) |
| **Direct Stretch (No letterbox)** | Ignoring aspect ratio | **0.0497** | **$-0.7648$** | **Complete collapse ($-93.9\%$)** |

---

## 6. Computational Latency & Inference Profiling

Timed across 200 consecutive inference passes on the training laptop:
* **GPU (RTX 4050 Laptop):**
  * Total Frame Time: **$10.35\text{ ms}$** ($\approx 96.6\text{ FPS}$)
  * Preprocessing: $0.31\text{ ms}$
  * Neural Network Inference: $3.66\text{ ms}$
  * Postprocessing (NMS): $0.96\text{ ms}$
* **CPU (AMD Ryzen 6000 series):**
  * Total Frame Time: **$58.47\text{ ms}$** ($\approx 17.1\text{ FPS}$)
* **On-Demand Advantage:** Because the app checks buffered frames only upon an accepted Stage 1 inertial trigger (evaluating at most 5 candidate frames), it prevents thermal throttling and preserves 67.7 Hz inertial sampling indefinitely.
