# 02 施法流程与数值

[返回目录](README.md)

代码入口：[Spell](../../src/main/java/dev/royalespells/Spell.java)、[SpellEngine](../../src/main/java/dev/royalespells/SpellEngine.java)、[SpellEntity](../../src/main/java/dev/royalespells/entity/SpellEntity.java)、[IronSpellProfile](../../src/main/java/dev/royalespells/iron/IronSpellProfile.java)、[RoyaleIronSpell](../../src/main/java/dev/royalespells/iron/RoyaleIronSpell.java)。完整默认参数见 [生成参考](spell-reference.md)。

## 1. 三种时间不能混用

| 参数 | 单位 | 含义 |
| --- | --- | --- |
| `IronSpellProfile.castTicks` | tick | 铁魔法吟唱时间，0 为瞬发 |
| `Spell.duration` | tick | 效果实体的基础生命周期；投射物常用于飞行时间 |
| `IronSpellProfile.cooldown` | 秒 | 提供给原生默认配置的冷却 |
| `ControlCooldown.TICKS` | tick | 冰冻、藤蔓硬性共享池，300 tick |
| `EvolvedArmySpell.getSpellCooldown()` | tick | 军团基础冷却，300 tick |

例如火箭需要先完成原生吟唱，然后效果沿轨迹飞行，最后命中；其吟唱时间不是落点特效播放时间。铁魔法冰冻的效果持续 100 tick，但施法冷却是 300 tick；卡牌版按最新原作持续 70 tick。

## 2. 独立卡牌路径

`SpellItem` / `TroopItem` 接受物品使用后调用 `SpellEngine.cast/deploy`；同装铁魔法时先受 `IronSpellSystem.allowCard` 限制。

- 每位玩家运行时圣水最多 10，每 tick 回复 0.025，即正常速度每秒 0.5。
- 成功施放后，独立全局施法间隔为 10 tick。
- 费用来自 `Spell.cost` / `TroopCard.cost`；创造不扣圣水。
- 独立镜像复制上一张成功卡，费用加 1，效果读取 12 级卡牌参数（不再统一乘 1.1）；镜像结果不会更新上次卡记录。
- 英雄滚桶在独立模式下通过潜行激活再滚动：查找 40 格内自己的英雄，再次消耗 1 圣水，每名英雄只可使用一次再滚动，长度 3 格。
- 独立玩家状态保存在内存中，离线清理或服务器停止后不作为永久进度保留。

`cast` 对当前维度效果实体做 256 数量限制；普通召唤工具同时检查每个主人 64 个和当前维度 256 个 `Summoned`。这些不是所有路径、所有维度、所有第三方实体的统一全局上限。

## 3. 原生铁魔法路径

`RoyaleIronSpell` 继承 `AbstractSpell`，提供默认配置、费用、吟唱类型和生命周期回调。物品消耗、普通魔力结算、普通冷却、施法取消和书本状态主要由上游处理。

`checkPreCastConditions` 检查服务端、启用状态、眩晕/冰冻、共享控制冷却、落点区块和世界边界、当前效果数量；小屋额外检查所有权数量、交互权限和实际体积碰撞。

`onCast` 计算强度和落点，生成效果实体，或走小屋/英雄再施放分支。普通效果保存铁魔法 ID、等级和持续倍率，使后续每一跳伤害仍能找到正确学派来源。吟唱完成不代表任何复杂召唤一定全部成功：添加实体可能被其他事件取消，相关回滚和收费边界需要用实际入口测试。

## 4. 强度与持续时间

模组设置 `baseSpellPower=100`，`spellPowerPerLevel=profile.powerPerLevel()`。基础等级曲线为 `100 + growth × (level - 1)`，再交给铁魔法的 `getSpellPower(level,caster)` 应用它所支持的属性和配置。模组最终使用：

```text
P = clamp(getSpellPower(level, caster) / 100, 0.05, 64)
```

法术每级成长并不统一：火球 30、治疗 35、小电和普通雪球 14、常见召唤 18、镜像和温暖 0；完整映射见参数参考。原生配置或装备生效后的精确值应读取当前 `getSpellPower`，不要自行把各个属性粗略相乘。

