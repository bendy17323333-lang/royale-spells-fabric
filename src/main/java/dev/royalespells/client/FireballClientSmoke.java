// SPDX-License-Identifier: MIT
package dev.royalespells.client;

import dev.royalespells.Spell;
import dev.royalespells.entity.SpellEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/** Opt-in screenshot QA only: no video, sound recording, or user-world access. */
public final class FireballClientSmoke {
    private static final Vec3 EYE=new Vec3(6.8,154,-10.5),FOCUS=new Vec3(-1.5,151.3,0);
    private static int ticks,warmup;private static volatile boolean ready;private static boolean started;
    private static IronGolem target;
    public static void tick(Minecraft c) {
        c.options.hideGui=true;c.options.fov().set(58);c.getToasts().clear();c.mouseHandler.releaseMouse();
        var d=FOCUS.subtract(EYE);float yaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z)),pitch=(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z)));
        c.player.setYRot(yaw);c.player.setXRot(pitch);c.player.yRotO=yaw;c.player.xRotO=pitch;
        if(!started){started=true;c.getSingleplayerServer().execute(()->{
            var server=c.getSingleplayerServer();var p=server.getPlayerList().getPlayers().getFirst();var w=p.serverLevel();
            w.setDayTime(6000);w.setWeatherParameters(0,0,false,false);
            for(int x=-1;x<=0;x++)for(int z=-1;z<=0;z++)w.setChunkForced(x,z,true);
            p.getInventory().clearContent();p.teleportTo(w,EYE.x,EYE.y-p.getEyeHeight(),EYE.z,yaw,pitch);
            target=EntityType.IRON_GOLEM.create(w);target.moveTo(0,150,1);target.setNoAi(true);
            target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(300);target.setHealth(300);
            target.setCustomName(Component.literal("Fireball visual QA"));w.addFreshEntity(target);ready=true;
        });return;}
        if(!ready||warmup++<40)return;
        ticks++;
        if(ticks==30||ticks==130||ticks==220) {
            final int scene=ticks;
            c.getSingleplayerServer().execute(()->{
                var p=c.getSingleplayerServer().getPlayerList().getPlayers().getFirst();var w=p.serverLevel();
                target.teleportTo(0,150,1);target.setDeltaMovement(Vec3.ZERO);
                Vec3 start=scene==30?new Vec3(-10,151.8,-3):scene==130?new Vec3(8,152,7):new Vec3(0,159,1);
                w.addFreshEntity(SpellEntity.create(w,Spell.FIREBALL,p.getUUID(),start,new Vec3(0,150,1)));
            });
        }
        if(List.of(43,49,54,55,56,57,59,61,63,77,110,143,149,157,170,233,239,247,260).contains(ticks))
            Screenshot.grab(c.gameDirectory,"fireball-dev6-"+ticks+".png",c.getMainRenderTarget(),m->{});
        if(ticks==310)c.getSingleplayerServer().execute(()->{
            if(target.getHealth()>=300)throw new IllegalStateException("Actual fireball failed to damage the target");
            System.out.println("FIREBALL_SERVER_DAMAGE hp="+target.getHealth());target.discard();
        });
        if(ticks==330){System.out.println("ROYALE_FIREBALL_SMOKE_COMPLETE screenshots_only=true");c.stop();}
    }
    private FireballClientSmoke(){}
}
