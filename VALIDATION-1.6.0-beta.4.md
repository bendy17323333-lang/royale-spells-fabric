# 1.6.0-beta.4 本地验证记录

环境：Minecraft 1.21.1、NeoForge 21.1.249、Java 21.0.12；铁魔法组合为 Iron’s Spells ’n Spellbooks 1.21.1-3.16.3 和已有固定依赖。未上传 GitHub、未修改用户 CurseForge 实例和存档。

## 构建与服务器

- `gradlew build --no-daemon` 成功。
- `runGameTestServer`：63 项通过。
- `runCompatGameTest -PcompatModsDir=.../compat-mods`：122 项通过。
- 实际 AI 追击中，16/16 次采样均至少有三名小兵在将军前方；全军虚化后六秒继续造成约 127.8 点伤害。测试靶为 500 生命、无护甲静止目标，不能视为所有实战的保证 DPS。
- 单个虚化小兵从静止前排侧面绕行并攻击；冻结后不继续位移，保存/加载保持槽位与幽灵状态。
- 旧的十五名小兵同 tick 伤害、虚化分散、原生法杖吟唱/取消、铁砧转换、驱散、友军识别和受伤间隔等检查继续通过。
- 电击覆盖第一刻只命中一人、每五 tick 继续、九个唯一目标上限、三格范围与隔墙停止；死亡起点和存档后剩余间隔继续生效。

## 原作与美术

模型经实际 Blockbench MCP 编辑和导出，火与电的法杖同步更新；继续采用已有 ImageGen 原始材质。四张法术图标为原作 302×363 PNG 原字节，来源、哈希见 [SPIRIT-CARD-SOURCES.json](SPIRIT-CARD-SOURCES.json)。电击数量、现行间隔和距离的官方来源见 [技术档案 11.5](docs/technical/11-furnace-staff-and-spirits.md#51-电精灵原作依据)。

## 成品客户端

最终 JAR 在 `run-production-spirit-beta4-verified` 的全新世界运行并正常退出，SHA-256 为 `7c184f3276bf92ab49abb83cd6ccd1632d8638fbcc1b8387ab090d3d42ec6e43`。27 张专题截图和 36 张连锁连续帧均为 1600×1000 原始帧。逐文件哈希、模组组合和检查标记见 [test-results.json](docs/beta4/test-results.json)。

已查看最终实机的全身煤火、低矮紫色冠簇、四张原卡图比例、原生图标外框保留、中文名称和短行说明、蓝白电弧连接相邻目标、将军保持后排及虚化/实体混合近战。另查看四种法杖握持、第三人称、背包、跳扑及夜间冰精灵遮罩，作为资源回归检查。

- 火精灵通过原生法杖实际吟唱、跳扑并命中，100 生命目标剩余 92.3；客户端观察到 10 个同步跳跃 tick。
- 电精灵完整连锁九个不同目标，客户端观察到九个电弧实体；连续帧只来自真实运行时，不是合成示意电击。
- 军团转化部分小兵并将其移到后方后，实际 AI 重新接敌；后续目标生命减少约 156.6。该静止目标场景仅用于回归，不承诺所有敌人/地形中的同等 DPS。
- 方块图集维持 `2048×1024×4`，未出现本模组缺失贴图错误；火焰透明区域未遮挡实体。

本轮第一份 `run-production-spirit-beta4-final` 虽完成战斗流程，但因窗口缩小，截图只有 1×1，未作为视觉证据。后来增加最低分辨率检查；`-display` 一轮发现四种原生法术沿用了不同翻译键，随后修正名称和过长说明，最终在 `-verified` 重跑确认。两个中间版本不作为交付 JAR。

实机示例：[火精灵](docs/beta4/screenshots/spirit-close-fire.png)、[电精灵](docs/beta4/screenshots/spirit-close-electro.png)、[原卡图轮盘](docs/beta4/screenshots/spirit-original-card-wheel.png)、[连锁第六跳](docs/beta4/screenshots/spirit-chain-link-6.png)、[将军后排](docs/beta4/screenshots/spirit-army-general-protected.png)、[混合近战](docs/beta4/screenshots/spirit-army-mixed-melee.png)。

## 验证边界

服务器 GameTest 使用开发运行时；成品客户端单独加载本轮 JAR 与铁魔法依赖。未验证用户完整整合包、多人高延迟、全部复杂地形或光影。没有将编译/自动检查当作用户的美术验收。
