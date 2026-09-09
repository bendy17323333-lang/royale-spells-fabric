# 素材记录

## 代码许可与第三方资产排除

**本项目自编代码、构建／工具脚本、配置与技术文档采用 [MIT 许可证](LICENSE)。第三方资产不在该许可范围内。** 完整的中英文范围说明见 [LICENSE-NOTICE.md](LICENSE-NOTICE.md)。下文明确记录为本项目独立制作或 ImageGen 生成、未混入第三方内容的新素材，其可由本项目授予的原创贡献同样采用 MIT；这不包含原作角色设计、商标或第三方改编素材的权利。

| 内容 | 适用说明 |
| --- | --- |
| 自编 Java、渲染／动画算法、法术／AI 机制、兼容层、测试和工具 | MIT，允许修改、再分发与商业使用，保留许可声明 |
| 《皇室战争》卡图、原作参考图、角色图、音频及转换版本 | **排除**；Supercell 等原权利人保留权利 |
| MineClash 冰精灵及冰法杖同源头部的模型、贴图、发光遮罩、工程和导出物 | **排除**；LiziYowo / MineClash 团队保留权利，源项目为 [MineClash](https://www.curseforge.com/minecraft/mc-mods/mineclash) |
| Minecraft 运行时原版模型、纹理、声音 | **排除**；Mojang/Microsoft 的资源条款不变 |
| Gradle Wrapper 与其它第三方软件 | 保留各自上游许可证；不被本项目重新许可 |

卡图通常位于 `src/main/resources/assets/royalespells/textures/card/` 与 `textures/gui/spell_icons/`，原作声音位于同一资源命名空间的 `sounds/`。MineClash 相关路径包括 `textures/entity/spirit_ice_mineclash*.png`、`models/troop/spirit_ice.json`、`models/item/furnace_staff_ice.obj` 和 `art/blockbench-v3/` 中对应冰精灵／冰法杖工程；各版本存在的文件以其来源清单为准。

这些排除同样适用于复制、改编、转换及嵌入代码或模型工程的资产数据。出处和已有使用授权不能替代原权利人的再许可；**含第三方资产的完整 JAR 不能被整体描述为“全部素材均为 MIT”。** 下文和 `*-SOURCES.json` 保留原始来源记录。

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
- 雷电复用 Minecraft 闪电实体外观；音效当前以文末 1.3.2 素材清单为准。其它效果使用可着色粒子、生成魔法粒子及专用运动模型。

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

## 1.3.0 墓园飘散粒子

使用内置 image_gen.imagegen 生成专用紫色幽光粒子，未重绘或后处理；原始 PNG 保留透明 alpha。完整提示词、路径及哈希见 [GRAVEYARD-PARTICLE-SOURCE.json](GRAVEYARD-PARTICLE-SOURCE.json)。虚空、狂暴与落点预览继续使用代码几何。原卡图原样复制到铁魔法图标路径，不重新绘制。

## 1.3.2 法术原作音频与程序特效

28 种法术与野蛮人小屋共 29 个音效配置，引用 46 个公开原作音频资源，组成 54 个施放／命中等阶段事件。完整分类路径、固定版本 URL、原始和安装后 SHA-256 见 [SPELL-AUDIO-SOURCES.json](SPELL-AUDIO-SOURCES.json)。主要取自 [Henrylq 原作音效分类固定版本](https://github.com/Henrylq/Clash-Royale-SFX/tree/c2d7d67271113cb9fe3ad896d9d03dd7f49eed52/Cards)，并参考 RoyaleAPI 的原作素材分类。导入脚本为 `tools/import-spell-audio.py`；需要 Python 3 与 FFmpeg（可用 `--ffmpeg` 指定）。

原始单声道文件按字节保留；立体声音频经 FFmpeg 转为单声道，以支持 Minecraft 距离衰减，未变速或变调。墓园保留旧版经典 `graveyard_deploy` 事件。温暖暂复用 `heal_magic_03`（目前归类于治疗精灵），派对火箭使用原作火箭音及派对小屋庆祝音；这两项不声称已找到单独确认的专属音轨。野蛮人小屋使用原作通用建筑放置音及既有野蛮人声音。下载探索时保留的 `card_epic_vines_hit.ogg` 为同版本 Vines 分类素材，当前未注册或播放。

毒雾、气泡、诅咒雾气、恢复光点和地震脉冲由 `AmbientSpellRenderer` 绘制；电击由 `ZapRenderer` 绘制。此次没有生成或重绘卡图，也没有用末影粒子代替墓园专用粒子。铁魔法 GUI 直接读取原卡图宽高比绘制，不改变图片文件。

## 1.4.0 圣水与暗黑重油

新液体通过 NeoForge 原生流体渲染复用 Minecraft 动态水纹，并在运行时着色；瓶子复用原生药水瓶分层模型，桶使用 NeoForge 流体容器模型。未生成或捆绑新的第三方贴图，也未用 image-gen 改写原版卡牌美术。

## 1.5.0 骷髅军团与声音

按用户最后反馈，删除最初的圆润骷髅几何资源，使用运行时 Minecraft `SkeletonModel`、`ModelLayers.SKELETON` 和原生 `textures/entity/skeleton/skeleton.png`；没有把 Minecraft 骷髅贴图复制进模组。`GeneralEquipment` 以 Minecraft 方块几何制作头盔、木盾、旗杆、分段旗帜与披风，复用既有 `troop_materials.png` 材质图集的白色区域着色，本次没有新增 AI 位图。装备轮廓参考本轮原作将军图片，身体造型以用户指定的 Minecraft 原版骨架为准。

觉醒骷髅军团图标来自 RoyaleAPI/cr-api-assets 固定版本 `b4530a1043b213ee2baf9c50a3d0d7fae22c2313` 的 `cards/skeleton-army-ev1.png`。7 个声音取自 Henrylq/Clash-Royale-SFX 固定版本 `c2d7d67271113cb9fe3ad896d9d03dd7f49eed52`：军团与骷髅专属声音来自 Skeleton Army／Skeletons 分类，通用死亡 `npc_die_02` 来自 Zappies 分类；原始 URL 和转换前后哈希见 `ARMY-ASSET-SOURCES.json`。单声道转换不变速、不变调，骷髅单位播放时的轻微音高参数属于运行时配置。

虚空三次劈击复用已导入的原作 `zap_02.ogg` 电击声，不重复播放 6 秒领域音轨。素材档案没有单独的虚空劈击文件；来源清单明确记录复用。`tools/update-audio-1.5.py` 在既有导入器之后统一应用 64 格衰减和三次劈击配置。

仪式的环阵、光束、物品轨道、闪光、将军攻击与旗帜摆动均由代码实时绘制。飘散粒子复用已记录来源的墓园紫色粒子，不使用末影粒子。

## 1.5.1 additions

General shield loss reuses the original Clash Royale Guards cue `shield_skele_lost_02.ogg`, pinned upstream and converted to mono without pitch/time changes. This archive does not identify a General-specific shield cue. See ARMY-ASSET-SOURCES.json. The general helmet geometry now ends above the vanilla skeleton jaw, referenced against the original Supercell render shown in the RoyaleAPI Skeleton Army Evolution gallery (evo-skarmy-a-288-6.jpg). No new generated textures are used.

## 1.5.2-beta.1 部署与头盔

将军头盔按当前用户要求改为连通 T 字开口及轻微上窄下宽的桶形。觉醒部队部署特效由 `EvolutionBurstRenderer` 的紫色渐变几何实时绘制，参考原作实机片段 https://www.youtube.com/watch?v=cXGNteyi6Yc 的短促展开与消散节奏；未导入视频或新增贴图。


## 本地 Beta 2：精灵与熔炉法杖

此段记录 Beta 2 历史实现。四精灵的原作参考与新建几何见 [SPIRIT-ASSET-SOURCES.json](SPIRIT-ASSET-SOURCES.json)。法杖使用内置 image_gen 生成的专属纹理图，旧提示词见 [FURNACE-IMAGEGEN-PROMPT.md](art/FURNACE-IMAGEGEN-PROMPT.md)。旧几何生成程序归档在 `art/legacy-beta2`；原脚本入口现转调 Beta 3 的 Blockbench 导出器。

## 本地 Beta 3–4：Blockbench 重做与 MineClash 冰精灵

当前八份模型在实际 Blockbench 中经 MCP 创建、导入、检查并导出，工作源见 [Blockbench 说明](art/blockbench-v3/README.md)。火、电、治疗精灵以方块主体、短方肢和贴面像素五官替代圆润设计，四种法杖同步改为切角方锅和相应精灵头。

`spirit_materials.png` 和 `spirit_flame.png` 由内置 ImageGen 生成，原始 PNG 不改动；最终提示词见 [PIXEL-FINAL-PROMPTS.md](art/blockbench-v3/PIXEL-FINAL-PROMPTS.md)。后者为四帧透明像素火焰。物品图集仅在内存上传时生成合适尺寸的最近邻副本，以避免奇数帧尺寸影响全局 mipmap。Beta 4 火焰包围整个主体，电精灵改为短紫色冠簇。

Beta 3 的模型渲染图标已被替换。四张精灵法术图标采用原作卡牌 PNG，从固定 RoyaleAPI 提交复制原字节，保持 302×363 原比例、自带边框，逐文件来源见 [SPIRIT-CARD-SOURCES.json](SPIRIT-CARD-SOURCES.json)。原作图片归 Supercell；仅图标打入 JAR，参考图不打包。

冰精灵的几何、皮肤与发光遮罩来自用户提供并明确允许使用的 `mineclash-0.7.5-1.21.1.jar`；皮肤和遮罩保持原始字节，几何等比缩放并适配本模组骨骼。原作者为 LiziYowo、YangXuKun、4y4u、AX_ZHANG、LieNiaoBiBai，原模组声明 All Rights Reserved。资源保留原作者归属，说明随运行 JAR 的 `assets/royalespells/model_credits.txt` 提供。没有运行或合并该 JAR 的代码，也没有完整移植其 GeckoLib/Molang 动画。

其它 MineClash 模型仅用于三维轮廓和材质研究，见 [模型研究记录](art/mineclash-reference/MODEL-STUDY.md)。Supercell/RoyaleAPI 参考图与整个 MineClash JAR 不打入本模组；本版未公开发布。逐文件来源、用途与哈希见 [SPIRIT-ASSET-SOURCES.json](SPIRIT-ASSET-SOURCES.json)。


## 公开铁魔法 Beta 2（1.6.1-beta.2）

冰精灵模型、原贴图和发光遮罩改编自 **[MineClash](https://www.curseforge.com/minecraft/mc-mods/mineclash)**，作者 **LiziYowo / MineClash 团队**，源版本 0.7.5；冰法杖使用同一头部。未捆绑 MineClash 代码，不需要安装 MineClash。

四精灵音效通过原作分类音轨导入，共 22 个 OGG、16 个事件；出处、原文件与单声道转换后哈希和共享片段说明见 [SPIRIT-AUDIO-SOURCES.json](SPIRIT-AUDIO-SOURCES.json)。模型、原卡图和声音的原始归属保留；本版不把第三方素材声明为本项目原创。

## 本地 1.7.0-dev.1 · 地狱飞龙

地狱飞龙几何由本项目在 Blockbench 经 MCP 制作，参考用户提供的皇室战争原作图片与 MineClash 的整体美术风格，没有导入 MineClash 飞龙模型。15 骨骼、173 网格的最终源、GLTF、离线预览和导出器见 [模型说明](art/inferno-dragon/README.md)。专属材质图集由内置 ImageGen 生成，保留原始 PNG；[提示词](art/inferno-dragon/texture-prompt.txt) 随源提供。

卡图取自固定 RoyaleAPI/cr-api-assets 提交 `b4530a1043b213ee2baf9c50a3d0d7fae22c2313` 的 `cards/inferno-dragon.png`，直接复制到卡牌与铁魔法图标路径。部署、振翅与喷射取自固定 Henrylq/Clash-Royale-SFX 提交 `c2d7d67271113cb9fe3ad896d9d03dd7f49eed52` 的 `Cards/Inferno Dragon` 分类，三个原始单声道 OGG 保留原字节；运行时调整音量和喷射音高。原作角色设计、卡图和音频归 Supercell。

完整 URL、原始和导入后 SHA-256、解码验证记录见 [地狱飞龙素材清单](art/inferno-dragon/asset-sources.json)。成品 JAR 中也保存来源摘要 `assets/royalespells/inferno_asset_sources.json` 和更新后的 `model_credits.txt`。该版本仅本地交付，未新增第三方素材许可声明。

## 本地 1.7.0-dev.5 · 火球与寒冷视觉

火球核心使用内置 ImageGen 新生成的专用炽热材质，PNG 按原字节导入；[完整提示词](art/FIREBALL-IMAGEGEN-PROMPT.md) 与尺寸、哈希记录随源提供。切面模型、火舌、沿轨迹拖尾、爆散和新增毒雾均由代码绘制，没有复制 MineClash 的火球资源。法术卡图及音效继续使用上文已注明的原作素材。

雪球寒冷状态的小图标复用本项目已有冰冻图标，原图不变；蓝色生物通过渲染时的颜色乘数实现，不重绘或替换生物皮肤。本轮没有修改四精灵、军团或地狱飞龙的原有素材归属。

## 本地 1.7.0-dev.6 · 火球尾焰

`textures/entity/fireball_plume.png` 是本轮通过内置 ImageGen 新生成的专用四帧火焰团图集。PNG 按原字节导入，保留透明度；没有复制或改编 MineClash 火球素材。它由短小、重叠的摄像机朝向面片显示，沿真实飞行轨迹形成连续燃烧尾迹；不是将此前精灵火焰拉成长条。完整提示词见 [art/FIREBALL-PLUME-IMAGEGEN-PROMPT.md](art/FIREBALL-PLUME-IMAGEGEN-PROMPT.md)，文件哈希见 `docs/validation-dev6/asset-verification.json`。

火球核心继续使用上一版独立 ImageGen 材质。烟雾、细小火星与短促火焰通过 Minecraft 粒子系统在运行时引用，未复制这些原版纹理进 JAR。上述原版资源依旧属于 Mojang/Microsoft。本轮独立生成的火焰贡献按根目录许可范围采用 MIT；《皇室战争》卡图与原作音效继续明确排除。
