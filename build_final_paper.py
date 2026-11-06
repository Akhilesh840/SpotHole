import os
import re
import zipfile
import xml.etree.ElementTree as ET

DOCX_BACKUP = "Passive Pothole Detection System using Smartphone Accelerometer and Gyroscope (1)_UNTOUCHED_BACKUP.docx"
DOCX_TEST = "Passive Pothole Detection System using Smartphone Accelerometer and Gyroscope (1).docx"
FIG_ARCH = "figures/fig_arch.png"
FIG_ROB = "figures/fig_robustness.png"

with zipfile.ZipFile(DOCX_BACKUP, 'r') as zin:
    files = {name: zin.read(name) for name in zin.namelist()}

with open(FIG_ARCH, 'rb') as f:
    files['word/media/fig_arch.png'] = f.read()
with open(FIG_ROB, 'rb') as f:
    files['word/media/fig_robustness.png'] = f.read()

rels_str = files['word/_rels/document.xml.rels'].decode('utf-8')
rids = [int(m) for m in re.findall(r'Id="rId(\d+)"', rels_str)]
next_id = max(rids) + 1
rid_arch = f"rId{next_id}"
rid_rob = f"rId{next_id+1}"

pos_close = rels_str.rfind('</Relationships>')
new_rels = (
    f'<Relationship Id="{rid_arch}" Type="http://purl.oclc.org/ooxml/officeDocument/relationships/image" Target="media/fig_arch.png"/>'
    f'<Relationship Id="{rid_rob}" Type="http://purl.oclc.org/ooxml/officeDocument/relationships/image" Target="media/fig_robustness.png"/>'
)
files['word/_rels/document.xml.rels'] = (rels_str[:pos_close] + new_rels + rels_str[pos_close:]).encode('utf-8')

cx_arch = "3108960" # 3.4 in
cy_arch = "2331720" # 2.55 in
cx_rob = "3108960"  # 3.4 in
cy_rob = "1965960"  # 2.15 in

def p_tag(text, style="BodyText", jc=None, space_before=None, space_after=None, bold_prefix=None, italic_prefix=None):
    jc_xml = f'<w:jc w:val="{jc}"/>' if jc else ''
    sp_xml = ''
    if space_before is not None or space_after is not None:
        sb = f'w:before="{space_before}" ' if space_before is not None else ''
        sa = f'w:after="{space_after}" ' if space_after is not None else ''
        sp_xml = f'<w:spacing {sb}{sa}/>'
    
    style_xml = f'<w:pStyle w:val="{style}"/>' if style else ''
    pPr = f'<w:pPr>{style_xml}{jc_xml}{sp_xml}</w:pPr>'
    
    runs = []
    if bold_prefix:
        runs.append(f'<w:r><w:rPr><w:b/><w:bCs/></w:rPr><w:t xml:space="preserve">{bold_prefix} </w:t></w:r>')
    if italic_prefix:
        runs.append(f'<w:r><w:rPr><w:i/><w:iCs/></w:rPr><w:t xml:space="preserve">{italic_prefix} </w:t></w:r>')
    
    safe_text = (text.replace("&", "&amp;")
                     .replace("<", "&lt;")
                     .replace(">", "&gt;"))
    runs.append(f'<w:r><w:t xml:space="preserve">{safe_text}</w:t></w:r>')
    
    return f'<w:p>{pPr}{"".join(runs)}</w:p>'

def heading1(title_without_num):
    return p_tag(title_without_num, style="Heading1", space_before=160, space_after=50)

def heading2(title_without_letter):
    return p_tag(title_without_letter, style="Heading2", space_before=120, space_after=30)

def fig_inline(rid, name, descr, cx, cy, caption_text):
    safe_cap = caption_text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    fig_p = f'''<w:p>
  <w:pPr>
    <w:jc w:val="center"/>
    <w:spacing w:before="120" w:after="50"/>
  </w:pPr>
  <w:r>
    <w:rPr><w:noProof/></w:rPr>
    <w:drawing>
      <wp:inline distT="0" distB="0" distL="0" distR="0">
        <wp:extent cx="{cx}" cy="{cy}"/>
        <wp:effectExtent l="0" t="0" r="0" b="0"/>
        <wp:docPr id="2001" name="{name}" descr="{descr}"/>
        <wp:cNvGraphicFramePr>
          <a:graphicFrameLocks xmlns:a="http://purl.oclc.org/ooxml/drawingml/main" noChangeAspect="1"/>
        </wp:cNvGraphicFramePr>
        <a:graphic xmlns:a="http://purl.oclc.org/ooxml/drawingml/main">
          <a:graphicData uri="http://purl.oclc.org/ooxml/drawingml/picture">
            <pic:pic xmlns:pic="http://purl.oclc.org/ooxml/drawingml/picture">
              <pic:nvPicPr>
                <pic:cNvPr id="0" name="{name}"/>
                <pic:cNvPicPr>
                  <a:picLocks noChangeAspect="1" noChangeArrowheads="1"/>
                </pic:cNvPicPr>
              </pic:nvPicPr>
              <pic:blipFill>
                <a:blip xmlns:r="http://purl.oclc.org/ooxml/officeDocument/relationships" r:embed="{rid}"/>
                <a:stretch><a:fillRect/></a:stretch>
              </pic:blipFill>
              <pic:spPr bwMode="auto">
                <a:xfrm>
                  <a:off x="0" y="0"/>
                  <a:ext cx="{cx}" cy="{cy}"/>
                </a:xfrm>
                <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
                <a:noFill/><a:ln><a:noFill/></a:ln>
              </pic:spPr>
            </pic:pic>
          </a:graphicData>
        </a:graphic>
      </wp:inline>
    </w:drawing>
  </w:r>
</w:p>'''
    cap_p = f'''<w:p>
  <w:pPr>
    <w:pStyle w:val="figurecaption"/>
    <w:jc w:val="center"/>
    <w:spacing w:before="50" w:after="120"/>
  </w:pPr>
  <w:r><w:t xml:space="preserve">{safe_cap}</w:t></w:r>
</w:p>'''
    return fig_p + cap_p

def table_head(cap_id, cap_text):
    return f'''<w:p>
  <w:pPr>
    <w:pStyle w:val="tablehead"/>
    <w:jc w:val="center"/>
    <w:spacing w:before="120" w:after="50"/>
  </w:pPr>
  <w:r><w:rPr><w:b/><w:bCs/></w:rPr><w:t>{cap_id}.</w:t></w:r>
  <w:r><w:t xml:space="preserve"> {cap_text.upper()}</w:t></w:r>
</w:p>'''

