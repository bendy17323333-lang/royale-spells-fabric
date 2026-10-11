# SPDX-License-Identifier: MIT
"""Export the saved native Blockbench Witch meshes; never generate geometry.

Inputs: art/blockbench/{witch,witch_evolution}.bbmodel, each with the same
embedded 128x128 atlas. Outputs are the two TroopModel JSONs, that exact PNG,
and art/witch-remake/native-export-audit.json. --check performs all validation
without writing. --self-test exercises the coordinate contract in memory.

The author's native meshes have origin/rotation zero and absolute vertices.
Bone origins are absolute in BB's Y-up space (feet=0). TroopModel uses relative
pivots in Y-down space (feet=24), degree-valued rest rotations, and normalized
UVs. Java converts those degrees to radians when applying each transform.
Reflecting Y reverses winding and the signs of X/Z rotations. Triangles use a
repeated final vertex because the runtime submits GL quads, not triangle lists.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import math
import os
from pathlib import Path
import struct
import tempfile
import zlib

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/royalespells'
KINDS = ('witch', 'witch_evolution')
LIMIT = 256.0  # Model-space pixels; an accidental block/pixel mix must fail.
REQUIRED_BONES = {'body', 'head', 'right_arm', 'left_arm', 'right_forearm',
                  'left_forearm', 'staff', 'right_leg',
                  'left_leg', 'cape'}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def vector(value, length, label, limit=LIMIT):
    require(isinstance(value, (list, tuple)) and len(value) == length,
            f'{label}: expected {length} coordinates')
    require(all(isinstance(v, (int, float)) and not isinstance(v, bool)
                and math.isfinite(v) and abs(v) <= limit for v in value),
            f'{label}: non-finite or out-of-range coordinates')
    return [float(v) for v in value]


def sub(a, b):
    return [x-y for x, y in zip(a, b)]


def cross(a, b):
    return [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]]


def reflect(point):
    return [point[0], 24-point[1], point[2]]


def rotate(point, degrees):
    """TroopModel's column-vector transform: Rz * Ry * Rx * point."""
    x, y, z = point
    rx, ry, rz = map(math.radians, degrees)
    y, z = y*math.cos(rx)-z*math.sin(rx), y*math.sin(rx)+z*math.cos(rx)
    x, z = x*math.cos(ry)+z*math.sin(ry), -x*math.sin(ry)+z*math.cos(ry)
    x, y = x*math.cos(rz)-y*math.sin(rz), x*math.sin(rz)+y*math.cos(rz)
    return [x, y, z]


def bounds(points):
    require(bool(points), 'Empty geometry has no bounds')
    return {'min': [round(min(p[i] for p in points), 7) for i in range(3)],
            'max': [round(max(p[i] for p in points), 7) for i in range(3)]}


def embedded_atlas(document, label):
    textures = document.get('textures', [])
    require(len(textures) == 1, f'{label}: exactly one embedded texture is required')
    texture = textures[0]
    source = texture.get('source', '')
    prefix = 'data:image/png;base64,'
    require(source.startswith(prefix), f'{label}: atlas must be an embedded PNG data URI')
    png = base64.b64decode(source[len(prefix):], validate=True)
    require(png[:8] == b'\x89PNG\r\n\x1a\n', f'{label}: invalid PNG signature')
    # Validate every chunk CRC, including IDAT. Copy original bytes unchanged.
    cursor, seen_header, ended, image_data = 8, False, False, False
    while cursor < len(png):
        require(cursor+12 <= len(png), f'{label}: truncated PNG chunk')
        length = struct.unpack_from('>I', png, cursor)[0]
        require(cursor+12+length <= len(png), f'{label}: truncated PNG payload')
        kind = png[cursor+4:cursor+8]
        payload = png[cursor+8:cursor+8+length]
        crc = struct.unpack_from('>I', png, cursor+8+length)[0]
        require(zlib.crc32(kind+payload) & 0xffffffff == crc, f'{label}: PNG CRC mismatch')
        if kind == b'IHDR':
            require(not seen_header and cursor == 8 and length == 13, f'{label}: invalid IHDR')
            width, height = struct.unpack_from('>II', payload)
            require((width, height) == (128, 128), f'{label}: atlas must be 128x128, got {width}x{height}')
            seen_header = True
        if kind == b'IDAT':
            image_data = True
        cursor += length+12
        if kind == b'IEND':
            require(length == 0 and cursor == len(png), f'{label}: invalid PNG end')
            ended = True
            break
    require(seen_header and image_data and ended, f'{label}: incomplete PNG')
    resolution = document.get('resolution', {})
    uv_width = texture.get('uv_width', resolution.get('width', 128))
    uv_height = texture.get('uv_height', resolution.get('height', 128))
    require((uv_width, uv_height) == (128, 128), f'{label}: atlas UV dimensions must be 128x128')
    return texture, png


