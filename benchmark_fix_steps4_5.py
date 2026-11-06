"""
benchmark_fix_steps4_5.py
Re-runs Steps 4 and 5 using a corrected approach:
Instead of a custom data.yaml pointing to a temp dir,
we use model.predict() per-image and compute metrics manually
so that the labels are correctly matched regardless of YAML cache.
"""
import os, json, csv, time, shutil, random
import numpy as np
import cv2
from pathlib import Path
from ultralytics import YOLO
import torch

SEED = 0
torch.manual_seed(SEED); np.random.seed(SEED); random.seed(SEED)

MODEL_50E  = r"D:\Khadda\Khadda\runs\detect\runs\detect\train_local_50epochs\weights\best.pt"
BENCH_DIR  = r"D:\Khadda\Khadda\runs\detect\runs\detect\train_local_50epochs\benchmarks"
TRAIN_IMG  = r"D:\Khadda\Khadda\Dataset\train\images"
VAL_IMG    = r"D:\Khadda\Khadda\Dataset\valid\images"
VAL_LBL    = r"D:\Khadda\Khadda\Dataset\valid\labels"
DATA_YAML  = r"D:\Khadda\Khadda\Dataset\data.yaml"

def get_image_list(folder):
    exts = {".jpg", ".jpeg", ".png", ".bmp"}
    return sorted([p for p in Path(folder).iterdir() if p.suffix.lower() in exts])

def parse_labels(label_folder):
    boxes = {}
    for lf in Path(label_folder).glob("*.txt"):
        b = []
        with open(lf) as f:
            for line in f:
                parts = line.strip().split()
                if len(parts) == 5:
                    b.append(tuple(float(x) for x in parts[1:]))
        boxes[lf.stem] = b
    return boxes

def compute_ap50(pred_list, gt_list):
    """
    pred_list: list of (x1,y1,x2,y2,conf) in pixel coords
    gt_list:   list of (x1,y1,x2,y2) in pixel coords
    Returns AP50 for one image.
    """
    if not gt_list:
        return None  # skip images with no GT
    pred_list = sorted(pred_list, key=lambda x: -x[4])
    matched = [False] * len(gt_list)
    tp_arr, fp_arr = [], []
    for pb in pred_list:
        best_iou, best_j = 0, -1
        for j, gb in enumerate(gt_list):
            ix1 = max(pb[0], gb[0]); iy1 = max(pb[1], gb[1])
            ix2 = min(pb[2], gb[2]); iy2 = min(pb[3], gb[3])
            iw = max(0, ix2-ix1); ih = max(0, iy2-iy1)
            inter = iw * ih
            union = (pb[2]-pb[0])*(pb[3]-pb[1]) + (gb[2]-gb[0])*(gb[3]-gb[1]) - inter
            iou = inter / (union + 1e-9)
            if iou > best_iou: best_iou = iou; best_j = j
        if best_iou >= 0.5 and best_j >= 0 and not matched[best_j]:
            tp_arr.append(1); fp_arr.append(0); matched[best_j] = True
        else:
            tp_arr.append(0); fp_arr.append(1)
    tp_c = np.cumsum(tp_arr); fp_c = np.cumsum(fp_arr)
    n_gt = len(gt_list)
    prec = tp_c / (tp_c + fp_c + 1e-9)
    rec  = tp_c / n_gt
    ap = sum(prec[rec >= t].max() if (rec >= t).any() else 0 for t in np.linspace(0,1,11)) / 11
    return ap

