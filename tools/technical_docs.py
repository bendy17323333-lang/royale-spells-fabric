#!/usr/bin/env python3
"""Generate reference tables and check the Chinese technical manual.

Uses only Python 3's standard library. This is a documentation tool, not a
Java parser or a gameplay test. Unexpected enum/method layouts fail loudly;
update the extractor when refactoring those declarations. Only the two
GENERATED files may be written, and --check never writes anything.
"""
from __future__ import annotations

import argparse
from collections import Counter
import json
from pathlib import Path
import re
import sys
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]
DOCS = ROOT / "docs/technical"
JAVA = ROOT / "src/main/java/dev/royalespells"
ASSETS = ROOT / "src/main/resources/assets/royalespells"
GENERATED = ("source-index.md", "spell-reference.md")
CHAPTERS = (
    "README.md", "01-architecture.md", "02-casting-and-balance.md",
    "03-target-preview.md", "04-rendering-and-audio.md", "05-iron-integration.md",
    "06-summons-and-combat.md", "07-elixir-structures-rituals.md",
    "08-data-and-mixins.md", "09-development-and-validation.md",
    "10-extension-and-troubleshooting.md", "11-furnace-staff-and-spirits.md", "12-inferno-dragon.md", "13-electrical-pause.md", "14-snowball-and-projectile-visuals.md", "15-card-balance.md", *GENERATED,
)