def export_geometry(document, label, require_bones=True):
    texture, png = embedded_atlas(document, label)
    source_elements = document.get('elements', [])
    elements = {e['uuid']: e for e in source_elements}
    require(len(elements) == len(source_elements), f'{label}: duplicate mesh UUID')
    native_groups = document.get('groups', [])
    groups = {g['uuid']: g for g in native_groups}
    require(len(groups) == len(native_groups), f'{label}: duplicate group UUID')
    parts, part_by_name, seen_groups, seen_meshes = [], {}, set(), set()
    counts = {'meshes': 0, 'source_faces': 0, 'triangles': 0, 'quads': 0,
              'runtime_quads': 0, 'source_vertices': 0}
    raw_points, runtime_points, world_faces, mesh_reports = [], [], [], []
    texture_refs = {0, '0', texture.get('uuid'), texture.get('id')}
    texture_refs.discard(None)

    def world_point(part, point):
        """Reconstruct runtime bind pose for bounds/facing audit only."""
        point = [a+b for a, b in zip(rotate(point, part['rotation']), part['pivot'])]
        return world_point(part_by_name[part['parent']], point) if part['parent'] else point

    def mesh(eid, part, origin):
        require(eid in elements, f'{label}: missing mesh UUID {eid}')
        require(eid not in seen_meshes, f'{label}: mesh {eid} referenced twice')
        seen_meshes.add(eid)
        element = elements[eid]
        name = element.get('name', eid)
        context = f'{label}/{part["name"]}/{name}'
        require(element.get('type') == 'mesh', f'{context}: all elements must be native meshes')
        require(element.get('export', True) and element.get('visibility', True),
                f'{context}: hidden/non-exported mesh; resolve it in Blockbench first')
        require(vector(element.get('origin', [0, 0, 0]), 3, context+' origin') == [0, 0, 0],
                f'{context}: expected absolute vertices with zero mesh origin; bake transforms in Blockbench')
        require(vector(element.get('rotation', [0, 0, 0]), 3, context+' rotation', 3600) == [0, 0, 0],
                f'{context}: bake mesh rotation in Blockbench; group rotations are supported')
        vertices = {key: vector(value, 3, context+'/'+key) for key, value in element.get('vertices', {}).items()}
        require(bool(vertices), f'{context}: empty vertices')
        raw_points.extend(vertices.values())
        counts['source_vertices'] += len(vertices)
        output = {'name': name, 'material': 0, 'faces': [], 'uvs': []}
        if name.startswith('EM_'):
            output['emissive'] = True
        faces = element.get('faces', {})
        require(bool(faces), f'{context}: empty faces')
        for face_id, face in faces.items():
            tag = context+'/'+str(face_id)
            ids = face.get('vertices', [])
            require(len(ids) in (3, 4) and len(set(ids)) == len(ids), f'{tag}: expected a triangle or quad')
            require(all(key in vertices for key in ids), f'{tag}: unknown vertex')
            reference = face.get('texture')
            require(not isinstance(reference, bool) and reference in texture_refs, f'{tag}: face must use the single atlas')
            # Reflecting Y changes handedness: reverse vertex and UV order together.
            ids = list(reversed(ids))
            points = [[vertices[key][0]-origin[0], origin[1]-vertices[key][1],
                       vertices[key][2]-origin[2]] for key in ids]
            normal = cross(sub(points[1], points[0]), sub(points[2], points[0]))
            require(sum(v*v for v in normal) > 1e-12, f'{tag}: first triangle is degenerate (runtime normal would be NaN)')
            uv = [vector(face.get('uv', {}).get(key), 2, tag+' UV', 128) for key in ids]
            require(all(0 <= n <= 128 for pair in uv for n in pair), f'{tag}: UV outside the atlas')
            uv = [[n/128 for n in pair] for pair in uv]
            if len(ids) == 3:
                counts['triangles'] += 1
                points.append(points[-1].copy())
                uv.append(uv[-1].copy())
            else:
                counts['quads'] += 1
                second = cross(sub(points[2], points[0]), sub(points[3], points[0]))
                require(sum(v*v for v in second) > 1e-12, f'{tag}: second quad triangle is degenerate')
                require(sum(a*b for a, b in zip(normal, second)) > 0, f'{tag}: crossed/inconsistent quad winding')
            output['faces'].append([round(n, 7) for point in points for n in point])
            output['uvs'].append([round(n, 8) for pair in uv for n in pair])
            posed = [world_point(part, point) for point in points]
            require(all(math.isfinite(n) and abs(n) <= LIMIT for point in posed for n in point), f'{tag}: posed geometry outside {LIMIT}px')
            runtime_points.extend(posed)
            world_faces.append(posed)
            counts['source_faces'] += 1
            counts['runtime_quads'] += 1
        part['boxes'].append(output)
        counts['meshes'] += 1
        mesh_reports.append({'name': name, 'bone': part['name'], 'uuid': eid,
                             'vertices': len(vertices), 'faces': len(faces), 'native_bounds': bounds(list(vertices.values()))})

    def walk(nodes, parent=None):
        for node in nodes:
            require(isinstance(node, dict), f'{label}: every mesh must belong to a named bone')
            gid = node.get('uuid')
            require(gid and gid not in seen_groups, f'{label}: duplicate/cyclic bone {gid}')
            seen_groups.add(gid)
            # BB 5 saves properties in groups[] and only hierarchy in outliner;
            # earlier codecs embed the properties directly in outliner objects.
            group = {**groups.get(gid, {}), **node}
            name = group.get('name')
            require(isinstance(name, str) and name and name not in part_by_name, f'{label}: duplicate or empty bone name {name!r}')
            require(group.get('export', True) and group.get('visibility', True), f'{label}/{name}: hidden/non-exported group')
            origin = vector(group.get('origin'), 3, label+'/'+name+' origin')
            rotation = vector(group.get('rotation', [0, 0, 0]), 3, label+'/'+name+' rotation', 3600)
            parent_origin = parent[1] if parent else [0, 24, 0]
            part = {'name': name, 'parent': parent[0]['name'] if parent else '',
                    'pivot': [origin[0]-parent_origin[0], parent_origin[1]-origin[1], origin[2]-parent_origin[2]],
                    'rotation': [-rotation[0], rotation[1], -rotation[2]], 'boxes': []}
            parts.append(part)
            part_by_name[name] = part
            children = node.get('children', group.get('children', []))
            for child in children:
                if isinstance(child, str):
                    mesh(child, part, origin)
            walk([child for child in children if not isinstance(child, str)], (part, origin))

    walk(document.get('outliner', []))
    require(set(elements) == seen_meshes, f'{label}: unparented meshes: {sorted(set(elements)-seen_meshes)}')
    require(not set(groups)-seen_groups, f'{label}: unreferenced native groups: {sorted(set(groups)-seen_groups)}')
    if require_bones:
        required = REQUIRED_BONES | ({'staff_crystal'} if label == 'witch_evolution' else set())
        require(required <= set(part_by_name), f'{label}: missing animation bones {sorted(required-set(part_by_name))}')
    require(parts and counts['source_faces'], f'{label}: empty model')
    # This is geometry coverage, NOT screenshots, occlusion or aesthetic approval.
    directions = {'front': (0, 0, -1), 'back': (0, 0, 1),
                  'left': (-1, 0, 0), 'right': (1, 0, 0)}
    views = {name: {'front_facing_faces': 0, 'projected_area_px2': 0.0} for name in directions}
    for face in world_faces:
        n = cross(sub(face[1], face[0]), sub(face[2], face[0]))
        n2 = cross(sub(face[2], face[0]), sub(face[3], face[0]))
        for name, direction in directions.items():
            dot = sum(a*b for a, b in zip(n, direction))
            dot2 = sum(a*b for a, b in zip(n2, direction))
            if dot > 1e-8 or dot2 > 1e-8:
                views[name]['front_facing_faces'] += 1
                views[name]['projected_area_px2'] += (max(0, dot)+max(0, dot2))*.5
    for view in views.values():
        view['projected_area_px2'] = round(view['projected_area_px2'], 5)
    audit = {'bones': len(parts), **counts, 'all_coordinates_finite': True,
             'all_uvs_in_range': True, 'source_bounds_bb_y_up': bounds(raw_points),
             'runtime_bind_pose_bounds_y_down': bounds(runtime_points),
             'rotation_units': 'degrees (TroopModel converts to radians)',
             'mesh_coordinate_contract': 'absolute vertices, zero element origin/rotation',
             'triangles_padded_as_quads': counts['triangles'], 'face_texture_meshes': 0,
             'four_view_geometry': views,
             'four_view_face_total_with_directional_overlap': sum(v['front_facing_faces'] for v in views.values()),
             'four_view_scope': 'Facing/projected-area counts; not visible-surface or image-quality acceptance',
             'meshes_detail': mesh_reports}
    return {'parts': parts}, png, audit