def build_table_1():
    rows_data = [
        ("Stage", "Parameter", "Configured Value", "Equivalent Acceleration"),
        ("Voting", "Z-DIFF threshold", "5.0 m/s²", "0.51 g"),
        ("Voting", "Z-THRESH threshold", "3.92 m/s²", "0.40 g"),
        ("Voting", "STDEV(Z) threshold", "5.0 m/s²", "0.51 g"),
        ("Voting", "G-ZERO threshold and duration", "2.5 m/s², 3 samples", "0.26 g"),
        ("Voting", "Consensus voting rule", "2 of 4 algorithms", "—"),
        ("Filter", "Hard braking threshold", "4.5 m/s² (Z_diff < 2.5)", "0.46 g"),
        ("Filter", "Sharp turning (yaw rate)", "0.3 rad/s", "17.2°/s"),
        ("Filter", "Excessive roll/pitch rate", "1.2 rad/s (Z_diff < 2.5)", "68.8°/s"),
        ("Filter", "Cooldown period", "1.5 seconds", "—"),
        ("Filter", "Deduplication window", "15 meters within 3.0 seconds", "—"),
        ("Vision", "Visual confidence threshold", "0.25", "—"),
        ("Severity", "HIGH / MEDIUM thresholds", "9.0 m/s² / 6.5 m/s²", "0.92 g / 0.66 g"),
        ("Camera", "Buffer capacity and frame rate", "25 frames at 5 FPS (5.0 s)", "—")
    ]
    xml = ['<w:tbl><w:tblPr><w:tblStyle w:val="TableGrid"/><w:tblW w:w="0pt" w:type="dxa"/><w:tblLook w:firstRow="1" w:lastRow="0" w:firstColumn="0" w:lastColumn="0" w:noHBand="0" w:noVBand="1"/></w:tblPr>']
    xml.append('<w:tblGrid><w:gridCol w:w="1100"/><w:gridCol w:w="2200"/><w:gridCol w:w="1900"/><w:gridCol w:w="1200"/></w:tblGrid>')
    for r_idx, row in enumerate(rows_data):
        xml.append('<w:tr>')
        is_header = (r_idx == 0)
        style = "tablecolhead" if is_header else "tablecopy"
        for val in row:
            bold = '<w:b/><w:bCs/>' if is_header else ''
            safe_val = val.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            xml.append(f'''<w:tc>
  <w:tcPr><w:tcW w:w="0pt" w:type="dxa"/></w:tcPr>
  <w:p>
    <w:pPr><w:pStyle w:val="{style}"/><w:jc w:val="left"/><w:rPr>{bold}<w:sz w:val="15"/><w:szCs w:val="15"/></w:rPr></w:pPr>
    <w:r><w:rPr>{bold}<w:sz w:val="15"/><w:szCs w:val="15"/></w:rPr><w:t>{safe_val}</w:t></w:r>
  </w:p>
</w:tc>''')
        xml.append('</w:tr>')
    xml.append('</w:tbl>')
    return "".join(xml)

def build_table_2():
    rows_data = [
        ("Split / Source", "Kaggle", "Roboflow", "Merged", "Boxes", "Avg/Img"),
        ("Training split", "1,581", "466", "2,046", "7,198", "3.52"),
        ("Validation split", "396", "133", "528 usable", "1,725", "3.27"),
        ("Test split (held-out)", "—", "67", "67", "—", "—")
    ]
    xml = ['<w:tbl><w:tblPr><w:tblStyle w:val="TableGrid"/><w:tblW w:w="0pt" w:type="dxa"/><w:tblLook w:firstRow="1" w:lastRow="0" w:firstColumn="0" w:lastColumn="0" w:noHBand="0" w:noVBand="1"/></w:tblPr>']
    xml.append('<w:tblGrid><w:gridCol w:w="1800"/><w:gridCol w:w="800"/><w:gridCol w:w="800"/><w:gridCol w:w="1000"/><w:gridCol w:w="800"/><w:gridCol w:w="800"/></w:tblGrid>')
    for r_idx, row in enumerate(rows_data):
        xml.append('<w:tr>')
        is_header = (r_idx == 0)
        style = "tablecolhead" if is_header else "tablecopy"
        for val in row:
            bold = '<w:b/><w:bCs/>' if is_header else ''
            safe_val = val.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            xml.append(f'''<w:tc>
  <w:tcPr><w:tcW w:w="0pt" w:type="dxa"/></w:tcPr>
  <w:p>
    <w:pPr><w:pStyle w:val="{style}"/><w:jc w:val="left"/><w:rPr>{bold}<w:sz w:val="15"/><w:szCs w:val="15"/></w:rPr></w:pPr>
    <w:r><w:rPr>{bold}<w:sz w:val="15"/><w:szCs w:val="15"/></w:rPr><w:t>{safe_val}</w:t></w:r>
  </w:p>
</w:tc>''')
        xml.append('</w:tr>')
    xml.append('</w:tbl>')
    return "".join(xml)

def build_table_train_config():
    rows_data = [
        ("Hyperparameter / Setting", "Configured Value", "Operational Specification"),
        ("Base Architecture", "YOLOv8n (yolov8n.pt)", "3,011,043 params (3,005,843 fused), 8.2 GFLOPs"),
        ("Input Dimensions", "640×640 pixels", "3-channel RGB, letterbox padded"),
        ("Fine-Tuning Schedule", "50 epochs (55 total seen)", "Fine-tuned from 5-epoch baseline checkpoint"),
        ("Batch Size & Optimizer", "Batch 16, SGD (auto)", "Momentum 0.937, Weight decay 0.0005"),
        ("Learning Rate Schedule", "lr0 = 0.01, lrf = 0.01", "Linear decay, 3.0 warmup epochs (momentum 0.8)"),
        ("Augmentation Policy", "Mosaic (1.0), Fliplr (0.5)", "HSV (0.015, 0.7, 0.4), Trans 0.1, Scale 0.5; mosaic off last 10 e"),
        ("Training Hardware", "RTX 4050 Laptop GPU (6 GB)", "PyTorch 2.5.1+cu121, Ultralytics 8.4.173, seed 0")
    ]
    xml = ['<w:tbl><w:tblPr><w:tblStyle w:val="TableGrid"/><w:tblW w:w="0pt" w:type="dxa"/><w:tblLook w:firstRow="1" w:lastRow="0" w:firstColumn="0" w:lastColumn="0" w:noHBand="0" w:noVBand="1"/></w:tblPr>']
    xml.append('<w:tblGrid><w:gridCol w:w="2100"/><w:gridCol w:w="2000"/><w:gridCol w:w="2300"/></w:tblGrid>')
    for r_idx, row in enumerate(rows_data):
        xml.append('<w:tr>')
        is_header = (r_idx == 0)
        style = "tablecolhead" if is_header else "tablecopy"
        for val in row:
            bold = '<w:b/><w:bCs/>' if is_header else ''
            safe_val = val.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            xml.append(f'''<w:tc>
  <w:tcPr><w:tcW w:w="0pt" w:type="dxa"/></w:tcPr>
  <w:p>
    <w:pPr><w:pStyle w:val="{style}"/><w:jc w:val="left"/><w:rPr>{bold}<w:sz w:val="15"/><w:szCs w:val="15"/></w:rPr></w:pPr>
    <w:r><w:rPr>{bold}<w:sz w:val="15"/><w:szCs w:val="15"/></w:rPr><w:t>{safe_val}</w:t></w:r>
  </w:p>
</w:tc>''')
        xml.append('</w:tr>')
    xml.append('</w:tbl>')
    return "".join(xml)

