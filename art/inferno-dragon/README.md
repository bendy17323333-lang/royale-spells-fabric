# 地狱飞龙 V2 · 可编辑模型

与本地 `1.7.0-dev.2` 游戏模型对应。几何在 Blockbench 中制作，通过 MCP 检查、导出；没有导入 MineClash 的飞龙网格。MineClash 用于整体美术风格参考，角色设计参考用户提供的皇室战争原图。

## 文件

| 文件 | 用途 |
| --- | --- |
| [inferno_dragon.bbmodel](inferno_dragon.bbmodel) | 最终模型源，含贴图、骨骼、悬停与蓄能动画 |
| [inferno_dragon.gltf](inferno_dragon.gltf) | 通用网格导出，包含贴图和动画 |
| [inferno-dragon-viewer.html](inferno-dragon-viewer.html) | 离线浏览器预览；拖动旋转、滚轮缩放、振翅/张嘴切换 |
| [inferno-materials.png](inferno-materials.png) | ImageGen 原始材质图集，未修改位图 |
| [texture-prompt.txt](texture-prompt.txt) | 图集生成提示词 |
| [build-model.js](build-model.js) | 初始几何构造器，运行前需要核对目标工程 UUID；不要覆盖手工修改后的源模型 |
| [export-runtime.py](export-runtime.py) | 从最终 .bbmodel 导出游戏 TroopModel 网格与 UV |
| [make-viewer.py](make-viewer.py) | 从同一源模型生成离线预览和几何检查报告 |
| [asset-sources.json](asset-sources.json) | 原作卡图、三段声音的固定来源和原始/导入后哈希 |
| [export-report.json](export-report.json) | 源模型与游戏网格 SHA-256 对应关系 |

最终 .bbmodel 是模型权威来源。旧版 V1 工程保留在原目录，修改 V2 不会自动更新 V1。`refine-v2.py` 是从 V1 迁移时使用的本机历史脚本，依赖当时目录，不是重建或导出的必需步骤。

## 姿态与动画

身体前倾 35°，头部相对身体反向抬起 22°。翅根固定在身体两侧，静态挂点补偿身体倾斜；翅膀围绕局部 Z 轴镜像振动，保持上下扑翼。蓝色上表面与红褐色下表面是分开的薄网格。

一根黄铜管从罐顶中心通向头盔顶部右侧。盔体薄且贴头，外挑面罩为独立薄片，有真实开孔、斜面和加厚边缘；下颌护甲为分片带齿轮廓。眼睛贴在连续的浅凸眉眼区，未创建独立突出的眼球。

模型有 15 个骨骼、177 个网格、1,277 个面，通用预览三角化后为 2,280 个三角形。材质图集 1,254×1,254；游戏按原字节使用，UV 对应各材质区。

修改完先保存 .bbmodel。在仓库根目录执行：

```text
python art/inferno-dragon/export-runtime.py
python art/inferno-dragon/make-viewer.py
```

GLTF 需要在 Blockbench 再次导出。游戏运行时会读取网格静态姿态，再由 `TroopModel` 驱动翼、下颌与尾部；光束、飞行路径和音效不包含在 GLTF 内。修改头颈姿态后必须同步检查口部光束定位，详见 [技术说明](../../docs/technical/12-inferno-dragon.md)。

`preview-dev2.png` 为当前薄盔体的 Blockbench 实际截图；`front-v2.png`、`wings-down.png` 保留为上一开发版历史截图。交付目录另有独立 Minecraft 成品客户端截图；浏览器预览不能代替游戏实机验证。

## 素材归属

几何为本项目新作；材质由内置 ImageGen 生成。皇室战争角色、原卡图及原声音归 Supercell。卡图来自固定版本 RoyaleAPI 资源档案；部署、振翅、喷射来自固定版本 Henrylq 原作音效分类。三段音频原本均为单声道，保留原始字节；运行时音量与喷射音高随距离/热量变化。

素材出处、固定 URL 与哈希见上表，全部素材的既有说明见 [ASSETS.md](../../ASSETS.md)。
