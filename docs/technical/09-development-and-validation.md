# 09 开发环境、测试和发布

[返回目录](README.md)

## 1. 固定环境

| 项目 | 当前构建/发布基线 |
| --- | --- |
| Minecraft | 1.21.1 |
| Java toolchain / release | 21 |
| Gradle Wrapper | 9.4.1 |
| NeoForge ModDevGradle | 2.0.146 |
| NeoForge 开发依赖 | 21.1.249 |
| NeoForge 元数据最低声明 | 21.1.200 |
| Iron’s Spells 编译依赖 | 1.21.1-3.16.3，compileOnly |
| GeckoLib 编译依赖 | Modrinth 版本 ID `tPkJmim6` |
| 发布同装验证 | GeckoLib 4.9.2、playerAnimator 2.0.4、Curios 9.5.1、Iron’s Lib 2.1.0 |

版本声明允许范围不等于该范围内所有组合都测过。依赖定义见 [build.gradle](../../build.gradle)、[gradle.properties](../../gradle.properties)、[wrapper](../../gradle/wrapper/gradle-wrapper.properties)、[模组元数据](../../src/main/resources/META-INF/neoforge.mods.toml)。

## 2. 获取源码和构建

```sh
git clone --branch iron-integration-1.21.1 https://github.com/bendy17323333-lang/royale-spells-fabric.git
cd royale-spells-fabric
```

Windows PowerShell：

```powershell
.\gradlew.bat --version
.\gradlew.bat build --console=plain
```

其他平台使用 `./gradlew`。先确认 Gradle 报告的 JVM 和编译 toolchain，不能凭系统默认 `java` 的文件名推断正在使用 Java 21。

产物默认位于 `build/libs/`，前缀 `royale-spells-neoforge-1.21.1`；普通 JAR 用于安装，sources JAR 用于阅读源码。初次构建需要访问 Gradle、NeoForge、Maven Central 与配置的 Modrinth Maven 仓库。

无需重新下载原作素材即可构建，资源已在仓库中。不要把音频导入或旧版资源迁移脚本当成 `build` 的前置步骤。

## 3. 独立与同装运行

```powershell
.\gradlew.bat runClient --console=plain
.\gradlew.bat runServer --console=plain
.\gradlew.bat runGameTestServer --console=plain
.\gradlew.bat runCompatGameTest -PcompatModsDir="D:/MinecraftQA/iron-compat-mods" --console=plain
```

`compatModsDir` 由调用者自行创建，放铁魔法及匹配依赖 JAR；路径只是示例。该参数通过 `runtimeOnly fileTree` 引入目录中所有 JAR，所以应只放本次要测的依赖，避免把整个日常整合包无差别混进测试。

带 `compatModsDir` 的构建会影响该次 Gradle 运行的 runtime classpath，不是只有任务名带 compat 才会生效。独立测试和同装测试必须用不同命令执行，独立那次不带参数。

普通服务端遵守 Minecraft 自身的 EULA 流程。开发运行目录与实际玩家实例不同；不要把任务的 gameDirectory 指向已有生存世界。

## 4. Gradle 已有运行目录

| 任务 | 目录 | 用途 |
| --- | --- | --- |
| runClient | run-client | 普通开发客户端 |
| runServer | run-server | 开发服务器 |
| runGameTestServer | run-gametest | 独立 GameTest |
| runCompatGameTest | run-compat-gametest | 带可选依赖的 GameTest |
| runVisualSmoke | run-visual | 自动创建隔离世界、截图 |
| runCompatVisual | run-compat-visual | 同装视觉检查入口 |
| runShowcase | run-showcase | 自动创建录制地图 |

视觉任务第一次生成 options，降低视距并关闭教程等；已有 options 不覆盖。视觉检查会创建世界、清空测试玩家背包、放置场景并自动退出，它不是仅观察游戏画面的无副作用工具。

## 5. 选择具体视觉检查

