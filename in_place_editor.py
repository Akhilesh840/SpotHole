"""
in_place_editor.py
Edits the original document in-place, preserving 100% of Word OOXML structure,
styles, table grids, section breaks, and two-column formatting.
"""

import zipfile
import re
import xml.etree.ElementTree as ET

DOCX_IN = "Passive Pothole Detection System using Smartphone Accelerometer and Gyroscope (1)_UNTOUCHED_BACKUP.docx"
DOCX_OUT = "Passive Pothole Detection System using Smartphone Accelerometer and Gyroscope (1).docx"
FIG_ARCH = "figures/fig_arch.png"
FIG_ROB = "figures/fig_robustness.png"

with zipfile.ZipFile(DOCX_IN, 'r') as zin:
    files = {name: zin.read(name) for name in zin.namelist()}

# 1. Add images
with open(FIG_ARCH, 'rb') as f:
    files['word/media/fig_arch.png'] = f.read()
with open(FIG_ROB, 'rb') as f:
    files['word/media/fig_robustness.png'] = f.read()

# 2. Update rels
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
rels_str_updated = rels_str[:pos_close] + new_rels + rels_str[pos_close:]
files['word/_rels/document.xml.rels'] = rels_str_updated.encode('utf-8')
print(f"Added relationships: {rid_arch} (fig_arch), {rid_rob} (fig_rob)")

# 3. Parse document.xml using DOM
# Register exact namespaces to avoid any prefix mangling
W_NS = "http://purl.oclc.org/ooxml/wordprocessingml/main"
WP_NS = "http://purl.oclc.org/ooxml/drawingml/wordprocessingDrawing"
A_NS = "http://purl.oclc.org/ooxml/drawingml/main"
PIC_NS = "http://purl.oclc.org/ooxml/drawingml/picture"
R_NS = "http://purl.oclc.org/ooxml/officeDocument/relationships"

namespaces = {
    'w': W_NS,
    'wp': WP_NS,
    'a': A_NS,
    'pic': PIC_NS,
    'r': R_NS,
    'w14': "http://schemas.microsoft.com/office/word/2010/wordml",
    'w15': "http://schemas.microsoft.com/office/word/2012/wordml",
    'mc': "http://schemas.openxmlformats.org/markup-compatibility/2006"
}

for prefix, uri in namespaces.items():
    ET.register_namespace(prefix, uri)

tree = ET.fromstring(files['word/document.xml'])
body = tree.find('w:body', namespaces)

print("Body has children:", len(body))

# Helpers to manipulate elements
def set_p_text(p, text, style=None, bold=False, italic=False):
    # Keep pPr if present
    pPr = p.find('w:pPr', namespaces)
    if pPr is None:
        pPr = ET.Element(f'{{{W_NS}}}pPr')
        p.insert(0, pPr)
    
    if style:
        pStyle = pPr.find('w:pStyle', namespaces)
        if pStyle is None:
            pStyle = ET.Element(f'{{{W_NS}}}pStyle')
            pPr.insert(0, pStyle)
        pStyle.attrib[f'{{{W_NS}}}val'] = style
    
    # Remove existing runs and drawings
    for r in list(p.findall('w:r', namespaces)):
        p.remove(r)
    
    # Add new run
    new_r = ET.Element(f'{{{W_NS}}}r')
    rPr = ET.Element(f'{{{W_NS}}}rPr')
    if bold:
        rPr.append(ET.Element(f'{{{W_NS}}}b'))
        rPr.append(ET.Element(f'{{{W_NS}}}bCs'))
    if italic:
        rPr.append(ET.Element(f'{{{W_NS}}}i'))
        rPr.append(ET.Element(f'{{{W_NS}}}iCs'))
    if len(rPr) > 0:
        new_r.append(rPr)
    
    t_el = ET.Element(f'{{{W_NS}}}t')
    t_el.attrib['{http://www.w3.org/XML/1998/namespace}space'] = 'preserve'
    t_el.text = text
    new_r.append(t_el)
    p.append(new_r)

def create_p(text, style="BodyText", bold=False, italic=False):
    p = ET.Element(f'{{{W_NS}}}p')
    set_p_text(p, text, style=style, bold=bold, italic=italic)
    return p

