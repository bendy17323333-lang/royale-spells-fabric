# 素材记录

28 张法术卡图直接取自 RoyaleAPI 维护的皇室战争原版素材仓库：

https://github.com/RoyaleAPI/cr-api-assets

图像文件按原始字节保存在 `src/main/resources/assets/royalespells/textures/card/`，未重绘、裁切、缩放或改色。渲染器按卡牌比例绘制，避免非标准原图尺寸影响 Minecraft 方块图集 mipmap。逐图来源及文件哈希见 `ASSET-SOURCES.json`。卡牌美术和相关角色属于 Supercell；本项目为非官方 Minecraft 适配。

机制核对来源：

- 全部法术分类：https://clashroyale.fandom.com/wiki/Category:Spell_Cards
- Zap 重做：https://supercell.com/en/games/clashroyale/blog/release-notes/october-balance-changes/
- 觉醒雪球：https://royaleapi.com/blog/giant-snowball-evolution-new-card-2024-december
- 藤蔓介绍：https://supercell.com/en/games/clashroyale/blog/release-notes/new-season-back-to-drool/
- 英雄滚桶：https://supercell.com/en/games/clashroyale/blog/release-notes/march-update-2026/
- 2026 年 8 月调整：https://supercell.com/en/games/clashroyale/blog/news/final-august-balance-changes-826/

Minecraft 尺度下的具体伤害、半径、存活时间为本项目适配值，并非宣称与皇室战争等级数值一比一一致。

## 新生成的魔法粒子

工具：内置 **image_gen**（不是 CLI/API fallback）。

项目文件：`src/main/resources/assets/royalespells/textures/particle/spell_spark.png`。保留工具生成 PNG 的透明通道。粒子由 `MagicParticle` 加载，配合原版粒子和颜色圆环，用于实际游戏法术效果。

完整提示词：

> Use case: stylized-concept. Asset type: a single Minecraft magic spell particle sprite texture, not a mockup. Create one centered luminous magical spark/rune sprite with true transparent background. Square image. A compact diamond-shaped white-hot core surrounded by four jagged violet magical tendrils, crisp pixel-art edges, Minecraft-compatible blocky pixel art with an apparent 32 by 32 pixel design enlarged with nearest-neighbor pixels. All art fully inside central 75 percent of canvas, transparent padding, no text, no letters, no border, no scene, no checkerboard, no drop shadow. Mainly white with pale violet accents so in-game tinting works for poison, zap, rage, void and curse spells. Output actual transparent PNG suitable for an in-game particle atlas.

## 模型及原版效果复用

- 滚木、尖刺、飞桶、皇家速递箱、火箭外壳和尾翼、雪球形体：在 `SpellRenderer` 中以 Minecraft 方块材质组合几何模型，并按飞行轨迹和滚动进度动态变换。
- 野蛮人和皇家卫兵：`TroopModel` 加载关节几何与已生成的材质图集；野蛮人的剑使用 Minecraft 实际装备的手持物品渲染。
- 小僵尸和持剑骷髅复用 Minecraft 原版模型及贴图。
- 雷电复用原版闪电实体外观；其它效果使用原版音效、可着色粒子、生成魔法粒子及专用运动模型。

## 墓园登场音效（1.0.1）

皇室战争素材归 Supercell 所有。选用 RoyaleAPI 公开素材库的经典 cemetary_deploy_01.ogg，原文件保持不变：

https://raw.githubusercontent.com/RoyaleAPI/cr-api-assets/b4530a1043b213ee2baf9c50a3d0d7fae22c2313/sfx/cemetary_deploy_01.ogg

SHA-256：06A0A9C773A06BA35E9364244C02D3B3CC11F358E5D6DC75F86C83FFDDA5AB94。单声道、44100 Hz、约 3.07 秒。Henrylq 的解包档案将同名素材归入 Cards/Graveyard，可交叉核对用途。具体地址见 AUDIO-SOURCES.json。交付的 WAV 试听版由 Minecraft 自带 OGG 解码器解码为 PCM，仅转换容器与编码，未调整音高、速度或混入其它音轨；模组中使用未改动的 OGG。

