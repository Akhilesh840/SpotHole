import zipfile
import shutil
import os
import re

TARGET_DOCX = "Passive Pothole Detection System using Smartphone Accelerometer and Gyroscope (1).docx"
BACKUP_DOCX = "Passive Pothole Detection System using Smartphone Accelerometer and Gyroscope (1)_BEFORE_FIG2.docx"
IMAGE_FILE = "fig_robustness.png"

# 1. Backup original docx
shutil.copy2(TARGET_DOCX, BACKUP_DOCX)
print(f"Created backup: {BACKUP_DOCX}")

# 2. Read existing files from zip
with zipfile.ZipFile(TARGET_DOCX, 'r') as zin:
    files = {name: zin.read(name) for name in zin.namelist()}

# 3. Add image to word/media/
with open(IMAGE_FILE, 'rb') as f:
    files['word/media/fig_robustness.png'] = f.read()

# 4. Update word/_rels/document.xml.rels
rels_str = files['word/_rels/document.xml.rels'].decode('utf-8')
rids = [int(m) for m in re.findall(r'Id="rId(\d+)"', rels_str)]
next_id = max(rids) + 1
new_rid = f"rId{next_id}"
print(f"Adding image relationship {new_rid}")

new_rel_entry = f'<Relationship Id="{new_rid}" Type="http://purl.oclc.org/ooxml/officeDocument/relationships/image" Target="media/fig_robustness.png"/>'
pos = rels_str.rfind('</Relationships>')
rels_str_updated = rels_str[:pos] + new_rel_entry + rels_str[pos:]
files['word/_rels/document.xml.rels'] = rels_str_updated.encode('utf-8')

# 5. Update word/document.xml
doc_str = files['word/document.xml'].decode('utf-8')

target_text = "threshold of 0.5.</w:t>"
target_pos = doc_str.find(target_text)
if target_pos == -1:
    raise ValueError("Target text not found in document.xml!")

p_end = doc_str.find('</w:p>', target_pos) + len('</w:p>')

# Dimensions matching column width:
# 3,086,100 EMUs width (~243 pt = 3.38 in), 1,700,836 EMUs height (~134 pt = 1.86 in)
cx = "3086100"
cy = "1595619"

fig_p = f'''<w:p>
  <w:pPr>
    <w:jc w:val="center"/>
    <w:spacing w:before="140" w:after="60"/>
  </w:pPr>
  <w:r>
    <w:rPr><w:noProof/></w:rPr>
    <w:drawing>
      <wp:inline distT="0" distB="0" distL="0" distR="0">
        <wp:extent cx="{cx}" cy="{cy}"/>
        <wp:effectExtent l="0" t="0" r="0" b="0"/>
        <wp:docPr id="2002" name="Fig. 2" descr="Fig. 2. mAP@0.5 of the 50-epoch model under synthetic image degradations (clean baseline 0.8145)"/>
        <wp:cNvGraphicFramePr>
          <a:graphicFrameLocks xmlns:a="http://purl.oclc.org/ooxml/drawingml/main" noChangeAspect="1"/>
        </wp:cNvGraphicFramePr>
        <a:graphic xmlns:a="http://purl.oclc.org/ooxml/drawingml/main">
          <a:graphicData uri="http://purl.oclc.org/ooxml/drawingml/picture">
            <pic:pic xmlns:pic="http://purl.oclc.org/ooxml/drawingml/picture">
              <pic:nvPicPr>
                <pic:cNvPr id="0" name="fig_robustness.png"/>
                <pic:cNvPicPr>
                  <a:picLocks noChangeAspect="1" noChangeArrowheads="1"/>
                </pic:cNvPicPr>
              </pic:nvPicPr>
              <pic:blipFill>
                <a:blip xmlns:r="http://purl.oclc.org/ooxml/officeDocument/relationships" r:embed="{new_rid}"/>
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

caption_p = '''<w:p>
  <w:pPr>
    <w:jc w:val="center"/>
    <w:spacing w:before="60" w:after="160"/>
    <w:rPr>
      <w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:cs="Times New Roman"/>
      <w:sz w:val="16"/>
      <w:szCs w:val="16"/>
    </w:rPr>
  </w:pPr>
  <w:r>
    <w:rPr>
      <w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:cs="Times New Roman"/>
      <w:b/>
      <w:bCs/>
      <w:sz w:val="16"/>
      <w:szCs w:val="16"/>
    </w:rPr>
    <w:t>Fig. 2.</w:t>
  </w:r>
  <w:r>
    <w:rPr>
      <w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:cs="Times New Roman"/>
      <w:sz w:val="16"/>
      <w:szCs w:val="16"/>
    </w:rPr>
    <w:t xml:space="preserve"> mAP@0.5 of the 50-epoch model under synthetic image degradations (clean baseline 0.8145).</w:t>
  </w:r>
</w:p>'''

doc_str_updated = doc_str[:p_end] + fig_p + caption_p + doc_str[p_end:]
files['word/document.xml'] = doc_str_updated.encode('utf-8')

# 6. Save back to target file
with zipfile.ZipFile(TARGET_DOCX, 'w', compression=zipfile.ZIP_DEFLATED) as zout:
    for name, data in files.items():
        zout.writestr(name, data)

print(f"Successfully updated {TARGET_DOCX} with Fig. 2 and its caption!")
