"""
make_figures.py
Generates publication-quality IEEE-compliant figures for the SpotHole paper:
1. figures/fig_arch.png: Compact horizontal two-row system architecture diagram (3.4 in wide, <= 2.6 in high, 300 dpi, text >= 7pt).
2. figures/fig_robustness.png: Monochrome mAP@0.5 robustness plot under synthetic degradations (3.4 in wide, <= 2.2 in high, 300 dpi).
"""

import os
import matplotlib.pyplot as plt
import matplotlib.patches as patches
import numpy as np

os.makedirs("figures", exist_ok=True)
plt.rcParams['font.sans-serif'] = 'Arial'
plt.rcParams['font.family'] = 'sans-serif'

# ----------------------------------------------------------------------
# 1. GENERATE FIG 1: SYSTEM ARCHITECTURE
# ----------------------------------------------------------------------
def generate_fig_arch():
    fig_w = 3.4
    fig_h = 2.55
    fig = plt.figure(figsize=(fig_w, fig_h), dpi=300)
    ax = fig.add_axes([0.005, 0.005, 0.99, 0.99])
    ax.set_xlim(0, 100)
    ax.set_ylim(100, 0) # 0 is top, 100 is bottom
    ax.axis('off')

    def draw_box(x, y, w, h, text, bg='#ffffff', ec='#1e293b', lw=0.8, text_fs=7.0, bold=False):
        rect = patches.FancyBboxPatch((x, y), w, h,
                                     boxstyle='round,pad=0.15,rounding_size=1.5',
                                     fc=bg, ec=ec, lw=lw, zorder=3)
        ax.add_patch(rect)
        cx = x + w / 2.0
        cy = y + h / 2.0
        ax.text(cx, cy, text, ha='center', va='center', fontsize=text_fs,
                color='#0f172a', weight='bold' if bold else 'normal', zorder=4,
                linespacing=1.15)

    # ROW 1 (y: 3 to 27, h=24): INERTIAL SCREENING (Left -> Right)
    draw_box(1.5, 3, 22.5, 24, 'Motion Sensors\n(100 Hz req.,\n67.7 Hz meas.)', text_fs=7.0, bold=True)
    ax.annotate('', xy=(26, 15), xytext=(24, 15),
                arrowprops=dict(arrowstyle='-|>', color='#0f172a', lw=0.9, mutation_scale=6))

    draw_box(26, 3, 22.5, 24, 'Signal Proc. &\nFeatures\n(Vert. Proj., MA)', text_fs=7.0)
    ax.annotate('', xy=(50.5, 15), xytext=(48.5, 15),
                arrowprops=dict(arrowstyle='-|>', color='#0f172a', lw=0.9, mutation_scale=6))

    draw_box(50.5, 3, 22.5, 24, 'Consensus\nVoting\n(2 of 4 rules)', text_fs=7.0)
    ax.annotate('', xy=(75, 15), xytext=(73, 15),
                arrowprops=dict(arrowstyle='-|>', color='#0f172a', lw=0.9, mutation_scale=6))

    draw_box(75, 3, 23.5, 24, 'Kinematic\nRejection Filters\n(Brake, Yaw, CD)', text_fs=7.0)

    # TRIGGER: From Kinematic Filters (x=86.7) straight down to YOLOv8n Veto (x=86.7)
    ax.annotate('', xy=(86.7, 44), xytext=(86.7, 27),
                arrowprops=dict(arrowstyle='-|>', color='#0f172a', lw=1.0, mutation_scale=7))
    ax.text(85, 35.5, 'Trigger\n(Candidate)', ha='right', va='center', fontsize=6.8, style='italic', weight='bold')

    # TELEMETRY LOGGER (Center-Left: x: 26 to 56, y: 34 to 48, h=14)
    draw_box(26, 34, 30, 14, 'Telemetry Logger\n(Continuous CSV)', bg='#f8fafc', ec='#64748b', lw=0.75, text_fs=6.8)

    # Dotted lines to Telemetry Logger
    ax.plot([37.2, 37.2], [27, 34], color='#64748b', lw=0.8, ls=':', zorder=2)
    ax.plot([75, 56], [20, 37], color='#64748b', lw=0.8, ls=':', zorder=2)
    ax.plot([75, 56], [44, 42], color='#64748b', lw=0.8, ls=':', zorder=2)

    # ROW 2: VERIFICATION & PERSISTENCE (Right -> Left)
    # YOLOv8n Veto (Col 4)
    draw_box(75, 44, 23.5, 25, 'YOLOv8n Veto\n(Letterbox 640²,\nConf. ≥ 0.25)', bg='#e2e8f0', ec='#0f172a', lw=1.1, text_fs=7.0, bold=True)

    # CameraX Buffer (Col 4 bottom)
    draw_box(75, 76, 23.5, 21, 'CameraX Buffer\n(25 frames @\n5 FPS ring)', bg='#ffffff', ec='#475569', lw=0.75, text_fs=6.8)
    ax.annotate('', xy=(86.7, 69), xytext=(86.7, 76),
                arrowprops=dict(arrowstyle='-|>', color='#475569', lw=0.9, ls='--', mutation_scale=6))

    # Confirmed Arrow: YOLOv8n Veto -> Event Builder (points LEFT)
    ax.annotate('', xy=(56, 56.5), xytext=(75, 56.5),
                arrowprops=dict(arrowstyle='-|>', color='#0f172a', lw=1.0, mutation_scale=7))
    ax.text(65.5, 54.0, 'Confirmed', ha='center', va='bottom', fontsize=6.8, style='italic', weight='bold')

    # Event Builder (Col 2-3 mid: x: 28 to 56, y: 55 to 77, w=28, h=22)
    draw_box(28, 55, 28, 22, 'Event Builder\n(Anomaly Package)', text_fs=7.0)

    # GNSS / GPS below Event Builder (x: 28 to 56, y: 83 to 97, h=14)
    draw_box(28, 83, 28, 14, 'GNSS / GPS\nLocation Tag', bg='#ffffff', ec='#475569', lw=0.75, text_fs=6.8)
    ax.annotate('', xy=(42, 77), xytext=(42, 83),
                arrowprops=dict(arrowstyle='-|>', color='#475569', lw=0.8, ls='--', mutation_scale=6))

    # Arrow Event Builder -> Room DB (points LEFT)
    ax.annotate('', xy=(24, 66), xytext=(28, 66),
                arrowprops=dict(arrowstyle='-|>', color='#0f172a', lw=0.9, mutation_scale=6))

    # Room DB (Col 1: y: 55 to 73, h=18)
    draw_box(1.5, 55, 22.5, 18, 'Room SQLite DB\n(Local Storage)', text_fs=7.0)

    # Arrow Room DB -> Map & Export (points DOWN)
    ax.annotate('', xy=(12.7, 79), xytext=(12.7, 73),
                arrowprops=dict(arrowstyle='-|>', color='#0f172a', lw=0.9, mutation_scale=6))

    # Map View & Export (Col 1 bottom: y: 79 to 97, h=18)
    draw_box(1.5, 79, 22.5, 18, 'Map View &\nZIP/CSV Export', text_fs=7.0, bold=True)

    out_path = "figures/fig_arch.png"
    plt.savefig(out_path, dpi=300)
    plt.close()
    print(f"Generated {out_path} ({fig_w} in x {fig_h} in)")

