package dev.royalespells.client;

import dev.royalespells.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.function.Consumer;

/** Opt-in regression scenes for the reported tree/step reticle distortion. */
final class TargetPreviewSmoke {
    private record Scene(String name,Vec3 eye,Vec3 target,Spell card,int fov,CameraType camera) {}
    private static final Vec3 STEP=new Vec3(-7.02,152.05,1.98),TREE=new Vec3(8.8,155.05,.8);
    private static final List<Scene> SCENES=List.of(
        scene("steps-near",new Vec3(-8,157,-4),STEP,Spell.VOID),
        scene("steps-far",new Vec3(-8,163,-16),STEP,Spell.VOID),
        scene("steps-side",new Vec3(2,158,2),STEP,Spell.VOID),
        scene("steps-reverse",new Vec3(-8,158,12),STEP,Spell.VOID),
        scene("canopy-near",new Vec3(8,160,-6),TREE,Spell.VOID),
        scene("canopy-far",new Vec3(8,164,-16),TREE,Spell.VOID),
        scene("canopy-side",new Vec3(17,160,0),TREE,Spell.VOID),
        new Scene("canopy-wide-fov",new Vec3(8,160,-6),TREE,Spell.VOID,100,CameraType.FIRST_PERSON),
        new Scene("canopy-third-person",new Vec3(8,160,-6),TREE,Spell.VOID,70,CameraType.THIRD_PERSON_BACK),
        scene("flat-zap",new Vec3(-8,155,-13),new Vec3(-8,150,-8),Spell.ZAP),
        scene("evolved-zap",new Vec3(-8,155,-13),new Vec3(-8,150,-8),Spell.ZAP_EVOLUTION),
        scene("two-landings",new Vec3(8,163,-11),TREE,Spell.GOBLIN_BARREL_EVOLUTION),
        scene("log-over-steps",new Vec3(-8,153,-6),STEP,Spell.THE_LOG),
        scene("barrel-over-steps",new Vec3(-8,153,-6),STEP,Spell.BARBARIAN_BARREL));
    private static Scene scene(String name,Vec3 eye,Vec3 target,Spell card){return new Scene(name,eye,target,card,70,CameraType.FIRST_PERSON);}
    private static volatile boolean setupDone;
    private static boolean setupQueued,ready,auditRequested;
    private static int tick,waiting,audits;
    private static FloatBuffer before;
    private static void server(Minecraft c,Consumer<ServerPlayer> action){c.getSingleplayerServer().execute(()->action.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst()));}
    private static void pose(ServerPlayer p,Scene s) {
        Vec3 look=s.target.subtract(s.eye);float yaw=(float)Math.toDegrees(Math.atan2(-look.x,look.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(look.y,Math.hypot(look.x,look.z)));
        p.teleportTo(p.serverLevel(),s.eye.x,s.eye.y-p.getEyeHeight(),s.eye.z,yaw,pitch);
        p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();p.getInventory().selected=0;
        p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(RoyaleSpells.ITEMS.get(s.card)));
    }
    static void tick(Minecraft c) {
        if(!setupQueued) {
            setupQueued=true;c.options.setCameraType(CameraType.FIRST_PERSON);c.options.hideGui=false;
            server(c,p->{var w=p.serverLevel();for(var e:com.google.common.collect.ImmutableList.copyOf(w.getAllEntities()))if(e!=p)e.discard();
                for(int x=-22;x<=22;x++)for(int z=-22;z<=18;z++) {
                    w.setBlockAndUpdate(new BlockPos(x,149,z),Blocks.GRASS_BLOCK.defaultBlockState());
                    for(int y=150;y<=167;y++)w.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
                }
                for(int dx=-5;dx<=5;dx++)for(int dz=-5;dz<=5;dz++) {
                    int top=149+Math.clamp(Math.floorDiv(dx+dz+6,3),0,4);
                    for(int y=150;y<=top;y++)w.setBlockAndUpdate(new BlockPos(-8+dx,y,dz),(y==top?Blocks.GRASS_BLOCK:Blocks.DIRT).defaultBlockState());
                }
                for(int y=150;y<=154;y++)w.setBlockAndUpdate(new BlockPos(8,y,0),Blocks.ACACIA_LOG.defaultBlockState());
                for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++) {
                    int distance=Math.max(Math.abs(dx),Math.abs(dz)),top=distance<=1?155:distance<=2?154:153;
                    for(int y=153;y<=top;y++)w.setBlockAndUpdate(new BlockPos(8+dx,y,dz),Blocks.ACACIA_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true));
                }
                pose(p,SCENES.getFirst());setupDone=true;System.out.println("ROYALE_TARGET_SETUP_READY");
            });return;
        }
        if(!ready) {
            if(!setupDone || !c.player.getMainHandItem().is(RoyaleSpells.ITEMS.get(Spell.VOID)) || !c.level.getBlockState(new BlockPos(8,155,0)).is(Blocks.ACACIA_LEAVES)) {
                if(++waiting>1200)throw new IllegalStateException("Target QA terrain did not synchronize");return;
            }
            ready=true;
        }
        int sceneIndex=tick/55,phase=tick++%55;
        if(sceneIndex<SCENES.size()) {
            var scene=SCENES.get(sceneIndex);
            if(phase==0){c.options.fov().set(scene.fov);c.options.setCameraType(scene.camera);server(c,p->pose(p,scene));}
            if(phase==30)auditRequested=true;
            if(phase==42){verifyGeometry(scene);shot(c,scene.name);}
            return;
        }
        int end=tick-SCENES.size()*55;
        if(end==1){c.options.setCameraType(CameraType.FIRST_PERSON);c.options.hideGui=true;}
        if(end==10 && (!TargetPreview.contours.isEmpty()))throw new IllegalStateException("F1 retained target overlay");
        if(end==11){c.options.hideGui=false;server(c,p->p.getInventory().clearContent());}
        if(end==30){
            if(!TargetPreview.contours.isEmpty())throw new IllegalStateException("Empty hand retained stale target overlay");
            if(audits<SCENES.size())throw new IllegalStateException("Not all target depth audits ran: "+audits);
            System.out.println("ROYALE_TARGET_PREVIEW_COMPLETE scenes="+SCENES.size()+" depthAudits="+audits+" hideGui=true emptyHand=true");c.stop();
        }
    }
    private static void shot(Minecraft c,String name) {
        Screenshot.grab(c.gameDirectory,"target-"+name+".png",c.getMainRenderTarget(),message->{});
        System.out.println("ROYALE_TARGET_FRAME "+name+" contours="+TargetPreview.contours.size()+" vanillaCrosshair=true gui="+c.getWindow().getGuiScaledWidth()+"x"+c.getWindow().getGuiScaledHeight());
    }
    private static void verifyGeometry(Scene scene) {
        var shapes=TargetPreview.contours;if(shapes.isEmpty())throw new IllegalStateException("Missing target shape: "+scene.name);
        for(var shape:shapes) {
            double y=shape.points().getFirst().y;
            for(var point:shape.points())if(Math.abs(point.y-y)>1e-8)throw new IllegalStateException("Target contour bent across terrain: "+scene.name);
        }
        if(!scene.card.rolling()) {
            double expected=scene.card==Spell.ZAP_EVOLUTION?Spell.ZAP.radius:scene.card.radius;
            var circle=shapes.getFirst().points();double cx=0,cz=0;
            for(int i=0;i<circle.size()-1;i++){cx+=circle.get(i).x;cz+=circle.get(i).z;}cx/=circle.size()-1;cz/=circle.size()-1;
            for(var point:circle)if(Math.abs(Math.hypot(point.x-cx,point.z-cz)-expected)>1e-6)throw new IllegalStateException("Target radius changed: "+scene.name);
            if(scene.card==Spell.GOBLIN_BARREL_EVOLUTION && (shapes.size()!=2))throw new IllegalStateException("Missing secondary landing point");
        }
    }
    static void beforeDraw() {
        if(!auditRequested)return;var target=Minecraft.getInstance().getMainRenderTarget();
        before=BufferUtils.createFloatBuffer(target.width*target.height);
        GL11.glReadPixels(0,0,target.width,target.height,GL11.GL_DEPTH_COMPONENT,GL11.GL_FLOAT,before);
    }
    static void afterDraw() {
        if(before==null)return;var target=Minecraft.getInstance().getMainRenderTarget();var after=BufferUtils.createFloatBuffer(before.capacity());
        GL11.glReadPixels(0,0,target.width,target.height,GL11.GL_DEPTH_COMPONENT,GL11.GL_FLOAT,after);
        for(int i=0;i<before.capacity();i++)if(before.get(i)!=after.get(i))throw new IllegalStateException("Target preview modified scene depth at "+i);
        System.out.println("ROYALE_TARGET_DEPTH_PRESERVED pixels="+before.capacity());before=null;auditRequested=false;audits++;
    }
    private TargetPreviewSmoke(){}
}
