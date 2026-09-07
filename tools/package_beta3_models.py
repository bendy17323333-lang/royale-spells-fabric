"""Package local model delivery, preserving original image/model/JAR bytes.

No network publication or game-instance replacement. Superseded designs and
the external MineClash JAR are deliberately not part of this deliverable.
"""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = '1.6.0-beta.3'
DEST = ROOT / 'dist' / f'royale-spells-{VERSION}-local'
ART = ROOT / 'art/blockbench-v3'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    evidence = json.loads((ROOT / 'docs/beta3/test-results.json').read_text(encoding='utf-8'))
    jar = ROOT / evidence['jar']['path']
    assert sha(jar) == evidence['jar']['sha256']
    DEST.mkdir(parents=True, exist_ok=True)
    for path in [jar, jar.with_name(jar.stem + '-sources.jar')]:
        shutil.copy2(path, DEST / path.name)

    archive = DEST / f'royale-spells-models-{VERSION}.zip'
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        models = list(json.loads((ART / 'export-report.json').read_text(encoding='utf-8')))
        assert len(models) == 8
        for name in models:
            path = ART / (name + '.bbmodel')
            z.write(path, 'models/' + path.name)
        for path in sorted((ART / 'previews').glob('*.png')):
            z.write(path, 'blockbench-previews/' + path.name)
        for path in sorted((ROOT / 'docs/beta3/screenshots').glob('*.png')):
            z.write(path, 'minecraft-screenshots/' + path.name)
        for name in ['PIXEL-FINAL-PROMPTS.md', 'IMAGEGEN-PROMPTS.md', 'export-report.json']:
            z.write(ART / name, name)
        z.write(ROOT / 'SPIRIT-ASSET-SOURCES.json', 'SPIRIT-ASSET-SOURCES.json')
        z.write(ROOT / 'src/main/resources/assets/royalespells/model_credits.txt', 'model_credits.txt')
        z.writestr('README.md', '''# 四精灵与熔炉法杖：1.6.0-beta.3

`models/` 内含 4 个精灵和 4 个法杖的实际 Blockbench 工程，纹理已经内嵌。直接用 Blockbench 打开 `.bbmodel`，无需另找贴图。

- 冰：来自用户提供并允许使用的 MineClash 0.7.5，保留原皮肤、方块轮廓和独立发光遮罩。
- 火：冒火的方形煤块。
- 电：紫色方块主体、平面方眼、像素闪电胡须与电弧。
- 治疗：金色方块主体、单齿、平面表情、扁平小舌头。
- 法杖：切角方锅、木杆、金属箍、侧环、元素溢流与对应的精灵头。

`blockbench-previews/` 是实际编辑器渲染；`minecraft-screenshots/` 是同版 JAR 的原始游戏截图，未用生成图代替实机。没有包含先前圆形设计。

如需重新导出至 Minecraft，将修改后的工程放回源码的 `art/blockbench-v3/`，在仓库根目录执行 `python art/blockbench-v3/export-runtime.py`，再进行 Gradle build。请保留骨骼名称，单独网格旋转需烘焙，网格只使用三角或四边面。

此压缩包是模型工作包，不应放进 Minecraft 的 mods 文件夹。实际游戏使用旁边的 NeoForge JAR。该本地版本未上传 GitHub，也未替换用户游戏实例。

来源和归属见 `SPIRIT-ASSET-SOURCES.json`、`model_credits.txt`；来源清单中的资源路径相对于完整源码。ImageGen 最终提示词见 `PIXEL-FINAL-PROMPTS.md`，早期稿见 `IMAGEGEN-PROMPTS.md`，早期概念不代表本版最终设计。
''')
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None
        assert len([n for n in z.namelist() if n.endswith('.bbmodel')]) == 8
        assert all('superseded' not in n for n in z.namelist())
        for name in models:
            assert z.read('models/' + name + '.bbmodel') == (ART / (name + '.bbmodel')).read_bytes()

    (DEST / 'README.md').write_text('''# 皇室法术 1.6.0-beta.3 本地预览版

Minecraft 1.21.1 / NeoForge。四精灵与熔炉法杖按方块风格重做，冰精灵适配用户提供的 MineClash 模型；奶豆子、电豆子不再使用圆球身体与深凹五官。

- `royale-spells-neoforge-1.21.1-1.6.0-beta.3.jar`：实际模组文件。
- 带 `-sources.jar` 的文件：Java 与资源源码，不放入 mods。
- `royale-spells-models-1.6.0-beta.3.zip`：8 份内嵌纹理的 Blockbench 工程、编辑器预览和实机截图，不放入 mods。

熔炉法杖需要 Iron’s Spells ’n Spellbooks 1.21.1-3.16.3 及其依赖。无须安装 MineClash；本包没有合并它的代码。完整依赖及本轮验证见源码根目录的 VALIDATION-1.6.0-beta.3.md、docs/beta3/test-results.json。

更新游戏时先保存并完全退出，备份旧皇室法术 JAR，再以对应的新 JAR 替换；同一实例只保留一份皇室法术。本次仅提供本地文件，没有替用户安装，没有上传 GitHub。

构建和独立客户端验证已完成，已查看模型、手持、火焰透明度、夜间遮罩与跳跃命中截图。用户完整整合包、多人和光影效果不属于本轮验证。
''', encoding='utf-8')
    manifest = {p.name: {'sha256': sha(p), 'bytes': p.stat().st_size}
                for p in sorted(DEST.iterdir()) if p.is_file() and p.name != 'SHA256.json'}
    (DEST / 'SHA256.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'directory': str(DEST), 'files': manifest}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
