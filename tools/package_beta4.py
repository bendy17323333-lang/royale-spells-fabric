"""Package the locally tested JAR and editable models without publishing or installing."""
from pathlib import Path
import hashlib, json, shutil, zipfile

ROOT=Path(__file__).resolve().parents[1]
VERSION='1.6.0-beta.4'
DEST=ROOT/'dist'/f'royale-spells-{VERSION}-local'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    evidence=json.loads((ROOT/'docs/beta4/test-results.json').read_text(encoding='utf-8'))
    DEST.mkdir(parents=True,exist_ok=True)
    for key in ['jar','sources_jar']:
        p=ROOT/evidence[key]['path'];assert sha(p)==evidence[key]['sha256'];shutil.copy2(p,DEST/p.name)
    for name in ['VALIDATION-1.6.0-beta.4.md','SPIRIT-CARD-SOURCES.json','SPIRIT-ASSET-SOURCES.json','CHANGELOG-1.6.0.md']:
        shutil.copy2(ROOT/name,DEST/name)
    video=ROOT/'docs/beta4/electro-chain.mp4';assert video.stat().st_size>10000;shutil.copy2(video,DEST/video.name)
    archive=DEST/f'royale-spells-models-{VERSION}.zip'
    report=json.loads((ROOT/'art/blockbench-v3/export-report.json').read_text(encoding='utf-8'))
    with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for name,metrics in report.items():
            p=ROOT/'art/blockbench-v3'/f'{name}.bbmodel';assert sha(p)==metrics['source_sha256'];z.write(p,'models/'+p.name)
        for p in sorted((ROOT/'art/blockbench-v3').glob('*-beta4-preview.png')):z.write(p,'blockbench-previews/'+p.name)
        for p in sorted((ROOT/'docs/beta4/screenshots').glob('*.png')):z.write(p,'minecraft-screenshots/'+p.name)
        z.write(video,'electro-chain.mp4')
        for name in ['PIXEL-FINAL-PROMPTS.md','export-report.json','export-runtime.py','build-models.js']:
            z.write(ROOT/'art/blockbench-v3'/name,name)
        for name in ['SPIRIT-ASSET-SOURCES.json','SPIRIT-CARD-SOURCES.json']:z.write(ROOT/name,name)
        z.write(ROOT/'src/main/resources/assets/royalespells/model_credits.txt','model_credits.txt')
        z.writestr('README.md','''# 四精灵与熔炉法杖 1.6.0-beta.4

models 中是 4 个精灵和 4 个法杖的实际 Blockbench 工程，纹理已内嵌。
火精灵全身环绕动态火焰，电精灵采用短紫色冠簇；对应法杖同步更新。
冰与治疗沿用 Beta 3 方块造型。冰原资源来自用户提供的 MineClash，归属见 model_credits.txt。
素材图集继续使用 ImageGen 原始 PNG，原卡图来源另见 SPIRIT-CARD-SOURCES.json。

minecraft-screenshots 是同版最终 JAR 的原始游戏截图；electro-chain.mp4 是连续实机帧按 10 fps 编码的无声连锁预览。
blockbench-previews 是本轮火、电精灵的实际编辑器预览。这里没有用概念图替代实机截图。

修改模型后保存回源码 art/blockbench-v3，并运行 python art/blockbench-v3/export-runtime.py，然后 Gradle build。
保留原骨骼名称。运行时导出器不会覆盖皇室战争原卡图。
本包是模型与验证资料，不放入 mods；实际游戏使用单独的模组 JAR。本地版本没有上传 GitHub。
''')
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None
        assert len([n for n in z.namelist() if n.endswith('.bbmodel')])==8
        for name in report:assert z.read('models/'+name+'.bbmodel')==(ROOT/'art/blockbench-v3'/f'{name}.bbmodel').read_bytes()
    (DEST/'README.md').write_text('''# 皇室法术 1.6.0-beta.4 本地测试版

Minecraft 1.21.1 / NeoForge，熔炉法杖需要 Iron’s Spells ’n Spellbooks 1.21.1-3.16.3 与其依赖。

- 不带 sources 的 JAR：安装用模组。
- sources.jar：Java 与资源源码，不放入 mods。
- models.zip：8 份内嵌纹理的 Blockbench 工程、实机截图和说明，不放入 mods。
- electro-chain.mp4：实际游戏连续帧的无声连锁预览。

火精灵全身燃烧，电精灵每 0.25 秒连锁一次、最多 9 个不同目标，下一跳 3 格；四种精灵法术采用原作卡图与原比例。
将军保持小兵后排，虚化小兵清理旧攻击状态、加快移动并尝试侧翼接敌。

更新前保存并完全退出游戏，备份旧版，再用新 JAR 替换同名模组；只保留一份皇室法术。
本次没有自动安装到 CurseForge，没有修改用户存档，也没有上传 GitHub。无需安装 MineClash。
服务器 GameTest 63 + 122 项通过，最终 JAR 通过隔离客户端流程，27 张有效截图已经查看主要项目。
完整测试证据位于源码 docs/beta4；详细机制、原作依据、获取方法位于 docs/technical/11-furnace-staff-and-spirits.md。
本轮不包括用户完整整合包、联机高延迟、光影或所有复杂地形的验证。
''',encoding='utf-8')
    files={p.name:dict(sha256=sha(p),bytes=p.stat().st_size) for p in sorted(DEST.iterdir()) if p.is_file() and p.name!='SHA256.json'}
    (DEST/'SHA256.json').write_text(json.dumps(files,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(dict(directory=str(DEST),files=files),ensure_ascii=False,indent=2))
if __name__=='__main__':main()
