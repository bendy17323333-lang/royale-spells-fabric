"""Update current release documentation without altering historical local evidence."""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[1]
A=R/'src/main/resources/assets/royalespells'
V='1.6.1-beta.2'
MINE='https://www.curseforge.com/minecraft/mc-mods/mineclash'
def write(p,s):p.write_text(s,encoding='utf-8')
for lang,values in {
 'zh_cn':{'army_general':'将军存活时小骷髅可虚化；将军阵亡仅幽灵消散','spirit_fire':'半径 2.4 格；不破坏方块','spirit_ice':'半径 2.2 格；冻结 1 秒','spirit_electro_timing':'每 0.25 秒连锁；眩晕 0.5 秒','spirit_heal':'半径 2.2 格；3 格内友军恢复 4 生命','spirit_damage':'每个目标受到 %s 伤害'},
 'en_us':{'army_general':'Ghosts need the General; living soldiers survive his death','spirit_fire':'2.4 block radius; no block damage','spirit_ice':'2.2 block radius; freezes for 1s','spirit_electro_timing':'0.25s interval; stuns for 0.5s','spirit_heal':'2.2 block radius; heals allies within 3 blocks for 4','spirit_damage':'%s damage per target'}
}.items():
 p=A/'lang'/f'{lang}.json';d=json.loads(p.read_text(encoding='utf-8'));d.update({'ui.royalespells.'+k:v for k,v in values.items()});write(p,json.dumps(d,ensure_ascii=False,indent=2)+'\n')
