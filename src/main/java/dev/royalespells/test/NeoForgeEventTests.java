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
