# 1.9.3 女巫卡牌、AI 与特效交接验证

验证日期：2026-10-11。Fabric 成品版本 **1.9.3 / MC 1.20.1**，NeoForge **1.9.3-beta.4 / MC 1.21.1**。本记录区分机制测试、图形客户端与实际用户整合包；不把编译成功当成画面验收。

## 本次可确认的结果

| 检查 | 环境 | 结果 |
| --- | --- | --- |
| Fabric GameTest | Loader 0.16.14、Fabric API 0.92.6+1.20.1，游戏 Java 17 | **51/51** |
| NeoForge 独立 GameTest | NeoForge 21.1.249、Java 21，无铁魔法 | **110/110** |
| NeoForge 同装 GameTest | 上述版本 + Iron's Spells 1.21.1-3.16.3 | **183/183** |
| Fabric 实际发布 JAR 客户端 | remapped 成品 JAR，独立测试世界，无外部光影 | **62 张本轮截图**，控制状态通过，1721 个实际 GPU 提交采样通过连续性阈值 |
| NeoForge 独立图形客户端 | ModDev，无铁魔法，独立女巫卡牌实体 | **20 张截图，359 个有体积 draw 的帧**，正常保存并退出 |
| NeoForge 同装图形客户端 | ModDev，塔内女巫及独立新增塔变体 | **24 张截图，330 个有体积 draw 的帧**，结构放置成功并正常退出 |
| 构建 | Gradle 9.4.1；Fabric 构建由 JDK 21 启动、输出 Java 17；NeoForge Java 21 | 两份 build 成功 |

同装目录只有 Iron's Spells 3.16.3、GeckoLib 4.9.2、Iron's Lib 2.1.0、Player Animation Lib 2.0.4、Curios 9.5.1。本轮没有加入 MineClash 或电车附属模组，不能把上述数字理解为它们也已完成兼容验收。

## 具体测试了什么

新增 SummonAiTests 使用实际世界寻路、碰撞和攻击，覆盖主人在后方时小骷髅越过闲置 home 接敌造成伤害、小僵尸持续攻击、母体丢失目标后护卫保留战斗、玩家命令优先、空闲护卫移到前方且不主动伤害牛。

WitchTests / WitchCardTests 覆盖普通与觉醒的实际弹体范围伤害、友方排除、不击退、首波四只归魂一次性账本、存档、克隆、控制状态、镜像等级。新回归用**生存模式真正调用两种卡牌的物品使用路径并扣 5 圣水**；也实际调用两种刷怪蛋的 useOn，验证种类和母子阵营。不是仅通过召唤指令绕过物品逻辑。

NeoForge 同装额外检查普通/觉醒女巫均不在铁魔法法术注册表，独立卡牌配方可用；原塔保留、独立女巫塔可放置、塔顶居民保持 NeutralWizard 继承、笼中灾厄村民、地下室刷怪笼、巡逻、数量限制与居民持久化。

真实截图检查包括两种女巫的正面、侧面、背面和斜角，以及左手蓄力/出弹、攻击飞行、命中、召唤、归魂、回血和觉醒外观。确认左手施法、右手持杖，普通版有腿、觉醒版悬空袍摆。NeoForge 塔概览/居民/巡逻图已目视检查；笼内截图距离过近，不能仅凭该画面验收笼内构图，实体种类另由 GameTest 检查。

Fabric 连续性日志：positionSpikes=0、clockBackwards=0、phaseBackwards=0、impactClockDiscontinuities=0、nonFinite=0。自动统计只说明采样满足设定阈值，不证明每帧都平滑；不是 GPU frame time 或 FPS 基准，也不是网络延迟测试。旧 QA 世界名中仍有 1.9.2 字样，以本次成品哈希和日志时间为准。

## 失败记录与未覆盖范围

- 首轮 Fabric/NeoForge 完整 GameTest 出现过护卫站位、将军命中等断言失败，Fabric 另有雪球释放计时断言；增加诊断、更新两张女巫加入后过时的卡牌数量断言后，最终完整运行全部通过。前几项间歇失败的根因没有逐一确定，**不把再次通过描述为已证明所有随机性已消除**；原始失败日志在 evidence ZIP 的 earlier-failures。
- NeoForge 首次独立画面测试在世界生成前触发 watchdog；后来扩大载入等待并从进世界后计时。另一次画面测试中 QA 清理逻辑直接修改正在遍历的实体集合而抛错；改为遍历快照后，两种最终图形运行均完成。旧失败不计入完成截图数。
- 最终 NeoForge 日志仍有可选 Mixin 缺失目标（无 Iron 环境）、共享数据定义调用者提示，以及同装环境上游模板模型提示；没有因此出现本轮测试失败。没有宣称日志完全无警告。
- NeoForge 图形验收使用 ModDev 客户端，不是用户 CurseForge 实例。未替换用户 JAR、未修改用户世界。
- 未验证伙伴将特效移植进 MineClash 后的表现、任意光影/渲染模组、全部自然生成种子、旧世界完整升级、拥挤复杂地形的所有路径和多人网络延迟。

## 如何复现

下载对应 source ZIP，使用 README 指定 JDK。首次需要网络获取构建依赖；`--offline` 仅适用于已经缓存依赖的环境。

```powershell
# Fabric：JDK 21 启动 Gradle，另安装 JDK 17 供客户端/测试运行
.\gradlew.bat build runGametest --console=plain --no-daemon
# 成品 JAR 客户端脚本读取本地合法游戏安装，先调整 INSTALL/JAVA/API 缓存路径
python tools/verify-production.py --witch

# NeoForge：JDK 21
.\gradlew.bat build runGameTestServer --console=plain --no-daemon
.\gradlew.bat runWitchCardVisual --console=plain --no-daemon
# compat-mods 是自行准备的上述五个运行依赖目录；不会打进本模组
.\gradlew.bat runCompatGameTest -PcompatModsDir=/path/to/compat-mods --console=plain --no-daemon
.\gradlew.bat runWitchVisual -PcompatModsDir=/path/to/compat-mods --console=plain --no-daemon
```

图形检查主动创建隔离世界、截图并退出；普通游戏启动不会自动运行。测试 XML：Fabric `run-gametest/build/gametest-results.xml`；NeoForge `run-gametest/gametest-results.xml`、`run-compat-gametest/gametest-results.xml`。

## 成品与资料

| 文件 | SHA-256 |
| --- | --- |
| `royale-spells-fabric-1.20.1-1.9.3.jar` | `340b362b87a63045f3736e4ebfa757e4c027f6b8c18263d08af72fa716b6feec` |
| `royale-spells-neoforge-1.21.1-1.9.3-beta.4.jar` | `34bc12f5df4dcdf2bd2313717105134b1adc50c8a657828c747a349c4a2dd29a` |

发布页附 `royale-spells-1.9.3-validation-evidence.zip`：三份 XML、最终构建和客户端日志、前期失败日志、仅由最终日志确认的 62+20+24 张截图、连续性 CSV 和文件哈希。日志只对本机绝对路径做脱敏，不包含登录令牌、启动参数文件、用户存档或第三方运行 JAR。

[女巫特效接入手册](docs/technical/16-witch-vfx-handoff.md) · [卡牌和 AI 说明](docs/technical/17-witch-cards-and-ai.md)。自编代码 MIT；第三方卡图、音频、参考与改编素材排除，见 LICENSE-NOTICE.md / ASSETS.md。
