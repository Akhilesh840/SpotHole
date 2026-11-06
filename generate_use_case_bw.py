"""
Generate a 100% black and white UML Use Case Diagram for spothole matching the reference image:
- Native UML stick figure actors on the left with Generalization arrows (--|>)
- Native rounded subsystem boundary in the center with <<Subsystem>> and use case ellipses
- Native dashed <<include>> arrows inside the subsystem
- Native <<Service>> stick figure actors on the right
- Pure black and white (white fill, black strokes, black text)
"""
import xml.etree.ElementTree as ET
import xml.dom.minidom

def generate_use_case_xml():
    mxfile = ET.Element('mxfile', {
        'host': 'app.diagrams.net',
        'modified': '2026-10-20T00:00:00.000Z',
        'agent': 'Antigravity',
        'version': '24.7.17',
        'type': 'device'
    })
    
    diagram = ET.SubElement(mxfile, 'diagram', {
        'id': 'spothole-use-case-bw',
        'name': 'Use Case Diagram'
    })
    
    model = ET.SubElement(diagram, 'mxGraphModel', {
        'dx': '1200', 'dy': '900', 'grid': '1', 'gridSize': '10',
        'guides': '1', 'tooltips': '1', 'connect': '1', 'arrows': '1',
        'page': '1', 'pageScale': '1', 'pageWidth': '1100', 'pageHeight': '850',
        'background': '#ffffff'
    })
    
    root = ET.SubElement(model, 'root')
    ET.SubElement(root, 'mxCell', {'id': '0'})
    ET.SubElement(root, 'mxCell', {'id': '1', 'parent': '0'})
    
    # Title
    t = ET.SubElement(root, 'mxCell', {
        'id': 'title',
        'value': '<b>Use Case diagram of spothole Pothole Detection System</b>',
        'parent': '1', 'vertex': '1',
        'style': 'text;html=1;strokeColor=none;fillColor=none;fontColor=#000000;fontSize=18;align=center;verticalAlign=middle;'
    })
    ET.SubElement(t, 'mxGeometry', {'x': '250', 'y': '20', 'width': '600', 'height': '40', 'as': 'geometry'})
    
    # Subsystem Boundary (Center)
    sub = ET.SubElement(root, 'mxCell', {
        'id': 'subsystem',
        'value': '&lt;&lt;Subsystem&gt;&gt;<br><b>spothole Detection System</b>',
        'parent': '1', 'vertex': '1',
        'style': 'rounded=1;whiteSpace=wrap;html=1;strokeColor=#000000;fillColor=#ffffff;strokeWidth=1.5;fontSize=14;verticalAlign=top;spacingTop=10;fontColor=#000000;'
    })
    ET.SubElement(sub, 'mxGeometry', {'x': '380', 'y': '90', 'width': '340', 'height': '540', 'as': 'geometry'})

    # Actors Left
    actors_left = [
        {'id': 'act_driver', 'val': 'Commuter /<br>Driver', 'x': 50, 'y': 250, 'w': 70, 'h': 90},
        {'id': 'act_rider', 'val': 'Motorcycle<br>Rider', 'x': 200, 'y': 150, 'w': 70, 'h': 90},
        {'id': 'act_surveyor', 'val': 'Road<br>Surveyor', 'x': 200, 'y': 380, 'w': 70, 'h': 90},
    ]

    # Actors Right
    actors_right = [
        {'id': 'act_gps', 'val': '&lt;&lt;Service&gt;&gt;<br>GPS Location', 'x': 860, 'y': 130, 'w': 70, 'h': 90},
        {'id': 'act_camera', 'val': '&lt;&lt;Service&gt;&gt;<br>CameraX Engine', 'x': 860, 'y': 260, 'w': 70, 'h': 90},
        {'id': 'act_authority', 'val': 'Road Maintenance<br>Authority', 'x': 860, 'y': 440, 'w': 70, 'h': 90},
    ]

    for a in actors_left + actors_right:
        c = ET.SubElement(root, 'mxCell', {
            'id': a['id'], 'value': a['val'], 'parent': '1', 'vertex': '1',
            'style': 'shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;outlineConnect=0;strokeColor=#000000;fillColor=#ffffff;strokeWidth=1.5;fontColor=#000000;fontSize=12;'
        })
        ET.SubElement(c, 'mxGeometry', {'x': str(a['x']), 'y': str(a['y']), 'width': str(a['w']), 'height': str(a['h']), 'as': 'geometry'})

    # Use cases inside subsystem (Ellipses)
    ucs = [
        {'id': 'uc1', 'val': 'Start Road Monitoring', 'x': 435, 'y': 160, 'w': 230, 'h': 55},
        {'id': 'uc2', 'val': 'Detect Road Anomaly', 'x': 435, 'y': 250, 'w': 230, 'h': 55},
        {'id': 'uc3', 'val': 'Verify with YOLOv8 Vision', 'x': 435, 'y': 345, 'w': 230, 'h': 55},
        {'id': 'uc4', 'val': 'Record Pothole Event', 'x': 435, 'y': 440, 'w': 230, 'h': 55},
        {'id': 'uc5', 'val': 'View Pothole Hazard Map', 'x': 435, 'y': 535, 'w': 230, 'h': 55},
    ]

    for u in ucs:
        c = ET.SubElement(root, 'mxCell', {
            'id': u['id'], 'value': u['val'], 'parent': '1', 'vertex': '1',
            'style': 'ellipse;whiteSpace=wrap;html=1;strokeColor=#000000;fillColor=#ffffff;strokeWidth=1.5;fontColor=#000000;fontSize=12;'
        })
        ET.SubElement(c, 'mxGeometry', {'x': str(u['x']), 'y': str(u['y']), 'width': str(u['w']), 'height': str(u['h']), 'as': 'geometry'})

    # Generalization arrows (--|>)
    gen_edges = [
        {'id': 'g1', 'src': 'act_rider', 'tgt': 'act_driver'},
        {'id': 'g2', 'src': 'act_surveyor', 'tgt': 'act_driver'},
    ]
    for g in gen_edges:
        c = ET.SubElement(root, 'mxCell', {
            'id': g['id'], 'parent': '1', 'edge': '1', 'source': g['src'], 'target': g['tgt'],
            'style': 'endArrow=block;endSize=12;endFill=0;html=1;strokeColor=#000000;strokeWidth=1.5;'
        })
        ET.SubElement(c, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    # Associations (Solid lines, no arrows)
    assocs = [
        ('act_rider', 'uc1'), ('act_rider', 'uc2'),
        ('act_surveyor', 'uc1'), ('act_surveyor', 'uc2'),
        ('act_driver', 'uc5'),
        ('uc1', 'act_gps'),
        ('uc4', 'act_gps'),
        ('uc3', 'act_camera'),
        ('uc4', 'act_authority'),
        ('uc5', 'act_authority'),
    ]
    for idx, (src, tgt) in enumerate(assocs):
        c = ET.SubElement(root, 'mxCell', {
            'id': f'assoc_{idx}', 'parent': '1', 'edge': '1', 'source': src, 'target': tgt,
            'style': 'endArrow=none;html=1;strokeColor=#000000;strokeWidth=1.2;'
        })
        ET.SubElement(c, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    # <<include>> dependencies (Dashed arrows)
    includes = [
        ('uc2', 'uc3', 'inc1'),
        ('uc2', 'uc4', 'inc2')
    ]
    for src, tgt, eid in includes:
        c = ET.SubElement(root, 'mxCell', {
            'id': eid, 'value': '&lt;&lt;include&gt;&gt;', 'parent': '1', 'edge': '1',
            'source': src, 'target': tgt,
            'style': 'endArrow=open;dashed=1;html=1;strokeColor=#000000;strokeWidth=1.2;fontColor=#000000;fontSize=11;labelBackgroundColor=#ffffff;'
        })
        ET.SubElement(c, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    xml_str = ET.tostring(mxfile, encoding='utf-8')
    pretty_xml = xml.dom.minidom.parseString(xml_str).toprettyxml(indent='  ')
    
    with open('spothole_use_case_bw.drawio', 'w', encoding='utf-8') as f:
        f.write(pretty_xml)
    print("Created spothole_use_case_bw.drawio successfully!")

if __name__ == '__main__':
    generate_use_case_xml()
