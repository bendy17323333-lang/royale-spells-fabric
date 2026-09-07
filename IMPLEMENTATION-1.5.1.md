> **历史档案**：本页保留该开发阶段的数值、计划和发布状态；其中“未发布／不要发布”等文字记录当时状态。当前实现与维护方法请读 [1.5.2-beta.1 技术档案](docs/technical/README.md)，当前发布信息见 [README](README.md)。

# 1.5.1 local changes requested 2026-09-06

Do not publish to GitHub or replace the player instance. Preserve 1.5.0 outputs. No subagents.

- [x] Evolved army cooldown 15 seconds; preserve per-player one-army rule and horn/scroll shared timer. General closer to formation.
- [x] General helmet reveals only the central lower face/jaw. Side cheeks cover both jaw corners and join deeper side/back walls. Reference rechecked in browser: RoyaleAPI evo-skarmy-a-288-6.jpg. Keep Minecraft skeleton basis; inspect front and oblique final-JAR screenshots.
- [x] Freeze lasts 5 seconds at every level. Vines duration unchanged. Shared 15-second player control cooldown across freeze/vines, including scrolls, books, cards and mirrored casts; native UI must show both cooldowns.
- [x] Lightning sound on each actual target strike; one target -> one, three -> three, no fake extra target sounds.
- [x] Summoned small skeleton speed +25%: graveyard, army/general, spectral army, Iron summoned skeleton; avoid cumulative modifier on reload.
- [x] Spell damage does not cause vanilla incidental knockback. Keep only explicitly implemented original spell displacement. Skeleton attacks cause no knockback, including native summoned skeleton projectiles.
- [x] Lethal hit converting an evolved army member to a ghost causes no knockback, including subsequent player attack impulse.
- [x] Freeze vanilla and GeckoLib model animation poses as well as movement/AI, resume after thaw, without affecting other entities sharing a renderer.
- [x] Independent neutral evolved-army spawn egg: 15+1 in a unique non-player faction, no player ownership/cooldown; useful for experiments.
- [x] Player summons prioritize the owner's recent attacked unit, then nearby hostile monsters or enemies actively attacking owner/allies. Do not hunt peaceful/passive or unprovoked neutral creatures. Keep player spells free to damage explicitly aimed passive targets.
- [x] Meaningful server regression tests, actual final-JAR client visual/interaction checks, local JAR/source/evidence package and concise Chinese delivery.

Current source root is this repository. Baseline 1.5.0 = 40 standalone / 91 Iron-compatible GameTests plus packaged client rituals/army/audio, JAR SHA256 5d7cc596b059fcaf68449d6b3783f04870114c72ef0bc87ceec8b8a74dd31e8a.
