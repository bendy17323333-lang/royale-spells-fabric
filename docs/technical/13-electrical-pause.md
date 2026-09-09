# 13 电击暂停、共享骨骼修复与电车附属

适用：本地 **1.7.0-dev.4**，NeoForge 1.21.1；仍未公开发布。

## 规则和入口

普通 Zap、觉醒 Zap 的各次电击、Lightning 的逐目标打击，以及 Electro Spirit 的每跳连锁，均调用 `SpellEngine.electricStun`。它添加 `ELECTRICAL_STUN`，并将瞬时速度置零；不使用旧的 `stun`，不清空目标或主动终止铁魔法施法。

普通单位被电击时保留当前攻击抬手和计时；解除后继续剩余动作。已经离体的投射物独立存在，不会倒退。原先完整冰冻、藤蔓等非电击控制继续沿各自规则执行。

**地狱飞龙例外**：原作的渐强光束受到电击后必须重新蓄热。`InfernoDragon.onEffectAdded/onEffectUpdated` 检查共享标签并立即清空 `HEAT`、`LOCK`、`lockedUuid` 和 `hitClock`；保留 AI 的 `getTarget()`。其服务器 tick 在电击持续期间反复保证光束为零；`travel` 也暂停，防止飞龙通过覆盖方法绕过一般 LivingEntity 的位移暂停。解除后第一发仍须完成 8 tick 命中计时，伤害回到第一阶段。

## 两个独立 JAR 如何协作

本模组与 `zappiesaddon` 都提供各自的电击效果，并加入通用标签 `c:electrical_action_pause`。行为依据标签，不硬编码对方的效果类。

`PauseMixinPlugin` 发现电车附属已加载时，跳过皇室法术自身的整组 pause mixin，由附属承担唯一驱动；未装附属时由皇室法术驱动。因此不会对同一角色重复扣减动画时间。双方都能独立构建、单独与各自必需的本体运行。

暂停标志用 `SynchedEntityData` 从服务器同步。电车驱动的实体并不实现本 JAR 的 `PauseState` 接口，客户端 `ElectricPause.active` 通过 `ClassValue` 缓存的 `electricallyPaused` 方法读取另一驱动的状态；不会每帧重新查找类或发网络请求。

## 服务器暂停边界

- `PauseMobMixin` 暂停 `Mob.serverAiStep`，保存 Goal/Brain 内的剩余抬手和目标，不调用 `stop()`。
- `PauseLivingMixin` 暂停原版挥手、物品使用和主动 travel；阻止受暂停者继续发出直接近战伤害，但不拦截它的环境自伤，也不倒退已经飞出的箭。
- 皇室法术的 ArmySkeleton 和 ElementalSpirit 在自己的额外计数逻辑中检查该状态。InfernoDragon 采用上面的特例。
- 铁魔法玩家施法只跳过 `MagicManager.tick` 中对当前玩家的施法推进分支。魔力、冷却等其它系统继续更新，原法术 ID、等级和施法数据保留。暂停期间也拒绝开启新施法。
- 铁魔法生物常规施法经 `serverAiStep` 暂停；客户端的施法条同步暂停。

这不是替换整个实体 tick。中毒、雨水伤害等世界规则继续生效。第三方直接在额外 tick、独立动画库或自定义网络协议里推进的攻击，需要单独适配和验证。

## 客户端局部动画时钟

`PauseClock` 以实体为键保存 `WeakHashMap` 状态。暂停时返回固定的开始时间，结束后扣掉累计暂停时长。这样保持姿势并从原进度恢复，避免解除瞬间跳过半秒动画。

GeckoLib 的 `GeoEntityRenderer` 会预先写入 `DataTickets.TICK`。只临时改变实体 `tickCount` 不会生效。`PauseGeoMixin` 在调用 `GeoModel.handleAnimations` 时同时替换该 ticket、实体 age 和 partial tick，随后在 `finally` 恢复调用环境。每个实体的 AnimatableManager 仍然是自己的；没有修改全局播放速度，也没有让一个实例直接写另一个实例的骨骼。

原版 `LivingEntityRenderer` 使用同一个局部时钟，配合暂停 swing tick 保持动作。`FrozenRender` 区分完整冰冻和短电击：完整冰冻保留整套捕获帧，短电击不重复重写已经由局部时钟处理的实体年龄，防止两层冻结重复计时。

## 旧版死亡/攻击姿势串扰

旧 `IronFrozenBoneMixin` 的未冻结路径仍然调用骨骼 setter。GeckoLib setter 同时设置 changed flags，而旧 finally 只恢复数值，没有恢复这三个标记。模型骨骼在实体间共享；后一个实体的 idle 动画未覆盖某些通道时，带脏标记的前一个死亡姿势就残留了。

dev.3 修复并由 dev.4 继承：

1. 没有冻结姿势替换时，完全走原逻辑，不触碰骨骼。
2. 真冻结时，同时保存位置/旋转/缩放与三个 changed flags。
3. 渲染后 `finally` 恢复变换，清空并按原状恢复 flags。

真实整合包对照显示：只装 MineClash 正常；旧皇室法术会让未死亡的车继承 `group2` 轮组偏移；只替换该修复后正常。小骷髅的攻击残留也是同一路径；精英火枪手主要表现为眼睛和眉骨残留，肉眼较不明显。

## 验证与技术边界

- 皇室法术独立 GameTest：**83 / 83**。
- 铁魔法、MineClash、电车附属同装：**149 / 149**，含真实 Zappy 射击后立即清零已达第三阶段的飞龙、解除后第一阶段重启。
- 电车附属自己的 **12 / 12** 包含三车逐发伤害、真实移动、雨水自伤、抬手暂停和持续周期。
- 打包客户端与用户同组成的整合包：被冻结的车和小骷髅各有 9 个客户端采样保持相同局部时间与全部骨骼；未受击同类继续活动；解除仅前进约 0.68 tick，没有跳跃 10 tick。三辆车持续开火分别观察到 7 / 5 / 6 次，所有连续命中相隔 46 tick。

数值测试、渲染观察和录像不是同一种证据。此轮检查了单机集成服务器、实际游戏 JAR 和已安装模组组合；未宣称覆盖任意第三方特殊攻击、所有玩家动画库或真实远程多人延迟。

雪球动画减速已在公开 Beta 3 纳入，沿用每实体时钟设计，详见 [雪球与投射物视觉](14-snowball-and-projectile-visuals.md)。雪球是连续减速，电击是短暂暂停，两者使用不同状态。
