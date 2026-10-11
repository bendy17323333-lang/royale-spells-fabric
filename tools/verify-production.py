"""Launch the REMAPPED release JAR in an isolated, opt-in Fabric client.
Uses an existing legal Minecraft installation. Never reads login tokens or user saves.
"""
from pathlib import Path
import csv, hashlib, json, os, re, shutil, subprocess, urllib.request, sys, time
sys.stdout.reconfigure(encoding='utf-8', errors='replace')

ROOT=Path(__file__).resolve().parents[1]
INSTALL=Path('C:/Users/BliBe/curseforge/minecraft/Install')
JAVA=Path('C:/Program Files/Java/jdk-17/bin/java.exe')
WITCH='--witch' in sys.argv
RUN=ROOT/('run-production-witch' if WITCH else 'run-production')
for d in ['mods','libraries','natives']: (RUN/d).mkdir(parents=True,exist_ok=True)
base=json.loads((INSTALL/'versions/1.20.1/1.20.1.json').read_text(encoding='utf-8'))
profile=RUN/'fabric-profile.json'
if not profile.exists():
    cached=ROOT/'run-production/fabric-profile.json'
    profile.write_bytes(cached.read_bytes() if cached.exists() else urllib.request.urlopen('https://meta.fabricmc.net/v2/versions/loader/1.20.1/0.16.14/profile/json',timeout=30).read())
fabric=json.loads(profile.read_text(encoding='utf-8'))

def allowed(rules):
    result=not rules
    for rule in rules or []:
        system=rule.get('os',{})
        if system.get('name','windows')!='windows' or system.get('arch','amd64') not in ('amd64','x86_64'):continue
        if any({'has_custom_resolution':True}.get(k,False)!=v for k,v in rule.get('features',{}).items()):continue
        result=rule['action']=='allow'
    return result

libs={}
for lib in base['libraries']+fabric['libraries']:
    if allowed(lib.get('rules')):libs[tuple(lib['name'].split(':')[:2]+lib['name'].split(':')[3:])]=lib
paths=[]
for lib in libs.values():
    a=lib.get('downloads',{}).get('artifact',{})
    group,name,version,*classifier=lib['name'].split(':')
    path=a.get('path',group.replace('.','/')+'/'+name+'/'+version+'/'+name+'-'+version+('-'+classifier[0] if classifier else '')+'.jar')
    local=INSTALL/'libraries'/path
    def valid(p):return p.is_file() and (not a.get('sha1') or hashlib.sha1(p.read_bytes()).hexdigest()==a['sha1'])
    if not valid(local):
        local=RUN/'libraries'/path
        cached=ROOT/'run-production/libraries'/path
        if not valid(local) and valid(cached):
            local.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(cached,local)
        if not valid(local):
            local.parent.mkdir(parents=True,exist_ok=True)
            local.write_bytes(urllib.request.urlopen(a.get('url') or lib.get('url','https://libraries.minecraft.net/')+path,timeout=45).read())
        if not valid(local):raise RuntimeError('Library checksum mismatch: '+path)
    paths.append(local)
paths.append(INSTALL/'versions/1.20.1/1.20.1.jar')
api=next(Path('C:/Users/BliBe/.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/0.92.6+1.20.1').rglob('fabric-api-0.92.6+1.20.1.jar'))
version=re.search(r'^mod_version=(.+)$',(ROOT/'gradle.properties').read_text(),re.M)[1].strip()
jar=ROOT/f'build/libs/royale-spells-fabric-1.20.1-{version}.jar'
for previous in (RUN/'mods').glob('royale-spells-fabric-*.jar'):
    if previous.name!=jar.name:
        backup=RUN/'previous-mods';backup.mkdir(exist_ok=True);shutil.move(str(previous),str(backup/previous.name))
for src in [api,jar]:shutil.copy2(src,RUN/'mods'/src.name)
if WITCH:
    unexpected=[p.name for p in (RUN/'mods').glob('*.jar') if p.name not in (api.name,jar.name)]
    if unexpected:raise RuntimeError('Witch no-external-shader baseline contains extra mods: '+repr(unexpected))
(RUN/'options.txt').write_text('lang:zh_cn\nrenderDistance:6\nsimulationDistance:5\nguiScale:2\nmaxFps:60\ntutorialStep:none\npauseOnLostFocus:false\nonboardAccessibility:false\n',encoding='utf-8')
values={'natives_directory':str(RUN/'natives'),'launcher_name':'RoyaleFabricQA','launcher_version':'1','classpath':';'.join(map(str,paths)),
        'version_name':fabric['id'],'auth_player_name':'RoyaleFabricQA','auth_uuid':'33a9774a2f05426f8aafc517a2b39b39','auth_access_token':'0','clientid':'','auth_xuid':'',
        'user_type':'legacy','version_type':'release','game_directory':str(RUN),'assets_root':str(INSTALL/'assets'),'assets_index_name':base['assetIndex']['id'],
        'resolution_width':'1280','resolution_height':'800'}
