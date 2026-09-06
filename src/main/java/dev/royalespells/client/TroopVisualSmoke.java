package dev.royalespells.client;
import dev.royalespells.*;
import dev.royalespells.entity.*;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

public final class TroopVisualSmoke {
    private static volatile UUID barbarianId;
    private static boolean attackShot;
    public static void tick(Minecraft client,int ready) {
        if(ready==500)client.options.hideGui=true;
        if(ready==500)client.getSingleplayerServer().execute(()->{
            var world=client.getSingleplayerServer().overworld();var player=client.getSingleplayerServer().getPlayerList().getPlayers().get(0);
            for(int x=103;x<=171;x++)for(int z=-15;z<=12;z++){
                world.setBlockAndUpdate(new BlockPos(x,149,z),((x+z)%2==0?Blocks.SMOOTH_STONE:Blocks.STONE_BRICKS).defaultBlockState());
                for(int y=150;y<=162;y++)world.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
            }
            player.teleportTo(world,113,152,-8,0,9);
        });
        // Let the client sound listener reach the teleported camera before deployment.
        if(ready==530)client.getSingleplayerServer().execute(()->{
            var world=client.getSingleplayerServer().overworld();var player=client.getSingleplayerServer().getPlayerList().getPlayers().get(0);
            String[] units={"barbarian","recruit","skeleton"};
            for(int i=0;i<units.length;i++){
                var mob=SpellEngine.summon(world,player.getUUID(),new Vec3(110+i*3,150,0),units[i],false);
                if(mob==null)throw new IllegalStateException("Showroom spawn failed: "+units[i]);
                mob.setNoAi(true);mob.setNoGravity(true);mob.setYRot(180);mob.setYHeadRot(180);mob.setYBodyRot(180);
                ((Summoned)mob).setup(player.getUUID(),1200,false);
                if(i==0)barbarianId=mob.getUUID();
            }
            var hut=(RoyaleUnit)SpellEngine.summon(world,player.getUUID(),new Vec3(142,150,0),"barbarian_hut",false);hut.lockFacing(180);
        });
        if(ready==540)client.getSingleplayerServer().execute(()->{
            for(var entity:client.getSingleplayerServer().overworld().getAllEntities())if(entity instanceof Mob mob&&mob.getType()==RoyaleSpells.BARBARIAN&&mob.getX()>130)mob.setNoAi(true);
        });
        if(ready==570){
            for(var entity:client.level.entitiesForRendering())if(entity.getType()==RoyaleSpells.SKELETON&&!net.minecraft.client.renderer.entity.SkeletonRenderer.class.isInstance(client.getEntityRenderDispatcher().getRenderer(entity)))
                throw new IllegalStateException("Skeleton must use the restored vanilla renderer");
            shot(client,"royale-troops-lineup.png","ROYALE_TROOP_MODELS_COMPLETE");
        }
        if(ready==580)camera(client,113,151,-5,0,9);
        if(ready==615)shot(client,"royale-remade-troops.png","ROYALE_REMADE_MODELS_COMPLETE");
        if(ready==625)camera(client,106,151,-3.5,-49,11);
        if(ready==650)shot(client,"royale-barbarian-side.png","ROYALE_WEAPON_IDLE_COMPLETE");
        if(ready==660){
            camera(client,114,151,-6,53,11);
            client.getSingleplayerServer().execute(()->{
                var world=client.getSingleplayerServer().overworld();var barbarian=(Mob)world.getEntity(barbarianId);
                var target=EntityType.IRON_GOLEM.create(world);target.setPos(barbarian.position().add(0,0,-4));target.setNoAi(true);world.addFreshEntity(target);
                barbarian.setNoGravity(false);barbarian.setNoAi(false);barbarian.setTarget(target);
            });
        }
        if(ready>660&&ready<730&&!attackShot)for(var entity:client.level.entitiesForRendering())if(entity.getUUID().equals(barbarianId)&&entity instanceof Mob mob&&mob.getAttackAnim(0)>.4f){
            attackShot=true;shot(client,"royale-barbarian-attack.png","ROYALE_WEAPON_ATTACK_COMPLETE");
        }
        if(ready==735){
            if(!attackShot)throw new IllegalStateException("No real Barbarian attack animation captured");
            client.getSingleplayerServer().execute(()->{
                var world=client.getSingleplayerServer().overworld();var mob=world.getEntity(barbarianId);
                mob.hurt(world.damageSources().generic(),1000);
            });
        }
        if(ready==745)camera(client,142,153,-9,0,9);
        if(ready==780)shot(client,"royale-barbarian-hut.png","ROYALE_HUT_VISUAL_COMPLETE");
        if(ready==795)client.getSingleplayerServer().execute(()->{
            var world=client.getSingleplayerServer().overworld();var player=client.getSingleplayerServer().getPlayerList().getPlayers().get(0);
            player.teleportTo(world,160,150,0,180,10);player.getInventory().clearContent();
            for(int i=0;i<2;i++){
                var at=new Vec3(160,151.3+i*.18,0);var fx=SpellEntity.create(world,i==0?Spell.FREEZE:Spell.RAGE,player.getUUID(),at,at);fx.preview=true;fx.setPreviewTime(20);world.addFreshEntity(fx);
            }
        });
        if(ready==810)client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        if(ready==825)RangeDepthAudit.requested=true;
        if(ready==845){if(!RangeDepthAudit.complete)throw new IllegalStateException("Depth audit never ran");shot(client,"royale-range-third-front.png","ROYALE_RANGE_FRONT_COMPLETE");}
        if(ready==855)client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if(ready==880)shot(client,"royale-range-third-back.png","ROYALE_RANGE_BACK_COMPLETE");
        if(ready>=900)RevisionVisualSmoke.tick(client,ready);
    }
    private static void camera(Minecraft client,double x,double y,double z,float yaw,float pitch){client.getSingleplayerServer().execute(()->client.getSingleplayerServer().getPlayerList().getPlayers().get(0).teleportTo(client.getSingleplayerServer().overworld(),x,y,z,yaw,pitch));}
    private static void shot(Minecraft client,String name,String marker){Screenshot.grab(client.gameDirectory,name,client.getMainRenderTarget(),message->System.out.println(marker));}
}