# Curated descriptions complement the mechanically extracted path/line counts.
# A new source file must receive a description instead of silently appearing as
# an undocumented runtime feature. Presence does not imply active registration.
ROLES_TEXT = """
CardBalance|2026-09-08 卡牌独立数值配置；11/12 级、每次伤害、召唤物武器与盾
test/CardBalanceTests|完整卡牌伤害、配置隔离、转换继承、盾牌与军团存档回归
AnimationTimeline|积分局部动画时钟，支持降速、暂停输入和连续恢复
SnowballChill|雪球专用状态、60 tick 缓慢和 65% 动画速率
client/ChillAnimation|弱引用实体键，分开维护年龄与步态时钟
client/ProjectileVisuals|火球切面网格、沿轨迹火焰团、短促爆燃与独立雪球余效
client/FireballParticles|运行时引用原版粒子，生成上升黑烟和带重力的细小火星
client/FireballClientSmoke|只做截图的隔离火球客户端检查，不启用录像器
client/VisualFrameRecorder|显式启用的隔离世界帧缓冲导出器
client/VisualAudioRecorder|按实际游戏音效事件导出原始样本与混音参数
client/VisualUpdateRecording|独立录制场景、原版与 Gecko 动画输入观测
mixin/ChillGeoAnimationMixin|局部调整 Gecko TICK 与 partial 并在 finally 恢复
mixin/ChillVanillaAnimationMixin|仅降速原版模型年龄与步态输入，不改变死亡计时
mixin/ChillSwingMixin|仅客户端降低挥手进度，服务器攻击频率保持不变
test/SnowballVisualTests|朝向存档、普通觉醒雪球、状态过期、温暖解除与时钟回归
pause/ElectricPause|共享电击标签、服务器效果检测和另一暂停驱动的客户端标志桥接
pause/PauseClock|逐实体局部动画时间，暂停与解除不跳帧
pause/PauseMixinPlugin|可选依赖检测，两模组同装只启用一份电击驱动
pause/PauseState|同步暂停状态的实体接口
pause/mixin/PauseGeoMixin|临时替换 GeckoLib TICK ticket 和局部帧时间并恢复
pause/mixin/PauseIronClientMixin|暂停本地铁魔法施法条
pause/mixin/PauseIronManagerMixin|保留原施法数据，只暂停玩家施法计时分支
pause/mixin/PauseIronStartMixin|电击期间阻止开始新施法
pause/mixin/PauseLivingMixin|同步电击状态，暂停挥手、物品使用、移动与主动近战
pause/mixin/PauseMobMixin|暂停 Mob AI 推进，保留 Goal 与 Brain 状态
pause/mixin/PauseVanillaRendererMixin|原版生物渲染采用每实体局部动画时间
test/ZappiesIntegrationTests|真实电车与皇室、铁魔法召唤物友军互认和飞龙重置测试
entity/InfernoDragon|飞行寻路、单体三档升温、打断重置、无击退伤害和存档
iron/InfernoDragonSpell|传奇火系召唤、生成空间、原生施法和召唤伤害属性桥接
client/InfernoDragonRenderer|地狱飞龙网格、口部定位、三层升温光束与命中闪光
client/InfernoDragonAudio|单实例原作喷射循环、低音量振翅、冻结与卸载停止
client/InfernoClientSmoke|隔离成品客户端中的模型、原生卷轴、三档光束和音频验证
test/InfernoDragonTests|三档实际伤害、打断、对空寻路、友军、克隆和存档测试
test/InfernoIronTests|原生施法、魔力冷却、镜像、召唤属性和伤害取消测试
spirit/SpiritElement|四种精灵的稳定名称、学派、媒介、费用与基础伤害
entity/ElementalSpirit|精灵寻敌、治疗寻友、提交跳跃、单次爆发、链击与存档
entity/SpiritArc|七 tick 电弧端点和年龄同步，不产生伤害或碰撞
client/SpiritArcRenderer|逐跳蓝白折线电弧、命中闪光与淡出
army/ArmyCombatGoal|将军后排锚点、小兵环形攻击位、侧翼绕路与近距离寻路
iron/FurnaceStaffItem|原生法杖属性、锁定元素法术容器与物品说明
iron/SpiritSpell|原生吟唱、生成空间、跨维度数量限制与召唤
iron/SpiritSpells|精灵注册、右键施法、共享冷却、铁砧转化与元素伤害桥接
client/SpiritRenderer|按元素选择模型、原皮肤、局部发光及透明火焰层
client/FurnaceSpriteSource|资源重载时适配 ImageGen 图集上传尺寸，保留全局 mipmap
client/SpiritClientSmoke|隔离精灵画廊与法杖握持截图
test/SpiritTests|军团同刻攻击、幽灵分散、精灵爆发、实际寻路和存档测试
test/SpiritIronTests|真实奥术铁砧转换、原生法杖施法与取消、共享冷却测试
test/SpiritBeta2Tests|近中远跳扑、真实跑近起跳、飞行碰撞、将军迎战和普通小兵存活回归
RoyaleSpells|模组入口、注册、事件挂接与管理指令
Spell|旧卡枚举：费用、半径、时间、颜色及 ordinal
SpellItem|手持旧法术卡的使用入口
SpellEngine|独立圣水、目标检测、公共伤害和召唤入口
IronSpellSystem|隔离可选铁魔法类型的公共桥接门面
SpellSounds|按法术/阶段注册、播放固定范围声音
UnitSounds|野蛮人等单位的声音事件
TroopCard|部队卡定义，目前为野蛮人小屋
TroopItem|部队卡使用与小屋召唤入口
SceneControlItem|录制地图前一场/后一场道具
ShowcaseMap|隔离录制地图、场景搭建与目标重置
TargetGeometry|预览长条端点、侧向偏移等共享几何
FieldAnimation|领域展开、维持、收束的时间曲线
RocketMotion|火箭曲线位置、速度和朝向计算
LogMotion|滚木横轴滚动与行进几何
ArrowPattern|万箭齐发落点分布和时序
SpellMotion|位移、NoAI 目标移动与建筑锚定处理
CombatImpact|用作用域抑制非位移伤害的额外击退
ControlCooldown|冰冻/藤蔓共享控制冷却
CombatCompatibility|友军、敌意与跨模组召唤关系判断
SummonOrders|玩家指令优先、附近敌人次之的目标选择
VisualState|LivingEntity 同步视觉状态访问契约
EarthquakeDestruction|地震候选方块、分批裂纹、保留与破坏
iron/ArmyMagic|原生军团、号角施法与共享资源检查
iron/ElixirCrafting|皇室卷轴锻造、墨水替代和铁砧觉醒
iron/ElixirInkItem|按稀有度映射的圣水/重油材料
iron/EvolvedArmySpell|仪式专属觉醒军团的原生法术
iron/IronIntegration|法术注册、伤害/治疗/召唤事件和原生历史
iron/IronSpellProfile|原生默认学派、稀有度、魔力、等级和成长
iron/MirrorHistoryPayload|服务器向客户端同步上一次原生法术
iron/MirrorIronSpell|复制上一次成功法术、提高一级及取消清理
iron/NativeClones|复制原生召唤物并保留原生召唤关系
iron/RoyaleIronSpell|通用原生生命周期、信息、效果和英雄再施放
iron/SharedControl|原生控制冷却同步与施法检查
army/ArmyFormation|军团的 15+1 生成位置和空间检测
army/ArmyLedger|服务器持久化的每人军团与冷却记录
army/ArmySounds|军团号角、攻击、破盾等声音
army/NeutralArmyEgg|无玩家归属的实验军团蛋
elixir/DarkPoolBlock|天然源方块接收掉落物并启动仪式
elixir/ElixirConfig|塔池生成的 common 配置
elixir/ElixirContent|流体、源/流动方块、桶和瓶注册与打捞
elixir/TowerPools|火术师塔池和更深密室的生成尝试
entity/AllySkeleton|墓园石剑骷髅：低伤、主人、寿命与受伤间隔
entity/AllyZombie|小僵尸、野蛮人/皇家卫队的行为基类与主人关系
entity/ArmySkeleton|将军/军团小骷髅、护盾、幽灵、攻击状态
entity/EvolutionBurst|一次性觉醒部署视觉实体
entity/RageMeleeGoal|狂暴影响下的近战攻击节奏
entity/RitualEntity|重油转化、输入保管、输出和退款
entity/RoyaleUnit|保留的野蛮人小屋：朝向、生产、寿命与死亡波
entity/SpellEntity|投射物、领域、滚动物体的服务端时序与同步
entity/Summoned|召唤物主人、寿命等公共契约
client/AmbientSpellRenderer|无主要模型的法术实体渲染入口
client/ArmyClientSmoke|军团/仪式/号角隔离客户端检查
client/BarbarianAudioSmoke|野蛮人音频辅助检查
client/CardRenderer|旧卡手持物品的几何与显示变换
client/CombatClientSmoke|战斗、将军、冻结及部署隔离客户端检查
client/ElixirClient|流体颜色、贴图等客户端接入
client/ElixirClientSmoke|圣水、重油和结构客户端检查
client/EvolutionBurstRenderer|紫色部署闪光、环与碎晶
client/FrozenRender|按实体缓存并恢复原生/Geo 冻结姿势
client/GeneralEquipment|将军头盔、旗帜、盾与装备动作
client/GraveMoteParticle|墓园独立的持续紫色微粒
client/HelmetTaper|头盔局部坐标的轻微上窄下宽变形
client/IronCardUi|皇室原生 UI 图标比例与重复边框过滤
client/IronClientPreview|原生可施法道具、当前法术和预览选择
client/IronClientSmoke|同装战斗视觉辅助检查
client/IronSkeletonClient|原生亡者召唤的小骷髅显示接入
client/IronSystemClientSmoke|原生卷轴、法术书及系统客户端检查
client/MagicParticle|通用自定义粒子显示
client/PolishClientSmoke|UI、预览、音频等专题客户端检查
client/RangeDepthAudit|显式检查开关下的深度缓冲前后审计
client/RevisionVisualSmoke|历史视觉修订的辅助检查类
client/RitualRenderer|仪式液面、符文、物品和转化动画
client/RoyaleClient|客户端注册和渲染事件总入口
client/RoyaleSkeletonRenderer|以原版骨架为基础的召唤骷髅渲染
client/ShowcaseCapture|录制场景截图辅助工具
client/SpellFields|持续法术的地面领域几何
client/SpellLayers|深度、透明、写掩码等 RenderType 定义
client/SpellOverlays|冻结冰层、克隆/状态覆盖等世界效果
client/SpellRenderer|投射物、滚木/桶、箭与局部模型渲染
client/SpellTint|模型颜色、透明度和状态 tint
client/TargetPreview|世界坐标落点和固定形状范围预览
client/TargetPreviewSmoke|14 场景准星几何、像素/深度回归检查
client/TroopModel|JSON 部件、父子关系、姿势与持剑挂点
client/TroopRenderer|通用部队/小屋模型渲染
client/TroopVisualSmoke|部队模型辅助检查
client/VisualSmoke|专题检查分派与普通视觉流程
client/VoidRenderer|虚空领域、三次劈击与色彩层次
client/WarriorRenderer|人形部队渲染入口及装备层
client/ZapRenderer|细电弧、分支和紫色第二击
mixin/FrozenModelPartMixin|冻结时替换原生 ModelPart 姿势
mixin/IronDispelMixin|让皇室效果/召唤物参与原生驱散
mixin/IronElixirAnvilMixin|原生奥术铁砧中的浓缩/觉醒接入
mixin/IronElixirForgeMixin|原生锻造台接受皇室墨水替代品
mixin/IronElixirForgeScreenMixin|锻造台客户端候选与材料显示
mixin/IronFrozenBoneMixin|冻结时替换 GeoBone 姿势
mixin/IronGeoVisualMixin|Geo 模型状态颜色与冻结作用域
mixin/IronLootCacheAccess|访问原生卷轴战利品缓存
mixin/IronSchoolCacheAccess|访问原生学派法术候选缓存
mixin/IronSpellBarUiMixin|法术栏仅对皇室图标调整框与比例
mixin/IronSpellWheelUiMixin|法术轮盘仅对皇室图标调整框与比例
mixin/IronTargetMixin|原生召唤物目标关系兼容
mixin/ItemCombinerPlayerAccess|取得容器中的玩家上下文
mixin/LivingEntityMixin|同步状态、冻结和无击退等公共行为
mixin/LivingVisualMixin|原生 Living 渲染的颜色与冻结作用域
mixin/MobEntityMixin|眩晕时取消 Mob 的服务端 AI 步骤
mixin/PoolTemplateAccess|访问塔模板局部放置所需信息
mixin/RoyaleMixinPlugin|对部分可选依赖 Mixin 做加载条件过滤
mixin/SilentLightningMixin|静音大闪外观实体，声音改由法术逐击播放
mixin/SkeletonArrowImpactMixin|相关召唤骷髅箭命中时的击退限制
mixin/TowerPoolsMixin|原生火术师塔放置后追加池生成尝试
test/ArmyTests|军团、盾、幽灵、号角与保存机制测试
test/Combat151Tests|控制、击退、召唤目标等回归测试
test/ElixirTests|流体、材料、结构与仪式测试
test/EvolutionTests|觉醒部署实体与触发规则测试
test/Iron151Tests|原生控制、共享冷却等回归测试
test/IronBalanceTests|原生数值、时长与等级成长测试
test/IronCompatibilityTests|双方召唤、伤害、控制和位移测试
test/IronSpellSystemTests|原生注册、卷轴、镜像及系统交互测试
test/NeoForgeEventTests|NeoForge 事件与可选依赖回归测试
test/SpellCoverageTests|旧卡全覆盖行为与资源检查
test/SpellGameTests|独立法术核心机制 GameTests
test/TestPlayers|测试玩家上下文辅助
test/TroopTests|部队、小屋和装备相关测试
"""
ROLES = dict(line.split("|", 1) for line in ROLES_TEXT.strip().splitlines())


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8-sig")


