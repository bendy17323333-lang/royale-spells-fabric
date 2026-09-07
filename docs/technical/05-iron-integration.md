# 05 铁魔法系统接入

[返回目录](README.md)

核心：[IronIntegration](../../src/main/java/dev/royalespells/iron/IronIntegration.java)、[RoyaleIronSpell](../../src/main/java/dev/royalespells/iron/RoyaleIronSpell.java)、[MirrorIronSpell](../../src/main/java/dev/royalespells/iron/MirrorIronSpell.java)。本章按 3.16.3 的调用契约整理，不承诺任意未来铁魔法版本二进制兼容。

## 1. 注册原生法术

使用 `DeferredRegister<AbstractSpell>` 注册到 `SpellRegistry.SPELL_REGISTRY_KEY`，命名空间 `royalespells`。遍历 30 个 `IronSpellProfile`：镜像使用 `MirrorIronSpell`，觉醒军团使用 `EvolvedArmySpell`，其余使用 `RoyaleIronSpell`。

每个 profile 提供学派、最低稀有度、最大等级、魔力与等级增量、冷却和吟唱。默认最大等级按最低稀有度：普通 10、罕见 8、稀有 6、史诗 5、传说 3；军团为 1。

原生法术 ID 是 `royalespells:<profile.id>`，学派 ID 是 `irons_spellbooks:<school>`。不要把资源域误写成原生法术命名空间。界面名称键是 `ironspell.royalespells.<id>`。

## 2. 物品、法术与等级是不同对象

一个法术可以存在多个等级的卷轴，也能抄入法术书或附到武器。不是为每个等级注册一个物品 ID。

`IronIntegration.scroll(profile,level)` 创建铁魔法原生 `SCROLL`，再用 `ISpellContainer.createScrollContainer` 保存法术与等级。创造卷轴组遍历当前启用法术的有效等级。普通卡牌物品仍在独立物品组，两者不是同一件物品。

旧配方使用 `neoforge:conditions` 检测未安装铁魔法才加载；普通生存卡牌使用也受到 `allowCard` 限制。因此只删除旧配方并不足以阻止玩家通过已有卡牌绕过原生系统。

## 3. 获取、抄写、升级和战利品

普通皇室法术进入对应学派候选列表，卷轴生成仍由铁魔法的筛选与战利品流程决定。本模组没有保证每个箱子都刷皇室卷轴，也没有为所有结构重新写固定掉落表。

流程由原生系统负责：纸+学派媒介+墨水制作卷轴，抄写到法术书，提取卷轴，奥术铁砧合并升级或附到允许武器。本模组扩展的材料过滤和觉醒转化见 [07](07-elixir-structures-rituals.md)。

`ModifyDefaultConfigValuesEvent` 后清空学派与战利品候选缓存，避免配置修改学派、禁用或战利品权限后仍沿用旧列表。对应访问器触及上游私有字段，是升级时的敏感位置。

觉醒骷髅军团单独 `allowLooting=false`，默认 `allowCrafting=false`，通过天然重油仪式获得。不能从它出现在创造组推断普通生存制作列表也应出现。

## 4. 保留原生生命周期

`RoyaleIronSpell` 只提供本模组的条件与效果创建。一般不要在 `onCast` 再手动扣一次魔力、消耗卷轴或添加一遍普通 CD，否则上游结算后会发生重复收费。

`castTicks=0` 对应 `CastType.INSTANT`，其余为 `LONG`。普通皇室法术没有直接使用原生连续引导类型，但镜像会代理原生连续法术的完整入口。

结束声返回 `Optional.empty()`，主要声音在实际效果阶段由本模组触发。吟唱完成声、投射物命中声和持续区域每轮打击声应分清，不能都挤到 `getCastFinishSound()`。

## 5. 镜像：复制原法术的生命周期

### 历史记录

监听成功施法事件 `SpellOnCastEvent`，记录玩家 `PersistentData.RoyaleIronMirrorHistory` 中的 Id/Level，同时向客户端发送 `MirrorHistoryPayload`。不记录镜像自身，也不把镜像代理结果再次写成新来源，避免反复加一无限堆级。

### 开始镜像

