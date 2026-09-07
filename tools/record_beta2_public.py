"""Record the exact tested public Beta 2 JAR, original screenshots and asset sources."""
from pathlib import Path
import hashlib, json, re, shutil, struct, zipfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
VERSION = '1.6.1-beta.2'
RUN = ROOT / 'run-production-spirit-beta2-verified'
OUT = ROOT / 'docs/beta2-public'
ASSETS = ROOT / 'src/main/resources/assets/royalespells'

def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def record(p): return dict(path=p.relative_to(ROOT).as_posix(), sha256=sha(p), bytes=p.stat().st_size)
def dump(p, obj): p.write_text(json.dumps(obj, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')

def main():
    jar = ROOT / f'build/libs/royale-spells-neoforge-1.21.1-{VERSION}.jar'
    assert sha(jar) == sha(RUN/'mods'/jar.name), 'Current JAR was not the tested JAR'
    log = (RUN/'console.log').read_text(encoding='utf-8', errors='replace')
    required = ['ROYALE_SPIRIT_CLIENT_COMPLETE models=4', 'ROYALE_SPIRIT_NATIVE_LIVE_HIT health=89.0',
        'ROYALE_SPIRIT_ORIGINAL_CARDS portraits=4 native=1 ratio=302/363',
        'ROYALE_SPIRIT_LIVE_CHAIN victims=9 arcs=9', 'ROYALE_SPIRIT_LIVE_ARMY damageAfterConversion=',
        'ROYALE_BETA2_SURVIVORS living=7 ghosts=0', 'ROYALE_BETA2_FOUR_LIVE_HITS count=4',
        'ROYALE_BETA2_AUDIO_PLAYED events=', '2048x1024x4 minecraft:textures/atlas/blocks.png-atlas']
    for marker in required: assert marker in log, marker
    for bad in ['ROYALE_SPIRIT_CLIENT_FAILURE', 'ROYALE_SPIRIT_CLIENT_TIMEOUT', 'Missing furnace sprite source',
                'Unable to prepare furnace sprite', 'Untranslated spirit spell name']:
        assert bad not in log, bad
    assert not any('Missing textures' in line and 'royalespells' in line for line in log.splitlines())
    played = next(line for line in log.splitlines() if line.startswith('ROYALE_BETA2_AUDIO_PLAYED'))
    events = sorted(set(re.findall(r'spirit_(?:fire|ice|electro|heal)_(?:deploy|jump|impact|step)', played)))
    assert len(events) == 16, events
    OUT.mkdir(parents=True, exist_ok=True)
    tests = {}
    for mode, folder in [('standalone', 'run-gametest'), ('iron', 'run-compat-gametest')]:
        src = ROOT/folder/'gametest-results.xml'
        tree = ET.parse(src); cases = tree.findall('.//testcase')
        assert len(cases) == (73 if mode == 'standalone' else 132)
        assert not tree.findall('.//failure') and not tree.findall('.//error')
        shutil.copy2(src, OUT/(mode+'-gametests.xml'))
        tests[mode] = dict(passed=len(cases), failed=0, report=record(OUT/(mode+'-gametests.xml')))
    shots, motion = [], []
    for path in sorted((RUN/'screenshots').glob('*.png')):
        data = path.read_bytes(); assert data.startswith(b'\x89PNG') and len(data) > 50000
        size = struct.unpack('>II', data[16:24]); assert size == (1600, 1000), path.name
        folder = 'chain-frames' if '-chain-motion-' in path.name else 'screenshots'
        dest = OUT/folder/path.name; dest.parent.mkdir(exist_ok=True); shutil.copy2(path, dest)
        (motion if folder == 'chain-frames' else shots).append(record(dest) | dict(size=list(size)))
    assert len(shots) >= 27 and len(motion) == 36
    for src, dest in [(RUN/'console.log', 'client.log'), (ROOT/'build-beta2-public.log', 'build.log'),
                     (ROOT/'gametest-beta2-public.log', 'standalone.log'),
                     (ROOT/'gametest-compat-beta2-public.log', 'iron.log')]:
        shutil.copy2(src, OUT/dest)
    cards = json.loads((ROOT/'SPIRIT-CARD-SOURCES.json').read_text(encoding='utf-8'))
    for card in cards: assert sha(ROOT/card['path']) == card['sha256']
    sources_path = ROOT/'SPIRIT-ASSET-SOURCES.json'
    sources = json.loads(sources_path.read_text(encoding='utf-8'))
    sources['version'] = VERSION; sources['icons'] = cards
    report = json.loads((ROOT/'art/blockbench-v3/export-report.json').read_text(encoding='utf-8'))
    projects = []
    for name, metrics in report.items():
        path = ROOT/'art/blockbench-v3'/f'{name}.bbmodel'
        assert sha(path) == metrics['source_sha256']
        project = json.loads(path.read_text(encoding='utf-8'))
        assert all(t['source'].startswith('data:') for t in project['textures'])
        projects.append(record(path) | metrics)
    assert len(projects) == 8; sources['blockbench_projects'] = projects
    sources['public_beta2_changes'] = 'Electro spirit and staff: three chamfered purple tufts, no floating horizontal ornament, longer lightning moustache. Original-category phase sounds imported with pinned provenance.'
    for asset in sources['current_generated_assets']: assert sha(ROOT/asset['path']) == asset['sha256']
    with zipfile.ZipFile(jar) as z:
        assert z.testzip() is None
        paths = [ASSETS/f'textures/gui/spell_icons/summon_{k}_spirit.png' for k in ('fire','ice','electro','heal')]
        paths += [ASSETS/f'models/troop/spirit_{k}.json' for k in ('fire','ice','electro','heal')]
        paths += [ASSETS/f'models/item/furnace_staff_{k}.obj' for k in ('fire','ice','electro','heal')]
        paths += [ROOT/a['path'] for a in sources['current_generated_assets']]
        paths += [ASSETS/'model_credits.txt', ASSETS/'sounds.json', ASSETS/'spell_audio.json',
                  ASSETS/'lang/zh_cn.json', ASSETS/'lang/en_us.json']
        tracks = sorted((ASSETS/'sounds/spirits').rglob('*.ogg')); assert len(tracks) == 22
        paths += tracks
        for p in paths: assert z.read(p.relative_to(ROOT/'src/main/resources').as_posix()) == p.read_bytes(), p
        for name in ['entity/SpiritArc', 'client/SpiritArcRenderer', 'army/ArmyCombatGoal', 'test/SpiritBeta2Tests']:
            assert f'dev/royalespells/{name}.class' in z.namelist()
    dump(sources_path, sources)
    dump(OUT/'test-results.json', dict(version=VERSION, jar=record(jar),
        sources_jar=record(jar.with_name(jar.stem+'-sources.jar')), tests=tests,
        runtime=dict(minecraft='1.21.1', neoforge='21.1.249', java='21.0.12', exit_code=0,
                     run_directory=RUN.relative_to(ROOT).as_posix(), shader_pack=False),
        mods=[record(p) for p in sorted((RUN/'mods').glob('*.jar'))],
        audio=dict(actual_source_events=events, original_tracks=22, attenuation_distance=64),
        markers=[line for line in log.splitlines() if line.startswith(('ROYALE_SPIRIT_', 'ROYALE_BETA2_'))],
        screenshots=shots, chain_frames=motion,
        limits=['No user-instance installation or user-world access', 'No full modpack/shader/multiplayer-latency coverage',
                'Audio channel playback confirms runtime loading, not subjective loudness approval']))
    print(json.dumps(dict(jar=sha(jar), screenshots=len(shots), chain_frames=len(motion), audio_events=len(events),
                         tests={k:v['passed'] for k,v in tests.items()})))

if __name__ == '__main__': main()
