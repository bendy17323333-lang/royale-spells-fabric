package dev.royalespells.client;

import dev.royalespells.*;
import dev.royalespells.entity.*;

import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Opt-in, isolated development world and screenshots. Never active during ordinary play. */
public final class VisualSmoke {
    private static int ticks,stage,ready;
    private static int graveyardSounds;
    private static void verifyGraveyardAudio(net.minecraft.client.Minecraft client) {
        try(var input=client.getResourceManager().open(RoyaleSpells.id("sounds/graveyard_deploy.ogg"));
            var ogg=new net.minecraft.client.sounds.JOrbisAudioStream(input)) {
            var format=ogg.getFormat();var buffer=ogg.readAll();byte[] pcm=new byte[buffer.remaining()];buffer.get(pcm);
            double seconds=(double)pcm.length/format.getFrameSize()/format.getFrameRate();
            if(format.getChannels()!=1 || seconds<3 || seconds>3.2)throw new IllegalStateException("Unexpected Graveyard audio format");
            try(var audio=new javax.sound.sampled.AudioInputStream(new java.io.ByteArrayInputStream(pcm),format,pcm.length/format.getFrameSize())) {
                javax.sound.sampled.AudioSystem.write(audio,javax.sound.sampled.AudioFileFormat.Type.WAVE,new java.io.File(client.gameDirectory,"graveyard-preview.wav"));
            }
            System.out.println("ROYALE_AUDIO_DECODE_OK seconds="+seconds+" format="+format);
        } catch(java.io.IOException ex) {throw new RuntimeException("Graveyard OGG decode failed",ex);}
        client.getSoundManager().addListener((sound,set,range)->{
            if(sound.getLocation().equals(RoyaleSpells.GRAVEYARD_DEPLOY.getLocation())) {
                if(sound.getPitch()!=1 || sound.isLooping() || sound.isRelative() || sound.getSound()==net.minecraft.client.sounds.SoundManager.EMPTY_SOUND)
                    throw new IllegalStateException("Invalid Graveyard sound playback");
                graveyardSounds++;
                System.out.println("ROYALE_AUDIO_PLAY_EVENT count="+graveyardSounds+" pitch="+sound.getPitch());
            }
        });
    }
    public static void install() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event)->{var client=net.minecraft.client.Minecraft.getInstance();
            ticks++;client.options.pauseOnLostFocus=false;
            if(stage==3 && client.screen instanceof net.minecraft.client.gui.screens.PauseScreen)client.setScreen(null);
            if(stage==0 && ticks>60 && client.screen instanceof TitleScreen) {
                stage=1;CreateWorldScreen.openFresh(client,client.screen);
            } else if(stage==1 && client.screen instanceof CreateWorldScreen screen) {
                var creator=screen.getUiState();creator.setName("Royale Spells Visual QA");creator.setSeed("642155");
                creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);creator.setAllowCommands(true);creator.setDifficulty(Difficulty.NORMAL);
                creator.setGenerateStructures(false);
                for(var child:screen.children())if(child instanceof Button button && button.getMessage().getString().equals(Component.translatable("selectWorld.create").getString())) {
                    stage=2;button.onPress();break;
                }
            } else if(stage==2 && client.screen instanceof net.minecraft.client.gui.screens.ConfirmScreen screen) {
                String title=screen.getTitle().getString();
                boolean newWorldWarning=title.equals(Component.translatable("selectWorld.warning.experimental.title").getString())||title.equals(Component.translatable("selectWorld.warning.deprecated.title").getString());
                if(!newWorldWarning)throw new IllegalStateException("Unexpected isolated-world confirmation: "+title);
                for(var child:screen.children())if(child instanceof Button button&&button.getMessage().getString().equals(Component.translatable("gui.yes").getString())){
                    System.out.println("ROYALE_QA_NEW_WORLD_CONFIRM "+title);button.onPress();break;
                }
            } else if(stage==2 && client.level!=null && client.player!=null && client.getSingleplayerServer()!=null) {
                stage=3;client.options.renderDistance().set(6);verifyGraveyardAudio(client);BarbarianAudioSmoke.install(client);
                client.getSingleplayerServer().execute(()->{
                    var server=client.getSingleplayerServer();var world=server.overworld();var player=server.getPlayerList().getPlayers().get(0);
                    world.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);world.setDayTime(6000);
                    world.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);
                    for(int x=-14;x<=14;x++)for(int z=-16;z<=18;z++) {
                        world.setBlockAndUpdate(new BlockPos(x,149,z),((x+z)%2==0?Blocks.SMOOTH_STONE:Blocks.STONE_BRICKS).defaultBlockState());
                        for(int y=150;y<159;y++)world.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
                    }
                    player.teleportTo(world,0,153,-13,0,15);
                    player.getAbilities().flying=true;player.onUpdateAbilities();
                    player.getInventory().clearContent();for(var item:RoyaleSpells.ITEMS.values())player.addItem(new ItemStack(item));
                    player.getInventory().selected=2;
                    if(Boolean.getBoolean("royalespells.fireballSmoke")||Boolean.getBoolean("royalespells.snowballSmoke")||Boolean.getBoolean("royalespells.armySmoke")||Boolean.getBoolean("royalespells.combatSmoke")||Boolean.getBoolean("royalespells.spiritSmoke")||Boolean.getBoolean("royalespells.infernoSmoke"))return;
                    Spell[] spells={Spell.FIREBALL,Spell.ROCKET,Spell.THE_LOG,Spell.GOBLIN_BARREL_EVOLUTION,Spell.GIANT_SNOWBALL_EVOLUTION,Spell.ROYAL_DELIVERY};
                    Vec3[] locations={new Vec3(-7,151,1),new Vec3(-2.5,151,1),new Vec3(2.5,150,1),new Vec3(7,151,1),new Vec3(-7,150,7),new Vec3(-2.5,150,7)};
                    for(int i=0;i<spells.length;i++) {
                        var effect=SpellEntity.create(world,spells[i],player.getUUID(),locations[i],locations[i]);effect.preview=true;effect.setPreviewTime(spells[i]==Spell.ROYAL_DELIVERY?58:spells[i]==Spell.GIANT_SNOWBALL_EVOLUTION?24:0);
                        world.addFreshEntity(effect);
                    }
                    var barb=SpellEngine.summon(world,player.getUUID(),new Vec3(2,150,7),"barbarian",false);barb.setNoAi(true);barb.setYRot(180);
                    var recruit=SpellEngine.summon(world,player.getUUID(),new Vec3(4.5,150,7),"recruit",false);recruit.setNoAi(true);recruit.setYRot(180);
                    var skeleton=SpellEngine.summon(world,player.getUUID(),new Vec3(7,150,7),"skeleton",false);skeleton.setNoAi(true);skeleton.setYRot(180);
                    var zombie=SpellEngine.summon(world,player.getUUID(),new Vec3(9,150,7),"zombie",false);zombie.setNoAi(true);zombie.setYRot(180);
                    var field=SpellEntity.create(world,Spell.RAGE,player.getUUID(),new Vec3(0,150,5),new Vec3(0,150,5));field.preview=true;world.addFreshEntity(field);
                    Vec3 voidPos=new Vec3(0,150,-2);
                    var victim=net.minecraft.world.entity.EntityType.IRON_GOLEM.create(world);victim.setPos(voidPos);victim.setNoAi(true);victim.setNoGravity(true);world.addFreshEntity(victim);
                    var voidSpell=SpellEntity.create(world,Spell.VOID,player.getUUID(),voidPos,voidPos);
                    for(int i=0;i<16;i++)voidSpell.tick();voidSpell.setPreviewTime(18);voidSpell.preview=true;world.addFreshEntity(voidSpell);
                });
            } else if(stage==3 && client.level!=null) {
                if(Boolean.getBoolean("royalespells.fireballSmoke")){FireballClientSmoke.tick(client);return;}
                if(VisualUpdateRecording.ENABLED){VisualUpdateRecording.tick(client);return;}
                if(Boolean.getBoolean("royalespells.infernoSmoke")){InfernoClientSmoke.tick(client);return;}
                if(Boolean.getBoolean("royalespells.spiritSmoke")){SpiritClientSmoke.tick(client);return;}
                if(Boolean.getBoolean("royalespells.combatSmoke")){CombatClientSmoke.tick(client);return;}
                if(Boolean.getBoolean("royalespells.armySmoke")){ArmyClientSmoke.tick(client);return;}
                if(Boolean.getBoolean("royalespells.elixirSmoke")){ElixirClientSmoke.tick(client);return;}
                if(Boolean.getBoolean("royalespells.polishSmoke")){PolishClientSmoke.tick(client);return;}
                if(Boolean.getBoolean("royalespells.targetPreviewSmoke")){TargetPreviewSmoke.tick(client);return;}
                if(Boolean.getBoolean("royalespells.ironSystemSmoke")){IronSystemClientSmoke.tick(client,ready++);return;}
                ready++;
                if(ready==20)client.getSingleplayerServer().execute(()->{
                    var world=client.getSingleplayerServer().overworld();var player=client.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                    var pos=player.position().add(0,-2,2);
                    world.addFreshEntity(SpellEntity.create(world,Spell.GRAVEYARD,player.getUUID(),pos,pos));
                });
                if(ready==110) { for(var e:client.level.entitiesForRendering())if(e instanceof SpellEntity fx)System.out.println("PREVIEW_CLIENT "+fx.spell()+" tick="+fx.time()+" age="+fx.tickCount+" pos="+fx.position()+" visual="+fx.visualPosition(0));client.getSingleplayerServer().execute(()->{for(var e:client.getSingleplayerServer().overworld().getAllEntities())if(e instanceof SpellEntity fx)System.out.println("PREVIEW_SERVER "+fx.spell()+" tick="+fx.time()+" preview="+fx.preview+" pos="+fx.position());}); }
                if(ready==120)Screenshot.grab(client.gameDirectory,"royale-models.png",client.getMainRenderTarget(),message->System.out.println("ROYALE_VISUAL_MODELS "+message.getString()));
                if(ready==140)client.getSingleplayerServer().execute(()->client.getSingleplayerServer().getPlayerList().getPlayers().get(0).setGameMode(net.minecraft.world.level.GameType.SURVIVAL));
                if(ready==150)client.setScreen(new InventoryScreen(client.player));
                if(ready==160 && client.screen instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen creative) { try { var method=creative.getClass().getDeclaredMethod("setSelectedTab",net.minecraft.world.item.CreativeModeTab.class);method.setAccessible(true);method.invoke(creative,net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.get(RoyaleSpells.id("spells"))); } catch(Exception ex) {throw new RuntimeException(ex);} }
                if(ready==170)Screenshot.grab(client.gameDirectory,"royale-cards.png",client.getMainRenderTarget(),message->System.out.println("ROYALE_VISUAL_CARDS "+message.getString()));
                if(ready==180) {
                    client.setScreen(null);
                    client.getSingleplayerServer().execute(()->{
                        var server=client.getSingleplayerServer();var world=server.overworld();var player=server.getPlayerList().getPlayers().get(0);
                        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
                        for(int x=33;x<=47;x++)for(int z=-12;z<=8;z++) {
                            world.setBlockAndUpdate(new BlockPos(x,149,z),Blocks.STONE_BRICKS.defaultBlockState());
                            for(int y=150;y<=164;y++)world.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
                        }
                        for(int x=38;x<=40;x++)for(int z=-1;z<=2;z++)for(int y=150;y<=154;y++)
                            if(x==38||x==40||z==-1||z==2||y==154)world.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.OAK_PLANKS.defaultBlockState());
                        for(int y=150;y<=158;y++)world.setBlockAndUpdate(new BlockPos(42,y,0),Blocks.OAK_LOG.defaultBlockState());
                        for(int x=41;x<=43;x++)for(int z=-1;z<=1;z++)for(int y=157;y<=159;y++)if(x!=42||z!=0||y==159)
                            world.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true));
                        player.teleportTo(world,40,154,-11,0,17);player.getInventory().selected=1;
                    });
                }
                if(ready==225)Screenshot.grab(client.gameDirectory,"royale-quake-before.png",client.getMainRenderTarget(),message->{});
                if(ready==235)client.getSingleplayerServer().execute(()->{
                    var world=client.getSingleplayerServer().overworld();var player=client.getSingleplayerServer().getPlayerList().getPlayers().get(0);Vec3 pos=new Vec3(40.5,150,.5);
                    world.addFreshEntity(SpellEntity.create(world,Spell.EARTHQUAKE,player.getUUID(),pos,pos));
                });
                if(ready==325)Screenshot.grab(client.gameDirectory,"royale-quake-after.png",client.getMainRenderTarget(),message->System.out.println("ROYALE_QUAKE_VISUAL_COMPLETE"));
                if(ready==340)client.getSingleplayerServer().execute(()->{
                    var server=client.getSingleplayerServer();var world=server.overworld();var player=server.getPlayerList().getPlayers().get(0);
                    for(int x=71;x<=89;x++)for(int z=-12;z<=9;z++) {
                        world.setBlockAndUpdate(new BlockPos(x,149,z),Blocks.STONE_BRICKS.defaultBlockState());
                        for(int y=150;y<169;y++)world.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
                    }
                    player.teleportTo(world,80,153,-11,0,12);player.getInventory().clearContent();
                    for(int i=0;i<3;i++) {
                        var unit=SpellEngine.summon(world,player.getUUID(),new Vec3(75+i*2.5,150,0),"barbarian",false);unit.setNoAi(true);unit.setYRot(180);
                        if(i==1)unit.addEffect(new net.minecraft.world.effect.MobEffectInstance(RoyaleSpells.RAGED,1000,0,false,false));
                        if(i==2){((Summoned)unit).setup(player.getUUID(),1000,true);unit.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1);unit.setHealth(1);}
                    }
                    for(int i=0;i<2;i++){
                        var unit=net.minecraft.world.entity.EntityType.IRON_GOLEM.create(world);unit.setPos(82.5+i*2.5,150,0);unit.setNoAi(true);unit.setYRot(180);world.addFreshEntity(unit);
                        unit.addEffect(new net.minecraft.world.effect.MobEffectInstance(i==0?RoyaleSpells.FROZEN:RoyaleSpells.ROOTED,1000,0,false,false,false));
                    }
                    var rocket=SpellEntity.create(world,Spell.ROCKET,player.getUUID(),new Vec3(74,151,-4),new Vec3(74,151,-4));rocket.preview=true;world.addFreshEntity(rocket);
                });
                if(ready==400){
                    SpellTint.verifyVisualCoverage();
                    int clones=0,raged=0,frozen=0,rooted=0;
                    for(var entity:client.level.entitiesForRendering()) {
                        if(entity.getX()<71 || entity.getX()>89)continue;
                        if(entity instanceof Summoned s && s.isClone())clones++;
                        if(entity instanceof net.minecraft.world.entity.LivingEntity living){if(dev.royalespells.VisualState.raged(living))raged++;if(dev.royalespells.VisualState.frozen(living))frozen++;if(dev.royalespells.VisualState.rooted(living))rooted++;}
                    }
                    if(clones!=1||raged!=1||frozen!=1||rooted!=1)throw new IllegalStateException("Missing client visual states "+clones+","+raged+","+frozen+","+rooted);
                    Screenshot.grab(client.gameDirectory,"royale-status-models.png",client.getMainRenderTarget(),message->System.out.println("ROYALE_STATUS_VISUAL_COMPLETE"));
                }
                if(ready==410)client.getSingleplayerServer().execute(()->{
                    var world=client.getSingleplayerServer().overworld();var player=client.getSingleplayerServer().getPlayerList().getPlayers().get(0);var at=new Vec3(80,150,-4);
                    var zap=SpellEntity.create(world,Spell.ZAP_EVOLUTION,player.getUUID(),at,at);zap.setPreviewTime(2);zap.preview=true;world.addFreshEntity(zap);
                });
                if(ready==430)Screenshot.grab(client.gameDirectory,"royale-zap-first.png",client.getMainRenderTarget(),message->{});
                if(ready==440)client.getSingleplayerServer().execute(()->{for(var entity:client.getSingleplayerServer().overworld().getAllEntities())if(entity instanceof SpellEntity fx && fx.spell()==Spell.ZAP_EVOLUTION && fx.preview)fx.setPreviewTime(22);});
                if(ready==460)Screenshot.grab(client.gameDirectory,"royale-zap-second.png",client.getMainRenderTarget(),message->System.out.println("ROYALE_ZAP_VISUAL_COMPLETE"));
                if(ready==490){if(graveyardSounds!=1)throw new IllegalStateException("Expected one Graveyard deploy sound, got "+graveyardSounds);System.out.println("ROYALE_AUDIO_SMOKE_COMPLETE");}
                TroopVisualSmoke.tick(client,ready);
            }
            if(ticks>3600){System.err.println("ROYALE_VISUAL_SMOKE_TIMEOUT stage="+stage);client.stop();}
        });
    }
}