def data(path: Path):
    return json.loads(read(path))


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def link(path: Path, label: str | None = None) -> str:
    relative = path.relative_to(ROOT).as_posix()
    return f"[{label or path.name}](../../{relative})"


def cell(value) -> str:
    return str(value).replace("|", "\\|").replace("\n", " ")


def table(headers, rows) -> str:
    return "\n".join(
        ["| " + " | ".join(map(cell, headers)) + " |",
         "| " + " | ".join("---" for _ in headers) + " |"]
        + ["| " + " | ".join(map(cell, row)) + " |" for row in rows]
    )


def java_without_comments(text: str) -> str:
    # The extracted declarations contain no comment-like string literals.
    return re.sub(r"/\*.*?\*/|//[^\n]*", "", text, flags=re.S)


def enum_rows(path: Path, pattern: str):
    text = java_without_comments(read(path))
    match = re.search(r"public enum \w+\s*\{(.*?);", text, flags=re.S)
    require(match is not None, f"Cannot find enum declarations: {path}")
    body = match.group(1)
    rows = re.findall(pattern, body)
    residue = re.sub(pattern, "", body).replace(",", "").strip()
    require(bool(rows) and not residue, f"Unparsed enum syntax in {path}: {residue[:100]}")
    require(len({row[0] for row in rows}) == len(rows), f"Duplicate enum names: {path}")
    return text, rows


