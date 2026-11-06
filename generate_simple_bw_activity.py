"""
Update activity diagram.drawio to a small, simple, black & white UML Activity Diagram.
Zero colors used - strictly monochrome/B&W.
"""

import xml.etree.ElementTree as ET
import xml.dom.minidom

def create_simple_bw_diagram():
    mxfile = ET.Element('mxfile', {
        'host': 'app.diagrams.net',
        'modified': '2026-10-16T00:00:00.000Z',
        'agent': 'Antigravity',
        'version': '24.7.17',
        'type': 'device'
    })
    
    diagram = ET.SubElement(mxfile, 'diagram', {
        'id': 'khadda-simple-bw-activity',
        'name': 'Page-1'
    })
    
    model = ET.SubElement(diagram, 'mxGraphModel', {
        'dx': '800',
        'dy': '900',
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
    
    # Nodes (All Black & White: fillColor=#ffffff, strokeColor=#000000, fontColor=#000000)
    nodes = [
        # Start Node (Solid Black Circle)
        {'id': 'start', 'val': '', 'x': 346, 'y': 40, 'w': 28, 'h': 28,
         'style': 'ellipse;whiteSpace=wrap;html=1;fillColor=#000000;strokeColor=#000000;'},
        
        # Action 1: Start Session
        {'id': 'a1', 'val': 'Start Detection Session', 'x': 270, 'y': 100, 'w': 180, 'h': 42,
         'style': 'rounded=1;whiteSpace=wrap;html=1;arcSize=24;fillColor=#ffffff;strokeColor=#000000;strokeWidth=1.5;fontSize=12;fontColor=#000000;align=center;'},
        
        # Action 2: Read Data
        {'id': 'a2', 'val': 'Read Accelerometer &<br/>GPS Data', 'x': 270, 'y': 175, 'w': 180, 'h': 45,
         'style': 'rounded=1;whiteSpace=wrap;html=1;arcSize=24;fillColor=#ffffff;strokeColor=#000000;strokeWidth=1.5;fontSize=12;fontColor=#000000;align=center;'},
        
        # Action 3: Process Z-Shock
        {'id': 'a3', 'val': 'Calculate Z-Axis Shock<br/>&amp; Filter Noise', 'x': 270, 'y': 250, 'w': 180, 'h': 45,
         'style': 'rounded=1;whiteSpace=wrap;html=1;arcSize=24;fillColor=#ffffff;strokeColor=#000000;strokeWidth=1.5;fontSize=12;fontColor=#000000;align=center;'},
        
        # Decision 1: Anomaly detected?
        {'id': 'd1', 'val': 'Pothole<br/>Detected?', 'x': 295, 'y': 330, 'w': 130, 'h': 60,
         'style': 'rhombus;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#000000;strokeWidth=1.5;fontSize=12;fontColor=#000000;align=center;'},
        
        # Decision 2: Passes Filter?
        {'id': 'd2', 'val': 'Passes Speed &amp;<br/>Turn Filter?', 'x': 295, 'y': 430, 'w': 130, 'h': 60,
         'style': 'rhombus;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#000000;strokeWidth=1.5;fontSize=11;fontColor=#000000;align=center;'},
        
        # Action 4: Save to Database
        {'id': 'a4', 'val': 'Save Pothole Event &amp;<br/>GPS Location to Database', 'x': 270, 'y': 530, 'w': 180, 'h': 45,
         'style': 'rounded=1;whiteSpace=wrap;html=1;arcSize=24;fillColor=#ffffff;strokeColor=#000000;strokeWidth=1.5;fontSize=12;fontColor=#000000;align=center;'},
        
        # Decision 3: Stop Session?
        {'id': 'd3', 'val': 'Stop<br/>Session?', 'x': 300, 'y': 615, 'w': 120, 'h': 55,
         'style': 'rhombus;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#000000;strokeWidth=1.5;fontSize=12;fontColor=#000000;align=center;'},
        
        # Action 5: Generate Report
        {'id': 'a5', 'val': 'Stop Sensors &amp;<br/>Display Summary Report', 'x': 270, 'y': 705, 'w': 180, 'h': 45,
         'style': 'rounded=1;whiteSpace=wrap;html=1;arcSize=24;fillColor=#ffffff;strokeColor=#000000;strokeWidth=1.5;fontSize=12;fontColor=#000000;align=center;'},
        
        # Final Node: Bullseye (Outer + Inner)
        {'id': 'endOuter', 'val': '', 'x': 344, 'y': 785, 'w': 32, 'h': 32,
         'style': 'ellipse;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#000000;strokeWidth=2;'},
        {'id': 'endInner', 'val': '', 'x': 351, 'y': 792, 'w': 18, 'h': 18,
         'style': 'ellipse;whiteSpace=wrap;html=1;fillColor=#000000;strokeColor=#000000;'}
    ]

    edges = [
        {'id': 'e1', 'src': 'start', 'tgt': 'a1', 'val': ''},
        {'id': 'e2', 'src': 'a1', 'tgt': 'a2', 'val': ''},
        {'id': 'e3', 'src': 'a2', 'tgt': 'a3', 'val': ''},
        {'id': 'e4', 'src': 'a3', 'tgt': 'd1', 'val': ''},
        
        # d1 -> d2 [Yes]
        {'id': 'e5', 'src': 'd1', 'tgt': 'd2', 'val': '[Yes]'},
        # d1 -> d3 [No] (bypass right)
        {'id': 'e6', 'src': 'd1', 'tgt': 'd3', 'val': '[No]',
         'style': 'edgeStyle=orthogonalEdgeStyle;entryX=1;entryY=0.5;exitX=1;exitY=0.5;'},
        
        # d2 -> a4 [Yes]
        {'id': 'e7', 'src': 'd2', 'tgt': 'a4', 'val': '[Yes]'},
        # d2 -> d3 [No] (bypass left)
        {'id': 'e8', 'src': 'd2', 'tgt': 'd3', 'val': '[No]',
         'style': 'edgeStyle=orthogonalEdgeStyle;entryX=0;entryY=0.5;exitX=0;exitY=0.5;'},
        
        # a4 -> d3
        {'id': 'e9', 'src': 'a4', 'tgt': 'd3', 'val': ''},
        
        # d3 -> a2 [No] (loop back to Read Data)
        {'id': 'e10', 'src': 'd3', 'tgt': 'a2', 'val': '[No]',
         'style': 'edgeStyle=orthogonalEdgeStyle;entryX=0;entryY=0.5;exitX=0;exitY=0.5;'},
        
        # d3 -> a5 [Yes]
        {'id': 'e11', 'src': 'd3', 'tgt': 'a5', 'val': '[Yes]'},
        
        # a5 -> end
        {'id': 'e12', 'src': 'a5', 'tgt': 'endOuter', 'val': ''}
    ]

    for n in nodes:
        c = ET.SubElement(root, 'mxCell', {
            'id': n['id'], 'value': n['val'], 'parent': '1', 'vertex': '1', 'style': n['style']
        })
        ET.SubElement(c, 'mxGeometry', {
            'x': str(n['x']), 'y': str(n['y']), 'width': str(n['w']), 'height': str(n['h']), 'as': 'geometry'
        })

    for e in edges:
        style = e.get('style', 'edgeStyle=orthogonalEdgeStyle;') + "rounded=0;html=1;strokeColor=#000000;strokeWidth=1.5;endArrow=block;endFill=1;fontSize=11;fontColor=#000000;"
        c = ET.SubElement(root, 'mxCell', {
            'id': e['id'], 'value': e['val'], 'parent': '1', 'edge': '1',
            'source': e['src'], 'target': e['tgt'], 'style': style
        })
        ET.SubElement(c, 'mxGeometry', {'relative': '1', 'as': 'geometry'})

    xml_str = ET.tostring(mxfile, encoding='utf-8')
    pretty_xml = xml.dom.minidom.parseString(xml_str).toprettyxml(indent='  ')
    
    with open('activity diagram.drawio', 'w', encoding='utf-8') as f:
        f.write(pretty_xml)
    print("Updated activity diagram.drawio with small, simple B&W diagram")

if __name__ == '__main__':
    create_simple_bw_diagram()
