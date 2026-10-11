# 素材出处与许可边界 · Fabric 1.9.1

自编代码、脚本、配置和文档使用 MIT；第三方资产不被重新许可。完整法律范围见 [LICENSE-NOTICE.md](LICENSE-NOTICE.md)。以下历史来源清单保留采集时的版本与路径；其中法杖、卷轴图标和液体资源已从本版运行资源移除，不能把历史清单当作当前注册物品表。

| 素材 | 来源与说明 |
| --- | --- |
| 37 张卡牌 | Supercell 原作卡图，来自 [RoyaleAPI/cr-api-assets](https://github.com/RoyaleAPI/cr-api-assets)；按原始 302:363 比例绘制，不裁切或重画；1.9.0 新增普通和觉醒女巫两图 |
| 法术原作音效 | [Henrylq/Clash-Royale-SFX](https://github.com/Henrylq/Clash-Royale-SFX)、RoyaleAPI 的固定版本；部分立体声在历史版本转为单声道用于距离衰减 |
| 冰精灵模型、贴图及发光遮罩 | 用户提供的 MineClash 0.7.5 素材，原作者 LiziYowo / MineClash 团队；[原项目](https://www.curseforge.com/minecraft/mc-mods/mineclash)。原始及改编素材均排除于 MIT，不声称取得公众再许可 |
| 野蛮人、卫队、小屋、火／电／治疗精灵、地狱飞龙 | 本项目几何及专用材质；1.8.0 移植沿用既有资源；原作角色设计权利仍归原权利人 |
| 粒子、火球核心及尾焰、历史专用材质图集 | 历史版本由内置 ImageGen 生成并记录提示词；1.8.0 移植沿用相同字节；1.9.0 女巫新增资产另列 |
| 小骷髅、铁剑、石剑及原版方块材质 | 运行时调用 Minecraft 原版资源，权利归 Mojang/Microsoft；不把其原图复制为本项目原创纹理 |
| 程序化电弧、领域、冰壳、藤蔓、军团头盔与旗帜动画 | 本项目程序实现，代码采用 MIT；不授予原作角色、商标权利 |
| Gradle Wrapper / MixinExtras | 第三方软件保留上游许可；MixinExtras Fabric 0.5.0 嵌入 JAR，其许可证位于嵌套 JAR 的 LICENSE_MixinExtras |

详细记录：`ASSET-SOURCES.json`、`ARMY-ASSET-SOURCES.json`、`AUDIO-SOURCES.json`、`BARBARIAN-AUDIO-SOURCES.json`、`SPELL-AUDIO-SOURCES.json`、`SPIRIT-ASSET-SOURCES.json`、`SPIRIT-AUDIO-SOURCES.json`、`SPIRIT-CARD-SOURCES.json`、`TROOP-ASSETS.json`、`GRAVEYARD-PARTICLE-SOURCE.json`，以及 `art/inferno-dragon/asset-sources.json`。

1.8.0 基线打包的卡图、PNG、OGG、模型逐文件 SHA-256 在 `docs/reference/runtime-assets-1.8.0.json`，它不包含本轮女巫增量；新增原作卡图和音频详见 `WITCH-ASSET-SOURCES.json`。历史文件名变化不影响归属排除。冰精灵当前资源为 `models/troop/spirit_ice.json`、`textures/entity/spirit_ice_mineclash.png` 与 `spirit_ice_mineclash_glowmask.png`。

虚空三次劈击音在既有档案中复用原作 `zap_02.ogg`，不是冒充已找到独立虚空劈击采样；温暖使用已记录的治疗音，派对火箭使用火箭／庆祝音。完整归属及复用说明保留在对应来源 JSON。本版延续既有空间音效衰减，不宣称所有事件都有独占采样。

本项目独立制作／生成且不混入第三方内容的素材，其可由本项目授予的原创贡献同样适用 MIT；不包括第三方原作角色设计、商标和改编资产权利。完整 JAR 不应被称为“所有素材均为 MIT”。

## 1.9.0 女巫新增原作素材

两张卡图直接保存原始 PNG 字节，尺寸均为 302×363。上游为 RoyaleAPI 原作资源镜像，固定版本 `b4530a1043b213ee2baf9c50a3d0d7fae22c2313`：[普通女巫](https://github.com/RoyaleAPI/cr-api-assets/blob/b4530a1043b213ee2baf9c50a3d0d7fae22c2313/cards/witch.png)、[觉醒女巫](https://github.com/RoyaleAPI/cr-api-assets/blob/b4530a1043b213ee2baf9c50a3d0d7fae22c2313/cards/witch-ev1.png)。正式资源名为 `textures/card/witch.png` 和 `textures/card/witch_evolution.png`。

十二份原作 OGG 来自 [Henrylq/Clash-Royale-SFX 的 Witch 目录](https://github.com/Henrylq/Clash-Royale-SFX/tree/c2d7d67271113cb9fe3ad896d9d03dd7f49eed52/Cards/Witch)，固定版本 `c2d7d67271113cb9fe3ad896d9d03dd7f49eed52`。原始录音留存于 `art/witch/original-audio/`；运行资源在 `sounds/witch/`，必要时仅转为单声道 Vorbis，用于三维距离衰减，不改音高、播放速度、剪辑或响度。十二份均经 FFmpeg 完整解码检查。声音事件的衰减距离为 48 格。

| 事件 | 原作文件（省略 .ogg） | 说明 |
| --- | --- | --- |
| witch_deploy | summon_witch_01 | 普通部署 |
| witch_attack | witch_attack_02 | 普通攻击 |
| witch_impact | card_evolution_witch_attack_hit | 普通独立命中音未在该源中找到，明确复用原作觉醒命中音，事件增益 0.55 |
| witch_summon | witch_summon_02 | 普通召唤 |
| witch_heal | card_evolution_witch_healing | 觉醒回血 |
| witch_death | npc_die_02 | 原作共享死亡音，不冒充女巫专属台词 |
| evolved_witch_deploy | card_evolution_witch_deploy_sfx | 觉醒部署音效 |
| evolved_witch_deploy_voice | card_evolution_witch_deploy_vo_01 / 02 | 随机选一条语音，与部署音效分别触发 |
| evolved_witch_attack | card_evolution_witch_attack | 觉醒攻击 |
| evolved_witch_impact | card_evolution_witch_attack_hit | 觉醒命中 |
| evolved_witch_summon | card_evolution_witch_skill_skeleton_01 / 02 | 随机选一条召唤音 |

未找到可靠的独立受伤语音，因此没有用 Minecraft 女巫或僵尸声音冒充。原始与转换后文件的 SHA-256、完整固定 URL、采集日期、声道和时长均在 [WITCH-ASSET-SOURCES.json](WITCH-ASSET-SOURCES.json)；导入步骤见 `tools/import-witch-assets.py`。

**上述卡图、原作声音，以及 `docs/research/witch-*-original-*.jpg`、`witch-original-render.png`、`witch-evo-card.png` 等原作参考图，均为第三方资产，明确排除于 MIT 授权。** 可公开读取上游不等于取得再许可；角色设计、商标和录音权利仍归 Supercell 等原权利人。

女巫几何为本项目新建，没有复制 MineClash 网格或纹理；用户截图用于风格参考。法球、命中烟团和回血效果为世界坐标中的本项目实现，依据 [RoyaleAPI 觉醒女巫实机短片](https://www.youtube.com/watch?v=8Eoq4MF424g) 的外观观察。未分发该视频，也未提取其画面制作贴图。实机参考的观察范围、2026 年玩法重做与尚未量化的特效细节记录在 [研究档案](docs/research/witch-2026-10-10.md)。

**历史 1.9.0 女巫材质**：旧 `witch_materials.png` 和 `witch_face.png` 曾使用内置 ImageGen，参考输入与提示词记录在旧 `art/witch/` 档案。它们不再代表 1.9.1 的当前女巫材质；旧面部生成图不是本轮最终脸部纹理。历史制作记录不用于宣称当前原生模型已通过验收。

## 1.9.1 女巫原生模型与像素图集

普通与觉醒女巫在真实 Blockbench 项目中重新制作，可编辑源为 `art/blockbench/witch.bbmodel`、`art/blockbench/witch_evolution.bbmodel`。当前 `textures/entity/witch_materials.png` 是 **128×128 的新像素图集**，在 Blockbench 原生 Texture 对象中通过编辑器内绘图与像素编辑脚本制作，包含当前面部的像素表现；不采用旧的大幅 ImageGen 面部贴图。角色设计参考仍来自用户提供的 Minecraft 风格女巫图和 Supercell 原角色，不能因此授予第三方角色权利。

制作过程脚本位于 `art/witch-remake/`：`build-in-blockbench.js`、`refine-in-blockbench.js`、`polish-in-blockbench.js`、`proportion-in-blockbench.js`、`face-opening-in-blockbench.js`、`evolution-in-blockbench.js`、`simplify-hands-in-blockbench.js`。原生到运行时的导出器为 `tools/export-native-witch.py`，导出核验记录为 `art/witch-remake/native-export-audit.json`；真实编辑器截取脚本为 `art/witch-remake/capture-native-witch.js`。最终两模型四视角保存在 `art/witch-remake/blockbench-final-*.png`，由 `model-provenance.json` 明确列出并逐文件绑定哈希。

本项目制作的代码、脚本和独立原创贡献适用 MIT；上述角色衍生表现不授予 Supercell 的角色、原卡图、声音或其他第三方资产权利。原生项目截图只能证明相应编辑器中的实际外观，不能替代 Minecraft 渲染验收。最终游戏截图、模型审查和连续性 CSV 必须与当前 JAR 对应，不沿用 1.9.0 的美术结论。

## 1.9.2 共享女巫模型

本轮在现有原生 Blockbench 工程中重塑带厚度的衣身、补齐脖颈并重新分配身高；普通女巫保留腿，觉醒女巫移除腿和靴子并增加中空悬浮袍摆。两端共用 `.bbmodel`、导出网格和 128×128 图集。未新增 ImageGen 位图，沿用 1.9.1 的原生像素图集。动作和左手挂点为本项目代码；角色设计、声音的第三方权利保持原样。当前版本制作及核验记录见 `art/witch-remake/1.9.2/`。
