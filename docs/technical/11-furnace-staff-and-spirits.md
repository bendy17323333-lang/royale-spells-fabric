# 11 熔炉法杖与四种精灵

[返回目录](README.md)

对应公开铁魔法适配 Beta 2 `1.6.1-beta.2`，Minecraft 1.21.1 / NeoForge 21.1.249 / Iron's Spells 1.21.1-3.16.3。本版整合此前仅本地交付的 1.6.0 Beta 2–4；内部迭代编号与公开 Beta 次序不同。以下是实现规则，不代表对所有整合包、光影或多人延迟的验证保证。

## 1. 获取与使用

先在工作台合成烈焰熔炉法杖。摆放如下，空格表示空槽：

```text
空       炼药锅   火焰弹
空       烈焰棒   铁锭
烈焰棒   空       空
```

这是一条有序配方，只在加载铁魔法时启用。配方资源：[furnace_staff.json](../../src/main/resources/data/royalespells/recipe/furnace_staff.json)。合成的是可反复使用的法杖，不消耗整根法杖来召唤。

奥术铁砧左槽放法杖，右槽放一种元素媒介，取出结果消耗一个媒介。基础配方已是烈焰，不能用同种元素重复转换。

| 元素 | 媒介 | 法杖 ID | 基础魔力 | 命中效果 |
| --- | --- | --- | --- | --- |
| 烈焰 | 火焰弹 | `royalespells:furnace_staff_fire` | 32 | 半径 2.4 格内 10 伤害，无地形爆炸 |
| 寒冰 | 浮冰 | `royalespells:furnace_staff_ice` | 28 | 半径 2.2 格内 7 伤害，冻结 1 秒 |
| 雷电 | 紫水晶碎片 | `royalespells:furnace_staff_electro` | 36 | 每 0.25 秒连锁至 3 格内下一单位；含首个目标最多 9 个，每次 7 伤害、眩晕 0.5 秒 |
| 治疗 | 闪烁的西瓜片 | `royalespells:furnace_staff_heal` | 32 | 半径 2.2 格内 7 伤害；3 格内友军恢复 4 生命 |

表格是基础效果；施法者法术强度和相关学派强度改变精灵的 power，召唤伤害属性另外影响伤害。治疗不乘召唤伤害属性。实际消耗、施法速度和冷却也遵守铁魔法配置与属性。

法杖主手提供 +10% 法术强度、+25 最大魔力，稀有品质，耐久 512，物品防火。副手也能施法，但不会重复获得主手属性加成。普通施法不扣耐久；普通耐久/附魔逻辑沿用铁魔法 StaffItem。

每次召唤一只，基础吟唱 12 tick（0.6 秒），四种元素共享 120 tick（6 秒）基础冷却。精灵基础生命 4，寿命 400 tick（20 秒），跳跃爆发后消散；每位施法者所有已加载维度合计最多 6 只，单维度最多 96 只。后者是安全上限，并不保证普通装备能在寿命内堆到 6 只。

管理员测试示例：

```mcfunction
/give @s royalespells:furnace_staff_fire
/give @s royalespells:furnace_staff_ice
/give @s royalespells:furnace_staff_electro
/give @s royalespells:furnace_staff_heal
```

## 2. 为什么分为公共实体与可选法杖

[SpiritElement](../../src/main/java/dev/royalespells/spirit/SpiritElement.java) 保存稳定字符串 ID、颜色、媒介、伤害和费用。[ElementalSpirit](../../src/main/java/dev/royalespells/entity/ElementalSpirit.java) 只继承 Minecraft 的 PathfinderMob，并实现本模组 Summoned。

实体和渲染器在无铁魔法模式也能注册，以保证旧存档、命令和隔离测试不因缺少上游类型而崩溃。四件法杖和四种原生法术则只通过 `IronIntegration.install → SpiritSpells.install` 注册。没有铁魔法时，不能用工作台获得法杖；不伪造一套独立魔力系统来冒充铁魔法。

四种法术 ID 为 `royalespells:summon_<fire|ice|electro|heal>_spirit`，单独继承 AbstractSpell，不加入旧 `Spell` ordinal 枚举，也不改变原有 30 项 IronSpellProfile 的索引。默认最高一级、稀有等级，不参与随机卷轴战利品和常规卷轴锻造。它们主要作为绑定在法杖上的固有法术；这不妨碍原生施法事件、法术轮盘和镜像识别成功施法。

## 3. 原生施法链