持续时间计算额外定义：

```text
E = clamp(P / (1 + growth × (level - 1) / 100) - 1, 0, 2)
冰冻比例 = 1
藤蔓比例 = clamp((1.5 + 0.12 × (level - 1) + 0.2 × E) / 2.5, 0.5, 1)
狂暴比例 = clamp((6 + 0.5 × (level - 1) + E) / 6, 1, 1.75)
温暖比例 = clamp((5 + 0.5 × (level - 1) + E) / 5, 1, 1.75)
实际效果 tick = round(Spell.duration × 比例)
```

因此冰冻所有等级都是约 5 秒；藤蔓在铁魔法一级、无额外属性时为 30 tick，最高受限于 50 tick。不要把 `Spell.VINES.duration=50` 误读成任何等级都固定 2.5 秒。效果结束前刷新在生物上的 3 tick 状态，实际状态清除还有很短的 tick 尾部。

## 5. 瞄准与命中体积

`SpellEngine.aim` 从眼睛沿插值视线取 32 格终点。先检测方块，再在不超过方块命中距离的线段上寻找存活生物；生物命中使用其脚底位置，方块命中加 0.07 高度，没有命中时调用 `ground`。

`ground` 在指定 X/Z 从原点上方 2 格到下方 12 格，逐个读取碰撞形状并做垂直射线检测；找到表面后加 0.05。它不是无限高度地形图查询，也不保证悬空点一定能落地。

`targets` 查询中心上下 `-3 / +5` 的 AABB，再用 `dx² + dz² <= radius²` 筛选。因此大部分范围法术是有限高度的圆柱筛选，而非三维球体；不额外逐目标检查墙壁遮挡。滚动法术再过滤高于滚动物体 1.8 格的目标。范围圈代表水平覆盖，不是全部碰撞和权限条件。

铁魔法潜行施放狂暴、治疗、温暖、克隆时，目标改为自己位置；非玩家施法者若有攻击目标，使用目标生物位置。这些是 `RoyaleIronSpell.target` 的规则，不能推断所有独立卡牌路径都完全相同。

## 6. 效果实体时序

服务端每 tick 先把 `TIME` 加 1，再计算位置并执行 `roll/projectile/field`，最后检查是否到期。下面的 tick 都从效果生成后的第一个服务端 tick 开始，不含铁魔法吟唱。

| 法术 | 核心时序与基础值（乘 P 前） |
| --- | --- |
| 万箭齐发 | 独立 4/12/20；铁魔法 4/14/24。每波 3，半径 4.5 |
| 火球 | 飞行结束 25 tick 命中，12，半径 2.8，有显式推力 |
| 普通小电 | tick 1：4，半径 2.2，眩晕 10 tick |
| 觉醒小电 | tick 1、21 各 4；第一次半径 2.2，第二次 3，第二次紫色 |
| 雷电 | tick 1 按当前生命值锁定最多三个目标；2/8/14 分别打击，单次 22，各自响一次 |
| 火箭 | 40 tick 飞行结束，30，半径 2.5，有显式推力 |
| 派对火箭 | 同样 40 tick，30，半径 3；先记录诅咒，再伤害与播放庆祝声音 |
| 毒药 | tick 1、21…141，每次 2，共 8 跳；每跳刷新短减速 |
| 冰冻 | tick 1 造成 2；持续区域内逐 tick 刷新 3 tick 眩晕和冻结标志 |
| 狂暴 | tick 1 对敌人 3；每 5 tick 对友方刷新狂暴、速度、急迫 |
| 滚木 | 30 tick 移动；单实体只命中一次，8，有前向推力 |
| 普通/英雄滚桶 | 22 tick 移动；单实体只命中一次，6；结束召唤野蛮人或英雄 |
| 飓风 | 持续 30 tick 拉向中心上方；tick 1/16 各 2 |
| 地震 | 60 tick；1/21/41 对地面目标各 3，同时分批累计方块裂纹 |
| 普通雪球 | tick 24：4，半径 2.5，推力与 60 tick 减速 |
| 觉醒雪球 | tick 24 捕获并伤害；之后移动被捕获目标，到 42 tick 释放并减速 |
| 普通/觉醒飞桶 | tick 30 尝试生成三只小僵尸；觉醒额外生成一个偏移 5 格的诱饵桶 |
| 皇家速递 | 从目标上方落下，tick 60 造成 10 并召唤皇家卫队；没有额外推力 |
| 墓园 | 从 tick 20 起、满足 `t % 12 == 8` 时召唤；到 tick 200 共尝试 16 次 |
| 克隆 | tick 1 复制符合条件的友方召唤物；生命周期 16 tick 是视觉载体时间 |
| 小僵尸诅咒 | 持续刷新 24 tick 死亡转化窗口；每 20 tick 造成 1，共 6 跳 |
| 虚空 | tick 16/40/64；本轮目标数为 1 / 2–4 / ≥5 时每目标 20 / 9 / 4 |
| 藤蔓 | tick 1 锁定最多三个当前生命最高的目标，持续控制；每 20 tick 造成 2 |
| 治疗 | tick 1/21/41，友方每次恢复 `2 × P` |
| 温暖 | 逐 tick 清除本模组眩晕、冻结、缠绕与原版减速，清除冻伤积累并短暂防火 |
| 镜像 | 解析上次法术；不应直接运行一个空的 MIRROR 效果实体 |
| 野蛮人小屋 | 直接生成建筑召唤物，详见召唤章节 |
| 觉醒骷髅军团 | 专用原生法术路径，生成 15+1，详见军团章节 |

