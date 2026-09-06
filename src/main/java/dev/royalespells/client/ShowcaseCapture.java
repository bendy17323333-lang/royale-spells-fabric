package dev.royalespells.client;

import dev.royalespells.*;
import dev.royalespells.entity.*;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;
import java.util.*;

/** Explicit offline map-building run; drives real item packets and exports an ordinary save. */
final class ShowcaseCapture {
    private static int ticks,stage,age,scene;
    private static final Set<String> SHOTS=new HashSet<>();
    private static final Map<UUID,Vec3> POSITIONS=new HashMap<>();
    static void install(){net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event)->{var c=net.minecraft.client.Minecraft.getInstance();
        ticks++;c.options.pauseOnLostFocus=false;
        if(c.screen instanceof PauseScreen&&stage>=3)c.setScreen(null);
        if(stage==0&&ticks>60&&c.screen instanceof TitleScreen){stage=1;CreateWorldScreen.openFresh(c,c.screen);}
        else if(stage==1&&c.screen instanceof CreateWorldScreen screen){
            var creator=screen.getUiState();creator.setName("皇室法术 · 录制片场 1.2.0 (MC 1.21.1 NeoForge)");creator.setSeed("642155");
            creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);creator.setAllowCommands(true);creator.setDifficulty(Difficulty.NORMAL);creator.setGenerateStructures(false);
            creator.setWorldType(creator.getNormalPresetList().stream().filter(t->t.preset()!=null&&t.preset().is(WorldPresets.FLAT)).findFirst().orElseThrow());
            for(var child:screen.children())if(child instanceof Button b&&b.getMessage().getString().equals(Component.translatable("selectWorld.create").getString())){stage=2;b.onPress();break;}
        }else if(stage==2&&c.screen instanceof ConfirmScreen screen){
            String title=screen.getTitle().getString();
            if(!title.equals(Component.translatable("selectWorld.warning.experimental.title").getString())&&!title.equals(Component.translatable("selectWorld.warning.deprecated.title").getString()))throw new IllegalStateException("Unexpected map confirmation: "+title);
            for(var child:screen.children())if(child instanceof Button b&&b.getMessage().getString().equals(Component.translatable("gui.yes").getString())){b.onPress();break;}
        }else if(stage==2&&c.level!=null&&c.player!=null&&c.getSingleplayerServer()!=null){
            stage=3;c.options.renderDistance().set(6);c.options.fov().set(65);c.options.setCameraType(CameraType.FIRST_PERSON);c.options.hideGui=true;
            c.getSingleplayerServer().execute(()->ShowcaseMap.beginBuild(c.getSingleplayerServer().overworld()));
        }else if(stage==3&&c.getSingleplayerServer()!=null&&ShowcaseMap.ready(c.getSingleplayerServer().overworld())){stage=4;age=0;}
        else if(stage==4&&c.player!=null){
            age++;var spell=ShowcaseMap.SCENES.get(scene).spell();
            if(age==30){
                POSITIONS.clear();for(var e:c.level.entitiesForRendering())if(e instanceof net.minecraft.world.entity.Mob&&e.distanceToSqr(Vec3.atCenterOf(ShowcaseMap.center(scene)))<100)POSITIONS.put(e.getUUID(),e.position());
                if(ShowcaseMap.index(c.getSingleplayerServer().overworld())!=scene)throw new IllegalStateException("Scene controller did not move to "+scene);
                if(c.player.getInventory().getItem(7).getItem()!=RoyaleSpells.PREVIOUS_SCENE||c.player.getInventory().getItem(8).getItem()!=RoyaleSpells.NEXT_SCENE)throw new IllegalStateException("Missing scene items");
                shot(c,String.format(Locale.ROOT,"scene-%02d-%s.png",scene+1,spell.id()));
            }
            if(age==45)use(c,0);
            if(spell==Spell.MIRROR&&age==65)use(c,1);
            for(var e:c.level.entitiesForRendering())if(e instanceof SpellEntity fx&&fx.spell()==spell){
                int t=fx.time();
                if((spell==Spell.ZAP||spell==Spell.ZAP_EVOLUTION)&&((t>=1&&t<=7)||(t>=21&&t<=27)))shotOnce(c,"motion-"+spell.id()+"-"+t+".png");
                if(spell==Spell.EARTHQUAKE&&(t==15||t==35||t==55))shotOnce(c,"quake-wave-"+(t/20+1)+".png");
            }
            if(age==105){
                if(spell==Spell.TORNADO||spell==Spell.GIANT_SNOWBALL){
                    int moved=0;for(var e:c.level.entitiesForRendering())if(POSITIONS.containsKey(e.getUUID())&&e.position().subtract(POSITIONS.get(e.getUUID())).horizontalDistance()>.6)moved++;
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
            if(age==25){if(ShowcaseMap.index(c.getSingleplayerServer().overworld())!=0)throw new IllegalStateException("Next must wrap to first scene");use(c,7);}
            if(age==50){if(ShowcaseMap.index(c.getSingleplayerServer().overworld())!=ShowcaseMap.SCENES.size()-1)throw new IllegalStateException("Previous must wrap to last scene");c.options.keyShift.setDown(true);}
            if(age==55)use(c,8);
            if(age==60)c.options.keyShift.setDown(false);
            if(age==80){if(ShowcaseMap.index(c.getSingleplayerServer().overworld())!=ShowcaseMap.SCENES.size()-1)throw new IllegalStateException("Sneak-use must reset the same scene");System.out.println("ROYALE_MAP_CONTROLS_COMPLETE");}
            if(age==85)c.getSingleplayerServer().execute(()->{
                var w=c.getSingleplayerServer().overworld();var p=c.getSingleplayerServer().getPlayerList().getPlayers().get(0);ShowcaseMap.enter(p,20);
                for(var e:w.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,new AABB(ShowcaseMap.center(20)).inflate(10),m->m instanceof Summoned)){
                    e.setYRot(180);e.setYBodyRot(180);e.setYHeadRot(180);
                }
                var center=ShowcaseMap.center(20);p.teleportTo(w,center.getX()+.5,ShowcaseMap.Y+.4,center.getZ()-3.4,0,4);
            });
            if(age==115)shot(c,"royale-grip-front-1.2.0.png");
            if(age==120)c.getSingleplayerServer().execute(()->{var p=c.getSingleplayerServer().getPlayerList().getPlayers().get(0);var center=ShowcaseMap.center(20);p.teleportTo(p.serverLevel(),center.getX()-3.2,ShowcaseMap.Y+.25,center.getZ()-1.6,-60,6);});
            if(age==150)shot(c,"royale-grip-side-1.2.0.png");
            if(age==155)c.getSingleplayerServer().execute(()->{var p=c.getSingleplayerServer().getPlayerList().getPlayers().get(0);var center=ShowcaseMap.center(20);p.teleportTo(p.serverLevel(),center.getX()+.5,ShowcaseMap.Y+.5,center.getZ()-1.1,0,-8);});
            if(age==185)shot(c,"royale-recruit-bucket-1.2.0.png");
            if(age==190)c.getSingleplayerServer().execute(()->ShowcaseMap.enter(c.getSingleplayerServer().getPlayerList().getPlayers().get(0),0));
            if(age==215){c.options.hideGui=false;c.player.getInventory().selected=0;}
            if(age==220){
                shot(c,"recording-map-controls.png");
                try{Files.writeString(c.gameDirectory.toPath().resolve("showcase-save-path.txt"),c.getSingleplayerServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize().toString());}catch(Exception ex){throw new RuntimeException(ex);}
                System.out.println("ROYALE_MAP_COMPLETE scenes="+ShowcaseMap.SCENES.size()+" controls=next,previous,wrap,reset");c.stop();stage=6;
            }
        }
        if(ticks>10000&&stage<6)throw new IllegalStateException("Recording map build timed out at stage "+stage);
    });}
    private static void use(Minecraft c,int slot){c.player.getInventory().selected=slot;c.gameMode.useItem(c.player,InteractionHand.MAIN_HAND);}
    private static void shotOnce(Minecraft c,String name){if(SHOTS.add(name))shot(c,name);}
    private static void shot(Minecraft c,String name){Screenshot.grab(c.gameDirectory,name,c.getMainRenderTarget(),message->{});}
}
