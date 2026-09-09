package dev.mineclash.zappies.client;

import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import com.google.gson.GsonBuilder;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.worldselection.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.liziyowo.mineclash.CREntities;
import org.liziyowo.mineclash.entity.mob.*;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

/** A fresh independent world, native actors, actual controller and bone output. */
public final class AnimationProbe {
    public static final boolean ENABLED=Boolean.getBoolean("zappiesaddon.smoke");
    private static int boot,stage,tick;private static volatile boolean ready;
    private static final List<UUID> cars=new ArrayList<>(),muskets=new ArrayList<>(),skeletons=new ArrayList<>();
    private static final List<Map<String,Object>> observations=new ArrayList<>();
    private static final Map<Integer,Integer> sampled=new HashMap<>();
    public static void install(){NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e)->{
        var c=Minecraft.getInstance();try{run(c);}catch(Throwable ex){ex.printStackTrace();System.err.println("ZAPPIES_QA_FAILURE");c.stop();}
    });}
    static void server(Minecraft c,Consumer<ServerPlayer> action){c.getSingleplayerServer().execute(()->{try{action.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst());}catch(Throwable ex){ex.printStackTrace();System.err.println("ZAPPIES_QA_FAILURE");c.execute(c::stop);}});}
    private static void look(ServerPlayer p,Vec3 eye,Vec3 at){var d=at.subtract(eye);p.teleportTo(p.serverLevel(),eye.x,eye.y-p.getEyeHeight(),eye.z,(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))));}
    static void shot(Minecraft c,String name){Screenshot.grab(c.gameDirectory,"zappies-"+name+".png",c.getMainRenderTarget(),m->{});System.out.println("ZAPPIES_QA_FRAME "+name);}
    private static void run(Minecraft c)throws Exception{
        boot++;c.options.pauseOnLostFocus=false;
        if(stage==0&&boot>50&&c.screen instanceof TitleScreen){stage=1;CreateWorldScreen.openFresh(c,c.screen);}
        else if(stage==1&&c.screen instanceof CreateWorldScreen s){var u=s.getUiState();u.setName("Zappies isolated diagnostics");u.setSeed("328174");u.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);u.setAllowCommands(true);u.setDifficulty(Difficulty.NORMAL);u.setGenerateStructures(false);
            for(var child:s.children())if(child instanceof Button b&&b.getMessage().getString().equals(Component.translatable("selectWorld.create").getString())){stage=2;b.onPress();break;}
        }else if(stage==2&&c.screen instanceof ConfirmScreen s){String title=s.getTitle().getString();if(!title.equals(Component.translatable("selectWorld.warning.experimental.title").getString())&&!title.equals(Component.translatable("selectWorld.warning.deprecated.title").getString()))throw new IllegalStateException("Unexpected world confirmation");for(var child:s.children())if(child instanceof Button b&&b.getMessage().getString().equals(Component.translatable("gui.yes").getString())){b.onPress();break;}}
        else if(stage==2&&c.level!=null&&c.player!=null&&c.getSingleplayerServer()!=null){stage=3;c.options.hideGui=true;
            server(c,p->{var w=p.serverLevel();w.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.server);w.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);w.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,p.server);w.setDayTime(6000);w.setWeatherParameters(0,0,false,false);
                for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++){w.setBlockAndUpdate(new BlockPos(x,149,z),Blocks.SMOOTH_STONE.defaultBlockState());for(int y=150;y<158;y++)w.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
                for(int x=-1;x<=0;x++)for(int z=-1;z<=0;z++)w.setChunkForced(x,z,true);
                if(BattleShowcase.ENABLED){BattleShowcase.prepare(p);ready=true;return;}
                for(int i=0;i<3;i++){var e=CREntities.ZAPPIES.get().create(w);e.moveTo((i-1)*2.5,150,0,180,0);e.setYBodyRot(180);e.setYHeadRot(180);e.setNoAi(true);e.setCustomName(Component.literal("Z"+i));w.addFreshEntity(e);cars.add(e.getUUID());}
                for(int i=0;i<2;i++){var e=CREntities.ELITE_MUSKETEER.get().create(w);e.moveTo((i-.5)*3,150,4,180,0);e.setYBodyRot(180);e.setYHeadRot(180);e.setNoAi(true);e.setCustomName(Component.literal("M"+i));w.addFreshEntity(e);muskets.add(e.getUUID());}
                for(int i=0;i<3;i++){var e=CREntities.CLASH_SKELETON.get().create(w);e.setEvolved(false);e.moveTo((i-1)*2.5,150,-3.5,180,0);e.setYBodyRot(180);e.setYHeadRot(180);e.setNoAi(true);e.setCustomName(Component.literal("S"+i));w.addFreshEntity(e);skeletons.add(e.getUUID());}
                p.getAbilities().flying=true;p.onUpdateAbilities();look(p,new Vec3(8,154.8,-9),new Vec3(0,151,1));ready=true;
            });
        }
        if(stage!=3||!ready)return;if(c.screen instanceof PauseScreen)c.setScreen(null);
        if(c.getMainRenderTarget().width<1200){if(tick%20==0){org.lwjgl.glfw.GLFW.glfwRestoreWindow(c.getWindow().getWindow());c.getWindow().setWindowed(1600,1000);c.resizeDisplay();}return;}
        tick++;c.getToasts().clear();
        if(BattleShowcase.ENABLED){BattleShowcase.tick(c);return;}
        // The diagnostic has a fixed camera even when the user moves the mouse in
        // their own game. No input is sent to any existing game window.
        c.mouseHandler.releaseMouse();var direction=new Vec3(0,151,1).subtract(new Vec3(8,154.8,-9));
        c.player.setYRot((float)Math.toDegrees(Math.atan2(-direction.x,direction.z)));
        c.player.setXRot((float)-Math.toDegrees(Math.atan2(direction.y,Math.hypot(direction.x,direction.z))));
        c.player.yRotO=c.player.getYRot();c.player.xRotO=c.player.getXRot();
        if(ElectricalProbe.ENABLED){
            ElectricalProbe.tick(c,tick,cars,muskets,skeletons);
            if(tick==279)Files.writeString(c.gameDirectory.toPath().resolve("animation-isolation.json"),new GsonBuilder().setPrettyPrinting().create().toJson(observations));
            if(tick==700){Files.writeString(c.gameDirectory.toPath().resolve("animation-observations.json"),new GsonBuilder().setPrettyPrinting().create().toJson(observations));System.out.println("ZAPPIES_QA_COMPLETE observations="+observations.size());c.stop();}
            return;
        }
        if(tick==100)shot(c,"baseline-alive");
        if(tick==120)server(c,p->{var e=(LivingEntity)p.serverLevel().getEntity(cars.getFirst());e.hurt(e.damageSources().genericKill(),10000);System.out.println("ZAPPIES_QA_KILLED_ONE "+e.getId());});
        if(tick==120)server(c,p->{var e=(ClashSkeleton)p.serverLevel().getEntity(skeletons.getFirst());e.triggerAnim("attack","attack_1");System.out.println("ZAPPIES_QA_ONE_SKELETON_ATTACK "+e.getId());});
        if(tick==130||tick==145||tick==165||tick==200)shot(c,"one-car-death-"+tick);
        if(tick==225)server(c,p->{var e=(LivingEntity)p.serverLevel().getEntity(muskets.getFirst());e.hurt(e.damageSources().genericKill(),10000);});
        if(tick==235||tick==250||tick==280)shot(c,"one-musket-death-"+tick);
        if(tick>=310&&tick<=360)server(c,p->{var e=p.serverLevel().getEntity(cars.get(1));e.setDeltaMovement(.035,0,0);});
        if(tick==340||tick==390)shot(c,"moving-then-idle-"+tick);
        if(tick==410){Files.writeString(c.gameDirectory.toPath().resolve("animation-observations.json"),new GsonBuilder().setPrettyPrinting().create().toJson(observations));System.out.println("ZAPPIES_QA_COMPLETE observations="+observations.size());c.stop();}
    }
    public static void observe(GeoAnimatable a,long id,GeoModel<?> model){
        if(!ENABLED||tick<90||tick>(ElectricalProbe.ENABLED?700:405)||!(a instanceof Zappies||a instanceof EliteMusketeer||a instanceof ClashSkeleton))return;
        Entity e=(Entity)a;if(sampled.getOrDefault(e.getId(),-1)==tick||!ElectricalProbe.ENABLED&&tick%3!=0)return;sampled.put(e.getId(),tick);
        var manager=a.getAnimatableInstanceCache().getManagerForId(id);Map<String,Object> out=new LinkedHashMap<>();out.put("tick",tick);out.put("id",e.getId());out.put("instance_id",id);out.put("name",e.getName().getString());out.put("health",((LivingEntity)e).getHealth());out.put("deathTime",((LivingEntity)e).deathTime);out.put("cache_identity",System.identityHashCode(a.getAnimatableInstanceCache()));
        out.put("animation_time",manager.getLastUpdateTime());out.put("paused",dev.mineclash.zappies.pause.ElectricPause.active((LivingEntity)e));out.put("world_time",e.level().getGameTime());
        Map<String,Object> controls=new LinkedHashMap<>();manager.getAnimationControllers().forEach((name,ctrl)->controls.put(name,Map.of("state",ctrl.getAnimationState().toString(),"raw",stages(ctrl.getCurrentRawAnimation()),"trigger",stages(ctrl.getTriggeredAnimation()))));out.put("controllers",controls);
        Map<String,Object> bones=new LinkedHashMap<>();for(var b:model.getAnimationProcessor().getRegisteredBones())bones.put(b.getName(),List.of(b.getPosX(),b.getPosY(),b.getPosZ(),b.getRotX(),b.getRotY(),b.getRotZ(),b.getScaleX(),b.getScaleY(),b.getScaleZ()));out.put("bones",bones);observations.add(out);
    }
    private static List<String> stages(software.bernie.geckolib.animation.RawAnimation a){return a==null?List.of():a.getAnimationStages().stream().map(s->s.animationName()).toList();}
}
