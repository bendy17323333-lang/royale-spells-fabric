package dev.royalespells.client;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Extra isolated client QA: real GeckoLib renderers, a real player owner, and live cross-mod projectiles. */
final class IronClientSmoke {
    private static volatile UUID pyroId,coldId,barbId,summonId;
    private static volatile boolean combatPassed;
    private static Mob iron(ServerLevel world,String type,double x) {
        var mob=(Mob)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("irons_spellbooks",type)).create(world);
        mob.setPos(x,150,0);mob.setNoAi(true);mob.setNoGravity(true);mob.setYRot(180);mob.setYBodyRot(180);mob.setYHeadRot(180);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);mob.setHealth(200);world.addFreshEntity(mob);return mob;
    }
    static void tick(Minecraft c,int tick) {
        if(tick==0)c.getSingleplayerServer().execute(()->{
            var world=c.getSingleplayerServer().overworld();var player=c.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
            for(int x=228;x<=255;x++)for(int z=-14;z<=9;z++) {
                world.setBlockAndUpdate(new BlockPos(x,149,z),Blocks.STONE_BRICKS.defaultBlockState());
                for(int y=150;y<169;y++)world.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
            }
            player.teleportTo(world,241,153,-11,0,10);player.getInventory().clearContent();
            for(var spell:new Spell[]{Spell.FIREBALL,Spell.FREEZE,Spell.VINES,Spell.ZAP})player.addItem(new ItemStack(RoyaleSpells.ITEMS.get(spell)));
            var pyro=iron(world,"pyromancer",236);pyroId=pyro.getUUID();pyro.addEffect(new MobEffectInstance(RoyaleSpells.FROZEN,100,0,false,false));SpellEngine.stun(pyro,100);
            var cold=iron(world,"cryomancer",247);coldId=cold.getUUID();cold.addEffect(new MobEffectInstance(RoyaleSpells.ROOTED,100,0,false,false));SpellEngine.stun(cold,100);
            var barb=SpellEngine.summon(world,player.getUUID(),new Vec3(240,150,0),"barbarian",false);barbId=barb.getUUID();barb.setNoAi(true);barb.setYRot(180);barb.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);barb.setHealth(200);
            var summon=iron(world,"summoned_zombie",243);summonId=summon.getUUID();SummonManager.setOwner(summon,player);
            if(!SpellEngine.friendly(player.getUUID(),summon))throw new IllegalStateException("Real player ownership did not resolve across mods");
            summon.setTarget(barb);if(summon.getTarget()!=null)throw new IllegalStateException("Friendly Iron summon targeted player's Barbarian");
            System.out.println("ROYALE_IRON_REAL_PLAYER_ALLIANCE_COMPLETE");
        });
        if(tick==65) {
            int found=0,status=0;
            for(var e:c.level.entitiesForRendering())if(e.getUUID().equals(pyroId)||e.getUUID().equals(coldId)||e.getUUID().equals(barbId)||e.getUUID().equals(summonId)) {
                found++;if(e instanceof net.minecraft.world.entity.LivingEntity living&&(VisualState.frozen(living)||VisualState.rooted(living)))status++;
            }
            if(found!=4||status!=2)throw new IllegalStateException("Iron's client entities/status sync missing: "+found+"/"+status);
            Screenshot.grab(c.gameDirectory,"royale-irons-coinstall-status.png",c.getMainRenderTarget(),message->System.out.println("ROYALE_IRON_CLIENT_ENTITIES_COMPLETE entities=4 status=2"));
        }
        if(tick==110)c.getSingleplayerServer().execute(()->{
            var world=c.getSingleplayerServer().overworld();var player=c.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
            var pyro=(Mob)world.getEntity(pyroId);var cold=(Mob)world.getEntity(coldId);var barb=(Mob)world.getEntity(barbId);
            pyro.setNoAi(false);pyro.setTarget(barb);
            try{pyro.getClass().getMethod("initiateCastSpell",AbstractSpell.class,int.class).invoke(pyro,SpellRegistry.FIREBOLT_SPELL.get(),1);}
            catch(Exception ex){throw new IllegalStateException("Cannot start real Iron's spell",ex);}
            world.addFreshEntity(SpellEntity.create(world,Spell.FIREBALL,player.getUUID(),player.position(),cold.position()));
        });
        if(tick==190)c.getSingleplayerServer().execute(()->{
            var world=c.getSingleplayerServer().overworld();var cold=(Mob)world.getEntity(coldId);var barb=(Mob)world.getEntity(barbId);var summon=(Mob)world.getEntity(summonId);
            if(cold.getHealth()>=200||barb.getHealth()>=200)throw new IllegalStateException("Cross-mod projectiles did not deal damage: iron="+cold.getHealth()+" royale="+barb.getHealth());
            if(summon.getHealth()!=200)throw new IllegalStateException("Friendly Iron's summon was damaged by our area spell");
            combatPassed=true;System.out.println("ROYALE_IRON_LIVE_PROJECTILES_COMPLETE ironTargetHp="+cold.getHealth()+" royaleTargetHp="+barb.getHealth()+" friendlyHp="+summon.getHealth());
        });
        if(tick==215) {
            if(!combatPassed)throw new IllegalStateException("Live cross-mod combat did not complete");
            Screenshot.grab(c.gameDirectory,"royale-irons-live-combat.png",c.getMainRenderTarget(),message->System.out.println("ROYALE_IRON_CLIENT_SMOKE_COMPLETE"));c.stop();
        }
    }
    private IronClientSmoke(){}
}
