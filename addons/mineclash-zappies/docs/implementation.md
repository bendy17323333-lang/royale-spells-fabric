# 机制与渲染实现

## 接入边界

`ZappiesMixin` 只对 `org.liziyowo.mineclash.entity.mob.Zappies` 替换 Goal 注册和远程攻击回调，并增加编队 NBT、短电弧同步数据。实体类型、碰撞箱、渲染器、GeoModel、材质、动画 JSON、死亡回调和原有雨水自伤 tick 均沿用本体。`ZappyEggMixin` 只拦截本体刷怪蛋在普通地面上的 `useOn`；原版刷怪笼配置行为保留。

原攻击控制器将动画加速 5 倍，而且伤害先于动画回弹。附属把倍率调整为 1.5625：原文件 1.25 秒的回弹帧 / 1.5625 = 0.8 秒。它调整播放时序，不替换动画文件或骨骼。

## 每辆车的战斗状态

`ZappyBrain` 由每个 Zappies 实例独立持有。没有静态共享的死亡、攻击计数器或动画缓存。

```text
部署等待 → 寻敌 / 前进 → 抬手 16 tick → 一次放电 → 后摇/间隔 → 下一次抬手
                         ↓ 电击
                     原状态暂停
                         ↓ 解除
                     从剩余进度继续
```

攻击间隔配置包含抬手时间。默认两次伤害相隔 46 tick；目标持续可见时不混入 MineClash 原 GenericAttackGoal 的另一套 cooldown。失去目标、目标死亡或失去视线会取消当前未完成的攻击；这与仅仅受到短电击不同。

`ZappySquad` 先试放三个 1×1×1.3 的原生实体体积，检查地面、实体、方块和世界边界，确认全部可放后才提交。所有车共享 squad UUID，但 owner、slot、level、deploy、configured 分别写入 `ZappiesAddon` NBT。属性由实体原生 NBT 保存。重载实体时不保留未完成的抬手或射击目标，重新开始寻敌；不会凭空补打一发。

寻路目标按前车与后排左右位置分散。路径规划的“到达”范围不等同于武器射程；若走到编队节点后仍然不能射击，会继续向敌人接近，进入武器射程就停下。避免后排卡在边缘永久零输出。碰撞和爬一格台阶使用原生导航。

## 电击：暂停而非取消

两套独立 JAR 用 `c:electrical_action_pause` 生物效果标签互通。各自注册自己的状态效果。皇室法术的 `PauseMixinPlugin` 检测到 `zappiesaddon` 时跳过自身所有暂停 mixin，由附属驱动；未装附属时皇室法术自己驱动。附属不必依赖或反射加载皇室法术类。

- `PauseMobMixin` 在 `Mob.serverAiStep` 入口短暂暂停 AI。Goal 和 Brain 的状态、剩余抬手计数、原目标和导航路径保留，不执行 `Goal.stop()`。
- `PauseLivingMixin` 保留挥手和物品使用计时，暂停主动位移，阻止处于暂停状态的攻击者直接造成新一轮近战伤害。自伤不被拦截，因此小电车自身的雨水和涉水伤害继续存在。
- 服务器用 `SynchedEntityData` 同步当前是否暂停，客户端不依赖“自己是否收到了别人的药水效果”来判断。
- `PauseIronManagerMixin` 只跳过 Iron's `MagicManager.tick` 中该玩家的施法分支。冷却、重施信息和魔力恢复继续更新；施法 ID、等级、剩余时间和附加 cast data 保留。解除后原施法继续。`PauseIronStartMixin` 阻止暂停期间新开施法，`PauseIronClientMixin` 同步停住本地施法条。
- 铁魔法生物施法由 `serverAiStep` 一并暂停。独立运行的第三方自定义 tick / 另一个动画库不一定经过这些入口，不能据此宣称对任意模组的所有特殊攻击都自动兼容。

皇室法术的 Zap、觉醒 Zap、Lightning、电精灵改用 `SpellEngine.electricStun`。冰冻、藤蔓、雪球滚压等原有非电击控制仍走原来的路径。皇室法术自带的骷髅军团、精灵和地狱飞龙也对自身额外的攻击计数进行了暂停保护。

**飞龙特例：** `InfernoDragon.onEffectAdded/onEffectUpdated` 检查共享电击标签并立即 `resetBeam()`；其 tick 在整个电击期间保持零热量和零速度，`travel` 同样暂停。AI 目标保留，热量、同步光束目标和 8 tick 命中计数重置。这样电车附属自己的状态和皇室法术自己的状态都会触发重置；解除后第一发是第一阶段伤害。普通单位和铁魔法施法不受此特例影响。

## 每实体动画时间

`PauseClock` 使用以实体为键的 `WeakHashMap`，记录进入暂停的时间及累计暂停时长：

```text
播放时间 =（暂停时取进入暂停的时间，否则取当前时间）－ 已累计暂停时长
```

解除时累计刚才暂停的时间，因此不会直接跳过 10 tick 动作。实体对象回收后对应时钟可释放。

GeckoLib 4.9.2 的 `GeoEntityRenderer` 在调用 `GeoModel.handleAnimations` 前，会把 `animatable.getTick()` 放进 `DataTickets.TICK`。仅修改 `entity.tickCount` 无法冻结这条动画链。`PauseGeoMixin` 暂时替换这个 ticket、partial tick 和实体 age，调用原处理器，然后在 `finally` 恢复输入数据。未设置过 ticket 的场合会移除临时项。**不改变共享骨骼、不重建控制器、不修改全局动画速度。**

原版 `LivingEntityRenderer` 使用同一局部时间；原版挥手计时也在实体层暂停。皇室法术的自定义模型通过修复后的 `FrozenRender` 保持每实体姿势，电击时不再次回退实体真实年龄，避免嵌套包装重复扣时。

## 伤害、声音和电弧

每发伤害临时清零目标 `invulnerableTime`，调用原 MineClash 电击 damage source，再恢复原本更长的无敌时间。这样同 tick 的三辆车都能命中，其它来源的受伤间隔仍然保留。护甲、免疫和 NeoForge 伤害取消事件照常工作。`ThreadLocal` 仅在本次命中范围内屏蔽普通受伤击退，异常时也会清理。

`ZappyCombat` 在服务器决定伤害，使用本体 `CRSoundEvents.ZAP_ATTACK`（原来的两个随机样本，音量 0.65）及原生电火花。死亡音效仍由本体 `getDeathSound()` 提供，没有新增走路音效。

发射数据包括服务器游戏时间、序号和命中点，经实体同步发送给跟踪玩家。`ZappyArcRenderer` 仅渲染 5 tick 的连贯折线带：蓝色外缘、亮蓝层、白芯、小分叉，形状快速变化后淡出。使用相机朝向的细四边形，深度测试开启但只写颜色；透明区域不会裁掉生物。`AFTER_LEVEL` 阶段只应用一次 model-view 变换，避免以前范围准星重复旋转的问题。

## 下一轮

雪球动作减速已单独排在下一轮皇室法术更新。本轮没有更改它的控制时长、击退或伤害。该功能应继续使用每实体局部时钟，并回归同模型未受影响者、死亡、卸载和解除状态。
