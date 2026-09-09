# 构建与复测

需要 Java 21；Gradle Wrapper 为 9.4.1，NeoForge ModDev 2.0.146，开发 NeoForge 21.1.249。

将合法取得的本体放在 `libs/mineclash-0.7.5-1.21.1.jar`、`libs/geckolib-neoforge-1.21.1-4.9.2.jar`；Gradle 从公开 Modrinth Maven 下载 Iron's Spells `1.21.1-3.16.3` 作为可选兼容代码的**编译依赖**。运行本附属不要求安装铁魔法。

```powershell
./gradlew.bat build
./gradlew.bat runGameTestServer
```

也可用 `-PmineclashJar=完整路径`、`-PgeckolibJar=完整路径` 覆盖 `libs` 位置。`-PcompatModsDir=目录` 会额外把目录中的 JAR 加入开发运行环境，勿重复放已作为 implementation 的本体或 GeckoLib。

12 项 GameTest 覆盖等级换算、三车体积和存档、部署原子性、同 tick 三次伤害、不附带击退、射程/视线/友军、精确周期、实际路径移动、三车均到达射程、原 MineClash 抬手暂停续接、雨水、水中自伤。测试里的 `spawnWithNoFreeWill` 会删掉 Goal，不能把它当作“暂停 AI 后还能直接恢复”使用；真实导航测试必须用普通 spawn。

客户端诊断默认关闭。仅在专门的测试实例加入以下 JVM 参数，才会自动创建测试世界、生成实体、记录截图并退出：

```text
-Dzappiesaddon.smoke=true
-Dzappiesaddon.electricalProbe=true
```

**不要把这两个测试参数加到自己的正式实例。** 它们用于全新的隔离客户端，而非对现有世界执行验证。

`tools/verify-client.py <测试目录>` 对实际逐帧日志和服务器射击计数做断言。`sources.jar` 只是 IDE 源码附件；完整 source ZIP 才包括 Wrapper、构建说明和技术档案。研究目录、原模组反编译代码、依赖 JAR、缓存、游戏存档和用户日志不在源码包中。