def method_body(text: str, name: str) -> str:
    match = re.search(r"\b" + re.escape(name) + r"\(\)\s*\{", text)
    require(match is not None, f"Cannot find method {name}")
    start = match.end()
    depth = 1
    for index in range(start, len(text)):
        depth += (text[index] == "{") - (text[index] == "}")
        if depth == 0:
            return text[start:index]
    raise ValueError(f"Unbalanced method {name}")


def profile_data():
    path = JAVA / "iron/IronSpellProfile.java"
    text, rows = enum_rows(path, r'([A-Z_]+)\(\s*"([a-z_]+)"\s*,\s*([A-Z]+)\s*,\s*(\d+)\s*,\s*(-?\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)')
    growth_body = method_body(text, "powerPerLevel")
    cases = re.findall(r"case\s+([A-Z_,\s]+?)\s*->\s*(\d+)\s*;", growth_body)
    fallback = re.search(r"default\s*->\s*(\d+)\s*;", growth_body)
    require(bool(cases) and fallback is not None, "Unrecognized powerPerLevel switch")
    expected_names = {row[0] for row in rows}
    growth = {name.strip(): int(value) for names, value in cases for name in names.split(",")}
    require(set(growth) <= expected_names, "powerPerLevel references unknown profiles")
    residue = re.sub(r"case\s+[A-Z_,\s]+?\s*->\s*\d+\s*;|default\s*->\s*\d+\s*;", "", growth_body)
    require(re.sub(r"\s+", "", residue) == "returnswitch(this){};", "powerPerLevel structure changed")
    max_body = re.sub(r"\s+", "", method_body(text, "maxLevel"))
    require(max_body.startswith("returnthis==SKELETON_ARMY_EVOLUTION?1:switch(rarity){"), "maxLevel exception changed; update extractor")
    maximums = dict(re.findall(r"case([A-Z]+)->(\d+);", max_body))
    require(re.sub(r"case[A-Z]+->\d+;", "", max_body) == "returnthis==SKELETON_ARMY_EVOLUTION?1:switch(rarity){};", "maxLevel switch structure changed")
    result = []
    for name, school, rarity, mana, per_level, cooldown, ticks in rows:
        require(rarity in maximums, f"No max level for {rarity}")
        result.append(dict(name=name, id=name.lower(), school=school, rarity=rarity,
                           mana=int(mana), per_level=int(per_level), cooldown=int(cooldown), ticks=int(ticks),
                           growth=growth.get(name, int(fallback.group(1))),
                           maximum=1 if name == "SKELETON_ARMY_EVOLUTION" else int(maximums[rarity])))
    return result