箭雨可见箭由 `ArrowPattern` 分布；它们不是 48 个独立伤害箭实体。逻辑伤害是一波一次范围查询，不能把箭的可见数量乘到伤害上。

## 7. 伤害、受伤间隔与击退

普通效果先在 `SpellEntity.damage` 乘强度 P，再调用 `IronSpellSystem.damage`。

- 有铁魔法来源 ID：走 `IronIntegration.damage`、原生法术伤害源和 `DamageSources.applyDamage`。虚空在这里额外乘 0.6，因此单目标三次基础预算为 `20 × 3 × 0.6 = 36`。
- 无铁魔法来源 ID：走 `SpellEngine.hit`，使用间接魔法伤害，并清除原版受伤间隔。这与铁魔法路径的普通伤害不同。
- 墓园 `AllySkeleton` 的低伤近战单独临时清除目标 `invulnerableTime`，调用结束后恢复不低于原先的值。Beta 2 在重写近战方法的 `ArmySkeleton` 小兵分支补上同样处理；将军仍保留原生间隔。
- 无附带击退的伤害使用 `CombatImpact` 抑制原版受伤冲量，显式 `SpellMotion.impulse/pull` 仍有效。当前代码中火球、火箭、派对火箭、滚木、雪球、飓风等有显式位移；这是当前实现清单，不是对原作所有击退规则的额外考证。
- 玩家目标还受服务端 PVP 设置限制。攻击预算仍会受到护甲、学派抗性、取消事件和受伤间隔影响。

## 8. 调整数值时必须联动的文件

| 改动 | 至少核对 |
| --- | --- |
| 范围 | `Spell.radius`、`SpellEntity` 硬编码查询、预览、模型/粒子覆盖、说明与测试 |
| 吟唱/魔力/普通冷却 | `IronSpellProfile`、世界原生配置、镜像与连施入口 |
| 控制时长 | `Spell.duration`、`durationScale`、状态刷新、冻结动画、共享 CD |
| 飞行时间 | `Spell.duration`、`visualPosition`、命中 tick、箭雨/雪球特殊时序、声音 |
| 召唤伤害 | `SpellEngine.summon/empower`、武器属性、原生召唤伤害装备、专用军团方法 |
| 等级曲线 | `powerPerLevel`、`getUniqueInfo`、素材生成工具里的历史表、参数参考 |

不要只修改物品提示。提示文本和理论预算不是服务端判定；两边必须以代码和针对性测试核对。

卡牌新参数、来源传递和例外见 [第 15 章](15-card-balance.md) 与 [卡牌换算表](../../CARD-BALANCE.md)。
