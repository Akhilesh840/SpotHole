"""
Generate a Draw.io XML Use Case Diagram that matches the user's reference image EXACTLY:
- Title at the top: 'Use Case diagram of spothole Detection System'
- Left side: 3 stick-figure actors with UML generalization inheritance arrows pointing to the base actor
- Center: Rounded green subsystem container (<<Subsystem>> spothole Detection System) with vertically stacked use case ovals
- Vertical dashed arrows with <<include>> inside the subsystem
- Right side: 4 stick-figure actors (services and external authorities)
- Solid association lines connecting actors to the corresponding use cases
"""
import xml.etree.ElementTree as ET
import xml.dom.minidom

def generate_exact_diagram():
    mxfile = ET.Element('mxfile', {
        'host': 'app.diagrams.net',
        'modified': '2026-10-20T00:00:00.000Z',
        'agent': 'Antigravity',
        'version': '24.7.17',
        'type': 'device'
    })
    
    diagram = ET.SubElement(mxfile, 'diagram', {
        'id': 'spothole-use-case-exact',
        'name': 'Use Case Diagram'
    })
    
    model = ET.SubElement(diagram, 'mxGraphModel', {
        'dx': '1300', 'dy': '850', 'grid': '1', 'gridSize': '10',
        'guides': '1', 'tooltips': '1', 'connect': '1', 'arrows': '1',
        'page': '1', 'pageScale': '1', 'pageWidth': '1100', 'pageHeight': '750',
        'background': '#ffffff'
    })
    
    root = ET.SubElement(model, 'root')
    ET.SubElement(root, 'mxCell', {'id': '0'})
    ET.SubElement(root, 'mxCell', {'id': '1', 'parent': '0'})
    
    # 1. Title at Top
    title = ET.SubElement(root, 'mxCell', {
        'id': 'title',
        'value': '<b>Use Case diagram of spothole Detection System</b>',
        'parent': '1', 'vertex': '1',
        'style': 'text;html=1;strokeColor=none;fillColor=none;fontColor=#111111;fontSize=20;align=center;verticalAlign=middle;fontFamily=Helvetica;'
    })
    ET.SubElement(title, 'mxGeometry', {'x': '250', 'y': '25', 'width': '600', 'height': '40', 'as': 'geometry'})
    
    # 2. Central Subsystem Box (Soft Green rounded box like example)
    sub = ET.SubElement(root, 'mxCell', {
        'id': 'subsystem',
        'value': '&lt;&lt;Subsystem&gt;&gt;<br><b>spothole System</b>',
        'parent': '1', 'vertex': '1',
        'style': 'rounded=1;arcSize=14;whiteSpace=wrap;html=1;strokeColor=#4d7c0f;fillColor=#dcfce7;strokeWidth=1.5;fontSize=14;verticalAlign=top;spacingTop=12;fontColor=#14532d;fontFamily=Helvetica;'
    })
    ET.SubElement(sub, 'mxGeometry', {'x': '410', 'y': '90', 'width': '280', 'height': '530', 'as': 'geometry'})

    # 3. Actors Left
    actors_left = [
        {'id': 'act_driver', 'val': 'Commuter /<br>Driver', 'x': 80, 'y': 280, 'w': 55, 'h': 85},
        {'id': 'act_rider', 'val': 'Motorcycle<br>Rider', 'x': 230, 'y': 160, 'w': 55, 'h': 85},
        {'id': 'act_surveyor', 'val': 'Road<br>Surveyor', 'x': 230, 'y': 420, 'w': 55, 'h': 85},
    ]

    # 4. Actors Right (4 actors like example)
    actors_right = [
        {'id': 'act_sensor', 'val': '&lt;&lt;Service&gt;&gt;<br>SensorManager', 'x': 810, 'y': 110, 'w': 55, 'h': 85},
        {'id': 'act_camera', 'val': '&lt;&lt;Service&gt;&gt;<br>CameraX Engine', 'x': 810, 'y': 240, 'w': 55, 'h': 85},
        {'id': 'act_gps', 'val': 'GPS Location<br>Provider', 'x': 810, 'y': 380, 'w': 55, 'h': 85},
        {'id': 'act_authority', 'val': 'Road Maintenance<br>Authority', 'x': 810, 'y': 510, 'w': 55, 'h': 85},
    ]

    for a in actors_left + actors_right:
        c = ET.SubElement(root, 'mxCell', {
            'id': a['id'], 'value': a['val'], 'parent': '1', 'vertex': '1',
            'style': 'shape=umlActor;verticalLabelPosition=bottom;verticalAlign=top;html=1;outlineConnect=0;strokeColor=#27272a;fillColor=#f4f4f5;strokeWidth=1.5;fontColor=#18181b;fontSize=12;fontFamily=Helvetica;'
        })
        ET.SubElement(c, 'mxGeometry', {'x': str(a['x']), 'y': str(a['y']), 'width': str(a['w']), 'height': str(a['h']), 'as': 'geometry'})

    # 5. 4 Use Cases inside Subsystem (Ellipses matching example)
    ucs = [
        {'id': 'uc1', 'val': 'Start Road<br>Monitoring', 'x': 465, 'y': 165, 'w': 170, 'h': 60},
        {'id': 'uc2', 'val': 'Detect Road<br>Anomaly', 'x': 465, 'y': 275, 'w': 170, 'h': 60},
        {'id': 'uc3', 'val': 'Verify with<br>YOLOv8', 'x': 465, 'y': 400, 'w': 170, 'h': 60},
        {'id': 'uc4', 'val': 'Record Pothole<br>Event', 'x': 465, 'y': 515, 'w': 170, 'h': 60},
    ]

    for u in ucs:
        c = ET.SubElement(root, 'mxCell', {
            'id': u['id'], 'value': u['val'], 'parent': '1', 'vertex': '1',
            'style': 'ellipse;whiteSpace=wrap;html=1;strokeColor=#15803d;fillColor=#ffffff;strokeWidth=1.5;fontColor=#14532d;fontSize=12;fontFamily=Helvetica;align=center;'
        })
        ET.SubElement(c, 'mxGeometry', {'x': str(u['x']), 'y': str(u['y']), 'width': str(u['w']), 'height': str(u['h']), 'as': 'geometry'})

    # 6. Generalization Arrows on Left (--|> hollow block arrow pointing to general driver)
    gen_edges = [
        {'id': 'g1', 'src': 'act_rider', 'tgt': 'act_driver'},
        {'id': 'g2', 'src': 'act_surveyor', 'tgt': 'act_driver'},
    ]
    for g in gen_edges:
        c = ET.SubElement(root, 'mxCell', {
            'id': g['id'], 'parent': '1', 'edge': '1', 'source': g['src'], 'target': g['tgt'],
            'style': 'endArrow=block;endSize=10;endFill=0;html=1;strokeColor=#18181b;strokeWidth=1.3;'
        })
        ET.SubElement(c, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    # 7. Internal <<include>> Dashed Arrows inside Subsystem
    includes = [
        ('uc2', 'uc1', 'inc1'),
        ('uc2', 'uc3', 'inc2'),
    ]
    for src, tgt, eid in includes:
        c = ET.SubElement(root, 'mxCell', {
            'id': eid, 'value': '&lt;&lt;include&gt;&gt;', 'parent': '1', 'edge': '1',
            'source': src, 'target': tgt,
            'style': 'endArrow=open;dashed=1;html=1;strokeColor=#15803d;strokeWidth=1.2;fontColor=#166534;fontSize=11;labelBackgroundColor=#ffffff;fontFamily=Helvetica;'
        })
        ET.SubElement(c, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    # 8. Association lines (Solid lines, no arrows like the example)
    assocs = [
        # Left actor connections
        ('act_rider', 'uc1'),
        ('act_rider', 'uc2'),
        ('act_surveyor', 'uc1'),
        ('act_surveyor', 'uc4'),
        ('act_driver', 'uc4'),
        
        # Right actor connections
        ('uc1', 'act_sensor'),
        ('uc2', 'act_sensor'),
        ('uc2', 'act_camera'),
        ('uc3', 'act_camera'),
        ('uc3', 'act_gps'),
        ('uc4', 'act_gps'),
        ('uc4', 'act_authority'),
    ]
    for idx, (src, tgt) in enumerate(assocs):
        c = ET.SubElement(root, 'mxCell', {
            'id': f'edge_{idx}', 'parent': '1', 'edge': '1', 'source': src, 'target': tgt,
            'style': 'endArrow=none;html=1;strokeColor=#27272a;strokeWidth=1.2;'
        })
        ET.SubElement(c, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    xml_str = ET.tostring(mxfile, encoding='utf-8')
    pretty_xml = xml.dom.minidom.parseString(xml_str).toprettyxml(indent='  ')
    
    with open('spothole_use_case_exact.drawio', 'w', encoding='utf-8') as f:
        f.write(pretty_xml)
    print("Generated spothole_use_case_exact.drawio successfully!")

if __name__ == '__main__':
    generate_exact_diagram()
