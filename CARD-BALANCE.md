# 卡牌数值换算 · 2026-09-08

适用版本：**1.7.1-beta.3，Minecraft 1.21.1 NeoForge**。只重做卡牌形态的皇室法术和召唤部队；不调整普通铁魔法法术、四元素法杖或 MineClash 数值。觉醒骷髅军团是用户明确指定的例外。

统一采用皇室战争 **11 级**；原作小骷髅 **81 HP** 对应模组 **3 点生命值（1.5 颗心）**。因此伤害、生命及治疗的基本换算是 `原作值 / 27`。不把中途取整的数字继续参与计算。镜像卡读取 12 级表；本表只为显示保留三位小数。

雷电示例：`1057 / 81 × 3 = 39.148148…` 点基础伤害，每个被选中的目标各一次，最多三个不同目标；并非三次都打同一单位。护甲、抗性和其它伤害事件可能改变实际扣血。

## 数据截止与优先级

已核对官方 9 月 8 日正式平衡（冰冻 3.5 秒）、8 月 4 日及 26 日平衡、6 月墓园数量、5 月觉醒雪球及英雄滚桶调整。正式公告覆盖缓存页面的旧值；未调整字段读取现行卡牌等级表。没有把 2023 年的旧静态统计 JSON 当作最新数据。

