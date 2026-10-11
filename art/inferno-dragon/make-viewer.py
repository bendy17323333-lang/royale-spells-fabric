"""Export the actual saved Blockbench geometry to a dependency-free WebGL viewer.

Texture bytes are embedded unchanged. This script creates no raster artwork.
Group rest rotations and keyframes come from the saved .bbmodel, not a second rig.
"""
import base64
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parent
source = json.loads((ROOT / 'inferno_dragon.bbmodel').read_text(encoding='utf-8'))
group_source = {g['uuid']: g for g in source['groups']}
group_indices, groups, parents = {}, [], {}

def walk(nodes, parent=-1):
    for node in nodes:
        if isinstance(node, str):
            parents[node] = parent
            continue
        group = group_source[node['uuid']]
        i = len(groups)
        group_indices[group['uuid']] = i
        groups.append(dict(name=group['name'], origin=group['origin'], rotation=group['rotation'], parent=parent))
        walk(node.get('children', []), i)

walk(source['outliner'])
geometry = {}
for element in source['elements']:
    parent = parents[element['uuid']]
    assert parent >= 0
    emissive = element['name'].startswith('EM_')
    stream = geometry.setdefault((parent, emissive), [])
    offset = [element['origin'][i] - groups[parent]['origin'][i] for i in range(3)]
    vertices = {k: [v[i] + offset[i] for i in range(3)] for k, v in element['vertices'].items()}
    for face in element['faces'].values():
        ids = face['vertices']
        for i in range(1, len(ids) - 1):
            triangle = [ids[0], ids[i], ids[i+1]]
            a, b, c = [vertices[k] for k in triangle]
            ab = [b[j] - a[j] for j in range(3)]
            ac = [c[j] - a[j] for j in range(3)]
            n = [ab[1]*ac[2]-ab[2]*ac[1], ab[2]*ac[0]-ab[0]*ac[2], ab[0]*ac[1]-ab[1]*ac[0]]
            length = math.hypot(*n)
            assert length > 1e-8, element['name']
            n = [v / length for v in n]
            for k in triangle:
                uv = [face['uv'][k][0] / source['resolution']['width'], face['uv'][k][1] / source['resolution']['height']]
                assert all(0 <= v <= 1 for v in uv)
                stream.extend(round(v, 6) for v in vertices[k] + n + uv)

animations = []
for animation in source['animations']:
    channels = {}
    for uuid, animator in animation['animators'].items():
        if uuid not in group_indices:
            continue
        by_channel = {}
        for key in animator.get('keyframes', []):
            point = key['data_points'][0]
            by_channel.setdefault(key['channel'], []).append([key['time'], [float(point.get(axis, 0)) for axis in 'xyz']])
        for keys in by_channel.values():
            keys.sort(key=lambda k: k[0])
        channels[str(group_indices[uuid])] = by_channel
    animations.append(dict(name=animation['name'], length=animation['length'], loop=animation['loop']=='loop', channels=channels))

model = dict(groups=groups, batches=[dict(parent=k[0], emissive=k[1], vertices=v) for k, v in geometry.items()], animations=animations)
texture = 'data:image/png;base64,' + base64.b64encode((ROOT / 'inferno-materials.png').read_bytes()).decode('ascii')
template = (ROOT / 'viewer-template.html').read_text(encoding='utf-8')
html = template.replace('__MODEL__', json.dumps(model, separators=(',', ':'))).replace('__TEXTURE__', texture)
(ROOT / 'inferno-dragon-viewer.html').write_text(html, encoding='utf-8')
stats = dict(meshes=len(source['elements']), bones=len(groups), triangles=sum(len(v)//24 for v in geometry.values()), animations=len(animations), rest_pose={g['name']:g['rotation'] for g in groups if any(g['rotation'])}, texture_dimensions=source['resolution'])
(ROOT / 'model-check.json').write_text(json.dumps(stats, indent=2), encoding='utf-8')
print(json.dumps(stats))
