# Document 03: Algorithm Mathematics & Physics Derivations
**SpotHole (Khadda / खड्डा) — Comprehensive Mathematical Reference**

---

## 1. Dynamic Gravity Projection (Orientation Invariance)

### Physical Problem
When a smartphone is clamped into a motorcycle handlebar mount, its physical axes are tilted at an arbitrary angle ($30^\circ\text{--}60^\circ$ toward the rider).
* The phone's native Z-axis points toward the rider's chest, not the center of the Earth.
* Forward acceleration or braking would erroneously register as a vertical shock on a tilted phone.

### Mathematical Formulation
The device's Gravity sensor reports the continuous direction of Earth's gravity vector:
$$\mathbf{g} = \begin{bmatrix} g_x \\ g_y \\ g_z \end{bmatrix}$$

1. **Calculate the Unit Gravity Vector ($\hat{\mathbf{g}}$):**
   $$\|\mathbf{g}\| = \sqrt{g_x^2 + g_y^2 + g_z^2}$$
   $$\hat{\mathbf{g}} = \frac{\mathbf{g}}{\|\mathbf{g}\|} = \begin{bmatrix} g_x / \|\mathbf{g}\| \\ g_y / \|\mathbf{g}\| \\ g_z / \|\mathbf{g}\| \end{bmatrix}$$

2. **Isolate True Vertical Acceleration ($a_v$):**
   The Linear Acceleration sensor provides vehicular motion without gravitational bias:
   $$\mathbf{a}_{\text{linear}} = \begin{bmatrix} a_{\text{lin},x} \\ a_{\text{lin},y} \\ a_{\text{lin},z} \end{bmatrix}$$

   By calculating the scalar **dot product** of $\mathbf{a}_{\text{linear}}$ with the unit gravity vector $\hat{\mathbf{g}}$, the app projects motion directly onto the Earth's vertical axis:
   $$a_v = \mathbf{a}_{\text{linear}} \cdot \hat{\mathbf{g}} = (a_{\text{lin},x} \cdot \hat{g}_x) + (a_{\text{lin},y} \cdot \hat{g}_y) + (a_{\text{lin},z} \cdot \hat{g}_z)$$

