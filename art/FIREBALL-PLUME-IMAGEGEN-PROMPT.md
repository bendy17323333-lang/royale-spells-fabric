# 火球尾焰纹理 · 1.7.0-dev.6

- 工具：内置 `image_gen.imagegen`，新图生成。
- 最终纹理：`src/main/resources/assets/royalespells/textures/entity/fireball_plume.png`。
- 用途：火球轨迹上局部火焰团的四帧图集，不是整条尾焰的拉伸贴图。
- 处理：生成 PNG 按原字节复制，保留原始 alpha，没有裁切、重绘或转码。
- 来源：本项目独立生成的火焰视觉，不含 MineClash 模型、卡图或音频；许可范围见根目录 `LICENSE-NOTICE.md`。

## 完整提示词

Use case: stylized-concept. Asset type: game VFX RGBA particle texture atlas for a Minecraft magic fireball, NOT a concept illustration. Create a square transparent PNG containing exactly FOUR filled volumetric combustion billows in a uniform 2 by 2 grid, one sprite centered in each quadrant, with wide transparent gutters so no sprite crosses into another quadrant. Each billow is a dense irregular compact ball of turbulent orange fire with yellow-white hot knots INSIDE the volume, red-orange cooling edges, small jagged licking tips in multiple directions, not a candle flame. All four are slightly different temporal phases of the same billow. The main interior of each sprite is solid filled flame, not transparent: no hollow centers, no rings, no thin outlines, no three long spikes, no flame emoji, no isolated giant teardrop. Use painterly voxel-friendly hand-painted VFX with chunky angular small shapes and dithered/smoky alpha-edge breakup, lively and physical, neither glossy nor flat cartoon clipart. Only short compact flames, aspect ratio approximately 1:1. These sprites will be overlapped as many small 3D billboards to build one continuous thick tail in game, so avoid any baked long trail, no cast shadow, no background glow rectangle, no surrounding scene, no text, no labels, no border, no platform. Genuine transparent background with clean alpha around each fire puff, not black or checkerboard baked in. Restrained bloom on fire only. Four equal cells, consistent scale and center.
