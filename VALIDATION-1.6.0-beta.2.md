# 1.6.0-beta.2 本地验证记录

本版按用户要求不上传 GitHub，未替换实际 CurseForge 游戏实例。Minecraft 1.21.1、NeoForge 21.1.249、Java 21.0.12、Iron’s Spells 1.21.1-3.16.3。

## 构建与机制

- Gradle build 通过，JAR 和 sources JAR 已生成。
- 无铁魔法 GameTests：59/59 通过。
- 同装铁魔法 GameTests：118/118 通过。
- [独立 XML](docs/beta2/standalone-gametest-results.xml)、[同装 XML](docs/beta2/iron-gametest-results.xml)、[汇总](docs/beta2/test-results.json)。
- 军团 15 次同 tick 攻击及转化后的 15 次攻击全部计入伤害；原先已有的目标受伤间隔仍对其它攻击者有效。
- 15 只完全重合的虚化小兵在 90 tick 内分散，同时仍不能被普通击退。
- 12 种跨元素铁砧转换保留名称、损耗和附魔；取出一次只消耗一组材料。同元素不产生结果。
- 真实 RightClickItem → 原生 MagicManager 吟唱 → 召唤；四种物品共享冷却；取消和零魔力不产生召唤。
- 精灵真实寻路跳跃、治疗寻友、隔墙拒绝、九目标唯一电链、一次爆发和 NBT 保存检查通过。

早期寻路测试曾不稳定：测试平台位于模板范围之外，100 个测试 tick 内实体只运行了 11～14 tick，导致超时。已为需要真实寻路的测试显式保持周围区块加载并在结束后释放；保留了失败日志，没有降低伤害或命中断言来消除失败。

## 最终图形验证

使用发布结构的最终 JAR 和五个固定兼容依赖，在 `run-production-spirit-beta2-release` 独立目录运行，未使用用户存档。测试会创建自己的场景、截取四精灵近景、法杖第一/第三人称和物品栏，并通过真实原生施法召唤一只烈焰精灵，捕获同步跳跃姿态及实际目标伤害。运行最终结果见该目录 console.log 与根目录 build-spirit-production-final.log；客户端 marker 和摘录另外写入汇总 JSON。

法杖重做后使用内置 image_gen 生成专属材质图；木杆、收底锅身、卷边、侧环和溢流通过 OBJ 专属 UV 映射，不引用原版方块材质。图片保存与完整提示词见 [来源记录](SPIRIT-ASSET-SOURCES.json) 和 [提示词](art/FURNACE-IMAGEGEN-PROMPT.md)。

该检查确认固定依赖的实际 JAR 启动、模型加载和施法过程，不代表用户已认可审美，也不代替用户整合包、所有光影、多人延迟和长期数值平衡测试。

## 待交付文件

`royale-spells-neoforge-1.21.1-1.6.0-beta.2.jar`，SHA-256：

```text
fb9d6d2425ad4ebd73a6cc6b81cb16eaf62e99fd951ebb951ea3a0ffe452632e
```

[使用与技术说明](docs/technical/11-furnace-staff-and-spirits.md)。新法杖需要铁魔法；安装时保留一份对应版本的皇室法术 JAR，更新前保存退出游戏并备份旧版。
