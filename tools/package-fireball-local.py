# SPDX-License-Identifier: MIT
"""Verify the dev.6 artifact and package local source, notices and screenshot evidence.

This tool does not publish, install, record media, send messages or control power.
"""
from pathlib import Path
import hashlib
import json
import shutil
import struct
import zipfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
VERSION = '1.7.0-dev.6'
OUT = ROOT / 'dist' / f'royale-spells-{VERSION}-local'
EVIDENCE = ROOT / 'docs/validation-dev6'
JAR = ROOT / 'build/libs' / f'royale-spells-neoforge-1.21.1-{VERSION}.jar'
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()

def save(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')

OUT.mkdir(parents=True, exist_ok=True)
EVIDENCE.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(JAR) as z:
    assert z.testzip() is None
    for name in ['LICENSE', 'LICENSE-NOTICE.md', 'ASSETS.md']:
        assert z.read(name) == (ROOT/name).read_bytes(), name
    metadata = z.read('META-INF/neoforge.mods.toml').decode()
    assert 'license="MIT"' in metadata and f'version="{VERSION}"' in metadata
    current_entries = {n: hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist() if not n.endswith('/')}

report = ROOT/'run-gametest/gametest-results.xml'
cases = list(ET.parse(report).getroot().iter('testcase'))
assert len(cases) == 88 and not any(list(case) for case in cases)
assert 'All 88 required tests passed' in (ROOT/'fireball-dev6-validation.log').read_text(encoding='utf-8')
shutil.copyfile(report, EVIDENCE/'standalone.xml')
save(EVIDENCE/'server-verification.json', {'version': VERSION, 'passed': 88, 'failed': 0,
    'report_sha256': sha(report), 'jar_sha256': sha(JAR),
    'scope': 'Standalone GameTests rerun on dev.6. Prior dev.5 joint tests are not claimed as a new run.'})

client = {'version': VERSION, 'final_jar_sha256': sha(JAR), 'runs': []}
for suffix in ['fireball6d', 'fireball6fabulous']:
    run = ROOT/('run-production-visual'+suffix)
    # Java on this Windows host can mix localized legacy-codepage diagnostics
    # into the log. Verification markers are ASCII and remain byte-stable.
    log = (run/'console.log').read_text(encoding='utf-8', errors='replace')
    assert 'ROYALE_FIREBALL_SMOKE_COMPLETE screenshots_only=true' in log
    assert log.count('FIREBALL_IMPACT_PARTICLES smoke=18 embers=48') == 3
    assert 'FIREBALL_SERVER_DAMAGE hp=264.0' in log
    assert not (run/'visual-recording').exists()
    tested = run/'mods'/JAR.name
    with zipfile.ZipFile(tested) as z:
        entries = {n: hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist() if not n.endswith('/')}
    changes = [n for n in set(entries)|set(current_entries) if entries.get(n) != current_entries.get(n)]
    # Documentation changed after the normal-graphics screenshots. No runtime
    # or texture difference is allowed to be called the same tested build.
    assert set(changes) <= {'ASSETS.md'}, changes
    shots = sorted((run/'screenshots').glob('fireball-dev6-*.png'))
    assert len(shots) == 19
    client['runs'].append({'directory': run.name, 'jar_sha256': sha(tested),
        'differences_from_final_jar': sorted(changes), 'impacts': 3, 'target_health_before': 300,
        'target_health_after': 264, 'screenshots': [{'name': p.name, 'sha256': sha(p)} for p in shots],
        'graphics_configuration': 'fabulous' if 'fabulous' in suffix else 'default',
        'console_sha256': sha(run/'console.log')})
    (EVIDENCE/(suffix+'-markers.txt')).write_text('\n'.join(line for line in log.splitlines()
        if 'FIREBALL_' in line or 'OpenGL Renderer:' in line)+'\n', encoding='utf-8')
client['visual_review'] = {'reviewer': 'Codex screenshot inspection',
    'observations': ['Filled curved plume in both horizontal directions and vertical descent',
        'Brief irregular combustion followed by sparks and ascending charcoal smoke',
        'No orange ground ring, no hollow ribbons, no rectangular texture background'],
    'limits': 'Isolated world with installed-pack dependencies; no external shader-pack or user-world acceptance claim.'}
save(EVIDENCE/'client-verification.json', client)

plume = ROOT/'src/main/resources/assets/royalespells/textures/entity/fireball_plume.png'
raw = plume.read_bytes()
assert raw[:8] == b'\x89PNG\r\n\x1a\n'
w, h = struct.unpack('>II', raw[16:24])
save(EVIDENCE/'asset-verification.json', {'path': plume.relative_to(ROOT).as_posix(),
    'sha256': sha(plume), 'width': w, 'height': h, 'tool': 'image_gen.imagegen',
    'post_processing': 'none; original PNG and alpha copied unchanged',
    'prompt': 'art/FIREBALL-PLUME-IMAGEGEN-PROMPT.md'})

for name in ['LICENSE', 'LICENSE-NOTICE.md', 'ASSETS.md', 'CHANGELOG-1.7.0-dev.6.md']:
    shutil.copyfile(ROOT/name, OUT/name)
for p in EVIDENCE.glob('*.json'):
    shutil.copyfile(p, OUT/p.name)
shutil.copyfile(JAR, OUT/JAR.name)
preview = OUT/'screenshots'
preview.mkdir(exist_ok=True)
for tick, name in [(49,'fireball-flight'), (149,'fireball-opposite-flight'), (239,'fireball-vertical'),
                   (59,'fireball-impact'), (77,'fireball-smoke'), (110,'fireball-dissipated')]:
    shutil.copyfile(ROOT/f'run-production-visualfireball6fabulous/screenshots/fireball-dev6-{tick}.png', preview/(name+'.png'))

sources = OUT/f'royale-spells-{VERSION}-source.zip'
excluded = {'__pycache__', '.git', '.gradle', 'research', 'decompiled', 'mineclash-reference', 'vendor'}
files = []
for folder in ['src', 'gradle', 'tools', 'docs', 'art']:
    for p in (ROOT/folder).rglob('*'):
        if p.is_file() and not (set(p.relative_to(ROOT).parts) & excluded) and p.suffix.lower() not in {'.jar', '.zip', '.mp4', '.pyc'}:
            files.append(p)
for p in ROOT.iterdir():
    if p.is_file() and (p.suffix.lower() in {'.md', '.json', '.gradle', '.properties'}
                       or p.name in {'gradlew', 'gradlew.bat', '.gitignore', 'LICENSE'}):
        files.append(p)
files.append(ROOT/'gradle/wrapper/gradle-wrapper.jar')
with zipfile.ZipFile(sources, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as z:
    for p in sorted(set(files)):
        z.write(p, p.relative_to(ROOT).as_posix())
with zipfile.ZipFile(sources) as z:
    assert z.testzip() is None
    for n in ['LICENSE', 'LICENSE-NOTICE.md', 'ASSETS.md', plume.relative_to(ROOT).as_posix()]:
        assert n in z.namelist()
    assert not any(n.startswith(('run', 'test-deps', 'research/', '.git/')) for n in z.namelist())

manifest = {'version': VERSION, 'minecraft': '1.21.1', 'loader': 'NeoForge',
    'gameplay_update_published': False, 'installed_to_user_instance': False,
    'code_license_published': True, 'license': 'MIT', 'third_party_assets_excluded': True,
    'screenshots_only': True, 'files': [{'name': p.name, 'bytes': p.stat().st_size, 'sha256': sha(p)}
        for p in [OUT/JAR.name, sources]]}
save(OUT/'delivery-manifest.json', manifest)
(OUT/'SHA256SUMS.txt').write_text('\n'.join(x['sha256']+'  '+x['name'] for x in manifest['files'])+'\n', encoding='utf-8')
(OUT/'使用说明.txt').write_text('皇室法术 1.7.0-dev.6，本地火球视觉更新。\n'
    '适用于 Minecraft 1.21.1 / NeoForge / Java 21；保留原有铁魔法及依赖。\n'
    '完全退出 Minecraft 后备份旧版皇室法术 JAR，再放入本目录的新 JAR。同一实例只保留一份。\n'
    '本次尚未替换用户实例；火球游戏更新未上传 GitHub。许可证已同步四个 GitHub 分支。\n'
    '自编代码采用 MIT，第三方卡图、声音及 MineClash 改编资产明确排除；见 LICENSE-NOTICE.md 和 ASSETS.md。\n'
    '源码包供开发阅读，不要放入 mods；screenshots 是本版的实际客户端截图。\n'
    '验证：88 项独立 GameTest 通过；隔离客户端两种画质配置各三次火球命中扣血 36；不代表所有光影和整合包均验证。\n', encoding='utf-8')
print(json.dumps(manifest, ensure_ascii=False, indent=2))