实现：[FurnaceStaffItem](../../src/main/java/dev/royalespells/iron/FurnaceStaffItem.java)、[SpiritSpells](../../src/main/java/dev/royalespells/iron/SpiritSpells.java)、[SpiritSpell](../../src/main/java/dev/royalespells/iron/SpiritSpell.java)。

```text
RightClickItem（只截获 FurnaceStaffItem）
  → prepare：确保锁定的 ISpellContainer 对应当前元素
  → 正在吟唱则使用原生取消入口
  → AbstractSpell.attemptInitiateCast，CastSource.SWORD
  → 原生魔力、冷却、状态、事件检查
  → LONG 吟唱 12 tick，原生 MagicManager 推进
  → onCast 再检查落点与数量 → addFreshEntity
  → IronSpellSystem.summon 发出原生召唤事件
  → 保存并同步四种元素共同的冷却截止时刻
```

截获只针对自己的法杖，因此其它铁魔法法杖继续使用其原生选择规则。这里右键总是召唤该法杖绑定元素，避免玩家原先在法术书选了火球却从熔炉法杖放出火球。原生 `SWORDS_CONSUME_MANA` 配置仍有效；默认配置下需要魔力，不硬编码绕开管理员设置。

生成位置在施法者前方约 1.2 格，经 ground 检查地表。验证目标区块已加载、世界边界、玩家交互权限以及完整 0.6×0.8 实体空间。禁止眩晕/冰冻期间施法。不使用客户端传来的坐标决定服务器生成位置。

共享截止时刻保存在玩家 PersistentData 的 `RoyaleSpiritCooldown`，使用服务器主世界 gameTime，避免跨维度使用不同时间基准。登录、重生时同步四种原生 PlayerCooldowns；死亡克隆事件复制截止时刻。切换物品、换元素和换维度不会清除该计时。实际有效冷却使用原生计算，最短保底 20 tick。

## 4. 铁砧转换如何保留数据

[IronElixirAnvilMixin](../../src/main/java/dev/royalespells/mixin/IronElixirAnvilMixin.java) 在原生 ArcaneAnvilMenu.createResult 结束时处理自己的法杖。输入不是法杖时继续原有圣水/重油逻辑；没有合法跨元素媒介时不凭空生成结果。

`ItemStack.transmuteCopy(targetStaff, 1)` 更换基础物品并保留组件补丁，然后 `prepare` 只把绑定法术重建成目标元素的单槽锁定容器。因此名称、耐久、附魔等保持，旧元素绑定不会漏到新物品上。四种元素保持相同基础属性，避免替换物品时产生两组叠加属性。

消耗交给原生结果槽 onTake，不在 createResult 预览时扣物品，避免不停更新界面就吃材料。GameTest 覆盖 12 种有方向的转换和 4 种同元素拒绝，验证实际取出结果后的输入数量，以及名称、77 点损耗和耐久 II 附魔。

## 5. 寻路、跳跃和唯一命中

精灵使用服务器 AI。默认沿用 SummonOrders：优先玩家攻击的目标，其次对玩家有敌意的附近单位；不会主动追杀没有敌意的和平生物。离主人较远时沿用跟随逻辑，主人离线时不会变成见人就打的野生怪。

治疗精灵额外查找附近受伤友军，选择最近且可见者。即便没有敌人，也能跳向受伤的主人/友军进行治疗。这是治疗精灵的用途差异，不只是改成黄色的火精灵。

进入三格左右且可见目标时，提交一次跳跃：保存目标 UUID，停止寻路，设置有限水平速度和向上速度。提交后不每 tick 瞬移到目标，不允许穿墙重选远处目标。从第一个空中 tick 起，用上个 tick 到当前 tick 的身体中心线段，对按精灵半尺寸和 0.24 格接触容差扩展的目标包围盒做相交检测；按最早接触位置选取可见敌人，治疗精灵也接受受伤友军。原目标死亡不阻止命中实际撞到的下一单位；不按原目标 UUID 在远处补伤害。落地或 30 tick 上限会在当前位置结束。错过目标不会对原目标远程补伤害。

起跳前明确离开 onGround 状态并清理旧寻路移动目标，避免第一步被地面摩擦削掉水平速度。水平初速按约十一 tick 的 0.91 空中阻力总和（约 7.2）估算，上限 0.5 格/tick；飞行仍由原版碰撞和重力推进。回归包含从六格外真实跑近再起跳的四元素场景，而不只测试直接调用 beginLeap。

