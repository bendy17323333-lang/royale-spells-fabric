# 最终方块风格 ImageGen 提示词

工具：内置 `image_gen.imagegen`。均为新建位图，原始 PNG 直接复制，未用程序重新绘制素材。

## 大色块材质表

原始生成文件：`exec-bd642dfc-f2df-40eb-9320-4fb61b1034f5.png`。

```text
Create a usable 4 by 4 material texture atlas for a Minecraft Java low-poly Blockbench mod. This is a square flat bitmap of 16 seamless square swatches, each exactly one quarter width and height, no gaps borders labels text icons scenes objects or perspective. CRITICAL STYLE: authentic coarse Minecraft pixel art. Treat EACH SWATCH as only a 16 by 16 logical pixel grid, sharply nearest-neighbor enlarged. Very large visible square pixel clusters, only 4-6 flat colors per swatch. No brush strokes, fine noise, smooth gradients, realistic grain, photographic highlights, or tiny details. Match muted Iron's Spells medieval fantasy equipment. Layout row major exactly: Row1 col1 charcoal nearly black with restrained warm orange pixel fissures occupying <6 percent area; col2 snowy ivory with pale ice blue block shadows; col3 muted saturated violet elemental skin with broad lavender patches; col4 warm honey yellow elemental skin with 4 gold shades. Row2 col1 glowing molten orange with large golden pixel islands; col2 glacier azure crystals in blue and cyan angular clusters; col3 pale electric cyan mostly very light flat blue with simple brighter clusters; col4 warm coral red tongue with subtle block shading. Row3 col1 dark blue grey forged iron in broad horizontal shades NOT wood NOT stripes bands rivets objects; col2 cool pale grey steel as plain metal sheet with block highlights NOT bands or rivets; col3 dark brown wooden shaft grain, several broad vertical brown pixel runs, no knots or bars; col4 reddish dark brown leather plain large patches no stitching. Row4 col1 very dark indigo near black mouth cavity; col2 pale ivory bone in 4 offwhite shades; col3 almost uniform warm white light surface, brightest center and pale cream corners, no circular object or concentric rings; col4 muted gold brass metal in 4 shades. All swatches are flat 2D materials not depictions of objects. Exactly aligned quadrant boundaries at 25,50,75 percent. No antialiasing. Opaque square atlas.
```

## 四帧像素火焰

原始生成文件：`exec-98352c00-1a0e-46a7-8e26-23e5557abdf3.png`。

```text
Game asset sprite sheet: four frames of a small bright orange-yellow flame in a precisely aligned 2x2 grid, square canvas, true alpha transparent background. Each quarter contains one whole isolated flame with no clipping or overlap. Top left=frame0, top right=frame1, bottom left=frame2, bottom right=frame3. Flame anchored at exact bottom center of each cell, occupies 90 percent cell width and 86 percent cell height. Minecraft pixel art aesthetic with crisp big rectangular pixel clusters and stepped silhouettes. Each flame should resemble a native 16x16 pixel Minecraft fire sprite enlarged with nearest neighbor: tiny vocabulary of six flat colours dark orange, vivid orange, yellow orange, yellow, pale yellow, ivory. ZERO curved edges, ZERO airbrushing, ZERO fine noise, ZERO gradients, ZERO semitransparent halo. Outer edge makes broad stair-step tongues, three main tongues waving left and right between the four frames, short fiery curls made only of square pixels. Pleasant magical coal-spirit fire, dynamic four frames with coherent center and size. Strict regular square pixel grid, clear hard opaque coloured pixels inside silhouette and fully transparent outside it. No objects, no charcoal body, no text, no outlines, no cast shadow, no sheet grid or panel borders.
```

## 最终几何约束

用户要求三只精灵均按照 MineClash 冰精灵的方块标准重做。最终火、电、治疗精灵均为 9px 方块主体、短方肢、贴面像素眼睛与表情；不再使用先前圆滚主体、圆形眼窝或深空嘴。火精灵为冒火煤块，电精灵保留闪电胡须，治疗精灵保留单齿和扁平小舌头。法杖使用切角方形深锅、方木杆、金属箍及像素材质。旧圆形工程归档于 `superseded-rounded`，不打入交付包。
