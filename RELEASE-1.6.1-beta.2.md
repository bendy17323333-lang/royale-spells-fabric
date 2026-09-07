# 皇室法术 · 铁魔法适配 Beta 2

**Minecraft 1.21.1 / NeoForge / Java 21 · 1.6.1-beta.2**

本次新增四元素熔炉法杖，并修正觉醒骷髅军团的输出、虚化接敌与将军站位。此前的 1.6.0 Beta 2–4 是本地开发迭代，本版是铁魔法适配的第二个公开 Beta。

- 熔炉法杖可在工作台制作，在奥术铁砧加入火焰弹、浮冰、紫水晶碎片或闪烁西瓜片切换火、冰、电、治疗精灵。使用铁魔法原生魔力、吟唱和共享冷却。
- 火精灵基础伤害 10，其他精灵 7，基础强度可一跳击杀 6 生命小骷髅。修复近距离碰撞漏判及地面起跳提前落地。
- 电精灵最多连锁 9 名不同敌人，每跳间隔 0.25 秒、下一跳范围 3 格。火精灵全身燃烧；电精灵改为三处切角冠簇和延伸闪电胡须。
- 四精灵使用原作卡图及原比例，加入原作分类音效：部署、跳扑、命中和低音量脚步，共 22 个音轨。
- 军团小骷髅的实际近战绕过目标受伤间隔，虚化后继续进攻并尝试绕侧。小兵积极冲锋，将军稍慢跟进、近敌迎战。
- **将军阵亡后，未虚化的小骷髅继续存活和战斗；已经虚化的才消散。**

## 安装

下载 `royale-spells-neoforge-1.21.1-1.6.1-beta.2.jar`，退出游戏、备份旧版后替换，只保留一份皇室法术。`sources.jar`、源码 ZIP 和模型 ZIP 用于开发查阅，不放入 mods。

验证组合：NeoForge 21.1.249、Iron’s Spells ’n Spellbooks 1.21.1-3.16.3、GeckoLib 4.9.2、Curios 9.5.1+1.21.1、playerAnimator 2.0.4+1.21.1、Iron’s Lib 2.1.0。第三方依赖不捆绑。

73 项独立 GameTest、132 项铁魔法兼容 GameTest 全部通过；最终 JAR 完成独立客户端模型/战斗检查，16 种声音进入实际播放通道。未覆盖全部整合包、光影与联机延迟场景。

## 模型出处

**冰精灵模型、皮肤及发光遮罩改编自 [MineClash](https://www.curseforge.com/minecraft/mc-mods/mineclash)，作者 LiziYowo / MineClash 团队，源版本 0.7.5。** 冰法杖使用同一头部；无需安装 MineClash，没有捆绑其代码。感谢原作者的模型工作。

皇室战争角色、原卡图及原作音效归 Supercell。全部素材保留原归属，来源链接、固定版本与哈希见源码中的 SPIRIT-ASSET-SOURCES.json、SPIRIT-AUDIO-SOURCES.json、SPIRIT-CARD-SOURCES.json。

[本版源码与安装说明](https://github.com/bendy17323333-lang/royale-spells-fabric/tree/iron-integration-1.21.1) · [完整技术档案](https://github.com/bendy17323333-lang/royale-spells-fabric/blob/iron-integration-1.21.1/docs/technical/README.md)