官方来源：[9 月](https://supercell.com/en/games/clashroyale/blog/release-notes/september-balance-changes-2026/)、[8 月及紧急平衡](https://supercell.com/en/games/clashroyale/blog/news/final-august-balance-changes-826/)、[6 月](https://supercell.com/en/games/clashroyale/blog/release-notes/june-balance-changes-2026/)、[5 月](https://supercell.com/en/games/clashroyale/blog/release-notes/may-balance-changes-2026/)。逐卡来源见下表。

## 法术：每次、总量和范围

“总量”是假设目标始终在范围内、每次都命中的理论总量；召唤卡不把随后部队造成的伤害计入法术总量。治疗用正向恢复量。滚木／滚桶的范围列写完整宽度及前进距离，其它写半径。生命周期包括部分投射物飞行，不应全都理解为控制时长。

| 卡牌 | 原作每次 L11 | 模组每次 | 结算次数 | 模组总量 | 范围 / 方块 |
| --- | ---: | ---: | ---: | ---: | --- |
| [万箭齐发](https://www.deckshop.pro/card/detail/arrows) | 122 | 4.519 | 3 | 13.556 | 半径 3.5 |
| [火球](https://www.deckshop.pro/card/detail/fireball) | 688 | 25.481 | 1 | 25.481 | 半径 2.5 |
| [电击法术 · Zap](https://www.deckshop.pro/card/detail/zap) | 192 | 7.111 | 1 | 7.111 | 半径 2.5 |
| [觉醒电击 · Zap](https://www.deckshop.pro/card/detail/zap) | 192 | 7.111 | 2 | 14.222 | 半径 2.5 → 3 |
| [雷电法术](https://www.deckshop.pro/card/detail/lightning) | 1057 | 39.148 | 1 | 39.148 / 目标 | 半径 3.5 |
| [火箭](https://www.deckshop.pro/card/detail/rocket) | 1484 | 54.963 | 1 | 54.963 | 半径 2 |
| [毒药法术](https://www.deckshop.pro/card/detail/poison) | 92 | 3.407 | 8 | 27.259 | 半径 3.5 |
| [冰冻法术](https://www.deckshop.pro/card/detail/freeze) | 148 | 5.481 | 1 | 5.481 | 半径 3 |
| [狂暴法术](https://www.deckshop.pro/card/detail/rage) | 179 | 6.630 | 1 | 6.630 | 半径 3 |
| [滚木](https://www.deckshop.pro/card/detail/the-log) | 268 | 9.926 | 1 | 9.926 | 宽 3.9 × 长 10.1 |
| [飓风法术](https://www.deckshop.pro/card/detail/tornado) | 84 | 3.111 | 2 | 6.222 | 半径 5.5 |
| [地震法术](https://www.deckshop.pro/card/detail/earthquake) | 81 | 3.000 | 3 | 9.000 | 半径 3.5 |
| [巨大雪球](https://www.deckshop.pro/card/detail/giant-snowball) | 179 | 6.630 | 1 | 6.630 | 半径 2.5 |
| [觉醒巨大雪球](https://www.deckshop.pro/card/detail/giant-snowball) | 179 | 6.630 | 1 | 6.630 | 半径 2.5 |
| [小僵尸飞桶](https://www.deckshop.pro/card/detail/goblin-barrel) | — | — | — | 召唤／复制 | 半径 1.5 |
| [觉醒小僵尸飞桶](https://www.deckshop.pro/card/detail/goblin-barrel) | — | — | — | 召唤／复制 | 半径 1.5 |
| [野蛮人滚桶](https://www.deckshop.pro/card/detail/barbarian-barrel) | 232 | 8.593 | 1 | 8.593 | 宽 2.6 × 长 4.5；英雄再滚 3 |
| [英雄野蛮人滚桶](https://www.deckshop.pro/card/detail/barbarian-barrel) | 232 | 8.593 | 1 | 8.593 | 宽 2.6 × 长 4.5；英雄再滚 3 |
| [皇家速递](https://www.deckshop.pro/card/detail/royal-delivery) | 384 | 14.222 | 1 | 14.222 | 半径 3 |
| [墓园](https://www.deckshop.pro/card/detail/graveyard) | — | — | — | 召唤／复制 | 半径 4 |
| [克隆法术](https://www.deckshop.pro/card/detail/clone) | — | — | — | 召唤／复制 | 半径 3 |
| [镜像法术](https://www.deckshop.pro/card/detail/mirror) | — | — | — | 召唤／复制 | 半径 3 |
| [小僵尸诅咒](https://www.deckshop.pro/card/detail/goblin-curse) | 35 | 1.296 | 6 | 7.778 | 半径 3 |
| [虚空法术](https://www.deckshop.pro/card/detail/void) | 696 / 294 / 153 | 25.778 / 10.889 / 5.667 | 3 | 77.333 / 32.667 / 17.000 | 半径 2.5 |
| [藤蔓法术](https://www.deckshop.pro/card/detail/vines) | 153 | 5.667 | 2 | 11.333 | 半径 2.5 |
| [治疗法术（历史）](https://royaleapi.com/blog/season10?lang=kr) | 78 | 2.889 | 2 | 5.778 | 半径 4 |
| [温暖法术（活动）](https://clashroyale.fandom.com/wiki/Warmth) | 25 | 0.926 | 5 | 4.630 | 半径 4 |
| [派对火箭（活动）](https://clashroyale.fandom.com/wiki/Party_Rocket) | 非固定伤害 | 转化 | — | 见例外说明 | 半径 4 |

虚空三档按每次结算时的敌人数：1 个、2–4 个、5 个以上。地震对本模组建筑使用每次 `283/27 = 10.481`，三次 `849/27 = 31.444`；方块破坏仍采用已有分阶段裂纹规则，不把方块当作有生命的生物。墓园生命期 9 秒、生成 12 只；藤蔓控制 2 秒；狂暴场 4.5 秒；毒药 8 秒。卡牌冰冻 3.5 秒，铁魔法冰冻仍为 5 秒。

小电和大闪的电击暂停仍为 0.5 秒。两种雪球都减速 3 秒，卡牌移速降低 35%；觉醒雪球滚动 4 格。地震减速 50%；卡牌狂暴移速和攻击速率增幅为 35%。移速效果继续经过 Minecraft 属性合并，未重新实现所有跨模组减速优先级。小僵尸诅咒为 15% 减速，不恢复已经移除的伤害放大机制。

## 部队

| 部队 | 原作生命 / L11 | MC 生命 | 原作攻击 | MC 每击 | 攻击间隔 |
| --- | ---: | ---: | ---: | ---: | --- |
| [墓园小骷髅](https://www.deckshop.pro/card/detail/skeletons) | 81 | 3.000 | 81 | 3.000 | 1.1 秒 |
| [小僵尸（哥布林替代）](https://www.deckshop.pro/card/detail/goblins) | 202 | 7.481 | 125 | 4.630 | 1.1 秒 |
| [觉醒飞桶诱饵小僵尸](https://www.deckshop.pro/card/detail/goblin-barrel) | 81 | 3.000 | 66 | 2.444 | 1.1 秒 |
| [野蛮人／英雄滚桶的野蛮人](https://www.deckshop.pro/card/detail/barbarians) | 716 | 26.519 | 192 | 7.111 | 1.4 秒 |
| [皇家卫队](https://www.deckshop.pro/card/detail/royal-recruits) | 547 | 20.259 | 133 | 4.926 | 1.3 秒 |
| [野蛮人小屋](https://www.deckshop.pro/card/detail/barbarian-hut) | 1164 | 43.111 | — | 0.000 | 产兵 |
| [地狱飞龙](https://www.deckshop.pro/card/detail/inferno-dragon) | 1295 | 47.963 | 35 | 1.296 | 0.4 秒 |

皇家卫队的盾：`240/27 = 8.889 HP`，破盾伤害不溢出。小屋存活 30 秒，三只一波、每 15 秒产兵，死亡补一只；后续野蛮人继承小屋卡等级。卡牌持剑模型不再叠加原版石剑／铁剑属性，卡牌卫队头盔也不额外提供原版护甲。

地狱飞龙三档每次伤害为 `35/27 = 1.296`、`120/27 = 4.444`、`422/27 = 15.630`，每 0.4 秒一击；水平射程 3.5 格。保留适合三维战斗的 6 格垂直攻击容差与至少 3.5 格离地高度。这里是卡牌飞龙的数值；铁魔法飞龙保留本轮重做前的等级、装备与三阶段加成。

觉醒骷髅军团统一为：小骷髅 3 HP、将军 3 HP、将军盾 3 HP，基础攻击 3。卷轴、法术书、号角与中立刷怪蛋入口一致；将军不因卷轴等级变成高生命坦克。旧存档生命／盾会被封顶，不恢复已损失生命。十五只小骷髅同 tick 命中基础总伤害为 45，幽灵状态同样逐个结算。

## 明确的例外和证据边界

- 治疗卡已经移除，使用最后公开的 11 级总治疗 156、2 秒、半径 4；它没有“当前天梯版本”。温暖和派对火箭是活动卡，采用已公开活动数据，不能与现行天梯平衡混称。
- 温暖参考 11 级每秒治疗 25、半径 4；保留模组 5 秒生命周期与解冻功能。派对火箭参考半径 4 和转化机制；没有可靠的固定原作伤害值，不捏造一个数值。模组通过普通伤害路径尝试消灭目标并转化，抗性、盾和免疫可阻止；不会绕过第三方事件强制删除实体。
- 克隆卡等级决定复制体的基础攻击等级。Minecraft `MAX_HEALTH` 下限为 1，所以克隆保留 1 HP，并非数学上的 1/27。所有造成伤害的卡牌仍足以一击击杀无额外防御的克隆体。
- 墓园当前原作采用固定刷出布局。模组用平均半径 3.3 的八个外围锚点、共 12 次刷出适配方块地面；并不声称其坐标、顺序和 20 TPS 时序是原作竞技场数据的逐项导出。
- 12 级镜像多数字段来自逐级表。虚空 2–4 目标档 323、5+ 档 168，以及诱饵小僵尸伤害 72，是依据当前 11 级和等级比例推算的值；不是已独立核对的三个原作条目。基础 11 级对应值已按最新正式公告核对。
- 本模组没有皇室公主塔／国王塔实体分类。普通生物不使用皇冠塔伤害减免；9 月火球调整的是对皇冠塔伤害，不把单位伤害错误降为 159。
- 不调整 MineClash 原单位生命、攻击或小电车的 1/26 比例。电车附属依然是独立系统，使用自己的配置。精灵只通过铁魔法／法杖召唤，未纳入卡牌换算。

完整实现见 [技术章 15](docs/technical/15-card-balance.md)，机器可读参数见 [JSON 快照](docs/reference/card-balance-2026-09-08.json)，数据来源明细在 JSON 的 sources 中。该快照不会自动跟随未来官方平衡；下一次更新需要重新核查公告和逐级表。
