package dev.mineclash.zappies.client;

import com.google.gson.GsonBuilder;
import dev.mineclash.zappies.*;
import dev.mineclash.zappies.pause.ElectricPause;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.*;
import org.liziyowo.mineclash.entity.mob.*;
import java.nio.file.Files;
import java.util.*;

/** Opt-in packaged-client QA. It only operates inside AnimationProbe's new world. */
public final class ElectricalProbe {
    static final boolean ENABLED=Boolean.getBoolean("zappiesaddon.electricalProbe");
    private static final List<UUID> squad=new ArrayList<>();
    private static UUID victim;
    private static boolean pausedCharge;
    private static int arcShots;
    private static final List<Map<String,Object>> combat=new ArrayList<>();
    private static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    static void tick(Minecraft c,int tick,List<UUID> cars,List<UUID> muskets,List<UUID> skeletons)throws Exception{
        if(tick==110)AnimationProbe.shot(c,"electrical-idle");
        if(tick==120)AnimationProbe.server(c,p->{var w=p.serverLevel();
            for(UUID id:cars)((Zappies)w.getEntity(id)).triggerAnim("attack","attack");
            for(UUID id:skeletons)((ClashSkeleton)w.getEntity(id)).triggerAnim("attack","attack_1");
        });
        if(tick==127)AnimationProbe.server(c,p->{var w=p.serverLevel();
            for(UUID id:List.of(cars.get(1),skeletons.get(1)))((LivingEntity)w.getEntity(id)).addEffect(new MobEffectInstance(ZappiesAddon.ELECTRICAL_STUN,10));
        });
        if(tick==132||tick==145||tick==190)AnimationProbe.shot(c,"pause-and-resume-"+tick);
        if(tick==220)AnimationProbe.server(c,p->{var e=(LivingEntity)p.serverLevel().getEntity(cars.getFirst());e.hurt(e.damageSources().genericKill(),10000);});
        if(tick==265)AnimationProbe.shot(c,"one-dead-two-survive");
        if(tick==280)AnimationProbe.server(c,p->{var w=p.serverLevel();
            for(var ids:List.of(cars,muskets,skeletons))for(UUID id:ids){var e=w.getEntity(id);if(e!=null)e.discard();}
            var egg=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("mineclash:zappies_spawn_egg")));
            p.setItemInHand(InteractionHand.MAIN_HAND,egg);
            var use=new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(new Vec3(.5,150,.5),Direction.UP,new BlockPos(0,149,0),false));
            check(egg.useOn(use).consumesAction(),"Existing spawn egg did not deploy a squad");
            var deployed=w.getEntitiesOfClass(Zappies.class,new AABB(-6,149,-6,6,154,6));
            check(deployed.size()==3,"Existing egg must create three native entities");
            deployed.sort(Comparator.comparingInt(e->ZappyBrain.of(e).slot));
            for(int i=0;i<3;i++){var e=deployed.get(i);e.setCustomName(Component.literal("Squad"+i));squad.add(e.getUUID());
                check(p.getUUID().equals(ZappyBrain.of(e).owner),"Egg owner lost");
                for(int j=0;j<i;j++)check(!e.getBoundingBox().intersects(deployed.get(j).getBoundingBox()),"Squad bodies overlapped at spawn");}
            var target=EntityType.ZOMBIE.create(w);target.moveTo(.5,150,8.5,180,0);target.setNoAi(true);target.setNoGravity(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);target.setCustomName(Component.literal("Combat target"));w.addFreshEntity(target);victim=target.getUUID();
            System.out.println("ZAPPIES_QA_EGG_THREE owner=true overlap=false");
        });
        if(tick>=281&&tick<=680)AnimationProbe.server(c,p->{var w=p.serverLevel();
            for(UUID id:squad){var e=(Zappies)w.getEntity(id);if(e==null)continue;var b=ZappyBrain.of(e);
                if(!pausedCharge&&b.slot==1&&b.windup>=6){e.addEffect(new MobEffectInstance(ZappiesAddon.ELECTRICAL_STUN,10));pausedCharge=true;}
                Map<String,Object> row=new LinkedHashMap<>();row.put("tick",tick);row.put("server_time",w.getGameTime());row.put("slot",b.slot);row.put("position",List.of(e.getX(),e.getY(),e.getZ()));row.put("windup",b.windup);row.put("recovery",b.recovery);row.put("shots",b.shots);row.put("paused",ElectricPause.active(e));row.put("health",e.getHealth());row.put("deploy",b.deploy);row.put("noAi",e.isNoAi());row.put("target",e.getTarget()==null?"none":e.getTarget().getName().getString());row.put("navigation_done",e.getNavigation().isDone());row.put("valid_enemy",b.valid((LivingEntity)w.getEntity(victim)));combat.add(row);
            }
            if(tick==680){var t=(LivingEntity)w.getEntity(victim);
                try{Files.writeString(c.gameDirectory.toPath().resolve("electrical-combat.json"),new GsonBuilder().setPrettyPrinting().create().toJson(combat));}catch(Exception ex){throw new RuntimeException(ex);}
                check(t!=null&&t.getHealth()<990,"Real squad AI did not deal damage");check(pausedCharge,"No real windup was paused");
                for(UUID id:squad){var e=(Zappies)w.getEntity(id);check(e!=null&&ZappyBrain.of(e).shots>0,"Each living car must actually discharge through its AI");}
                System.out.println("ZAPPIES_QA_REAL_COMBAT victim_health="+t.getHealth()+" samples="+combat.size());}
        });
        if(tick==390||tick==460||tick==570||tick==670)AnimationProbe.shot(c,"native-squad-combat-"+tick);
        if(tick>300&&ZappyArcRenderer.visibleArcs>0&&arcShots<3){AnimationProbe.shot(c,"connected-attack-arc-"+(++arcShots));}
    }
    private ElectricalProbe(){}
}