`VisualSmoke` 的总开关为 `royalespells.visualSmoke=true`。在本地测试工作树对应 run 配置内加一个 `systemProperty`，例如：

```groovy
// 放在 neoForge.runs.visualSmoke 的配置块内；只用于测试工作树。
systemProperty 'royalespells.targetPreviewSmoke', 'true'
```

随后运行 `runVisualSmoke`。需要铁魔法的检查同时提供 compatModsDir。不要假设给 Gradle 命令随意加 `-D` 就必然把属性传给 Minecraft 子进程；明确配置 run 的 systemProperty。

| 属性 | 检查类 | 重点 |
| --- | --- | --- |
| combatSmoke | CombatClientSmoke | 战斗、头盔、冻结、部署与声音 |
| armySmoke | ArmyClientSmoke | 天然池、仪式、号角和军团 |
| elixirSmoke | ElixirClientSmoke | 材料、流体与结构 |
| polishSmoke | PolishClientSmoke | 卡图 GUI、手持预览、音频和效果 |
| targetPreviewSmoke | TargetPreviewSmoke | 14 个台阶/树冠/透视场景与深度对比 |
| ironSystemSmoke | IronSystemClientSmoke | 原生卷轴、法术书等系统交互 |
| ironSmoke | IronClientSmoke（经既有视觉流程进入） | 同装战斗场景 |

前六项在 `VisualSmoke.tick` 有顺序选择并提前返回，应一次只开启一个专题，不要期待同时开启后全部运行。`buildShowcase` 是另一条入口，会优先于普通 VisualSmoke 安装。

源码中还有旧的 `RevisionVisualSmoke`、`TroopVisualSmoke` 等辅助检查，不是每个类都对应独立 Gradle 任务。

## 6. 发布版已有证据

以下是 **Beta 1 发布时已有结果**，不是此次文档整理重新运行得出的结果：

| 项目 | 原记录 |
| --- | --- |
| 构建 | 通过 |
| 无铁魔法 GameTests | 51/51 |
| 同装 GameTests | 107/107 |
| 最终 JAR 战斗客户端 | 通过对应脚本检查 |
| 最终 JAR 仪式/军团客户端 | 通过对应脚本检查 |
| 音频 | 55 个阶段的网络/客户端 64 格衰减设置检查 |

证据：[VALIDATION-1.5.2-beta.1](../../VALIDATION-1.5.2-beta.1.md)、[test-results.json](../beta1/test-results.json)。该版 JAR SHA-256：

```text
c1a1d9c9486d44074f7ef06db10ba702de1d0b8a42b4f7c4797656ea1bcf772d
```

每个运行中的 GameTest XML 为该配置目录下 `gametest-results.xml`。普通 Gradle `build` 不会自动运行全部图形检查，也不能替代进入真实玩家整合包。

测试类索引包括 `SpellGameTests`、`SpellCoverageTests`、`TroopTests`、`NeoForgeEventTests`、`IronCompatibilityTests`、`IronSpellSystemTests`、`IronBalanceTests`、`ElixirTests`、`ArmyTests`、`Combat151Tests`、`Iron151Tests`、`EvolutionTests`。类名里的历史版本不表示它在当前版已经停用。

## 7. 按改动选择验证

| 改动 | 服务端检查 | 客户端检查 |
| --- | --- | --- |
| 伤害/范围 | 边界内外目标、跳数、受伤间隔、PVP | 可见覆盖与实际范围、目标位移 |
| 准星 | 未改变机制时无需重跑全部战斗测试 | 14 场景、切手、F1、深度、FOV、第三人称 |
| 原生接口 | 抄写/锻造/铁砧/镜像/取消/冷却 | 实际容器、法术选择、消耗与反馈 |
| 军团 | 15+1、重复限制、盾、幽灵、主人、NBT | 头盔正侧面、动画命中时刻、消散 |
| 结构/仪式 | 旋转、clip、来源、堆叠、退款、重载 | 真塔入口、池、转化动画与号角 |
| 音频 | 时序、事件数、衰减配置 | 实际接收、近远过渡与主观音量 |
| 模型 | 装备/实体属性不应意外变更 | 持剑、面部、移动/攻击、受伤/冻结 |
| 文档 | 链接、参数生成和 diff 检查 | 阅读排版；不需要重跑整套 Minecraft |

