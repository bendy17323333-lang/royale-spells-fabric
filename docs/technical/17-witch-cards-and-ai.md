# 17 · 女巫卡牌边界与召唤物 AI（1.9.3）

本章解释这轮重构解决的实际问题、规则与测试边界。两种女巫只提供独立卡牌和刷怪蛋；不加入铁魔法法术注册表、法术卷轴、抄写候选或法术书。

## 角色与可选依赖

| 路径 | Fabric 1.20.1 | NeoForge 1.21.1 |
| --- | --- | --- |
| 普通/觉醒卡牌 | `witch` / `witch_evolution` | 相同物品 ID；安装铁魔法仍能在生存模式直接使用 |
| 普通卡牌实体 | `royalespells:witch` | `royalespells:witch_troop`，避免与旧塔居民 ID 冲突 |
| 觉醒卡牌实体 | `royalespells:witch_evolution` | 同左 |
| 两枚刷怪蛋 | `witch_spawn_egg` / `witch_evolution_spawn_egg` | 同左，始终注册 |
| 塔内女巫 | 不存在 | `royalespells:witch`，仍是铁魔法 NeutralWizard；需安装铁魔法 |
| 自然女巫塔 | 不存在 | `royalespells:witch_tower` 独立变体，保留原劫掠者法师塔 |

NeoForge 原来直接让 RoyalWitch 继承 NeutralWizard，使实体类本身必须加载铁魔法。现在 `RoyalWitch` 是普通 PathfinderMob；塔内 `TowerWitch` 独立继承 NeutralWizard。两者组合复用 **一份 WitchBehavior**，通过 WitchActor 向渲染器和弹体暴露状态，不复制攻击、召唤、首波归魂账本或存档代码。客户端 WitchRenderer/体积收集器支持两类宿主。Fabric 延用自己的 RoyalWitch，不为跨版本共享而引入额外框架。

卡牌、弹体、音效和刷怪蛋在公共入口注册。NeoForge `WitchContent.install` 只注册可选世界居民、巡逻骷髅、塔资源包和塔测试。没有铁魔法时，相关世界生成数据包不会启用，避免遗留 Iron 结构池引用。**不存在女巫 IronSpellProfile 条目**。

卡牌和刷怪蛋的母体都可以战斗/召唤。玩家卡牌继承玩家 UUID；刷怪蛋母体使用自己的 UUID 作为小队阵营，使其骷髅不会攻击母体。它不是全局统一的“所有女巫都友好”阵营。两张卡各消耗 5 圣水，卡牌重复使用；本轮没有修改既有伤害、攻击节拍、生命和归魂规则。卡牌生命周期 1800 tick；自然塔居民不按卡牌时间消失。

## 获取与指令

两版的无形状工作台配方相同，NeoForge 安装铁魔法也不会隐藏这两条配方：

| 产物 | 材料 |
| --- | --- |
| 女巫卡 | 纸 + 骨头 + 紫水晶碎片 + 发酵蛛眼 |
| 觉醒女巫卡 | 女巫卡 + 2 个紫水晶碎片 + 不死图腾 |

```mcfunction
give @s royalespells:witch
give @s royalespells:witch_evolution
give @s royalespells:witch_spawn_egg
give @s royalespells:witch_evolution_spawn_egg
```

NeoForge 同装铁魔法时可使用 `place structure royalespells:witch_tower` 生成新增塔变体。普通卡与觉醒卡可独立工作，不需要这座塔、法术书、魔力或卷轴。

## 为什么会来回切换和停在远处

旧召唤指令逻辑在单位 tick 中直接给 Navigation 下达跟随命令；正式近战 Goal 在同一时间也在控制 Navigation。即使锁定敌人，跟随路径也可能覆盖攻击路径。塔内护卫又可能把“母体当前没有目标”直接传播成自己的空目标，丢掉已经发现的敌人；巡逻限制还会让默认近战 canContinueToUse 在离开小范围 home 后退出。

现在只保留两种明确职责：

- `SummonOrders` **只选目标**，不写 Navigation；所有普通召唤物使用同一个优先级策略。
- `FollowSummonerGoal` 是带 MOVE 标志的正式 Goal；只有无战斗目标时才可运行，优先级 5，低于近战的 2。开始/结束距离不同，避免边缘抖动。

`RageMeleeGoal` 的持续条件允许追逐有效敌人至 32 格，闲逛 home 不再中断已经授权的战斗。不会为此删除寻路碰撞或打开穿墙。觉醒军团仍使用既有 ArmyCombatGoal 的前排/侧翼策略，本轮未重写它。

## 统一目标优先级

每 5 tick 决策一次；仅在目标确实变化时调用 setTarget：

1. 玩家主动攻击命令，最多 200 tick，32 格内。
2. 护卫母体正在攻击的有效目标，32 格内。
3. 最近伤害自身的敌人，24 格内。
4. 保持当前合法目标，24 格内；短暂丢失视线继续寻路，不立刻跟随。
5. 16 格内最近的可见、允许主动攻击的敌对目标。

阵营规则仍优先于距离。不会把玩家自己、同主人部队或队友当敌人；无敌虚化军团骷髅不是可攻击目标；不主动打无仇恨的中立/和平动物或另一玩家拥有的部队。玩家明确攻击可以指定和平生物；受到攻击时可以还击。失去有效主人且没有存活护卫母体时，不自动变成敌对游荡召唤物；中立军团原有独立阵营例外保留。

女巫每次召唤的全部骷髅都写入 `RoyaleWitchEscort`。它用于护卫和队形，**不同于** `RoyaleWitchMother`：后者只给觉醒首波四只非克隆骷髅授予归魂治疗资格。不能用“可以跟随母体”来判断“可以给母体治疗”。

空闲护卫尝试站在母体朝向前方 2.5 格，按实体槽位左右错开；每 10 tick 重寻路，不再默认拖在玩家身后。进入战斗后由近战接管并主动压近。这里是队形与寻路规则，**不保证每次伤害都能替母体拦截，也不让骷髅瞬移或免碰撞**。复杂拥挤地形仍应实战验证。

塔的自然巡逻单位保留 12 格闲置区域和持久化 home；战斗期间不强行拉回。母体无目标时护卫仍能保留自身战斗或寻找附近敌对生物，而不是反复清空目标。

## 验证入口

`SummonAiTests` 运行真实寻路、碰撞与攻击：主人在后方时小骷髅越过 idle home 接敌并造成伤害，小僵尸保持攻击，不会被跟随覆盖；母体清空目标后护卫保持自己的战斗；玩家命令优先；空闲护卫向前排移动且不伤害旁边的牛。

`WitchTests`（Fabric）/ `WitchCardTests`（NeoForge）检查弹体与范围判定、归魂一次性账本、克隆、存档、冰冻/电击/雪球时钟、镜像数值，以及两张卡的实际 survival item-use、配方、刷怪蛋物品交互和子兵阵营。NeoForge `WitchTowerTests` 另验证真实 NeutralWizard、无女巫法术注册、原塔与新塔共存、实际结构放置、刷怪笼、居民持久化和数量上限。

构建和完整结果见 [本版验证记录](../../VALIDATION-1.9.3.md)。测试用隔离世界，不改变用户存档；测试通过不代表任意整合包、所有拥挤地形及网络延迟均无问题。