# ----------------------------------------------------------------------
# 2. GENERATE FIG 2: ROBUSTNESS PLOT
# ----------------------------------------------------------------------
def generate_fig_robustness():
    fig_w = 3.4
    fig_h = 2.15
    fig = plt.figure(figsize=(fig_w, fig_h), dpi=300)
    ax = fig.add_axes([0.41, 0.16, 0.56, 0.80])

    degradations = [
        {"name": "Motion Blur (15 px)", "map50": 0.3576, "delta": -0.4569},
        {"name": "Brightness -40% (Dark)", "map50": 0.5546, "delta": -0.2599},
        {"name": "Brightness +40% (Overexp.)", "map50": 0.6179, "delta": -0.1966},
        {"name": "Motion Blur (9 px)", "map50": 0.6298, "delta": -0.1847},
        {"name": "Gaussian Blur (9×9)", "map50": 0.7023, "delta": -0.1122},
        {"name": "Gaussian Blur (5×5)", "map50": 0.7685, "delta": -0.0460},
        {"name": "Gaussian Noise (σ=10)", "map50": 0.7748, "delta": -0.0397},
        {"name": "JPEG Compress. (Q=30)", "map50": 0.7967, "delta": -0.0178},
        {"name": "Clean Road Baseline", "map50": 0.8145, "delta": 0.0000}
    ]

    names = [d["name"] for d in degradations]
    map_values = [d["map50"] for d in degradations]
    deltas = [d["delta"] for d in degradations]

    y_pos = np.arange(len(names))

    # Uniform monochrome bars
    bars = ax.barh(y_pos, map_values, height=0.62, color="#4c4c4c",
                   edgecolor="#1a1a1a", linewidth=0.7, alpha=0.92, zorder=2)

    # Baseline line at 0.8145
    baseline_val = 0.8145
    ax.axvline(x=baseline_val, color="#000000", linestyle="--", linewidth=1.0, alpha=0.9, zorder=1)

    ax.set_yticks(y_pos)
    ax.set_yticklabels(names, fontsize=6.3, color="#000000")
    ax.set_xlabel("Detection Accuracy (mAP@0.5)", fontsize=7.0, fontweight='bold', labelpad=3, color="#000000")
    ax.set_xlim(0, 1.36)
    ax.tick_params(axis='x', labelsize=6.3)
    ax.grid(axis='x', linestyle=':', alpha=0.60, color='#cccccc', zorder=0)
    ax.set_axisbelow(True)

    ax.spines['top'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.spines['left'].set_color('#333333')
    ax.spines['bottom'].set_color('#333333')

    for i, bar in enumerate(bars):
        val = map_values[i]
        delta = deltas[i]
        if delta == 0:
            label = f" {val:.4f} (Baseline)"
            fontweight = 'bold'
        else:
            label = f" {val:.4f} (Δ {delta:+.4f})"
            fontweight = 'normal'
        
        ax.text(val + 0.015, bar.get_y() + bar.get_height()/2, label,
                va='center', ha='left', fontsize=5.8, fontweight=fontweight, color='#000000',
                zorder=3,
                bbox=dict(boxstyle='square,pad=0.08', facecolor='#ffffff', edgecolor='none', alpha=0.95))

    out_path = "figures/fig_robustness.png"
    plt.savefig(out_path, dpi=300)
    plt.savefig("fig_robustness.png", dpi=300)
    plt.close()
    print(f"Generated {out_path} ({fig_w} in x {fig_h} in)")

if __name__ == "__main__":
    generate_fig_arch()
    generate_fig_robustness()
