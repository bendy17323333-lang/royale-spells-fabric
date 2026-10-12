# Beta 5 觉醒雪球 · 方形 Blockbench 工程

最终方向来自用户明确要求：**保持方形**。白色压实雪块、窄削角、浅积雪层以及半埋紫晶；不用球形多面体或长尖刺轮廓。普通雪球不变。

## 工作源与流程

- `evolved_snowball.bbmodel`：Blockbench 5.2.2 原生 Generic Model 工程，MCP 1.10.0 中实际建模、检查和导出。14 个 mesh、300 个顶点、346 个面，8 张嵌入纯色色板。
- `build-in-blockbench.js`：本次在编辑器里执行的脚本，有 Undo 事务和项目 UUID 检查。并非鼠标逐点手工雕刻；修改已保存工程通常应直接编辑，不必重跑脚本。重新建项目时先将目标 UUID 改为新项目的 UUID，脚本会清空**该目标项目**的元素与色板，务必先备份。
- `export-runtime.py`：只读取已保存工程并转换游戏数据，不另造一套几何。执行 `python art/evolved-snowball/export-runtime.py`，生成 `src/main/resources/assets/royalespells/models/effects/evolved_snowball.json` 和 `export-report.json`。
- `export-report.json`：源文件与运行资源 SHA-256、拓扑数量、闭合检查结果。每条边正好属于两个面；禁止退化面、未应用旋转以及大于四边的面，错误时直接停止。

如手动编辑后出现旋转，请在 Blockbench 应用变换到顶点再导出；保持原生 Y-up、16 单位等于一格。雪部件名称以 `SNOW_` 开头，晶体使用其它名称。8 个材质名称与导出器中的 `COLORS` 对照；新增材质时同时补映射。平面法线用于保留方块感，不使用平滑法线把方块视觉圆化。

游戏 [EvolvedSnowballModel](../../src/main/java/dev/royalespells/client/EvolvedSnowballModel.java) 懒加载一次 JSON，资源重载时清空缓存。三角面补一个重合顶点以适配实体四边形格式；每只雪球每帧 1384 个提交顶点、两个材质批次，不逐帧重建拓扑。着色走标准实体光照和深度，姿态与膨胀按已有施法状态驱动。

## 素材与检查边界

配色/晶块参考资源中既有的《皇室战争》觉醒雪球原卡图。原作角色设计归 Supercell；新建几何、纯色色板、脚本和导出代码属于本项目贡献。游戏运行时引用 Minecraft `snow.png`、`white_concrete.png`，没有将 Mojang 纹理拷进工程或 JAR；不新增 ImageGen 位图。完整权利范围见 [ASSETS.md](../../ASSETS.md)。

已经实际检查 Blockbench 正面、侧面、背面和斜角，以及隔离 Minecraft NeoForge 客户端中的四角度和飞行、落地、翻滚、释放阶段。所有当前截图均来自实际编辑器/游戏，未用离线渲染替代。首次程序球形版本和随后偏圆的 Blockbench 草稿均未作为最终版本交付。详见 [Beta 5 验证](../../VALIDATION-1.9.4-beta.5.md)。
