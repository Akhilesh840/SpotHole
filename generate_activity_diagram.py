"""
Script to generate standard UML Activity Diagram for Khadda (Passive Pothole Detection System)
Outputs a fully valid Draw.io XML file with proper UML symbols:
- Partitions / Swimlanes (User, UI/Controller, 100Hz Sensing, Detection & Kinematics, Verification & Storage)
- Initial Node (Solid Circle)
- Actions (Rounded Rectangles with arcSize)
- Decision & Merge Nodes (Rhombus with guarded transitions [Yes] / [No])
- Fork & Join Synchronization Bars (Solid Synchronizer Bars)
- Object Nodes (Dashed Rectangles)
- Flow Final Nodes (Circle with X)
- Activity Final Node (Concentric Circles / Bullseye)
"""

import xml.etree.ElementTree as ET
import xml.dom.minidom

def create_diagram():
    mxfile = ET.Element('mxfile', {
        'host': 'app.diagrams.net',
        'modified': '2026-10-16T00:00:00.000Z',
        'agent': 'Antigravity',
        'version': '24.7.17',
        'type': 'device'
    })
    
    diagram = ET.SubElement(mxfile, 'diagram', {
        'id': 'khadda-uml-activity',
        'name': 'UML Activity Diagram'
    })
    
    model = ET.SubElement(diagram, 'mxGraphModel', {
        'dx': '1800',
        'dy': '1400',
        'grid': '1',
        'gridSize': '10',
        'guides': '1',
        'tooltips': '1',
        'connect': '1',
        'arrows': '1',
        'fold': '1',
        'page': '1',
        'pageScale': '1',
        'pageWidth': '1850',
        'pageHeight': '1650',
        'math': '0',
        'shadow': '0',
        'background': '#ffffff'
    })
    
    root = ET.SubElement(model, 'root')
    
    # Layer 0 and Layer 1
    ET.SubElement(root, 'mxCell', {'id': '0'})
    ET.SubElement(root, 'mxCell', {'id': '1', 'parent': '0'})
    
    # Title
    title_cell = ET.SubElement(root, 'mxCell', {
        'id': 'diag_title',
        'value': '<b>UML ACTIVITY DIAGRAM</b><br/><font style="font-size:14px;color:#555555;">Khadda: Passive Pothole & Road Anomaly Detection Architecture</font>',
        'parent': '1',
        'vertex': '1',
        'style': 'text;html=1;strokeColor=none;fillColor=none;fontColor=#1a237e;fontSize=20;align=center;verticalAlign=middle;'
    })
    ET.SubElement(title_cell, 'mxGeometry', {
        'x': '500', 'y': '15', 'width': '800', 'height': '50', 'as': 'geometry'
    })
    
    # 5 Swimlanes / Partitions
    lanes = [
        {
            'id': 'lane_user',
            'name': 'Rider / User',
            'x': 40, 'y': 80, 'w': 280, 'h': 1500,
            'fill': '#e8f5e9', 'header': '#c8e6c9', 'stroke': '#2e7d32', 'text': '#1b5e20'
        },
        {
            'id': 'lane_ui',
            'name': 'Android UI & Service Manager',
            'x': 320, 'y': 80, 'w': 330, 'h': 1500,
            'fill': '#e3f2fd', 'header': '#bbdefb', 'stroke': '#1565c0', 'text': '#0d47a1'
        },
        {
            'id': 'lane_sensor',
            'name': 'Sensor Pipeline (100Hz Hardware & GPS)',
            'x': 650, 'y': 80, 'w': 380, 'h': 1500,
            'fill': '#fff8e1', 'header': '#ffecb3', 'stroke': '#f57f17', 'text': '#e65100'
        },
        {
            'id': 'lane_detector',
            'name': 'Detection & Kinematic Validation Engine',
            'x': 1030, 'y': 80, 'w': 400, 'h': 1500,
            'fill': '#ede7f6', 'header': '#d1c4e9', 'stroke': '#512da8', 'text': '#311b92'
        },
        {
            'id': 'lane_storage',
            'name': 'Post-Ride Batch Verification & Storage',
            'x': 1430, 'y': 80, 'w': 360, 'h': 1500,
            'fill': '#fce4ec', 'header': '#f8bbd0', 'stroke': '#c2185b', 'text': '#880e4f'
        }
    ]
    
    for l in lanes:
        style = (f"swimlane;whiteSpace=wrap;html=1;startSize=38;horizontal=1;collapsible=0;dropTarget=0;"
                 f"fillColor={l['fill']};swimlaneFillColor={l['fill']};strokeColor={l['stroke']};"
                 f"strokeWidth=1.5;fontColor={l['text']};fontStyle=1;fontSize=13;align=center;")
        cell = ET.SubElement(root, 'mxCell', {
            'id': l['id'],
            'value': f"<b>{l['name']}</b>",
            'parent': '1',
            'vertex': '1',
            'style': style
        })
        ET.SubElement(cell, 'mxGeometry', {
            'x': str(l['x']), 'y': str(l['y']), 'width': str(l['w']), 'height': str(l['h']), 'as': 'geometry'
        })

    # Nodes definition
    nodes = []
    edges = []

    # Helper function to add Action
    def add_action(node_id, text, x, y, w=200, h=54, fill='#ffffff', stroke='#37474f'):
        style = (f"rounded=1;whiteSpace=wrap;html=1;arcSize=22;fillColor={fill};strokeColor={stroke};"
                 f"strokeWidth=1.5;fontColor=#212121;fontSize=12;align=center;verticalAlign=middle;")
        nodes.append({'id': node_id, 'value': text, 'x': x, 'y': y, 'w': w, 'h': h, 'style': style})

    # Helper function to add Decision
    def add_decision(node_id, text, x, y, w=140, h=70, fill='#ffffff', stroke='#d84315'):
        style = (f"rhombus;whiteSpace=wrap;html=1;fillColor={fill};strokeColor={stroke};"
                 f"strokeWidth=1.5;fontColor=#212121;fontSize=11;align=center;verticalAlign=middle;")
        nodes.append({'id': node_id, 'value': text, 'x': x, 'y': y, 'w': w, 'h': h, 'style': style})

    # Helper function to add Fork/Join Bar
    def add_sync_bar(node_id, text, x, y, w=300, h=8):
        style = ("shape=rectangle;whiteSpace=wrap;html=1;fillColor=#212121;strokeColor=#212121;"
                 "strokeWidth=1;fontColor=#ffffff;fontSize=9;align=center;verticalAlign=bottom;")
        nodes.append({'id': node_id, 'value': '', 'x': x, 'y': y, 'w': w, 'h': h, 'style': style})

    # Helper function to add Flow Final (Circle with X)
    def add_flow_final(node_id, x, y, label="Discard Anomaly"):
        style = ("ellipse;whiteSpace=wrap;html=1;aspect=fixed;fillColor=#ffebee;strokeColor=#c62828;"
                 "strokeWidth=2;fontSize=14;fontStyle=1;fontColor=#c62828;align=center;verticalAlign=middle;")
        nodes.append({'id': node_id, 'value': '&#10006;', 'x': x, 'y': y, 'w': 28, 'h': 28, 'style': style})

    # Helper function to add Object Node (Data Store)
    def add_object(node_id, text, x, y, w=150, h=45):
        style = ("shape=rectangle;whiteSpace=wrap;html=1;dashed=1;fillColor=#f5f5f5;strokeColor=#616161;"
                 "strokeWidth=1.2;fontColor=#37474f;fontSize=10;align=center;verticalAlign=middle;")
        nodes.append({'id': node_id, 'value': text, 'x': x, 'y': y, 'w': w, 'h': h, 'style': style})

    # Helper function to add Edge
    def add_edge(edge_id, source_id, target_id, label="", style_extra=""):
        style = ("edgeStyle=orthogonalEdgeStyle;rounded=1;orthogonalLoop=1;jettySize=auto;html=1;"
                 f"strokeColor=#37474f;strokeWidth=1.5;endArrow=classic;endFill=1;fontSize=11;fontColor=#263238;{style_extra}")
        edges.append({'id': edge_id, 'src': source_id, 'tgt': target_id, 'val': label, 'style': style})

    # --- NODE CREATION ---
    
    # LANE 1: Rider / User
    # 1. Initial Node (Solid Circle)
    nodes.append({
        'id': 'init_node',
        'value': '',
        'x': 165, 'y': 140, 'w': 30, 'h': 30,
        'style': 'ellipse;whiteSpace=wrap;html=1;fillColor=#000000;strokeColor=#000000;'
    })
    
    add_action('act_u_launch', 'Launch Khadda App &<br/>Select Tracking Mode', 85, 200, 190, 50)
    add_action('act_u_start', 'Tap "Start Detection"<br/>Button', 85, 280, 190, 50)
    add_action('act_u_ride', 'Ride Vehicle over<br/>Road Surface', 85, 480, 190, 50)
    add_action('act_u_manual', 'Optional: Press Volume Key<br/>(Manual Ground-Truth Tag)', 85, 600, 190, 50)
    add_action('act_u_stop', 'Tap "Stop Detection"<br/>Session Button', 85, 1080, 190, 50)
    add_action('act_u_review', 'Review Batch Results,<br/>Pothole Map & Export Zip', 85, 1420, 190, 54)
    
    # Activity Final Node (Double Circle)
    nodes.append({
        'id': 'act_final_outer',
        'value': '',
        'x': 160, 'y': 1510, 'w': 40, 'h': 40,
        'style': 'ellipse;whiteSpace=wrap;html=1;fillColor=#ffffff;strokeColor=#000000;strokeWidth=2;'
    })
    nodes.append({
        'id': 'act_final_inner',
        'value': '',
        'x': 168, 'y': 1518, 'w': 24, 'h': 24,
        'style': 'ellipse;whiteSpace=wrap;html=1;fillColor=#000000;strokeColor=#000000;'
    })

    # LANE 2: Android UI & Service Manager
    add_action('act_ui_perms', 'Check Hardware & Location<br/>Runtime Permissions', 380, 200, 210, 50)
    add_decision('dec_perms', 'Permissions<br/>Granted?', 415, 270, 140, 65)
    add_action('act_ui_prompt', 'Request Required<br/>Permissions from User', 380, 360, 210, 45)
    add_action('act_ui_start_fg', 'Start Foreground Service &<br/>Post Sticky Notification', 380, 430, 210, 50)
    add_action('act_ui_stream', 'Stream Real-time Waveform<br/>& Speed to Live Monitor UI', 380, 520, 210, 50)
    add_action('act_ui_alert', 'Emit Immediate Feedback<br/>(Haptic Vibration & Audio)', 380, 830, 210, 50)
    add_action('act_ui_stopping', 'Signal Background Service<br/>to Terminate Sensing', 380, 1080, 210, 50)
    add_action('act_ui_dialog', 'Show Post-Ride Verification<br/>Progress Dialog in UI', 380, 1260, 210, 50)

    # LANE 3: Sensor Pipeline (100Hz Hardware & GPS)
    add_action('act_s_reg', 'Register 100Hz Sensor Listeners<br/>& Fused Location Provider', 720, 430, 240, 50)
    
    # FORK BAR (Split into parallel sensing flows)
    add_sync_bar('fork_sensing', 'FORK: Parallel Sampling', 700, 510, 280, 8)
    
    add_action('act_s_motion', 'Sample 100Hz Accelerometer,<br/>Gyroscope, Gravity & LinAccel', 670, 545, 180, 54)
    add_action('act_s_gps', 'Poll Fused GPS Coordinates<br/>(Lat, Lon, Speed, Bearing)', 870, 545, 150, 54)
    add_action('act_s_camera', 'Maintain 1-Sec Circular<br/>Buffer of Camera Frames', 670, 625, 180, 50)
    
    add_action('act_s_proj', 'Dynamic Gravity Projection &<br/>Earth-Relative Z-Axis Isolation', 670, 700, 180, 54)
    add_action('act_s_smooth', 'Apply 5-Sample Moving Avg &<br/>Compute Statistical Rolling Window', 670, 780, 180, 54)
    
    # JOIN BAR (Synchronize sensor stream with GPS)
    add_sync_bar('join_sensing', 'JOIN: Synchronize Features', 700, 860, 280, 8)
    
    add_action('act_s_log_telemetry', 'Queue Raw 100Hz Row to<br/>ConcurrentLinkedQueue', 730, 890, 220, 48)
    add_object('obj_sensor_csv', 'sensor_log.csv<br/>gps_log.csv', 765, 960, 150, 40)

    # LANE 4: Detection & Kinematic Validation Engine
    add_action('act_d_extract', 'Extract Kinematic Features:<br/>[zDiff, zThresh, stdevZ, yawRate]', 1110, 510, 240, 50)
    add_action('act_d_eval', 'Evaluate Scientific Algorithms:<br/>1. Z-DIFF  2. Z-THRESH<br/>3. STDEV(Z)  4. G-ZERO', 1110, 585, 240, 60)
    add_decision('dec_anomaly', 'Any Anomaly<br/>Signature<br/>Triggered?', 1160, 675, 140, 70)
    add_decision('dec_2vote', 'Passes 2-Vote<br/>Ensemble Rule<br/>(&ge; 2 Algorithms)?', 1160, 775, 140, 70)
    add_decision('dec_kinematic', 'Passes Kinematic<br/>False-Positive<br/>Gates?', 1160, 875, 140, 75)
    
    # Rejection actions for false-positive logs
    add_action('act_d_rej_vote', 'Log to candidate_log.csv<br/>[Reason: Failed 2-Vote Rule]', 1280, 785, 140, 50, fill='#ffebee', stroke='#c62828')
    add_action('act_d_rej_kin', 'Log to candidate_log.csv<br/>[Reason: Braking/Turn/Speed/Cool]', 1280, 885, 140, 54, fill='#ffebee', stroke='#c62828')
    add_flow_final('flow_rej_1', 1335, 745)
    add_flow_final('flow_rej_2', 1335, 965)
    
    add_action('act_d_accept', 'Accept as CandidateEvent:<br/>Assign ID, Severity & Timestamp', 1120, 990, 220, 50, fill='#e8f5e9', stroke='#2e7d32')

    # LANE 5: Post-Ride Batch Verification & Storage
    add_action('act_v_async_save', 'Asynchronous Worker:<br/>Compress 5 Post-Impact Frames<br/>& Store Candidate (Status: PENDING)', 1490, 830, 240, 55)
    add_object('obj_room_cand', 'SQLite Room DB<br/>[CandidateEntity (PENDING)]', 1530, 915, 160, 40)
    
    add_action('act_v_flush', 'Unregister Sensors, Unbind<br/>Camera & Flush Telemetry Files', 1490, 1080, 240, 50)
    add_decision('dec_has_cand', 'Pending<br/>Candidates<br/>in Session?', 1540, 1160, 140, 65)
    
    add_action('act_v_batch', 'Run TFLite / YOLO Vision<br/>Analyzer on Multi-Frame Sequence<br/>(Dispatchers.Default)', 1490, 1260, 240, 55)
    add_decision('dec_vision_conf', 'Vision Score &ge; 0.50<br/>on &ge; 2 Frames?', 1540, 1345, 140, 70)
    
    add_action('act_v_confirm', 'Promote Candidate to CONFIRMED<br/>& Insert into PotholeEvent Table', 1435, 1445, 195, 50, fill='#e8f5e9', stroke='#2e7d32')
    add_action('act_v_reject', 'Mark Candidate REJECTED<br/>(Preserve Frames for Retraining)', 1640, 1445, 175, 50, fill='#fff3e0', stroke='#ef6c00')
    
    add_action('act_v_finalize', 'Calculate Session Aggregates &<br/>Compress Logs to Telemetry.zip', 1490, 1520, 240, 50)
    add_object('obj_zip', 'KhaddaTelemetry.zip<br/>(CSV logs + Metadata)', 1530, 1600, 160, 40)

    # --- EDGES / TRANSITIONS WITH PROPER UML GUARDS ---
    
    # User initiation
    add_edge('e1', 'init_node', 'act_u_launch')
    add_edge('e2', 'act_u_launch', 'act_ui_perms')
    add_edge('e3', 'act_ui_perms', 'dec_perms')
    add_edge('e4', 'dec_perms', 'act_ui_prompt', '[No: Missing]')
    add_edge('e5', 'act_ui_prompt', 'act_ui_perms', 'User Grants')
    add_edge('e6', 'dec_perms', 'act_u_start', '[Yes: All Granted]')
    add_edge('e7', 'act_u_start', 'act_ui_start_fg')
    
    # Foreground Service starting sensors
    add_edge('e8', 'act_ui_start_fg', 'act_s_reg')
    add_edge('e9', 'act_s_reg', 'fork_sensing')
    
    # Parallel Sensing Branches
    add_edge('e10', 'fork_sensing', 'act_s_motion')
    add_edge('e11', 'fork_sensing', 'act_s_gps')
    add_edge('e12', 'fork_sensing', 'act_s_camera')
    
    add_edge('e13', 'act_s_motion', 'act_s_proj')
    add_edge('e14', 'act_s_proj', 'act_s_smooth')
    add_edge('e15', 'act_s_smooth', 'join_sensing')
    add_edge('e16', 'act_s_gps', 'join_sensing')
    add_edge('e17', 'act_s_camera', 'join_sensing')
    
    # After Join: Telemetry log and Live UI
    add_edge('e18', 'join_sensing', 'act_s_log_telemetry')
    add_edge('e19', 'act_s_log_telemetry', 'obj_sensor_csv', '', 'dashed=1;endArrow=none;')
    add_edge('e20', 'join_sensing', 'act_ui_stream', 'Stream 100Hz')
    
    # Manual Trigger parallel path
    add_edge('e_man1', 'act_u_manual', 'act_d_accept', 'Physical Key Event')
    
    # Feature extraction & detection pipeline
    add_edge('e21', 'join_sensing', 'act_d_extract')
    add_edge('e22', 'act_d_extract', 'act_d_eval')
    add_edge('e23', 'act_d_eval', 'dec_anomaly')
    
    # Anomaly Decision: No -> Continue continuous sampling
    add_edge('e24', 'dec_anomaly', 'fork_sensing', '[No Anomaly]', 'edgeStyle=orthogonalEdgeStyle;')
    
    # Anomaly Decision: Yes -> Check 2-Vote Rule
    add_edge('e25', 'dec_anomaly', 'dec_2vote', '[Yes: Impact / Anomaly]')
    add_edge('e26', 'dec_2vote', 'act_d_rej_vote', '[No: < 2 algorithms]')
    add_edge('e27', 'act_d_rej_vote', 'flow_rej_1')
    
    # 2-Vote Passed: Check Kinematic Gates
    add_edge('e28', 'dec_2vote', 'dec_kinematic', '[Yes: >= 2 algorithms]')
    add_edge('e29', 'dec_kinematic', 'act_d_rej_kin', '[False Positive Detected]')
    add_edge('e30', 'act_d_rej_kin', 'flow_rej_2')
    
    # Kinematic Gates Passed: Accept Candidate
    add_edge('e31', 'dec_kinematic', 'act_d_accept', '[Passed All Gates]')
    
    # Instant User Feedback & Background Frame Capture
    add_edge('e32', 'act_d_accept', 'act_ui_alert', 'Event Triggered')
    add_edge('e33', 'act_d_accept', 'act_v_async_save', 'Dispatch 5 Frames')
    add_edge('e34', 'act_v_async_save', 'obj_room_cand', '', 'dashed=1;endArrow=none;')
    add_edge('e35', 'act_d_accept', 'fork_sensing', 'Continue Monitoring', 'edgeStyle=orthogonalEdgeStyle;')
    
    # User Stops Session
    add_edge('e36', 'act_u_stop', 'act_ui_stopping')
    add_edge('e37', 'act_ui_stopping', 'act_v_flush')
    add_edge('e38', 'act_v_flush', 'dec_has_cand')
    
    # Verification Decision
    add_edge('e39', 'dec_has_cand', 'act_v_finalize', '[No Candidates]')
    add_edge('e40', 'dec_has_cand', 'act_v_batch', '[Yes: Verify Pending]')
    add_edge('e41', 'act_v_batch', 'act_ui_dialog', 'Update Progress (0..N)')
    add_edge('e42', 'act_v_batch', 'dec_vision_conf')
    
    add_edge('e43', 'dec_vision_conf', 'act_v_confirm', '[Yes: Confirmed]')
    add_edge('e44', 'dec_vision_conf', 'act_v_reject', '[No: Rejected]')
    
    add_edge('e45', 'act_v_confirm', 'act_v_finalize')
    add_edge('e46', 'act_v_reject', 'act_v_finalize')
    
    # Finalization to review & exit
    add_edge('e47', 'act_v_finalize', 'obj_zip', '', 'dashed=1;endArrow=none;')
    add_edge('e48', 'act_v_finalize', 'act_u_review', 'Export Ready')
    add_edge('e49', 'act_u_review', 'act_final_outer')

    # Append all nodes to XML
    for n in nodes:
        cell = ET.SubElement(root, 'mxCell', {
            'id': n['id'],
            'value': n['value'],
            'parent': '1',
            'vertex': '1',
            'style': n['style']
        })
        ET.SubElement(cell, 'mxGeometry', {
            'x': str(n['x']), 'y': str(n['y']), 'width': str(n['w']), 'height': str(n['h']), 'as': 'geometry'
        })

    # Append all edges to XML
    for e in edges:
        cell = ET.SubElement(root, 'mxCell', {
            'id': e['id'],
            'value': e['val'],
            'parent': '1',
            'edge': '1',
            'source': e['src'],
            'target': e['tgt'],
            'style': e['style']
        })
        ET.SubElement(cell, 'mxGeometry', {
            'relative': '1', 'as': 'geometry'
        })

    xml_str = ET.tostring(mxfile, encoding='utf-8')
    parsed = xml.dom.minidom.parseString(xml_str)
    pretty_xml = parsed.toprettyxml(indent='  ')
    
    # Write to target files
    with open('activity diagram.drawio', 'w', encoding='utf-8') as f:
        f.write(pretty_xml)
        
    print("Successfully generated 'activity diagram.drawio'")

if __name__ == '__main__':
    create_diagram()
