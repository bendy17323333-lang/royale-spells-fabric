// SPDX-License-Identifier: MIT
package dev.royalespells.iron;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/** Loaded only with Iron's Spells. This registers residents, never player spells. */
public final class WitchContent {
    public static EntityType<TowerWitch> WITCH;
    public static EntityType<WitchSkeleton> SKELETON;
    public static void install(IEventBus bus){
        bus.addListener((net.neoforged.neoforge.registries.RegisterEvent event)->{
            var key=event.getRegistryKey();
            if(key.equals(Registries.ENTITY_TYPE)){
                WITCH=Registry.register(BuiltInRegistries.ENTITY_TYPE,RoyaleSpells.id("witch"),EntityType.Builder.of(TowerWitch::new,MobCategory.CREATURE).sized(.65f,1.95f).clientTrackingRange(80).updateInterval(1).build("royalespells:witch"));
                SKELETON=Registry.register(BuiltInRegistries.ENTITY_TYPE,RoyaleSpells.id("witch_skeleton"),EntityType.Builder.of(WitchSkeleton::new,MobCategory.MONSTER).sized(.48f,1.4f).clientTrackingRange(80).updateInterval(1).build("royalespells:witch_skeleton"));
            }
        });
        bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e)->{
            e.put(WITCH,TowerWitch.attributes().build());
            e.put(SKELETON,AllySkeleton.createAttributes().add(Attributes.MAX_HEALTH,3).add(Attributes.ATTACK_DAMAGE,3).build());
        });
        // A required built-in pack exists only in Iron installations. Standalone
        // NeoForge never sees dangling Iron biome/pool/processor references.
        bus.addListener((net.neoforged.neoforge.event.AddPackFindersEvent e)->e.addPackFinders(RoyaleSpells.id("optional/witch_tower"),PackType.SERVER_DATA,Component.literal("Royale Witch Tower"),PackSource.BUILT_IN,true,Pack.Position.TOP));
        bus.addListener((net.neoforged.neoforge.event.RegisterGameTestsEvent e)->e.register(dev.royalespells.test.WitchTowerTests.class));
    }
    private WitchContent(){}
}
