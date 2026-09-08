# 皇室法术技术档案

本档案面向接手维护、排查问题和添加内容的开发者。它解释代码实际怎样运行、关键设计为什么这样实现，以及修改时需要一起检查哪些位置。

**当前公开 Beta 2：`1.6.1-beta.2`，Minecraft 1.21.1 / NeoForge，铁魔法 1.21.1-3.16.3。**

公开 Beta 1 的代码基线：[d033310](https://github.com/bendy17323333-lang/royale-spells-fabric/tree/d03331030f40f2f46f505f07abf021ad188f4713)。本地 Beta 2 增加熔炉法杖、四种精灵并修复军团攻击和拥挤；[Beta 2 机制验证](../../VALIDATION-1.6.0-beta.2.md) 保留为历史证据。Beta 3 重做模型和材质；Beta 4 增加按原作时序的连锁电击、全身火焰、原卡图 UI 及军团后排/侧翼 AI，见 [第 11 章](11-furnace-staff-and-spirits.md) 和 [历史验证](../../VALIDATION-1.6.0-beta.4.md)。公开 Beta 2 再修复跳扑漏判、将军逃跑、普通小兵随将军消失，提高精灵伤害并接入原作音效；见 [本版验证](../../VALIDATION-1.6.1-beta.2.md)。

## 阅读导航

新增：[11 熔炉法杖与精灵](11-furnace-staff-and-spirits.md)，包括工作台配方、奥术铁砧转换、原生施法链、共享冷却、AI、模型与军团修复。

| 章节 | 内容 | 适合解决的问题 |
| --- | --- | --- |
| [01 架构与执行边界](01-architecture.md) | 注册、两种施法模式、客户端与服务端、模块调用关系 | 从哪里开始读源码？ |
| [02 施法与数值](02-casting-and-balance.md) | 生命周期、30 种法术参数、伤害时序、范围、等级成长 | 修改伤害、范围和持续时间要改哪里？ |
| [03 准星与落点预览](03-target-preview.md) | 视线检测、矩阵、圆形与长条、双落点、深度、历史故障 | 圈为什么会歪、偏移或遮断模型？ |
| [04 特效、模型、界面与音频](04-rendering-and-audio.md) | 渲染阶段、程序几何、冻结骨骼、卡图比例、分阶段声音 | 怎么做新特效并避免渲染回归？ |
| [05 铁魔法系统接入](05-iron-integration.md) | 法术注册、卷轴、抄写、镜像、事件与可选依赖 | 如何保持原生魔力、权限和施法生命周期？ |
| [06 召唤物与战斗](06-summons-and-combat.md) | 目标优先级、击退、军团状态、将军、克隆、小屋 | 友军识别、无敌状态、共享限制怎样工作？ |
| [07 圣水、结构与仪式](07-elixir-structures-rituals.md) | 有限流体、浓缩、卷轴觉醒、塔地下密室、物品转化 | 配方与天然重油来源如何判定？ |
| [08 同步、存档与 Mixin](08-data-and-mixins.md) | 网络消息、实体数据、持久化、所有 Mixin 的职责与风险 | 联机不同步或更新上游后崩溃查哪里？ |
| [09 开发、测试与发布](09-development-and-validation.md) | 环境、构建、隔离测试、证据、发布流程 | 怎样复现、验证并交付一个改动？ |
| [10 扩展与故障排查](10-extension-and-troubleshooting.md) | 新增法术步骤、修改清单、性能限制、已知边界 | 如何扩展而不遗漏预览、音效、配方？ |
| [源码索引](source-index.md) | 全部 Java 文件及资源、工具入口 | 按类名快速定位 |
| [生成的参数参考](spell-reference.md) | 从枚举与资源提取的参数、音效阶段、配方 | 核对当前源码默认值 |

## 建议阅读路线

初次维护先读 01、02、09，再按功能读专题。只研究准星，直接读 03，随后读 04 的渲染状态部分和 08 的客户端同步部分。新增铁魔法法术需要连读 02、05、10；军团或天然重油改动需要同时读 06、07、08。

## 版本和证据的使用规则

- 本档案主要描述 `iron-integration-1.21.1`。`main` 是 Fabric 1.20.1，`mc-1.21.1` 是 Fabric 1.21.1，`neoforge-1.21.1` 是旧版同装分支。三个旧分支不能直接套用本档案的事件类型、注册方法、数值或构建命令。
- “默认值”是源码提供的初始值。铁魔法服务器配置、装备属性、伤害抗性、原版受伤间隔可能改变实战结果。表格里的伤害预算不是对任意目标的实测扣血保证。
- 1 tick 按正常 20 TPS 换算为 0.05 秒；服务器卡顿时不能把 tick 时间视为真实墙钟时间。
- 新档案按当前源码整理。根目录旧文档保留版本历史，不应把旧数值表拼接成当前规则。冲突时先查本档案标明的代码基线，再查正在运行的 JAR 和世界配置。
- 注释、文档、编译成功、GameTest 成功和实机确认是不同证据。本档案会明确说明已实现的机制与尚未覆盖的验证。

## 常用入口

- [模组入口](../../src/main/java/dev/royalespells/RoyaleSpells.java)
- [客户端入口](../../src/main/java/dev/royalespells/client/RoyaleClient.java)
- [独立卡牌与公共战斗工具](../../src/main/java/dev/royalespells/SpellEngine.java)
- [效果实体](../../src/main/java/dev/royalespells/entity/SpellEntity.java)
- [铁魔法法术基类](../../src/main/java/dev/royalespells/iron/RoyaleIronSpell.java)
- [最新发布验证](../../VALIDATION-1.5.2-beta.1.md)

## 维护本档案

修改机制时更新相应专题；修改枚举、音效配置、配方或源码文件清单后执行：

```text
python tools/technical_docs.py --generate
python tools/technical_docs.py --check
```

生成器只维护 `source-index.md` 和 `spell-reference.md`，不会改写人工说明、Java 源码或游戏资源。检查器核对本档案本地链接、必需章节、索引与生成表是否过期；它不代替游戏测试，也不能自动判断全部文字描述是否符合机制。

每次发布在当前版本分支保留该版文档，再更新默认分支的技术档案入口。不要让默认分支首页继续指向已经被替代的“当前说明”。

## 公开源码与素材

本项目自编代码、构建／工具脚本、配置与技术文档采用 [MIT 许可证](../../LICENSE)。第三方卡图、音频、MineClash 原始与改编素材等明确排除，保留原权利人与适用条款；详见 [LICENSE-NOTICE.md](../../LICENSE-NOTICE.md) 和 [ASSETS.md](../../ASSETS.md)。公开素材或项目内使用授权不等于本项目可以把第三方资产重新许可为 MIT。