def atomic_write(path, payload):
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(dir=path.parent, prefix=path.name+'.', suffix='.tmp', delete=False) as stream:
        temporary = Path(stream.name)
        stream.write(payload)
    try:
        os.replace(temporary, path)
    finally:
        if temporary.exists():
            temporary.unlink()


def json_bytes(value):
    return (json.dumps(value, ensure_ascii=False, indent=2, allow_nan=False)+'\n').encode('utf-8')


def self_test():
    """A nested rotated bone, reflected triangle/UV and invalid input regression."""
    def chunk(kind, data):
        return struct.pack('>I', len(data))+kind+data+struct.pack('>I', zlib.crc32(kind+data) & 0xffffffff)
    png = b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR', struct.pack('>IIBBBBB', 128, 128, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress((b'\0'+b'\xff'*512)*128))+chunk(b'IEND', b'')
    fixture = {'textures': [{'uuid': 'atlas', 'source': 'data:image/png;base64,'+base64.b64encode(png).decode()}],
        'groups': [{'uuid': 'body-id', 'name': 'body', 'origin': [0, 12, 0], 'rotation': [0, 0, 0]},
                   {'uuid': 'head-id', 'name': 'head', 'origin': [0, 24, 0], 'rotation': [30, 20, 10]}],
        'outliner': [{'uuid': 'body-id', 'children': [{'uuid': 'head-id', 'children': ['mesh-id']}]}],
        'elements': [{'uuid': 'mesh-id', 'name': 'test', 'type': 'mesh',
                      'vertices': {'a': [0, 24, 0], 'b': [1, 24, 0], 'c': [0, 25, 0]},
                      'faces': {'f': {'vertices': ['a', 'b', 'c'], 'texture': 0,
                                      'uv': {'a': [0, 0], 'b': [128, 0], 'c': [0, 128]}}}}]}
    model, extracted, report = export_geometry(fixture, 'fixture', False)
    head = model['parts'][1]
    require(head['pivot'] == [0, -12, 0] and head['rotation'] == [-30, 20, -10], 'Coordinate/rotation self-test failed')
    require(head['boxes'][0]['faces'][0] == [0, -1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0], 'Triangle winding self-test failed')
    require(head['boxes'][0]['uvs'][0] == [0, 1, 1, 0, 0, 0, 0, 0], 'UV ordering self-test failed')
    require(extracted == png and report['triangles'] == 1, 'Atlas/coverage self-test failed')
    # Reflection commutes with the appropriately sign-flipped hierarchy rotation.
    point = [1, 2, 3]
    native = rotate(point, [30, 20, 10])
    mirrored = rotate([1, -2, 3], [-30, 20, -10])
    require(math.dist([native[0], -native[1], native[2]], mirrored) < 1e-10, 'Reflection rotation self-test failed')
    for bad_value in (math.nan, math.inf, LIMIT+1):
        fixture['elements'][0]['vertices']['a'][0] = bad_value
        try:
            export_geometry(fixture, 'invalid-fixture', False)
        except ValueError:
            pass
        else:
            raise ValueError('Non-finite/range guard self-test failed')
    print('NATIVE_WITCH_EXPORT_SELF_TEST_OK (in-memory only; no runtime assets changed)')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='Validate both native models without writing outputs')
    parser.add_argument('--self-test', action='store_true', help='Run coordinate/UV/rotation guard checks in memory only')
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return
    outputs, reports, atlases = {}, {}, []
    for kind in KINDS:
        source = ROOT/'art/blockbench'/f'{kind}.bbmodel'
        data = source.read_bytes()
        model, atlas, report = export_geometry(json.loads(data.decode('utf-8-sig')), kind)
        payload = json_bytes(model)
        destination = ASSETS/'models/troop'/f'{kind}.json'
        outputs[destination] = payload
        atlases.append(atlas)
        reports[kind] = {'source': source.relative_to(ROOT).as_posix(),
                         'source_sha256': hashlib.sha256(data).hexdigest(),
                         'runtime': destination.relative_to(ROOT).as_posix(),
                         'runtime_sha256': hashlib.sha256(payload).hexdigest(), **report}
    require(atlases[0] == atlases[1], 'Normal and evolved native models must embed the identical shared atlas')
    outputs[ASSETS/'textures/entity/witch_materials.png'] = atlases[0]
    report = {'schema': 1, 'exporter': 'tools/export-native-witch.py',
              'atlas': {'size': [128, 128], 'sha256': hashlib.sha256(atlases[0]).hexdigest(),
                        'bytes_copied_unchanged_from_native': True},
              'models': reports, 'runtime_visual_review_required': True}
    audit_path = ROOT/'art/witch-remake/native-export-audit.json'
    # Validate BOTH models and the shared texture before replacing any output.
    if not args.check:
        for path, payload in outputs.items():
            atomic_write(path, payload)
        atomic_write(audit_path, json_bytes(report))
    for kind, result in reports.items():
        print(f'{kind}: {result["bones"]} bones, {result["meshes"]} meshes, '
              f'{result["source_faces"]} faces ({result["triangles"]} triangles / {result["quads"]} quads)')
    print('NATIVE_WITCH_EXPORT_CHECK_OK (no outputs written)' if args.check else 'NATIVE_WITCH_EXPORT_OK '+str(audit_path))


if __name__ == '__main__':
    main()
