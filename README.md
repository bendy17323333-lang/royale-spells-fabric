# 皇室法术 · 铁魔法适配 Beta 1

**Royale Spells `1.5.2-beta.1` · Minecraft Java 1.21.1 · NeoForge**

把皇室战争法术融入 Iron’s Spells ’n Spellbooks 的法术系统：学派、稀有度、等级、魔力、卷轴制作、抄写、提取、奥术铁砧升级、法术书和武器施法。包括原有 28 种法术、野蛮人小屋，以及新的觉醒骷髅军团，共 **30 种铁魔法原生法术**。

这是铁魔法完整适配的**首个公开 Beta**，发布为 GitHub 预发布版。独立卡牌玩法仍可在没有铁魔法时运行；军团卷轴、号角和墨水替代流程需要铁魔法。

## 下载和安装

[下载 Beta 1](https://github.com/bendy17323333-lang/royale-spells-fabric/releases/tag/v1.5.2-beta.1-neoforge-mc1.21.1)。安装 `royale-spells-neoforge-1.21.1-1.5.2-beta.1.jar` 到 NeoForge 实例的 `mods`；同一实例只保留一个皇室法术 JAR。更新前退出游戏并备份存档。本版不能放进 Fabric 实例。

| 组件 | 本次验证版本 |
| --- | --- |
| Minecraft / Java | 1.21.1 / 21 |
| NeoForge | 21.1.249（最低声明 21.1.200） |
| Iron’s Spells ’n Spellbooks | 1.21.1-3.16.3 |
| GeckoLib | 4.9.2 |
| playerAnimator | 2.0.4+1.21.1 |
| Curios | 9.5.1+1.21.1 |
| Iron’s Lib | 1.21.1-2.1.0 |

第三方模组没有包含在下载包里；通过各模组的官方发布渠道安装。多人模式客户端、服务端使用相同模组版本。源码 ZIP 用于开发，不要放入 `mods`。

## 生存获取

- 普通皇室法术进入铁魔法对应学派的卷轴候选池，也可通过卷轴锻造台制作，再抄写到法术书或附到可施法武器。学习与启用条件遵循铁魔法配置。
- 新生成的火术师塔附近有概率出现圣水池；原地下室更深处有概率出现暗黑重油密室。用玻璃瓶打捞，用桶搬运；材料可代替对应稀有度的墨水制作和升级皇室法术。圣水池尝试概率 65%，重油密室概率 35%，不会回填已探索的塔。
- 普通小电、巨大雪球、小僵尸飞桶卷轴加浓缩暗黑重油，可在奥术铁砧变成对应觉醒卷轴。
- 将任意等级的铁魔法亡者召唤或皇室墓园卷轴扔进**天然重油源方块**，完成仪式后得到觉醒骷髅军团卷轴。再把该卷轴与山羊角一起投入天然池，消耗卷轴并赋予号角召唤能力。
- 军团为 15 只小骷髅和 1 名将军；卷轴、法术书、号角共享每人一支军团的限制和基础 15 秒冷却。号角不耗魔力，将军仍存活时不能召唤下一支。

详细流程见 [铁魔法系统](IRON-INTEGRATION.md)、[材料与数值](BALANCE-AND-ELIXIR-1.4.0.md) 和 [重油仪式、卷轴与号角](ARMY-AND-AUDIO-1.5.0.md)。旧文档开头已注明后续规则修订。

## Beta 1 的表现与战斗规则

- 镜像复制上一种成功施放的皇室或铁魔法法术并提高一级，保留原法术前置条件与取消机制。
- 冰冻固定持续 5 秒，生物动画同步冻结；冰冻与藤蔓共享 15 秒控制冷却。藤蔓时长沿用现有数值。
- 召唤部队优先攻击玩家指定的目标，其次攻击附近敌对单位；不会主动猎杀无敌意的和平或中立生物。小骷髅移动加快，去掉小骷髅攻击和无明确击退法术的额外击退。
- 小骷髅以 Minecraft 原版骨架为基础。将军头盔保留两侧护颊，眼睛至嘴部是连通的 T 字开口，盔身上窄下宽、轻微外扩；保留发光眼睛、旗帜、盾牌及蓄力前刺动作。
- 觉醒军团部署和觉醒小僵尸飞桶的两个落点有短促紫色闪光、地面扩散和渐隐碎晶。每组只触发一次，约 1.2 秒消散，不增加伤害或碰撞。
- 使用原作卡图和音效素材，补全虚空三次劈击、大闪逐目标霹击及将军破盾声音。法术音效传播至 64 格，近处音量略降，随距离衰减。部分阶段复用原作其它音轨，具体来源见素材清单。
- 恢复原生十字准星和稳定范围预览；长条滚动轨迹、双飞桶落点、铁魔法卡图原比例与去重复边框均保留。

独立军团实验蛋：`/give @s royalespells:neutral_skeleton_army_spawn_egg`。该实验军团不属于玩家，行为与玩家召唤军团不同。

## 验证与反馈

发布前通过 51 项独立 GameTest、107 项铁魔法同装 GameTest，并使用最终打包 JAR 运行独立 Minecraft 客户端检查。验证范围和实际截图见 [Beta 1 验证记录](VALIDATION-1.5.2-beta.1.md)。这不代表已覆盖所有整合包、多人服务器或光影组合。

反馈时请附模组版本、加载器版本、铁魔法版本、复现步骤与 `latest.log`。完整变化见 [Beta 1 更新说明](CHANGELOG-1.5.2.md)。

## 开发与旧版

使用 Java 21：

```powershell
.\gradlew.bat build --console=plain
.\gradlew.bat runGameTestServer --console=plain
.\gradlew.bat runCompatGameTest -PcompatModsDir=/path/to/compat-mods --console=plain
```

最后一条命令需要自行准备上表中的铁魔法及依赖 JAR。源码分支为 `iron-integration-1.21.1`。

旧版下载：[Fabric 1.20.1](https://github.com/bendy17323333-lang/royale-spells-fabric/releases/tag/v1.2.0)、[Fabric 1.21.1](https://github.com/bendy17323333-lang/royale-spells-fabric/releases/tag/v1.2.0-mc1.21.1)、[NeoForge 1.2.0](https://github.com/bendy17323333-lang/royale-spells-fabric/releases/tag/v1.2.0-neoforge-mc1.21.1)。旧版 26 场录制地图仍可从对应版本发布页单独下载。

本项目为非官方粉丝作品，与 Supercell、Mojang 及 Iron’s Spells 作者没有隶属关系。原作卡图、声音等第三方素材归其权利人所有；来源及使用说明见 [ASSETS.md](ASSETS.md)。
