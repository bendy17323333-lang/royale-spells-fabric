package dev.royalespells.client;
import dev.royalespells.*;
import dev.royalespells.entity.SpellEntity;
import dev.royalespells.iron.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.gui.overlays.SpellWheelOverlay;
import net.minecraft.client.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.Consumer;

/** Opt-in packaged-client screenshots, selection transitions and actual sound-engine events. */
final class PolishClientSmoke {
    private static boolean queued,ready;
    private static volatile boolean serverReady;
    private static int tick,wait,settle;
    private static UUID effectId;
    private static final Set<ResourceLocation> heard=new HashSet<>();
    private static List<String> cues;
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));}
    private static void server(Minecraft c,Consumer<ServerPlayer> action){c.getSingleplayerServer().execute(()->action.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst()));}
    private static void hand(Minecraft c,ItemStack stack){server(c,p->{p.getInventory().clearContent();p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,stack);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));});}
    private static void shot(Minecraft c,String name){Screenshot.grab(c.gameDirectory,"polish-"+name+".png",c.getMainRenderTarget(),message->{});System.out.println("ROYALE_POLISH_FRAME "+name+" contours="+TargetPreview.contours.size());}
    private static void field(Minecraft c,Spell spell,int age,boolean paused){server(c,p->{
        var w=p.serverLevel();for(var e:com.google.common.collect.ImmutableList.copyOf(w.getAllEntities()))if(e instanceof SpellEntity)e.discard();
        var at=new Vec3(0,150,0);var fx=SpellEntity.create(w,spell,p.getUUID(),at,at);fx.preview=paused;if(paused)fx.setPreviewTime(age);w.addFreshEntity(fx);effectId=fx.getUUID();
    });}
    private static void noPreview(String context){if(!TargetPreview.contours.isEmpty() || IronClientPreview.selected(Minecraft.getInstance().player)!=null)throw new IllegalStateException("Stale targeting preview: "+context);System.out.println("ROYALE_PREVIEW_HIDDEN "+context);}
    private static void decodeAudio(Minecraft c) {
        try(var source=c.getResourceManager().open(RoyaleSpells.id("spell_audio.json"))) {
            var profiles=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(source)).getAsJsonObject();
            var files=new HashSet<String>();profiles.entrySet().forEach(p->p.getValue().getAsJsonObject().entrySet().forEach(s->files.add(s.getValue().getAsJsonObject().get("file").getAsString())));
            for(String file:files)try(var stream=c.getResourceManager().open(RoyaleSpells.id("sounds/original/"+file+".ogg"));var audio=new net.minecraft.client.sounds.JOrbisAudioStream(stream)) {
                if(audio.getFormat().getChannels()!=1 || audio.readAll().remaining()<100)throw new IllegalStateException("Invalid positional audio: "+file);
            }
            System.out.println("ROYALE_ORIGINAL_AUDIO_DECODED files="+files.size()+" profiles="+profiles.size());
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
        cues=new ArrayList<>(SpellSounds.cues().keySet());
        c.getSoundManager().addListener((sound,set,range)->{
            if(sound.getLocation().getNamespace().equals("royalespells")) {
                if(sound.getSound()==net.minecraft.client.sounds.SoundManager.EMPTY_SOUND || sound.getPitch()!=1 || sound.isRelative())throw new IllegalStateException("Invalid original sound playback: "+sound.getLocation());
                heard.add(sound.getLocation());
            }
        });
    }
    static void tick(Minecraft c) {
        c.getToasts().clear();c.gui.getChat().clearMessages(false);
        if(!queued) {
            queued=true;c.options.hideGui=false;c.options.setCameraType(CameraType.FIRST_PERSON);c.options.guiScale().set(4);c.resizeDisplay();decodeAudio(c);
            server(c,p->{var w=p.serverLevel();for(var e:com.google.common.collect.ImmutableList.copyOf(w.getAllEntities()))if(e!=p)e.discard();
                p.teleportTo(w,0,153,-9,0,27);p.getInventory().clearContent();p.getInventory().selected=0;p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
                p.setItemInHand(InteractionHand.MAIN_HAND,item("irons_spellbooks:ice_staff"));
                var book=item("irons_spellbooks:iron_spell_book");var spells=ISpellContainer.create(5,true,true).mutableCopy();
                spells.addSpellAtIndex(IronIntegration.spell(IronSpellProfile.GRAVEYARD),2,0,true);
                spells.addSpellAtIndex(IronIntegration.spell(IronSpellProfile.ZAP_EVOLUTION),1,1,true);
                spells.addSpellAtIndex(SpellRegistry.FIREBOLT_SPELL.get(),2,2,true);
                spells.addSpellAtIndex(IronIntegration.spell(IronSpellProfile.POISON),1,3,true);
                ISpellContainer.set(book,spells.toImmutable());Utils.setPlayerSpellbookStack(p,book);serverReady=true;
            });return;
        }
        if(!ready) {
            if(!serverReady || ClientMagicData.getSpellSelectionManager().getSpellCount()<4 || !c.player.getMainHandItem().is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("irons_spellbooks:ice_staff")))) {
                if(++wait>1000)throw new IllegalStateException("Polish QA setup did not synchronize");return;
            }
            if(++settle<80)return;
            ready=true;ClientMagicData.getSpellSelectionManager().makeSelection(0);
        }
        int t=++tick;
        if(t==30){if(IronClientPreview.selected(c.player)==null)throw new IllegalStateException("Held staff preview missing");shot(c,"mixed-bar-portraits");}
        if(t==40)SpellWheelOverlay.instance.open();
        if(t==55)shot(c,"mixed-wheel-original-frames");
        if(t==65){SpellWheelOverlay.instance.close();ClientMagicData.getSpellSelectionManager().makeSelection(2);}
        if(t==80){noPreview("native-spell-selected");shot(c,"native-selection-no-royale-preview");ClientMagicData.getSpellSelectionManager().makeSelection(0);}
        if(t==90)hand(c,new ItemStack(Items.STONE));
        if(t==105){noPreview("ordinary-item-with-book-equipped");shot(c,"ordinary-item-no-preview");}
        if(t==112)hand(c,ItemStack.EMPTY);
        if(t==125){noPreview("empty-hand-with-book-equipped");shot(c,"empty-hand-no-preview");}
        if(t==135)hand(c,IronIntegration.scroll(IronSpellProfile.ZAP_EVOLUTION,1));
        if(t==150){if(IronClientPreview.selected(c.player).profile!=IronSpellProfile.ZAP_EVOLUTION || TargetPreview.contours.size()!=2)throw new IllegalStateException("Scroll not authoritative over book selection");shot(c,"scroll-preview-no-extra-crosshair");}
        if(t==160){hand(c,ItemStack.EMPTY);field(c,Spell.POISON,0,false);}
        if(t==175)shot(c,"poison-opening");
        if(t==220)shot(c,"poison-active-haze");
        if(t==240)shot(c,"poison-bubbles-moving");
        if(t==255)field(c,Spell.GOBLIN_CURSE,0,false);
        if(t==280)shot(c,"curse-active");
        if(t==305)field(c,Spell.HEAL,0,false);
        if(t==325)shot(c,"heal-pulses");
        if(t==340)field(c,Spell.WARMTH,0,false);
        if(t==365)shot(c,"warmth-pulses");
        if(t==380)field(c,Spell.ZAP_EVOLUTION,2,true);
        if(t==395)shot(c,"zap-first-blue");
        if(t==405)server(c,p->{if(p.serverLevel().getEntity(effectId) instanceof SpellEntity fx)fx.setPreviewTime(22);});
        if(t==420)shot(c,"zap-second-purple");
        if(t==435){c.options.setCameraType(CameraType.THIRD_PERSON_FRONT);server(c,p->{p.teleportTo(p.serverLevel(),0,150,0,180,8);for(var e:com.google.common.collect.ImmutableList.copyOf(p.serverLevel().getAllEntities()))if(e instanceof SpellEntity)e.discard();var at=p.position().add(0,1.3,0);var fx=SpellEntity.create(p.serverLevel(),Spell.POISON,p.getUUID(),at,at);fx.preview=true;fx.setPreviewTime(40);p.serverLevel().addFreshEntity(fx);});}
        if(t==450){RangeDepthAudit.complete=false;RangeDepthAudit.requested=true;}
        if(t==465){if(!RangeDepthAudit.complete)throw new IllegalStateException("Poison depth audit incomplete");shot(c,"poison-player-depth");}
        if(t==475){c.options.setCameraType(CameraType.FIRST_PERSON);server(c,p->{p.teleportTo(p.serverLevel(),0,153,-9,0,27);for(var e:com.google.common.collect.ImmutableList.copyOf(p.serverLevel().getAllEntities()))if(e instanceof SpellEntity)e.discard();});}
        if(t>=490 && (t-490)%8==0) {
            int i=(t-490)/8;
            if(i<cues.size()){String key=cues.get(i);server(c,p->{String[] parts=key.split("/");SpellSounds.play(p.level(),new Vec3(0,150,0),parts[0],parts[1]);});}
            else if(i==cues.size()+5) {
                var missing=SpellSounds.cues().values().stream().map(cue->cue.sound().getLocation()).filter(id->!heard.contains(id)).toList();
                if(!missing.isEmpty())throw new IllegalStateException("Original sound events not played: "+missing);
                System.out.println("ROYALE_POLISH_CLIENT_COMPLETE cues="+cues.size()+" nativeUiRetained=true handGating=true");c.stop();
            }
        }
    }
    private PolishClientSmoke(){}
}