def create_fig_inline(rid, name, descr, cx, cy, cap_id, caption_text):
    # Image paragraph
    fig_p = ET.Element(f'{{{W_NS}}}p')
    pPr = ET.SubElement(fig_p, f'{{{W_NS}}}pPr')
    jc = ET.SubElement(pPr, f'{{{W_NS}}}jc')
    jc.attrib[f'{{{W_NS}}}val'] = 'center'
    sp = ET.SubElement(pPr, f'{{{W_NS}}}spacing')
    sp.attrib[f'{{{W_NS}}}before'] = '140'
    sp.attrib[f'{{{W_NS}}}after'] = '60'
    
    r = ET.SubElement(fig_p, f'{{{W_NS}}}r')
    rPr = ET.SubElement(r, f'{{{W_NS}}}rPr')
    ET.SubElement(rPr, f'{{{W_NS}}}noProof')
    drawing = ET.SubElement(r, f'{{{W_NS}}}drawing')
    inline = ET.SubElement(drawing, f'{{{WP_NS}}}inline', {
        'distT': '0', 'distB': '0', 'distL': '0', 'distR': '0'
    })
    extent = ET.SubElement(inline, f'{{{WP_NS}}}extent', {'cx': str(cx), 'cy': str(cy)})
    ET.SubElement(inline, f'{{{WP_NS}}}effectExtent', {'l': '0', 't': '0', 'r': '0', 'b': '0'})
    ET.SubElement(inline, f'{{{WP_NS}}}docPr', {'id': '2001', 'name': name, 'descr': descr})
    cNv = ET.SubElement(inline, f'{{{WP_NS}}}cNvGraphicFramePr')
    ET.SubElement(cNv, f'{{{A_NS}}}graphicFrameLocks', {'noChangeAspect': '1'})
    graphic = ET.SubElement(inline, f'{{{A_NS}}}graphic')
    gData = ET.SubElement(graphic, f'{{{A_NS}}}graphicData', {'uri': 'http://purl.oclc.org/ooxml/drawingml/picture'})
    pic = ET.SubElement(gData, f'{{{PIC_NS}}}pic')
    nvPicPr = ET.SubElement(pic, f'{{{PIC_NS}}}nvPicPr')
    ET.SubElement(nvPicPr, f'{{{PIC_NS}}}cNvPr', {'id': '0', 'name': name})
    cNvPicPr = ET.SubElement(nvPicPr, f'{{{PIC_NS}}}cNvPicPr')
    ET.SubElement(cNvPicPr, f'{{{A_NS}}}picLocks', {'noChangeAspect': '1', 'noChangeArrowheads': '1'})
    blipFill = ET.SubElement(pic, f'{{{PIC_NS}}}blipFill')
    ET.SubElement(blipFill, f'{{{A_NS}}}blip', {f'{{{R_NS}}}embed': rid})
    stretch = ET.SubElement(blipFill, f'{{{A_NS}}}stretch')
    ET.SubElement(stretch, f'{{{A_NS}}}fillRect')
    spPr = ET.SubElement(pic, f'{{{PIC_NS}}}spPr')
    xfrm = ET.SubElement(spPr, f'{{{A_NS}}}xfrm')
    ET.SubElement(xfrm, f'{{{A_NS}}}off', {'x': '0', 'y': '0'})
    ET.SubElement(xfrm, f'{{{A_NS}}}ext', {'cx': str(cx), 'cy': str(cy)})
    prst = ET.SubElement(spPr, f'{{{A_NS}}}prstGeom', {'prst': 'rect'})
    ET.SubElement(prst, f'{{{A_NS}}}avLst')
    
    # Caption paragraph
    cap_p = ET.Element(f'{{{W_NS}}}p')
    pPr_c = ET.SubElement(cap_p, f'{{{W_NS}}}pPr')
    ET.SubElement(pPr_c, f'{{{W_NS}}}pStyle', {f'{{{W_NS}}}val': 'figurecaption'})
    jc_c = ET.SubElement(pPr_c, f'{{{W_NS}}}jc', {f'{{{W_NS}}}val': 'center'})
    sp_c = ET.SubElement(pPr_c, f'{{{W_NS}}}spacing', {f'{{{W_NS}}}before': '60', f'{{{W_NS}}}after': '140'})
    
    r_bold = ET.SubElement(cap_p, f'{{{W_NS}}}r')
    rPr_b = ET.SubElement(r_bold, f'{{{W_NS}}}rPr')
    ET.SubElement(rPr_b, f'{{{W_NS}}}b')
    ET.SubElement(rPr_b, f'{{{W_NS}}}bCs')
    t_b = ET.SubElement(r_bold, f'{{{W_NS}}}t')
    t_b.text = f"{cap_id}."
    
    r_txt = ET.SubElement(cap_p, f'{{{W_NS}}}r')
    t_txt = ET.SubElement(r_txt, f'{{{W_NS}}}t', {'{http://www.w3.org/XML/1998/namespace}space': 'preserve'})
    t_txt.text = f" {caption_text}"
    
    return fig_p, cap_p

def create_table_head(cap_id, cap_text):
    p = ET.Element(f'{{{W_NS}}}p')
    pPr = ET.SubElement(p, f'{{{W_NS}}}pPr')
    ET.SubElement(pPr, f'{{{W_NS}}}pStyle', {f'{{{W_NS}}}val': 'tablehead'})
    ET.SubElement(pPr, f'{{{W_NS}}}jc', {f'{{{W_NS}}}val': 'center'})
    ET.SubElement(pPr, f'{{{W_NS}}}spacing', {f'{{{W_NS}}}before': '140', f'{{{W_NS}}}after': '60'})
    
    r_b = ET.SubElement(p, f'{{{W_NS}}}r')
    rPr_b = ET.SubElement(r_b, f'{{{W_NS}}}rPr')
    ET.SubElement(rPr_b, f'{{{W_NS}}}b')
    ET.SubElement(rPr_b, f'{{{W_NS}}}bCs')
    t_b = ET.SubElement(r_b, f'{{{W_NS}}}t')
    t_b.text = f"{cap_id}."
    
    r_t = ET.SubElement(p, f'{{{W_NS}}}r')
    t_t = ET.SubElement(r_t, f'{{{W_NS}}}t', {'{http://www.w3.org/XML/1998/namespace}space': 'preserve'})
    t_t.text = f" {cap_text.upper()}"
    return p

print("Loaded XML structure and defined helpers successfully.")
