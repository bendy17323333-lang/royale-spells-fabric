"""Gate and package the local 1.9.1 Witch release using its own evidence.

This script does not launch Minecraft, install into a player instance, upload,
or publish. A completed framebuffer capture is technical evidence, not an
art-quality verdict. The separate, hash-bound visual review must exist first.
Run only after the final build, final production QA and visual review finish.
"""
from pathlib import Path
import argparse
import base64
import csv
import hashlib
import io
import json
import math
import re
import shutil
import struct
import tempfile
import zipfile
import xml.etree.ElementTree as ET


P = Path(__file__).resolve().parents[1]
VERSION = '1.9.1'
JAR_NAME = f'royale-spells-fabric-1.20.1-{VERSION}.jar'
OUT = P.parents[1] / f'releases/royale-spells-{VERSION}-fabric-1.20.1'
BUILD_LOG = P / 'witch-final-build.log'
PRODUCTION = P / 'run-production-witch'
PRODUCTION_LOG = PRODUCTION / 'production.log'
TEST_XML = P / 'run-gametest/build/gametest-results.xml'
REVIEW = P / 'docs/validation/witch-visual-review.json'
RUNTIME_MANIFEST = P / f'docs/reference/runtime-assets-{VERSION}.json'
VERIFY = P / 'docs/validation/verification.json'
CONTINUITY = PRODUCTION / 'witch-visual-continuity.csv'
ART_PROVENANCE = P / 'art/witch-remake/model-provenance.json'
SCENES = ('front-oblique', 'high', 'rear', 'close-side', 'stone-occlusion')
ATTACK_FRAMES = ('windup', 'release', 'flight', 'impact-core',
                 'impact-expansion', 'impact-smoke', 'impact-embers')
STATIC_FRAMES = (
    'normal-front', 'normal-three-quarter', 'normal-back',
    'evolved-front', 'evolved-three-quarter', 'evolved-back',
    'summon-opening', 'summon-four-skeletons', 'evolved-first-wave',
    'two-witches-one-attacking', 'snow-slow-early', 'snow-slow-late',
    'snow-recovered-early', 'two-witches-independent-attacks',
    'freeze-hold-01', 'freeze-hold-02', 'freeze-hold-03', 'freeze-resumed',
    'soul-04', 'soul-10', 'soul-17', 'soul-arrival', 'heal-flash',
    'face-close', 'profile-and-grip', 'walk-stride-a', 'walk-stride-b',
)
EXPECTED = {f'{scene}-{frame}' for scene in SCENES for frame in ATTACK_FRAMES}
EXPECTED.update(STATIC_FRAMES)
WITCH_ART = (
    'art/blockbench/witch.bbmodel', 'art/blockbench/witch_evolution.bbmodel',
    'art/witch-remake/build-in-blockbench.js',
    'art/witch-remake/refine-in-blockbench.js',
    'art/witch-remake/polish-in-blockbench.js',
    'art/witch-remake/proportion-in-blockbench.js',
    'art/witch-remake/face-opening-in-blockbench.js',
    'art/witch-remake/evolution-in-blockbench.js',
    'art/witch-remake/simplify-hands-in-blockbench.js',
    'art/witch-remake/final-cleanup-in-blockbench.js',
    'art/witch-remake/witch-legacy-staff.json',
    'art/witch-remake/witch_evolution-legacy-staff.json',
    'art/witch-remake/capture-native-witch.js',
    'art/witch-remake/native-export-audit.json',
    'tools/export-native-witch.py',
    'art/witch-remake/model-provenance.json',
)
NATIVE_CAPTURES = {f'art/witch-remake/blockbench-final-{model}-{view}.png'
                   for model in ('witch', 'witch_evolution')
                   for view in ('front', 'side', 'back', 'three-quarter')}


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def sha(path):
    h = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024*1024), b''):
            h.update(chunk)
    return h.hexdigest()