def build_table_4():
    rows_data = [
        ("Model", "Precision", "Recall", "F1", "mAP@0.5", "mAP@0.5:0.95"),
        ("5-epoch baseline", "0.7408", "0.6250", "0.6780", "0.7072", "0.4129"),
        ("50-epoch fine-tuned", "0.8247", "0.6986", "0.7564", "0.7992", "0.5162")
    ]
    xml = ['<w:tbl><w:tblPr><w:tblStyle w:val="TableGrid"/><w:tblW w:w="0pt" w:type="dxa"/><w:tblLook w:firstRow="1" w:lastRow="0" w:firstColumn="0" w:lastColumn="0" w:noHBand="0" w:noVBand="1"/></w:tblPr>']
    xml.append('<w:tblGrid><w:gridCol w:w="1900"/><w:gridCol w:w="800"/><w:gridCol w:w="800"/><w:gridCol w:w="800"/><w:gridCol w:w="900"/><w:gridCol w:w="1100"/></w:tblGrid>')
    for r_idx, row in enumerate(rows_data):
        xml.append('<w:tr>')
        is_header = (r_idx == 0)
        style = "tablecolhead" if is_header else "tablecopy"
        for val in row:
            bold = '<w:b/><w:bCs/>' if is_header else ''
            safe_val = val.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            xml.append(f'''<w:tc>
  <w:tcPr><w:tcW w:w="0pt" w:type="dxa"/></w:tcPr>
  <w:p>
    <w:pPr><w:pStyle w:val="{style}"/><w:jc w:val="left"/><w:rPr>{bold}<w:sz w:val="15"/><w:szCs w:val="15"/></w:rPr></w:pPr>
    <w:r><w:rPr>{bold}<w:sz w:val="15"/><w:szCs w:val="15"/></w:rPr><w:t>{safe_val}</w:t></w:r>
  </w:p>
</w:tc>''')
        xml.append('</w:tr>')
    xml.append('</w:tbl>')
    return "".join(xml)

doc_xml = files['word/document.xml'].decode('utf-8')
pos_abstract = doc_xml.find('<w:pStyle w:val="Abstract"/>')
p_start_abstract = doc_xml.rfind('<w:p ', 0, pos_abstract)
if p_start_abstract == -1:
    p_start_abstract = doc_xml.rfind('<w:p>', 0, pos_abstract)

header_xml = doc_xml[:p_start_abstract]

new_title_p = (
    '<w:p><w:pPr><w:pStyle w:val="papertitle"/><w:spacing w:after="240"/><w:jc w:val="center"/></w:pPr>'
    '<w:r><w:t>SpotHole: Two-Stage Pothole Detection for Handlebar-Mounted Smartphones Using Inertial Sensing and On-Demand YOLOv8n Verification</w:t></w:r>'
    '</w:p>'
)
pos_pt = header_xml.find('<w:pStyle w:val="papertitle"/>')
p_start_pt = header_xml.rfind('<w:p ', 0, pos_pt)
if p_start_pt == -1:
    p_start_pt = header_xml.rfind('<w:p>', 0, pos_pt)
p_end_pt = header_xml.find('</w:p>', pos_pt) + 6
header_xml = header_xml[:p_start_pt] + new_title_p + header_xml[p_end_pt:]

# Locate section properties for 2-column body
matches = [m.start() for m in re.finditer(r'<w:sectPr\b', doc_xml)]
sect2_pos = matches[1]
sect_2col_p_start = max(doc_xml.rfind('<w:p ', 0, sect2_pos), doc_xml.rfind('<w:p>', 0, sect2_pos))
sect_2col_p_end = doc_xml.find('</w:p>', sect2_pos) + 6
sect_2col_p = doc_xml[sect_2col_p_start:sect_2col_p_end]

sect3_pos = matches[2]
end_sect_xml = doc_xml[sect3_pos:doc_xml.find('</w:sectPr>', sect3_pos)+11]

body_parts = []

# Abstract and Keywords
abstract_text = (
    "Abstract—Potholes damage vehicles and cause accidents, yet road agencies rely on slow and costly manual surveys "
    "to find them. Smartphone accelerometers offer a cheaper alternative, but inertial detectors raise false alarms "
    "on speed breakers, hard braking, and sudden steering. This paper presents SpotHole, a two-stage Android framework "
    "for smartphones mounted on motorcycle handlebars. The first stage projects inertial data (100 Hz requested, "
    "67.7 Hz measured) onto the gravity vector to obtain orientation-independent vertical acceleration, then applies "
    "a voting rule over four threshold algorithms and kinematic rejection filters. The second stage checks buffered "
    "camera frames with a one-class YOLOv8n model (3.01M parameters) only when a candidate shock occurs. The model "
    "was trained on 2,046 images with 7,198 annotated potholes. On 528 validation images it reached a precision of "
    "0.825, a recall of 0.699, an F1 score of 0.756, and an mAP@0.5 of 0.799 (mAP@0.5:0.95 of 0.516). Under synthetic "
    "degradations, JPEG compression changed mAP@0.5 by less than 0.02, whereas strong motion blur lowered it by 0.457 "
    "and brightness changes by up to 0.260. A hash audit found 242 near-duplicate training and validation pairs, so the "
    "metrics are optimistic. Field evaluation on labeled rides is future work."
)
keywords_text = "Keywords—pothole detection, smartphone sensing, accelerometer, YOLOv8, sensor-camera fusion, Android"