`spent` 保证 detonate 最多一次。火、冰、治疗命中后发送粒子和声音，再 discard；电精灵转入有时限的连锁阶段，身体消失后继续结算剩余跳数。实体禁止掉落、经验和物品拾取。寿命上限也负责清理长期找不到目标的精灵。冻结时停止额外行动，客户端粒子暂停，骨骼沿用 FrozenRender。

### 5.1 电精灵原作依据

2026-09-07 核对了三个官方更新和官方实机视频：

- [2020 赛季 16](https://supercell.com/en/games/clashroyale/blog/release-notes/season-16/)：最多九个敌人，包括初始命中。
- [2025 年 12 月平衡](https://supercell.com/en/games/clashroyale/blog/release-notes/december-balance-changes/)：连锁间隔从 0.2 秒调整为 0.25 秒。
- [2026 年 8 月最终平衡](https://supercell.com/en/games/clashroyale/blog/news/final-august-balance-changes-826/)：8 月 26 日更新把连锁距离从 4 格改为 3 格。
- [官方 SHOCKTOBER 实机视频](https://www.youtube.com/watch?v=QIb1bjTlD7I)：查看约 17–22 秒的电精灵冠簇与连接单位的蓝白折线电弧。旧视频只用于形象与表现参考，时序采用后续官方调整。

当前常量 `CHAIN_TARGETS=9`、`CHAIN_INTERVAL=5`、`CHAIN_RANGE=3`。0.25 秒以正常 20 TPS 换算，原作一格在本模组映射为一个 Minecraft 方块。服务器低 TPS 时仍按游戏 tick 推进，不在客户端按墙钟抢先伤害。

### 5.2 连锁状态机与渲染

首次 detonate 只伤害初始目标，并将精灵设为不可攻击、无 AI、不可推挤、不可见的连锁载体。`chainWait` 每次减一，到零后才选择下一个目标：以上一次目标包围盒中心为起点，在三维三格范围内查询，排除 visited UUID、友军、不可攻击目标和禁用 PVP 的玩家，用碰撞射线排除隔墙目标，按距离和实体 ID 稳定排序。一次命中只加入一个 UUID，不会反复在两名敌人间弹跳。

前一目标还活着时，起点跟随其当前位置；前一目标被电死或移除时保留上次命中点，链条仍能继续。下一跳没有合法目标、累计九个目标或达到 50 tick 截止时刻时销毁载体。载体仍计入短期精灵数量上限，不能重新 detonate。

[SpiritArc](../../src/main/java/dev/royalespells/entity/SpiritArc.java) 是独立、无伤害且不可点击的视觉实体，保存相对终点和年龄，寿命七 tick。[SpiritArcRenderer](../../src/main/java/dev/royalespells/client/SpiritArcRenderer.java) 在现有粒子后世界阶段绘制：深蓝微光、蓝色外层、白色细芯，折点随年龄改变，前约 1.3 tick 完成伸展，随后淡出；终点出现短促局部火花。电弧没有范围伤害环，伤害始终由服务器链条结算。使用现有只写颜色的特效 RenderType，保留深度测试，不让透明光效写入实体深度。

其它三种爆发使用三维距离和可见性过滤，范围规则比旧领域法术的圆柱筛选更严格。

伤害使用原生 SpellDamageSource，按元素提供学派；经 CombatImpact 局部禁止击退，尊重 PVP 和友军。ElementalSpirit 不实现上游 IMagicSummon，所以手动应用一次 SUMMON_DAMAGE，不能再在未来给实体加 IMagicSummon 后保留这个乘数，否则会双倍应用属性。

## 6. 网络与存档

| 状态 | 同步/保存 | 意义 |
| --- | --- | --- |
| ELEMENT | SynchedEntityData 字符串；SpiritElement NBT | 客户端选择独立模型、颜色 |
| LEAP | SynchedEntityData 整数；Leap NBT | 客户端抱身/收腿跳跃姿态 |
| owner | SpellOwner UUID | 友军、伤害归属、召唤限制 |
| leapTarget | LeapTarget UUID | 一次跳跃提交的对象，缺失时只落地结束 |
| life | SpellLife | 保存寿命剩余值，不因区块重载恢复 20 秒 |
| power | SpiritPower | 保存施放时强度，范围有上限 |
| spent | Spent | 已爆发的重载实体不能再次爆发 |
| CHAIN | SynchedEntityData 布尔；ChainActive | 身体隐藏后进入连锁阶段 |
| chainWait / chainExpires | ChainWait / ChainExpires | 距离下一跳的 tick 和绝对截止时刻 |
| chainVisited / chainLast | ChainVisited UUID 列表 / ChainLast | 已命中的最多九个 UUID 与上个单位 |
| chainFrom | ChainX/Y/Z | 上一单位死亡或消失后仍可继续的命中点 |

公共实体通过可选桥接调用 Iron 伤害/治疗，在独立模式使用本模组基本伤害，不从客户端渲染类引用服务器战斗。

## 7. 模型与素材来源

### 7.1 当前工作源与造型

8 份实际 Blockbench 工程保存在 [art/blockbench-v3](../../art/blockbench-v3/README.md)。通过 Blockbench MCP 创建、导入和检查，工程内嵌纹理。`export-runtime.py` 读取保存后的工程，导出四份实体 JSON、四份法杖 OBJ、MTL 和物品 JSON。旧 `make-spirit-models.py`、`make-furnace-staff.py` 已改成新导出器的兼容入口，旧生成程序归档于 `art/legacy-beta2`，避免误运行时恢复旧圆形模型。

冰精灵直接适配用户提供的 MineClash 0.7.5：沿用 128px 原图、发光遮罩和几何，统一缩小到 0.75，骨骼别名适配本模组。运行时不需要安装 MineClash，也没有复制其实体注册或 AI。原作者、归档哈希与研究记录见 [MODEL-STUDY.md](../../art/mineclash-reference/MODEL-STUDY.md)。原始动画文件留作参考；当前动作仍由本模组控制，不声称完整移植 GeckoLib/Molang 动画。

根据用户最终反馈，其余三只也采用方块主体、短方肢和贴面像素五官。火精灵是冒火煤块；电精灵保留闪电胡须和电弧；治疗精灵保留单齿与扁平小舌头。不再使用先前圆滚主体、深空嘴和圆形眼窝。法杖顶部的小精灵同步采用这一套造型。

Beta 4 的火焰从主体四周和底部升起：七组双面动态火焰片覆盖正面下半部、两侧、背面、正面边缘和顶部，正面眼睛周围留出识别空间；侧面增加发光煤缝，行走时从身体周围散出小火星。电精灵头顶改为三段短紫色冠簇，细电弧贴近冠簇，移除原来的长闪电杆。两者同步导出到对应法杖。

法杖以熔炉角色手持的木杆炼药锅为参考：切角方形深锅、较厚的口沿与金属束箍、侧环、熔融液面、溢流、木杆及皮革握把。材质使用大像素色块和有限色阶，让造型更接近 Minecraft 与铁魔法装备。原作参考图不打入 JAR。

### 7.2 坐标和逐面 UV

Blockbench 为 Y 向上、-Z 正面；TroopModel 为 Y 向下、24px 基准。导出时翻转 Y 和面顺序，组的绝对原点转成父组相对原点。单独网格的休止旋转已在编辑器中烘焙，骨骼休止角继续保留。OBJ 使用 Y 向上、像素除以 16 的方块单位。

新 JSON 在每个网格中提供 `uvs`，每面 8 个归一化 UV 数值，与面顶点顺序对应。三角面复制最后一个顶点为渲染四边形；零面积面跳过，避免平面方块产生非法法线。UV 根据每张纹理自身的 `uv_width/uv_height` 归一化，不能把 MineClash 的 128px 图当作 1254px 图集。其它召唤物没有 `uvs` 时仍使用旧材质采样，不改变已有野蛮人等模型。

### 7.3 发光、透明火焰和图集

[SpiritRenderer](../../src/main/java/dev/royalespells/client/SpiritRenderer.java) 按元素选择模型和纹理；[TroopModel](../../src/main/java/dev/royalespells/client/TroopModel.java) 提供摆身、摆臂、短腿动作与同步跳扑。普通模型、局部发光和透明火焰分开提交。冰精灵第二遍使用原独立遮罩，只有遮罩中的像素发光；身体保留环境光照。

`spirit_flame.png` 是内置 ImageGen 生成的 2×2 四帧像素火焰。实体纹理不是方块精灵图，因此每两 tick 在四分区间切换 UV；火焰使用支持 alpha 的 `entityTranslucentEmissive`，不能使用会把透明像素 RGB 加亮的纯 additive eyes 混合。

法杖使用 NeoForge 内置 `neoforge:obj` 加载器。实体目录的材质不会自动进入物品图集，故 [blocks.json](../../src/main/resources/assets/minecraft/atlases/blocks.json) 显式加入对应来源。[FurnaceSpriteSource](../../src/main/java/dev/royalespells/client/FurnaceSpriteSource.java) 在资源重载时为物品图集创建 1024px 的最近邻上传副本，火焰每帧为 512px、每两 tick 播放一帧；不改变磁盘上的生成原图。这样避免 1254px 材质及 627px 火焰帧将整个方块图集降到低 mip 级别。旧版未使用的 `item/furnace_staff_atlas` 在本图集中排除。实体继续直接读取原始 PNG，归一化 UV 不受物品图集尺寸影响。

### 7.4 资产归属与图标

法杖及火、电、治疗精灵的材质和火焰均通过内置 ImageGen 生成，原始 PNG 直接复制保存。图集布局和完整提示词见 [最终提示词](../../art/blockbench-v3/PIXEL-FINAL-PROMPTS.md)，概念与被替代稿的提示词见 [迭代记录](../../art/blockbench-v3/IMAGEGEN-PROMPTS.md)。逐文件来源与 SHA-256 见 [SPIRIT-ASSET-SOURCES.json](../../SPIRIT-ASSET-SOURCES.json)。冰精灵原模型、皮肤与发光遮罩改编自 [MineClash](https://www.curseforge.com/minecraft/mc-mods/mineclash)，作者 **LiziYowo / MineClash 团队**，来源版本 0.7.5。MineClash 原资源保留原作者和原许可归属，说明也随 JAR 的 `model_credits.txt` 提供。

四张法术图标现在使用皇室战争原卡图，原 PNG 为 302×363；不裁剪、重画、压成方形或另加卡框。[导入工具](../../tools/import-spirit-cards.py) 从固定 RoyaleAPI 资源提交复制原文件，URL 与哈希记录在 [SPIRIT-CARD-SOURCES.json](../../SPIRIT-CARD-SOURCES.json)，原作图片归 Supercell。旧的 128×128 模型截图图标已经替换。

图标路径仍为 `royalespells:textures/gui/spell_icons/summon_<element>_spirit.png`，因此 [IronCardUi](../../src/main/java/dev/royalespells/client/IronCardUi.java) 的 namespace/path 识别直接生效：热键条高 22、轮盘高 24，宽度由真实图片比例计算；皇室卡牌隐藏原生额外槽框/金框，保留自带边框、选择标记和冷却遮罩。原生铁魔法图标继续使用原有比例和框。不改手持法杖的物品模型，也不将模型导出器用于重新生成法术图标。

音频现在使用四种精灵各自原作分类下的 22 个 OGG，注册 16 个阶段事件：首次部署、跳扑、命中、低音量脚步。电精灵每次连锁命中播放原作电击声，不再复用普通小电部署事件；火、冰也不再借用大火球或冰冻法术整段声音。部署标志写入 NBT，重载不重复登场。脚步至少间隔 10 tick，音量 0.075×全局 0.9，且不使用攻击音轨。所有音轨为单声道、64 格线性衰减；单声道转换不裁剪、不变速、不变调。治疗跳扑采用原作 Heal Spirit 分类中的共享精灵移动叫声，冰跳扑采用其分类中火精灵命名的共享攻击声；未虚构独占录音。参见 [精灵音轨来源与处理](../../SPIRIT-AUDIO-SOURCES.json) 和 [导入器](../../tools/import-spirit-audio.py)。

## 8. 骷髅军团修复

原版 ArmySkeleton 自己重写了 doHurtTarget，因此没有继承 AllySkeleton 已有的临时受伤间隔处理。此次在 **小兵自己的真实伤害调用** 前临时将目标 invulnerableTime 置零，在 finally 中恢复为旧计时和本次计时的较大值。正常小兵与紫色幽灵同样生效，将军仍走原生间隔。其它攻击者不会因此绕过受伤间隔。

回归测试用同一 tick 的 15 次真实 doHurtTarget：无护甲目标从 500 降至 473；小兵转化为幽灵后再攻击一次降至 446。各次弱伤害不再被同一受伤窗口吞掉，仍无击退。此数字用于机制验证，不是所有装备、护甲、实战站位下的 DPS 保证。

虚化小兵继续保持 isPushable=false，不通过恢复普通碰撞来取消免击退。新增同军团短距离分散：只查询 0.65 格附近的友方军团单位，在距离小于 0.56 格时产生有限横向移动，每 tick 最大 0.075 格，并通过实体 move 保留方块碰撞。完全重合时用 UUID 生成成对相反方向，避免距离为零造成永久重叠。进入近战距离后停止继续向目标中心寻路；冻结、眩晕和消散阶段不执行分散。

### 8.1 后排将军与侧翼小兵

Beta 4 的 [ArmyCombatGoal](../../src/main/java/dev/royalespells/army/ArmyCombatGoal.java) 取代全军共用的普通 MeleeAttackGoal。启用条件只检查活的合法目标、部署与消散状态，不再要求第一次寻路就成功；失败时持续有上限地重试，避免虚化后因无路径而永久不启动攻击目标。

每十 tick 缓存附近 14 格内同军团成员。将军按前五名小兵的位置、较慢的 0.90 推进倍率和约 0.75 格跟随距离控制前进。锚点最多与当前位置一样远，不会生成远离敌人的后退路径；敌人进入原有较长的法杖攻击范围时立即停步迎战。小兵以更高速度主动冲锋和绕侧，从而自然形成前排；将军不会看到目标就逃跑。这里不是远程免伤、传送或绝对保证将军永远不会被围攻。

小兵 `ArmySlot` 固定为 1–15，生成时分配、存档时保留；旧存档缺失时用 UUID 稳定分配。每名小兵有不同的环形接敌方向。发现前方 1.25 格内有同军团小兵阻挡，或连续没有进展时，保持约 24 tick 的侧翼目的地；左右由槽位决定，长期停滞可换边。近战环内走外侧弧段，不抄近路挤向目标中心。已经够得着目标且有视线时停止追中心并起手攻击。

远处和复杂地形仍用原版 Navigation，每八 tick 才重规划；近处平坦位置检查沿途区块、世界边界、地面支撑与方块碰撞，使用 MoveControl 到达小数坐标，避免原版节点把多个小兵都舍入同一个方块中心。没有设置 noPhysics，也没有穿墙传送。幽灵的微分散优先横向，减小向后挤出近战的分量。

虚化会清理死亡计时、半次 STRIKE、旧目标 UUID 和路径，四 tick 后可重新起手。基础移动速度为将军 0.25、普通小兵 0.28、幽灵 0.30，仍叠加既有小骷髅 25% 速度属性。`SummonOrders` 在玩家指令、维持目标及中立寻敌时均排除不可攻击单位，避免攻击方被无敌幽灵吸引。主人攻击优先、和平/中立生物保护、PVP、军团账本和护盾规则保持。

### 8.3 将军阵亡后的普通生还者

将军死亡结束该觉醒军团的支持账本并保留共享冷却。只有已虚化幽灵进入 20 tick 消散；未虚化小兵保持当前血量、主人、队形槽位、低伤害近战和剩余寿命，以普通小骷髅身份继续战斗。它们以后受到致命伤正常死亡，不能重新虚化。保存/重载不会恢复已结束的虚化支持。铁魔法驱散直接移除被驱散的实体，仍然有效。

## 9. 验证与边界

服务端测试：[SpiritTests](../../src/main/java/dev/royalespells/test/SpiritTests.java)、[SpiritIronTests](../../src/main/java/dev/royalespells/test/SpiritIronTests.java)。客户端画廊：[SpiritClientSmoke](../../src/main/java/dev/royalespells/client/SpiritClientSmoke.java)，启用 `royalespells.visualSmoke=true` 和 `royalespells.spiritSmoke=true`；Gradle 的 runSpiritVisual 使用独立目录，不打开用户存档。

本轮验证记录在 [公开 Beta 2 验证](../../VALIDATION-1.6.1-beta.2.md)。[旧本地 Beta 4 验证](../../VALIDATION-1.6.0-beta.4.md) 保留为历史证据。服务器新增覆盖单次初始命中、每五 tick 连锁、九目标上限、三格范围、隔墙停止、死亡起点和存档续接，以及将军实际后排站位、全军转化后继续攻击、受阻幽灵从侧面绕路与冻结不漂移。既有同刻十五次伤害、幽灵分散、铁砧转换、原生施法与驱散测试也执行。

历史外观和机制结果分别保留于 [Beta 3 验证](../../VALIDATION-1.6.0-beta.3.md) 与 [Beta 2 验证](../../VALIDATION-1.6.0-beta.2.md)。编译成功不能当成外观通过；用户整合包联机、光影和长期平衡仍需实际使用反馈。
