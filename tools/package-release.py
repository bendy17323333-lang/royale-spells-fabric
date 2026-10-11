"""Audit release resources and create reproducible, portable local delivery archives."""
from pathlib import Path
import hashlib,io,json,re,shutil,struct,zipfile
import xml.etree.ElementTree as ET

P=Path(__file__).resolve().parents[1]
OUT=P.parents[1]/'releases/royale-spells-1.8.0-fabric-1.20.1'
OUT.mkdir(parents=True,exist_ok=True)
jar=P/'build/libs/royale-spells-fabric-1.20.1-1.8.0.jar'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
assert 'All 37 required tests passed' in (P/'final-build.log').read_text(encoding='utf-8')
log=(P/'run-production/production.log').read_text(encoding='utf-8',errors='replace')
assert 'FABRIC18_CLIENT_QA_COMPLETE' in log and 'All dimensions are saved' in log
assert sha(jar)==sha(P/'run-production/mods'/jar.name),'Production client tested a different release'
for error in ['Mixin apply failed','MixinApplyError','InvalidMixinException','NoClassDefFoundError','Exception in server tick loop']:
    assert error not in log,error

with zipfile.ZipFile(jar) as z:
    names=set(z.namelist());meta=json.loads(z.read('fabric.mod.json'))
    assert meta['version']=='1.8.0' and meta['depends']['minecraft']=='1.20.1'
    assert set(meta['depends'])=={'fabricloader','minecraft','java','fabric-api'}
    assert not any('furnace_staff' in n or 'irons_spellbooks/' in n or 'neoforge.mods.toml' in n or 'elixir_bucket' in n for n in names)
    classes=[n for n in names if n.startswith('dev/royalespells/') and n.endswith('.class')]
    assert classes and all(struct.unpack('>H',z.read(n)[6:8])[0]==61 for n in classes)
    for n in names:
        if n.endswith('.json'):json.loads(z.read(n))
    sounds=json.loads(z.read('assets/royalespells/sounds.json'))
    count=0
    for event,description in sounds.items():
        for entry in description.get('sounds',[]):
            if isinstance(entry,dict) and entry.get('type')=='event':continue
            name=entry if isinstance(entry,str) else entry['name']
            domain,sep,path=name.partition(':')
            if not sep:domain,path='royalespells',domain
            if domain=='minecraft':continue
            assert f'assets/{domain}/sounds/{path}.ogg' in names,(event,name)
            count+=1
    nested=[n for n in names if n.startswith('META-INF/jars/') and n.endswith('.jar')]
    assert len(nested)==1 and 'mixinextras' in nested[0]
    with zipfile.ZipFile(io.BytesIO(z.read(nested[0]))) as dep:
        license_names=[n for n in dep.namelist() if 'license' in n.lower()]
        assert license_names,'Bundled dependency lacks license'
    print('JAR audit: Java17 classes',len(classes),'sound file references',count,'nested license',license_names)

assets=[]
for f in sorted((P/'src/main/resources/assets/royalespells').rglob('*')):
    if f.is_file() and f.suffix in ('.png','.ogg','.json'):
        assets.append({'path':f.relative_to(P).as_posix(),'sha256':sha(f),'bytes':f.stat().st_size})
