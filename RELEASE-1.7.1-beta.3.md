Minecraft **1.21.1 / NeoForge / Java 21**。这是铁魔法适配公开 Beta 3，汇总地狱飞龙、电击暂停、雪球和火球视觉更新，并完成卡牌数值重做。

卡牌采用 2026-09-08 数据，以原作小骷髅 81 HP 对应模组 3 HP（1.5 颗心），基础伤害除以 27、范围一格一个方块。例如火球 25.481、大闪每目标 39.148；镜像使用 12 级参数。卡牌冰冻 3.5 秒，铁魔法版仍为 5 秒。普通铁魔法法术和 MineClash 不套用本次换算，觉醒军团按约定统一 3 HP。

火球现在使用连续火焰团拖尾、落地火星和黑烟；雪球减速时生物蓝色、动画减速，投掷朝向不再固定于世界方向。共享动画污染、骷髅独立命中及幽灵拥挤等修复一并包含。

**下载：**

- `royale-spells-neoforge-1.21.1-1.7.1-beta.3.jar`：皇室法术本体。
- `mineclash-zappies-neoforge-1.21.1-0.1.1.jar`：可选电车附属；需要 MineClash 0.7.5 和 GeckoLib。三辆一组、连线电弧、0.5 秒电击暂停，保留雨水自伤。只装本体不会启用这个附属。
- `royale-spells-1.7.1-beta.3-with-zappies.zip`：上面两份 JAR、安装说明、许可证及数值表；第三方依赖另装。
- `royale-spells-1.7.1-beta.3-source.zip`：完整本体源码、可编辑模型、文档和 `addons/mineclash-zappies` 附属源码。不是安装包。
- `CARD-BALANCE.md`：逐卡换算、数据出处、历史活动卡及克隆生命下限等例外。
- `SHA256SUMS.txt`：产物校验。

安装前保存并退出游戏，备份旧 JAR 与存档，同一实例只留一份本体和一份电车附属。无需覆盖正式存档。Iron's Spells 3.16.3、GeckoLib 4.9.2、Curios 9.5.1、Iron's Lib 2.1.0、playerAnimator 2.0.4 为本轮同装验证版本；依赖从各自发布渠道获取。

验证：本体独立 96 项、Iron/MineClash/电车同装 162 项、电车独立 12 项 GameTest 通过。隔离实际客户端确认火球飞行与落地画面、电车连线电弧、46 tick 连续攻击、暂停续接和死亡姿态隔离。截图和逐帧检查不代表所有第三方光影、网络和整合包配置都已覆盖。

**开源范围：自编代码、工具和文档 MIT；《皇室战争》原版卡图／音频、MineClash 原始和改编素材等第三方资产明确排除。** 原作权利归相应作者，不因随源码提供而转为 MIT。MineClash 素材出处：[MineClash / LiziYowo 与团队](https://www.curseforge.com/minecraft/mc-mods/mineclash)。