def source_index() -> str:
    files = sorted((ROOT / "src/main/java").rglob("*.java"))
    actual = {path.relative_to(JAVA).with_suffix("").as_posix() for path in files}
    require(actual == set(ROLES), f"Update source descriptions: missing={sorted(actual-set(ROLES))}, removed={sorted(set(ROLES)-actual)}")
    lines = ["# 源码与资源索引", "", "[返回目录](README.md)", "",
             "<!-- Generated by tools/technical_docs.py; edit curated descriptions in that script. -->", "",
             f"由生成器扫描当前 `src/main/java` 得到 **{len(files)} 个 Java 文件**。行数包含空行、注释，适合估算阅读量，不表示复杂度或注释覆盖率。职责为人工维护，路径和数量自动提取。", "",
             "索引列出所有源文件，包括测试与历史辅助类；不能由文件存在推断其已注册或在普通游戏执行。真实入口见 [架构](01-architecture.md) 与 [开发运行方式](09-development-and-validation.md)。", ""]
    groups = [(".", "公共入口与机制"), ("iron", "铁魔法接入"), ("army", "军团"), ("spirit", "精灵元素"),
              ("elixir", "流体与结构"), ("entity", "实体与 AI"), ("client", "客户端与视觉检查"),
              ("mixin", "Mixin 和访问器"), ("test", "服务端 GameTests")]
    for package, title in groups:
        selected = [p for p in files if p.parent.relative_to(JAVA).as_posix() == package]
        lines += [f"## {title}（{len(selected)}）", "", table(["文件", "行数", "职责"], [
            [link(p), len(read(p).splitlines()), ROLES[p.relative_to(JAVA).with_suffix("").as_posix()]] for p in selected]), ""]
    resources = [
        ("src/main/resources/META-INF/neoforge.mods.toml", "加载器、版本、依赖、许可声明"),
        ("src/main/resources/royalespells.mixins.json", "实际启用的 common/client Mixin 列表与插件"),
        ("src/main/resources/assets/royalespells/lang", "中英文物品、法术、提示与字幕"),
        ("src/main/resources/assets/royalespells/models", "物品和方块模型；实体自定义模型见同资源目录"),
        ("src/main/resources/assets/royalespells/textures", "卡图、GUI、实体、流体和粒子贴图"),
        ("src/main/resources/assets/royalespells/particles", "粒子 sprite 声明"),
        ("src/main/resources/assets/royalespells/sounds.json", "声音文件、字幕、预载和客户端衰减"),
        ("src/main/resources/assets/royalespells/spell_audio.json", "法术阶段到声音事件与音量的映射"),
        ("src/main/resources/assets/royalespells/sounds", "实际 OGG 声音资源"),
        ("src/main/resources/data/royalespells", "配方、标签、伤害类型等服务端数据"),
        ("build.gradle", "固定依赖、编译与隔离运行任务"),
        ("gradle.properties", "模组/Minecraft/NeoForge 版本与 Gradle 参数"),
        ("gradle/wrapper/gradle-wrapper.properties", "构建 Wrapper 版本和分发地址"),
        ("ASSETS.md", "第三方素材来源和权利说明"),
        ("VALIDATION-1.5.2-beta.1.md", "已发布 Beta 1 的验证边界和产物哈希"),
        ("docs/beta1/test-results.json", "发布时保存的结构化测试结果"),
    ]
    lines += ["## 资源与构建入口", "", table(["入口", "用途"], [[link(ROOT / p, p), desc] for p, desc in resources]), "",
              "## tools 目录", "", "这些文件不是一条应依次运行的流水线。历史 update/import 脚本可能写入资源和源码，且含旧断言或本机路径；执行前阅读脚本。`technical_docs.py` 只维护本文档的两张生成页。", "",
              table(["工具", "类型"], [[link(p), "文档生成与检查" if p.name == "technical_docs.py" else "历史制作/迁移或验证辅助；以文件内容为准"] for p in sorted((ROOT / "tools").iterdir()) if p.is_file()]), ""]
    return "\n".join(lines)


