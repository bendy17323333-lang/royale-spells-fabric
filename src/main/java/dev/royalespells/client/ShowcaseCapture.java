package dev.royalespells.client;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.world.*;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.gen.WorldPresets;
import java.nio.file.*;
import java.util.*;

/** Explicit offline map-building run; drives real item packets and exports an ordinary save. */
final class ShowcaseCapture {
    private static int ticks,stage,age,scene;
    private static final Set<String> SHOTS=new HashSet<>();
    private static final Map<UUID,Vec3d> POSITIONS=new HashMap<>();
    static void install(){ClientTickEvents.END_CLIENT_TICK.register(c->{
        ticks++;c.options.pauseOnLostFocus=false;
        if(c.currentScreen instanceof GameMenuScreen&&stage>=3)c.setScreen(null);
        if(stage==0&&ticks>60&&c.currentScreen instanceof TitleScreen){stage=1;CreateWorldScreen.create(c,c.currentScreen);}
        else if(stage==1&&c.currentScreen instanceof CreateWorldScreen screen){
            var creator=screen.getWorldCreator();creator.setWorldName("皇室法术 · 录制片场 1.2.0 (MC 1.21.1)");creator.setSeed("642155");
            creator.setGameMode(WorldCreator.Mode.CREATIVE);creator.setCheatsEnabled(true);creator.setDifficulty(Difficulty.NORMAL);creator.setGenerateStructures(false);
            creator.setWorldType(creator.getNormalWorldTypes().stream().filter(t->t.preset()!=null&&t.preset().matchesKey(WorldPresets.FLAT)).findFirst().orElseThrow());
            for(var child:screen.children())if(child instanceof ButtonWidget b&&b.getMessage().getString().equals(Text.translatable("selectWorld.create").getString())){stage=2;b.onPress();break;}
        }else if(stage==2&&c.currentScreen instanceof ConfirmScreen screen){
            String title=screen.getTitle().getString();
            if(!title.equals(Text.translatable("selectWorld.warning.experimental.title").getString())&&!title.equals(Text.translatable("selectWorld.warning.deprecated.title").getString()))throw new IllegalStateException("Unexpected map confirmation: "+title);
            for(var child:screen.children())if(child instanceof ButtonWidget b&&b.getMessage().getString().equals(Text.translatable("gui.yes").getString())){b.onPress();break;}
        }else if(stage==2&&c.world!=null&&c.player!=null&&c.getServer()!=null){
            stage=3;c.options.getViewDistance().setValue(6);c.options.getFov().setValue(65);c.options.setPerspective(Perspective.FIRST_PERSON);c.options.hudHidden=true;
            c.getServer().execute(()->ShowcaseMap.beginBuild(c.getServer().getOverworld()));
        }else if(stage==3&&c.getServer()!=null&&ShowcaseMap.ready(c.getServer().getOverworld())){stage=4;age=0;}
        else if(stage==4&&c.player!=null){
            age++;var spell=ShowcaseMap.SCENES.get(scene).spell();
            if(age==30){
                POSITIONS.clear();for(var e:c.world.getEntities())if(e instanceof net.minecraft.entity.mob.MobEntity&&e.squaredDistanceTo(Vec3d.ofCenter(ShowcaseMap.center(scene)))<100)POSITIONS.put(e.getUuid(),e.getPos());
                if(ShowcaseMap.index(c.getServer().getOverworld())!=scene)throw new IllegalStateException("Scene controller did not move to "+scene);
                if(c.player.getInventory().getStack(7).getItem()!=RoyaleSpells.PREVIOUS_SCENE||c.player.getInventory().getStack(8).getItem()!=RoyaleSpells.NEXT_SCENE)throw new IllegalStateException("Missing scene items");
                shot(c,String.format(Locale.ROOT,"scene-%02d-%s.png",scene+1,spell.id()));
            }
            if(age==45)use(c,0);
            if(spell==Spell.MIRROR&&age==65)use(c,1);
            for(var e:c.world.getEntities())if(e instanceof SpellEntity fx&&fx.spell()==spell){
                int t=fx.time();
                if((spell==Spell.ZAP||spell==Spell.ZAP_EVOLUTION)&&((t>=1&&t<=7)||(t>=21&&t<=27)))shotOnce(c,"motion-"+spell.id()+"-"+t+".png");
                if(spell==Spell.EARTHQUAKE&&(t==15||t==35||t==55))shotOnce(c,"quake-wave-"+(t/20+1)+".png");
            }
            if(age==105){
                if(spell==Spell.TORNADO||spell==Spell.GIANT_SNOWBALL){
                    int moved=0;for(var e:c.world.getEntities())if(POSITIONS.containsKey(e.getUuid())&&e.getPos().subtract(POSITIONS.get(e.getUuid())).horizontalLength()>.6)moved++;
                    if(moved==0)throw new IllegalStateException("No visible target displacement in "+spell);
                    System.out.println("ROYALE_MAP_DISPLACEMENT "+spell+" moved="+moved);
                }
                shot(c,String.format(Locale.ROOT,"effect-%02d-%s.png",scene+1,spell.id()));
            }
            if(age==125){
                System.out.println("ROYALE_MAP_SCENE_CHECK "+scene+" "+spell);
                use(c,8);scene++;age=0;
                if(scene==ShowcaseMap.SCENES.size()){stage=5;age=0;}
            }
        }else if(stage==5&&c.player!=null){
            age++;
            if(age==25){if(ShowcaseMap.index(c.getServer().getOverworld())!=0)throw new IllegalStateException("Next must wrap to first scene");use(c,7);}
            if(age==50){if(ShowcaseMap.index(c.getServer().getOverworld())!=ShowcaseMap.SCENES.size()-1)throw new IllegalStateException("Previous must wrap to last scene");c.options.sneakKey.setPressed(true);}
            if(age==55)use(c,8);
            if(age==60)c.options.sneakKey.setPressed(false);
            if(age==80){if(ShowcaseMap.index(c.getServer().getOverworld())!=ShowcaseMap.SCENES.size()-1)throw new IllegalStateException("Sneak-use must reset the same scene");System.out.println("ROYALE_MAP_CONTROLS_COMPLETE");}
            if(age==85)c.getServer().execute(()->{
                var w=c.getServer().getOverworld();var p=c.getServer().getPlayerManager().getPlayerList().get(0);ShowcaseMap.enter(p,20);
                for(var e:w.getEntitiesByClass(net.minecraft.entity.mob.MobEntity.class,new Box(ShowcaseMap.center(20)).expand(10),m->m instanceof Summoned)){
                    e.setYaw(180);e.setBodyYaw(180);e.setHeadYaw(180);
                }
                var center=ShowcaseMap.center(20);p.teleport(w,center.getX()+.5,ShowcaseMap.Y+.4,center.getZ()-3.4,0,4);
            });
            if(age==115)shot(c,"royale-grip-front-1.2.0.png");
            if(age==120)c.getServer().execute(()->{var p=c.getServer().getPlayerManager().getPlayerList().get(0);var center=ShowcaseMap.center(20);p.teleport(p.getServerWorld(),center.getX()-3.2,ShowcaseMap.Y+.25,center.getZ()-1.6,-60,6);});
            if(age==150)shot(c,"royale-grip-side-1.2.0.png");
            if(age==155)c.getServer().execute(()->{var p=c.getServer().getPlayerManager().getPlayerList().get(0);var center=ShowcaseMap.center(20);p.teleport(p.getServerWorld(),center.getX()+.5,ShowcaseMap.Y+.5,center.getZ()-1.1,0,-8);});
            if(age==185)shot(c,"royale-recruit-bucket-1.2.0.png");
            if(age==190)c.getServer().execute(()->ShowcaseMap.enter(c.getServer().getPlayerManager().getPlayerList().get(0),0));
            if(age==215){c.options.hudHidden=false;c.player.getInventory().selectedSlot=0;}
            if(age==220){
                shot(c,"recording-map-controls.png");
                try{Files.writeString(c.runDirectory.toPath().resolve("showcase-save-path.txt"),c.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT).toAbsolutePath().normalize().toString());}catch(Exception ex){throw new RuntimeException(ex);}
                System.out.println("ROYALE_MAP_COMPLETE scenes="+ShowcaseMap.SCENES.size()+" controls=next,previous,wrap,reset");c.scheduleStop();stage=6;
            }
        }
        if(ticks>10000&&stage<6)throw new IllegalStateException("Recording map build timed out at stage "+stage);
    });}
    private static void use(MinecraftClient c,int slot){c.player.getInventory().selectedSlot=slot;c.interactionManager.interactItem(c.player,Hand.MAIN_HAND);}
    private static void shotOnce(MinecraftClient c,String name){if(SHOTS.add(name))shot(c,name);}
    private static void shot(MinecraftClient c,String name){ScreenshotRecorder.saveScreenshot(c.runDirectory,name,c.getFramebuffer(),message->{});}
}
