# 08 同步、存档、配置与 Mixin

[返回目录](README.md)

## 1. 数据的三种寿命

“保存到字段”“通过网络同步”“写入存档”是三种不同能力。本项目中常见状态分为实体同步数据、持久化 NBT/SavedData、运行时缓存。增加新状态前应明确需要哪一种，不能靠每次登录重新猜测。

## 2. 实体同步

| 实体/对象 | 同步字段 | 用途 |
| --- | --- | --- |
| `SpellEntity` | `DATA: CompoundTag`、`TIME: int` | 法术类别、起终点、强度、打击点与时序 |
| `AllyZombie / AllySkeleton` | `CLONED: boolean` | 自己的召唤物克隆外观 |
| `ArmySkeleton` | GENERAL、GHOST、SHIELD、AGE、DISSOLVE、STRIKE | 将军装备、紫色幽灵、升起/消散与攻击 |
| `RoyaleUnit` | FACING | 建筑锁定朝向 |
| `RitualEntity` | AGE、MODE、DISPLAY | 转化阶段与展示物品 |
| `EvolutionBurst` | AGE、RADIUS | 短暂觉醒部署闪光 |
| 所有 `LivingEntity` | 本模组注入的 BYTE 标志 | 狂暴 1、冻结 2、缠绕 4、克隆 8 |

实际登记的 accessor 名和序列化类型以对应类为准。状态字节从服务端状态效果每 tick 更新，客户端 `VisualState` 读取它，以便不同观察者看见同一生物的外观。它不是只发给施法者的 HUD 消息。

`SpellEntity.DATA` 当前包含：

| 键 | 内容 |
| --- | --- |
| `spell` | `Spell.ordinal()`，整数，不是字符串 ID |
| `startX/Y/Z`、`targetX/Y/Z` | 双精度世界坐标 |
| `Power` | 强度，缺省 1，范围约束 0.05…64 |
| `IronSpell`、`IronLevel` | 原生来源 ID 和等级 |
| `DurationScale` | 生命周期倍率，限制 0.5…1.75 |
| `ZapPoints` | 受击点列表，每点为 pX/pY/pZ |
| `VoidPoints`、`VoidStrikeTick`、`VoidStrength` | 虚空最近一轮受击点、时刻和分档 |

更改 DATA 时先 copy 再 set，使原生同步层识别更新。客户端不计算伤害列表，Zap/虚空目标点来自服务器。当前同步整个 CompoundTag，在目标很多或高频更新时有带宽成本；不能把任意大列表无上限塞进去。

## 3. 自定义网络消息

唯一显式自定义 payload：[MirrorHistoryPayload](../../src/main/java/dev/royalespells/iron/MirrorHistoryPayload.java)。

- ID：`royalespells:mirror_history`。
- 注册协议版本字符串：`1`。
- 方向：服务端到客户端。
- 内容：最长 256 字符的 UTF 法术 ID，随后 VarInt 等级。
- 成功施法和登录时发给对应玩家；接收后 enqueueWork 更新 `MirrorIronSpell.clientHistory`。

普通物品使用、实体出生、状态同步、冷却和声音继续用 Minecraft/铁魔法现有网络机制。没有自定义“客户端上传任意落点即可造成伤害”的包。

增加新消息时给协议和方向明确约束，避免把客户端预览当成权威。客户端包中的实体 ID/UUID 不等于具备操作权限。

## 4. 持久化索引

