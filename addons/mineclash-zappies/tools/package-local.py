"""Produce local deliverables without dependencies, private research or worlds."""
from pathlib import Path
import hashlib,json,shutil,subprocess,zipfile,xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1];B=ROOT.parent/'royale-spells-iron-1.21.1';OUT=ROOT/'dist';OUT.mkdir(exist_ok=True)
V=ROOT/'docs/validation'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
mods=[ROOT/'build/libs/mineclash-zappies-neoforge-1.21.1-0.1.0.jar',B/'build/libs/royale-spells-neoforge-1.21.1-1.7.0-dev.4.jar']
for p in mods:shutil.copy2(p,OUT/p.name)
reports={}
for kind,run in [('pekka','run-production-battlepackpekka4'),('dragon','run-production-battlepackdragon3')]:
    p=ROOT/run;report=json.loads((p/'video-report.json').read_text());battle=json.loads((p/'battle-evidence.json').read_text())
    report['winner']=battle['winner'];report['combat_ticks']=battle['ticks'];report['opponent_base_health']=battle['opponent_max_health']
    if kind=='dragon':
        paused=[s for s in battle['samples'] if s['opponent_paused']]
        assert paused and all(s['heat']==0 and s['lock']==0 for s in paused)
        report['paused_dragon_samples']=len(paused);report['all_paused_heat_and_lock_zero']=True
    reports[kind]=report;(V/('video-'+kind+'.json')).write_text(json.dumps(report,ensure_ascii=False,indent=2))
    for mod in mods:assert sha(p/'mods'/mod.name)==sha(mod),'Recorded artifact must match delivery'
original=ROOT/'run-production-combatpackfinal7/mods'/mods[0].name
with zipfile.ZipFile(original) as old,zipfile.ZipFile(mods[0]) as new:
    changed=[n for n in set(old.namelist())|set(new.namelist()) if (old.read(n) if n in old.namelist() else None)!=(new.read(n) if n in new.namelist() else None)]
    allowed=('dev/mineclash/zappies/client/Battle','dev/mineclash/zappies/client/ClientSetup')
    assert all(n.startswith(allowed) for n in changed),changed
tests={}
for file in V.glob('*-tests.xml'):
    tree=ET.parse(file);root=tree.getroot();cases=root.findall('.//testcase')
    assert not root.findall('.//failure') and not root.findall('.//error')
    tests[file.name]=len(cases)
assert sorted(tests.values())==[12,83,149],tests
manifest={'versions':{'zappies':'0.1.0','royale':'1.7.0-dev.4'},'jars':{p.name:sha(p) for p in mods},'tests':tests,
 'client_qa_afterwards_changed_only_opt_in_recording':sorted(changed),'videos':reports,'installed_in_user_instance':False,'published_to_github':False}
(V/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2))
# Fixed explicit source roots. In particular no research, libs, run, .gradle or audio extraction directories.
nfiles=[p for folder in ['src','docs','gradle'] for p in (ROOT/folder).rglob('*') if p.is_file()]
nfiles += [ROOT/n for n in ['README.md','build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat','.gitignore'] if (ROOT/n).is_file()]
nfiles += [ROOT/'tools'/n for n in ['launch-client.py','verify-client.py','encode-battle.py','package-local.py']]
with zipfile.ZipFile(OUT/'mineclash-zappies-0.1.0-source.zip','w',zipfile.ZIP_DEFLATED) as z:
    for p in sorted(set(nfiles)):z.write(p,'mineclash-zappies-addon/'+p.relative_to(ROOT).as_posix())
raw=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z'],cwd=B).decode('utf-8')
bfiles=[B/name for name in raw.split('\0') if name and (B/name).is_file()]
with zipfile.ZipFile(OUT/'royale-spells-1.7.0-dev.4-source.zip','w',zipfile.ZIP_DEFLATED) as z:
    for p in sorted(set(bfiles)):
        rel=p.relative_to(B).as_posix()
        assert not rel.startswith(('run-','build/','research/','.gradle/','.git/'))
        z.write(p,'royale-spells-iron-1.21.1/'+rel)
for p in OUT.glob('*.zip'):
    with zipfile.ZipFile(p) as z:assert z.testzip() is None
(OUT/'SHA256SUMS.txt').write_text('\n'.join(sha(p)+'  '+p.name for p in sorted(OUT.iterdir()) if p.is_file() and p.name!='SHA256SUMS.txt')+'\n')
print(json.dumps(manifest,ensure_ascii=False,indent=2))
