"""Package only the local mod, reproducible source, documentation and evidence.

Never includes user saves, authentication, dependency JARs or decompiled research.
The recording remains separate in dist/videos for the authorized WeChat upload.
"""
from pathlib import Path
import hashlib, json, shutil, struct, zipfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT/'dist/royale-spells-1.7.0-dev.5-local'
EVIDENCE = ROOT/'docs/validation-dev5'
OUT.mkdir(parents=True, exist_ok=True)
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
jar = ROOT/'build/libs/royale-spells-neoforge-1.21.1-1.7.0-dev.5.jar'
client = json.loads((EVIDENCE/'client-verification.json').read_text(encoding='utf-8'))
assert client['final_jar']['sha256'] == sha(jar)

results = {}
for kind, directory, count in [('standalone', 'run-gametest', 88), ('joint', 'run-compat-gametest', 154)]:
    path = ROOT/directory/'gametest-results.xml'
    root = ET.parse(path).getroot()
    cases = list(root.iter('testcase'))
    assert len(cases) == count, (kind, len(cases))
    assert not any(list(c) for c in cases), 'Failed or skipped tests are not a pass'
    results[kind] = {'passed': len(cases), 'failed': 0, 'sha256': sha(path)}
    shutil.copyfile(path, EVIDENCE/(kind+'-final.xml'))
(EVIDENCE/'server-verification.json').write_text(json.dumps(results, indent=2), encoding='utf-8')

png = ROOT/'src/main/resources/assets/royalespells/textures/entity/fireball_core.png'
raw = png.read_bytes()
assert raw[:8] == b'\x89PNG\r\n\x1a\n'
w, h = struct.unpack('>II', raw[16:24])
asset = {'path': str(png.relative_to(ROOT)).replace('\\', '/'), 'sha256': sha(png),
         'width': w, 'height': h, 'tool': 'image_gen.imagegen',
         'post_processing': 'none; original PNG bytes copied unchanged',
         'prompt': 'art/FIREBALL-IMAGEGEN-PROMPT.md'}
(EVIDENCE/'asset-verification.json').write_text(json.dumps(asset, indent=2), encoding='utf-8')
for name in ['client-verification.json', 'server-verification.json', 'asset-verification.json']:
    shutil.copyfile(EVIDENCE/name, OUT/name)
for name in ['CHANGELOG-1.7.0-dev.5.md', 'ASSETS.md']:
    shutil.copyfile(ROOT/name, OUT/name)
shutil.copyfile(jar, OUT/jar.name)
video = ROOT/'dist/videos/Royale-Spells-dev5-Snowball-Fireball.mp4'
shutil.copyfile(ROOT/'run-production-visualsolo4/video-report.json', EVIDENCE/'video-report.json')
shutil.copyfile(ROOT/'run-production-visualsolo4/visual-scenes.json', EVIDENCE/'video-scenes.json')

sources = OUT/'royale-spells-1.7.0-dev.5-source.zip'
trees = ['src', 'gradle', 'tools', 'docs', 'art']
excluded = {'__pycache__', '.git', '.gradle', 'research', 'decompiled', 'mineclash-reference', 'vendor'}
files = []
for folder in trees:
    for p in (ROOT/folder).rglob('*'):
        if p.is_file() and not (set(p.relative_to(ROOT).parts) & excluded) and p.suffix.lower() not in {'.jar', '.zip', '.mp4', '.pyc'}:
            files.append(p)
for p in ROOT.iterdir():
    if p.is_file() and (p.suffix.lower() in {'.md', '.json', '.gradle', '.properties'} or p.name in {'gradlew', 'gradlew.bat', '.gitignore', 'LICENSE'}):
        files.append(p)
wrapper = ROOT/'gradle/wrapper/gradle-wrapper.jar'
if wrapper.exists(): files.append(wrapper)
with zipfile.ZipFile(sources, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as z:
    for p in sorted(set(files)):
        z.write(p, p.relative_to(ROOT).as_posix())
with zipfile.ZipFile(sources) as z:
    assert z.testzip() is None
    assert 'src/main/resources/assets/royalespells/textures/entity/fireball_core.png' in z.namelist()

manifest = {'version': '1.7.0-dev.5', 'minecraft': '1.21.1', 'loader': 'NeoForge',
            'github_published': False, 'installed_to_user_instance': False,
            'tests': results, 'files': [
                {'file': str(p.relative_to(ROOT/'dist')).replace('\\','/'), 'bytes': p.stat().st_size, 'sha256': sha(p)}
                for p in [OUT/jar.name, sources, video]]}
(OUT/'delivery-manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf-8')
(OUT/'SHA256SUMS.txt').write_text('\n'.join(x['sha256']+'  '+x['file'] for x in manifest['files'])+'\n', encoding='utf-8')
(OUT/'使用说明.txt').write_text('皇室法术 1.7.0-dev.5，本地视觉更新。\n适用于 Minecraft 1.21.1 / NeoForge / Java 21。\n\n新增：雪球蓝色与 65% 动画速度、朝向修复、火球核心和拖尾重做、雪球爆散、毒药雾气。\n安装：完全退出游戏，备份 mods 中旧的皇室法术 JAR，再放入本目录的新 JAR；同一实例只保留一份皇室法术。保留原有铁魔法及相关依赖。\n本轮没有替换用户现有实例，也没有上传 GitHub。源码、素材来源、技术档案和测试记录已保存。\n视频在上一级 videos/Royale-Spells-dev5-Snowball-Fireball.mp4。\n', encoding='utf-8')
print(json.dumps(manifest, indent=2))
