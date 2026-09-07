"""Package verified Beta 2 release files. This never uploads or installs anything."""
from pathlib import Path
import hashlib, json, shutil, zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = '1.6.1-beta.2'
DEST = ROOT/'dist'/f'royale-spells-{VERSION}-public'
DOCS = ['README.md', 'CHANGELOG-1.6.1.md', 'VALIDATION-1.6.1-beta.2.md',
        'SPIRIT-ASSET-SOURCES.json', 'SPIRIT-AUDIO-SOURCES.json', 'SPIRIT-CARD-SOURCES.json', 'ASSETS.md']

def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    evidence = json.loads((ROOT/'docs/beta2-public/test-results.json').read_text(encoding='utf-8'))
    DEST.mkdir(parents=True, exist_ok=True)
    for key in ('jar', 'sources_jar'):
        p = ROOT/evidence[key]['path']; assert sha(p) == evidence[key]['sha256']; shutil.copy2(p, DEST/p.name)
    for name in DOCS: shutil.copy2(ROOT/name, DEST/name)
    archive = DEST/f'royale-spells-models-{VERSION}.zip'
    report = json.loads((ROOT/'art/blockbench-v3/export-report.json').read_text(encoding='utf-8'))
    with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as z:
        for name, metrics in report.items():
            p = ROOT/'art/blockbench-v3'/f'{name}.bbmodel'; assert sha(p) == metrics['source_sha256']; z.write(p, 'models/'+p.name)
        for p in (ROOT/'docs/beta2-public/screenshots').glob('*.png'): z.write(p, 'minecraft-screenshots/'+p.name)
        z.write(ROOT/'art/blockbench-v3/electro-beta2-front.png', 'blockbench-preview/electro-front.png')
        for name in ('PIXEL-FINAL-PROMPTS.md', 'export-report.json', 'export-runtime.py', 'build-models.js'):
            z.write(ROOT/'art/blockbench-v3'/name, name)
        for name in DOCS: z.write(ROOT/name, 'documentation/'+name)
        z.write(ROOT/'src/main/resources/assets/royalespells/model_credits.txt', 'model_credits.txt')
        z.writestr('README.md', '''# 四精灵与熔炉法杖 · 铁魔法适配 Beta 2

models：4 个精灵、4 个法杖的实际 Blockbench 工程，纹理内嵌，可直接打开。
minecraft-screenshots：同版成品 JAR 的原始游戏截图。blockbench-preview：电精灵本轮编辑器正面图。

冰精灵模型、纹理与发光遮罩改编自 MineClash；作者 LiziYowo / MineClash 团队。
原链接：https://www.curseforge.com/minecraft/mc-mods/mineclash
原素材权利归原作者，其他皇室战争素材归 Supercell。详细来源、原文件和哈希见 model_credits.txt 与 documentation 中的清单。

此压缩包用于编辑与查阅，不放进 mods。修改工程后保存回源码 art/blockbench-v3，运行 export-runtime.py，再用 Java 21 构建。
''')
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None
        assert len([n for n in z.namelist() if n.endswith('.bbmodel')]) == 8
        for name in report: assert z.read('models/'+name+'.bbmodel') == (ROOT/'art/blockbench-v3'/f'{name}.bbmodel').read_bytes()
    files = {p.name:dict(sha256=sha(p), bytes=p.stat().st_size) for p in sorted(DEST.iterdir()) if p.is_file() and p.name!='SHA256.json'}
    (DEST/'SHA256.json').write_text(json.dumps(files, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    print(json.dumps(dict(directory=str(DEST), files=files), ensure_ascii=False, indent=2))

if __name__ == '__main__': main()