body_parts.append(p_tag(abstract_text, style="Abstract", jc="both", space_before=120, space_after=50))
body_parts.append(p_tag(keywords_text, style="Keywords", jc="both", space_before=50, space_after=140))

# I. INTRODUCTION
body_parts.append(heading1("INTRODUCTION"))
intro_p1 = (
    "Road infrastructure condition directly impacts commuter safety, vehicle durability, and transit efficiency. "
    "In developing road networks, rapid surface deterioration driven by monsoon precipitation, inadequate drainage, "
    "and overloaded commercial traffic produces severe asphalt anomalies, predominantly potholes. According to the "
    "Ministry of Road Transport and Highways (MoRTH), road accidents claimed 1,856 lives specifically attributable to "
    "potholes in 2022, while two-wheeler riders accounted for 44.5% of total road accident casualties [1]. Motorcyclists "
    "face elevated vulnerability because sudden impacts destabilize two-wheeled dynamics, precipitating rider falls "
    "and fatal collisions. Municipal road agencies rely largely on manual visual inspections or specialized laser "
    "profilometer vehicles. These conventional approaches are labor-intensive, logistically demanding, and suffer from "
    "infrequent update intervals that leave severe road hazards undetected for months."
)
body_parts.append(p_tag(intro_p1, style="BodyText", jc="both"))

intro_p2 = (
    "Smartphones offer a ubiquitous edge-sensing platform equipped with tri-axial accelerometers, gyroscopes, GNSS receivers, "
    "and high-resolution image sensors. Prior mobile sensing systems utilize threshold heuristics, statistical energy "
    "filters, or machine learning classifiers on accelerometer streams. However, single-modality inertial systems suffer "
    "from high false alarm rates caused by non-pothole operational disturbances, including speed breakers, railway tracks, "
    "expansion joints, abrupt braking, and aggressive lane changes. Conversely, pure vision systems require continuous camera "
    "capture and dense object detection inference, consuming excessive power and degrading under motion blur or lighting extremes."
)
body_parts.append(p_tag(intro_p2, style="BodyText", jc="both"))

intro_p3 = (
    "To overcome these limitations, this paper presents SpotHole, a two-stage multimodal pothole detection architecture optimized "
    "for motorcycle handlebar-mounted smartphones. SpotHole operates passively: Stage 1 continuously monitors orientation-independent "
    "vertical acceleration derived from linear acceleration and gravity vectors, using a 2-of-4 consensus voting heuristic coupled with "
    "kinematic rejection filters. Only upon consensus trigger does Stage 2 activate on-demand vision confirmation using a lightweight, "
    "one-class YOLOv8n detector applied to buffered camera frames. In this work, we formalize the system architecture, evaluate the YOLOv8n "
    "detector across 528 validation images, measure robustness against optical degradations, and disclose a perceptual hash audit finding "
    "242 near-duplicate image pairs between training and validation splits."
)
body_parts.append(p_tag(intro_p3, style="BodyText", jc="both"))

# II. RELATED WORK
body_parts.append(heading1("RELATED WORK"))
rw_pa = (
    "A. Inertial Sensing: Pothole Patrol fitted taxis with three-axis accelerometers and GPS and detected 75% of surfaces a human "
    "rated as bad with under 5% false positives [2]; Nericell moved sensing to smartphones and added accelerometer axis reorientation [3]; "
    "Mednis et al. ran four threshold algorithms (Z-THRESH, Z-DIFF, STDEV(Z), G-ZERO) in real time on Android [4]; Bhatt et al. added the "
    "gyroscope and a real-time app with road condition maps [5]."
)
body_parts.append(p_tag(rw_pa, style="BodyText", jc="both"))

rw_pb = (
    "B. Vision-Based Detection: Maeda et al. built a dataset of 9,053 smartphone images with 15,435 damage instances from a car "
    "and compared accuracy and runtime on a GPU server and a smartphone [6]; RDD2020 has 26,336 images from India, Japan, and "
    "the Czech Republic with over 31,000 instances in four classes including potholes [7]; YOLO-family detectors [8], [9]; "
    "vision accuracy depends on lighting and shadow [10]."
)
body_parts.append(p_tag(rw_pb, style="BodyText", jc="both"))

rw_pc = (
    "C. Multi-Modal Sensor and Camera Fusion: Xin et al. fuse phone acceleration with in-vehicle video uploaded by users and "
    "classify with an LSTM [11]; Silvister et al. combine an SSD camera detector with a network on accelerometer and gyroscope "
    "data, so each cross-checks the other [12]; Villareal and Samaniego built an Android app combining YOLO with accelerometer, "
    "gyroscope, and GPS, and in 18.97 km of field testing obtained 130 detections of which 84 were verified, a verification rate "
    "of 64.62% [13]; Ramesh et al. describe cloud-based collaborative road-damage monitoring with smartphones [14]."
)
body_parts.append(p_tag(rw_pc, style="BodyText", jc="both"))

rw_pd = (
    "D. Positioning of SpotHole: The fusion systems above run both detectors at once or fuse uploaded data on a server; SpotHole "
    "uses the accelerometer first and runs the camera model only for candidate events, on the phone, on a handlebar-mounted "
    "bike, and adds a robustness and leakage analysis."
)
body_parts.append(p_tag(rw_pd, style="BodyText", jc="both"))

# III. SYSTEM ARCHITECTURE
body_parts.append(heading1("SYSTEM ARCHITECTURE"))
body_parts.append(heading2("Architectural Overview"))
arch_p1 = (
    "SpotHole adopts the Model-View-ViewModel (MVVM) architectural pattern, organized into four functional tiers: sensing, "
    "detection, persistence, and presentation (Fig. 1). Road monitoring requires continuous background operation while riding; "
    "thus, the sensing and detection pipelines execute within a persistent Android Foreground Service. This design prevents "
    "operating system task termination when the smartphone is mounted on motorcycle handlebars. The service performs sensor "
    "sampling, maintains an in-memory video ring buffer, extracts kinematic features, evaluates candidate shocks, and commits "
    "confirmed anomalies to a local SQLite database using Room. The presentation tier observes reactive database streams to render "
    "anomaly markers on an interactive map."
)
body_parts.append(p_tag(arch_p1, style="BodyText", jc="both"))

body_parts.append(fig_inline(rid_arch, "Fig. 1", "SpotHole system architecture.", cx_arch, cy_arch, "Fig. 1. SpotHole system architecture."))