def spell_reference() -> str:
    profiles = profile_data()
    _, cards = enum_rows(JAVA / "Spell.java", r"([A-Z_]+)\(\s*(\d+)\s*,\s*([\d.]+)\s*,\s*(\d+)\s*,\s*0x([A-Fa-f\d]+)\s*\)")
    language = data(ASSETS / "lang/zh_cn.json")
    for profile in profiles:
        require(f"ironspell.royalespells.{profile['id']}" in language, f"Missing language key: {profile['id']}")
    def label(identifier):
        return language.get("ironspell.royalespells." + identifier, identifier).removeprefix("皇室 · ")
    rarity_names = {"COMMON": "普通", "UNCOMMON": "罕见", "RARE": "稀有", "EPIC": "史诗", "LEGENDARY": "传说"}
    lines = ["# 源码默认参数与资源参考", "", "[返回目录](README.md)", "",
             "<!-- Generated by tools/technical_docs.py; do not edit tables by hand. -->", "",
             "表格直接提取当前枚举和 JSON，并按源码顺序保留 ID。它展示默认声明，不声称每项都是配置后的有效值或实际扣血结果。公式与例外见 [施法与数值](02-casting-and-balance.md)、[铁魔法接入](05-iron-integration.md)。", "",
             f"## 原生法术（{len(profiles)}）", "",
             "来源：" + link(JAVA / "iron/IronSpellProfile.java") + "。所有法术 ID 的命名空间均为 `royalespells`；学派命名空间为 `irons_spellbooks`。", "",
             "`基础/每级魔力` 是 profile 声明，吟唱单位为 tick、基础 CD 单位为秒；20 tick 按正常服务器速率为 1 秒。`成长` 是每级基础法术强度增量，不能直接当最终伤害百分比。", "",
             table(["ID", "名称", "学派", "最低稀有度", "最高等级", "基础/每级魔力", "基础 CD 秒", "吟唱 tick", "成长"], [
                 [f"`{p['id']}`", label(p['id']), p['school'], rarity_names[p['rarity']], p['maximum'], f"{p['mana']} / {p['per_level']:+d}", p['cooldown'], p['ticks'], p['growth']] for p in profiles]), "",
             "镜像的魔力列只描述自身附加成本；实际施法还处理被复制法术的成本。冰冻/藤蔓另有共享 300 tick 控制冷却；军团与号角另有持久化共享限制，基础 15 秒可经过原生有效冷却计算。虚空伤害另乘 0.6。详见人工章节，不由本表单独推导完整平衡。", "",
             f"## 独立旧卡声明（{len(cards)}）", "",
             "铁魔法／旧工具基础字段（不是新版卡牌最终值）：" + link(JAVA / "Spell.java") + "。新版卡牌见 [卡牌换算表](../../CARD-BALANCE.md)。`duration` 是效果实体的基础生命周期，不等于吟唱时长，也不保证等于控制时长。部分实际半径在效果逻辑中另有常量；基色同样不等于每一层特效颜色。序号是存档/同步使用的 ordinal，已有项不能随意重排。", "",
             table(["ordinal", "ID", "名称", "圣水费用", "radius 格", "duration tick", "基色 RGB"], [
                 [index, f"`{name.lower()}`", label(name.lower()), cost, radius, ticks, f"`#{color.upper()}`"] for index, (name, cost, radius, ticks, color) in enumerate(cards)]), ""]
    cues = data(ASSETS / "spell_audio.json")
    sounds = data(ASSETS / "sounds.json")
    for name, *_ in cards:
        require("deploy" in cues.get(name.lower(), {}), f"Missing deploy cue: {name}")
    sound_source = read(JAVA / "SpellSounds.java")
    scales = re.search(r"RANGE\s*=\s*([\d.]+)f\s*,\s*VOLUME_SCALE\s*=\s*([\d.]+)f", sound_source)
    require(scales is not None, "SpellSounds range/volume structure changed")
    sound_range, volume_scale = map(float, scales.groups())
    audio_rows = []
    for card, phases in cues.items():
        for phase, cue in phases.items():
            event = cue["event"]
            require(event in sounds, f"Unknown sound event: {event}")
            entries = sounds[event]["sounds"]
            require(bool(entries), f"Empty sound event: {event}")
            files = []
            attenuations = []
            for entry in entries:
                require(isinstance(entry, dict) and entry.get("type", "file") == "file", f"Unsupported sound entry: {event}")
                namespace, resource = entry["name"].split(":", 1)
                path = ROOT / f"src/main/resources/assets/{namespace}/sounds/{resource}.ogg"
                require(path.is_file(), f"Missing OGG: {path}")
                files.append(link(path, resource))
                attenuations.append(str(entry.get("attenuation_distance", 16)))
            audio_rows.append([f"`{card}` / {phase}", f"`{event}`", "<br>".join(files), f"{cue['volume']:g}", f"{cue['volume']*volume_scale:g}", "/".join(attenuations)])
    lines += [f"## 法术声音阶段（{len(audio_rows)}）", "",
              f"服务端事件固定传播半径声明为 {sound_range:g} 格，cue 音量乘 {volume_scale:g}。客户端衰减距离单独从 sounds.json 读取；实际声音仍受玩家音量设置、距离和同屏混音影响。", "",
              "表里的“阶段”是配置键，不是自动播放计划。例如大闪虽使用 deploy 键，实际调用在逐目标劈击时。军团与野蛮人专用事件由 ArmySounds / UnitSounds 管理，不包含在本表数量中。素材文件链接指向仓库已有资源；原作来源和复用情况见 ASSETS 与资源来源清单。", "",
              table(["法术/阶段", "事件", "实际音轨", "cue 音量", "缩放后音量", "客户端衰减格数"], audio_rows), ""]
    recipes = sorted((ROOT / "src/main/resources/data/royalespells/recipe").glob("*.json"))
    require(bool(recipes), "No recipe resources found")
    recipe_rows = []
    absent_iron = [{"type": "neoforge:not", "value": {"type": "neoforge:mod_loaded", "modid": "irons_spellbooks"}}]
    for path in recipes:
        recipe = data(path)
        require(recipe["type"] in ("minecraft:crafting_shapeless", "minecraft:crafting_shaped"), f"New recipe type; update renderer: {path}")
        entries = recipe.get("ingredients")
        if recipe["type"] == "minecraft:crafting_shaped":
            entries = [recipe["key"][char] for row in recipe["pattern"] for char in row if char != " "]
        ingredients = Counter()
        for entry in entries:
            require(isinstance(entry, dict) and (set(entry) == {"item"} or set(entry) == {"tag"}), f"New ingredient structure: {path}")
            ingredients[("#" if "tag" in entry else "") + entry.get("item", entry.get("tag", ""))] += 1
        conditions = recipe.get("neoforge:conditions", [])
        present_iron = [{"type":"neoforge:mod_loaded", "modid":"irons_spellbooks"}]
        require(not conditions or conditions in (absent_iron, present_iron), f"New recipe condition; update renderer: {path}")
        result = recipe["result"]
        require(set(result) <= {"id", "count"}, f"New recipe result structure: {path}")
        recipe_rows.append([link(path, path.stem), " + ".join(f"{count}×`{item}`" for item, count in ingredients.items()),
                            f"{result.get('count',1)}×`{result['id']}`", ("未加载铁魔法" if conditions == absent_iron else "已加载铁魔法" if conditions else "无模组条件") + ("；有序摆放，见原文件" if recipe["type"].endswith("crafting_shaped") else "；无序")])
    lines += [f"## 数据包合成配方（{len(recipes)}）", "",
              "旧卡配方只在未加载铁魔法时生效；熔炉法杖是加载铁魔法后的有序配方。原生卷轴锻造和铁砧升级通过 Java 接口接入，不是漏列的 JSON。浓缩顺序、天然重油仪式与等级映射见 [材料与结构](07-elixir-structures-rituals.md)。四种法杖绑定法术单独见 [熔炉法杖与精灵](11-furnace-staff-and-spirits.md)，不计入旧有 30 种 IronSpellProfile 表。", "",
              table(["配方文件", "输入", "输出", "生效条件"], recipe_rows), ""]
    return "\n".join(lines)


