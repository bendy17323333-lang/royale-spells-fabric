# Fabric 1.9.2 验证记录

> 当前发布 1.9.3 的完整结果见 [VALIDATION-1.9.3.md](VALIDATION-1.9.3.md)。以下保留 1.9.2 历史记录。

本轮模型与左手施法改动见 [技术说明](docs/WITCH-1.9.2.md)。此前记录保存在 `docs/validation/BUILD_STATUS-1.9.1.md`，不作为本版实机证据。

## 构建和机制

- JDK 21 构建、Minecraft / JDK 17 实际运行，正式 remapped JAR 1.9.2。
- 45 项 GameTest 全部通过，见 `build-witch-192-final.log`、`run-gametest/build/gametest-results.xml`。
- 首次运行出现既有雪球伤害测试的单次失败；没有修改雪球实现或降低断言，相同测试重跑和最终回归均为 45/45。该情况保留记录，不声称测试从未出现波动。
- 原生导出器 `--check` 与 `--self-test` 通过；普通版 17 骨骼 / 92 网格 / 1149 面，觉醒版 18 / 107 / 1306。两端同源文件 SHA-256 相同。

## 当前正式 JAR 的实际游戏

隔离实例：Minecraft 1.20.1、Fabric Loader 0.16.14、Fabric API 0.92.6+1.20.1，1280×800，Fancy，无外部光影、无 Bloom。成品 JAR 安装在 `run-production-witch/mods`，新建一次性 QA 世界，最终正常保存退出；没有使用用户存档。

本次原始运行曾受文件沙盒本机回环网络限制，未进入世界；获得所需运行权限后重新启动并完成。未把那次失败计入成功截图。

- 11 个女巫音效事件的中英文字幕补齐，当前日志无女巫字幕缺失。已有自定义实体的 DataFixer 提示仍存在，不能描述为零日志警告。
- 62 张当次真实 framebuffer 截图，核验文件时间与实际保存回调。
- 1,887 个实际提交渲染的样本：位置突跳 0、时间倒退 0、相位倒退 0、影响时钟不连续 0、非有限值 0。
- 冰冻保持单个实体同一姿态，另一女巫仍正常运动；雪球动作速率实测 0.65，解除后回到 1.0。
- 检出并修复首次部署后原版身体朝向滞后导致的出弹错位。只在施法渲染时对齐躯干与实际出弹方向，结束后还原实体数据。最终 100 个释放帧的左掌锚点与服务端发射点最大偏差 0.04447 格（含浮动与收手的帧插值）。
- 直接查看了普通/觉醒正、侧、背、45 度，握杖、释放、飞行及命中截图。脖颈连接可见，觉醒没有靴子或腿，右手持杖，左掌对准弹体。特效保持亮芯暗雾、实体遮挡与三维形体。

实测 JAR SHA-256：`430bb215fd8c5b3b836a0300edc489f03a746b315c41c0fd5933e72c2671f683`。打包核验会再次逐字节核对实测副本。

## 限制

该验证覆盖隔离单机、指定相机和控制状态，不代表所有整合包、光影、多人网络或复杂地形均通过。美术可辨识度经过实际图像审查，不能用测试计数代替用户对最终外观的评价。历史未采用截图不作为本轮验收证据。

```text
WITCH_QA_VISUAL_CONTINUITY passed=true samples=1887 flightSubjects=45 flightPairs=635 stationaryPairs=66 stationaryRatio=0.10393700787401575 stairEvents=0 positionSpikes=0 clockBackwards=0 phaseBackwards=0 impactPairs=856 impactClockDiscontinuities=0 nonFinite=0 excludedFrameGaps=142 maxFlightStep=0.7225914653650571 maxStepPerVisualTick=1.0567321880545895 maxFlightToImpactJoin=2.056619392351395 completionSamples=119 maxReceivedImpactGap=2.056619392351395 serverSpeed=0.72 csv=witch-visual-continuity.csv scope=actual-rendered-frames notNetworkImpairmentTest=true visualQualityRequiresHumanReview=true
WITCH_CLIENT_QA_COMPLETE frames=62 renderedFrames=4225 controlsPassed=true missing=[] world=Witch-1.9.2-QA-remake-1791701450207
```
