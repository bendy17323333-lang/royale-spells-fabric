# Fabric 1.20.1 独立版技术说明

## 版本边界和目录

版本 1.8.0 基于公开 NeoForge Beta 3 提交 `761abea66ae67c0fd3c035b7a6fd46c148316e55` 的卡牌／表现部分移植。Java 包为 `dev.royalespells`，资源命名空间 `royalespells`。源代码采用 Mojang 官方 1.20.1 命名，不是旧 Fabric 工程的 Yarn 命名。Fabric Loom 1.16.1 完成官方名到 intermediary 的正式 JAR 重映射；Gradle 9.4.1 使用 JDK 21，所有 Java 源文件编译为 release 17，游戏任务用 JDK 17。

本版没有 NeoForge 事件、流体类型、OBJ 加载器、数据组件、Iron’s Spells API、法术注册表、魔力／学派／抄写接口，也没有 GeckoLib 依赖。MineClash 只作为明确署名并排除于 MIT 的冰精灵素材来源，不调用 MineClash 的代码或实体。小电车附属 JAR 不属于本次移植。

## 注册与生命周期

`RoyaleSpells` 是 Fabric `ModInitializer`。`Registry.register` 注册 28 个 SpellItem、7 个 TroopItem、2 个场景控制物品、中立军团刷怪蛋、11 种实体类型、状态与音效；实体属性通过 `FabricDefaultAttributeRegistry` 注册。渲染器只在 `RoyaleClient` 客户端入口注册，公共初始化不引用 Minecraft 客户端类。

`ServerTickEvents.END_SERVER_TICK` 驱动圣水、诅咒账本、地震裂纹、运动控制。实体自己的 tick 驱动投射物、精灵与部队 AI。服务器停止时清理静态缓存，避免同一客户端换存档后继承旧场景状态。死亡回调只在成功死亡后进行诅咒转化。JOIN／RESPAWN 恢复持久化控制池和军团冷却的物品显示。

## 卡牌事务与经济

`SpellItem.use`、`TroopItem.use` 只在服务端实际施放；客户端返回交互结果。`SpellEngine` 维护每个玩家圣水上限 10，tick 回复 0.025，成功施放后的共用输入间隔 10 tick。创造模式跳过圣水扣除，其它规则仍受检查。圣水和镜像历史是在线会话状态，退出后恢复默认；长冷却和军团资格另行持久化。

验证顺序是状态／输入间隔 → 长冷却 → 圣水 → 目标／世界边界／已加载区块／交互权限 → 实体生成 → 扣费、记忆上张卡、播放动作。`CardDeployment` 将 16 个军团位置先全部预检，再逐个加入世界，全部成功后才写入军团账本。加入失败会撤销已加入成员，不扣圣水或创建冷却。普通召唤上限为每位玩家 64、单世界 256；法术实体另有单世界 256 的上限。

`State.last` 与 `lastTroop` 互斥，只有成功施放普通卡才改变历史。镜像重复读取最后成功卡，不把自己记为上一张。普通卡使用 CR 11 级表，镜像为 12 级，费用加 1。`CardBalance` 负责卡牌召唤物属性，展示用的石剑／铁剑不再次叠加原版攻击加成；皇家卫队的装饰头盔不额外提供护甲。

## 合成和数据资源

35 个无形状工作台配方位于 `data/royalespells/recipes`，均采用 1.20.1 的 `result.item`。精灵配方有史莱姆球作为额外成分，与其它纸张+单一素材法术配方区分。测试实际将配方放入 3×3 合成容器，验证每套输入恰好只匹配一张正确卡。数据标签目录使用 1.20.1 的复数 `tags/blocks`、`tags/damage_type` 等名称，不照搬 1.21 单数目录。

## 原生卡图与准星渲染

卡牌模型使用 `builtin/entity`，`CardRenderer` 通过 Fabric `BuiltinItemRendererRegistry` 绘制单独的四边形，读取卡图独立纹理而非强行加入地形方块图集。302×363 原图在高 1、宽 302/363 的面片上居中显示；透明边框使用 cutout；原卡图自带边框，不新增铁魔法 UI 框。

`TargetPreview` 仅检查当前主手或副手是否为 SpellItem／TroopItem，空手或普通物品不渲染。相机方向用当前帧插值取得，施法方向由服务端部署时固定。`TargetGeometry` 生成圆或长条的世界坐标，只有落点锚点确定地面高度；轮廓顶点不各自吸附台阶、树叶，以免圆被拉成长条。屏幕中心采用 Minecraft 原生准星，不添加会被地形拉伸的十字。

Fabric `WorldRenderEvents.LAST` 提供的 PoseStack 已含相机旋转。渲染时仅减去相机坐标一次，不重复施加视图旋转。圆环分两层：有深度测试的清晰可见部分，以及低透明度、不进行深度测试的完整轮廓。两层都只写颜色，不写深度，从而避免透明范围切掉人物模型。正常透视仍会令远处的圆在屏幕上变小、斜视时呈椭圆，这是世界范围的投影，不是 UI 固定像素圈。

滚动卡用完整宽度和运动长度画矩形；觉醒小电显示 2.5／3 格两圈；觉醒飞桶显示主落点和横向 5 格的诱饵落点。预览不产生网络实体；实际命中始终以服务端为准。

## 特效和模型

实体主体在正常实体渲染阶段绘制。领域、激光和拖尾在 AFTER_TRANSLUCENT 用 `SpellFields`、`ProjectileVisuals` 等绘制；所有相对坐标只进行一次 camera 平移。`SpellLayers.EFFECT/FIRE_TRAIL` 采用透明混合、颜色写入和深度测试；实心核心有适当深度写入。1.20.1 VertexConsumer 每个顶点需要显式 `endVertex()`，颜色从新版 packed ARGB 接口还原为 RGBA 参数，模型法线走法线矩阵。

