package dev.royalespells.client;
import dev.royalespells.*;
import dev.royalespells.army.*;
import dev.royalespells.entity.*;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.function.Consumer;

/** Actual final-JAR render, audio and item-network regression in a disposable opt-in world. */
final class CombatClientSmoke {
    private static boolean queued;private static volatile boolean ready;private static int tick,lightning,shield,freezeSamples;
    private static final List<UUID> models=new ArrayList<>(),victims=new ArrayList<>();
    private static final Map<UUID,Set<Integer>> before=new HashMap<>(),frozen=new HashMap<>(),after=new HashMap<>();
    private static final Set<Integer> deploymentFrames=new HashSet<>();
    private static void server(Minecraft c,Consumer<ServerPlayer> work){c.getSingleplayerServer().execute(()->{try{work.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst());}catch(Throwable e){e.printStackTrace();System.err.println("ROYALE_COMBAT_CLIENT_FAILURE");c.execute(c::stop);}});}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private static void shot(Minecraft c,String name){Screenshot.grab(c.gameDirectory,"combat-"+name+".png",c.getMainRenderTarget(),m->{});System.out.println("ROYALE_COMBAT_FRAME "+name);}
    private static void camera(ServerPlayer p,Vec3 eye,Vec3 at){var d=at.subtract(eye);p.teleportTo(p.serverLevel(),eye.x,eye.y-p.getEyeHeight(),eye.z,(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))));}
    private static void wide(ServerPlayer p){camera(p,new Vec3(1,152,-11),new Vec3(1,150.8,0));}
    private static void setup(ServerPlayer p){
        var w=p.serverLevel();for(var e:com.google.common.collect.ImmutableList.copyOf(w.getAllEntities()))if(e instanceof Summoned||e instanceof SpellEntity)e.discard();
        var skeleton=RoyaleSpells.SKELETON.create(w);skeleton.setup(p.getUUID(),12000,false);skeleton.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(net.minecraft.world.item.Items.STONE_SWORD));
        var general=RoyaleSpells.ARMY_SKELETON.create(w);UUID faction=UUID.randomUUID(),army=UUID.randomUUID();general.enlist(faction,army,true,4,1.8f,12000);general.setNeutralArmy();ArmyLedger.get(p.server).start(faction,army,general.getUUID(),ArmyLedger.now(p.server)+12000,0);
        var barb=RoyaleSpells.BARBARIAN.create(w);barb.setup(p.getUUID(),12000,false);
        var cryo=(Mob)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("irons_spellbooks:cryomancer")).create(w);
        var neighbor=(Mob)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("irons_spellbooks:cryomancer")).create(w);
        int i=0;for(var mob:List.of(skeleton,general,barb,cryo,neighbor)){
            mob.moveTo(-4+(i++)*2.5,150,0,180,0);mob.setYBodyRot(180);mob.setYHeadRot(180);mob.setNoAi(true);mob.setNoGravity(true);w.addFreshEntity(mob);models.add(mob.getUUID());
        }
        p.getInventory().clearContent();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();wide(p);ready=true;
    }
    private static void sample(Minecraft c,Map<UUID,Set<Integer>> samples){for(var entity:c.level.entitiesForRendering())if(entity instanceof LivingEntity e&&models.contains(e.getUUID())&&FrozenRender.parts(e)>0)samples.computeIfAbsent(e.getUUID(),k->new HashSet<>()).add(FrozenRender.signature(e));}
    private static void lightning(ServerPlayer p,int count){
        var w=p.serverLevel();for(var id:victims){var old=w.getEntity(id);if(old!=null)old.discard();}victims.clear();var center=new Vec3(0,150,12);
        for(int i=0;i<count;i++){var target=EntityType.COW.create(w);target.setPos(center.add(i*.8,0,0));target.setNoAi(true);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);w.addFreshEntity(target);victims.add(target.getUUID());}
        w.addFreshEntity(SpellEntity.create(w,Spell.LIGHTNING,p.getUUID(),center,center));
    }
    static void tick(Minecraft c){
        try{
            c.getToasts().clear();c.gui.getChat().clearMessages(false);
            if(!queued){queued=true;c.options.hideGui=true;c.getSoundManager().addListener((sound,event,range)->{
                if(sound.getLocation().equals(RoyaleSpells.id("spell_lightning_deploy"))){lightning++;require(range==64,"Lightning falloff changed");System.out.println("ROYALE_COMBAT_LIGHTNING_AUDIO "+lightning);}
                if(sound.getLocation().equals(ArmySounds.SHIELD_BREAK.getLocation())){shield++;require(range==32&&sound.getSound()!=net.minecraft.client.sounds.SoundManager.EMPTY_SOUND,"Shield cue/range missing");System.out.println("ROYALE_COMBAT_SHIELD_AUDIO "+shield);}
            });server(c,CombatClientSmoke::setup);return;}
            if(!ready)return;int t=++tick;
            if(t>=55&&t<=275&&t%5==0)server(c,p->{for(var id:models){var e=(LivingEntity)p.serverLevel().getEntity(id);e.setYHeadRot(180+(float)Math.sin(t*.12)*28);e.setXRot((float)Math.sin(t*.1)*10);}});
            if(t==100)shot(c,"model-front");
            if(t>=55&&t<=115)sample(c,before);
            if(t==120)server(c,p->{for(int i=0;i<4;i++){var e=(LivingEntity)p.serverLevel().getEntity(models.get(i));SpellEngine.stun(e,100);e.addEffect(new MobEffectInstance(RoyaleSpells.FROZEN,100,0,false,false));}System.out.println("ROYALE_COMBAT_FREEZE_STARTED");});
            if(t>=130&&t<=205){
                boolean allFrozen=true;
                for(var entity:c.level.entitiesForRendering())if(entity instanceof LivingEntity e&&models.contains(e.getUUID())&&FrozenRender.parts(e)>0){
                    if(models.indexOf(e.getUUID())<4&&(!VisualState.frozen(e)||!FrozenRender.wasFrozen(e))){allFrozen=false;continue;}
                    frozen.computeIfAbsent(e.getUUID(),k->new HashSet<>()).add(FrozenRender.signature(e));
                }
                if(allFrozen){freezeSamples++;if(freezeSamples==12||freezeSamples==26)shot(c,"frozen-sample-"+freezeSamples);}
            }
            if(t==210){
                require(freezeSamples>=15,"Insufficient actually frozen frames: "+freezeSamples);
                for(int i=0;i<4;i++){var id=models.get(i);System.out.println("ROYALE_FROZEN_MATRIX_AUDIT model="+i+" before="+before.getOrDefault(id,Set.of()).size()+" frozen="+frozen.getOrDefault(id,Set.of()).size()+" samples="+freezeSamples);require(before.getOrDefault(id,Set.of()).size()>1,"Model did not animate before Freeze: "+i);require(frozen.getOrDefault(id,Set.of()).size()==1,"Frozen render matrices changed: "+i+" variants="+frozen.get(id));}
                require(frozen.getOrDefault(models.get(4),Set.of()).size()>1,"Unfrozen neighbor sharing Gecko renderer was also frozen");System.out.println("ROYALE_COMBAT_FROZEN_POSES_VERIFIED vanilla skeleton, general, barbarian, Gecko cryomancer; neighbor animates");
            }
            if(t>=245&&t<=275)sample(c,after);
            if(t==277){for(int i=0;i<4;i++)require(after.getOrDefault(models.get(i),Set.of()).size()>1,"Model did not resume after thaw: "+i);shot(c,"thawed");System.out.println("ROYALE_COMBAT_THAW_VERIFIED");}
            if(t==280)server(c,p->{var g=(LivingEntity)p.serverLevel().getEntity(models.get(1));g.setYHeadRot(180);g.setXRot(0);camera(p,new Vec3(-1.5,151.04,-2.2),new Vec3(-1.5,151.02,0));});
            if(t==305)shot(c,"helmet-front-cheek-guards");
            if(t==315)server(c,p->{camera(p,new Vec3(0,153,7),new Vec3(0,150.8,12));lightning(p,1);});
            if(t==345){require(lightning==1,"One victim must yield one sound: "+lightning);server(c,p->lightning(p,3));}
            if(t==352)shot(c,"lightning-strike");
            if(t==375){require(lightning==4,"Three victims must yield three more sounds: "+lightning);System.out.println("ROYALE_COMBAT_LIGHTNING_ONE_AND_THREE_VERIFIED");server(c,p->{var g=(ArmySkeleton)p.serverLevel().getEntity(models.get(1));g.hurt(p.damageSources().generic(),1);g.hurt(p.damageSources().generic(),5);g.hurt(p.damageSources().generic(),1);camera(p,new Vec3(-1.5,151.04,-2.2),g.position().add(0,1.02,0));});}
            if(t==405){require(shield==1,"One breaking hit must produce one original cue: "+shield);shot(c,"shield-broken");System.out.println("ROYALE_COMBAT_SHIELD_BREAK_VERIFIED");}
            if(t==420)server(c,p->{p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(RoyaleSpells.NEUTRAL_ARMY_EGG));camera(p,new Vec3(8,151.7,6.5),new Vec3(8.5,149.9,10.5));});
            if(t==445){c.options.hideGui=false;c.gameMode.useItemOn(c.player,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(8.5,150,10.5),Direction.UP,new BlockPos(8,149,10),false));}
            if(t>445&&t<478)for(var e:c.level.entitiesForRendering())if(e instanceof EvolutionBurst burst){
                int phase=burst.age()<5?0:burst.age()<13?1:2;
                if(deploymentFrames.add(phase)){shot(c,"evolution-deploy-phase-"+phase);System.out.println("ROYALE_COMBAT_EVOLUTION_FRAME phase="+phase+" age="+burst.age());}
            }
            if(t==470)server(c,p->{var list=p.serverLevel().getEntitiesOfClass(ArmySkeleton.class,AABB.ofSize(new Vec3(8,151,10),11,8,11));require(list.size()==16&&list.stream().allMatch(s->s.neutralArmy()&&s.supported()),"Actual spawn egg item/network path did not deploy a whole independent army: "+list.size());camera(p,new Vec3(12,155,4),new Vec3(8,150.8,10));System.out.println("ROYALE_COMBAT_NEUTRAL_EGG_CLIENT_INPUT_VERIFIED count=16");});
            if(t==495){require(deploymentFrames.size()==3,"Missing synchronized evolution deployment phases: "+deploymentFrames);for(var e:c.level.entitiesForRendering())require(!(e instanceof EvolutionBurst),"Deployment effect did not expire");shot(c,"independent-army-egg");System.out.println("ROYALE_COMBAT_EVOLUTION_DEPLOYMENT_VERIFIED phases=3 expired=true");}
            if(t==500){c.options.hideGui=true;server(c,p->camera(p,new Vec3(-3.05,151.16,-2),new Vec3(-1.5,151.02,0)));}
            if(t==530)shot(c,"helmet-oblique-cheek-guards");
            if(t==540){System.out.println("ROYALE_COMBAT_CLIENT_COMPLETE");c.stop();}
        }catch(Throwable e){e.printStackTrace();System.err.println("ROYALE_COMBAT_CLIENT_FAILURE");c.stop();}
    }
}