body_parts.append(heading2("Sensing Layer"))
arch_p2 = (
    "The sensing tier registers hardware callbacks for the accelerometer, gyroscope, magnetometer, linear "
    "acceleration sensor, and gravity sensor. The application requests a 100 Hz sampling rate (10,000 µs interval hint). Owing "
    "to non-deterministic hardware dispatch on Android, empirical measurements indicate an effective average rate of 67.7 Hz "
    "(mean interval of 14.77 ms). The physical phone orientation on a handlebar mount is typically angled 30°–60° toward the rider. "
    "SpotHole resolves arbitrary tilt by mathematically projecting the three-axis linear acceleration vector onto the normalized "
    "gravity vector, yielding true orientation-independent vertical acceleration. Sensor noise is smoothed via a 5-sample moving average window. "
    "Concurrently, CameraX captures road frames at 5 FPS into a 25-frame circular ring buffer, maintaining approximately 5.0 seconds of pre-impact road imagery."
)
body_parts.append(p_tag(arch_p2, style="BodyText", jc="both"))

body_parts.append(heading2("Detection Layer"))
arch_p3 = (
    "Detection proceeds through three progressive stages: feature extraction, consensus voting, and on-demand visual confirmation. "
    "First, impact features are extracted from orientation-independent vertical acceleration and evaluated against four heuristic "
    "threshold algorithms: Z-DIFF, Z-THRESH, STDEV(Z), and G-ZERO. A candidate anomaly triggers only when at least two of the four "
    "algorithms fire simultaneously. Second, candidates pass through kinematic rejection gates: hard braking (>4.5 m/s² horizontal "
    "acceleration with vertical shock <2.5 m/s²), sharp turning (>0.3 rad/s yaw rate), and excessive roll/pitch (>1.2 rad/s gyro "
    "magnitude). Duplicate detections are suppressed by a 1.5 s cooldown and a 15 m spatial radius within 3.0 s. Third, candidate "
    "events are confirmed visually on demand with a lightweight YOLOv8n model through TensorFlow Lite. Feature processing and vision "
    "inference execute asynchronously on a background thread using Kotlin coroutines. The model takes a 640×640 RGB image and outputs "
    "8,400 anchor predictions with bounding boxes and pothole confidence scores. When no detection reaches a confidence of 0.25, the "
    "event is treated as a visual false alarm and rejected."
)
body_parts.append(p_tag(arch_p3, style="BodyText", jc="both"))

body_parts.append(heading2("Storage Layer"))
arch_p4 = (
    "Confirmed potholes are stored in a local Room SQLite database with a UUID primary key, timestamp, WGS84 GPS coordinates, severity rating "
    "(Low, Medium, High), impact magnitude, heuristic consensus vote mask, and a local URI reference to the saved camera frame. In addition, "
    "SpotHole writes continuous telemetry logs on an asynchronous I/O thread for offline analysis: raw sensor streams (sensor_log.csv), candidate "
    "trigger decisions and rejection reasons (candidate_log.csv), and session metadata logging vehicle type (Bike), mount type, phone model, "
    "and active threshold parameters."
)
body_parts.append(p_tag(arch_p4, style="BodyText", jc="both"))

body_parts.append(heading2("Presentation Layer"))
arch_p5 = (
    "The presentation layer uses osmdroid to render OpenStreetMap tiles showing detected potholes as color-coded markers based on "
    "severity (low, medium, high). A live waveform component displays real-time vertical acceleration during test rides. Users can "
    "review ride sessions and export all database records and raw CSV telemetry files as a compressed ZIP archive via the Android share sheet."
)
body_parts.append(p_tag(arch_p5, style="BodyText", jc="both"))

# IV. METHODOLOGY
body_parts.append(heading1("METHODOLOGY"))
body_parts.append(heading2("Orientation Reorientation and Signal Preprocessing"))
meth_p1 = (
    "Handlebar-mounted smartphones experience arbitrary pitch and roll angles relative to the road plane. SpotHole resolves orientation "
    "by computing the normalized gravity unit vector g_hat = g / ||g|| from the hardware gravity sensor. Orientation-independent "
    "vertical acceleration a_v is derived via dot product projection: a_v = a_linear · g_hat. To suppress high-frequency motorcycle engine "
    "vibrations and mechanical mount resonance, a_v is smoothed using a 5-point moving average filter: a_smooth[t] = 0.2 * sum_{i=0}^4 a_v[t-i]."
)
body_parts.append(p_tag(meth_p1, style="BodyText", jc="both"))

body_parts.append(heading2("Feature Extraction and Consensus Voting"))
meth_p2 = (
    "The feature extractor computes four quantities. Z_diff is the absolute difference between the current smoothed vertical value "
    "and the oldest value in a 10-sample history: Z_diff = |a_smooth[t] - a_smooth[t-9]|. Z_thresh is the absolute value of the current "
    "smoothed vertical acceleration: Z_thresh = |a_smooth[t]|. STDEV(Z) is the standard deviation of the last 20 unsmoothed vertical "
    "readings, capturing raw vibration energy. m is the Euclidean magnitude of the raw accelerometer vector, including gravity: "
    "m = sqrt(ax² + ay² + az²)."
)
body_parts.append(p_tag(meth_p2, style="BodyText", jc="both"))

meth_p2_extra = (
    "Each feature captures distinct physical dynamics. Z_diff operates as a finite-difference edge detector over a ~133 ms sliding window "
    "(at 67.7 Hz), accentuating abrupt asphalt step-downs while attenuating gradual road gradients and bridge approaches. Conversely, "
    "G-ZERO detects suspension decompression and momentary airborne free-fall immediately following high-velocity pothole rim impacts, "
    "where the net acceleration magnitude collapses below 2.5 m/s² (0.26 g). STDEV(Z) reflects persistent road roughness."
)
body_parts.append(p_tag(meth_p2_extra, style="BodyText", jc="both"))

meth_p3 = (
    "Candidate shocks are flagged when at least 2 of the 4 algorithms satisfy threshold conditions simultaneously: (1) Z_diff >= 5.0 m/s², "
    "(2) Z_thresh >= 3.92 m/s² (0.40 g), (3) STDEV(Z) >= 5.0 m/s², and (4) G-ZERO condition, where m < 2.5 m/s² for at least 3 consecutive "
    "samples. Consensus voting balances sensitivity and false alarm rejection across diverse road surface speeds."
)
body_parts.append(p_tag(meth_p3, style="BodyText", jc="both"))