credits=f'''Royale Spells {V} — Iron integration public Beta 2

Ice Spirit model, texture and glowmask: MineClash 0.7.5, by LiziYowo / the MineClash team.
Original project: {MINE}
Source entries: assets/mineclash/geo/ice_spirit.geo.json, textures/entity/ice_spirit.png and ice_spirit_glowmask.png.
The user supplied the MineClash JAR and authorized use for a planned merge. Scale, bones and export were adapted; the ice staff uses the same head. MineClash code is not bundled and MineClash is not a runtime dependency. Original ownership and license are retained.

Fire, Electro and Heal spirit geometry and furnace staff geometry: created for Royale Spells in Blockbench via MCP.
Material atlas and animated flame bitmap: generated with built-in ImageGen; original generated pixels retained.
Clash Royale character designs, card artwork and original audio: Supercell.
Original card PNGs: pinned RoyaleAPI/cr-api-assets archive; source URLs and hashes in SPIRIT-CARD-SOURCES.json.
Original spirit audio: pinned Henrylq/Clash-Royale-SFX archive; source URLs, shared-cue notes, hashes and mono conversion in SPIRIT-AUDIO-SOURCES.json.
'''
write(A/'model_credits.txt',credits)
p=R/'SPIRIT-ASSET-SOURCES.json';d=json.loads(p.read_text(encoding='utf-8'));d['version']=V;d['mineclash_ice']['project_url']=MINE;d['mineclash_ice']['author']='LiziYowo / MineClash team';d['sound_reuse']='Replaced by 22 original spirit-category clips and 16 phase events; see SPIRIT-AUDIO-SOURCES.json for documented shared cues.'
write(p,json.dumps(d,ensure_ascii=False,indent=2)+'\n')
p=R/'docs/technical/11-furnace-staff-and-spirits.md';s=p.read_text(encoding='utf-8')
s=s.replace('对应本地 `1.6.0-beta.4`','对应公开铁魔法适配 Beta 2 `'+V+'`').replace('本次按用户要求暂不上传 GitHub。','本版整合此前仅本地交付的 1.6.0 Beta 2–4；内部迭代编号与公开 Beta 次序不同。')
s=s.replace('半径 2.4 格内 7 伤害','半径 2.4 格内 10 伤害').replace('半径 2.2 格内 2.5 伤害','半径 2.2 格内 7 伤害').replace('每次 2.5 伤害','每次 7 伤害').replace('半径 2.2 格内 2 伤害','半径 2.2 格内 7 伤害')
s=s.replace('跳跃第 4 tick 起进行实际包围盒接触检查；','从第一个空中 tick 起，用上个 tick 到当前 tick 的身体中心线段，对按精灵半尺寸和 0.24 格接触容差扩展的目标包围盒做相交检测；按最早接触位置选取可见敌人，治疗精灵也接受受伤友军。原目标死亡不阻止命中实际撞到的下一单位；不按原目标 UUID 在远处补伤害。')
s=s.replace('将军按照最靠近目标的五名小兵平均前进距离，在其后约 1.1 格处寻找锚点，最近保留约 1.8 格攻击距离。被挤到前面时先向后排锚点移动；普通推进比小兵慢，有条件时仍使用原来较长的法杖攻击范围。','将军按前五名小兵的位置、较慢的 0.90 推进倍率和约 0.75 格跟随距离控制前进。锚点最多与当前位置一样远，不会生成远离敌人的后退路径；敌人进入原有较长的法杖攻击范围时立即停步迎战。小兵以更高速度主动冲锋和绕侧，从而自然形成前排；将军不会看到目标就逃跑。')
s=s.replace('音频复用已有法术原作声音，烈焰使用火球 impact、冰使用冰冻 deploy、电使用小电 deploy、治疗使用治疗 deploy。没有把这些复用声音声称为单独提取的原作精灵音效。','音频现在使用四种精灵各自原作分类下的 22 个 OGG，注册 16 个阶段事件：首次部署、跳扑、命中、低音量脚步。电精灵每次连锁命中播放原作电击声，不再复用普通小电部署事件；火、冰也不再借用大火球或冰冻法术整段声音。部署标志写入 NBT，重载不重复登场。脚步至少间隔 10 tick，音量 0.075×全局 0.9，且不使用攻击音轨。所有音轨为单声道、64 格线性衰减；单声道转换不裁剪、不变速、不变调。治疗跳扑采用原作 Heal Spirit 分类中的共享精灵移动叫声，冰跳扑采用其分类中火精灵命名的共享攻击声；未虚构独占录音。参见 [精灵音轨来源与处理](../../SPIRIT-AUDIO-SOURCES.json) 和 [导入器](../../tools/import-spirit-audio.py)。')
s=s.replace('MineClash 原资源保留原作者和原许可归属，','冰精灵原模型、皮肤与发光遮罩改编自 [MineClash]('+MINE+')，作者 **LiziYowo / MineClash 团队**，来源版本 0.7.5。MineClash 原资源保留原作者和原许可归属，')
s=s.replace('本轮验证记录在 [VALIDATION-1.6.0-beta.4.md](../../VALIDATION-1.6.0-beta.4.md)。','本轮验证记录在 [公开 Beta 2 验证](../../VALIDATION-1.6.1-beta.2.md)。[旧本地 Beta 4 验证](../../VALIDATION-1.6.0-beta.4.md) 保留为历史证据。')
s=s.replace('## 9. 验证', '### 8.3 将军阵亡后的普通生还者\n\n将军死亡结束该觉醒军团的支持账本并保留共享冷却。只有已虚化幽灵进入 20 tick 消散；未虚化小兵保持当前血量、主人、队形槽位、低伤害近战和剩余寿命，以普通小骷髅身份继续战斗。它们以后受到致命伤正常死亡，不能重新虚化。保存/重载不会恢复已结束的虚化支持。铁魔法驱散直接移除被驱散的实体，仍然有效。\n\n## 9. 验证')
write(p,s)
p=R/'docs/technical/06-summons-and-combat.md';s=p.read_text(encoding='utf-8').replace('正常战斗 --> 消散: 将军消失或账本失效','正常战斗 --> 普通生还者: 将军阵亡\n    普通生还者 --> [*]: 正常死亡或剩余寿命结束').replace('成员发现账本不再有效后停止追击，20 tick 消散。','只有幽灵发现账本失效后停止追击并在 20 tick 后消散。未虚化小兵保持当前血量和归属继续近战、不会再虚化，剩余寿命不延长。').replace('Beta 4 的','公开 Beta 2 的').replace('让将军保持在前排小兵之后','让小兵更积极地冲锋、将军稍慢跟随并在贴身时迎战，不产生后退逃跑锚点');write(p,s)
p=R/'docs/technical/README.md';s=p.read_text(encoding='utf-8').replace('**当前本地版本：`1.6.0-beta.4`，Minecraft 1.21.1 / NeoForge，铁魔法 1.21.1-3.16.3。暂未上传 GitHub。**','**当前公开 Beta 2：`'+V+'`，Minecraft 1.21.1 / NeoForge，铁魔法 1.21.1-3.16.3。**').replace('和 [本轮验证](../../VALIDATION-1.6.0-beta.4.md)','和 [历史验证](../../VALIDATION-1.6.0-beta.4.md)。公开 Beta 2 再修复跳扑漏判、将军逃跑、普通小兵随将军消失，提高精灵伤害并接入原作音效；见 [本版验证](../../VALIDATION-1.6.1-beta.2.md)');write(p,s)
p=R/'art/blockbench-v3/README.md';s=p.read_text(encoding='utf-8').replace('**1.6.0-beta.4**','**'+V+'（公开 Beta 2）**').replace('低矮冠簇和近距离细电弧','三处柔和的切角冠簇、超出脸颊的折线闪电胡须；头顶横电弧已删除').replace('来源见 model_credits.txt','来源见 model_credits.txt');s+='\n冰精灵原模型与贴图来源：[MineClash]('+MINE+')，作者 LiziYowo。公开 Beta 2 电精灵前视图为 `electro-beta2-front.png`。\n';write(p,s)
p=R/'ASSETS.md';s=p.read_text(encoding='utf-8');s+='\n\n## 公开铁魔法 Beta 2（'+V+'）\n\n冰精灵模型、原贴图和发光遮罩改编自 **[MineClash]('+MINE+')**，作者 **LiziYowo / MineClash 团队**，源版本 0.7.5；冰法杖使用同一头部。未捆绑 MineClash 代码，不需要安装 MineClash。\n\n四精灵音效通过原作分类音轨导入，共 22 个 OGG、16 个事件；出处、原文件与单声道转换后哈希和共享片段说明见 [SPIRIT-AUDIO-SOURCES.json](SPIRIT-AUDIO-SOURCES.json)。模型、原卡图和声音的原始归属保留；本版不把第三方素材声明为本项目原创。\n';write(p,s)
print('Updated languages, runtime credits, technical chapters and MineClash attribution.')