`TroopModel` 读取 `models/troop` 几何与材质图集，单独处理部位、眼睛发光、火精灵全身火焰、翅膀、旗帜和攻击动作。野蛮人持剑沿已修正的手部挂点调用实际装备渲染。军团以 Minecraft 原版 SkeletonModel 为骨架，`GeneralEquipment` 加入 T 字开口桶形头盔、盾、旗帜和披风。冰精灵保持 MineClash 的 UV 和原发光遮罩；火／电／治疗精灵使用已批准的方块造型。

火球保留 Beta 3 的核心、长拖尾、火星、落地碎屑和黑烟；虚空有展开、收束和三次分层下劈；毒药有低雾、气泡与脉冲；墓园使用专用紫色飘散粒子。效果颜色与范围读取统一卡牌数值。经典法术音效由 SpellSounds 分阶段播放，大闪逐个命中播放，小电第二击为紫色。声音继续使用既有单声道素材和空间衰减事件。

## 冰冻、雪球与电击暂停

`VisualState` 基于每个实体自己的同步状态，区分冰冻、雪球减速、狂暴和克隆。`FrozenRender` 在一次实体渲染进入时保存该实体的姿态上下文，退出时关闭；`FrozenModelPartMixin` 暂时应用被保存的 pose，再在 finally 中恢复模型的真实 pose。模型通常由多个实体共享，因此不能把骨骼修改永久留在共享 ModelPart 上，否则会出现一辆车死亡导致其它车一起摆死亡姿势的串扰。

雪球将颜色变蓝，动作时间线减为原来的 0.65；冰冻暂停动作与移动；`ElectricPause` 保存攻击抬手的进度并短暂停住，解除后继续。它与打断／取消攻击不同。地狱飞龙受电击则按自身机制重置热量、锁定和三阶段攻击。各钩子保留默认行为，只在对应效果实际激活时处理实体；范围不会扩展到全局渲染时钟。

## 精灵与军团

四精灵采用服务端真实跳跃与扫掠碰撞，避免高速越过目标没有命中；墙面与友军阻挡经过专门测试。火／冰／治疗为接触后的溅射，电精灵只从有效敌人向下一个有效敌人连锁，最多 9 个，每跳 5 tick，后继距离 3 格，不使用一圈同时电击的替代做法。治疗精灵保留已有实现的一次恢复，半径 2.5；尚未模拟原作治疗圈的每一帧脉冲。寿命与碰撞容差为 Minecraft 三维适配值。

`ArmyLedger` 将每位玩家军团 ID、将军 UUID、过期时间和冷却写入世界 SavedData。跨维度和重登使用主世界 gameTime，避免靠退出重置 15 秒冷却。将军死亡释放其军团支持；幽灵失去支持后消散，仍活着的普通士兵继续存活并战斗。中立刷怪蛋有独立阵营 UUID，不绑定任何真实玩家。

小骷髅成功出手会暂时把目标的 `invulnerableTime` 清零，完成攻击后取新旧窗口的较大值恢复，不让其它攻击者意外获得免疫绕过。`CombatImpact.withoutKnockback` 包住该次伤害，既不推目标，也不清除其原有运动。部署后的前 18 tick 不攻击；测试先验证这段保护，再验证 15 次同时剑击基础伤害为 45。将军保留自身攻击节奏，不直接套用所有士兵的免疫绕过。

## 存档和迁移

NeoForge 的 Entity persistent data 在 1.20.1 Fabric 没有相同 API。`PersistentState` 由 `PersistentDataMixin` 给实体附加独立 CompoundTag，在 load/saveWithoutId 注入 `RoyaleData` 子标签，保存卡级、标签和状态数据。`ControlCooldown`、`ArmyLedger` 使用 1.20.1 SavedData 的 load/new/key 入口；没有 1.21 DataComponent 强依赖。SpiritCardLevel 和实体存档单独校验，克隆保持元素种类和 1 HP，不会错误退化为野蛮人。

保留原 Fabric 卡牌资源 ID 并不等于世界降级支持。实体内部行为和本次新卡牌结构已经变化；没有验证全部旧存档组合，不能声称 NeoForge 存档可安全降级。保留原工程与原安装，不用新 JAR 覆盖 1.21.1 实例。

## 测试入口和维护边界

`CardBalanceTests`、`SpiritBeta2Tests`、`InfernoDragonTests`、`SnowballVisualTests`、`StandaloneTests` 合计 37 项 GameTest。游戏内真实测试覆盖伤害总量、装备不叠伤、护盾溢出、镜像、克隆、诅咒、精灵命中、地狱飞龙热量／飞行／控制、军团行为、合成、圣水、NBT 和冷却。图形检查由 `StandaloneClientSmoke` 新建隔离超平坦世界，保存 PNG 后正常退出；只有显式 JVM 参数才开启。测试类保留在源码与 JAR 中，但普通游戏不注册游戏测试执行流程。

`tools/verify-production.py` 用现有正版 Minecraft 运行库、Fabric Loader 0.16.14 与 Fabric API 加载最终重映射 JAR，而非加载 IDE classes。它不读取登录令牌、用户存档或整合包 mods，只在项目内 `run-production` 写测试数据。该脚本含本机默认安装路径，换机器需改 INSTALL/JAVA；普通构建不调用它。

已有实测不能替代多人联机、Sodium/Iris/光影、大规模战斗或任意整合包兼容验证。模型截图表示该画面实际渲染成功，不等于所有角度和全部特效已由用户主观验收。