body_parts.append(heading2("Kinematic False-Positive Rejection Filters"))
meth_p4 = (
    "Motorcycle dynamics introduce non-pothole inertial transients from braking, swerving, and leaning. Candidate events are vetted "
    "against three kinematic filters: (1) Hard Braking: horizontal acceleration > 4.5 m/s² with vertical shock < 2.5 m/s² indicates "
    "longitudinal deceleration without road impact; (2) Sharp Turn: absolute yaw rate > 0.3 rad/s (17.2°/s) identifies cornering; and "
    "(3) Excessive Angular Rate: total angular velocity > 1.2 rad/s (68.8°/s) with vertical shock < 2.5 m/s² suppresses rider mounting "
    "or handling disturbances. Furthermore, a 1.5 s temporal cooldown and a 15 m spatial deduplication window (within 3.0 s) prevent "
    "multiple registrations of the same pothole."
)
body_parts.append(p_tag(meth_p4, style="BodyText", jc="both"))

meth_p5 = (
    "A speed gate designed to suppress walking vibrations (<2.77 m/s / 10 km/h) is present in the architecture but was disabled in the "
    "evaluated build to permit stationary mount testing and low-speed validation."
)
body_parts.append(p_tag(meth_p5, style="BodyText", jc="both"))

body_parts.append(heading2("Camera Confirmation and Severity Classification"))
meth_p6 = (
    "CameraX acquires road frames at 640×640 resolution using a camera 5 FPS, 25-frame buffer. When an inertial "
    "candidate is accepted, up to 5 buffered frames captured prior to the shock are evaluated. Each frame undergoes letterbox pre-processing "
    "with aspect-ratio preservation and constant padding (rgb(114, 114, 114)) to match training dimensions. The TFLite YOLOv8n detector evaluates "
    "frames without non-maximum suppression (NMS) to minimize latency, confirming the event if any anchor exhibits a pothole confidence >= 0.25. "
    "Confirmed potholes are classified into High (>=9.0 m/s²), Medium (6.5–9.0 m/s²), and Low (<6.5 m/s²) severity based on vertical shock magnitude, "
    "as configured in Table I."
)
body_parts.append(p_tag(meth_p6, style="BodyText", jc="both"))

body_parts.append(table_head("TABLE I", "SpotHole Detection Parameters"))
body_parts.append(build_table_1())

# V. DATASET AND MODEL TRAINING
body_parts.append(heading1("DATASET AND MODEL TRAINING"))
body_parts.append(heading2("Data Sources and Composition"))
data_p1 = (
    "The vision confirmation model was trained on a single-class pothole dataset aggregated from two public sources annotated with normalized "
    "bounding boxes: the Kaggle Pothole Detection dataset (1,581 training and 396 validation images) [15] and the Roboflow Pothole v1 dataset "
    "(466 training, 133 validation, and 67 test images) [16]. Merging the training splits yielded 2,046 images containing 7,198 annotated pothole "
    "instances (3.52 boxes/image). The merged validation set comprises 529 files; one corrupt GIF was discarded, leaving 528 usable validation "
    "images with 1,725 annotated potholes (3.27 boxes/image). Table II details split distributions and box scale categories: small (<32² px), "
    "medium (32²–96² px), and large (>96² px)."
)
body_parts.append(p_tag(data_p1, style="BodyText", jc="both"))

body_parts.append(table_head("TABLE II", "Dataset Composition and Annotated Box Distribution"))
body_parts.append(build_table_2())

body_parts.append(heading2("Data Quality, Preprocessing, and Leakage Analysis"))
data_p2 = (
    "To evaluate split integrity, we performed a perceptual hash audit (pHash with Hamming distance threshold <= 4) across all image pairs. "
    "The audit identified 242 near-duplicate pairs between the training and validation sets. These duplicate pairs stem from video frame "
    "extraction in the underlying source repositories without strict sequence-based splitting, so some validation images resemble training images. "
    "Consequently, standard validation metrics represent an optimistic upper bound on generalization, underscoring the necessity of real-world "
    "field trials."
)
body_parts.append(p_tag(data_p2, style="BodyText", jc="both"))

body_parts.append(heading2("Model Architecture and Training Configuration"))
data_p3 = (
    "We chose YOLOv8n in its detection form, with 3,011,043 parameters (3,005,843 after layer fusing) and 8.2 GFLOPs at 640×640 input [9]. "
    "Training ran on an NVIDIA GeForce RTX 4050 Laptop GPU (6 GB) with Ultralytics 8.4.173 and PyTorch 2.5.1, seed 0. We trained a 5-epoch "
    "baseline from yolov8n.pt and a 50-epoch model fine-tuned from the baseline checkpoint, so the second model has seen about 55 epochs in total. "
    "Batch size, optimizer, learning rate, and augmentation follow args.yaml: batch size 16, SGD optimizer (momentum 0.937, weight decay 0.0005), "
    "initial learning rate 0.01 with linear decay to 0.01, and mosaic augmentation enabled (probability 1.0, turned off for the final 10 epochs). "
    "Table III summarizes the complete hyperparameter specification. Training loss converged steadily: at epoch 50, training box loss reached 1.044, "
    "classification loss reached 0.694, and distribution focal loss (dfl) reached 1.069, with validation box loss 1.206 and cls loss 0.904."
)
body_parts.append(p_tag(data_p3, style="BodyText", jc="both"))

body_parts.append(table_head("TABLE III", "YOLOv8n Training Configuration and Hyperparameters"))
body_parts.append(build_table_train_config())

# VI. RESULTS AND ANALYSIS
body_parts.append(heading1("RESULTS AND ANALYSIS"))
body_parts.append(heading2("Detection Accuracy"))
res_p1 = (
    "Table IV gives validation results for both models on the 528 usable validation images, evaluated at a confidence threshold of 0.001 "
    "and a non-maximum-suppression IoU of 0.6 following COCO metric standards [17]. The 50-epoch model improves on the baseline by 0.0839 in precision, "
    "0.0736 in recall, 0.0784 in F1, 0.0920 in mAP@0.5, and 0.1033 in mAP@0.5:0.95. The mAP@0.5 of 0.7992 against an mAP@0.5:0.95 of 0.5162 shows "
    "that the detector finds most potholes but fits boxes loosely at stricter overlap. SpotHole needs only the presence of a pothole, so mAP@0.5 "
    "and recall matter most. A recall of 0.6986 means about 30% of annotated potholes are missed at IoU 0.5."
)
body_parts.append(p_tag(res_p1, style="BodyText", jc="both"))

body_parts.append(table_head("TABLE IV", "Validation Accuracy"))
body_parts.append(build_table_4())

