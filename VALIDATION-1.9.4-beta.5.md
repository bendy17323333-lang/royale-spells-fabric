# 1.9.4-beta.5 验证记录

Minecraft **1.21.1 / NeoForge 21.1.249 / Java 21.0.12**。本轮只更新 NeoForge，Fabric 已发布的 1.9.3 保持原样。验证在工作区新建的隔离世界中进行，不安装到用户 CurseForge 实例、不修改用户存档。

## 机制与兼容检查

| 环境 | 最终结果 | 测试范围 |
| --- | --- | --- |
| 本体独立，无 Iron / MineClash | **126/126 GameTest** | 等级施法、女巫与护卫、原有法术回归、命令权限、可选依赖隔离 |
| 本体 + MineClash 0.7.5 + GeckoLib 4.9.2，无 Iron | **126/126 GameTest** | 上述内容，以及 15 种卡面包装与原生蛋的实际生成对照 |
| 本体 + MineClash + Iron's Spells 1.21.1-3.16.3 | **201/201 GameTest** | 上述内容及 Iron 法术隔离、塔与驻军、卷轴等既有接入回归 |

Iron 同装还包含 GeckoLib 4.9.2、Iron's Lib 2.1.0、playerAnimator 2.0.4、Curios 9.5.1。**没有加入电车附属**，不能把本轮数据理解为附属所有机制的验收。

实际运行验证了：所有 32 张独立卡的等级组件保存，1/11/16 级施法伤害与半径边界，真实右键卡牌、镜像内部 17 级、护盾/治疗/子兵等级传递，创造/生存/管理员命令权限。MineClash 包装通过原生蛋路径生成，比较实体类和基础属性；无 Royale 等级、圣水或玩家归属注入。

AI 检查涵盖母体死亡/卸载后继续寻路和攻击、塔巡逻存活、近战就近、远程保持首次目标、视线与玩家命令优先；保留被否定方案的失败回归记录。女巫原有移动、射程、攻击间隔和施法动作未重写。

虚空测试在各脉冲间切换空场、单体、群体，确保空场没有劈击声音、单体有独立事件、群体用原事件。新的音频由已有原录音按查得的上游配置层次混合；原录音及混音不适用 MIT。

## 方形觉醒雪球：编辑器和实际游戏

使用 **Blockbench 5.2.2 / MCP 1.10.0** 的实际桌面编辑器。脚本在独立 Generic Model 项目中创建方形几何，通过原生 `project` codec 导出 `.bbmodel`，在独立视口检查正面、侧面、背面和斜角。不是只把磁盘生成文件冠以 Blockbench 名称，也不是声称手工鼠标逐点雕刻。

`art/evolved-snowball/export-runtime.py` 检查 14 个闭合网格、300 个共享顶点、346 个面，直接将原生文件转换成游戏资源。最终保持方形、窄削角、浅积雪层和半埋紫晶；此前圆形多面体草稿未发布。`export-report.json` 记录源与运行资源哈希。

**实际 ModDev 图形客户端**加载 MineClash + GeckoLib，在无额外光影的全新平坦世界检查：

- G 键打开等级菜单、选 16 级并提交；集成服务端读回 16。
- 两张创造页截图，检查卡面比例和 MineClash 包装显示。
- 雪球四角度，以及飞行、下降、落地、翻滚、释放前、释放后：共 **13 张本轮截图**。
- 实际客户端声音监听：单体劈击 3 次、群体 3 次、空场劈击 0 次。
- Minecraft 自带 JOrbis 解码新 OGG：48 kHz 单声道，读取 267136 字节 PCM。

最终日志 `BETA5_VISUAL_PASS`，686 个模型渲染帧计数；飞行/下降/落地/滚动/释放前的样本来自同一个实际发射实体，年龄分别为 6/15/24/31/38 tick。当前截图目视检查中，方形轮廓、晶体遮挡和各面明暗正常，没有看到透明破洞；释放后模型消失，单位保留蓝色寒冷状态。截图和日志保存于发布的验证包。截图数量和渲染帧计数**不是 FPS 基准**，声音解码与事件次数**不是人工耳听混音验收**。

## 可复现命令

游戏与构建均使用 Java 21；Gradle 9.4.1。可选测试目录仅在本地准备，不随源码包分发第三方 JAR。

```text
python art/evolved-snowball/export-runtime.py
python tools/technical_docs.py --generate
python tools/technical_docs.py --check
gradlew.bat runGameTestServer --offline --console=plain
gradlew.bat runCompatGameTest -PcompatModsDir=test-deps-mineclash --offline --console=plain
gradlew.bat runCompatGameTest -PcompatModsDir=test-deps-all --offline --console=plain
gradlew.bat runBeta5Visual -PcompatModsDir=test-deps-mineclash --offline --console=plain
gradlew.bat build --offline --console=plain
```

`Beta5VisualSmoke` 只有指定任务显式启用，普通游戏启动不运行；会新建一次性 QA 世界并自动保存退出。`BETA5_VISUAL_PASS` 才代表所列阶段完成，**Gradle BUILD SUCCESSFUL 本身不足以证明视觉检查通过**。

## 迭代失败与范围限制

- 先前 AI 版本在回归环境重现过 5 个目标/护卫断言失败，原始失败记录保留于验证资料中。
- 首轮等级测试因测试仍引用旧的 12 级取整表失败，改为本版明确的连续 `1.1^(L−11)` 规则后通过。
- MineClash-only 首轮护卫站位用第 100 tick 单一快照判断“曾移到前方”，后续闲置活动会影响终点。改成每 5 tick 观察实际越过前方的位置，同时仍验证不伤害牛，最终通过；不把缺少位置诊断的第一次失败宣称为已证实的引擎 bug。
- 原程序球形外观被用户否定，按最新要求在 Blockbench 重做为方形。图形检查曾因 24–25 tick 的窄截帧窗口错过落地；保留该 `VISUAL_FAIL` 日志。扩窗后发现静态模型预览也会落进同一窗口，因此将样本绑定到实际发射实体 ID，再运行得到上述最终结果；早期误纳预览的日志不作为验收结果。
- 本轮没有实测任意用户整合包、长期多人服务器、网络延迟、第三方光影或跨版本世界迁移；没有替用户安装。独立服机制测试与开发客户端画面检查不等于完整生产整合包验收。

发布 JAR、完整当前源码、验证包、女巫 VFX 源码交接包均附 SHA-256。MIT 只覆盖本项目自编代码及许可范围内贡献；第三方资产边界见 [ASSETS.md](ASSETS.md) 和 [LICENSE-NOTICE.md](LICENSE-NOTICE.md)。