图像生成结果、模型预览、编译成功都不能单独证明游戏中的武器挂点正确。正式视觉检查应使用准备交付的同一 JAR，记下 SHA-256、分辨率、渲染模式、依赖和观察边界。

## 8. 原始画面与证据管理

截图应保留游戏原始内容，后续制作拼图/视频应明确其为实际截图串联或剪辑。记录一次 draw 前后的深度缓冲只说明该次检查没有写深度，不保证所有 shader/渲染器组合都安全。

已经覆盖的是固定版本组合和隔离客户端。真实双客户端联机、所有第三方光影、长期平衡、任意原生/第三方法术都没有通用覆盖保证。故障报告应附完整版本、复现步骤和日志中的第一个根因，而非只截最末尾报错。

## 9. 录制地图与管理指令

`ShowcaseMap` 保存启用标记，并限制自动构建只在带 buildShowcase 的隔离平坦世界执行。现有 26 个场景不包含治疗与温暖，也不是为后来的军团新增了完整 30 场景地图。场景道具放在快捷栏第 8、9 格，前一场/后一场会重置场景并布置测试对象。

| 指令 | 用途 |
| --- | --- |
| `/royalespells give` | 发放旧卡牌与小屋卡，不等于发全部原生卷轴 |
| `/royalespells give zap` | 指定旧卡牌 |
| `/royalespells refill` | 恢复独立圣水，不清全部原生魔力/CD |
| `/royalespells scene next` / `previous` | 在启用的录制地图切场 |
| `/royalespells clear` | 丢弃当前维度属于自己的 Summoned 与 SpellEntity |
| `/give @s royalespells:neutral_skeleton_army_spawn_egg` | 中立军团实验蛋 |
| `/place structure irons_spellbooks:pyromancer_tower` | 管理员放置原生塔，池仍受生成规则约束 |

`royalespells` 指令根要求权限等级 2。清除指令不等于清所有维度/所有原生召唤管理器，且不会自动重置所有持久化 CD。仅在测试世界执行会建结构或清场的操作。

## 10. 发布流程

1. 确认所在 Minecraft/加载器分支和工作区差异，明确发布对象。
2. 修改版本号、当前说明和变更记录；更新本技术档案及生成参数表。
3. 执行与改动相关的构建、机制测试、图形验证；原始失败记录不能改写成通过。
4. 为最终 JAR 计算 SHA-256，检查版本元数据、Mixin、所需资源；不要把第三方依赖和本机账户/启动参数打包公开。
5. 发布 tag 对应实际源码提交，附 JAR、源码、说明、校验和验证边界。
6. 更新默认分支的下载表和技术文档入口，标明 Beta/预发布，回读线上页面确认。

文档更新本身可以单独提交，不必更改已发布 JAR 和旧校验值。不要为了补说明重新打一个内容不变却版本/哈希不同的模组给玩家。

## 11. 文档生成与检查

```powershell
python tools/technical_docs.py --generate
python tools/technical_docs.py --check
git diff --check
```

脚本只用 Python 标准库。生成表会列出全部源文件、30 个原生 profile、28 个旧法术、实际资源中的声音阶段与配方。`--check` 只检查，不写文件；生成器若无法解析源码结构应报错，而不是静默发布空表。

旧的 `generate-iron-resources.py` 仍带 29 个 profile 的历史断言，部分 update/import 脚本包含旧路径、历史数值或会修改源代码。它们是保留的制作/迁移工具，不是本技术文档生成器；当前版不要按文件名顺序全部运行。
