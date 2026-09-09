package dev.mineclash.zappies.client;

import dev.mineclash.zappies.*;
import dev.mineclash.zappies.pause.ElectricPause;
import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.liziyowo.mineclash.CREntities;
import org.liziyowo.mineclash.entity.mob.Zappies;

/** Explicit opt-in recording in a newly created diagnostic world only.
 * The director places opponents and assigns hostile targets; it never changes
 * damage, health, stun timing, animation or the outcome of either native AI.
 */
public final class BattleShowcase {
    private static final String KIND=System.getProperty("zappiesaddon.battle","");
    public static final boolean ENABLED=!KIND.isEmpty();
    private static final boolean DRAGON=KIND.equals("dragon");
    // Keep the actual listener inside native 16-block sound attenuation while
    // framing both deployment points and the dragon's real flight altitude.
    private static final Vec3 EYE=new Vec3(7.6,155.0,-7.8),FOCUS=new Vec3(0,151.6,.8);
    private static volatile boolean started,finished;
    private static int tick,finishAt=-1,exitClock;
    private static long startTime;
    private static List<Zappies> squad=List.of();
    private static Mob opponent;
    private static final List<Map<String,Object>> samples=new ArrayList<>();
    private static final Path dir=Minecraft.getInstance().gameDirectory.toPath();
    public static void prepare(ServerPlayer p){
        var w=p.serverLevel();
        for(int x=-14;x<=14;x++)for(int z=-14;z<=14;z++){
            boolean edge=Math.abs(x)==14||Math.abs(z)==14;
            w.setBlockAndUpdate(new BlockPos(x,149,z),(edge?Blocks.POLISHED_DEEPSLATE:((x+z)&1)==0?Blocks.SMOOTH_SANDSTONE:Blocks.CUT_SANDSTONE).defaultBlockState());
            for(int y=150;y<160;y++)w.setBlockAndUpdate(new BlockPos(x,y,z),(edge&&y==150?Blocks.STONE_BRICK_WALL:Blocks.AIR).defaultBlockState());
            if(z==-8&&Math.abs(x)<6)w.setBlockAndUpdate(new BlockPos(x,149,z),Blocks.CYAN_TERRACOTTA.defaultBlockState());
            if(z==8&&Math.abs(x)<6)w.setBlockAndUpdate(new BlockPos(x,149,z),Blocks.RED_TERRACOTTA.defaultBlockState());
        }
        p.getAbilities().flying=true;p.onUpdateAbilities();var d=FOCUS.subtract(EYE);
        p.teleportTo(w,EYE.x,EYE.y-p.getEyeHeight(),EYE.z,(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))));
    }
    public static void tick(Minecraft c)throws Exception{
        org.lwjgl.glfw.GLFW.glfwSetWindowTitle(c.getWindow().getWindow(),"Astra Zappies Showcase - "+KIND);
        c.mouseHandler.releaseMouse();var d=FOCUS.subtract(EYE);
        c.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));c.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))));
        c.player.yRotO=c.player.getYRot();c.player.xRotO=c.player.getXRot();c.options.fov().set(55);
        if(!Files.exists(dir.resolve("battle-ready.txt")))Files.writeString(dir.resolve("battle-ready.txt"),KIND);
        // Let the recorder attach to this exact window before any deployment.
        if(!Files.exists(dir.resolve("recording-start.txt")))Files.writeString(dir.resolve("recording-start.txt"),"game framebuffer and resolved audio events");
        if(finished){
            if(++exitClock>120){System.out.println("ZAPPIES_QA_COMPLETE recorded battle "+KIND);c.stop();}
            return;
        }
        BattleRecorder.start();
        BattleAudioRecorder.start();BattleAudioRecorder.tick();
        AnimationProbe.server(c,p->{try{serverTick(p);}catch(Exception e){throw new RuntimeException(e);}});
    }
    private static void serverTick(ServerPlayer p)throws Exception{
        var w=p.serverLevel();
        if(!started){started=true;startTime=w.getGameTime();}
        tick=(int)(w.getGameTime()-startTime);
        if(tick>=60&&opponent==null){
            squad=ZappySquad.spawn(w,new Vec3(0,150,-4.2),0,null,11);
            if(squad.size()!=3)throw new IllegalStateException("Incomplete squad deployment");
            opponent=DRAGON?(Mob)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("royalespells:inferno_dragon")).create(w):CREntities.PEKKA.get().create(w);
            opponent.moveTo(0,DRAGON?153.5:150,4.7,180,0);opponent.setYBodyRot(180);opponent.setYHeadRot(180);opponent.setPersistenceRequired();
            if(DRAGON)opponent.getClass().getMethod("setup",UUID.class,int.class,boolean.class).invoke(opponent,UUID.randomUUID(),3600,false);
            w.addFreshEntity(opponent);System.out.println("BATTLE_DEPLOY "+KIND+" hp="+opponent.getHealth());
        }
        if(opponent==null)return;
        var alive=squad.stream().filter(LivingEntity::isAlive).toList();
        // Equivalent to assigning opposing teams with the laboratory tool.
        // Retarget only after a death; movement and attack remain the mobs' own.
        if(opponent.isAlive())for(var car:alive)if(car.getTarget()!=opponent)car.setTarget(opponent);
        if(!alive.isEmpty()&&(opponent.getTarget()==null||!opponent.getTarget().isAlive()))opponent.setTarget(alive.stream().min(Comparator.comparingDouble(opponent::distanceToSqr)).orElseThrow());
        if(tick%2==0){
            var s=new LinkedHashMap<String,Object>();s.put("tick",tick);s.put("opponent_hp",opponent.getHealth());s.put("opponent_paused",ElectricPause.active(opponent));s.put("opponent_y",opponent.getY());
            if(DRAGON){s.put("heat",opponent.getClass().getMethod("heatTicks").invoke(opponent));s.put("lock",opponent.getClass().getMethod("beamTargetId").invoke(opponent));}
            s.put("cars",squad.stream().map(z->Map.of("hp",z.getHealth(),"shots",ZappyBrain.of(z).shots,"windup",ZappyBrain.of(z).windup,"x",z.getX(),"z",z.getZ())).toList());samples.add(s);
        }
        if(finishAt<0&&(!opponent.isAlive()||alive.isEmpty()||tick>2000))finishAt=tick+100;
        if(finishAt>=0&&tick>=finishAt&&!finished){
            var out=new LinkedHashMap<String,Object>();out.put("opponent",KIND);out.put("opponent_max_health",opponent.getMaxHealth());out.put("ticks",tick);out.put("winner",opponent.isAlive()?(alive.isEmpty()?KIND:"timeout"):"zappies");out.put("samples",samples);
            Files.writeString(dir.resolve("battle-evidence.json"),new GsonBuilder().setPrettyPrinting().create().toJson(out));
            BattleRecorder.finish();BattleAudioRecorder.finish();finished=true;
            System.out.println("BATTLE_COMPLETE "+KIND+" winner="+out.get("winner")+" ticks="+tick);
        }
    }
}