| 保存位置 | 键/内容 | 载入与边界 |
| --- | --- | --- |
| 主世界 SavedData `royale_skeleton_armies` | 每主人 Army、General、Expires、Cooldown | 跨维度共享，主世界 gameTime；结束军团保留 CD |
| 主世界 SavedData `royale_control_cooldown` | Player、Until | 登录/重生同步到两种原生法术和旧卡牌 |
| 录制世界 SavedData `royale_recording_map` | Enabled、Ready、Index | 仅标记启用的片场执行切场逻辑 |
| 玩家 PersistentData | `RoyaleIronMirrorHistory` 的 Id/Level | 登录同步；不保存 ACTIVE 半完成镜像会话 |
| 玩家 PersistentData | `RoyaleAttackOrder/Until` | 200 tick 的近期指令，不是永久仇恨 |
| 召唤物 NBT | `SpellOwner/SpellLife/SpellClone` | 寿命通常是已加载 tick 的倒计时 |
| 野蛮人 NBT | Hero、NextReroll、DeploymentPlayed | 再滚动与登场声去重 |
| 小屋 NBT | SpawnClock、DeathSpawned、TroopPower、HutFacing | 生产进度和死亡波去重 |
| 军团成员 NBT | Army、NeutralArmy、General、Ghost、ArmyShield、ArmyAge、ArmyDissolve | 不保存 STRIKE 中途攻击目标 |
| 仪式 NBT | Age、Mode、Source、Settled、First、Second | 保存投入材料；输出时有明确结算标记 |
| 觉醒闪光 NBT | Age、Radius、Expires | 防止加载后从头播放 |
| 原生克隆 PersistentData | RoyaleIronClone、RoyaleCloneExpires | 恢复后仍检查时限/克隆状态 |
| 召唤物 PersistentData | RoyaleIronSpell、RoyaleIronLevel | 原生召唤事件与伤害属性关联 |
| 号角物品组件 | 自定义布尔标志、INSTRUMENT、GLINT override | 随物品保存，不是新附魔注册 |
| 重油方块状态 | natural | 只在天然源方块上保留资格 |

`SpellEntity` 另保存 `SpellData/SpellTime`、Owner、RerollId、Decoy、Reroll、Hits、Captured、QuakeBroken、QuakeWork。已命中 UUID 防止滚木加载后对同一实体重复命中；Captured 保留大闪/藤蔓/觉醒雪球锁定列表。

## 5. 没有持久化的状态

- `SpellEngine.PLAYERS` 独立圣水、上一张旧卡与短间隔；`CURSES` 的短死亡窗口。
- `SpellMotion.PUSHES` 的 NoAI 临时推力。
- `EarthquakeDestruction.CRACKS` 的累积方块损伤。
- `MirrorIronSpell.ACTIVE` 镜像会话。
- 客户端预览轮廓、冻结姿势、图片宽高比、视觉测试计数。

地震 `QuakeWork` 会保存扫描位置、波次和游标，但共享裂纹账本不会随服务器重启保存。不能据此宣称方块累计破坏进度永久保留或原版挖掘可直接接续这份伤害；它只是本模组的临时损伤与裂纹覆盖。

玩家 PersistentData 是存储方式，但不自动证明任何死亡克隆/跨服迁移都保留所有键。当前显式重生同步主要针对控制池；新增需要跨死亡保留的玩家状态应检查 PlayerEvent.Clone 等实际流程。

## 6. 地震方块损伤的限制

[EarthquakeDestruction](../../src/main/java/dev/royalespells/EarthquakeDestruction.java) 在半径 3.5、中心下 3 到上 32 的范围收集候选，最多 2048 个，按距离排序。每波最多每 tick 处理 128 个，三波在 1–20、21–40、41–60 tick 中推进。

- `earthquake_breakable` 每波加 1/3，第三波达到约 1 后破坏。
- `earthquake_stone` 每波加 0.30，三波达到 0.90；再受地震可累计破坏。
- 波次结束后裂纹保留 100 tick，普通完整施法的过期时刻约为效果结束+5 秒。
- 每维度裂纹表最多 8192 个，方块改变/卸载/到期会清理。
- 破坏前检查边界、区块、硬度、交互权限及可取消 `BlockEvent.BreakEvent`；玩家不存在时没有对应玩家 BreakEvent。

方块集合受标签、数量和扫描高度限制，所以“所有木头”准确指本次收集到的可破坏木质标签候选，不是整栋无限高度建筑或整个区块。方块标签见 [earthquake_breakable](../../src/main/resources/data/royalespells/tags/block/earthquake_breakable.json)、[earthquake_stone](../../src/main/resources/data/royalespells/tags/block/earthquake_stone.json)。

## 7. Mixin 完整清单

配置：[royalespells.mixins.json](../../src/main/resources/royalespells.mixins.json)，声明 `required=true`、`defaultRequire=1`、Java 21。以下不含辅助插件本身；对应源码可从 [完整索引](source-index.md) 进入。

