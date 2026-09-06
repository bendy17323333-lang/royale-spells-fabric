package dev.royalespells.client;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.*;
import java.util.UUID;

/** Opt-in 1.1.2 regression scene: close faces, quiet walking, and the whole rocket arc. */
final class RevisionVisualSmoke {
    private static volatile UUID faceId,walkerId,flightId;
    private static boolean ascending,descending;
    private static int originalFov;
    private static final Vec3d START=new Vec3d(194,153,0),END=new Vec3d(210,153,0);
    static void tick(MinecraftClient c,int ready){
        if(ready==900){
            c.options.setPerspective(Perspective.FIRST_PERSON);
            originalFov=c.options.getFov().getValue();c.options.getFov().setValue(40);
            c.getServer().execute(()->{
                var w=c.getServer().getOverworld();var player=c.getServer().getPlayerManager().getPlayerList().get(0);
                for(int x=182;x<=216;x++)for(int z=-12;z<=15;z++)w.setBlockState(new BlockPos(x,149,z),Blocks.SMOOTH_STONE.getDefaultState());
                var face=SpellEngine.summon(w,player.getUuid(),new Vec3d(190,150,0),"barbarian",false);
                face.setAiDisabled(true);face.setNoGravity(true);face.setYaw(180);face.setBodyYaw(180);face.setHeadYaw(180);faceId=face.getUuid();
                player.teleport(w,190,150.25,-2.2,0,0);
            });
        }
        if(ready==940)shot(c,"royale-barbarian-face-front.png","ROYALE_FACE_FRONT_COMPLETE");
        if(ready==945)camera(c,new Vec3d(188.4,150.25,-1.8),-42,0);
        if(ready==975)shot(c,"royale-barbarian-face-side.png","ROYALE_FACE_SIDE_COMPLETE");
        if(ready==980){c.options.getFov().setValue(originalFov);camera(c,new Vec3d(191,150.5,-2),-90,8);}
        if(ready==990){
            BarbarianAudioSmoke.beginWalking();
            c.getServer().execute(()->{
                var w=c.getServer().getOverworld();var player=c.getServer().getPlayerManager().getPlayerList().get(0);
                var walker=SpellEngine.summon(w,player.getUuid(),new Vec3d(195,150,-4),"barbarian",false);
                walker.setAiDisabled(false);walker.setOnGround(true);walkerId=walker.getUuid();
            });
        }
        if(ready>=1005&&ready<=1080)c.getServer().execute(()->{
            var walker=(MobEntity)c.getServer().getOverworld().getEntity(walkerId);
            if(walker!=null)walker.getNavigation().startMovingTo(195,150,5,1);
        });
        if(ready==1085)BarbarianAudioSmoke.endWalking();
        if(ready==1090){
            camera(c,new Vec3d(202,155,-18),0,-3);
            c.getServer().execute(()->{
                var w=c.getServer().getOverworld();var player=c.getServer().getPlayerManager().getPlayerList().get(0);
                w.getEntity(faceId).discard();w.getEntity(walkerId).discard();
                for(int tick:new int[]{6,20,34}){
                    var rocket=SpellEntity.create(w,Spell.ROCKET,player.getUuid(),START,END);
                    rocket.preview=true;rocket.setPreviewTime(tick);rocket.setPosition(rocket.visualPosition(0));w.spawnEntity(rocket);
                }
            });
        }
        if(ready==1125)shot(c,"royale-rocket-arc.png","ROYALE_ROCKET_ARC_COMPLETE");
        if(ready==1130)rocketCamera(c,.15);
        if(ready==1160)shot(c,"royale-rocket-ascent.png","ROYALE_ROCKET_ASCENT_COMPLETE");
        if(ready==1165)rocketCamera(c,.5);
        if(ready==1195)shot(c,"royale-rocket-apex.png","ROYALE_ROCKET_APEX_COMPLETE");
        if(ready==1200)rocketCamera(c,.85);
        if(ready==1230)shot(c,"royale-rocket-descent.png","ROYALE_ROCKET_DESCENT_COMPLETE");
        if(ready==1235){
            camera(c,new Vec3d(202,155,-18),0,-3);
            c.getServer().execute(()->{
                var w=c.getServer().getOverworld();var player=c.getServer().getPlayerManager().getPlayerList().get(0);
                for(var entity:w.getEntitiesByClass(SpellEntity.class,new Box(190,150,-5,215,165,5),e->true))entity.discard();
                var rocket=SpellEntity.create(w,Spell.PARTY_ROCKET,player.getUuid(),START,END);flightId=rocket.getUuid();w.spawnEntity(rocket);
            });
        }
        if(ready>1235&&ready<1300)for(var entity:c.world.getEntities())if(entity.getUuid().equals(flightId)&&entity instanceof SpellEntity rocket){
            var dir=RocketMotion.direction(rocket.start(),rocket.target(),(double)rocket.time()/rocket.spell().duration);
            if(rocket.time()<15&&dir.y>0)ascending=true;
            if(rocket.time()>25&&dir.y<0)descending=true;
        }
        if(ready==1305){
            if(!ascending||!descending)throw new IllegalStateException("Live rocket never completed both flight phases");
            System.out.println("ROYALE_ROCKET_LIVE_FLIGHT_COMPLETE");BarbarianAudioSmoke.verify();
            System.out.println("ROYALE_VISUAL_SMOKE_COMPLETE");c.scheduleStop();
        }
    }
    private static void rocketCamera(MinecraftClient c,double progress){camera(c,RocketMotion.position(START,END,progress).add(0,-1.62,-5.2),0,0);}
    private static void camera(MinecraftClient c,Vec3d at,float yaw,float pitch){c.getServer().execute(()->c.getServer().getPlayerManager().getPlayerList().get(0).teleport(c.getServer().getOverworld(),at.x,at.y,at.z,yaw,pitch));}
    private static void shot(MinecraftClient c,String name,String marker){ScreenshotRecorder.saveScreenshot(c.runDirectory,name,c.getFramebuffer(),message->System.out.println(marker));}
}
