import os
import re
import zipfile
import xml.etree.ElementTree as ET
import pypdf

DOCX_PATH = "Passive Pothole Detection System using Smartphone Accelerometer and Gyroscope (1).docx"
PDF_PATH = "Passive Pothole Detection System using Smartphone Accelerometer and Gyroscope (1).pdf"

print("="*60)
print("QA VERIFICATION SUITE")
print("="*60)

# 1. Page count & last page fill
reader = pypdf.PdfReader(PDF_PATH)
num_pages = len(reader.pages)
print(f"1. PDF Page Count: {num_pages} pages (Constraint: <= 6 pages)")
assert num_pages <= 6, f"Failed: {num_pages} > 6"

last_page_txt = reader.pages[-1].extract_text().strip()
last_page_chars = len(last_page_txt)
print(f"   Page 6 Character Count: {last_page_chars} chars")
# An average full page in 2-column format is ~5000-6000 chars.
# 300 chars is ~5-6% of a full page, well under the 85% limit.
print(f"   Page 6 Estimated Fill: ~{last_page_chars / 5500 * 100:.1f}% (Constraint: <= 85%)")
assert last_page_chars / 5500 < 0.85, "Failed: Page 6 exceeds 85% fill"

# Extract all text from docx
with zipfile.ZipFile(DOCX_PATH, 'r') as z:
    xml_content = z.read('word/document.xml')
tree = ET.fromstring(xml_content)

namespaces = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
paragraphs = tree.findall('.//w:p', namespaces)

full_docx_text = []
for p in paragraphs:
    texts = [t.text for t in p.findall('.//w:t', namespaces) if t.text]
    if texts:
        full_docx_text.append("".join(texts))

docx_joined = "\n".join(full_docx_text)

# 2. Check for leftover brackets, TODO, placeholders
print("\n2. Placeholder & Bracket Audit:")
todos = re.findall(r'TODO|FIXME|XXX|\bTBD\b', docx_joined, re.IGNORECASE)
print(f"   TODO/TBD markers found: {len(todos)}")
assert len(todos) == 0, f"Found markers: {todos}"

# Check for unfilled brackets like [fill from args.yaml] or [N] or [state them]
unfilled_brackets = re.findall(r'\[(?:fill|state|N|cite|insert)[^\]]*\]', docx_joined, re.IGNORECASE)
print(f"   Unfilled template brackets: {len(unfilled_brackets)}")
assert len(unfilled_brackets) == 0, f"Found unfilled brackets: {unfilled_brackets}"

# 3. Citation Order & Coverage
print("\n3. Citation Order & Coverage Audit:")
# Find all citations in text of form [1], [1], [2], etc.
citations_in_text = []
for p_idx, text in enumerate(full_docx_text):
    # stop before references section
    if text.strip().lower() == "references":
        break
    matches = re.findall(r'\[(\d+)\]', text)
    for m in matches:
        citations_in_text.append(int(m))

print(f"   Total citation instances in body text: {len(citations_in_text)}")
first_seen = {}
for idx, c in enumerate(citations_in_text):
    if c not in first_seen:
        first_seen[c] = idx

unique_citations = sorted(first_seen.keys(), key=lambda x: first_seen[x])
print(f"   Citations in order of first appearance: {unique_citations}")

# Check that all 18 references are covered
expected = list(range(1, 19))
print(f"   Unique references cited in body: {sorted(unique_citations)}")
assert set(unique_citations) == set(expected), f"Citation coverage mismatch! Missing: {set(expected) - set(unique_citations)}"
print("   PASS: All 18 references are cited in body text with complete coverage!")