def run_eval_on_perturbed(model, val_imgs, gt_boxes, perturbation_fn, conf=0.25):
    """Predict per-image and compute mAP50 and recall manually."""
    aps, tp_total, gt_total = [], 0, 0
    for p in val_imgs:
        img = cv2.imread(str(p))
        if img is None: continue
        h, w = img.shape[:2]
        perturbed = perturbation_fn(img)
        res = model.predict(perturbed, conf=conf, iou=0.5, imgsz=640, verbose=False)

        gt_raw = gt_boxes.get(p.stem, [])
        gt_xyxy = [[(cx-bw/2)*w, (cy-bh/2)*h, (cx+bw/2)*w, (cy+bh/2)*h]
                   for (cx,cy,bw,bh) in gt_raw]
        pred_xyxy = []
        if res and res[0].boxes is not None and len(res[0].boxes.xyxy) > 0:
            for box, c in zip(res[0].boxes.xyxy.cpu().numpy(), res[0].boxes.conf.cpu().numpy()):
                pred_xyxy.append((*box, float(c)))

        ap = compute_ap50(pred_xyxy, gt_xyxy)
        if ap is not None:
            aps.append(ap)
            # Recall: count TP for recall denominator
            matched = [False]*len(gt_xyxy)
            for pb in sorted(pred_xyxy, key=lambda x:-x[4]):
                best_iou, best_j = 0, -1
                for j, gb in enumerate(gt_xyxy):
                    ix1=max(pb[0],gb[0]); iy1=max(pb[1],gb[1])
                    ix2=min(pb[2],gb[2]); iy2=min(pb[3],gb[3])
                    iw=max(0,ix2-ix1); ih=max(0,iy2-iy1)
                    inter=iw*ih; union=(pb[2]-pb[0])*(pb[3]-pb[1])+(gb[2]-gb[0])*(gb[3]-gb[1])-inter
                    iou=inter/(union+1e-9)
                    if iou>best_iou: best_iou=iou; best_j=j
                if best_iou>=0.5 and best_j>=0 and not matched[best_j]:
                    tp_total+=1; matched[best_j]=True
            gt_total += len(gt_xyxy)

    map50 = round(float(np.mean(aps)), 4) if aps else 0.0
    recall = round(tp_total / (gt_total + 1e-9), 4)
    return {"mAP50": map50, "recall": recall}


# ── PERTURBATION FUNCTIONS ──────────────────────────────────────────────────
def identity(img): return img.copy()
def bright_down(img): return np.clip(img.astype(np.int16)-102,0,255).astype(np.uint8)
def bright_up(img):   return np.clip(img.astype(np.int16)+102,0,255).astype(np.uint8)
def blur5(img):   return cv2.GaussianBlur(img,(5,5),0)
def blur9(img):   return cv2.GaussianBlur(img,(9,9),0)
def motion9(img):
    k=np.zeros((9,9)); k[4,:]=1/9; return cv2.filter2D(img,-1,k)
def motion15(img):
    k=np.zeros((15,15)); k[7,:]=1/15; return cv2.filter2D(img,-1,k)
def jpeg30(img):
    _,buf=cv2.imencode(".jpg",img,[cv2.IMWRITE_JPEG_QUALITY,30]); return cv2.imdecode(buf,cv2.IMREAD_COLOR)
def noise10(img):
    np.random.seed(SEED)
    n=np.random.normal(0,10,img.shape).astype(np.int16)
    return np.clip(img.astype(np.int16)+n,0,255).astype(np.uint8)
def letterbox_fn(img): return img.copy()  # YOLO applies letterbox internally
def stretch_fn(img): return cv2.resize(img,(640,640))
def rotate90_fn(img):
    r=cv2.rotate(img,cv2.ROTATE_90_CLOCKWISE)
    return cv2.resize(r,(640,640))

model = YOLO(MODEL_50E)
val_imgs = get_image_list(VAL_IMG)
gt_boxes = parse_labels(VAL_LBL)

print("Computing clean baseline...")
baseline = run_eval_on_perturbed(model, val_imgs, gt_boxes, identity)
print(f"  Baseline: {baseline}")

