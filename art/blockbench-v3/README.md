# 四精灵与熔炉法杖：Blockbench 工作源

本目录保存 **1.6.1-beta.2（公开 Beta 2）** 的 8 份可编辑 `.bbmodel`；为保持原有工具路径，目录名仍沿用 blockbench-v3。工程通过实际 Blockbench MCP 创建、转换、预览和导出，纹理内嵌，可直接打开。运行时不需要 Blockbench、MCP 或 MineClash。旧 Beta 3 模型保存在其历史交付压缩包中。

## 当前生效文件

- `spirit_fire.bbmodel`：方形煤块，贴面方眼、短方肢，身体四周与顶部的七组透明动态火焰面、侧面发光煤缝。
- `spirit_ice.bbmodel`：用户提供的 MineClash 冰精灵，经比例和骨骼适配。
- `spirit_electro.bbmodel`：紫色方块主体、贴面像素五官、闪电胡须、三处柔和的切角冠簇、超出脸颊的折线闪电胡须；头顶横电弧已删除。
- `spirit_heal.bbmodel`：金色方块主体、单齿、贴面像素五官和扁平小舌头。
- `furnace_staff_{fire,ice,electro,heal}.bbmodel`：切角方形锅体、木杆、皮革握把、束箍、侧环及对应精灵头。

带 `superseded` 的文件只保留迭代历史，不参与导出。概念图中的火精灵设计已被用户纠正，当前模型以冒火煤块为准。

## 修改和导出

直接在 Blockbench 打开目标工程，编辑后保存到原 `.bbmodel`。模型骨骼名称用于运行时动作，请保留。网格必须是三角面或四边面；单独网格旋转应烘焙为零，旋转轴放在组上。新部件可通过 `EM_` 前缀指定发光，火焰动态面使用 `FLAME_`。

在仓库根目录运行：

```powershell
python art/blockbench-v3/export-runtime.py
.\gradlew.bat build --no-daemon
```

导出器读取保存的实际模型，产生四份实体 JSON、四份 OBJ 和 MTL；不会重新生成另一套几何。逐面 UV 根据每张纹理各自的宽高归一化，兼容 1254px 生成图集与 128px MineClash 原图。跳过零面积面，避免平面方块的退化面产生非法法线。

导出器不生成法术图标。四张精灵图标由 `tools/import-spirit-cards.py` 导入原作 302×363 卡图，不能再用方形模型截图覆盖。`fire-beta4-preview.png`、`electro-beta4-preview.png` 是本轮 Blockbench 预览，其余旧预览只代表当时版本；以模型文件及同版实机截图为准。

`build-models.js` 保留本次在 Blockbench 内构建几何的脚本。冰精灵的正确流程是导入原始 GEO 和 PNG，再运行 `import-mineclash-ice.js`；冰杖在生成锅体后运行 `replace-staff-ice.js`。这些是编辑器重建脚本，不是 Minecraft 启动代码。实际修改后以保存的工程为准。

## 坐标、材质与动画

Blockbench 使用 Y 向上、-Z 为正面，实体运行时使用 Y 向下的 24px 模型基准。导出器翻转 Y、面顺序和相应的骨骼休止角；骨骼原点从编辑器绝对坐标转换成父组相对坐标。OBJ 仍使用 Y 向上，并将像素除以 16 得到方块单位。

`spirit_materials.png` 为内置 ImageGen 生成的大色块 4×4 材质表。`spirit_flame.png` 为 2×2、四帧透明火焰。实体通过每两 tick 切换 UV 四分区播放；法杖使用原生 sprite 动画，上传时生成与 `.mcmeta` 对应的四帧元数据。同一纹理不能在这两个加载路径上使用相同 UV 尺度。冰精灵保持原始纹理与发光遮罩，不进行重绘。

普通模型、局部发光、火焰分开提交。冰精灵的遮罩使用独立纹理，不把整个主体设为满亮。其它召唤物的旧材质图和没有逐面 UV 的几何继续使用旧渲染路径。

四张法术图标已换成皇室战争原卡图。`previews/` 中保留 Beta 3 的编辑器渲染历史，不应拿来覆盖原作卡牌图片。

最终素材提示词见 [PIXEL-FINAL-PROMPTS.md](PIXEL-FINAL-PROMPTS.md)，旧稿提示词见 [IMAGEGEN-PROMPTS.md](IMAGEGEN-PROMPTS.md)，外部模型对照见 [MODEL-STUDY.md](../mineclash-reference/MODEL-STUDY.md)，本版实际游戏验证见 [VALIDATION-1.6.1-beta.2.md](../../VALIDATION-1.6.1-beta.2.md)。旧本地 Beta 3/4 的截图与验证保留作历史记录。

物品图集加载时由 `FurnaceSpriteSource` 生成 1024px 上传副本，以保留正常 mipmap；原始 PNG、Blockbench 工程纹理和实体纹理不变。

冰精灵原模型与贴图来源：[MineClash](https://www.curseforge.com/minecraft/mc-mods/mineclash)，作者 LiziYowo。公开 Beta 2 电精灵前视图为 `electro-beta2-front.png`。
