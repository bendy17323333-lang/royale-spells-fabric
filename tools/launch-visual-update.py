from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import urllib.request,hashlib,json,shutil,re,subprocess,sys

work=Path(__file__).resolve().parents[2]
install=work/'neoforge-reference/production-launcher'
repo=work/'royale-spells-iron-1.21.1'
version='neoforge-21.1.249'
base=json.loads((install/'versions/1.21.1/1.21.1.json').read_text())
neo=json.loads((install/f'versions/{version}/{version}.json').read_text())
def allowed(rules):
    result=not rules
    for r in rules or []:
        os=r.get('os',{});features=r.get('features',{})
        if os.get('name','windows')!='windows' or os.get('arch','amd64') not in ('amd64','x86_64'):continue
        if any({'has_custom_resolution':True}.get(k,False)!=v for k,v in features.items()):continue
        result=r['action']=='allow'
    return result
libraries={}
for lib in base['libraries']+neo['libraries']:
    if not allowed(lib.get('rules')):continue
    coord=lib['name'].split(':');key=tuple(coord[:2]+coord[3:]);libraries[key]=lib
cache={}
for p in Path('C:/Users/BliBe/.gradle/caches/modules-2/files-2.1').rglob('*.jar'):cache.setdefault(p.name,[]).append(p)
def fetch(lib):
    a=lib['downloads']['artifact'];p=install/'libraries'/a['path']
    def valid(p):return p.exists() and (not a.get('sha1') or hashlib.sha1(p.read_bytes()).hexdigest()==a['sha1'])
    if not valid(p):
        p.parent.mkdir(parents=True,exist_ok=True)
        for candidate in cache.get(p.name,[]):
            if valid(candidate):shutil.copy2(candidate,p);break
        else:
            if not a.get('url'):raise RuntimeError(f'Installer did not produce {p}')
            p.write_bytes(urllib.request.urlopen(a['url']).read())
        if not valid(p):raise RuntimeError(f'Checksum failed: {p}')
    return p
with ThreadPoolExecutor(max_workers=8) as pool:paths=list(pool.map(fetch,libraries.values()))
client=install/f'versions/{version}/{version}.jar'
if not client.exists():shutil.copy2(install/'versions/1.21.1/1.21.1.jar',client)
paths.append(client)
mode=sys.argv[1] if len(sys.argv)>1 else 'prepare'
if mode=='prepare':print('Production launcher ready; verified libraries:',len(paths)-1);sys.exit()
suffix=sys.argv[2] if len(sys.argv)>2 else ''
assert mode in ('visual',) and re.fullmatch(r'[a-zA-Z0-9_-]*',suffix)
run=repo/('run-production-'+mode+suffix);(run/'mods').mkdir(parents=True,exist_ok=True);(run/'natives').mkdir(exist_ok=True)
mod_version=re.search(r'^mod_version=(.+)$',(repo/'gradle.properties').read_text(),re.M)[1].strip()
jar=repo/f'build/libs/royale-spells-neoforge-1.21.1-{mod_version}.jar';shutil.copy2(jar,run/'mods'/jar.name)
if True:
    for p in Path('C:/Users/BliBe/curseforge/minecraft/Instances/1.21.1/mods').glob('*.jar'):
        if p.name.startswith(('royale-spells-','mineclash-zappies-')):continue
        shutil.copy2(p,run/'mods'/p.name)
    if 'joint' in suffix:
        addon=work/'mineclash-zappies-addon/dist/mineclash-zappies-neoforge-1.21.1-0.1.0.jar'
        shutil.copy2(addon,run/'mods'/addon.name)
(run/'options.txt').write_text('lang:zh_cn\nrenderDistance:6\nsimulationDistance:5\nguiScale:2\nmaxFps:60\ntutorialStep:none\npauseOnLostFocus:false\nonboardAccessibility:false\n',encoding='utf-8')
if 'fabulous' in suffix:
    with (run/'options.txt').open('a',encoding='utf-8') as opts:opts.write('graphicsMode:2\n')
values={'natives_directory':str(run/'natives'),'launcher_name':'ZappiesIsolatedQA','launcher_version':'1',
 'classpath':';'.join(map(str,paths)),'library_directory':str(install/'libraries'),'classpath_separator':';',
 'version_name':version,'auth_player_name':'ZappiesQA','auth_uuid':'b2f3a759fb65444d95d5a7fa88ab604b','auth_access_token':'0','clientid':'','auth_xuid':'',
 'user_type':'legacy','version_type':'release','game_directory':str(run),'assets_root':'C:/Users/BliBe/.gradle/caches/neoformruntime/assets','assets_index_name':base['assetIndex']['id'],
 'resolution_width':'1280','resolution_height':'800'}
def args(seq):
    result=[]
    for a in seq:
        if isinstance(a,dict):
            if not allowed(a.get('rules')):continue
            a=a['value']
        if isinstance(a,str):a=[a]
        result.extend(re.sub(r'\$\{([^}]+)\}',lambda m:values[m[1]],v) for v in a)
    return result
params=['-Xmx3G','-Droyalespells.visualSmoke=true']
# Fireball review captures a small fixed set of screenshots, never video/audio.
params+=['-Droyalespells.fireballSmoke=true' if 'fireball' in suffix else '-Droyalespells.snowballSmoke=true']
if mode=='combat':params+=['-Dzappiesaddon.electricalProbe=true']
if mode=='battle':params+=['-Dzappiesaddon.battle='+('dragon' if 'dragon' in suffix else 'pekka')]
params+=args(base['arguments']['jvm'])+args(neo['arguments']['jvm'])+[neo['mainClass']]+args(base['arguments']['game'])+args(neo['arguments']['game'])
argfile=run/'launch-args.txt';argfile.write_text('\n'.join('"'+s.replace('\\','/').replace('"','\\"')+'"' for s in params),encoding='utf-8')
print('Launching packaged JAR',hashlib.sha256(jar.read_bytes()).hexdigest(),run,flush=True)
with (run/'console.log').open('w',encoding='utf-8') as log:code=subprocess.call(['C:/Program Files/Java/jdk-21.0.12/bin/java.exe','@'+str(argfile)],cwd=run,stdout=log,stderr=subprocess.STDOUT)
log=(run/'console.log').read_text(encoding='utf-8',errors='replace')
marker='ROYALE_FIREBALL_SMOKE_COMPLETE' if 'fireball' in suffix else 'ROYALE_VISUAL_UPDATE_COMPLETE'
if code or marker not in log:raise RuntimeError(f'Production {mode} QA failed: exit={code}; see {run}/console.log')
print('PRODUCTION_QA_PASSED',mode,run)
