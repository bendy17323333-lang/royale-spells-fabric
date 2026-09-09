# MineClash · 电击车小队附属 0.1.1

Minecraft **1.21.1 / NeoForge**。在 MineClash 原有电击车实体上增加三车部署、独立攻击计时、编队寻路、电弧和可恢复的电击暂停。

模型、贴图、动画文件、刷怪蛋图标、攻击和死亡声音都由 **MineClash 本体**提供。本附属不捆绑或重画这些素材，也不捆绑 MineClash JAR。

## 安装

客户端和服务器均安装：

- `mineclash-zappies-neoforge-1.21.1-0.1.1.jar`
- MineClash `0.7.5-1.21.1`
- GeckoLib `4.9.2`（实际验证版本）
- NeoForge `21.1.249`、Java 21（实际验证环境；最低 NeoForge 沿用本体的 21.1.219）

**可以只搭配 MineClash 使用，不需要皇室法术或铁魔法。**

若同时安装皇室法术，请将旧版替换为本次提供的 **`royale-spells-neoforge-1.21.1-1.7.0-dev.4.jar` 或更新版**。旧版皇室法术的冻结渲染兼容层会污染共享骨骼，即使没有施放冰冻，也可能让其它同类继承攻击或死亡姿势。可选依赖版本检查会阻止已知有问题的旧组合启动。详见 [动画故障调查](docs/animation-investigation.md)。

先保存并退出游戏、保留旧 JAR 备份，再替换。同一实例内不能保留两份皇室法术。本附属与皇室法术 Beta 3 一起提供下载；使用电车功能必须安装这个独立 JAR，只装 MineClash 与皇室法术不会启用本附属的连线电弧。

## 使用

用 MineClash 原有的 **电击车刷怪蛋**右键有足够空间的地面，一次部署三辆车，消耗一个刷怪蛋；创造模式不消耗。三辆车以小三角排列，分别经过 20 / 22 / 24 tick 部署时间再行动。空间不够时整组不生成、不消耗。

管理员命令：

```mcfunction
/zappies squad
/zappies squad 11
/zappies neutral 11
/give @s mineclash:zappies_spawn_egg
```

`squad` 生成属于执行者的三车小队，`neutral` 生成无主小队。等级范围 3～16。普通 `/summon mineclash:zappies` 仍生成单辆；刷怪笼、发射器和水面使用刷怪蛋保持原有生成入口，不自动扩展为三辆。

小队优先响应主人的近期攻击目标，其次保护主人和攻击附近敌对生物。不会主动扫射和平生物、自己的主人、同队车辆或主人的驯服动物。原版记分板队伍继续生效；工具明确指定的非友军目标也能参与测试。

## 战斗参数

| 参数 | 默认实现 |
|---|---|
| 数量 | 3 辆，每辆独立生命和攻击状态 |
| 首次抬手 | 0.8 秒（16 tick） |
| 持续攻击周期 | 2.3 秒（46 tick，按 2026 年 9 月最终平衡公告） |
| 电击 | 0.5 秒；刷新剩余时间，不叠加延长 |
| 射程 | 水平 4.5 格，中心高度差最多 6 格；必须有视线 |
| 攻击对象 | 地面及空中单位，单体命中 |
| 移动 | 地面寻路，平地约 1 格/秒；不飞行、不穿透实体 |
| 11 级单车生命 | 529 / 26 ≈ 20.35 |
| 11 级单车伤害 | 117 / 26 = 4.5，仍经过目标的护甲和伤害事件 |
| 击退 | 不附带普通受击击退 |
| 淋雨 / 涉水 | 保留 MineClash 原有自伤；电击暂停时也不会免除 |

生命和伤害采用同一个换算比例 **1/26**，保留等级间比例并接近 MineClash 的数值尺度，未把原作数百点伤害直接搬进 Minecraft。普通目标每一颗红心等于 2 生命值。三个同刻到达的电击分别结算，随后恢复其它伤害来源原本的受伤间隔。

`config/zappiesaddon-common.toml`：

```toml
defaultLevel = 11
statScale = 0.038461538461538464
hitIntervalTicks = 46
```

`statScale = 1` 使用原卡表的原始生命/伤害；`hitIntervalTicks = 44` 可回到 8 月的 2.2 秒周期。等级和属性设置作用于新部署车辆，修改后重启游戏/服务器。已保存的车辆保留已写入的属性。

## 电击动作与特效

电击会**停住正在进行的抬手、施法和动作，结束后继续剩余进度**。不调用清空目标、停止 Goal、取消施法或重启动画。已经发射的箭和独立飞行物继续运动。

**地狱飞龙是明确的例外**：皇室法术 dev.4 的飞龙受到任意这类电击时，会立即清空蓄热、激光锁定和命中计时。解除后保留敌人目标，从第一阶段重新蓄热和攻击；暂停期间也保持飞行位置。

线圈放电时显示连到目标的白芯蓝色短电弧，带分叉、快速闪动和命中火花；约 0.25 秒消退。原生攻击动画的回弹帧与伤害时刻对齐，原动画文件没有改动。原生攻击声音只在放电时播放，未拿攻击音效当脚步声。

皇室法术 dev.4 将普通小电、觉醒小电、大闪、电精灵接入相同的暂停规则；单独安装它也能使用这套机制。两模组同装只运行一个暂停驱动，不会重复减速或重复累加暂停时长。

## 说明与来源

- [MineClash 原项目 / 素材出处](https://www.curseforge.com/minecraft/mc-mods/mineclash)，原作者 LiziYowo、YangXuKun、4y4u、AX_ZHANG、LieNiaoBiBai。原始资源的权利归其作者及相应权利人所有。
- [技术实现](docs/implementation.md) · [动画故障调查与证据](docs/animation-investigation.md) · [验证记录](docs/validation/README.md)
- [构建说明](docs/building.md)
- 小电车 0.8 秒首次攻击参考 [Supercell 2025 年 8 月平衡](https://supercell.com/en/games/clashroyale/blog/release-notes/august-balance-changes-2/)。2.3 秒周期采用 [RoyaleAPI 发布的 2026 年 9 月最终平衡公告](https://royaleapi.com/blog/season-87-balance-final-september-2026)；此前 2.2 秒见 [Supercell 2026 年 8 月公告](https://supercell.com/en/games/clashroyale/blog/news/final-august-balance-changes-826/)。这是对公布参数的实现，不宣称自动跟随原作后续平衡。

自编代码与文档采用 [MIT](LICENSE)，第三方素材明确排除，详见 [ASSETS.md](ASSETS.md)。0.1.1 补全公开许可、可公开构建的铁魔法编译依赖及安装说明，电车数值沿用 0.1.0。
