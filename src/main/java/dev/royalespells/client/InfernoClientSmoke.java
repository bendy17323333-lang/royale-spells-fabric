package dev.royalespells.client;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import dev.royalespells.iron.*;
import net.minecraft.client.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.Consumer;

/** Opt-in checks in a fresh isolated world, never in an ordinary user session. */
final class InfernoClientSmoke {
    private static int tick,resizeWait;private static volatile boolean ready;private static UUID model,target;
    private static final Set<String> played=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static int maxLoops;private static float healthBeforeFreeze;
    private static double minClearance=100,maxServerYawError,maxClientYawError;
    private static int clientFacingSamples,serverFlightSamples;
    private static void server(Minecraft c,Consumer<ServerPlayer> action){c.getSingleplayerServer().execute(()->{try{action.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst());}catch(Throwable error){error.printStackTrace();System.err.println("ROYALE_INFERNO_FAILURE");c.execute(c::stop);}});}
    private static void camera(ServerPlayer p,Vec3 eye,Vec3 look){var d=look.subtract(eye);p.teleportTo(p.serverLevel(),eye.x,eye.y-p.getEyeHeight(),eye.z,(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))));}
    private static void shot(Minecraft c,String name){var f=c.getMainRenderTarget();if(f.width<1200||f.height<700)throw new IllegalStateException("Invalid screenshot size");Screenshot.grab(c.gameDirectory,"inferno-"+name+".png",f,m->{});System.out.println("ROYALE_INFERNO_FRAME "+name+" "+f.width+"x"+f.height);}
    static void tick(Minecraft c){
        if(c.getMainRenderTarget().width<1200||c.getMainRenderTarget().height<700){
            if(resizeWait++%20==0){org.lwjgl.glfw.GLFW.glfwRestoreWindow(c.getWindow().getWindow());c.getWindow().setWindowed(1600,1000);c.resizeDisplay();}
            if(resizeWait>160)throw new IllegalStateException("Isolated window is unusable");return;
        }
        if(tick>0&&!ready)return;
        c.getToasts().clear();c.gui.getChat().clearMessages(false);tick++;maxLoops=Math.max(maxLoops,InfernoDragonAudio.activeLoops());
        if(tick==1){
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent event)->{var id=event.getSound().getLocation();if(id.getNamespace().equals("royalespells")&&id.getPath().startsWith("inferno_dragon_")){played.add(id.getPath());System.out.println("ROYALE_INFERNO_OPENAL "+id);}});
            c.options.hideGui=true;
            server(c,p->{var w=p.serverLevel();for(int x=-1;x<=0;x++)for(int z=-1;z<=0;z++)w.setChunkForced(x,z,true);
                p.getInventory().clearContent();var e=RoyaleSpells.INFERNO_DRAGON.create(w);e.setup(p.getUUID(),6000,false);e.moveTo(0,153.5,0,180,0);e.setYBodyRot(180);e.setYHeadRot(180);e.setNoAi(true);w.addFreshEntity(e);model=e.getUUID();camera(p,new Vec3(3.5,155.5,-5.5),new Vec3(0,154.5,0));ready=true;});return;
        }
        if(tick==65)shot(c,"front-hover");
        if(tick==80)server(c,p->camera(p,new Vec3(-3.5,154.2,-4.5),new Vec3(0,154.5,0)));
        if(tick==120)shot(c,"lower-wings");
        if(tick==135)server(c,p->camera(p,new Vec3(3,156.5,4.5),new Vec3(0,154.5,0)));
        if(tick==175)shot(c,"back-tank");
        if(tick==190)server(c,p->{p.serverLevel().getEntity(model).discard();camera(p,new Vec3(0,153,-6),new Vec3(0,150,0));p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();p.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA).setBaseValue(300);io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(p).setMana(300);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,IronIntegration.scroll(IronSpellProfile.INFERNO_DRAGON,1));});
        if(tick==210)server(c,p->{var spell=IronIntegration.spell(IronSpellProfile.INFERNO_DRAGON);if(!spell.attemptInitiateCast(p.getMainHandItem(),1,p.level(),p,io.redspace.ironsspellbooks.api.spells.CastSource.SCROLL,true,"mainhand"))throw new IllegalStateException("Native scroll cast did not start");});
        if(tick==255)server(c,p->{
            var list=p.serverLevel().getEntitiesOfClass(InfernoDragon.class,p.getBoundingBox().inflate(30),e->p.getUUID().equals(e.ownerId()));if(list.size()!=1)throw new IllegalStateException("Native scroll did not create exactly one dragon: "+list.size());
            var e=list.getFirst();model=e.getUUID();e.moveTo(0,153.5,0,180,0);e.setYBodyRot(180);e.setYHeadRot(180);e.setDeltaMovement(Vec3.ZERO);
            float mana=io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(p).getMana();System.out.println("ROYALE_INFERNO_NATIVE_CAST mana="+mana+" spell="+e.getPersistentData().getString("RoyaleIronSpell"));
            var victim=EntityType.HUSK.create(p.serverLevel());victim.moveTo(0,150,-3.8,0,0);victim.setNoAi(true);victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);victim.getAttribute(Attributes.ARMOR).setBaseValue(0);victim.setHealth(1000);p.serverLevel().addFreshEntity(victim);target=victim.getUUID();SummonOrders.order(p,victim);e.setTarget(victim);
            var golem=EntityType.IRON_GOLEM.create(p.serverLevel());golem.moveTo(2.1,150,0,0,0);golem.setNoAi(true);p.serverLevel().addFreshEntity(golem);
            camera(p,new Vec3(7,155.5,-7),new Vec3(0,153.1,-1.4));
        });
        if(tick>=270&&tick<=370){
            for(var e:c.level.getEntitiesOfClass(InfernoDragon.class,c.player.getBoundingBox().inflate(40))){
                var victim=e.beamTarget();if(victim!=null&&e.heatTicks()>5){maxClientYawError=Math.max(maxClientYawError,facingError(e,victim));clientFacingSamples++;}
            }
            if(tick%5==0)server(c,p->{var e=(InfernoDragon)p.serverLevel().getEntity(model);var victim=(LivingEntity)p.serverLevel().getEntity(target);
                minClearance=Math.min(minClearance,e.getY()-150);maxServerYawError=Math.max(maxServerYawError,facingError(e,victim));serverFlightSamples++;
                if(e.getY()<153.42||e.heatTicks()<5||maxServerYawError>1)throw new IllegalStateException("Flight clearance / target lock failed");
                // Gentle lateral movement exercises both networked body yaw and the extended range.
                victim.setPos(Math.sin(e.tickCount*.02)*.25,150,-3.8);
            });
        }
        if(tick==285)shot(c,"beam-low");
        if(tick==320)shot(c,"beam-medium");
        if(tick==355)shot(c,"beam-hot");
        if(tick==375)server(c,p->{var e=(InfernoDragon)p.serverLevel().getEntity(model);var victim=(LivingEntity)p.serverLevel().getEntity(target);if(e.heatTicks()<80||victim.getHealth()>=950)throw new IllegalStateException("Live heat / damage missing: "+e.heatTicks()+" / "+victim.getHealth());System.out.println("ROYALE_INFERNO_FLIGHT minClearance="+minClearance+" maxServerYawError="+maxServerYawError+" samples="+serverFlightSamples+" heat="+e.heatTicks()+" hp="+victim.getHealth());e.addEffect(new net.minecraft.world.effect.MobEffectInstance(RoyaleSpells.FROZEN,80));healthBeforeFreeze=victim.getHealth();});
        if(tick==400){shot(c,"frozen-beam-stopped");if(InfernoDragonAudio.activeLoops()!=0)throw new IllegalStateException("Frozen beam loop kept playing");}
        if(tick==445)server(c,p->{var victim=(LivingEntity)p.serverLevel().getEntity(target);if(victim.getHealth()!=healthBeforeFreeze)throw new IllegalStateException("Frozen dragon continued damage");});
        if(tick==510){shot(c,"resumed");server(c,p->{var e=p.serverLevel().getEntity(model);if(e!=null)e.discard();});}
        if(tick==530){
            if(clientFacingSamples<50||maxClientYawError>3)throw new IllegalStateException("Client model faces away from the laser: error="+maxClientYawError+" samples="+clientFacingSamples);
            System.out.println("ROYALE_INFERNO_FACING maxClientYawError="+maxClientYawError+" samples="+clientFacingSamples);
            if(InfernoDragonAudio.activeLoops()!=0||maxLoops!=1)throw new IllegalStateException("Loop leak / overlap: "+InfernoDragonAudio.activeLoops()+" max="+maxLoops);
            if(!played.containsAll(List.of("inferno_dragon_deploy","inferno_dragon_wing","inferno_dragon_beam")))throw new IllegalStateException("OpenAL did not play all original cues: "+played);
            System.out.println("ROYALE_INFERNO_CLIENT_COMPLETE maxLoops="+maxLoops+" played="+played);c.stop();
        }
    }
    private static double facingError(InfernoDragon dragon,LivingEntity victim){
        float yaw=(float)Math.toDegrees(Math.atan2(dragon.getX()-victim.getX(),victim.getZ()-dragon.getZ()));
        return Math.abs(net.minecraft.util.Mth.wrapDegrees(dragon.yBodyRot-yaw));
    }
}
