"""
Generate a clean, small, and simple UML Activity Diagram for Khadda
Compatible with Draw.io / diagrams.net
"""

import xml.etree.ElementTree as ET
import xml.dom.minidom

def create_simple_diagram():
    mxfile = ET.Element('mxfile', {
        'host': 'app.diagrams.net',
        'modified': '2026-10-16T00:00:00.000Z',
        'agent': 'Antigravity',
        'version': '24.7.17',
        'type': 'device'
    })
    
    diagram = ET.SubElement(mxfile, 'diagram', {
        'id': 'khadda-simple-activity',
        'name': 'Simple Activity Diagram'
    })
    
    model = ET.SubElement(diagram, 'mxGraphModel', {
        'dx': '1000',
        'dy': '1000',
        'grid': '1',
        'gridSize': '10',
        'guides': '1',
        'tooltips': '1',
        'connect': '1',
        'arrows': '1',
        'fold': '1',
        'page': '1',
        'pageScale': '1',
        'pageWidth': '850',
        'pageHeight': '1100',
        'math': '0',
        'shadow': '0',
        'background': '#ffffff'
    })
    
    root = ET.SubElement(model, 'root')
    ET.SubElement(root, 'mxCell', {'id': '0'})
    ET.SubElement(root, 'mxCell', {'id': '1', 'parent': '0'})
    
    # Title
    t_cell = ET.SubElement(root, 'mxCell', {
        'id': 'title',
        'value': '<b>UML Activity Diagram: Pothole Detection</b>',
        'parent': '1',
        'vertex': '1',
        'style': 'text;html=1;strokeColor=none;fillColor=none;fontColor=#1a237e;fontSize=18;align=center;verticalAlign=middle;'
    })
    ET.SubElement(t_cell, 'mxGeometry', {'x': '200', 'y': '20', 'width': '450', 'height': '40', 'as': 'geometry'})

    nodes = [
        # Start Node
        {'id': 'start', 'val': '', 'x': 410, 'y': 80, 'w': 30, 'h': 30,
         'style': 'ellipse;whiteSpace=wrap;html=1;fillColor=#000000;strokeColor=#000000;'},
        
        # 1. Start Detection
        {'id': 'a1', 'val': 'Start Detection Session', 'x': 325, 'y': 140, 'w': 200, 'h': 45,
         'style': 'rounded=1;whiteSpace=wrap;html=1;arcSize=24;fillColor=#ffffff;strokeColor=#333333;strokeWidth=1.5;fontSize=12;align=center;'},
        
        # 2. Collect Data
        {'id': 'a2', 'val': 'Collect Accelerometer &<br/>GPS Location Data', 'x': 325, 'y': 220, 'w': 200, 'h': 50,
         'style': 'rounded=1;whiteSpace=wrap;html=1;arcSize=24;fillColor=#ffffff;strokeColor=#333333;strokeWidth=1.5;fontSize=12;align=center;'},
        
        # 3. Process signal
        {'id': 'a3', 'val': 'Filter Noise & Compute<br/>Vertical Z-Axis Shock', 'x': 325, 'y': 305, 'w': 200, 'h': 50,
         'style': 'rounded=1;whiteSpace=wrap;html=1;arcSize=24;fillColor=#ffffff;strokeColor=#333333;strokeWidth=1.5;fontSize=12;align=center;'},
        
        # Decision 1: Anomaly
        {'id': 'd1', 'val': 'Pothole<br/>Detected?', 'x': 355, 'y': 390, 'w': 140, 'h': 70,
         'style': 'rhombus;whiteSpace=wrap;html=1;fillColor=#fff9c4;strokeColor=#f57f17;strokeWidth=1.5;fontSize=12;align=center;'},
        
        # Decision 2: False Positive Filter
        {'id': 'd2', 'val': 'Passes Filter?<br/>(Speed &gt; 10 km/h<br/>&amp; Not Turning)', 'x': 350, 'y': 500, 'w': 150, 'h': 75,
         'style': 'rhombus;whiteSpace=wrap;html=1;fillColor=#fff9c4;strokeColor=#f57f17;strokeWidth=1.5;fontSize=11;align=center;'},
        
        # 4. Record Event
        {'id': 'a4', 'val': 'Record Pothole Event<br/>(Tag GPS &amp; Severity)', 'x': 325, 'y': 615, 'w': 200, 'h': 50,
         'style': 'rounded=1;whiteSpace=wrap;html=1;arcSize=24;fillColor=#e8f5e9;strokeColor=#2e7d32;strokeWidth=1.5;fontSize=12;align=center;'},
        
        # 5. Local Database Store
        {'id': 'db1', 'val': 'Local SQLite Database', 'x': 345, 'y': 700, 'w': 160, 'h': 45,
         'style': 'shape=cylinder3;whiteSpace=wrap;html=1;boundedLbl=1;backgroundOutline=1;size=10;fillColor=#f5f5f5;strokeColor=#616161;fontSize=11;align=center;'},
        
        # Decision 3: Stop Session?
        {'id': 'd3', 'val': 'User Stops<br/>Detection?', 'x': 355, 'y': 785, 'w': 140, 'h': 70,
         'style': 'rhombus;whiteSpace=wrap;html=1;fillColor=#fff9c4;strokeColor=#f57f17;strokeWidth=1.5;fontSize=12;align=center;'},
        
        # 6. Stop and Report
        {'id': 'a5', 'val': 'Stop Sensors &amp;<br/>Display Summary Report', 'x': 325, 'y': 895, 'w': 200, 'h': 50,
         'style': 'rounded=1;whiteSpace=wrap;html=1;arcSize=24;fillColor=#ffffff;strokeColor=#333333;strokeWidth=1.5;fontSize=12;align=center;'},
        
        # Final Node (Bullseye)
        {'id': 'endOuter', 'val': '', 'x': 407, 'y': 980, 'w': 36, 'h': 36,
         'style': 'ellipse;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#000000;strokeWidth=2;'},
        {'id': 'endInner', 'val': '', 'x': 414, 'y': 987, 'w': 22, 'h': 22,
         'style': 'ellipse;whiteSpace=wrap;html=1;fillColor=#000000;strokeColor=#000000;'}
    ]

    edges = [
        {'id': 'e1', 'src': 'start', 'tgt': 'a1', 'val': ''},
        {'id': 'e2', 'src': 'a1', 'tgt': 'a2', 'val': ''},
        {'id': 'e3', 'src': 'a2', 'tgt': 'a3', 'val': ''},
        {'id': 'e4', 'src': 'a3', 'tgt': 'd1', 'val': ''},
        
        # Anomaly = Yes -> Filter
        {'id': 'e5', 'src': 'd1', 'tgt': 'd2', 'val': '[Yes]'},
        # Anomaly = No -> Loop to d3
        {'id': 'e6', 'src': 'd1', 'tgt': 'd3', 'val': '[No]', 'style': 'edgeStyle=orthogonalEdgeStyle;entryX=1;entryY=0.5;exitX=1;exitY=0.5;'},
        
        # Filter = Yes -> Record
        {'id': 'e7', 'src': 'd2', 'tgt': 'a4', 'val': '[Yes]'},
        # Filter = No -> Loop to d3
        {'id': 'e8', 'src': 'd2', 'tgt': 'd3', 'val': '[No: False Positive]', 'style': 'edgeStyle=orthogonalEdgeStyle;entryX=0;entryY=0.5;exitX=0;exitY=0.5;'},
        
        # Record -> Save DB
        {'id': 'e9', 'src': 'a4', 'tgt': 'db1', 'val': ''},
        {'id': 'e10', 'src': 'db1', 'tgt': 'd3', 'val': ''},
        
        # Stop = No -> Loop back to Collect Data (a2)
        {'id': 'e11', 'src': 'd3', 'tgt': 'a2', 'val': '[No: Continue]', 'style': 'edgeStyle=orthogonalEdgeStyle;entryX=0;entryY=0.5;exitX=0;exitY=0.5;'},
        
        # Stop = Yes -> Stop & Report -> End
        {'id': 'e12', 'src': 'd3', 'tgt': 'a5', 'val': '[Yes]'},
        {'id': 'e13', 'src': 'a5', 'tgt': 'endOuter', 'val': ''}
    ]

    for n in nodes:
        c = ET.SubElement(root, 'mxCell', {
            'id': n['id'], 'value': n['val'], 'parent': '1', 'vertex': '1', 'style': n['style']
        })
        ET.SubElement(c, 'mxGeometry', {
            'x': str(n['x']), 'y': str(n['y']), 'width': str(n['w']), 'height': str(n['h']), 'as': 'geometry'
        })

    for e in edges:
        style = e.get('style', 'edgeStyle=orthogonalEdgeStyle;') + "rounded=1;html=1;strokeColor=#333333;strokeWidth=1.5;endArrow=classic;endFill=1;fontSize=11;"
        c = ET.SubElement(root, 'mxCell', {
            'id': e['id'], 'value': e['val'], 'parent': '1', 'edge': '1',
            'source': e['src'], 'target': e['tgt'], 'style': style
        })
        ET.SubElement(c, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    xml_str = ET.tostring(mxfile, encoding='utf-8')
    pretty_xml = xml.dom.minidom.parseString(xml_str).toprettyxml(indent='  ')
    
    with open('activity diagram.drawio', 'w', encoding='utf-8') as f:
        f.write(pretty_xml)
    print("Updated activity diagram.drawio with simple clean diagram")

if __name__ == '__main__':
    create_simple_diagram()
