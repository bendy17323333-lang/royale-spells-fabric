# 1.3.0 本地验证记录

验证日期：2026-09-06。未推送或发布 GitHub。

## 最终构建

- 文件：`royale-spells-neoforge-1.21.1-1.3.0.jar`，10,418,403 字节。
- SHA-256：`a2511a24d3ab8db8708cfb1fd32c5c95e1a1fcc3cb9404fda138fa2765da61fc`。
- Java 21.0.12、Minecraft 1.21.1、NeoForge 21.1.249。
- 以 Git 提交 `c969e927c3997b8b9622e821af348bcebf55e213` 为基础，在本地 `iron-integration-1.21.1` 分支开发；原公开 NeoForge 工作树仍干净，未变更其提交。

## 自动化服务器验证

- 无铁魔法：**38 / 38** GameTests 通过。
- 同装完整铁魔法组合：**65 / 65** GameTests 通过。
- 覆盖 29 项原生法术注册、稀有度、学派和随机／指定学派战利品发现。
- 覆盖每个法术的实际卷轴锻造与耗材、抄写与提取、各级奥术铁砧升级、武器附魔、魔力与冷却、取消施法、抗性和伤害事件、召唤事件和召唤伤害装备。
- 覆盖镜像双方 +1、持续施法、完整五次原生连施、费用只收一次、禁止递归叠级、学习条件、命令入口、英雄翻滚连施、原生反制和 1 生命值原生召唤物克隆。
- 旧有地震、范围、电击、冰冻、召唤、伤害与跨模组战斗回归保持通过。

## 实际客户端验证

三个隔离客户端均加载上面同一哈希的最终 JAR，并正常退出：

1. 铁魔法组合 + Fancy：`run-production-compat-system6`。
2. 铁魔法组合 + Fabulous：`run-production-compat-system-fabulous`，配置 `graphicsMode:2`。
3. 无铁魔法：`run-production-visual-standalone-final`，完整旧有模型、特效、火箭飞行与声音检查。

前两次各保存 24 张实机截图。检查正前方及 55°/135° 转向后的落点、滚木与滚桶轨迹、觉醒飞桶双落点、三个领域的展开／活动／结束状态、箭雨覆盖、颈部高度透明层、卷轴锻造台／抄写台／奥术铁砧、装备书的地面预览和真实镜像原生火焰弹。

实际选法术通过客户端原生选择数据包；日志确认 `book=true mirror=true source=irons_spellbooks:firebolt level=3`。GPU 在绘制可见颈部高度领域前后逐像素比较 4096 个深度值，Fancy 和 Fabulous 均不变，玩家头部仍可见。紫色墓园粒子、青色半透明原生克隆均在客户端确认。

截图保留原始游戏输出，没有裁切、重绘或合成。动画截图是对应游戏时间的状态采样，不是视频录像。图库见交付目录的 `预览.html`。

## 构建与归档检查

`gradlew.bat build` 通过；JAR ZIP 完整性、版本元数据、29 张原版卡图与法术书图标逐字节一致、29 个旧配方的同装条件、专用墓园粒子引用均通过检查。JAR 未包含铁魔法、GeckoLib 或 Curios 的类。

未检查所有第三方整合包、光影包、多人公网服务器或全部铁魔法版本。独立 QA 使用新存档，没有替换用户当前游戏实例。

## 复现服务器测试

```powershell
./gradlew.bat runGameTestServer
./gradlew.bat runCompatGameTest '-PcompatModsDir=<包含固定铁魔法及依赖 JAR 的目录>'
```

完整集成组合和使用方式见 `IRON-INTEGRATION.md`；墓园 image-gen 素材的完整提示词和哈希见 `GRAVEYARD-PARTICLE-SOURCE.json`。