1. 正在施法时先取消当前施法，本次返回失败，不并行启动两种法术。
2. 检查眩晕/冻结、历史记录、双方启用状态、目标原法术学习条件。
3. 检查镜像自己的原生施法条件；原法术还有非本次镜像的连发时，拒绝开始。
4. 新目标等级为 `min(255, 上次实际等级 + 1)`，可暂时高于通常卷轴上限。
5. 需要魔力时预检查“提高一级后的原法术费用 + 镜像附加费用”。镜像附加费用按等级通常为 30/20/10，仍受其方法最低 1 的约束。
6. 发送镜像自己的 `SpellPreCastEvent`，取消则返回。
7. 暂时移除原法术冷却条目，调用原法术 `attemptInitiateCast`，随后在 `finally` 恢复原先保存的条目。

临时移除只针对原法术的旧冷却检查；学习、目标、取消、物品和原法术重写方法仍运行。它不是直接调用 `onCast` 或反射复制投射物。

### ACTIVE 会话与收费

`ACTIVE[玩家 UUID]` 保存目标法术、目标等级、镜像等级、是否已收附加费用。原法术发出第一次成功事件时加上镜像附加费用，并按 CastSource / 创造配置处理镜像 CD。后续连发不重复收附加费用。

没有正在施法且没有原目标连发状态时，tick 清理会话；玩家离线、启动失败、服务器停止也会清理相应内存状态。ACTIVE 不持久化为存档中的半完成吟唱。

**需要保留的边界：**这段逻辑调用的是上游具体方法，不能保证所有第三方法术都遵守同一契约。原法术冷却恢复与新施法 CD 的叠加关系应检查其实际实现；不要简化写成“无条件清除原法术 CD”。

## 6. 英雄滚桶再施放

原生英雄滚桶注册两段再施放，窗口 600 tick。首段部署英雄，后续通过同一个法术入口查找 40 格内自己的英雄，创建 3 格滚动效果并在结束时恢复一半已损失生命。

需要保留原生 Recast 状态；不能把第二次施放当作又一次召唤。它与独立卡牌潜行花 1 圣水的激活方式不同。英雄死亡或尚未生成时拒绝激活。

## 7. 控制共享池与军团共享池

冰冻与藤蔓：`ControlCooldown` 使用主世界 `SavedData` 保存每位玩家绝对到期 tick。它始终为 300 tick，同步两张旧卡牌物品 CD，也通过 `SharedControl` 同步两种原生法术 CD。该池不依赖普通单法术冷却配置，被装备减 CD 后也不能跳过持久化校验。

军团：`ArmyLedger` 保存一支军团与共享 CD；基础 300 tick，实际原生入口使用 `MagicManager.getEffectiveSpellCooldown` 得到玩家有效 CD。因此军团与控制池的装备减 CD 行为不同，不能笼统写成都固定无法改变。

军团卷轴、法术书、镜像和号角最终进入同一个部署路径。号角使用 `CastSource.NONE` 并走额外条件，不耗魔力，但仍检查启用、取消、军团存活与共享冷却。

## 8. 战斗事件与反制

- 伤害：效果保存原生法术 ID，按该法术获取 DamageSource，调用 `DamageSources.applyDamage`，继续经过原生抗性与伤害事件。
- 治疗：发布 `SpellHealEvent`，随后调用 `target.heal(amount)`。当前没有把事件当成可替换金额或可取消事务；不要在文档中承诺不存在的治疗取消行为。
- 召唤：给生物写 `RoyaleIronSpell/Level`，发布 `SpellSummonEvent`。自己的 `Summoned` 生物额外乘施法者 `SUMMON_DAMAGE` 属性。
- 打断：本模组眩晕通过原生 `serverSideCancelCast` 中断玩家吟唱。
- 驱散：`IronDispelMixin` 给本模组效果及召唤物提供上游反制接口，见 [08](08-data-and-mixins.md)。

这些接口各有调用时机。发出一个事件不等于它一定可取消，也不等于所有第三方监听器都已被实机验证。

## 9. 升级铁魔法时的检查顺序

先确认依赖版本、类名和方法签名，再检查 Mixin 应用，再跑服务端系统测试，最后看实际界面与动作。重点为 ScrollForge/ArcaneAnvil 的槽位及方法、SpellBar/SpellWheel 的 blit 调用、私有候选缓存、GeoRenderer 的签名、镜像生命周期、伤害与召唤事件。

具体敏感点清单在 [08](08-data-and-mixins.md)。只要 `remap=false` 指向的上游方法变了，即使 Java 编译成功，游戏仍可能在加载 Mixin 时崩溃。