def args(seq):
    out=[]
    for arg in seq:
        if isinstance(arg,dict):
            if not allowed(arg.get('rules')):continue
            arg=arg['value']
        for value in ([arg] if isinstance(arg,str) else arg):out.append(re.sub(r'\$\{([^}]+)\}',lambda m:values[m[1]],value))
    return out
arguments=['-Xmx2G','-Droyalespells.'+('witchSmoke' if WITCH else 'visualSmoke')+'=true']+args(base['arguments']['jvm'])+[fabric['mainClass']]+args(base['arguments']['game'])
argfile=RUN/'launch.args'
argfile.write_text('\n'.join('"'+a.replace('\\','/').replace('"','\\"')+'"' for a in arguments),encoding='utf-8')
log=RUN/'production.log'
launched_at=time.time()
with log.open('w',encoding='utf-8') as out:
    proc=subprocess.Popen([str(JAVA),'@'+str(argfile)],cwd=RUN,stdout=out,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
    print('Isolated release client PID',proc.pid,flush=True)
    code=proc.wait(timeout=220 if WITCH else 150)
content=log.read_text(encoding='utf-8',errors='replace')
marker='WITCH_CLIENT_QA_COMPLETE' if WITCH else 'FABRIC18_CLIENT_QA_COMPLETE'
print('Exit',code,'Completion marker',marker in content,flush=True)
if code or marker not in content:
    print('\n'.join(content.splitlines()[-70:]));raise SystemExit(1)
if WITCH:
    # A finished client is not necessarily a passed visual regression. Require
    # all real-frame captures and per-entity slow/freeze checks from this run.
    if 'WITCH_VOLUME_SHADER_READY' not in content or 'WITCH_VOLUME_ACTIVE' not in content:
        raise RuntimeError('The current 3D mist shader did not load and render')
    complete=re.search(r'WITCH_CLIENT_QA_COMPLETE frames=(\d+) renderedFrames=(\d+) controlsPassed=(true|false) missing=\[([^\]]*)\]',content)
    if not complete or complete[3]!='true' or complete[4]:
        print('Witch regression checks failed:',complete.group(0) if complete else 'missing structured completion')
        raise SystemExit(1)
    continuity=re.search(r'WITCH_QA_VISUAL_CONTINUITY passed=(true|false) samples=(\d+)[^\r\n]*',content)
    if not continuity or continuity[1]!='true':
        raise RuntimeError('Rendered position/time continuity failed: '+(continuity.group(0) if continuity else 'missing result'))
    trace=RUN/'witch-visual-continuity.csv'
    if not trace.is_file() or trace.stat().st_mtime<launched_at:
        raise RuntimeError('Missing or stale actual-rendered-frame continuity trace')
    with trace.open(newline='',encoding='utf-8') as source:
        rows=list(csv.DictReader(source))
    if len(rows)!=int(continuity[2]) or not rows or not {'entity','frame','x','y','z','shaderTime','impactTime','impacted','serverImpacted','receivedImpactGap'}.issubset(rows[0]):
        raise RuntimeError('Continuity CSV coverage/header mismatch')
    requested=set(re.findall(r'WITCH_QA_FRAME_RENDER name=([\w-]+)',content))
    saved=set(re.findall(r'WITCH_QA_FRAME ([\w-]+) ',content))
    if saved!=requested or len(requested)!=int(complete[1]):
        raise RuntimeError('Screenshot callback coverage mismatch: '+repr({'requested':len(requested),'saved':len(saved),'missing':sorted(requested-saved)}))
    for name in saved:
        image=RUN/'screenshots'/('witch19-'+name+'.png')
        if not image.is_file() or image.stat().st_size<1000 or image.stat().st_mtime<launched_at:
            raise RuntimeError('Missing, stale or empty real-frame screenshot: '+str(image))
    print('Witch mechanism/capture checks PASS:',len(saved),'real-frame screenshots; slow/recovery; isolated freeze; five attack views, face/grip closeups and natural walking poses.')
    print('Actual-rendered-frame position/time checks PASS:',len(rows),'samples; evidence:',trace)
    print('External shader/Bloom baseline: none. Volume continuity, colour hierarchy, softness and correct visible occlusion still require human screenshot review; they are NOT automatically approved.')
print('Production JAR SHA256',hashlib.sha256(jar.read_bytes()).hexdigest())