def outside_fences(text: str, path: Path) -> str:
    visible = []
    fence = None
    length = 0
    for line in text.splitlines():
        match = re.match(r"^\s{0,3}(`{3,}|~{3,})(.*)$", line)
        if match and fence is None:
            fence, length = match.group(1)[0], len(match.group(1))
        elif match and fence == match.group(1)[0] and len(match.group(1)) >= length and not match.group(2).strip():
            fence = None
        elif fence is None:
            visible.append(line)
    require(fence is None, f"Unclosed code fence: {path}")
    return "\n".join(visible)


def anchors(text: str, path: Path) -> set[str]:
    found = set()
    counts = Counter()
    for line in outside_fences(text, path).splitlines():
        heading = re.match(r"^#{1,6}\s+(.+?)(?:\s+#+)?$", line)
        if not heading:
            continue
        # Sufficient for this manual's headings: retain Unicode letters/digits,
        # underscores and hyphens, strip punctuation, and deduplicate slugs.
        slug = re.sub(r"[^\w\- ]", "", heading.group(1).lower()).replace(" ", "-")
        count = counts[slug]
        counts[slug] += 1
        found.add(slug if count == 0 else f"{slug}-{count}")
    return found


def check_links(paths) -> int:
    total = 0
    for path in paths:
        text = outside_fences(read(path), path)
        for match in re.finditer(r"!?\[[^\]\n]*\]\(([^)\n]+)\)", text):
            target = match.group(1).strip().removeprefix("<").removesuffix(">")
            parsed = urlsplit(target)
            if parsed.scheme or parsed.netloc:
                continue
            location = (path.parent / unquote(parsed.path)).resolve()
            require(location.is_relative_to(ROOT), f"Link escapes repository: {path}: {target}")
            require(location.exists(), f"Broken link: {path.relative_to(ROOT)} -> {target}")
            if parsed.fragment and location.suffix == ".md":
                require(unquote(parsed.fragment) in anchors(read(location), location), f"Missing heading: {path}: {target}")
            total += 1
    return total


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--generate", action="store_true", help="rewrite only source-index.md and spell-reference.md")
    mode.add_argument("--check", action="store_true", help="read-only freshness, chapter, link and resource checks")
    args = parser.parse_args()
    expected = {"source-index.md": source_index(), "spell-reference.md": spell_reference()}
    if args.generate:
        DOCS.mkdir(parents=True, exist_ok=True)
        for name, content in expected.items():
            (DOCS / name).write_text(content, encoding="utf-8", newline="\n")
        print("Generated: " + ", ".join(expected))
    else:
        for name, content in expected.items():
            require((DOCS / name).is_file(), f"Missing generated page: {name}; run --generate")
            require(read(DOCS / name) == content, f"Stale generated page: {name}; run --generate")
        for name in CHAPTERS:
            require((DOCS / name).is_file(), f"Missing chapter: {name}")
        pages = sorted(DOCS.glob("*.md"))
        links = check_links(pages)
        print(f"PASS: {len(pages)} chapters, generated tables current, {links} local links, balanced fences, referenced audio/recipes valid")
        print("Scope: documentation and resource references only; no Minecraft build/runtime tests executed.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (ValueError, KeyError, OSError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        raise SystemExit(1)