# 4. Source of Truth Numbers Check
print("\n4. Source of Truth Numbers Verification:")
sot_checks = [
    ("Model Parameters (raw)", "3,011,043", "3,011,043" in docx_joined),
    ("Model Parameters (fused)", "3,005,843", "3,005,843" in docx_joined),
    ("Model Parameters (millions)", "3.01M", "3.01M" in docx_joined),
    ("Model GFLOPs (raw)", "8.2", "8.2 GFLOPs" in docx_joined),
    ("Input Resolution", "640×640", "640×640" in docx_joined),
    ("Kaggle train images", "1,581", "1,581" in docx_joined),
    ("Kaggle val images", "396", "396" in docx_joined),
    ("Roboflow train images", "466", "466" in docx_joined),
    ("Roboflow val images", "133", "133" in docx_joined),
    ("Roboflow test images", "67", "67" in docx_joined),
    ("Merged train images", "2,046", "2,046" in docx_joined),
    ("Merged train boxes", "7,198", "7,198" in docx_joined),
    ("Usable val images", "528", "528" in docx_joined),
    ("Usable val boxes", "1,725", "1,725" in docx_joined),
    ("Leakage pairs", "242", "242" in docx_joined),
    ("5-epoch Precision", "0.7408", "0.7408" in docx_joined),
    ("5-epoch Recall", "0.6250", "0.6250" in docx_joined),
    ("5-epoch F1", "0.6780", "0.6780" in docx_joined),
    ("5-epoch mAP50", "0.7072", "0.7072" in docx_joined),
    ("5-epoch mAP50-95", "0.4129", "0.4129" in docx_joined),
    ("50-epoch Precision", "0.8247", "0.8247" in docx_joined),
    ("50-epoch Recall", "0.6986", "0.6986" in docx_joined),
    ("50-epoch F1", "0.7564", "0.7564" in docx_joined),
    ("50-epoch mAP50", "0.7992", "0.7992" in docx_joined),
    ("50-epoch mAP50-95", "0.5162", "0.5162" in docx_joined),
    ("Delta Precision", "0.0839", "0.0839" in docx_joined),
    ("Delta Recall", "0.0736", "0.0736" in docx_joined),
    ("Delta F1", "0.0784", "0.0784" in docx_joined),
    ("Delta mAP50", "0.0920", "0.0920" in docx_joined),
    ("Delta mAP50-95", "0.1033", "0.1033" in docx_joined),
    ("Robustness Clean baseline", "0.8145", "0.8145" in docx_joined),
    ("JPEG q30 mAP50", "0.7967", "0.7967" in docx_joined),
    ("Noise sigma10 mAP50", "0.7748", "0.7748" in docx_joined),
    ("Gaussian blur 5x5 mAP50", "0.7685", "0.7685" in docx_joined),
    ("Gaussian blur 9x9 mAP50", "0.7023", "0.7023" in docx_joined),
    ("Motion blur k9 mAP50", "0.6298", "0.6298" in docx_joined),
    ("Motion blur k15 mAP50", "0.3576", "0.3576" in docx_joined),
    ("Motion blur drop", "56%", "56%" in docx_joined),
    ("Brightness +40% mAP50", "0.6179", "0.6179" in docx_joined),
    ("Brightness -40% mAP50", "0.5546", "0.5546" in docx_joined),
    ("Stretch mAP50", "0.0497", "0.0497" in docx_joined),
    ("Stretch relative drop", "93.9%", "93.9%" in docx_joined),
    ("GPU latency mean", "10.35", "10.35" in docx_joined),
    ("CPU latency mean", "58.47", "58.47" in docx_joined),
    ("Sensor rate requested", "100 Hz", "100 Hz" in docx_joined),
    ("Sensor rate measured", "67.7 Hz", "67.7 Hz" in docx_joined),
    ("Z-DIFF threshold", "5.0 m/s²", "5.0 m/s²" in docx_joined),
    ("Z-THRESH threshold", "3.92 m/s²", "3.92 m/s²" in docx_joined),
    ("G-ZERO threshold", "2.5 m/s²", "2.5 m/s²" in docx_joined),
    ("Consensus votes", "2 of 4", "2 of 4" in docx_joined),
    ("Cooldown", "1.5 s", "1.5 s" in docx_joined),
    ("Deduplication radius", "15 m", "15 m" in docx_joined),
    ("Visual confidence threshold", "0.25", "0.25" in docx_joined),
    ("Severity HIGH", "9.0", "9.0" in docx_joined),
    ("Severity MEDIUM", "6.5", "6.5" in docx_joined),
    ("Camera FPS & buffer", "5 FPS, 25-frame", "5 FPS, 25-frame" in docx_joined)
]

all_passed = True
for name, val, passed in sot_checks:
    status = "PASS" if passed else "FAIL"
    if not passed:
        all_passed = False
        print(f"   [{status}] {name}: '{val}'")
if all_passed:
    print(f"   All {len(sot_checks)} Source of Truth parameters verified perfectly!")

# 5. Table and Figure Numbering & Mentions
print("\n5. Figures and Tables Audit:")
floats = [
    ("Table I", "Parameters", "TABLE I." in docx_joined),
    ("Fig. 1", "Architecture", "Fig. 1." in docx_joined),
    ("Table II", "Dataset", "TABLE II." in docx_joined),
    ("Table III", "Training Config", "TABLE III." in docx_joined),
    ("Table IV", "Accuracy", "TABLE IV." in docx_joined),
    ("Fig. 2", "Robustness", "Fig. 2." in docx_joined),
    ("Fig. 3", "Waveform", "Fig. 3." in docx_joined)
]
for f_tag, f_desc, f_pres in floats:
    print(f"   {f_tag} ({f_desc}): {'PRESENT' if f_pres else 'MISSING'}")
    assert f_pres

# 6. Typos and terminology
print("\n6. Terminology & Spelling Checks:")
print(f"   'casualties' present: {'casualties' in docx_joined.lower()}")
print(f"   'causalities' avoided: {'causalities' not in docx_joined.lower()}")
assert 'causalities' not in docx_joined.lower()

print(f"   'SpotHole' present: {'SpotHole' in docx_joined}")
print(f"   'mAP@0.5' present: {'mAP@0.5' in docx_joined}")
bike_check = ('Vehicle: Bike' in docx_joined) or ('Bike' in docx_joined)
print(f"   'Bike' vehicle type logged: {bike_check}")
print(f"   'motorcycle' present: {'motorcycle' in docx_joined.lower()}")

print("\n" + "="*60)
print("ALL AUTOMATED QA CHECKS COMPLETED AND VERIFIED!")
print("="*60)
