# 皇室法术 · 铁魔法适配 Beta 3

**Royale Spells `1.7.1-beta.3` · Minecraft 1.21.1 · NeoForge · Java 21**

将皇室战争法术接入 Iron’s Spells ’n Spellbooks 的魔力、学派、稀有度、卷轴、抄写、法术书和奥术铁砧。新版汇总地狱飞龙、雪球与火球视觉更新、电击暂停和卡牌数值重做。

[下载 Beta 3](https://github.com/bendy17323333-lang/royale-spells-fabric/releases/tag/v1.7.1-beta.3-neoforge-mc1.21.1) · [本版更新](CHANGELOG-1.7.1.md) · [卡牌数值与来源](CARD-BALANCE.md) · [验证记录](VALIDATION-1.7.1-beta.3.md) · [完整技术档案](docs/technical/README.md)

## 开源许可证

自编代码、工具和文档采用 **[MIT](LICENSE)**。原作卡图、音频、MineClash 原始和改编素材等第三方资产明确排除，保留其原作者权利；详见 [LICENSE-NOTICE.md](LICENSE-NOTICE.md) 与 [ASSETS.md](ASSETS.md)。Original code is MIT licensed; third-party assets are excluded and retain their respective terms.

## 本版重点

- 卡牌以原作 11 级小骷髅 81 HP 对应模组 3 HP：伤害与召唤生命除以 27，范围一格一个方块，镜像使用 12 级数据。
- **只调整卡牌形态与指定的觉醒军团**。普通铁魔法法术、精灵法杖及 MineClash 数值不套用换算。卡牌冰冻同步为 3.5 秒，铁魔法版仍为 5 秒。
- 觉醒军团小骷髅、将军、盾各 3 HP；修复持剑隐藏加成、诅咒／小屋后续召唤的等级传递和卡牌卫队独立盾。
- 连续火球尾焰、落地火星与黑烟；雪球蓝色动画减速和朝向修复；地狱飞龙模型、悬浮高度、逐级升温激光与被电击重置。
- 电车附属提供三车编队、原作素材、连线电弧及可续接的电击暂停，保留雨水自伤。

## 安装

下载 `royale-spells-neoforge-1.21.1-1.7.1-beta.3.jar` 放入 `mods`。电车功能还需同次发布的 **`mineclash-zappies-neoforge-1.21.1-0.1.1.jar`** 和 MineClash 0.7.5；只装皇室法术与 MineClash 不会加载这个独立附属的机制或电弧。

`royale-spells-1.7.1-beta.3-with-zappies.zip` 包含这两份 JAR、安装说明、许可和数值表，第三方依赖另装。`source.zip` 是源码与可编辑模型，不要放入 mods。更新前保存并退出游戏、备份旧 JAR 与存档，同一实例只留一份本体和一份电车附属。

| 依赖 | 本轮验证版本 |
| --- | --- |
| Minecraft / Java | 1.21.1 / 21 |
| NeoForge | 21.1.249（声明最低 21.1.200） |
| Iron’s Spells ’n Spellbooks | 1.21.1-3.16.3 |
| GeckoLib | 4.9.2 |
| playerAnimator | 2.0.4+1.21.1 |
| Curios | 9.5.1+1.21.1 |
| Iron’s Lib | 1.21.1-2.1.0 |

第三方依赖请从各自官方渠道安装，本发布包不附带。未安装铁魔法时仍能使用独立卡牌玩法；熔炉法杖、卷轴、号角仪式等原生系统功能需要铁魔法。MineClash 无需安装。多人模式客户端与服务端使用相同版本。

## 四元素熔炉法杖

工作台使用炼药锅、火焰弹、两个烈焰棒和铁锭制作烈焰法杖；精确摆法见 [配方与使用说明](docs/technical/11-furnace-staff-and-spirits.md)。把法杖放入奥术铁砧左槽，右槽放对应元素材料即可改换元素；保留名称、损耗和附魔。

| 精灵 | 铁砧材料 | 基础伤害 | 额外效果 |
| --- | --- | ---: | --- |
| 烈焰 | 火焰弹 | 10 | 半径 2.4 格爆发，不破坏方块 |
| 寒冰 | 浮冰 | 7 | 半径 2.2 格；冻结 1 秒 |
| 雷电 | 紫水晶碎片 | 7 / 目标 | 每 0.25 秒向 3 格内下一敌人连锁，最多 9 个目标，眩晕 0.5 秒 |
| 治疗 | 闪烁的西瓜片 | 7 | 半径 2.2 格伤害；3 格内友军恢复 4 生命 |

右键走铁魔法原生吟唱和魔力扣除。四种元素共享 6 秒基础冷却，不可切换法杖绕过；伤害随法术和召唤强度成长。治疗精灵优先照顾受伤友军。基础伤害足以一击击杀本模组铁魔法基础 6 生命小骷髅以及本版 3 生命卡牌小骷髅；装备、强化、护甲和其他模组抗性会改变实战结果。

四精灵使用原作卡图、原比例和自带边框，铁魔法额外图标框仅对皇室卡牌隐藏。火精灵为全身燃烧的煤块；电精灵采用三处分叉冠簇与向外延伸的闪电胡须。四种精灵接入原作部署、跳扑、命中和低音量脚步声音。

## 觉醒骷髅军团

- 小骷髅普通与虚化状态的真实近战均绕过目标的受伤间隔，不产生攻击击退。
- 小兵更积极地冲到前排，受阻时尝试从侧面绕行；将军稍慢跟进，敌人贴身时迎战，不主动往后逃。
- 将军阵亡仅使幽灵消散。未虚化小骷髅保持当前血量、主人和剩余寿命继续战斗，之后不再虚化。
- 卷轴与附魔山羊角保留共享冷却和觉醒军团限制；号角不消耗魔力。

## 其它生存获取

普通皇室法术参与铁魔法卷轴候选池，也可在卷轴锻造台制作、抄写到法术书。新生成的火术师塔附近有概率出现圣水池，深层密室有暗黑重油。圣水瓶可替代皇室法术的墨水材料，浓缩圣水用于较高阶材料；天然重油支持觉醒骷髅军团卷轴和号角仪式。获取、配方、天然池判定与共享冷却详见 [圣水、结构与仪式](docs/technical/07-elixir-structures-rituals.md)。

## 模型与声音出处

**冰精灵模型、皮肤与发光遮罩改编自 [MineClash](https://www.curseforge.com/minecraft/mc-mods/mineclash)，作者 LiziYowo / MineClash 团队，源版本 0.7.5。** 冰法杖采用同一头部；感谢原作者的模型工作。改动涉及尺寸、骨骼和运行时导出，没有捆绑 MineClash 代码。

火、电、治疗精灵及熔炉法杖的几何在 Blockbench 中制作，材质与火焰由 ImageGen 生成。皇室战争角色、原卡图及原作音效归 Supercell；卡图取自固定版本的 RoyaleAPI 资源档案，精灵音效取自固定版本的 Henrylq/Clash-Royale-SFX 分类档案。共享音轨和单声道处理均有记录。

[模型与贴图来源](SPIRIT-ASSET-SOURCES.json) · [精灵原作音效出处](SPIRIT-AUDIO-SOURCES.json) · [原卡图出处](SPIRIT-CARD-SOURCES.json) · [完整素材说明](ASSETS.md) · [可编辑 Blockbench 工程](art/blockbench-v3/README.md)

源码公开不等于重新授予第三方素材许可；各素材保留原归属及许可。本项目自编代码采用 MIT；第三方资产的排除范围见 LICENSE-NOTICE.md。

## 验证与反馈

本版通过 96 项独立 GameTest、162 项 Iron/MineClash/电车同装 GameTest，独立电车附属通过 12 项。新增回归覆盖完整卡牌伤害、原生数值隔离、持剑伤害、盾牌、镜像及军团旧存档，原有精灵、召唤 AI、电击暂停与生还者测试继续保留。成品客户端的具体结果与截图见 [验证记录](VALIDATION-1.7.1-beta.3.md)。测试不等于覆盖所有整合包、多人延迟、复杂地形或光影。

反馈请附版本、加载器、铁魔法版本、复现步骤与 `latest.log`。开发入口、准星渲染、音频、数值、AI、模型和存档说明见 [技术档案](docs/technical/README.md)。

## 历史版本

[铁魔法 Beta 1](https://github.com/bendy17323333-lang/royale-spells-fabric/releases/tag/v1.5.2-beta.1-neoforge-mc1.21.1) · [Fabric 1.20.1](https://github.com/bendy17323333-lang/royale-spells-fabric/releases/tag/v1.2.0) · [Fabric 1.21.1](https://github.com/bendy17323333-lang/royale-spells-fabric/releases/tag/v1.2.0-mc1.21.1) · [NeoForge 独立 1.2.0](https://github.com/bendy17323333-lang/royale-spells-fabric/releases/tag/v1.2.0-neoforge-mc1.21.1)。旧版 26 场录制地图仍从对应历史发布页下载；本次不宣称重新制作了地图。