manifest=P/'docs/reference/runtime-assets-1.8.0.json'
manifest.write_text(json.dumps({'release':'1.8.0','third_party_exclusions':'See ASSETS.md and LICENSE-NOTICE.md','files':assets},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
v=P/'docs/validation';v.mkdir(parents=True,exist_ok=True)
for f in (P/'run-production/screenshots').glob('fabric18-*.png'):shutil.copy2(f,v/f.name)
shutil.copy2(P/'run-gametest/build/gametest-results.xml',v/'gametest-results.xml')
record={'release':'1.8.0','minecraft':'1.20.1','loader':'Fabric 0.16.14','fabric_api':'0.92.6+1.20.1','game_java':'17.0.12','build_java':'21.0.12',
        'jar_sha256':sha(jar),'gametests_passed':37,'isolated_production_client_completed':True,'world_saved':True,
        'installed_into_user_instance':False,'published_to_github':False,'class_major':61,'runtime_asset_count':len(assets),'sound_references':count}
(v/'verification.json').write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

# Archive selected current editable models. Removed staves and historical experiments
# are deliberately not part of the standalone source bundle; runtime model JSON is complete.
files=set()
for folder in ['src','gradle','docs','tools']:
    files.update(f for f in (P/folder).rglob('*') if f.is_file() and '__pycache__' not in f.parts)
for f in P.iterdir():
    if f.is_file() and (f.suffix in ('.md','.json','.gradle','.properties') or f.name in ['LICENSE','gradlew','gradlew.bat','.gitignore']):files.add(f)
files.update((P/'art/blockbench').glob('*.bbmodel'))
for name in ['spirit_fire','spirit_ice','spirit_electro','spirit_heal']:files.add(P/'art/blockbench-v3'/f'{name}.bbmodel')
for name in ['inferno_dragon.bbmodel','asset-sources.json','texture-prompt.txt','inferno-materials.png']:files.add(P/'art/inferno-dragon'/name)
for f in [P/'art/FIREBALL-IMAGEGEN-PROMPT.md',P/'art/FIREBALL-PLUME-IMAGEGEN-PROMPT.md',P/'art/blockbench-v3/IMAGEGEN-PROMPTS.md',P/'art/blockbench-v3/PIXEL-FINAL-PROMPTS.md']:
    if f.is_file():files.add(f)

def archive(path,entries):
    with zipfile.ZipFile(path,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as out:
        for file,name in sorted(entries,key=lambda x:x[1]):
            item=zipfile.ZipInfo(name,date_time=(2026,9,11,0,0,0));item.compress_type=zipfile.ZIP_DEFLATED;item.external_attr=(0o100755 if file.name=='gradlew' else 0o100644)<<16
            out.writestr(item,file.read_bytes())
    with zipfile.ZipFile(path) as out:assert out.testzip() is None
shutil.copy2(jar,OUT/jar.name)
source=OUT/'royale-spells-1.8.0-fabric-1.20.1-source.zip'
archive(source,[(f,'royale-spells-1.8.0/'+f.relative_to(P).as_posix()) for f in files])
api=next(Path('C:/Users/BliBe/.gradle/caches/modules-2/files-2.1/net.fabricmc.fabric-api/fabric-api/0.92.6+1.20.1').rglob('fabric-api-0.92.6+1.20.1.jar'))
bundle=OUT/'royale-spells-1.8.0-fabric-1.20.1-install.zip'
bundle_docs=[(P/name,name) for name in ['README.md','LICENSE','LICENSE-NOTICE.md','ASSETS.md','BUILD_STATUS.md','CARD-BALANCE.md']]
bundle_docs.extend((f,f.relative_to(P).as_posix()) for f in (P/'docs').rglob('*') if f.is_file())
bundle_docs.extend((f,f.name) for f in P.glob('*.json'))
archive(bundle,[(jar,'mods/'+jar.name),(api,'mods/'+api.name)]+bundle_docs)
for name in ['README.md','BUILD_STATUS.md','LICENSE','LICENSE-NOTICE.md','ASSETS.md']:
    shutil.copy2(P/name,OUT/name)
shutil.copy2(v/'verification.json',OUT/'verification.json')
shutil.copytree(P/'docs',OUT/'docs',dirs_exist_ok=True)
shutil.copy2(P/'CARD-BALANCE.md',OUT/'CARD-BALANCE.md')
for f in P.glob('*.json'):shutil.copy2(f,OUT/f.name)
hashes=''.join(sha(f)+'  '+f.name+'\n' for f in sorted(OUT.iterdir()) if f.is_file() and f.name!='SHA256SUMS.txt')
(OUT/'SHA256SUMS.txt').write_text(hashes,encoding='utf-8')
print('Delivery',OUT)
for f in [OUT/jar.name,bundle,source]:print(f.name,f.stat().st_size,sha(f))