def read_json(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def project_file(relative):
    path = (P / relative).resolve()
    require(path.is_relative_to(P.resolve()), f'Path escapes project: {relative}')
    require(path.is_file(), f'Missing required file: {relative}')
    return path


def png_size(path):
    data = path.read_bytes()
    require(data[:8] == b'\x89PNG\r\n\x1a\n', f'Invalid PNG: {path}')
    require(len(data) > 100 and b'IEND' in data[-16:], f'Truncated PNG: {path}')
    width, height = struct.unpack('>II', data[16:24])
    require(width >= 640 and height >= 360, f'Unexpected QA image size: {path}')
    return width, height


def audit_build(jar):
    require(BUILD_LOG.is_file(), 'Missing witch-final-build.log; older build logs cannot be substituted')
    build = BUILD_LOG.read_text(encoding='utf-8', errors='replace')
    require('BUILD SUCCESSFUL' in build, 'Final Witch build did not succeed')
    require(re.search(r'All\s+45\s+required tests passed', build),
            'Final Witch build must report all 45 required GameTests passed')
    require('BUILD FAILED' not in build, 'Final build log contains a failed build')
    require(TEST_XML.is_file(), 'Missing current GameTest XML')
    xml = ET.parse(TEST_XML).getroot()
    cases = list(xml.iter('testcase'))
    require(len(cases) == 45, f'Expected 45 XML test cases, found {len(cases)}')
    require(not any(list(xml.iter(tag)) for tag in ('failure', 'error', 'skipped')),
            'GameTest XML contains failure, error or skipped tests')
    require(any('witchtests.' in case.attrib.get('name', '').lower() for case in cases),
            'XML does not include Witch GameTests')
    require(jar.is_file(), f'Missing current build JAR: {jar}')
    require(BUILD_LOG.stat().st_mtime + 2 >= jar.stat().st_mtime,
            'JAR was rebuilt after the final build evidence')
    for folder in ('src/main/java', 'src/gametest/java'):
        for source in (P / folder).rglob('*.java'):
            require(source.stat().st_mtime <= jar.stat().st_mtime + 2,
                    f'Java source changed after the packaged JAR: {source.relative_to(P)}')
    return {'passed': 45, 'xml_sha256': sha(TEST_XML),
            'build_log': BUILD_LOG.name, 'build_log_sha256': sha(BUILD_LOG)}


def audit_jar(jar):
    with zipfile.ZipFile(jar) as archive:
        require(archive.testzip() is None, 'JAR CRC check failed')
        names = set(archive.namelist())
        meta = json.loads(archive.read('fabric.mod.json'))
        require(meta['version'] == VERSION and meta['depends']['minecraft'] == '1.20.1',
                'JAR is not the 1.9.1 Minecraft 1.20.1 release')
        require(set(meta['depends']) == {'fabricloader', 'minecraft', 'java', 'fabric-api'},
                'Standalone release has unexpected mod dependencies')
        excluded = ('furnace_staff', 'irons_spellbooks/', 'neoforge.mods.toml', 'elixir_bucket')
        require(not any(any(token in name for token in excluded) for name in names),
                'Removed Iron spellbook/staff/liquid integration remains in JAR')
        classes = [n for n in names if n.startswith('dev/royalespells/') and n.endswith('.class')]
        require(classes and all(struct.unpack('>H', archive.read(n)[6:8])[0] == 61 for n in classes),
                'Mod classes are not Java 17 bytecode')
        for name in names:
            if name.endswith('.json'):
                json.loads(archive.read(name))
        sounds = json.loads(archive.read('assets/royalespells/sounds.json'))
        sound_count = 0
        for event, description in sounds.items():
            for entry in description.get('sounds', []):
                if isinstance(entry, dict) and entry.get('type') == 'event':
                    continue
                name = entry if isinstance(entry, str) else entry['name']
                domain, sep, path = name.partition(':')
                if not sep:
                    domain, path = 'royalespells', domain
                if domain == 'minecraft':
                    continue
                require(f'assets/{domain}/sounds/{path}.ogg' in names,
                        f'Missing sound resource for {event}: {name}')
                sound_count += 1
        for required in ('witch.png', 'witch_evolution.png'):
            require('assets/royalespells/textures/card/' + required in names,
                    f'Missing registered Witch card texture: {required}')
        nested = [n for n in names if n.startswith('META-INF/jars/') and n.endswith('.jar')]
        require(len(nested) == 1 and 'mixinextras' in nested[0], 'Unexpected nested dependencies')
        with zipfile.ZipFile(io.BytesIO(archive.read(nested[0]))) as dep:
            require(any('license' in n.lower() for n in dep.namelist()),
                    'Bundled MixinExtras lacks its upstream license')
        # Byte comparison binds all current source assets to the actual JAR;
        # merely finding a file of the right name in build/libs is insufficient.
        assets = []
        root = P / 'src/main/resources/assets/royalespells'
        for file in sorted(root.rglob('*')):
            if file.is_file() and file.suffix.lower() in ('.png', '.ogg', '.json', '.fsh', '.vsh'):
                entry = file.relative_to(P / 'src/main/resources').as_posix()
                require(entry in names and archive.read(entry) == file.read_bytes(),
                        f'Current resource differs from built JAR: {entry}')
                assets.append({'path': file.relative_to(P).as_posix(),
                               'sha256': sha(file), 'bytes': file.stat().st_size})
    return assets, len(classes), sound_count


def audit_continuity(log, started):
    """Recompute the client observer's gates from actual-draw CSV samples.

    This verifies the specific observed run, not packet-loss resilience, user
    modpack FPS, or subjective art quality. A PASS line alone is insufficient.
    """
    def f32(value):
        return struct.unpack('>f', struct.pack('>f', value))[0]
    lines = re.findall(r'WITCH_QA_VISUAL_CONTINUITY ([^\r\n]+)', log)
    require(len(lines) == 1, 'Need exactly one actual-draw continuity summary')
    report = dict(re.findall(r'(\w+)=([^\s]+)', lines[0]))
    require(report.get('passed') == 'true' and report.get('scope') == 'actual-rendered-frames',
            'Actual-draw continuity did not pass')
    require(report.get('notNetworkImpairmentTest') == 'true'
            and report.get('visualQualityRequiresHumanReview') == 'true',
            'Continuity summary omitted its verification boundaries')
    require(abs(float(report.get('serverSpeed', 'nan')) - .72) < 1e-8,
            'Continuity run did not use the 1.9.1 projectile speed')
    require(CONTINUITY.is_file(), 'Missing actual-draw witch-visual-continuity.csv')
    require(started - 2 <= CONTINUITY.stat().st_mtime <= PRODUCTION_LOG.stat().st_mtime + 3,
            'Continuity CSV is stale or newer than the production log')
    with CONTINUITY.open(encoding='utf-8-sig', newline='') as source:
        reader = csv.DictReader(source)
        require(reader.fieldnames == ['entity', 'kind', 'frame', 'wallNs', 'worldTick', 'partial',
                                     'x', 'y', 'z', 'shaderTime', 'impactTime', 'impacted',
                                     'serverImpacted', 'receivedImpactGap'],
                'Unexpected continuity CSV schema')
        rows = list(reader)
    require(len(rows) >= 100, 'Too few actual-draw CSV samples')
    previous, first_flight, stationary = {}, {}, {}
    subjects = set()
    counts = dict(samples=len(rows), flightPairs=0, stationaryPairs=0, stairEvents=0,
                  positionSpikes=0, clockBackwards=0, phaseBackwards=0,
                  impactPairs=0, impactClockDiscontinuities=0, nonFinite=0,
                  excludedFrameGaps=0, completionSamples=0)
    max_received_gap = max_visual_join = 0.0
    for raw in rows:
        require(None not in raw and re.fullmatch(r'[0-9a-fA-F-]{36}', raw['entity']) is not None,
                'Invalid actual-draw CSV entity or row')
        row = {'entity': raw['entity'], 'kind': int(raw['kind']), 'frame': int(raw['frame']),
               'wallNs': int(raw['wallNs']), 'worldTick': int(raw['worldTick']),
               **{key: float(raw[key]) for key in ('partial', 'x', 'y', 'z', 'shaderTime', 'impactTime', 'receivedImpactGap')}}
        for key in ('partial', 'shaderTime', 'impactTime'):
            row[key] = f32(row[key])
        require(raw['impacted'] in ('true', 'false') and raw['serverImpacted'] in ('true', 'false'),
                'Invalid visible/server impact state in CSV')
        row['impacted'] = raw['impacted'] == 'true'
        row['serverImpacted'] = raw['serverImpacted'] == 'true'
        require(not row['impacted'] or row['serverImpacted'], 'Visible impact predates server collision')
        require(all(math.isfinite(row[key]) for key in ('partial', 'x', 'y', 'z', 'shaderTime', 'impactTime', 'receivedImpactGap')),
                'Non-finite actual-draw coordinate or time')
        require(row['receivedImpactGap'] >= -1, 'Invalid raw impact distance')
        max_received_gap = max(max_received_gap, row['receivedImpactGap'])
        counts['completionSamples'] += int(row['serverImpacted'] and not row['impacted'])
        entity = row['entity']
        if not row['impacted']:
            first_flight.setdefault(entity, row['shaderTime'])
        old = previous.get(entity)
        if old:
            require(row['frame'] > old['frame'] and row['wallNs'] > old['wallNs'],
                    'Duplicate or reversed frame order in continuity CSV')
            elapsed = f32(row['shaderTime'] - old['shaderTime'])
            step = math.sqrt(sum((row[key] - old[key])**2 for key in ('x', 'y', 'z')))
            if not old['impacted'] and row['impacted']:
                max_visual_join = max(max_visual_join, step)
            if elapsed < f32(-.001):
                counts['clockBackwards'] += 1
            if old['impacted'] and (not row['impacted'] or f32(row['impactTime'] + f32(.001)) < old['impactTime']):
                counts['phaseBackwards'] += 1
            wall_ms = (row['wallNs'] - old['wallNs']) / 1_000_000
            regular = row['frame'] == old['frame'] + 1 and 2 <= wall_ms <= 45
            if not regular:
                counts['excludedFrameGaps'] += 1
            if regular and old['impacted'] and row['impacted'] and elapsed > f32(.035):
                counts['impactPairs'] += 1
                if abs(f32(f32(row['impactTime'] - old['impactTime']) - elapsed)) > f32(.04):
                    counts['impactClockDiscontinuities'] += 1
            flight = (row['kind'] == 0 and not old['impacted'] and not row['impacted']
                      and f32(old['shaderTime'] - first_flight[entity]) >= 1)
            if flight and regular and elapsed > f32(.035):
                counts['flightPairs'] += 1
                subjects.add(entity)
                still = step < .002
                counts['stationaryPairs'] += int(still)
                if stationary.get(entity, False) and step > .72 * .55 and elapsed < .75:
                    counts['stairEvents'] += 1
                if step > .72 * .4 and step / elapsed > .72 * 2.75:
                    counts['positionSpikes'] += 1
                stationary[entity] = still
            else:
                stationary[entity] = False
        previous[entity] = row
    counts['flightSubjects'] = len(subjects)
    require(counts['flightSubjects'] >= 5 and counts['flightPairs'] >= 40 and counts['impactPairs'] >= 100,
            'Insufficient independent flight/impact coverage in actual-draw CSV')
    for key in ('stairEvents', 'positionSpikes', 'clockBackwards', 'phaseBackwards',
                'impactClockDiscontinuities', 'nonFinite'):
        require(counts[key] == 0, f'Actual-draw continuity failed: {key}={counts[key]}')
    ratio = counts['stationaryPairs'] / max(1, counts['flightPairs'])
    require(ratio < .25, f'Flight frames still show excessive stationary steps: {ratio}')
    for key, actual in counts.items():
        require(int(report.get(key, '-1')) == actual,
                f'Continuity log/CSV disagree for {key}: {report.get(key)} vs {actual}')
    require(abs(float(report.get('stationaryRatio', 'nan')) - ratio) < 1e-7,
            'Continuity log/CSV disagree for stationaryRatio')
    for key, value in (('maxReceivedImpactGap', max_received_gap), ('maxFlightToImpactJoin', max_visual_join)):
        require(abs(float(report.get(key, 'nan')) - value) < 1e-7,
                f'Continuity log/CSV disagree for {key}')
    return {'passed': True, 'source': CONTINUITY.relative_to(P).as_posix(),
            'path': 'docs/validation/witch-visual-continuity.csv', 'sha256': sha(CONTINUITY),
            **counts, 'stationary_ratio': ratio, 'server_speed_blocks_per_tick': .72,
            'max_received_impact_gap': max_received_gap, 'max_visible_flight_to_impact_join': max_visual_join,
            'scope': 'Actual GPU submissions in this isolated run; not network impairment or FPS benchmark',
            'summary': lines[0]}


def audit_client(jar_hash):
    tested = PRODUCTION / 'mods' / JAR_NAME
    require(tested.is_file() and sha(tested) == jar_hash,
            'Production Witch client did not use the exact current release JAR')
    log = PRODUCTION_LOG.read_text(encoding='utf-8', errors='replace')
    require('WITCH_CLIENT_QA_TIMEOUT' not in log, 'Witch production QA timed out')
    require('WITCH_VOLUME_SHADER_READY' in log and 'WITCH_VOLUME_ACTIVE' in log,
            'The density shader was not loaded and used by this client')
    complete = re.findall(r'WITCH_CLIENT_QA_COMPLETE frames=(\d+) renderedFrames=(\d+) '
                          r'controlsPassed=(\w+) missing=(\[[^\]]*\]) world=(\S+)', log)
    require(len(complete) == 1, 'Need exactly one complete current Witch QA run')
    count, rendered, control, missing, world = complete[0]
    require(int(count) == len(EXPECTED) == 62 and control == 'true' and missing == '[]',
            f'Incomplete 62-frame Witch QA: {complete[0]}')
    require('WITCH_QA_CONTROL_CHECK passed=true ' in log,
            'Missing explicit current control-isolation/slow/freeze PASS')
    require('WITCH_QA_CONTROL_CHECK passed=false' not in log, 'Control check failed')
    require('All dimensions are saved' in log, 'Production world did not finish saving')
    # Preserve known baseline diagnostics in the evidence. Only these exact
    # messages are permitted, not arbitrary ERROR lines or mod exceptions.
    expected_entities = {'spell', 'baby_zombie', 'barbarian', 'royal_recruit',
        'graveyard_skeleton', 'army_skeleton', 'elemental_spirit', 'inferno_dragon',
        'witch', 'witch_bolt', 'spirit_chain_arc', 'evolution_deployment', 'barbarian_hut'}
    diagnostic_lines = []
    for line in log.splitlines():
        if not re.search(r'\b(?:ERROR|FATAL)\b', line):
            continue
        message = line.split(']: ', 1)[-1]
        data_fixer = re.fullmatch(r'No data fixer registered for royalespells:([a-z_]+)', message)
        known_data_fixer = bool(data_fixer and data_fixer[1] in expected_entities)
        offline_auth = (message == 'Failed to verify authentication'
            and 'Setting user: RoyaleFabricQA' in log
            and 'Failed to verify authentication\ncom.mojang.authlib.exceptions.InvalidCredentialsException: Status: 401' in log)
        require('/ERROR]' in line and (known_data_fixer or offline_auth),
                f'Production log contains an unrecognized error: {line}')
        diagnostic_lines.append(line)
    fatal_patterns = (
        r'Mixin apply failed', r'MixinApplyError',
        r'InvalidMixinException', r'NoClassDefFoundError', r'Exception in server tick loop',
        r'(?i)(?:failed|unable|could not) to (?:load|compile|link).{0,80}shader',
        r'(?i)shader.{0,80}(?:compilation failed|link failed|compile error|link error)',
    )
    for pattern in fatal_patterns:
        require(not re.search(pattern, log), f'Production log contains blocking error: {pattern}')
    render_names = re.findall(r'WITCH_QA_FRAME_RENDER name=([a-z0-9-]+)\s', log)
    callbacks = re.findall(r'WITCH_QA_FRAME ([a-z0-9-]+) ([^\r\n]+)', log)
    require(len(render_names) == 62 and set(render_names) == EXPECTED,
            'Current log lacks exactly the 62 required rendered frames')
    require(len(callbacks) == 62 and {name for name, _ in callbacks} == EXPECTED,
            'Current log lacks exactly the 62 screenshot callbacks')
    for name, message in callbacks:
        require(f'witch19-{name}.png' in message,
                f'Screenshot callback does not confirm expected saved file: {name}')
    timestamp = re.fullmatch(r'Witch-1\.9\.1-QA-remake-(\d{13})', world)
    require(timestamp is not None, 'QA world lacks its run-start timestamp')
    started = int(timestamp.group(1)) / 1000
    continuity = audit_continuity(log, started)
    # Gradle may remap byte-identical output again after a log-only rerun.
    # Bind evidence to the unchanged installed QA copy and the exact SHA-256
    # already checked above, not to build/libs' incidental remap timestamp.
    require(tested.stat().st_mtime <= started + 2,
            'The actual tested JAR was replaced after this production run began')
    images = []
    for name in sorted(EXPECTED):
        file = PRODUCTION / 'screenshots' / f'witch19-{name}.png'
        require(file.is_file(), f'Missing captured PNG: {file.name}')
        require(started - 2 <= file.stat().st_mtime <= PRODUCTION_LOG.stat().st_mtime + 3,
                f'Screenshot is stale or newer than the current QA evidence: {file.name}')
        width, height = png_size(file)
        images.append({'path': 'docs/validation/' + file.name, 'sha256': sha(file),
                       'width': width, 'height': height, 'source': file.relative_to(P).as_posix()})
    warnings = [line for line in log.splitlines() if 'WARN' in line and 'shader' in line.lower()]
    controls = next(line.split('WITCH_QA_CONTROL_CHECK ', 1)[1]
                    for line in log.splitlines() if 'WITCH_QA_CONTROL_CHECK passed=true ' in line)
    return images, {'world': world, 'rendered_frames': int(rendered), 'capture_count': 62,
                    'callback_count': 62, 'controls_passed': True, 'control_evidence': controls,
                    'actual_draw_continuity': continuity,
                    'world_saved': True, 'shader_errors_found': False,
                    'shader_warning_lines': warnings,
                    'allowed_baseline_diagnostics': diagnostic_lines,
                    'diagnostic_scope': 'Offline QA authentication 401 and exact custom-entity DataFixer registration messages only; not proof of old-save migration.',
                    'log_sha256': sha(PRODUCTION_LOG)}


def audit_review(jar_hash, images):
    require(REVIEW.is_file(), 'Missing docs/validation/witch-visual-review.json; capture PASS is not visual review')
    review = read_json(REVIEW)
    require(review.get('status') == 'reviewed', 'Visual review status must be reviewed')
    require(review.get('release') == VERSION, 'Visual review is not explicitly for 1.9.1')
    require(str(review.get('jar_sha256', '')).lower() == jar_hash,
            'Visual review is for a different JAR')
    selected = review.get('reviewed_images')
    require(isinstance(selected, list) and selected, 'Visual review has no reviewed_images')
    by_name = {Path(image['path']).name: image for image in images}
    reviewed = []
    for entry in selected:
        path = entry if isinstance(entry, str) else entry.get('path', '') if isinstance(entry, dict) else ''
        require(isinstance(path, str) and path, 'Invalid reviewed_images entry')
        name = path.replace('\\', '/').rsplit('/', 1)[-1]
        require(name in by_name, f'Reviewed image is not from current 62-frame set: {path}')
        require(isinstance(entry, dict) and str(entry.get('sha256', '')).lower() == by_name[name]['sha256'],
                f'Visual review image hash is absent or stale: {path}')
        reviewed.append(by_name[name]['path'])
    essential = {'face-close', 'profile-and-grip', 'walk-stride-a', 'walk-stride-b',
                 'normal-three-quarter', 'evolved-three-quarter', 'close-side-flight',
                 'close-side-impact-expansion', 'stone-occlusion-impact-expansion'}
    require({f'docs/validation/witch19-{name}.png' for name in essential} <= set(reviewed),
            'Visual review omits a required new-model, gait, VFX or occlusion view')
    return {'status': 'reviewed', 'record_sha256': sha(REVIEW),
            'record': REVIEW.relative_to(P).as_posix(), 'reviewed_images': reviewed,
            'quality_verdict': 'See the review record; this script does not judge visual quality',
            'capture_pass_is_not_art_acceptance': True}


def audit_art():
    for name in WITCH_ART:
        project_file(name)
    provenance = read_json(ART_PROVENANCE)
    require(provenance.get('version') == VERSION, 'Witch native model provenance is not 1.9.1')
    tracked = set()
    for entry in provenance.get('files', []):
        file = project_file(entry['path'])
        require(sha(file) == entry['sha256'].lower(), f'Stale model provenance: {entry["path"]}')
        tracked.add(entry['path'])
    required = set(WITCH_ART) - {ART_PROVENANCE.relative_to(P).as_posix()}
    required.update({
                'src/main/resources/assets/royalespells/models/troop/witch.json',
                'src/main/resources/assets/royalespells/models/troop/witch_evolution.json',
                'src/main/resources/assets/royalespells/textures/entity/witch_materials.png'})
    require(required <= tracked, 'Model provenance omits a final native model, exporter, build script or atlas')
    atlas = project_file('src/main/resources/assets/royalespells/textures/entity/witch_materials.png')
    data = atlas.read_bytes()
    require(data[:8] == b'\x89PNG\r\n\x1a\n' and struct.unpack('>II', data[16:24]) == (128, 128),
            'Final Witch atlas must be the native 128x128 pixel-painted atlas')
    for name in ('witch', 'witch_evolution'):
        native = read_json(project_file(f'art/blockbench/{name}.bbmodel'))
        require(native.get('elements') and native.get('outliner') and native.get('textures'),
                f'Native Blockbench source is incomplete: {name}')
        resolution = native.get('resolution', {})
        require(resolution.get('width') == 128 and resolution.get('height') == 128,
                f'Native atlas UV resolution is not 128x128: {name}')
        embedded = []
        for texture in native['textures']:
            source = texture.get('source', '')
            if source.startswith('data:image/png;base64,'):
                embedded.append(base64.b64decode(source.split(',', 1)[1], validate=True))
        require(data in embedded, f'Native source does not embed the current runtime atlas: {name}')
        runtime = read_json(project_file(f'src/main/resources/assets/royalespells/models/troop/{name}.json'))
        def contains_legacy_face(value):
            if isinstance(value, dict):
                return value.get('face_texture') is True or any(contains_legacy_face(child) for child in value.values())
            return isinstance(value, list) and any(contains_legacy_face(child) for child in value)
        require(not contains_legacy_face(runtime), f'Runtime {name} still uses the rejected ImageGen face pass')
    captures = provenance.get('native_editor_captures')
    require(isinstance(captures, list) and len(captures) == 8 and set(captures) == NATIVE_CAPTURES,
            'Need exactly the eight final normal/evolved front/side/back/three-quarter Blockbench captures')
    for relative in captures:
        require(isinstance(relative, str) and relative.startswith('art/witch-remake/blockbench-')
                and relative.endswith('.png') and 'rejected' not in relative.lower() and relative in tracked,
                f'Native editor capture is absent, excluded or not hash-bound: {relative}')
        captured = project_file(relative).read_bytes()
        require(captured[:8] == b'\x89PNG\r\n\x1a\n' and b'IEND' in captured[-16:],
                f'Invalid Blockbench framebuffer capture: {relative}')
    return {'record': ART_PROVENANCE.relative_to(P).as_posix(), 'sha256': sha(ART_PROVENANCE),
            'native_editor_captures': captures, 'atlas_size': [128, 128],
            'runtime_model_and_atlas_hash_bound': True,
            'editor_capture_is_not_game_visual_acceptance': True}


def excluded_source(path):
    rel = path.relative_to(P)
    parts = {part.lower() for part in rel.parts}
    return (bool(parts & {'__pycache__', 'rejected-v1', 'rejected-v2', 'rejected-v3',
                         'baseline-model', 'baseline-vfx', 'baseline-qa', '.git', '.gradle'})
            or tuple(part.lower() for part in rel.parts[:2]) == ('tools', 'blockbench')
            or path.suffix.lower() in {'.pyc', '.pyo'}
            or 'rejected' in path.name.lower()
            or bool(re.search(r'(?i)(?:^|[-_])(mcp-session|mcp-temp|import-payload|bb-import|blockbench-import)(?:[-_.]|$)', path.name)))


def source_files():
    files = set()
    for folder in ('src', 'gradle', 'docs', 'tools'):
        files.update(file for file in (P / folder).rglob('*') if file.is_file() and not excluded_source(file))
    for file in P.iterdir():
        if file.is_file() and (file.suffix in ('.md', '.json', '.gradle', '.properties')
                               or file.name in ('LICENSE', 'gradlew', 'gradlew.bat', '.gitignore')):
            if not excluded_source(file):
                files.add(file)
    # Preserve the prior standalone bundle's editable baseline models.
    files.update((P / 'art/blockbench').glob('*.bbmodel'))
    for name in ('spirit_fire', 'spirit_ice', 'spirit_electro', 'spirit_heal'):
        files.add(project_file(f'art/blockbench-v3/{name}.bbmodel'))
    for name in ('inferno_dragon.bbmodel', 'asset-sources.json', 'texture-prompt.txt', 'inferno-materials.png'):
        files.add(project_file('art/inferno-dragon/' + name))
    for name in ('art/FIREBALL-IMAGEGEN-PROMPT.md', 'art/FIREBALL-PLUME-IMAGEGEN-PROMPT.md',
                 'art/blockbench-v3/IMAGEGEN-PROMPTS.md', 'art/blockbench-v3/PIXEL-FINAL-PROMPTS.md'):
        file = P / name
        if file.is_file():
            files.add(file)
    files.update(project_file(name) for name in WITCH_ART)
    # Only the manifest's final editor captures, never whole remake folders
    # containing rejected versions, QA baselines or transport/session files.
    provenance = read_json(ART_PROVENANCE)
    files.update(project_file(name) for name in provenance['native_editor_captures'])
    require(all(file.is_file() and not excluded_source(file) for file in files),
            'Source selection contains an excluded or missing entry')
    return files


def archive(path, entries):
    names = set()
    with zipfile.ZipFile(path, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as target:
        for file, name in sorted(entries, key=lambda item: item[1]):
            require(name not in names, f'Duplicate ZIP entry: {name}')
            names.add(name)
            item = zipfile.ZipInfo(name, date_time=(2026, 10, 10, 0, 0, 0))
            item.compress_type = zipfile.ZIP_DEFLATED
            item.external_attr = (0o100755 if file.name == 'gradlew' else 0o100644) << 16
            target.writestr(item, file.read_bytes())
    with zipfile.ZipFile(path) as check:
        require(check.testzip() is None, f'Archive CRC failure: {path}')


def find_api(explicit):
    if explicit:
        result = Path(explicit).resolve()
    else:
        cache = Path.home() / '.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/0.92.6+1.20.1'
        candidates = sorted(cache.rglob('fabric-api-0.92.6+1.20.1.jar'))
        require(candidates, 'Fabric API cache missing; supply --fabric-api PATH (no automatic download)')
        require(len({sha(file) for file in candidates}) == 1, 'Ambiguous Fabric API cache hashes')
        result = candidates[0]
    require(result.is_file(), 'Fabric API JAR is missing')
    with zipfile.ZipFile(result) as archive_file:
        meta = json.loads(archive_file.read('fabric.mod.json'))
        require(meta['id'] == 'fabric-api' and meta['version'] == '0.92.6+1.20.1',
                'Installation bundle requires Fabric API 0.92.6+1.20.1')
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--fabric-api', help='Existing local Fabric API 0.92.6+1.20.1 JAR')
    parser.add_argument('--audit-only', action='store_true', help='Read-only gates; write no package or manifests')
    args = parser.parse_args()
    jar = P / 'build/libs' / JAR_NAME
    tests = audit_build(jar)
    jar_hash = sha(jar)
    assets, class_count, sound_count = audit_jar(jar)
    images, client = audit_client(jar_hash)
    visual = audit_review(jar_hash, images)
    art = audit_art()
    api = find_api(args.fabric_api)
    source_files()  # Fail for missing baseline art before writing release outputs.
    record = {'release': VERSION, 'minecraft': '1.20.1', 'loader_family': 'Fabric',
              'fabric_api': '0.92.6+1.20.1', 'jar_sha256': jar_hash,
              'class_major': 61, 'class_count': class_count, 'gametests': tests,
              'isolated_production_client': client, 'visual_review': visual, 'native_model_provenance': art,
              'captures': images, 'runtime_asset_count': len(assets),
              'sound_references': sound_count, 'installed_into_user_instance': False,
              'published_to_github': False,
              'limitations': ['Capture completion is not subjective art acceptance.',
                              'Review conclusions belong to witch-visual-review.json.',
                              'An isolated flat-world QA run is not player-modpack performance or compatibility proof.',
                              'No user save or installed instance was modified by this packaging script.']}
    if args.audit_only:
        print(json.dumps({'audit': 'passed', 'jar_sha256': jar_hash, 'gametests': 45,
                          'screenshots': 62, 'actual_draw_continuity': 'csv-recomputed',
                          'visual_review': 'reviewed', 'wrote_files': False}, indent=2))
        return
    require(not OUT.exists(), f'Release directory already exists; preserve or archive it before repackaging: {OUT}')
    RUNTIME_MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    RUNTIME_MANIFEST.write_text(json.dumps({'release': VERSION, 'jar_sha256': jar_hash,
        'third_party_exclusions': 'See ASSETS.md and LICENSE-NOTICE.md', 'files': assets},
        ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    VERIFY.parent.mkdir(parents=True, exist_ok=True)
    for image in images:
        shutil.copy2(P / image['source'], P / image['path'])
    shutil.copy2(TEST_XML, VERIFY.parent / 'gametest-results.xml')
    shutil.copy2(BUILD_LOG, VERIFY.parent / 'witch-final-build.log')
    shutil.copy2(PRODUCTION_LOG, VERIFY.parent / 'witch-production.log')
    shutil.copy2(CONTINUITY, VERIFY.parent / 'witch-visual-continuity.csv')
    VERIFY.write_text(json.dumps(record, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    OUT.parent.mkdir(parents=True, exist_ok=True)
    # A staging directory prevents a failed ZIP from looking like a completed
    # release. No old release is recursively deleted or overwritten.
    stage = Path(tempfile.mkdtemp(prefix=OUT.name+'.staging-', dir=OUT.parent))
    shutil.copy2(jar, stage / jar.name)
    source = stage / f'royale-spells-{VERSION}-fabric-1.20.1-source.zip'
    archive(source, [(file, f'royale-spells-{VERSION}/'+file.relative_to(P).as_posix())
                     for file in source_files()])
    docs = [(P/name, name) for name in ('README.md', 'LICENSE', 'LICENSE-NOTICE.md',
                                       'ASSETS.md', 'BUILD_STATUS.md', 'CARD-BALANCE.md', 'CHANGELOG.md')]
    docs.extend((file, file.relative_to(P).as_posix()) for file in (P/'docs').rglob('*')
                if file.is_file() and not excluded_source(file))
    docs.extend((file, file.name) for file in P.glob('*.json') if not excluded_source(file))
    bundle = stage / f'royale-spells-{VERSION}-fabric-1.20.1-install.zip'
    archive(bundle, [(jar, 'mods/'+jar.name), (api, 'mods/'+api.name)] + docs)
    for file, relative in docs:
        destination = stage / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(file, destination)
    shutil.copy2(VERIFY, stage / 'verification.json')
    checksums = ''.join(sha(file)+'  '+file.relative_to(stage).as_posix()+'\n'
                        for file in sorted(stage.rglob('*')) if file.is_file())
    (stage/'SHA256SUMS.txt').write_text(checksums, encoding='utf-8')
    stage.rename(OUT)
    print('Local delivery:', OUT)
    print('45 GameTests; 62 saved framebuffer captures; actual-draw CSV recomputed; separate hash-bound visual review present.')
    print('No installation or publication performed. Visual quality conclusions remain in the review record.')


if __name__ == '__main__':
    main()
