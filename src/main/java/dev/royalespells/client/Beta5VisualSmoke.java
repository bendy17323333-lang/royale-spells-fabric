// SPDX-License-Identifier: MIT
package dev.royalespells.client;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;
import java.util.*;

/** Explicit opt-in, disposable world. Captures actual game frames and checks the
 * creative transaction on the integrated server. Never installs into user saves. */
public final class Beta5VisualSmoke {
    private static int stage,ticks,ready;
    private static long started;
    private static final Set<String> frames=new LinkedHashSet<>();
    private static volatile boolean levelSynced;
    private static volatile int flightEntityId=-1;
    private static int singleSounds,groupSounds,modelFrames;
    public static void install(){
        started=System.currentTimeMillis();
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e)->tick(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((RenderLevelStageEvent e)->{
            var c=Minecraft.getInstance();if(c.level==null||e.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
            for(var entity:c.level.entitiesForRendering())if(entity instanceof SpellEntity fx&&fx.spell()==Spell.GIANT_SNOWBALL_EVOLUTION){
                modelFrames++;
                if(fx.getId()==flightEntityId){
                    // Entity metadata can arrive in multi-tick batches. Use
                    // disjoint phase windows, not a two-tick screenshot race.
                    int t=fx.time();if(t>=6&&t<15)snowShot(c,"flight",t);if(t>=15&&t<24)snowShot(c,"descent",t);
                    if(t>=24&&t<31)snowShot(c,"landing",t);if(t>=31&&t<38)snowShot(c,"rolling",t);if(t>=38&&t<42)snowShot(c,"before-release",t);
                }
            }
        });
    }
    private static void shot(Minecraft c,String name){if(frames.add(name))Screenshot.grab(c.gameDirectory,"beta5-"+name+".png",c.getMainRenderTarget(),m->System.out.println("BETA5_FRAME "+name));}
    private static void snowShot(Minecraft c,String name,int age){if(!frames.contains(name))System.out.println("BETA5_SNOW_SAMPLE name="+name+" entity="+flightEntityId+" age="+age);shot(c,name);}
    private static void tick(Minecraft c){
        ticks++;c.options.pauseOnLostFocus=false;
        if(System.currentTimeMillis()-started>300000){System.out.println("BETA5_VISUAL_TIMEOUT stage="+stage+" tick="+ready);c.stop();return;}
        if(stage==0&&ticks>35&&c.screen instanceof TitleScreen&&c.getOverlay()==null){
            stage=1;c.options.renderDistance().set(5);c.options.fov().set(50);c.options.framerateLimit().set(60);
            var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
            String name="Beta5-Disposable-QA-"+System.currentTimeMillis();
            c.createWorldOpenFlows().createFreshLevel(name,new LevelSettings(name,GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(194005L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),new TitleScreen());
        }else if(stage==1&&c.player!=null&&c.level!=null&&c.getSingleplayerServer()!=null){
            stage=2;
            c.getSoundManager().addListener((sound,set,range)->{
                String id=sound.getLocation().toString();if(id.equals("royalespells:spell_void_strike_single"))singleSounds++;
                if(id.equals("royalespells:spell_void_strike"))groupSounds++;
            });
            // Exercise Minecraft's actual Vorbis decoder, not only an external probe.
            try(var in=c.getResourceManager().open(RoyaleSpells.id("sounds/original/void_single_strike.ogg"));var ogg=new net.minecraft.client.sounds.JOrbisAudioStream(in)){
                var format=ogg.getFormat();var pcm=ogg.readAll();if(format.getChannels()!=1||pcm.remaining()==0)throw new IllegalStateException("Invalid Void audio");
                System.out.println("BETA5_VOID_DECODE_OK bytes="+pcm.remaining()+" rate="+format.getFrameRate());
            }catch(java.io.IOException ex){throw new RuntimeException(ex);}
            server(c,()->{
                var w=world(c);w.setDayTime(6000);for(int x=-16;x<=16;x++)for(int z=-16;z<=16;z++)w.setBlockAndUpdate(new BlockPos(x,79,z),Blocks.SMOOTH_STONE.defaultBlockState());
                var p=player(c);p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(RoyaleSpells.ITEMS.get(Spell.ZAP)));
                camera(c,new Vec3(5,84,-7),new Vec3(0,81,0));
            });
        }else if(stage==2&&c.player!=null){
            ready++;if(c.screen instanceof net.minecraft.client.gui.screens.PauseScreen)c.setScreen(null);
            if(ready==40)KeyMapping.click(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(org.lwjgl.glfw.GLFW.GLFW_KEY_G));
            if(ready==50){if(!(c.screen instanceof CardLevelScreen))throw new IllegalStateException("G did not open card levels");press(c,"16");}
            if(ready==65)shot(c,"creative-level-picker");
            if(ready==75)press(c,Component.translatable("gui.done").getString());
            if(ready==90)server(c,()->{levelSynced=CardLevels.get(player(c).getMainHandItem())==16;System.out.println("BETA5_CREATIVE_SERVER_LEVEL "+CardLevels.get(player(c).getMainHandItem()));});
            if(ready==100){
                c.setScreen(new CreativeModeInventoryScreen(c.player,c.player.connection.enabledFeatures(),true));
                try{var method=CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab",CreativeModeTab.class);method.setAccessible(true);method.invoke(c.screen,BuiltInRegistries.CREATIVE_MODE_TAB.get(RoyaleSpells.id("spells")));}
                catch(ReflectiveOperationException ex){throw new RuntimeException("Creative tab selection failed",ex);}
            }
            if(ready==120)shot(c,"creative-cards-top");
            if(ready==130&&c.screen instanceof CreativeModeInventoryScreen screen){screen.mouseScrolled(screen.width/2.0,screen.height/2.0,0,-20);}
            if(ready==145)shot(c,"creative-mineclash-cards");
            if(ready==160){c.setScreen(null);c.options.hideGui=true;server(c,()->{player(c).setGameMode(GameType.SPECTATOR);model(c,0);});}
            if(ready==195)shot(c,"snow-front");
            if(ready==210)server(c,()->model(c,1));
            if(ready==245)shot(c,"snow-side");
            if(ready==260)server(c,()->model(c,2));
            if(ready==295)shot(c,"snow-back");
            if(ready==320)server(c,()->model(c,3));
            if(ready==345)shot(c,"snow-angle");
            if(ready==360)server(c,()->{
                clean(c);camera(c,new Vec3(12,88,-14),new Vec3(0,85,0));
                for(int i=0;i<2;i++){var m=EntityType.HUSK.create(world(c));m.setPos(i*.9,80,.2);m.setNoAi(true);world(c).addFreshEntity(m);}
                var fx=SpellEntity.create(world(c),Spell.GIANT_SNOWBALL_EVOLUTION,player(c).getUUID(),new Vec3(0,81,-7),new Vec3(0,80,0));fx.setCastDirection(new Vec3(0,0,1));flightEntityId=fx.getId();world(c).addFreshEntity(fx);
            });
            if(ready==412)shot(c,"after-release");
            if(ready==440)server(c,()->voidScene(c,0));
            if(ready==525)server(c,()->voidScene(c,1));
            if(ready==610)server(c,()->voidScene(c,3));
            if(ready==695){
                boolean pass=levelSynced&&singleSounds==3&&groupSounds==3&&frames.containsAll(List.of("flight","descent","landing","rolling","before-release","after-release","creative-level-picker","snow-angle"));
                System.out.println("BETA5_VISUAL_"+(pass?"PASS":"FAIL")+" frames="+frames+" renderedModelFrames="+modelFrames+" levelSynced="+levelSynced+" single="+singleSounds+" group="+groupSounds);stage=3;c.stop();
            }
        }
    }
    private static void press(Minecraft c,String text){for(var child:c.screen.children())if(child instanceof Button b&&b.getMessage().getString().equals(text)){b.onPress();return;}throw new IllegalStateException("Button missing: "+text);}
    private static void server(Minecraft c,Runnable run){c.getSingleplayerServer().execute(run);}
    private static ServerLevel world(Minecraft c){return c.getSingleplayerServer().overworld();}
    private static ServerPlayer player(Minecraft c){return c.getSingleplayerServer().getPlayerList().getPlayers().getFirst();}
    private static void clean(Minecraft c){for(var e:com.google.common.collect.ImmutableList.copyOf(world(c).getAllEntities()))if(e instanceof SpellEntity||e instanceof Mob)e.discard();}
    private static void camera(Minecraft c,Vec3 eye,Vec3 toward){var d=toward.subtract(eye);var p=player(c);p.teleportTo(world(c),eye.x,eye.y-p.getEyeHeight(),eye.z,(float)Math.toDegrees(Math.atan2(d.z,d.x))-90,(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));p.getAbilities().flying=true;p.onUpdateAbilities();}
    private static void model(Minecraft c,int view){
        clean(c);var at=new Vec3(0,81,0);var e=SpellEntity.create(world(c),Spell.GIANT_SNOWBALL_EVOLUTION,player(c).getUUID(),at,at.add(0,0,-1.333333));e.setCastDirection(new Vec3(0,0,1));e.preview=true;e.setPreviewTime(30);e.setPos(e.visualPosition(0));world(c).addFreshEntity(e);
        Vec3[] eyes={new Vec3(0,83,-4.5),new Vec3(4.5,83,0),new Vec3(0,83,4.5),new Vec3(3.4,84,-4)};camera(c,eyes[view],new Vec3(0,82.2,0));
    }
    private static void voidScene(Minecraft c,int count){clean(c);var at=new Vec3(0,80,0);camera(c,new Vec3(6,84,-7),at.add(0,1,0));for(int i=0;i<count;i++){var m=EntityType.IRON_GOLEM.create(world(c));m.setPos(at.add(i*.65,0,0));m.setNoAi(true);world(c).addFreshEntity(m);}world(c).addFreshEntity(SpellEntity.create(world(c),Spell.VOID,player(c).getUUID(),at,at));}
    private Beta5VisualSmoke(){}
}