| Mixin | 目标/钩子 | 职责与维护风险 |
| --- | --- | --- |
| LivingEntityMixin | LivingEntity hurt、knockback、travel、tick、defineSynchedData | 小骷髅无击退、控制、外观位同步，影响面广 |
| SkeletonArrowImpactMixin | AbstractArrow.doKnockback | 取消原生召唤骷髅箭的额外击退 |
| MobEntityMixin | Mob.serverAiStep | 眩晕时停止 AI 步骤 |
| IronTargetMixin | ServerPlayerEvents.onLivingChangeTarget | 为本模组攻击敌方魔法召唤物提供窄例外，保留隐身/同盟约束 |
| IronDispelMixin | 本模组效果与召唤物类 | 实现 AntiMagicSusceptible，驱散时 discard |
| IronSchoolCacheAccess | SpellRegistry.SCHOOLS_TO_SPELLS | 配置后清学派候选缓存 |
| IronLootCacheAccess | SpellFilter 两个候选缓存 | 配置后清战利品缓存 |
| PoolTemplateAccess | SinglePoolElement.template | 读取结构模板 ID |
| TowerPoolsMixin | StructureStart.placeInChunk TAIL | 原塔完成后增加池与深层密室 |
| ItemCombinerPlayerAccess | ItemCombinerMenu.player | 奥术铁砧制作权限需要玩家 |
| IronElixirForgeMixin | ScrollForgeMenu 构造/结果生成 | 服务器替代墨水制作判定 |
| IronElixirAnvilMixin | ArcaneAnvilMenu.createResult TAIL | 限制皇室卷轴并处理觉醒 |
| LivingVisualMixin | LivingEntityRenderer render/getRenderType | 原版模型染色、克隆透明、冰冻 Scope 与覆盖层 |
| FrozenModelPartMixin | ModelPart.translateAndRotate | 锁定原版骨骼姿势 |
| IronFrozenBoneMixin | GeckoLib RenderUtil 三个骨骼变换方法 | 锁定 GeckoLib 平移/旋转/缩放 |
| IronGeoVisualMixin | GeoEntityRenderer render/color/type | 自定义生物冻结与染色/透明 |
| IronSpellBarUiMixin | SpellBarOverlay.render/blit | 仅皇室卡图去框与原比例 |
| IronSpellWheelUiMixin | SpellWheelOverlay.render/blit | 轮盘同样处理 |
| SilentLightningMixin | LightningBolt.tick 的 playLocalSound | 尊重 visual lightning 的静音，避免额外雷声 |
| IronElixirForgeScreenMixin | ScrollForgeScreen.generateSpellList | 客户端候选仅皇室法术 |

后八项为客户端项。`RoyaleMixinPlugin` 仅在铁魔法存在时启用特定接口/GeckoLib Mixin；部分上游目标通过 `@Pseudo` 避免缺类。新增 Mixin 不能只列进 JSON 而不处理可选依赖与两种运行模式。

`IronTargetMixin` 的 `ci.cancel()` 是跳过上游某个监听方法的处理，不是取消整个 NeoForge 事件；其他监听器仍有机会处理，不能误写成强行关闭上游全部友军识别。

## 8. 配置与资源 ID 稳定性

- `royalespells-worldgen.toml` 的两个概率是本模组显式注册的 COMMON 配置。
- 大多数法术启用、学派、稀有度、等级和可制作配置由铁魔法提供。本模组没有另外实现一套同名的全局平衡 JSON 热加载器。
- 控制法术 CD 和军团 CD 有覆写/账本逻辑，不是所有 DefaultConfig 字段都能覆盖掉这些硬性规则。
- 配方位于 1.21 的单数路径 `data/royalespells/recipe/`；方块标签位于 `tags/block/`。迁回 1.20.1 时路径和数据格式可能不同。
- 改物品、实体、法术字符串 ID 会影响现有卷轴、号角、NBT、镜像历史、资源和语言键；改枚举顺序另影响 ordinal。

本档案描述的接口是当前版本内部契约。做破坏性迁移应设计 NBT/数据组件兼容，并用旧版生成的存档测试，而不是只在全新世界看是否能启动。
