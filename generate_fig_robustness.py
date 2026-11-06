"""
Script to generate fig_robustness.png for the research paper:
'Passive Pothole Detection System using Smartphone Accelerometer and Gyroscope'

Fig. 2. mAP@0.5 of the 50-epoch model under synthetic image degradations (clean baseline 0.8145).
100% Pure Grayscale / Monochrome (Zero color coding, R=G=B) for IEEE print-friendly publication.
"""

import matplotlib.pyplot as plt
import numpy as np

plt.rcParams.update({
    'font.family': 'sans-serif',
    'font.sans-serif': ['Arial', 'DejaVu Sans', 'Helvetica'],
    'font.size': 10,
    'axes.labelsize': 10.5,
    'axes.titlesize': 11,
    'xtick.labelsize': 9.5,
    'ytick.labelsize': 9.5,
    'figure.autolayout': True,
    'mathtext.default': 'regular'
})

degradations = [
    {"name": "Motion Blur (15 px)", "map50": 0.3576, "delta": -0.4569},
    {"name": "Brightness -40% (Dark)", "map50": 0.5546, "delta": -0.2599},
    {"name": "Brightness +40% (Overexp.)", "map50": 0.6179, "delta": -0.1966},
    {"name": "Motion Blur (9 px)", "map50": 0.6298, "delta": -0.1847},
    {"name": "Gaussian Blur (9×9)", "map50": 0.7023, "delta": -0.1122},
    {"name": "Gaussian Blur (5×5)", "map50": 0.7685, "delta": -0.0460},
    {"name": "Gaussian Noise (σ = 10)", "map50": 0.7748, "delta": -0.0397},
    {"name": "JPEG Compression (Q = 30)", "map50": 0.7967, "delta": -0.0178},
    {"name": "Clean Road Baseline", "map50": 0.8145, "delta": 0.0000}
]

names = [d["name"] for d in degradations]
map_values = [d["map50"] for d in degradations]
deltas = [d["delta"] for d in degradations]

fig, ax = plt.subplots(figsize=(8.2, 4.4), dpi=300)

y_pos = np.arange(len(names))

# 100% Pure Grayscale: uniform dark neutral gray with pure black edge
bar_color = "#4c4c4c"
edge_color = "#1a1a1a"

bars = ax.barh(y_pos, map_values, height=0.62, color=bar_color,
                edgecolor=edge_color, linewidth=0.8, alpha=0.92, zorder=2)

# Baseline reference line at x = 0.8145 (pure black dashed line)
baseline_val = 0.8145
ax.axvline(x=baseline_val, color="#000000", linestyle="--", linewidth=1.2, alpha=0.9, zorder=1)

# Formatting axes
ax.set_yticks(y_pos)
ax.set_yticklabels(names, fontweight='normal', color='#000000')
ax.set_xlabel("Detection Accuracy (mAP@0.5)", fontweight='bold', labelpad=9, color='#000000')
ax.set_xlim(0, 1.05)
ax.grid(axis='x', linestyle=':', alpha=0.60, color='#cccccc', zorder=0)
ax.set_axisbelow(True)

# Spines styling
ax.spines['top'].set_visible(False)
ax.spines['right'].set_visible(False)
ax.spines['left'].set_color('#333333')
ax.spines['bottom'].set_color('#333333')

# Annotate each bar with value and delta
for i, bar in enumerate(bars):
    val = map_values[i]
    delta = deltas[i]
    if delta == 0:
        label = f" {val:.4f}  (Clean Reference Baseline)"
        fontweight = 'bold'
    else:
        label = f" {val:.4f}  (Δ {delta:+.4f})"
        fontweight = 'normal'
    
    ax.text(val + 0.012, bar.get_y() + bar.get_height()/2, label,
            va='center', ha='left', fontsize=9.2, fontweight=fontweight, color='#000000',
            zorder=3,
            bbox=dict(boxstyle='square,pad=0.15', facecolor='#ffffff', edgecolor='none', alpha=0.95))

output_path = "fig_robustness.png"
plt.savefig(output_path, dpi=300, bbox_inches='tight')
print(f"Generated 100% monochrome {output_path}")
