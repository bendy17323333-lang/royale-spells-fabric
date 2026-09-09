package dev.royalespells.client;

import dev.royalespells.*;
import dev.royalespells.entity.SpellEntity;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.nio.file.*;
import java.util.function.Consumer;
import com.google.gson.GsonBuilder;

/** Opt-in isolated demonstration. Real spell entities, real damage and effects;
 * no screen input and no access to user saves. Stable framing precedes export. */
public final class VisualUpdateRecording {
    public static final boolean ENABLED=Boolean.getBoolean("royalespells.snowballSmoke");
    private static final Vec3 EYE=new Vec3(7.5,154.2,-8.5),FOCUS=new Vec3(0,151,.5);
    private static final List<LivingEntity> actors=new ArrayList<>();
    private static final List<Map<String,Object>> observations=new ArrayList<>(),scenes=new ArrayList<>();
    private static final Map<Integer,Long> lastSamples=new HashMap<>();
    private static final Path dir=Minecraft.getInstance().gameDirectory.toPath();
    private static int tick,wait,finishedAt=-1;private static volatile boolean ready;private static boolean initialized;
    private static void server(Minecraft c,Consumer<ServerPlayer> action){c.getSingleplayerServer().execute(()->{try{action.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst());}catch(Throwable ex){ex.printStackTrace();System.err.println("ROYALE_VISUAL_QA_FAILURE");c.execute(c::stop);}});}
    private static void camera(ServerPlayer p){var d=FOCUS.subtract(EYE);p.teleportTo(p.serverLevel(),EYE.x,EYE.y-p.getEyeHeight(),EYE.z,(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))));}
    static void tick(Minecraft c){try{run(c);}catch(Throwable ex){ex.printStackTrace();System.err.println("ROYALE_VISUAL_QA_FAILURE");c.stop();}}
    private static void run(Minecraft c)throws Exception {
        c.options.hideGui=true;c.options.fov().set(55);c.options.pauseOnLostFocus=false;c.getToasts().clear();c.mouseHandler.releaseMouse();
        var d=FOCUS.subtract(EYE);c.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));c.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))));c.player.yRotO=c.player.getYRot();c.player.xRotO=c.player.getXRot();
        if(!initialized){initialized=true;
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(VisualFrameRecorder::render);c.getSoundManager().addListener(new VisualAudioRecorder());
            server(c,p->{var w=p.serverLevel();w.setWeatherParameters(0,0,false,false);w.setDayTime(18000);
                p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,6000,0,false,false));
                for(int x=-1;x<=0;x++)for(int z=-1;z<=0;z++)w.setChunkForced(x,z,true);
                for(int x=-14;x<=14;x++)for(int z=-14;z<=14;z++){boolean border=Math.abs(x)==14||Math.abs(z)==14;w.setBlockAndUpdate(new BlockPos(x,149,z),(border?Blocks.POLISHED_DEEPSLATE:((x+z)&1)==0?Blocks.SMOOTH_SANDSTONE:Blocks.CUT_SANDSTONE).defaultBlockState());}
                p.getInventory().clearContent();camera(p);ready=true;});return;
        }
        if(!ready||++wait<65)return;
        if(c.getMainRenderTarget().width<1200){if(wait%20==0){org.lwjgl.glfw.GLFW.glfwRestoreWindow(c.getWindow().getWindow());c.getWindow().setWindowed(1280,800);c.resizeDisplay();}return;}
        tick++;
        if(finishedAt>=0){if(tick-finishedAt>120){System.out.println("ROYALE_VISUAL_UPDATE_COMPLETE samples="+observations.size());c.stop();}return;}
        VisualFrameRecorder.start();VisualAudioRecorder.start();VisualAudioRecorder.tick();
        if(tick==1){scene("普通雪球：命中组变蓝并减速，对照组保持正常");server(c,p->{actor(p,"minecraft:skeleton",-3,0,"命中 · 原版骷髅");actor(p,"mineclash:skeleton",-3,2,"命中 · MineClash");actor(p,"minecraft:skeleton",3,0,"对照 · 原版骷髅");actor(p,"mineclash:skeleton",3,2,"对照 · MineClash");});}
        if(tick<220&&tick%24==0)server(c,p->actors.forEach(VisualUpdateRecording::swing));
        if(tick==55)server(c,p->cast(p,Spell.GIANT_SNOWBALL,new Vec3(-3,152,-8),new Vec3(-3,150,1)));
        if(tick==93)server(c,p->{for(int i=0;i<2;i++)SpellEngine.electricStun(actors.get(i),10);});
        if(tick==160)scene("雪球减速结束：颜色和动画速度恢复，不串到旁边的生物");
        if(tick==220){scene("觉醒雪球：从西向东滚动，释放后进入蓝色减速");server(c,p->{clear();for(int i=0;i<3;i++)actor(p,"minecraft:skeleton",-2,(i-1)*1.2,"觉醒雪球目标");});}
        if(tick==260)server(c,p->cast(p,Spell.GIANT_SNOWBALL_EVOLUTION,new Vec3(-10,152,0),new Vec3(-2,150,0)));
        if(tick==380){scene("改变施法方向：觉醒雪球从南向北滚动");server(c,p->{clear();for(int i=0;i<3;i++)actor(p,"minecraft:skeleton",(i-1)*1.2,2,"反方向目标");});}
        if(tick==412)server(c,p->cast(p,Spell.GIANT_SNOWBALL_EVOLUTION,new Vec3(0,152,10),new Vec3(0,150,2)));
        if(tick==525){scene("火球：炽热核心、沿飞行弧线拖尾、命中爆散");server(c,p->{clear();actor(p,"minecraft:iron_golem",0,1,"火球目标");});}
        if(tick==560||tick==620||tick==680)server(c,p->cast(p,Spell.FIREBALL,tick==620?new Vec3(8,152,4):new Vec3(-7,152,-5),new Vec3(0,150,1)));
        if(tick==730){scene("毒药：贴地翻涌、上升毒雾与气泡破裂");server(c,p->{clear();actor(p,"minecraft:iron_golem",0,1,"毒药目标");cast(p,Spell.POISON,new Vec3(0,150,1),new Vec3(0,150,1));});}
        if(tick%2==0)for(var entity:c.level.entitiesForRendering())if(entity instanceof LivingEntity e&&e.hasCustomName()){
            var row=new LinkedHashMap<String,Object>();row.put("kind","state");row.put("tick",tick);row.put("world_time",e.level().getGameTime());row.put("name",e.getName().getString());row.put("id",e.getId());row.put("blue",VisualState.snowbound(e));row.put("paused",dev.royalespells.pause.ElectricPause.active(e));row.put("hp",e.getHealth());observations.add(row);
        }
        if(List.of(48,84,96,104,135,195,274,297,426,447,573,585,633,750,825).contains(tick))Screenshot.grab(c.gameDirectory,"visual-update-"+tick+".png",c.getMainRenderTarget(),msg->{});
        if(tick==930){
            VisualFrameRecorder.finish();VisualAudioRecorder.finish();finishedAt=tick;
            Files.writeString(dir.resolve("visual-observations.json"),new GsonBuilder().setPrettyPrinting().create().toJson(observations));
            Files.writeString(dir.resolve("visual-scenes.json"),new GsonBuilder().setPrettyPrinting().create().toJson(scenes));
            server(c,p->clear());
        }
    }
    private static void scene(String title){scenes.add(Map.of("tick",tick,"title",title,"epoch_ms",System.currentTimeMillis()));}
    private static void actor(ServerPlayer p,String type,double x,double z,String name){
        var key=ResourceLocation.parse(type);if(!BuiltInRegistries.ENTITY_TYPE.containsKey(key))throw new IllegalStateException("Missing visual QA type "+type);
        var entity=BuiltInRegistries.ENTITY_TYPE.get(key).create(p.serverLevel());
        if(!(entity instanceof Mob mob))throw new IllegalStateException("Missing visual QA mob "+type);
        mob.moveTo(x,150,z,180,0);mob.setYBodyRot(180);mob.setYHeadRot(180);mob.setNoAi(true);mob.setPersistenceRequired();mob.setCustomName(Component.literal(name));mob.setCustomNameVisible(true);
        if(type.contains("skeleton")){
            mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STONE_SWORD));
            // Durable demonstration targets survive repeated spells. Spell
            // damage, control durations and movement are never modified.
            mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(80);mob.setHealth(80);
        }
        if(type.equals("mineclash:skeleton"))try{mob.getClass().getMethod("setEvolved",boolean.class).invoke(mob,false);}catch(Exception ex){throw new IllegalStateException(ex);}
        p.serverLevel().addFreshEntity(mob);actors.add(mob);
    }
    private static void swing(LivingEntity e){
        if(e.getClass().getName().startsWith("org.liziyowo")){
            try{e.getClass().getMethod("triggerAnim",String.class,String.class).invoke(e,"attack","attack_1");}catch(Exception ex){throw new IllegalStateException(ex);}
        }else e.swing(net.minecraft.world.InteractionHand.MAIN_HAND,true);
    }
    private static void cast(ServerPlayer p,Spell spell,Vec3 from,Vec3 to){var e=SpellEntity.create(p.serverLevel(),spell,p.getUUID(),from,to);e.setCastDirection(to.subtract(from));p.serverLevel().addFreshEntity(e);}
    private static void clear(){actors.forEach(Entity::discard);actors.clear();}
    public static void observe(LivingEntity e,double input,double local,String kind){
        if(!ENABLED||tick<1||tick>220||!e.hasCustomName())return;
        long now=e.level().getGameTime();if(lastSamples.getOrDefault(e.getId(),-1L)==now)return;lastSamples.put(e.getId(),now);
        var row=new LinkedHashMap<String,Object>();row.put("kind",kind);row.put("tick",tick);row.put("world_time",now);row.put("id",e.getId());row.put("name",e.getName().getString());row.put("input",input);row.put("local",local);row.put("blue",VisualState.snowbound(e));row.put("paused",dev.royalespells.pause.ElectricPause.active(e));observations.add(row);
    }
    private VisualUpdateRecording(){}
}