## 1.0.2 虚空视觉参考

参考 https://royaleapi.com/blog/void-2024-may?lang=en 的原版游戏截图，使用程序化几何重建暗色领域、红色边缘、亮色核心的三轮下劈光束。参考截图未作为模组贴图打包；本次无需新增位图素材。滚木继续使用 Minecraft 原版橡木材质，修正长轴与滚动轴。

## 1.0.3 素材使用变化

旧 imagegen 紫色星芒已停用，保留文件仅用于资源兼容。新增 Zap 折线电弧、独立领域以及虚空色彩均由程序化几何渲染。狂暴与克隆通过生物主体材质染色和透明渲染层实现；藤蔓、冰壳及拼装火箭使用 Minecraft 原版方块材质构成立体模型，本次没有新增 AI 位图。冻结和缠绕状态图标复用已有状态图标。

## 当前保留的部队材质与模型

1.1.0 使用内置 image_gen.imagegen 生成十六种材质图集并迭代一次，未使用 Python 重绘或拼贴图像。1.1.1 原样复用该图集：`src/main/resources/assets/royalespells/textures/entity/troop_materials.png`。原始与最终修改提示词见 `TROOP-ASSETS.json`。

当前仅保留野蛮人、皇家卫队、野蛮人小屋三个模型。几何由代码制作，眼睛和武器的问题来自代码建模与挂点设计；没有通过浏览器操作 Blockbench。`art/blockbench` 中是从运行时几何导出的可编辑工程，内嵌同一张图集。几何生成脚本为 `art/make-troop-models.py`。

小骷髅恢复 Minecraft 原版骷髅模型和贴图；野蛮人的剑使用 Minecraft 原版铁剑物品模型和贴图。去除石人、女巫、小石人、蝙蝠模型及两张卡牌素材；现存 29 张原版卡图的来源和原始哈希见 `ASSET-SOURCES.json`。

## 野蛮人原版音效（1.1.1）

15 个 OGG 均来自 [RoyaleAPI 固定版本的音效目录](https://github.com/RoyaleAPI/cr-api-assets/tree/b4530a1043b213ee2baf9c50a3d0d7fae22c2313/sfx)，并与 [Henrylq 的 Barbarians 解包分类](https://github.com/Henrylq/Clash-Royale-SFX/tree/c2d7d67271113cb9fe3ad896d9d03dd7f49eed52/Cards/Barbarians) 交叉核对：登场 deploy_barbarians_01，攻击 barbarian_attack_02 至 08，脚步 barb_footstep_01/02/03_no_vo/04/05/06，以及该分类中的通用死亡 npc_die_02。

音频归 Supercell 所有；未剪辑、重编码、变速或变调。逐文件来源、用途、SHA-256 记录于 `BARBARIAN-AUDIO-SOURCES.json`。没有将其它角色的叫声冒充野蛮人待机或受伤语音，这两项不播放语音。


## 1.1.2 造型参考与脚步筛选

野蛮人头部与胡子轮廓参考 [RoyaleAPI 保存的原版部队渲染图](https://raw.githubusercontent.com/RoyaleAPI/cr-api-assets/b4530a1043b213ee2baf9c50a3d0d7fae22c2313/chr/barbarians.png)，网格由 `art/make-troop-models.py` 编写。复用现有材质图集，野蛮人使用白色材质局部采样与顶点配色，避免脸部出现拉伸的发丝与皮肤纹理；没有新增或编辑位图。Blockbench 的网格导出保存顶点配色元数据，最终着色以游戏渲染器为准。

脚步随机池只保留原始 `barb_footstep_03_no_vo.ogg`，五个含人声变体停用，原始字节与哈希档案仍保留；`BARBARIAN-AUDIO-SOURCES.json` 的 `used` 字段标明是否参与播放。脚步播放音量为 0.12、衰减距离 8 格、每个单位最短间隔 8 tick。原版音频未重编码或变调。