body_parts.append(heading2("Robustness to Optical and Environmental Degradation"))
res_p2 = (
    "A handlebar camera sees vibration, changing light, and image compression. We degraded the validation images one factor at a time and measured "
    "mAP@0.5 (Fig. 2). The clean baseline under this protocol is 0.8145. JPEG compression at quality 30 costs 0.0178 (mAP@0.5 0.7967), Gaussian noise (sigma 10) costs "
    "0.0397 (0.7748), and a 5×5 Gaussian blur costs 0.0460 (0.7685). A 9×9 blur costs 0.1122 (0.7023). Horizontal motion blur, a well-known degradation in mobile imaging [18], "
    "costs 0.1847 at kernel size 9 (0.6298) and 0.4569 at kernel size 15 (0.3576, a relative loss of 56%). A 40% brightness increase costs 0.1966 (0.6179) and a 40% decrease "
    "costs 0.2599 (0.5546). The model tolerates compression and mild noise but not strong blur or large lighting changes. These perturbations are synthetic, "
    "so they show sensitivity and do not predict the loss at a given riding speed. The clean baseline (0.8145) differs from Table IV (0.7992) because "
    "the two runs used different evaluation settings: Table IV computes dataset-level PR curves across all recall thresholds at IoU 0.6 using "
    "Ultralytics validation, whereas the perturbation script evaluates per-image matching at IoU 0.5 with model default confidence."
)
body_parts.append(p_tag(res_p2, style="BodyText", jc="both"))

res_p2_extra = (
    "These degradation tests reflect operational environmental stresses encountered during motorcycle commutes. Low-quality JPEG "
    "compression simulates aggressive storage downsampling or bandwidth-constrained edge upload over cellular networks. Gaussian noise "
    "models sensor noise artifacts under dim or shadowed road conditions. Gaussian blur simulates optics defocusing, while directional "
    "motion blur captures chassis-induced vibration and handlebar jitter at speed. Finally, the ±40% illumination shifts demonstrate "
    "performance under direct midday sun glare and overcast twilight riding."
)
body_parts.append(p_tag(res_p2_extra, style="BodyText", jc="both"))

body_parts.append(fig_inline(rid_rob, "Fig. 2", "mAP@0.5 of the 50-epoch model under synthetic image degradations (clean baseline 0.8145).", cx_rob, cy_rob, "Fig. 2. mAP@0.5 of the 50-epoch model under synthetic image degradations (clean baseline 0.8145)."))

body_parts.append(heading2("Input Resizing"))
res_p3 = (
    "Letterboxing gives an mAP@0.5 of 0.8145 and direct stretching to a square gives 0.0497, a relative drop of 93.9%. The model was trained on "
    "letterboxed inputs, so stretched images differ strongly from its training data. The deployed app uses letterboxing with aspect-ratio preservation "
    "and constant padding (rgb(114, 114, 114))."
)
body_parts.append(p_tag(res_p3, style="BodyText", jc="both"))

body_parts.append(heading2("Computational Latency and Inference Speed"))
res_p4 = (
    "On the training laptop, PyTorch inference averaged 10.35 ms per image on the GPU (median 10.17 ms, p95 11.25 ms, standard deviation 0.68 ms) "
    "and 58.47 ms on the CPU (median 58.37 ms, p95 60.72 ms, standard deviation 1.37 ms) across 200 timed runs following 20 warm-up passes. "
    "On the GPU, pre-processing required 0.31 ms, model inference 3.66 ms, and post-processing 0.96 ms. These figures do not describe the phone. "
    "The app can check up to 5 buffered frames per candidate, so verification time per candidate is up to 5 times the per-frame time. Because SpotHole "
    "runs vision confirmation on-demand only upon an accepted inertial trigger—rather than evaluating every camera frame continuously—it avoids battery "
    "depletion and thermal throttling while preserving real-time 67.7 Hz inertial sampling. TensorFlow Lite timing on the moto g64 5G was not measured."
)
body_parts.append(p_tag(res_p4, style="BodyText", jc="both"))

# VII. LIMITATIONS AND FUTURE SCOPE
body_parts.append(heading1("LIMITATIONS AND FUTURE SCOPE"))
body_parts.append(heading2("Limitations"))
lim_p1 = (
    "Evaluation. Every validation image contains a pothole, so we cannot measure false alarms on clean asphalt, and 242 near-duplicate pairs link "
    "the training and validation splits. Reported precision therefore likely exceeds what a rider will see. We also have no labeled field rides that "
    "compare sensor-only with sensor-plus-camera detection. Method. The detection thresholds are fixed values tuned for one handlebar-mounted setup. "
    "A different vehicle or a mount that damps vibration differently will need retuning. Camera. In synthetic tests, strong horizontal motion blur "
    "cut mAP@0.5 by 0.457 (a 56% relative drop) and a 40% brightness reduction cut it by 0.260. Real riding blur and night conditions were not tested."
)
body_parts.append(p_tag(lim_p1, style="BodyText", jc="both"))

body_parts.append(heading2("Future Scope"))
lim_p2 = (
    "Future work will collect labeled rides across riders, phones, and mounts, and add pothole-free and handlebar-view images to training. We also plan "
    "to replace the fixed thresholds with speed-adaptive or learned ones, measure battery use and on-device latency, evaluate INT8 quantization with "
    "NNAPI acceleration for real-time mobile NPU execution, and deploy spatial clustering (e.g., DBSCAN) to merge detections from several riders on a server."
)
body_parts.append(p_tag(lim_p2, style="BodyText", jc="both"))

# VIII. CONCLUSION
body_parts.append(heading1("CONCLUSION"))
concl_p = (
    "This paper presented SpotHole, an Android framework that detects potholes from a handlebar-mounted smartphone in two stages. The first stage "
    "screens inertial data using orientation-independent vertical acceleration, a voting rule over four threshold algorithms, and kinematic rejection "
    "filters. The second stage checks camera frames with a one-class YOLOv8n model (3.01M parameters) only when a candidate shock occurs. On 528 "
    "validation images, the model reached a precision of 0.825, a recall of 0.699, and an mAP@0.5 of 0.799. It tolerated JPEG compression (a drop "
    "of 0.018 in mAP@0.5) and mild noise, but strong motion blur and low brightness hurt it most. A perceptual-hash audit found 242 near-duplicate "
    "image pairs between training and validation data, so the reported metrics should be read as optimistic. The advantage of the camera stage over "
    "sensor-only detection has not yet been measured on labeled field rides. These rides, together with training on handlebar-view and pothole-free "
    "images, are the next step toward a reliable rider-level detector."
)
body_parts.append(p_tag(concl_p, style="BodyText", jc="both"))

