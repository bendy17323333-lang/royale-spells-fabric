# Asset provenance and license boundary

The liquid geometry, trajectories, face shading and ground stain code in this
addon are original procedural work and covered by MIT. No Clash Royale texture,
sound, model, unpacked configuration or MineClash JAR is embedded in this addon.

- [MineClash](https://www.curseforge.com/minecraft/mc-mods/mineclash) supplies the
  creatures and their existing models, death animations and sounds at runtime.
  These belong to their original authors and are not relicensed here.
- Clash Royale and its elixir/death art direction belong to Supercell.
  Publicly mirrored original effect configuration was studied as a reference,
  not copied as a distributable asset. Reference revision:
  `Lee-The-Tree/Cr_Unpacked@59ef870142cef9b9dbf24ee07a05d3fd272b0e01`.
  The mirror is not an official Supercell publication.
- Minecraft and NeoForge supply runtime APIs and rendering. Gradle wrapper
  retains its upstream license. Third-party development dependencies must be
  downloaded separately and are excluded from source/release packages.

The public source package excludes `research/`, `test-mods/`, caches, game
instances and all third-party binaries. This is an unofficial visual addon,
not a Supercell or MineClash official release.

The included unedited Minecraft screenshots contain Minecraft and MineClash
characters and environment assets. They are visual validation references;
third-party elements in those screenshots are expressly excluded from MIT.
