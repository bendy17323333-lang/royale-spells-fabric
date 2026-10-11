# 女巫与觉醒女巫 · 1.9.1 技术增量

目标环境仍为 Minecraft 1.20.1、Fabric 和 Java 17。此版本重做女巫的原生模型与弹体呈现，并将弹速提高 20%；不重新引入铁魔法，不更改卡牌费用、伤害、范围、攻击周期、召唤或治疗机制。完整玩法、配方、命令、首波归魂资格与镜像/克隆边界沿用 [1.9.0 技术说明](WITCH-1.9.0.md)。该历史文档里的旧模型/贴图及每 tick 0.6 格弹速已由本档案替代。

## 原生 Blockbench 制作链

当前可编辑源为 `art/blockbench/witch.bbmodel` 与 `witch_evolution.bbmodel`。几何、层级与 128×128 UV 图集在真实 Blockbench 项目中编辑；新图集通过 Blockbench 的原生 Texture 和编辑器内绘图脚本制作，而非给旧模型继续贴大幅 ImageGen 面部图。普通与觉醒角色的最终轮廓与局部细节需要以本轮真实编辑器和游戏截图为准。

制作来源保留在 `art/witch-remake/`：

- `build-in-blockbench.js`：创建原生部件、层级和像素图集。
- `refine-in-blockbench.js`：轮廓、面部开口、衣料等细化编辑。
- `polish-in-blockbench.js`：最终局部调整。
- `proportion-in-blockbench.js`、`face-opening-in-blockbench.js`、`evolution-in-blockbench.js`：比例、面部开口与觉醒造型编辑。
- `simplify-hands-in-blockbench.js`：依照本轮反馈去掉分开的手指，保留 Minecraft 风格的整体手部。
- `capture-native-witch.js`：从真实编辑器获取最终双模型的正面、侧面、背面和四分之三视角，共八张 `blockbench-final-{witch|witch_evolution}-{front|side|back|three-quarter}.png`。
- `tools/export-native-witch.py`（项目根目录下）：从原生项目导出运行模型与图集，避免另造一套与可编辑源不同的网格。
- `native-export-audit.json`：导出器对模型坐标、层级、UV 与图集的核验记录；它不代替游戏内审图。
- `model-provenance.json`：绑定最终源模型、脚本、运行模型、图集及明确选中的 `blockbench-*.png` 截图哈希。

这些脚本是 Blockbench 制作过程的可读记录；最终 `.bbmodel` 是可继续编辑的模型源。旧版本备份、被否决模型、临时导入数据和 MCP 会话文件不作为新版本素材来源打包。旧 `witch_face.png` 与 ImageGen 提示词属于 1.9.0 历史，不是 1.9.1 的当前面部纹理。

原作卡图、声音、参考角色设计与 MineClash 改编资产仍明确排除于 MIT。来源与许可边界见 [ASSETS.md](../ASSETS.md)，不因本轮重新建模而改变。

## 弹速与帧间连续性

攻击弹体的服务端速度为 `WitchBolt.SPEED = 0.72`，即正常 20 TPS 下每秒 14.4 格。原先为 0.6 格/tick、每秒 12 格。连续射线碰撞仍覆盖每步起点到终点，因此提高速度不依赖离散采样来判断是否穿墙。灵魂按既有 20 tick 抵达，不跟随攻击法球速度一起缩短治疗等待。

旧客户端使用 Entity 默认 `lerpTo`，每收到网络位置就直接写入当前位置；即使渲染器调用了 `getPosition(partial)`，网络包仍可能在任意两个画面之间改变插值端点。新实现先保存网络目标，在客户端 tick 内推进短插值，再由每个渲染帧的 partial 给出显示位置。服务器真实位置、伤害和碰撞不由客户端插值反推。

尾迹缓存改为记录真正显示过的位置，最多保留 96 个帧采样，并裁剪时间和长度；提交 GPU 时重采样成 7 个等弧长节点。相机离开视锥后重新看到法球或发生大幅位置跳转时，清除过期历史，避免把两个不连续位置连接成长条。GPU 仍按原有节点数量采样，没有以成倍添加粒子或提高游戏 FPS 上限冒充流畅度改进。

