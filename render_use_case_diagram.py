import matplotlib.pyplot as plt
import matplotlib.patches as patches

# Set figure size and resolution
fig, ax = plt.subplots(figsize=(15, 8.5), dpi=300)
ax.set_xlim(0, 1500)
ax.set_ylim(0, 850)
ax.axis('off')

# Background color
fig.patch.set_facecolor('#ffffff')
ax.set_facecolor('#ffffff')

# 1. Title at Top
ax.text(750, 790, "Use Case diagram of spothole Detection System", 
        fontsize=24, fontweight='bold', ha='center', va='center', fontfamily='sans-serif', color='#111111')

# 2. Subsystem Box (Center)
sub_x, sub_y, sub_w, sub_h = 520, 50, 370, 680
box = patches.FancyBboxPatch((sub_x, sub_y), sub_w, sub_h,
                             boxstyle="round,pad=10,rounding_size=28",
                             facecolor='#dcfce7', edgecolor='#4d7c0f', linewidth=2.0)
ax.add_patch(box)

ax.text(sub_x + sub_w/2, 690, "<<Subsystem>>\nspothole System", 
        fontsize=15, ha='center', va='center', fontweight='bold', color='#14532d', fontfamily='sans-serif')

# Helper function to draw stick-figure actors (Exact UML Actor notation)
def draw_actor(x, y, label, label_pos='bottom', scale=1.0):
    head_radius = 16 * scale
    head_center = (x, y + 40 * scale)
    
    # Head
    head = patches.Circle(head_center, head_radius, facecolor='#f4f4f5', edgecolor='#27272a', linewidth=1.8, zorder=5)
    ax.add_patch(head)
    
    # Body
    neck_y = head_center[1] - head_radius
    crotch_y = y - 10 * scale
    ax.plot([x, x], [neck_y, crotch_y], color='#27272a', linewidth=1.8, zorder=5)
    
    # Arms
    arm_y = neck_y - 12 * scale
    ax.plot([x - 22 * scale, x + 22 * scale], [arm_y, arm_y], color='#27272a', linewidth=1.8, zorder=5)
    
    # Legs
    foot_y = y - 45 * scale
    ax.plot([x, x - 18 * scale], [crotch_y, foot_y], color='#27272a', linewidth=1.8, zorder=5)
    ax.plot([x, x + 18 * scale], [crotch_y, foot_y], color='#27272a', linewidth=1.8, zorder=5)
    
    # Label placement
    if label_pos == 'bottom':
        ax.text(x, foot_y - 15, label, fontsize=12, fontweight='bold', ha='center', va='top', 
                color='#18181b', fontfamily='sans-serif', zorder=5)
    elif label_pos == 'right':
        ax.text(x + 35 * scale, y + 5 * scale, label, fontsize=12, fontweight='bold', ha='left', va='center', 
                color='#18181b', fontfamily='sans-serif', zorder=5)
    return (x, y)

# Draw Left Actors (Labels below)
draw_actor(140, 390, "Commuter /\nDriver", label_pos='bottom', scale=1.1)
draw_actor(330, 540, "Motorcycle\nRider", label_pos='bottom', scale=1.1)
draw_actor(330, 240, "Road\nSurveyor", label_pos='bottom', scale=1.1)

# Draw Right Actors (Labels to the right, exactly matching reference image!)
draw_actor(1070, 600, "<<Service>>\nSensorManager", label_pos='right', scale=1.1)
draw_actor(1070, 440, "<<Service>>\nCameraX Engine", label_pos='right', scale=1.1)
draw_actor(1070, 280, "GPS Location\nProvider", label_pos='right', scale=1.1)
draw_actor(1070, 120, "Road Maintenance\nAuthority", label_pos='right', scale=1.1)

# Generalization Arrows on Left (Rider & Surveyor -> Driver)
arrow_prop = dict(arrowstyle="-|>", mutation_scale=18, facecolor='white', edgecolor='#27272a', lw=1.6)
ax.annotate("", xy=(170, 415), xytext=(290, 510), arrowprops=arrow_prop, zorder=4)
ax.annotate("", xy=(170, 395), xytext=(290, 270), arrowprops=arrow_prop, zorder=4)

# 3. Use Case Ovals inside Subsystem
ucs = [
    {"id": "uc1", "text": "Start Road\nMonitoring", "y": 570},
    {"id": "uc2", "text": "Detect Road\nAnomaly", "y": 430},
    {"id": "uc3", "text": "Verify with\nYOLOv8", "y": 280},
    {"id": "uc4", "text": "Record Pothole\nEvent", "y": 140},
]

for uc in ucs:
    oval = patches.FancyBboxPatch((sub_x + 55, uc["y"] - 32), 260, 64,
                                  boxstyle="round,pad=10,rounding_size=32",
                                  facecolor='#ffffff', edgecolor='#15803d', linewidth=2.0, zorder=3)
    ax.add_patch(oval)
    ax.text(sub_x + 185, uc["y"], uc["text"], fontsize=13, fontweight='bold',
            ha='center', va='center', color='#14532d', fontfamily='sans-serif', zorder=4)

# 4. <<include>> Dashed Arrows inside Subsystem
# uc2 -> uc1
ax.annotate("", xy=(sub_x + 185, 535), xytext=(sub_x + 185, 465),
            arrowprops=dict(arrowstyle="->", linestyle="dashed", color='#15803d', lw=1.6), zorder=4)
ax.text(sub_x + 235, 500, "<<include>>", fontsize=11, color='#166534', ha='left', va='center', fontweight='bold')

# uc2 -> uc3
ax.annotate("", xy=(sub_x + 185, 315), xytext=(sub_x + 185, 395),
            arrowprops=dict(arrowstyle="->", linestyle="dashed", color='#15803d', lw=1.6), zorder=4)
ax.text(sub_x + 235, 355, "<<include>>", fontsize=11, color='#166534', ha='left', va='center', fontweight='bold')

# 5. Association Lines (Solid lines connecting actors to use cases)
line_kw = dict(color='#374151', linewidth=1.5, zorder=2)

# Left Actor lines
ax.plot([360, sub_x + 55], [540, 570], **line_kw) # Rider -> uc1
ax.plot([360, sub_x + 55], [540, 430], **line_kw) # Rider -> uc2
ax.plot([360, sub_x + 55], [540, 280], **line_kw) # Rider -> uc3
ax.plot([360, sub_x + 55], [240, 570], **line_kw) # Surveyor -> uc1
ax.plot([360, sub_x + 55], [240, 140], **line_kw) # Surveyor -> uc4
ax.plot([170, sub_x + 55], [400, 570], **line_kw) # Driver -> uc1

# Right Actor lines (connect directly to right actors)
ax.plot([sub_x + 315, 1030], [570, 600], **line_kw) # uc1 -> SensorManager
ax.plot([sub_x + 315, 1030], [430, 600], **line_kw) # uc2 -> SensorManager
ax.plot([sub_x + 315, 1030], [430, 440], **line_kw) # uc2 -> CameraX
ax.plot([sub_x + 315, 1030], [280, 440], **line_kw) # uc3 -> CameraX
ax.plot([sub_x + 315, 1030], [280, 280], **line_kw) # uc3 -> GPS
ax.plot([sub_x + 315, 1030], [140, 280], **line_kw) # uc4 -> GPS
ax.plot([sub_x + 315, 1030], [140, 120], **line_kw) # uc4 -> Authority

plt.tight_layout()
plt.savefig('spothole_use_case_diagram.png', dpi=300, bbox_inches='tight', facecolor='#ffffff')
print("Rendered polished spothole_use_case_diagram.png successfully!")
