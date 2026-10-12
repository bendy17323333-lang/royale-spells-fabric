# MineClash 圣水死亡动画 · 源码交接工程

面向 **Minecraft 1.21.1 / NeoForge / Java 21**。这是可移植的程序化三维视觉效果：生物原有死亡动作结束后，圣水短促迸开，液丝断为液滴，液滴下落、碰地并留下逐渐消退的残液。

本次按要求只交付源码、实机画面和导入说明，不生成或发布模组 JAR。没有需要导入 GeckoLib 的 `.animation.json`，也不需要 PNG 帧序列；几何与运动由 Java 每帧生成。MineClash 的模型、骨骼、音效和战斗代码继续使用本体。

完整导入教程位于上层技术文档 **第 19 章**。

## 直接导入 MineClash

从 `src/main/java/dev/mineclash/elixirsplash/` 复制六个运行时类，保持包路径：

1. `SplashConfig.java`
2. `client/SplashEffects.java`
3. `client/Splash.java`
4. `client/LiquidMesh.java`
5. `client/SplashRenderer.java`
6. `mixin/DeathPoofMixin.java`

把 [integration/MineClashElixirClient.java](integration/MineClashElixirClient.java) 放入相同包名对应的 `client/` 文件夹，注册客户端 tick 与世界渲染回调。

在 MineClash 主模组构造器注册配置：

```java
container.registerConfig(
    net.neoforged.fml.config.ModConfig.Type.CLIENT,
    dev.mineclash.elixirsplash.SplashConfig.SPEC
);
```

复制 `src/main/resources/elixirsplash.mixins.json`，在 MineClash 的 `META-INF/neoforge.mods.toml` 追加：

```toml
[[mixins]]
config="elixirsplash.mixins.json"
```

`ElixirSplash.java` 是独立预览工程的入口，`VisualTest.java` 是自动检查器。**把特效合并进 MineClash 时不复制这两个文件，不覆盖 MineClash 的 mods.toml，也不要再注册一个 `elixirsplash` 模组。**

## 开发预览

此目录保留 `build.gradle`、`settings.gradle` 和开发入口，方便单独重现。需要本机 Gradle **9.4.1**、JDK **21**。第三方 JAR 不随资料发布；自行将 MineClash `0.7.5-1.21.1` 和 GeckoLib `4.9.2` 放进 `test-mods/`。

```powershell
gradle verifyImport
gradle runVisual
```

第一条只编译源码及导入适配器；第二条启动独立客户端、自动创建一次性世界、击杀测试目标、截取真实游戏帧并退出。两条命令均不执行 `jar`、`build` 或发布任务。日常 MineClash 工程内接入不需要这个预览工程。

## 源码许可和证据

- 自编代码、程序网格及文档：[MIT](LICENSE)。
- 第三方边界与原作参考：[ASSETS.md](ASSETS.md)。
- 实机记录：[验证说明](docs/validation/README.md)。
- 无新增物品、网络包、服务端逻辑、液体方块或存档数据。
