# 皇室法术 · Royale Spells

把皇室战争的法术带进 **Minecraft Java 1.20.1 / Fabric**。

当前版本 **1.1.2**：28 张法术卡牌与野蛮人小屋，原版卡图、圣水、觉醒、召唤物和独立法术效果。

Clash Royale inspired spells and a Barbarian Hut for Minecraft Java 1.20.1, built with Fabric.

## 下载与安装

到本仓库右侧的 **Releases** 下载最新版：

- `royale-spells-fabric-1.20.1-1.1.2.jar`：模组本体。
- `royale-spells-1.1.2-fabric-1.20.1.zip`：安装包，包含本体、Fabric API、说明与预览。
- `royale-spells-1.1.2-source.zip`：完整源码、资源和可编辑模型。

需要 Minecraft **1.20.1**、Fabric Loader **0.16.14 或兼容的新版本**、对应 1.20.1 的 **Fabric API**，以及 **Java 17 或以上**。

退出游戏，将 JAR 放入对应实例的 `mods` 文件夹。升级时替换旧版，只保留一份本模组；已有兼容 Fabric API 时也只保留一份。联机时服务端和每位玩家都需要安装本模组与 Fabric API。

## 怎么玩

创造模式在 **皇室卡牌** 物品组取卡，手持卡牌瞄准后右键施放。生存模式使用无序配方制作卡牌，卡牌可以反复使用。

圣水上限 10，每两秒恢复 1 点；创造模式免圣水消耗。镜像复制上一张卡，保留觉醒效果，并将伤害、治疗及召唤物属性提高约 10%。

管理员或开启作弊后可以使用：

```mcfunction
/royalespells give
/royalespells give barbarian_hut
/royalespells refill
/royalespells clear
```

全部卡牌、配方和适配数值见 [中文使用说明](README-zh_CN.md)。

## 法术与召唤物

- 包含火球、火箭、Zap、雷电、滚木、墓园、毒药、狂暴、冰冻、克隆、镜像、虚空、地震、藤蔓等 28 张法术卡，包含觉醒与历史／活动变体。
- 卡牌图标使用皇室战争原版卡图；哥布林召唤物改为小僵尸。
- 墓园使用经典原版登场音效，召唤持石剑、6 点生命的原版骷髅；每击基础伤害 1.5，多只骷髅的攻击可分别命中。
- 野蛮人使用定制金发、长胡子模型及原版角色音效；野蛮人小屋定时出兵。
- 克隆体为青色半透明，狂暴呈紫色染色，冰冻有冰壳，藤蔓有立体缠绕模型。
- 地震会分批破坏范围内的木材、木制建筑部件及树叶，保留其它材质。

![原版卡牌在游戏中的展示](docs/images/cards.png)

## 1.1.2 更新

- 火箭头部沿实际弹道转动，上升抬头、顶点转平、下落俯冲；尾焰和烟从尾部喷出。
- 重做野蛮人脸型、眉眼、金发和长马蹄形胡子，调整身体轮廓与服饰，铁剑跟随真实手部动作。
- 脚步只播放原作无喊声版本，降低音量并限制频率；攻击语音仍由实际攻击触发。

![野蛮人游戏内近景](docs/images/barbarian.png)

![火箭三个飞行阶段，画面中从右向左飞行](docs/images/rocket-arc.png)

![野蛮人小屋](docs/images/barbarian-hut.png)

## 从源码构建

构建使用 **JDK 21**，产物兼容 **Java 17**：

```powershell
./gradlew.bat build
./gradlew.bat runGametest
```

Linux / macOS 使用 `./gradlew`。无 `sources` 后缀的安装 JAR 位于 `build/libs`。

`runVisualSmoke` 会启动开发客户端，在工程自己的目录创建新测试世界并截图，完成后自动关闭。需要可用的图形环境；普通游戏不会启用该流程。游戏不会通过这个任务打开已有正式存档。

`art/make-troop-models.py` 是几何生成器，`art/blockbench` 包含可编辑的模型工程。野蛮人网格的顶点配色由游戏渲染器应用，导出工程保留对应元数据。

## 验证与素材

1.1.2 已通过 30 项 Minecraft GameTest，以及独立客户端和整合包副本中的实际游戏检查。详见 [验证记录](docs/validation-1.1.2.md)。

本项目为玩家制作的非官方模组。皇室战争卡图与音效属于原权利人，素材来源、原始哈希和生成提示词见 [ASSETS.md](ASSETS.md)、各项 `*-SOURCES.json` 及 [TROOP-ASSETS.json](TROOP-ASSETS.json)。本仓库尚未为项目代码另行指定开源许可证。