命中状态首次到达时，以同步年龄建立一次本地时钟起点；之后展开/消散进度由连续渲染时间推进，避免 `服务器整数年龄 + 本地 partial` 在两个不同 tick 边界上产生锯齿变化。爆点继续使用同包同步的 `impactPosition()`。

客户端短插值会落后服务器，直接切换爆点曾在首轮实测中留下最大 1.811 格的跳跃。现从最后一次实际 GPU 提交的核心位置，用单调 Hermite 曲线补完末段，匹配入射方向上的原速度后平缓追上；补程最多占两 tick（正常 20 TPS 为 100 ms），且从上一实际绘制时间起算。核心达到真正碰撞点后才切换命中雾，曲线不越过终点。服务端伤害、音效、真实命中坐标和实体到期时间均不延后；命中时钟在补程期间继续推进，因此没有延长整场效果。离屏超过两 tick 没有可见前帧时不凭空补画旧轨迹。

观察器分别保留服务器是否已命中、可见形态是否已爆开，以及收到命中时最后显示位置与爆点的原始距离。补程帧照常参与飞行阶梯/速度尖峰检查，不从样本里剔除。`maxReceivedImpactGap` 表示缓冲产生的原始差距，`maxFlightToImpactJoin` 表示实际可见阶段切换的帧差；长帧停顿依然原样记录，不把数学补程宣称为解决 FPS 停顿。

体积雾的密度积分、中心/边缘色彩、深度复制、透明状态恢复及距离 LOD 见 [体积渲染说明](WITCH-VOLUME-RENDERING.md)。

## 真实帧观察与打包门槛

`WitchMagicRenderer.visualSample(WitchBolt)` 只在当帧真正调用代理体 GPU 绘制后提供样本，包括实际世界锚点、传入 shader 的时间、命中阶段时间和 frameId。每个世界帧开始清空记录；视锥外、预算丢弃或没有实际提交的实体不会凭空生成“通过”数据。

独立测试客户端在 `WorldRenderEvents.END` 读取该样本并写入 `witch-visual-continuity.csv`，字段为：

```
entity,kind,frame,wallNs,worldTick,partial,x,y,z,shaderTime,impactTime,impacted,serverImpacted,receivedImpactGap
```

连续性门槛包括至少 100 个实际提交样本、5 个独立攻击弹体、40 对有效飞行相邻帧、100 对有效命中相邻帧；不允许时钟倒退、命中阶段倒退、明显静止后大步跳动、速度尖峰或命中时间轴不连续。相邻帧的实际间隔须在 2–45 ms；长停顿和离屏帧间隙会单独排除并报告，不能声称它们不存在。有效飞行帧的静止比例必须低于 25%。这些阈值检查具体断帧模式，**不构成“提升了多少 FPS”的结论，也没有模拟弱网**。

完整图像集合为 62 张：保留五机位的七个攻击阶段、六个角色视角、召唤/归魂/双实体/减速/冻结隔离场景，新增 `face-close`、`profile-and-grip`、`walk-stride-a`、`walk-stride-b`。所有图片必须来自同一次当前 JAR 运行，且有实际渲染记录和截图保存回调。

`tools/package-witch-release.py` 的只读审计与正式打包共用失败门槛：

1. 当前 JAR、资源和构建日志一致，Java 17 字节码与独立 Fabric 依赖正确，45 项服务端 GameTest 全部成功。
2. 当前 JAR 的独立客户端完成 62 张采集、状态隔离检查与保存；没有未允许的错误、shader 加载失败或采集超时。
3. 连续性 CSV 来自该次运行，脚本独立复算数据并与日志计数交叉核对，而非仅搜索 `passed=true`。
4. 原生 `.bbmodel`、128×128 图集、导出/编辑脚本和最终 Blockbench 截图都有匹配的来源哈希。
5. 人工图像审查绑定当前 JAR 和具体图片哈希，并包含面部、握持、两相步态、普通/觉醒轮廓、飞行、命中及遮挡视角。

任何一项缺失都停止打包，不补写虚假的视觉评价或通过记录。编辑器截图不是游戏画面；截图捕获通过也不是全部 62 张均已人工认可。当前发布、安装和最终验证状态由 `BUILD_STATUS.md` 与本版 `docs/validation/verification.json` 给出，不能沿用 1.9.0 的记录。
