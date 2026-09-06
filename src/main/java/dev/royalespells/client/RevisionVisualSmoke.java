package dev.royalespells.client;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Opt-in 1.1.2 regression scene: close faces, quiet walking, and the whole rocket arc. */
final class RevisionVisualSmoke {
    private static volatile UUID faceId,walkerId,flightId;
    private static boolean ascending,descending;
    private static int originalFov;
    private static final Vec3 START=new Vec3(194,153,0),END=new Vec3(210,153,0);
    static void tick(Minecraft c,int ready){
        if(ready==900){
            c.options.setCameraType(CameraType.FIRST_PERSON);
            originalFov=c.options.fov().get();c.options.fov().set(40);
            c.getSingleplayerServer().execute(()->{
                var w=c.getSingleplayerServer().overworld();var player=c.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                for(int x=182;x<=216;x++)for(int z=-12;z<=15;z++)w.setBlockAndUpdate(new BlockPos(x,149,z),Blocks.SMOOTH_STONE.defaultBlockState());
                var face=SpellEngine.summon(w,player.getUUID(),new Vec3(190,150,0),"barbarian",false);
                face.setNoAi(true);face.setNoGravity(true);face.setYRot(180);face.setYBodyRot(180);face.setYHeadRot(180);faceId=face.getUUID();
                player.teleportTo(w,190,150.25,-2.2,0,0);
            });
        }
        if(ready==940)shot(c,"royale-barbarian-face-front.png","ROYALE_FACE_FRONT_COMPLETE");
        if(ready==945)camera(c,new Vec3(188.4,150.25,-1.8),-42,0);
        if(ready==975)shot(c,"royale-barbarian-face-side.png","ROYALE_FACE_SIDE_COMPLETE");
        if(ready==980){c.options.fov().set(originalFov);camera(c,new Vec3(191,150.5,-2),-90,8);}
        if(ready==990){
            BarbarianAudioSmoke.beginWalking();
            c.getSingleplayerServer().execute(()->{
                var w=c.getSingleplayerServer().overworld();var player=c.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                var walker=SpellEngine.summon(w,player.getUUID(),new Vec3(195,150,-4),"barbarian",false);
                walker.setNoAi(false);walker.setOnGround(true);walkerId=walker.getUUID();
            });
        }
        if(ready>=1005&&ready<=1080)c.getSingleplayerServer().execute(()->{
            var walker=(Mob)c.getSingleplayerServer().overworld().getEntity(walkerId);
            if(walker!=null)walker.getNavigation().moveTo(195,150,5,1);
        });
        if(ready==1085)BarbarianAudioSmoke.endWalking();
        if(ready==1090){
            camera(c,new Vec3(202,155,-18),0,-3);
            c.getSingleplayerServer().execute(()->{
                var w=c.getSingleplayerServer().overworld();var player=c.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                w.getEntity(faceId).discard();w.getEntity(walkerId).discard();
                for(int tick:new int[]{6,20,34}){
                    var rocket=SpellEntity.create(w,Spell.ROCKET,player.getUUID(),START,END);
                    rocket.preview=true;rocket.setPreviewTime(tick);rocket.setPos(rocket.visualPosition(0));w.addFreshEntity(rocket);
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
            camera(c,new Vec3(202,155,-18),0,-3);
            c.getSingleplayerServer().execute(()->{
                var w=c.getSingleplayerServer().overworld();var player=c.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                for(var entity:w.getEntitiesOfClass(SpellEntity.class,new AABB(190,150,-5,215,165,5),e->true))entity.discard();
                var rocket=SpellEntity.create(w,Spell.PARTY_ROCKET,player.getUUID(),START,END);flightId=rocket.getUUID();w.addFreshEntity(rocket);
            });
        }
        if(ready>1235&&ready<1300)for(var entity:c.level.entitiesForRendering())if(entity.getUUID().equals(flightId)&&entity instanceof SpellEntity rocket){
            var dir=RocketMotion.direction(rocket.start(),rocket.target(),(double)rocket.time()/rocket.spell().duration);
            if(rocket.time()<15&&dir.y>0)ascending=true;
            if(rocket.time()>25&&dir.y<0)descending=true;
        }
        if(ready>=1310&&Boolean.getBoolean("royalespells.ironSmoke"))IronClientSmoke.tick(c,ready-1310);
        if(ready==1305){
            if(!ascending||!descending)throw new IllegalStateException("Live rocket never completed both flight phases");
            System.out.println("ROYALE_ROCKET_LIVE_FLIGHT_COMPLETE");BarbarianAudioSmoke.verify();
            System.out.println("ROYALE_VISUAL_SMOKE_COMPLETE");if(!Boolean.getBoolean("royalespells.ironSmoke"))c.stop();
        }
    }
    private static void rocketCamera(Minecraft c,double progress){camera(c,RocketMotion.position(START,END,progress).add(0,-1.62,-5.2),0,0);}
    private static void camera(Minecraft c,Vec3 at,float yaw,float pitch){c.getSingleplayerServer().execute(()->c.getSingleplayerServer().getPlayerList().getPlayers().get(0).teleportTo(c.getSingleplayerServer().overworld(),at.x,at.y,at.z,yaw,pitch));}
    private static void shot(Minecraft c,String name,String marker){Screenshot.grab(c.gameDirectory,name,c.getMainRenderTarget(),message->System.out.println(marker));}
}
