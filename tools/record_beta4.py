"""Record verified local Beta 4 artifacts, unaltered screenshots, and original-card provenance."""
from pathlib import Path
import hashlib, json, re, shutil, struct, zipfile
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
VERSION='1.6.0-beta.4'
RUN=ROOT/'run-production-spirit-beta4-verified'
OUT=ROOT/'docs/beta4'
ASSETS=ROOT/'src/main/resources/assets/royalespells'

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def record(p):return dict(path=p.relative_to(ROOT).as_posix(),sha256=sha(p),bytes=p.stat().st_size)
def dump(p,data):p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def main():
    jar=ROOT/f'build/libs/royale-spells-neoforge-1.21.1-{VERSION}.jar'
    assert sha(jar)==sha(RUN/'mods'/jar.name),'Current JAR was not the one tested'
    log=(RUN/'console.log').read_text(encoding='utf-8',errors='replace')
    for marker in ['ROYALE_SPIRIT_CLIENT_COMPLETE models=4 leapFrames=10 arcs=9',
                   'ROYALE_SPIRIT_NATIVE_LIVE_HIT health=92.3',
                   'ROYALE_SPIRIT_ORIGINAL_CARDS portraits=4 native=1 ratio=302/363',
                   'ROYALE_SPIRIT_LIVE_CHAIN victims=9 arcs=9','ROYALE_SPIRIT_LIVE_ARMY damageAfterConversion=',
                   '2048x1024x4 minecraft:textures/atlas/blocks.png-atlas']:
        assert marker in log,'Missing required runtime marker: '+marker
    for bad in ['ROYALE_SPIRIT_CLIENT_FAILURE','ROYALE_SPIRIT_CLIENT_TIMEOUT','Missing furnace sprite source','Unable to prepare furnace sprite','Untranslated spirit spell name']:
        assert bad not in log,bad
    assert not any('Missing textures' in l and 'royalespells' in l for l in log.splitlines())
    OUT.mkdir(parents=True,exist_ok=True)
    tests={}
    for mode,count in [('standalone',63),('iron',122)]:
        src=ROOT/('run-gametest' if mode=='standalone' else 'run-compat-gametest')/'gametest-results.xml'
        tree=ET.parse(src);cases=tree.findall('.//testcase')
        assert len(cases)==count and not tree.findall('.//failure') and not tree.findall('.//error')
        shutil.copy2(src,OUT/(mode+'-gametests.xml'));tests[mode]=dict(passed=count,failed=0,report=record(OUT/(mode+'-gametests.xml')))
    shots=[];motion=[]
    for path in sorted((RUN/'screenshots').glob('*.png')):
        data=path.read_bytes();assert data.startswith(b'\x89PNG') and len(data)>50000
        size=struct.unpack('>II',data[16:24]);assert size==(1600,1000),'Unusable screenshot size: '+path.name
        folder='chain-frames' if '-chain-motion-' in path.name else 'screenshots'
        dest=OUT/folder/path.name;dest.parent.mkdir(exist_ok=True);shutil.copy2(path,dest)
        (motion if folder=='chain-frames' else shots).append(record(dest)|dict(size=list(size)))
    assert len(shots)==27 and len(motion)==36
    shutil.copy2(RUN/'console.log',OUT/'client.log');shutil.copy2(ROOT/'build-beta4.log',OUT/'build.log')
    for f in ['gametest-beta4.log','gametest-compat-beta4.log']:shutil.copy2(ROOT/f,OUT/f)
    cards=json.loads((ROOT/'SPIRIT-CARD-SOURCES.json').read_text(encoding='utf-8'))
    for card in cards:assert sha(ROOT/card['path'])==card['sha256']
    sources_path=ROOT/'SPIRIT-ASSET-SOURCES.json';sources=json.loads(sources_path.read_text(encoding='utf-8'))
    sources['version']=VERSION;sources['icons']=cards
    report=json.loads((ROOT/'art/blockbench-v3/export-report.json').read_text(encoding='utf-8'))
    projects=[]
    for name,metrics in report.items():
        path=ROOT/'art/blockbench-v3'/f'{name}.bbmodel';assert sha(path)==metrics['source_sha256']
        project=json.loads(path.read_text(encoding='utf-8'));assert all(t['source'].startswith('data:') for t in project['textures'])
        projects.append(record(path)|metrics)
    assert len(projects)==8;sources['blockbench_projects']=projects
    sources['beta4_changes']='Body-wrapping fire sheets and short electro crest rebuilt in actual Blockbench projects; fire/electro staffs updated. Original Clash Royale portraits replace model-rendered icons.'
    for asset in sources['current_generated_assets']:assert sha(ROOT/asset['path'])==asset['sha256']
    with zipfile.ZipFile(jar) as z:
        assert z.testzip() is None
        paths=[ASSETS/f'textures/gui/spell_icons/summon_{k}_spirit.png' for k in ('fire','ice','electro','heal')]
        paths += [ASSETS/f'models/troop/spirit_{k}.json' for k in ('fire','ice','electro','heal')]
        paths += [ASSETS/f'models/item/furnace_staff_{k}.obj' for k in ('fire','ice','electro','heal')]
        paths += [ROOT/a['path'] for a in sources['current_generated_assets']]
        paths += [ASSETS/'model_credits.txt',ASSETS/'lang/zh_cn.json',ASSETS/'lang/en_us.json']
        for p in paths:assert z.read(p.relative_to(ROOT/'src/main/resources').as_posix())==p.read_bytes(),'Stale JAR resource: '+str(p)
        for name in ['entity/SpiritArc','client/SpiritArcRenderer','army/ArmyCombatGoal']:
            assert f'dev/royalespells/{name}.class' in z.namelist()
    dump(sources_path,sources)
    evidence=dict(version=VERSION,scope='Local only; no GitHub upload, user-instance installation or user-world access.',
        jar=record(jar),sources_jar=record(jar.with_name(jar.stem+'-sources.jar')),
        runtime=dict(minecraft='1.21.1',neoforge='21.1.249',java='21.0.12',exit_code=0,graphics='no shader pack',run_directory=RUN.relative_to(ROOT).as_posix()),
        mods=[record(p) for p in sorted((RUN/'mods').glob('*.jar'))],tests=tests,
        markers=[l for l in log.splitlines() if l.startswith('ROYALE_SPIRIT_')],screenshots=shots,chain_frames=motion,
        rejected_visual_run='run-production-spirit-beta4-final produced 1x1 PNGs; those images are excluded. Minimum-size guard added before rerun.',
        visual_review='Record the human-visible review separately in VALIDATION-1.6.0-beta.4.md; this script verifies file/provenance consistency only.',
        limits=['No full user modpack, shaders or multiplayer-latency validation','No claim of user aesthetic acceptance'])
    dump(OUT/'test-results.json',evidence)
    print('BETA4_EVIDENCE_OK jar='+sha(jar)+' screenshots=27 chain_frames=36 tests=63+122')

if __name__=='__main__':main()
