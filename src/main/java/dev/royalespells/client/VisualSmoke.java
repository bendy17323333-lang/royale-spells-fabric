package dev.royalespells.client;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import java.util.UUID;

/** Opt-in, isolated development world and screenshots. Never active during ordinary play. */
public final class VisualSmoke {
    private static int ticks,stage,ready;
    private static int graveyardSounds;
    private static void verifyGraveyardAudio(net.minecraft.client.MinecraftClient client) {
        try(var input=client.getResourceManager().open(RoyaleSpells.id("sounds/graveyard_deploy.ogg"));
            var ogg=new net.minecraft.client.sound.OggAudioStream(input)) {
            var format=ogg.getFormat();var buffer=ogg.readAll();byte[] pcm=new byte[buffer.remaining()];buffer.get(pcm);
            double seconds=(double)pcm.length/format.getFrameSize()/format.getFrameRate();
            if(format.getChannels()!=1 || seconds<3 || seconds>3.2)throw new IllegalStateException("Unexpected Graveyard audio format");
            try(var audio=new javax.sound.sampled.AudioInputStream(new java.io.ByteArrayInputStream(pcm),format,pcm.length/format.getFrameSize())) {
                javax.sound.sampled.AudioSystem.write(audio,javax.sound.sampled.AudioFileFormat.Type.WAVE,new java.io.File(client.runDirectory,"graveyard-preview.wav"));
            }
            System.out.println("ROYALE_AUDIO_DECODE_OK seconds="+seconds+" format="+format);
        } catch(java.io.IOException ex) {throw new RuntimeException("Graveyard OGG decode failed",ex);}
        client.getSoundManager().registerListener((sound,set,range)->{
            if(sound.getId().equals(RoyaleSpells.GRAVEYARD_DEPLOY.getId())) {
                if(sound.getPitch()!=1 || sound.isRepeatable() || sound.isRelative() || sound.getSound()==net.minecraft.client.sound.SoundManager.MISSING_SOUND)
                    throw new IllegalStateException("Invalid Graveyard sound playback");
                graveyardSounds++;
                System.out.println("ROYALE_AUDIO_PLAY_EVENT count="+graveyardSounds+" pitch="+sound.getPitch());
            }
        });
    }
    public static void install() {
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            ticks++;client.options.pauseOnLostFocus=false;
            if(stage==3 && client.currentScreen instanceof net.minecraft.client.gui.screen.GameMenuScreen)client.setScreen(null);
            if(stage==0 && ticks>60 && client.currentScreen instanceof TitleScreen) {
                stage=1;CreateWorldScreen.create(client,client.currentScreen);
            } else if(stage==1 && client.currentScreen instanceof CreateWorldScreen screen) {
                var creator=screen.getWorldCreator();creator.setWorldName("Royale Spells Visual QA");creator.setSeed("642155");
                creator.setGameMode(WorldCreator.Mode.CREATIVE);creator.setCheatsEnabled(true);creator.setDifficulty(Difficulty.NORMAL);
                creator.setGenerateStructures(false);
                for(var child:screen.children())if(child instanceof ButtonWidget button && button.getMessage().getString().equals(Text.translatable("selectWorld.create").getString())) {
                    stage=2;button.onPress();break;
                }
            } else if(stage==2 && client.currentScreen instanceof net.minecraft.client.gui.screen.ConfirmScreen screen) {
                String title=screen.getTitle().getString();
                boolean newWorldWarning=title.equals(Text.translatable("selectWorld.warning.experimental.title").getString())||title.equals(Text.translatable("selectWorld.warning.deprecated.title").getString());
                if(!newWorldWarning)throw new IllegalStateException("Unexpected isolated-world confirmation: "+title);
                for(var child:screen.children())if(child instanceof ButtonWidget button&&button.getMessage().getString().equals(Text.translatable("gui.yes").getString())){
                    System.out.println("ROYALE_QA_NEW_WORLD_CONFIRM "+title);button.onPress();break;
                }
            } else if(stage==2 && client.world!=null && client.player!=null && client.getServer()!=null) {
                stage=3;client.options.getViewDistance().setValue(6);verifyGraveyardAudio(client);BarbarianAudioSmoke.install(client);
                client.getServer().execute(()->{
                    var server=client.getServer();var world=server.getOverworld();var player=server.getPlayerManager().getPlayerList().get(0);
                    world.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false,server);world.setTimeOfDay(6000);
                    world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false,server);
                    for(int x=-14;x<=14;x++)for(int z=-16;z<=18;z++) {
                        world.setBlockState(new BlockPos(x,149,z),((x+z)%2==0?Blocks.SMOOTH_STONE:Blocks.STONE_BRICKS).getDefaultState());
                        for(int y=150;y<159;y++)world.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());
                    }
                    player.teleport(world,0,153,-13,0,15);
                    player.getAbilities().flying=true;player.sendAbilitiesUpdate();
                    player.getInventory().clear();for(var item:RoyaleSpells.ITEMS.values())player.giveItemStack(new ItemStack(item));
                    player.getInventory().selectedSlot=2;
                    Spell[] spells={Spell.FIREBALL,Spell.ROCKET,Spell.THE_LOG,Spell.GOBLIN_BARREL_EVOLUTION,Spell.GIANT_SNOWBALL_EVOLUTION,Spell.ROYAL_DELIVERY};
                    Vec3d[] locations={new Vec3d(-7,151,1),new Vec3d(-2.5,151,1),new Vec3d(2.5,150,1),new Vec3d(7,151,1),new Vec3d(-7,150,7),new Vec3d(-2.5,150,7)};
                    for(int i=0;i<spells.length;i++) {
                        var effect=SpellEntity.create(world,spells[i],player.getUuid(),locations[i],locations[i]);effect.preview=true;effect.setPreviewTime(spells[i]==Spell.ROYAL_DELIVERY?58:spells[i]==Spell.GIANT_SNOWBALL_EVOLUTION?24:0);
                        world.spawnEntity(effect);
                    }
                    var barb=SpellEngine.summon(world,player.getUuid(),new Vec3d(2,150,7),"barbarian",false);barb.setAiDisabled(true);barb.setYaw(180);
                    var recruit=SpellEngine.summon(world,player.getUuid(),new Vec3d(4.5,150,7),"recruit",false);recruit.setAiDisabled(true);recruit.setYaw(180);
                    var skeleton=SpellEngine.summon(world,player.getUuid(),new Vec3d(7,150,7),"skeleton",false);skeleton.setAiDisabled(true);skeleton.setYaw(180);
                    var zombie=SpellEngine.summon(world,player.getUuid(),new Vec3d(9,150,7),"zombie",false);zombie.setAiDisabled(true);zombie.setYaw(180);
                    var field=SpellEntity.create(world,Spell.RAGE,player.getUuid(),new Vec3d(0,150,5),new Vec3d(0,150,5));field.preview=true;world.spawnEntity(field);
                    Vec3d voidPos=new Vec3d(0,150,-2);
                    var victim=net.minecraft.entity.EntityType.IRON_GOLEM.create(world);victim.setPosition(voidPos);victim.setAiDisabled(true);victim.setNoGravity(true);world.spawnEntity(victim);
                    var voidSpell=SpellEntity.create(world,Spell.VOID,player.getUuid(),voidPos,voidPos);
                    for(int i=0;i<16;i++)voidSpell.tick();voidSpell.setPreviewTime(18);voidSpell.preview=true;world.spawnEntity(voidSpell);
                });
            } else if(stage==3 && client.world!=null) {
                ready++;
                if(ready==20)client.getServer().execute(()->{
                    var world=client.getServer().getOverworld();var player=client.getServer().getPlayerManager().getPlayerList().get(0);
                    var pos=player.getPos().add(0,-2,2);
                    world.spawnEntity(SpellEntity.create(world,Spell.GRAVEYARD,player.getUuid(),pos,pos));
                });
                if(ready==110) { for(var e:client.world.getEntities())if(e instanceof SpellEntity fx)System.out.println("PREVIEW_CLIENT "+fx.spell()+" tick="+fx.time()+" age="+fx.age+" pos="+fx.getPos()+" visual="+fx.visualPosition(0));client.getServer().execute(()->{for(var e:client.getServer().getOverworld().iterateEntities())if(e instanceof SpellEntity fx)System.out.println("PREVIEW_SERVER "+fx.spell()+" tick="+fx.time()+" preview="+fx.preview+" pos="+fx.getPos());}); }
                if(ready==120)ScreenshotRecorder.saveScreenshot(client.runDirectory,"royale-models.png",client.getFramebuffer(),message->System.out.println("ROYALE_VISUAL_MODELS "+message.getString()));
                if(ready==140)client.getServer().execute(()->client.getServer().getPlayerManager().getPlayerList().get(0).changeGameMode(net.minecraft.world.GameMode.SURVIVAL));
                if(ready==150)client.setScreen(new InventoryScreen(client.player));
                if(ready==160 && client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen creative) { try { var method=creative.getClass().getDeclaredMethod("setSelectedTab",net.minecraft.item.ItemGroup.class);method.setAccessible(true);method.invoke(creative,net.minecraft.registry.Registries.ITEM_GROUP.get(RoyaleSpells.id("spells"))); } catch(Exception ex) {throw new RuntimeException(ex);} }
                if(ready==170)ScreenshotRecorder.saveScreenshot(client.runDirectory,"royale-cards.png",client.getFramebuffer(),message->System.out.println("ROYALE_VISUAL_CARDS "+message.getString()));
                if(ready==180) {
                    client.setScreen(null);
                    client.getServer().execute(()->{
                        var server=client.getServer();var world=server.getOverworld();var player=server.getPlayerManager().getPlayerList().get(0);
                        player.changeGameMode(net.minecraft.world.GameMode.CREATIVE);player.getAbilities().flying=true;player.sendAbilitiesUpdate();
                        for(int x=33;x<=47;x++)for(int z=-12;z<=8;z++) {
                            world.setBlockState(new BlockPos(x,149,z),Blocks.STONE_BRICKS.getDefaultState());
                            for(int y=150;y<=164;y++)world.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());
                        }
                        for(int x=38;x<=40;x++)for(int z=-1;z<=2;z++)for(int y=150;y<=154;y++)
                            if(x==38||x==40||z==-1||z==2||y==154)world.setBlockState(new BlockPos(x,y,z),Blocks.OAK_PLANKS.getDefaultState());
                        for(int y=150;y<=158;y++)world.setBlockState(new BlockPos(42,y,0),Blocks.OAK_LOG.getDefaultState());
                        for(int x=41;x<=43;x++)for(int z=-1;z<=1;z++)for(int y=157;y<=159;y++)if(x!=42||z!=0||y==159)
                            world.setBlockState(new BlockPos(x,y,z),Blocks.OAK_LEAVES.getDefaultState().with(net.minecraft.block.LeavesBlock.PERSISTENT,true));
                        player.teleport(world,40,154,-11,0,17);player.getInventory().selectedSlot=1;
                    });
                }
                if(ready==225)ScreenshotRecorder.saveScreenshot(client.runDirectory,"royale-quake-before.png",client.getFramebuffer(),message->{});
                if(ready==235)client.getServer().execute(()->{
                    var world=client.getServer().getOverworld();var player=client.getServer().getPlayerManager().getPlayerList().get(0);Vec3d pos=new Vec3d(40.5,150,.5);
                    world.spawnEntity(SpellEntity.create(world,Spell.EARTHQUAKE,player.getUuid(),pos,pos));
                });
                if(ready==325)ScreenshotRecorder.saveScreenshot(client.runDirectory,"royale-quake-after.png",client.getFramebuffer(),message->System.out.println("ROYALE_QUAKE_VISUAL_COMPLETE"));
                if(ready==340)client.getServer().execute(()->{
                    var server=client.getServer();var world=server.getOverworld();var player=server.getPlayerManager().getPlayerList().get(0);
                    for(int x=71;x<=89;x++)for(int z=-12;z<=9;z++) {
                        world.setBlockState(new BlockPos(x,149,z),Blocks.STONE_BRICKS.getDefaultState());
                        for(int y=150;y<169;y++)world.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState());
                    }
                    player.teleport(world,80,153,-11,0,12);player.getInventory().clear();
                    for(int i=0;i<3;i++) {
                        var unit=SpellEngine.summon(world,player.getUuid(),new Vec3d(75+i*2.5,150,0),"barbarian",false);unit.setAiDisabled(true);unit.setYaw(180);
                        if(i==1)unit.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(RoyaleSpells.RAGED,1000,0,false,false));
                        if(i==2){((Summoned)unit).setup(player.getUuid(),1000,true);unit.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(1);unit.setHealth(1);}
                    }
                    for(int i=0;i<2;i++){
                        var unit=net.minecraft.entity.EntityType.IRON_GOLEM.create(world);unit.setPosition(82.5+i*2.5,150,0);unit.setAiDisabled(true);unit.setYaw(180);world.spawnEntity(unit);
                        unit.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(i==0?RoyaleSpells.FROZEN:RoyaleSpells.ROOTED,1000,0,false,false,false));
                    }
                    var rocket=SpellEntity.create(world,Spell.ROCKET,player.getUuid(),new Vec3d(74,151,-4),new Vec3d(74,151,-4));rocket.preview=true;world.spawnEntity(rocket);
                });
                if(ready==400){
                    SpellTint.verifyVisualCoverage();
                    int clones=0,raged=0,frozen=0,rooted=0;
                    for(var entity:client.world.getEntities()) {
                        if(entity.getX()<71 || entity.getX()>89)continue;
                        if(entity instanceof Summoned s && s.isClone())clones++;
                        if(entity instanceof net.minecraft.entity.LivingEntity living){if(dev.royalespells.VisualState.raged(living))raged++;if(dev.royalespells.VisualState.frozen(living))frozen++;if(dev.royalespells.VisualState.rooted(living))rooted++;}
                    }
                    if(clones!=1||raged!=1||frozen!=1||rooted!=1)throw new IllegalStateException("Missing client visual states "+clones+","+raged+","+frozen+","+rooted);
                    ScreenshotRecorder.saveScreenshot(client.runDirectory,"royale-status-models.png",client.getFramebuffer(),message->System.out.println("ROYALE_STATUS_VISUAL_COMPLETE"));
                }
                if(ready==410)client.getServer().execute(()->{
                    var world=client.getServer().getOverworld();var player=client.getServer().getPlayerManager().getPlayerList().get(0);var at=new Vec3d(80,150,-4);
                    var zap=SpellEntity.create(world,Spell.ZAP_EVOLUTION,player.getUuid(),at,at);zap.setPreviewTime(2);zap.preview=true;world.spawnEntity(zap);
                });
                if(ready==430)ScreenshotRecorder.saveScreenshot(client.runDirectory,"royale-zap-first.png",client.getFramebuffer(),message->{});
                if(ready==440)client.getServer().execute(()->{for(var entity:client.getServer().getOverworld().iterateEntities())if(entity instanceof SpellEntity fx && fx.spell()==Spell.ZAP_EVOLUTION && fx.preview)fx.setPreviewTime(22);});
                if(ready==460)ScreenshotRecorder.saveScreenshot(client.runDirectory,"royale-zap-second.png",client.getFramebuffer(),message->System.out.println("ROYALE_ZAP_VISUAL_COMPLETE"));
                if(ready==490){if(graveyardSounds!=1)throw new IllegalStateException("Expected one Graveyard deploy sound, got "+graveyardSounds);System.out.println("ROYALE_AUDIO_SMOKE_COMPLETE");}
                TroopVisualSmoke.tick(client,ready);
            }
            if(ticks>3600){System.err.println("ROYALE_VISUAL_SMOKE_TIMEOUT stage="+stage);client.scheduleStop();}
        });
    }
}