* **Implementation:** [`DataProcessor.kt`](file:///d:/Khadda/Khadda/KhaddaApp/app/src/main/java/com/pothole/khadda/detector/DataProcessor.kt#L71-L77)

---

## 2. Noise Reduction: Moving Average Filter

To suppress high-frequency motorcycle engine combustion cycles and frame vibration noise, $a_v$ is smoothed using a **5-sample finite impulse response (FIR) moving average filter**:

$$a_{\text{smooth}}[t] = \frac{1}{5} \sum_{i=0}^{4} a_v[t-i]$$

* At $67.7\text{ Hz}$ ($14.77\text{ ms/sample}$), a 5-sample window spans **$73.85\text{ ms}$**, eliminating high-frequency engine buzz while preserving the sharp, low-frequency shock deflection caused by road craters.
* **Implementation:** [`DataProcessor.kt`](file:///d:/Khadda/Khadda/KhaddaApp/app/src/main/java/com/pothole/khadda/detector/DataProcessor.kt#L79-L82)

---

## 3. Four Core Detection Algorithms (Mednis et al., 2011)

### 3.1 Z-DIFF (Sudden Vertical Jerk)
* **Physical Phenomenon:** Instantaneous compression of the motorcycle front suspension forks as the wheel hits the cavity lip.
* **Mathematical Formula:**
  $$Z_{\text{diff}}[t] = |a_{\text{smooth}}[t] - a_{\text{smooth}}[t-9]|$$
* **Calibrated Threshold:**
  $$Z_{\text{diff}} \ge 5.0\text{ m/s}^2 \quad (\approx 0.51g)$$
* **Temporal Span:** Compares the current sample against the 10th sample prior:
  $$\Delta t = 10 \text{ samples} \times 14.77\text{ ms} = 147.7\text{ ms} \approx 133\text{--}148\text{ ms}$$
  This window acts as a differential edge-detector that accentuates abrupt step-downs while attenuating gradual road gradients and bridge approaches.
* **Implementation:** [`DataProcessor.kt`](file:///d:/Khadda/Khadda/KhaddaApp/app/src/main/java/com/pothole/khadda/detector/DataProcessor.kt#L87-L89) & [`PotholeDetector.kt`](file:///d:/Khadda/Khadda/KhaddaApp/app/src/main/java/com/pothole/khadda/detector/PotholeDetector.kt#L36-L38)

---

### 3.2 Z-THRESH (Vertical Peak Deviation)
* **Physical Phenomenon:** Absolute amplitude excursion of vertical acceleration exceeding normal gravity.
* **Mathematical Formula:**
  $$Z_{\text{thresh}}[t] = |a_{\text{smooth}}[t]|$$
  *(Note: Because $a_{\text{smooth}}$ is derived from linear acceleration where gravity is already centered at 0, deviation from baseline is simply its absolute magnitude).*
* **Calibrated Threshold:**
  $$Z_{\text{thresh}} \ge 3.92\text{ m/s}^2 \quad (0.40g)$$
* **Implementation:** [`DataProcessor.kt`](file:///d:/Khadda/Khadda/KhaddaApp/app/src/main/java/com/pothole/khadda/detector/DataProcessor.kt#L94) & [`PotholeDetector.kt`](file:///d:/Khadda/Khadda/KhaddaApp/app/src/main/java/com/pothole/khadda/detector/PotholeDetector.kt#L43-L45)

---

### 3.3 STDEV(Z) (Rolling Window Standard Deviation)
* **Physical Phenomenon:** Elevated vibration energy across rough, broken, or corrugated road surfaces.
* **Mathematical Formula:** Over a sliding window of $N = 20$ samples:
  $$\mu = \frac{1}{N} \sum_{i=0}^{N-1} a_v[t-i]$$
  $$\sigma(Z) = \sqrt{\frac{1}{N} \sum_{i=0}^{N-1} (a_v[t-i] - \mu)^2}$$
* **Calibrated Threshold:**
  $$\sigma(Z) \ge 5.0\text{ m/s}^2$$
* **Temporal Span:**
  $$\Delta t = 20 \text{ samples} \times 14.77\text{ ms} \approx 295.4\text{ ms} \approx 0.3\text{ seconds}$$
* **Implementation:** [`DataProcessor.kt`](file:///d:/Khadda/Khadda/KhaddaApp/app/src/main/java/com/pothole/khadda/detector/DataProcessor.kt#L55-L68) & [`PotholeDetector.kt`](file:///d:/Khadda/Khadda/KhaddaApp/app/src/main/java/com/pothole/khadda/detector/PotholeDetector.kt#L50-L52)

---

### 3.4 G-ZERO (Cavity Free-Fall Drop)
* **Physical Phenomenon:** Momentary micro-gravity (weightlessness) as the vehicle wheel drops into a deep cavity before striking the bottom.
* **Mathematical Formula:** Total acceleration vector magnitude $M$ **including gravity**:
  $$M = \sqrt{a_x^2 + a_y^2 + a_z^2}$$
* **Calibrated Condition:**
  $$M \le 2.5\text{ m/s}^2 \quad (\approx 0.25g) \quad \text{for } \ge 3 \text{ consecutive samples } (\approx 45\text{ ms})$$
* **Why Total Acceleration is Required:** At rest, an accelerometer measures Earth's upward normal force ($1.0g \approx 9.8\text{ m/s}^2$). When falling into a hole, the normal force vanishes, causing $M$ to plunge toward $0\text{ m/s}^2$. If linear acceleration were used, it would already be 0 all the time on smooth roads.
* **Implementation:** [`SensorData.kt`](file:///d:/Khadda/Khadda/KhaddaApp/app/src/main/java/com/pothole/khadda/model/SensorData.kt#L29-L31) & [`PotholeDetector.kt`](file:///d:/Khadda/Khadda/KhaddaApp/app/src/main/java/com/pothole/khadda/detector/PotholeDetector.kt#L60-L68)

---

## 4. Consensus Voting Rule (Majority Rule)

To prevent individual algorithm flukes, an anomaly triggers **only when at least two of the four algorithms fire simultaneously**:

$$\text{Vote Count} = \mathbb{I}(Z_{\text{diff}}) + \mathbb{I}(Z_{\text{thresh}}) + \mathbb{I}(\text{STDEV}) + \mathbb{I}(G_{\text{zero}})$$
$$\text{Candidate Shock} \iff \text{Vote Count} \ge 2$$

* **Implementation:** [`DetectionForegroundService.kt`](file:///d:/Khadda/Khadda/KhaddaApp/app/src/main/java/com/pothole/khadda/service/DetectionForegroundService.kt#L235)

---

## 5. Kinematic Rejection Gate Mathematics

### 5.1 Hard Braking Rejection
$$a_{\text{horizontal}} = \sqrt{a_x^2 + a_y^2}$$
$$\text{Reject if } \left( a_{\text{horizontal}} > 4.5\text{ m/s}^2 \right) \;\land\; \left( Z_{\text{diff}} < 2.5\text{ m/s}^2 \right)$$
* Eliminates fork-dive compression caused by front brake application when no vertical cavity impact exists.

### 5.2 Sharp Cornering Rejection
$$\text{Reject if } |\omega_{\text{yaw}}| > 0.3\text{ rad/s} \quad (\approx 17.2^\circ/\text{s})$$
* Eliminates lateral centripetal acceleration spikes caused by sharp turns or swerving.

### 5.3 Excessive Roll/Pitch Tilt Rejection
$$\|\boldsymbol{\omega}\| = \sqrt{\omega_x^2 + \omega_y^2 + \omega_z^2}$$
$$\text{Reject if } \left( \|\boldsymbol{\omega}\| > 1.2\text{ rad/s} \right) \;\land\; \left( Z_{\text{diff}} < 2.5\text{ m/s}^2 \right) \quad (\approx 68.8^\circ/\text{s})$$
* Eliminates phone mount wobbling or deep motorcycle banking.

* **Implementation:** [`FalsePositiveFilter.kt`](file:///d:/Khadda/Khadda/KhaddaApp/app/src/main/java/com/pothole/khadda/detector/FalsePositiveFilter.kt#L40-L53)

---

## 6. Spatial-Temporal Deduplication: Haversine Formula

To compute the great-circle surface distance $D$ between two successive GPS coordinates $(\phi_1, \lambda_1)$ and $(\phi_2, \lambda_2)$:

$$\Delta \phi = \phi_2 - \phi_1, \quad \Delta \lambda = \lambda_2 - \lambda_1$$
$$a = \sin^2\left(\frac{\Delta \phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta \lambda}{2}\right)$$
$$c = 2 \cdot \text{atan2}\left(\sqrt{a}, \sqrt{1-a}\right)$$
$$D = R \cdot c \quad (\text{where Earth's radius } R = 6,371,000\text{ meters})$$

* **Suppression Rule:** If elapsed time $\Delta t < 3000\text{ ms}$ and distance $D \le 15.0\text{ meters}$, or if $\Delta t < 1500\text{ ms}$ (strict cooldown), the second event is suppressed as a duplicate.
* **Implementation:** [`FalsePositiveFilter.kt`](file:///d:/Khadda/Khadda/KhaddaApp/app/src/main/java/com/pothole/khadda/detector/FalsePositiveFilter.kt#L89-L105)

---

## 7. Dynamic Speed-to-Distance Camera Correlation

When the front-facing camera detects a pothole bounding box ahead of the motorcycle, the front wheel does not hit the hole immediately. The impact occurs $t$ seconds later based on vehicular velocity $v$:

$$\Delta t = \frac{d}{v}$$
* Where:
  * $d$ = Estimated distance to the visual bounding box ($3\text{--}8\text{ meters}$).
  * $v$ = GPS vehicle velocity ($m/s$).
  * At $v = 36\text{ km/h} = 10\text{ m/s}$ and $d = 5\text{ m}$, the physical shock strikes $\Delta t = 0.50\text{ seconds}$ after the camera frame.
* **Implementation:** State machine matching in [`CandidateVerifier.kt`](file:///d:/Khadda/Khadda/KhaddaApp/app/src/main/java/com/pothole/khadda/detector/CandidateVerifier.kt).

---

## 8. Letterboxing Coordinate Transformation

In [`LetterboxUtil.kt`](file:///d:/Khadda/Khadda/KhaddaApp/app/src/main/java/com/pothole/khadda/detector/LetterboxUtil.kt), images are resized to $640 \times 640$ while preserving aspect ratio with constant RGB(114, 114, 114) padding.

Given an input image of dimensions $W \times H$:
1. **Scale Factor:**
   $$s = \min\left(\frac{640}{W}, \frac{640}{H}\right)$$
2. **Rescaled Content Size:**
   $$W_{\text{new}} = W \cdot s, \quad H_{\text{new}} = H \cdot s$$
3. **Padded Offsets:**
   $$\text{pad}_x = \frac{640 - W_{\text{new}}}{2}, \quad \text{pad}_y = \frac{640 - H_{\text{new}}}{2}$$
4. **Bounding Box Coordinate Inversion:**
   To map a YOLO prediction $(x_{\text{model}}, y_{\text{model}})$ back to original image coordinates $(x_{\text{orig}}, y_{\text{orig}})$:
   $$x_{\text{orig}} = \frac{x_{\text{model}} - \text{pad}_x}{s}, \quad y_{\text{orig}} = \frac{y_{\text{model}} - \text{pad}_y}{s}$$
