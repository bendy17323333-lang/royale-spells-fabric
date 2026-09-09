"""Package the locally verified Inferno Dragon build; never installs or publishes.

Run from any directory after build, both GameTest suites and the isolated
production client. The final production client must have loaded this exact JAR.
"""
from pathlib import Path
import datetime
import hashlib
import json
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = re.search(r'^mod_version=(.+)$', (ROOT/'gradle.properties').read_text(), re.M)[1].strip()
assert VERSION == '1.7.0-dev.2', 'Select new validation directories before packaging a new version.'
QA = ROOT/'run-production-inferno-dev2-final'
DEST = ROOT/'dist'/f'royale-spells-{VERSION}-local'
EVIDENCE = ROOT/'docs/validation-inferno-dev2'
JAR = ROOT/'build/libs'/f'royale-spells-neoforge-1.21.1-{VERSION}.jar'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


assert JAR.is_file() and sha(JAR) == sha(QA/'mods'/JAR.name), 'Packaged and tested JAR differ.'
console = (QA/'console.log').read_text(encoding='utf-8', errors='replace')
assert 'ROYALE_INFERNO_CLIENT_COMPLETE maxLoops=1' in console
assert 'ROYALE_INFERNO_FAILURE' not in console
assert 'All dimensions are saved' in console
assert 'ROYALE_INFERNO_FLIGHT minClearance=' in console
assert 'ROYALE_INFERNO_FACING maxClientYawError=' in console
sounds = ['inferno_dragon_deploy', 'inferno_dragon_wing', 'inferno_dragon_beam']
for sound in sounds:
    assert 'ROYALE_INFERNO_OPENAL royalespells:'+sound in console

tests = {}
for name, folder, count, log in [
    ('standalone', 'run-gametest', 82, 'run-gametest/logs/latest.log'),
    ('iron_compatibility', 'run-compat-gametest', 143, 'inferno-dev2-iron-tests.log'),
]:
    test_log = (ROOT/log).read_text(encoding='utf-8', errors='replace')
    assert 'BUILD SUCCESSFUL' in test_log or f'All {count} required tests passed' in test_log
    cases = list(ET.parse(ROOT/folder/'gametest-results.xml').iter('testcase'))
    failures = [c.get('name') for c in cases if c.find('failure') is not None or c.find('error') is not None]
    skipped = [c.get('name') for c in cases if c.find('skipped') is not None]
    assert len(cases) == count and not failures and not skipped, (name, len(cases), failures, skipped)
    tests[name] = {'executed': count, 'failures': 0, 'skipped': 0}

subprocess.run(['git', 'diff', '--check'], cwd=ROOT, check=True)
art = ROOT/'art/inferno-dragon'
export = json.loads((art/'export-report.json').read_text())
assert export['source_sha256'] == sha(art/'inferno_dragon.bbmodel')
assert export['runtime_sha256'] == sha(ROOT/'src/main/resources/assets/royalespells/models/troop/inferno_dragon.json')
assets = 'assets/royalespells/'
with zipfile.ZipFile(JAR) as archive:
    assert archive.testzip() is None
    assert len(archive.namelist()) == len(set(archive.namelist()))
    for path in [
        'models/troop/inferno_dragon.json', 'models/item/inferno_dragon.json',
        'textures/entity/inferno_dragon.png', 'textures/card/inferno_dragon.png',
        'textures/gui/spell_icons/inferno_dragon.png', 'model_credits.txt', 'inferno_asset_sources.json',
        *['sounds/original/'+sound+'.ogg' for sound in sounds],
    ]:
        assert archive.read(assets+path) == (ROOT/'src/main/resources'/assets/path).read_bytes(), path
    assert archive.read(assets+'textures/entity/inferno_dragon.png') == (art/'inferno-materials.png').read_bytes()
    assert archive.read(assets+'textures/card/inferno_dragon.png') == archive.read(assets+'textures/gui/spell_icons/inferno_dragon.png')
    for locale in ['zh_cn', 'en_us']:
        language = json.loads(archive.read(assets+f'lang/{locale}.json'))
        assert language['troop.royalespells.inferno_dragon']
    for entry in json.loads((art/'asset-sources.json').read_text(encoding='utf-8')):
        if 'output' in entry:
            assert sha(ROOT/entry['output']) == entry['output_sha256'] == entry['source_sha256']

