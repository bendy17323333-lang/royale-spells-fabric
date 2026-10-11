# 皇室法术 · Royale Spells 1.9.3

**Minecraft Java 1.20.1 / Fabric / Java 17。独立卡牌版，共 37 张卡。**

基于已发布的 NeoForge Beta 3 内容移植，保留其卡牌数值、模型、原作卡图、音效与特效更新。本版完全移除铁魔法接入、法杖、卷轴、抄写、墨水、奥术铁砧、巫师塔、液体池和号角仪式。四元素精灵与觉醒骷髅军团均改为可合成、可重复使用的独立卡牌，地狱飞龙保留。

## 下载与版本选择

| Minecraft / 加载器 | 最新发布 | 内容 |
| --- | --- | --- |
| 1.20.1 Fabric | [1.9.3](https://github.com/bendy17323333-lang/royale-spells-fabric/releases/tag/v1.9.3-fabric-mc1.20.1) | 37 张独立卡牌，普通/觉醒女巫及两枚蛋，不需要铁魔法 |
| 1.21.1 NeoForge | [1.9.3-beta.4](https://github.com/bendy17323333-lang/royale-spells-fabric/releases/tag/v1.9.3-beta.4-neoforge-mc1.21.1) | 独立女巫卡牌；可选完整铁魔法接入与新增女巫塔 |
| 1.21.1 Fabric | [历史 1.2.0](https://github.com/bendy17323333-lang/royale-spells-fabric/releases/tag/v1.2.0-mc1.21.1) | 旧版移植与录制地图，未包含本轮女巫更新 |

两份新发布均有 JAR、完整源码、验证资料和女巫 VFX 交接包。**给 MineClash 合作开发者：[女巫全部特效分类与接入手册](docs/technical/16-witch-vfx-handoff.md)**，含动画挂点、GLSL、声音、生命周期、性能和许可边界。

## 1.9.3：女巫卡牌与召唤 AI

普通/觉醒女巫各有独立刷怪蛋。小骷髅和小僵尸改用统一目标选择与互斥的战斗/跟随 Goal，避免跟随路径覆盖追敌；女巫护卫会主动寻找附近敌对单位，保留自己的战斗并尝试站在母体前方。配方和详细规则见 [女巫卡牌与 AI](docs/technical/17-witch-cards-and-ai.md)。

[本版更新](CHANGELOG-1.9.3.md) · [验证记录](VALIDATION-1.9.3.md) · [NeoForge 技术档案](https://github.com/bendy17323333-lang/royale-spells-fabric/blob/iron-integration-1.21.1/docs/technical/README.md)

## 1.9.2：女巫比例与施法动作

补齐脖颈与立体衣身，加长普通版双腿；兜帽高约 1.895 格。觉醒版取消双腿，改成悬空空心袍摆。右手拄杖，左手施法，蓄力与弹体从左掌发出。详见 [1.9.2 改动与技术说明](docs/WITCH-1.9.2.md)。

## 历史 1.9.1：女巫模型与动态呈现重做

使用原生 Blockbench 重建兜帽、短脸、金色肩甲与分层袍摆，采用 128×128 像素图集和整体块状手掌；普通女巫保留羊头骨法杖。弹速由每 tick 0.6 格提高到 0.72 格，弹体和体积雾改为连续帧插值。详见 [本版技术增量](docs/WITCH-1.9.1.md) 与 [验证记录](BUILD_STATUS.md)。

新增两张 5 圣水卡牌：女巫、觉醒女巫。真实远程法球命中后产生 1.5 格范围伤害；每 7 秒召唤四只骷髅。觉醒只由自己首波四只非克隆骷髅返回灵魂，在灵魂飞回后治疗，并允许超出初始生命。支持镜像、克隆、玩家攻击指令，以及冰冻/电击暂停和雪球动作减速。

女巫的数值、配方、首波归魂和控制机制见 [玩法及机制说明](docs/WITCH-1.9.0.md)；该历史文档中的旧模型、脸部贴图及弹速由 1.9.1 替代。

## 安装

1. 新建或选用 **1.20.1 Fabric** 实例，游戏使用 **Java 17**。
2. 安装 Fabric Loader **0.16.14 或兼容新版**，以及 [Fabric API 0.92.6+1.20.1](https://modrinth.com/mod/fabric-api/version/0.92.6+1.20.1)。
3. 将 `royale-spells-fabric-1.20.1-1.9.3.jar` 放入 `mods`。同一实例只保留一份 Royale Spells。
4. 不需要 Iron’s Spells、NeoForge、GeckoLib 或 MineClash。本模组需同时装在服务端和客户端。

升级既有 Fabric 实例前请备份存档。旧卡牌 ID 保留，但本轮没有验证旧世界完整迁移；不能把 1.21.1 NeoForge 世界直接降级为 1.20.1 世界。本次发布不自动替换本地游戏实例。

## 使用与获取

工作台合成全部卡牌；配方无形状，详见 [完整合成表](docs/RECIPES.md)。物品可重复使用，不在施放后消失。

- 右键瞄准施法，最多瞄准 32 格；卡牌提示说明圣水花费。
- 圣水上限 10，正常 20 TPS 下每 2 秒回复 1。创造模式不扣圣水。
- 仅持有卡牌时显示范围预览与圣水状态；圆形范围不随台阶扭曲。滚木、滚桶显示长条；觉醒飞桶显示两个落点。
- 镜像复制上一次成功使用的卡，圣水费用 +1，使用 12 级表；其它普通卡使用 11 级表。军团仍保留 3 HP 固定基准与一支军团限制。
- 女巫与觉醒女巫各花费 5 圣水；四精灵各花费 1 圣水；觉醒骷髅军团 3、地狱飞龙 4、野蛮人小屋 6。
- 军团为将军 + 15 只小骷髅，每名玩家同一时间最多一支受将军支持的军团，冷却 15 秒；将军死亡后普通存活骷髅保留，幽灵消散。
- 冰冻持续 3.5 秒，冰冻和藤蔓共用 15 秒冷却。雪球带蓝色染色与动作减速；电击短暂暂停攻击动作。
- Shift + 狂暴／治疗／温暖／克隆可向脚下施法；Shift + 英雄滚桶触发已部署英雄的再次滚动。
- 创造栏包含全部卡、中立军团刷怪蛋，以及普通/觉醒女巫两枚刷怪蛋。管理员可用 `/royalespells give` 获得全部卡；`/royalespells give fire_spirit` 可给单卡；`/royalespells refill` 补满圣水；`/royalespells clear` 清理自己的召唤物与法术。

28 张法术含普通及觉醒小电、雪球、飞桶，以及活动／历史法术；另有野蛮人小屋、地狱飞龙、四精灵、觉醒骷髅军团、女巫与觉醒女巫。旧录制地图的上一场／下一场物品与场景识别保留；本次没有附带新录制地图。

## 数值、技术与许可证

[数值表](CARD-BALANCE.md) · [技术说明](docs/TECHNICAL.md) · [验证记录](BUILD_STATUS.md) · [素材出处](ASSETS.md)

自编代码、配置、构建脚本及技术文档按 [MIT](LICENSE) 开源。皇室战争原版卡图、音频、MineClash 改编素材等第三方资产明确排除，见 [许可范围](LICENSE-NOTICE.md)。MineClash 冰精灵出处：[原项目](https://www.curseforge.com/minecraft/mc-mods/mineclash)。本项目为非官方玩家模组。

构建需要 JDK 21 启动 Gradle 9.4.1，并安装 JDK 17 运行游戏测试；产出 Java 17 字节码。执行 `gradlew.bat build`，产物在 `build/libs`。`gradlew.bat runGametest` 运行 51 项独立测试；`gradlew.bat runVisualSmoke` 在新建的隔离世界截图并退出。普通游戏启动不会执行测试或自动新建世界。