# ── STEP 5: ROBUSTNESS ──────────────────────────────────────────────────────
print("\nStep 5: Robustness perturbations...")
robustness = {"clean_baseline": baseline}
perturbs = {
    "bright_down": (bright_down, "Brightness -40% (subtract 102 from all channels, clip [0,255])"),
    "bright_up":   (bright_up,   "Brightness +40% (add 102 to all channels, clip [0,255])"),
    "blur5":       (blur5,       "Gaussian blur, kernel=(5,5), sigma=0"),
    "blur9":       (blur9,       "Gaussian blur, kernel=(9,9), sigma=0"),
    "motion9":     (motion9,     "Horizontal motion blur, kernel size=9, horizontal kernel row k[4,:]=1/9"),
    "motion15":    (motion15,    "Horizontal motion blur, kernel size=15, horizontal kernel row k[7,:]=1/15"),
    "jpeg30":      (jpeg30,      "JPEG re-encode at quality=30"),
    "noise10":     (noise10,     "Gaussian additive noise, sigma=10, seed=0"),
}
for name, (fn, desc) in perturbs.items():
    res = run_eval_on_perturbed(model, val_imgs, gt_boxes, fn)
    res["delta_mAP50"]  = round(res["mAP50"]  - baseline["mAP50"],  4)
    res["delta_recall"] = round(res["recall"] - baseline["recall"], 4)
    res["params"] = desc
    robustness[name] = res
    print(f"  {name}: mAP50={res['mAP50']} recall={res['recall']} Δ={res['delta_mAP50']}")

# ── STEP 4: PREPROCESSING MISMATCH ─────────────────────────────────────────
print("\nStep 4: Preprocessing mismatch...")
preproc = {}
for name, fn in [("letterbox", letterbox_fn), ("stretch", stretch_fn), ("rotate90", rotate90_fn)]:
    res = run_eval_on_perturbed(model, val_imgs, gt_boxes, fn)
    preproc[name] = res
    print(f"  {name}: mAP50={res['mAP50']} recall={res['recall']}")

preproc["mAP50_delta_stretch_vs_letterbox"]   = round(preproc["stretch"]["mAP50"]  - preproc["letterbox"]["mAP50"],  4)
preproc["mAP50_delta_rotate90_vs_letterbox"]  = round(preproc["rotate90"]["mAP50"] - preproc["letterbox"]["mAP50"],  4)
preproc["recall_delta_stretch_vs_letterbox"]  = round(preproc["stretch"]["recall"] - preproc["letterbox"]["recall"], 4)
preproc["recall_delta_rotate90_vs_letterbox"] = round(preproc["rotate90"]["recall"]- preproc["letterbox"]["recall"], 4)
preproc["note"] = (
    "Preprocessing applied per-image in Python (cv2). Model runs predict() on the modified numpy array directly. "
    "Letterbox = identity (YOLO applies letterbox internally). "
    "Stretch = direct cv2.resize to 640x640 before passing to model (no padding). "
    "Rotate90 = cv2.ROTATE_90_CLOCKWISE then resize to 640x640."
)
print(f"  stretch delta mAP50={preproc['mAP50_delta_stretch_vs_letterbox']} recall={preproc['recall_delta_stretch_vs_letterbox']}")
print(f"  rotate90 delta mAP50={preproc['mAP50_delta_rotate90_vs_letterbox']} recall={preproc['recall_delta_rotate90_vs_letterbox']}")

# ── MERGE INTO results.json ─────────────────────────────────────────────────
results_path = os.path.join(BENCH_DIR, "results.json")
with open(results_path) as f:
    results = json.load(f)

results["step4_preprocessing"] = preproc
results["step5_robustness"] = robustness

with open(results_path, "w") as f:
    json.dump(results, f, indent=2, default=str)
print(f"\nresults.json updated: {results_path}")

# Save robustness CSV
rob_csv = os.path.join(BENCH_DIR, "robustness_results.csv")
with open(rob_csv, "w", newline="") as f:
    w = csv.writer(f)
    w.writerow(["perturbation","mAP50","recall","delta_mAP50","delta_recall","params"])
    w.writerow(["clean_baseline", baseline["mAP50"], baseline["recall"], 0.0, 0.0, "none"])
    for name, data in robustness.items():
        if name == "clean_baseline": continue
        w.writerow([name, data["mAP50"], data["recall"], data["delta_mAP50"], data["delta_recall"], data["params"]])
print(f"robustness_results.csv saved: {rob_csv}")