DEST.mkdir(parents=True, exist_ok=True)
EVIDENCE.mkdir(parents=True, exist_ok=True)
images = ['front-hover', 'lower-wings', 'back-tank', 'beam-low', 'beam-medium', 'beam-hot', 'frozen-beam-stopped', 'resumed']
for name in images:
    path = QA/'screenshots'/f'inferno-{name}.png'
    assert path.read_bytes()[:8] == b'\x89PNG\r\n\x1a\n'
    shutil.copy2(path, EVIDENCE/path.name)
for name, folder in [('standalone', 'run-gametest'), ('iron', 'run-compat-gametest')]:
    shutil.copy2(ROOT/folder/'gametest-results.xml', EVIDENCE/(name+'-gametest.xml'))
(EVIDENCE/'client-checks.txt').write_text('\n'.join(line for line in console.splitlines() if 'ROYALE_INFERNO_' in line)+'\n', encoding='utf-8')
report = {
    'version': VERSION, 'status': 'local development; not published or installed into the user instance',
    'created_utc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
    'minecraft': '1.21.1', 'neoforge': '21.1.249', 'java': '21.0.12', 'iron_spells': '1.21.1-3.16.3',
    'jar': {'name': JAR.name, 'bytes': JAR.stat().st_size, 'sha256': sha(JAR)},
    'tests': tests,
    'production_client': {'exact_jar_hash_match': True, 'max_beam_loops': 1, 'remaining_loops': 0, 'native_scroll_cast': True, 'real_ai_height_and_moving_target_facing': True, 'flight_evidence': [line for line in console.splitlines() if 'ROYALE_INFERNO_FLIGHT' in line or 'ROYALE_INFERNO_FACING' in line], 'freeze_stops_damage_and_audio': True, 'openal_sources': sounds, 'screenshots': ['inferno-'+name+'.png' for name in images], 'saved_and_exited': True},
    'model': export,
    'limits': ['The two GameTest suites share common cases.', 'The isolated client does not represent the full user modpack, shaders or multiplayer latency.', 'OpenAL source events verify playback submission; loudness preference remains a listening judgment.'],
}
for folder in [EVIDENCE, DEST]:
    (folder/'verification.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')

