# 1.5.0 local implementation

User-approved: one evolved scroll + one goat horn in a natural dark pool consumes the scroll and enchants the horn. One army per player across scroll/horn; shared cooldown; horn consumes no mana. Do not publish to GitHub.

- [x] Lower new natural dark pools eight blocks into a connected crypt. Preserve original containers, chunk clipping and four structure rotations.
- [x] Natural-source provenance, finite extraction and a persisted item ritual with tested interruption refunds.
- [x] Native Iron evolved army scroll, spellbook/scroll/horn lifecycle and persisted shared army/cooldown.
- [x] General plus 15 skeletons, shield and ghost transformation; owner, general death/removal, unload and expiry handling. Uses the existing Iron summon/dispel hook; army-specific dispel gameplay is not separately certified.
- [x] Apply the user's final model direction: actual Minecraft SkeletonModel and skeleton texture, vanilla held-item placement, cuboid helmet/shield/flag, eye glow and a synchronized staff attack. Rejected rounded skeleton mesh resources removed.
- [x] Original army horn and skeleton cues; 64-block spell broadcast and attenuation, 0.9 nearby gain multiplier, three independent Void strike cues at ticks 16/40/64.
- [x] 40 standalone and 91 Iron-compatible GameTests; isolated packaged-client ritual, horn, model, attack and distant-sound checks. Evidence and boundaries in VALIDATION-1.5.0.md.

References verified 2026-09-06:
- https://supercell.com/en/games/clashroyale/blog/release-notes/october-update-2025/
- https://royaleapi.com/blog/skeleton-army-evolution-2025-october (models viewed in browser, 15+1 composition, shield and spectral army)
- Original game sound archive pinned by existing importer: Henrylq/Clash-Royale-SFX, c2d7d67271113cb9fe3ad896d9d03dd7f49eed52

Current Iron 3.16.3 calls its native skeleton/zombie summoning spell `irons_spellbooks:raise_dead`. Accept that and our `royalespells:graveyard` at any level as ritual inputs. Replace only summoned skeleton renderers, preserving wild skeletons and native equipment/AI.

Prior deliverable 1.4.0 source ZIP and JAR remain untouched in outputs/royale-spells-1.4.0-iron-local.
