package dev.royalespells.test;

import dev.royalespells.*;
import dev.royalespells.entity.AllyZombie;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder("royalespells")
@PrefixGameTestTemplate(false)
public class NeoForgeEventTests {
    @GameTest(template="empty",batch="neo-void-audio")
    public void voidEmitsOneImpactSoundAtEachOfItsThreeDamageTicks(GameTestHelper c){
        var w=c.getLevel();var at=Vec3.atCenterOf(c.absolutePos(new BlockPos(2,12,2)));var fx=dev.royalespells.entity.SpellEntity.create(w,Spell.VOID,UUID.randomUUID(),at,at);var ticks=new java.util.ArrayList<Integer>();
        Consumer<net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition> listener=e->{if(e.getLevel()==w&&e.getSound()!=null&&e.getSound().value().getLocation().equals(RoyaleSpells.id("spell_void_strike"))){ticks.add(fx.time());c.assertTrue(Math.abs(e.getNewVolume()-.63f)<.001,"Only a ten percent reduction from the original .7 impact mix");}};
        NeoForge.EVENT_BUS.addListener(listener);try{for(int i=0;i<70;i++)fx.tick();c.assertTrue(ticks.equals(java.util.List.of(16,40,64)),"Exactly three timed impacts: "+ticks);}finally{NeoForge.EVENT_BUS.unregister(listener);fx.discard();}c.succeed();
    }
    @GameTest(template="empty",batch="neo-audio-distance")
    public void allSpellCuesHaveMatchingNetworkAndClientFalloffIndependentOfVolume(GameTestHelper c)throws Exception{
        try(var stream=SpellSounds.class.getResourceAsStream("/assets/royalespells/sounds.json")){
            var sounds=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for(var entry:SpellSounds.cues().entrySet()){
                var cue=entry.getValue();c.assertTrue(cue.sound().getRange(.05f)==64&&cue.sound().getRange(cue.volume())==64,"Quiet sounds still reach distant players: "+entry.getKey());
                for(var sound:sounds.getAsJsonObject(cue.sound().getLocation().getPath()).getAsJsonArray("sounds"))c.assertTrue(sound.getAsJsonObject().get("attenuation_distance").getAsInt()==64,"Client fades over the same 64-block radius: "+entry.getKey());
            }
        }c.succeed();
    }
    @GameTest(template="empty",batch="neo-protection")
    public void claimedBlocksRejectEarthquakeProgress(GameTestHelper c) {
        var w=c.getLevel();var p=c.absolutePos(new BlockPos(2,3,2));w.setBlockAndUpdate(p,Blocks.OAK_PLANKS.defaultBlockState());
        var player=TestPlayers.create(c);
        Consumer<BlockEvent.BreakEvent> protection=e->{if(e.getLevel()==w&&e.getPos().equals(p))e.setCanceled(true);};
        NeoForge.EVENT_BUS.addListener(protection);
        try {
            var quake=new EarthquakeDestruction(w,Vec3.atCenterOf(p));for(int t=1;t<=60;t++)quake.tick(w,player.getUUID(),t);
            c.assertTrue(w.getBlockState(p).is(Blocks.OAK_PLANKS),"A canceled NeoForge break event preserves the claimed block");
            c.assertTrue(EarthquakeDestruction.progress(w,p)==0,"Protected blocks accumulate no crack damage");
        } finally {NeoForge.EVENT_BUS.unregister(protection);player.discard();w.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());}
        c.succeed();
    }
    @GameTest(template="empty",batch="neo-damage-cancel")
    public void damageCancellationAndDeathCancellationAreRespected(GameTestHelper c) {
        var w=c.getLevel();var mob=c.spawnWithNoFreeWill(EntityType.HUSK,2,3,2);UUID owner=UUID.randomUUID();
        Consumer<LivingIncomingDamageEvent> shield=e->{if(e.getEntity()==mob)e.setCanceled(true);};
        NeoForge.EVENT_BUS.addListener(shield);
        try {float hp=mob.getHealth();SpellEngine.hit(w,owner,mob,5);c.assertTrue(mob.getHealth()==hp,"Royale damage flows through NeoForge cancellation");}
        finally {NeoForge.EVENT_BUS.unregister(shield);}
        Consumer<LivingDeathEvent> revive=e->{if(e.getEntity()==mob){e.setCanceled(true);mob.setHealth(1);}};
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH,revive);
        try {
            SpellEngine.curse(owner,mob);SpellEngine.hit(w,owner,mob,1000);
            c.assertTrue(mob.isAlive(),"A mod's death cancellation keeps the target alive");
            c.assertTrue(w.getEntitiesOfClass(AllyZombie.class,mob.getBoundingBox().inflate(2),e->owner.equals(e.ownerId())).isEmpty(),"Canceled death must not produce a curse zombie");
        } finally {NeoForge.EVENT_BUS.unregister(revive);mob.discard();}
        c.succeed();
    }
}