# REFERENCES
body_parts.append(p_tag("References", style="Heading5", space_before=160, space_after=50))

refs_list = [
    "[1] Ministry of Road Transport and Highways (MoRTH), \"Road Accidents in India — 2022,\" Transport Research Wing, Government of India, New Delhi, Tech. Rep., Oct. 2023. [Online]. Available: https://morth.nic.in",
    "[2] J. Eriksson, L. Girod, B. Hull, R. Newton, S. Madden, and H. Balakrishnan, \"The Pothole Patrol: Using a mobile sensor network for road surface monitoring,\" in Proc. 6th ACM Int. Conf. on Mobile Systems, Applications, and Services (MobiSys), Breckenridge, CO, USA, Jun. 2008, pp. 29–39, doi: 10.1145/1378600.1378605.",
    "[3] P. Mohan, V. N. Padmanabhan, and R. Ramjee, \"Nericell: Rich monitoring of road and traffic conditions using mobile smartphones,\" in Proc. 6th ACM Conf. on Embedded Network Sensor Systems (SenSys), Raleigh, NC, USA, Nov. 2008, pp. 323–336, doi: 10.1145/1460412.1460444.",
    "[4] A. Mednis, G. Strazdins, R. Zviedris, G. Kanonirs, and L. Selavo, \"Real time pothole detection using Android smartphones with accelerometers,\" in Proc. 2nd Int. Workshop on Mobility in Wireless Sensor Networks (MobiSensor), Barcelona, Spain, Jun. 2011, pp. 1–6.",
    "[5] U. Bhatt, S. Mani, E. Xi, and J. Z. Kolter, \"Intelligent pothole detection and road condition assessment,\" in Bloomberg Data for Good Exchange Conf., Chicago, IL, USA, Sep. 2017, arXiv:1710.02595.",
    "[6] H. Maeda, Y. Sekimoto, T. Seto, T. Kashiyama, and H. Omata, \"Road damage detection and classification using deep neural networks with smartphone images,\" Computer-Aided Civil and Infrastructure Engineering, vol. 33, no. 12, pp. 1127–1141, Dec. 2018, doi: 10.1111/mice.12387.",
    "[7] D. Arya, H. Maeda, S. K. Ghosh, D. Toshniwal, and Y. Sekimoto, \"RDD2020: An annotated image dataset for automatic road damage detection using deep learning,\" Data in Brief, vol. 36, art. 107133, Jun. 2021, doi: 10.1016/j.dib.2021.107133.",
    "[8] J. Redmon, S. Divvala, R. Girshick, and A. Farhadi, \"You Only Look Once: Unified, real-time object detection,\" in Proc. IEEE Conf. on Computer Vision and Pattern Recognition (CVPR), Las Vegas, NV, USA, Jun. 2016, pp. 779–788, doi: 10.1109/CVPR.2016.91.",
    "[9] G. Jocher, A. Chaurasia, and J. Qiu, \"Ultralytics YOLOv8,\" version 8.4.173, 2023. [Online]. Available: https://github.com/ultralytics/ultralytics",
    "[10] S. Sattar, S. Li, and M. Chapman, \"Road surface monitoring using smartphone sensors: A review,\" Sensors, vol. 18, no. 11, art. 3845, Nov. 2018, doi: 10.3390/s18113845.",
    "[11] H. Xin, Y. Ye, X. Na, H. Hu, G. Wang, C. Wu, and S. Hu, \"Sustainable road pothole detection: A crowdsourcing based multi-sensors fusion approach,\" Sustainability, vol. 15, no. 8, art. 6610, Apr. 2023, doi: 10.3390/su15086610.",
    "[12] S. Silvister, D. Komandur, S. Kokate, A. Khochare, U. More, V. Musale, and A. Joshi, \"Deep learning approach to detect potholes in real-time using smartphone,\" in Proc. 2019 IEEE Pune Section Int. Conf. (PuneCon), Pune, India, Dec. 2019, pp. 1–5, doi: 10.1109/PuneCon46936.2019.9105741.",
    "[13] A. J. A. Villareal and J. M. Samaniego, \"An Android application for pothole detection and severity analysis using sensor fusion and deep learning,\" in Proc. Int. Conf. on Technology, Innovation, and Business Solutions (TIBS), 2024, pp. 45–52.",
    "[14] A. Ramesh, D. Nikam, V. N. Balachandran, L. Guo, R. Wang, L. Hu, G. Comert, and Y. Jia, \"Cloud-based collaborative road-damage monitoring with deep learning and smartphones,\" Sustainability, vol. 14, no. 14, art. 8682, Jul. 2022, doi: 10.3390/su14148682.",
    "[15] C. Bhambhania, \"Potholes Detection YOLOv8 Dataset,\" Kaggle Datasets, 2023. [Online]. Available: https://www.kaggle.com/datasets/chintanbhambhania/potholes-detection-yolov8",
    "[16] B. Dwyer, \"Pothole v1 Dataset,\" Roboflow Universe, Open Database License (ODbL v1.0), 2020. [Online]. Available: https://universe.roboflow.com/brad-dwyer/pothole-voxrl/dataset/1",
    "[17] T.-Y. Lin, M. Maire, S. Belongie, J. Hays, P. Perona, D. Ramanan, P. Dollár, and C. L. Zitnick, \"Microsoft COCO: Common objects in context,\" in Proc. 13th European Conf. on Computer Vision (ECCV), Zurich, Switzerland, Sep. 2014, pp. 740–755, doi: 10.1007/978-3-319-10602-1_48.",
    "[18] O. Kupyn, V. Budzan, M. Mykhailych, D. Mishkin, and J. Matas, \"DeblurGAN: Blind motion deblurring using conditional adversarial networks,\" in Proc. IEEE Conf. on Computer Vision and Pattern Recognition (CVPR), Salt Lake City, UT, USA, Jun. 2018, pp. 8183–8192, doi: 10.1109/CVPR.2018.00854."
]

for ref in refs_list:
    body_parts.append(p_tag(ref, style="BodyText", jc="both", space_before=20, space_after=35))

body_parts.append(sect_2col_p)

final_doc_xml = header_xml + "".join(body_parts) + end_sect_xml + "</w:body></w:document>"
files['word/document.xml'] = final_doc_xml.encode('utf-8')

with zipfile.ZipFile(DOCX_TEST, 'w', compression=zipfile.ZIP_DEFLATED) as zout:
    for name, data in files.items():
        zout.writestr(name, data)

print(f"Successfully created {DOCX_TEST}")