note = f'''# 地狱飞龙 · 本地验证

最终成品：`{JAR.name}`，SHA-256：`{sha(JAR)}`。

独立 GameTest **82/82**，铁魔法同装 **143/143**，零失败、零跳过。两套包含重复公共用例。

独立生产客户端加载了与交付文件哈希一致的 JAR，完成原生卷轴召唤、前/下/后方模型检查、三档光束、冻结停止、恢复、实体移除后的声音清理。三个原作声音均有 OpenAL source 事件；喷射循环最多一个，结束后为零。游戏正常保存退出，未改动用户实例。

攻击段使用真实 AI，旁边有铁傀儡作为高度参照；目标水平相距 3.8 格并小幅横向移动。连续检查飞行净空、升温与服务器/客户端身体朝向，详细数值见客户端事件记录。新增 GameTest 另核对所有等级的真实伤害、地面起飞、射程上下限与空中稳定锁定。

![高温光束](inferno-beam-hot.png)

[正面](inferno-front-hover.png) · [翼腹](inferno-lower-wings.png) · [背部管路](inferno-back-tank.png) · [低温](inferno-beam-low.png) · [中温](inferno-beam-medium.png) · [冻结](inferno-frozen-beam-stopped.png) · [恢复](inferno-resumed.png)

[结构化记录](verification.json) · [客户端事件](client-checks.txt) · [独立测试报告](standalone-gametest.xml) · [铁魔法测试报告](iron-gametest.xml)

技术边界：未测试用户全量整合包、所有光影或多人延迟。客户端日志有铁魔法自身 `template_open_spell_book_model` 缺失模型警告，未阻止资源加载、召唤、动画或声音测试；本次没有改写铁魔法资源。
'''
(EVIDENCE/'README.md').write_text(note, encoding='utf-8')
shutil.copy2(JAR, DEST/JAR.name)
shutil.copy2(ROOT/'build/libs'/JAR.name.replace('.jar','-sources.jar'), DEST/JAR.name.replace('.jar','-sources.jar'))
shutil.copy2(art/'inferno_dragon.bbmodel', DEST/'inferno_dragon.bbmodel')
shutil.copy2(art/'inferno-dragon-viewer.html', DEST/'inferno-dragon-viewer.html')
shutil.copytree(EVIDENCE, DEST/'verification', dirs_exist_ok=True)
readme = f'''# 皇室法术 {VERSION} · 地狱飞龙本地测试版

仅 `{JAR.name}` 需要放入 Minecraft 1.21.1 NeoForge 实例的 mods。请先保存并退出游戏、备份旧版，同一实例只保留一份皇室法术。此目录尚未自动安装或发布到 GitHub。

铁魔法集成验证版本为 Iron's Spells 1.21.1-3.16.3、NeoForge 21.1.249、Java 21；沿用既有 Curios、GeckoLib、playerAnimator、Iron's Lib 依赖，不在此捆绑。

创造物品栏搜索“皇室 · 地狱飞龙”取得原生卷轴。管理员也可用 `/royalespells give inferno_dragon` 获取创造调试卡，或 `/summon royalespells:inferno_dragon ~ ~2 ~` 直接观察无主飞龙。

飞龙默认 48 生命、45 秒寿命；悬停高度至少为地面上方 3.5 格，水平射程 4 格、中心高度差上限 6 格。三级满级每 0.4 秒单体伤害 1.4 → 6.76 → 17.8，低等级按比例换算；锁定满 2/4 秒升温，换目标/打断清零。原生传奇火系最高 3 级，一级 100 魔力、40 秒冷却。

本版修正贴头盔体与独立面罩、飞行高度、扩大的攻击范围及锁定后转身甩光束的问题。

[可旋转预览](inferno-dragon-viewer.html) · [可编辑 Blockbench 模型](inferno_dragon.bbmodel) · [实机与测试证据](verification/README.md)

完整源码 ZIP 保留模型、GLTF、材质、构建文件、技术档案和素材出处；sources.jar 用于 IDE 源码关联，不是完整构建工程。
'''
(DEST/'README-zh_CN.md').write_text(readme, encoding='utf-8')

# Use the repository's explicit tracked/unignored source inventory. Never include
# caches, saves, downloaded dependency JARs, Git internals or other workspace apps.
paths = subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z'], cwd=ROOT).decode().split('\0')
source_zip = DEST/f'royale-spells-{VERSION}-source.zip'
with zipfile.ZipFile(source_zip, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    for relative in sorted(set(paths)):
        if not relative: continue
        path = ROOT/relative
        if path.is_file(): archive.write(path, relative)
with zipfile.ZipFile(source_zip) as archive:
    assert archive.testzip() is None
    assert 'art/inferno-dragon/inferno_dragon.bbmodel' in archive.namelist()
    assert 'docs/technical/12-inferno-dragon.md' in archive.namelist()
    assert 'src/main/java/dev/royalespells/entity/InfernoDragon.java' in archive.namelist()

files = sorted(p for p in DEST.rglob('*') if p.is_file() and p.name != 'SHA256SUMS.txt')
(DEST/'SHA256SUMS.txt').write_text(''.join(sha(p)+'  '+p.relative_to(DEST).as_posix()+'\n' for p in files), encoding='utf-8')
print(json.dumps({'output': str(DEST), 'files': len(files), 'jar_sha256': sha(JAR), 'source_zip_bytes': source_zip.stat().st_size, 'tests': tests}, indent=2))
