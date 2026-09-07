package dev.royalespells.client;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import dev.royalespells.iron.*;
import io.redspace.ironsspellbooks.api.magic.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.gui.scroll_forge.*;
import io.redspace.ironsspellbooks.gui.inscription_table.*;
import io.redspace.ironsspellbooks.gui.arcane_anvil.*;
import io.redspace.ironsspellbooks.item.InkItem;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.Consumer;

/** Opt-in production-JAR QA, using an isolated save and actual client/server menu packets. */
final class IronSystemClientSmoke {
    private static UUID fieldId,cloneId;
    private static volatile boolean bookPassed,mirrorPassed;
    private static volatile boolean setupDone;
    private static boolean setupQueued,clientReady;
    private static int sequence,waiting;
    private static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));}
    private static ItemStack ink(SpellRarity rarity){return BuiltInRegistries.ITEM.stream().filter(i->i instanceof InkItem in && in.getRarity()==rarity).findFirst().map(ItemStack::new).orElseThrow();}
    private static void server(Minecraft c,Consumer<ServerPlayer> action){c.getSingleplayerServer().execute(()->action.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst()));}
    private static void camera(Minecraft c,float yaw,float pitch){server(c,p->p.teleportTo(p.serverLevel(),0,154,-10,yaw,pitch));}
    private static void held(Minecraft c,IronSpellProfile spell){server(c,p->{p.getInventory().clearContent();p.getInventory().selected=0;p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));p.setItemInHand(InteractionHand.MAIN_HAND,IronIntegration.scroll(spell,1));});}
    private static void shot(Minecraft c,String name){Screenshot.grab(c.gameDirectory,"iron-system-"+name+".png",c.getMainRenderTarget(),message->System.out.println("ROYALE_IRON_SYSTEM_FRAME "+name));}
    private static void field(Minecraft c,Spell spell,int age) {
        server(c,p->{var w=p.serverLevel();for(var e:com.google.common.collect.ImmutableList.copyOf(w.getAllEntities()))if(e instanceof SpellEntity)e.discard();
            var at=new Vec3(0,150,0);var fx=SpellEntity.create(w,spell,p.getUUID(),at,at);fx.preview=true;fx.setPreviewTime(age);w.addFreshEntity(fx);fieldId=fx.getUUID();
        });
    }
    private static void age(Minecraft c,int age){server(c,p->{if(p.serverLevel().getEntity(fieldId) instanceof SpellEntity fx)fx.setPreviewTime(age);});}
    static void tick(Minecraft c,int elapsed) {
        int t=0;
        if(setupQueued && !clientReady) {
            var selection=IronClientPreview.selected(c.player);
            if(!setupDone || selection==null || selection.profile!=IronSpellProfile.ZAP || !c.level.hasChunkAt(new BlockPos(0,149,0))) {
                if(++waiting>1200)throw new IllegalStateException("QA setup did not sync: server="+setupDone+" hand="+c.player.getMainHandItem());return;
            }
            clientReady=true;
        }
        if(clientReady)t=++sequence;
        if(t==0) {
            setupQueued=true;
            c.options.setCameraType(CameraType.FIRST_PERSON);c.options.hideGui=false;
            server(c,p->{var w=p.serverLevel();for(var e:com.google.common.collect.ImmutableList.copyOf(w.getAllEntities()))if(e!=p)e.discard();
                for(int x=-22;x<=22;x++)for(int z=-25;z<=20;z++) {
                    w.setBlockAndUpdate(new BlockPos(x,149,z),((x+z)%2==0?Blocks.STONE_BRICKS:Blocks.SMOOTH_STONE).defaultBlockState());
                    for(int y=150;y<163;y++)w.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
                }
                p.teleportTo(w,0,154,-10,0,38);p.getInventory().clearContent();p.getInventory().selected=0;p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));p.setItemInHand(InteractionHand.MAIN_HAND,IronIntegration.scroll(IronSpellProfile.ZAP,1));
                setupDone=true;System.out.println("ROYALE_IRON_SYSTEM_SETUP_READY");
            });
        }
        if(t==65){shot(c,"aim-zap-front");var selected=IronClientPreview.selected(c.player);if(selected==null || selected.profile!=IronSpellProfile.ZAP)throw new IllegalStateException("Native held scroll preview is missing; hand="+c.player.getMainHandItem());}
        if(t==75)camera(c,55,38);
        if(t==95)shot(c,"aim-zap-turn-55");
        if(t==105)camera(c,135,38);
        if(t==125)shot(c,"aim-zap-turn-135");
        if(t==135){camera(c,0,38);held(c,IronSpellProfile.THE_LOG);}
        if(t==155)shot(c,"aim-log-path");
        if(t==165)held(c,IronSpellProfile.BARBARIAN_BARREL);
        if(t==185)shot(c,"aim-barrel-path");
        if(t==195)held(c,IronSpellProfile.GOBLIN_BARREL_EVOLUTION);
        if(t==215)shot(c,"aim-evolved-barrel-two-landings");
        if(t==225){camera(c,0,24);c.options.hideGui=true;field(c,Spell.VOID,2);}
        if(t==240)shot(c,"void-opening");
        if(t==245)age(c,30);
        if(t==265)shot(c,"void-active");
        if(t==275)age(c,76);
        if(t==285)shot(c,"void-closing");
        if(t==295)field(c,Spell.GRAVEYARD,3);
        if(t==310)shot(c,"graveyard-opening");
        if(t==315)age(c,70);
        if(t==355)shot(c,"graveyard-drifting-wisps");
        if(t==365)age(c,195);
        if(t==380)shot(c,"graveyard-closing");
        if(t==390)field(c,Spell.RAGE,1);
        if(t==405)shot(c,"rage-opening");
        if(t==410)age(c,25);
        if(t==425)shot(c,"rage-active");
        if(t==435)age(c,116);
        if(t==450)shot(c,"rage-fade");
        if(t==460)field(c,Spell.ARROWS,2);
        if(t==480)shot(c,"arrows-full-radius");
        if(t==490) {
            c.options.hideGui=false;c.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            server(c,p->{p.teleportTo(p.serverLevel(),0,150,0,180,8);
                for(var e:com.google.common.collect.ImmutableList.copyOf(p.serverLevel().getAllEntities()))if(e instanceof SpellEntity)e.discard();
                var at=p.position().add(0,1.3,0);var fx=SpellEntity.create(p.serverLevel(),Spell.RAGE,p.getUUID(),at,at);fx.preview=true;fx.setPreviewTime(25);p.serverLevel().addFreshEntity(fx);
            });
        }
        if(t==510)RangeDepthAudit.requested=true;
        if(t==535){if(!RangeDepthAudit.complete)throw new IllegalStateException("Transparent field depth audit incomplete");shot(c,"field-player-depth");}
        if(t==545) {
            c.options.setCameraType(CameraType.FIRST_PERSON);
            server(c,p->{var w=p.serverLevel();for(var e:com.google.common.collect.ImmutableList.copyOf(w.getAllEntities()))if(e instanceof SpellEntity)e.discard();p.teleportTo(w,0,150,-3,0,0);
                var at=new BlockPos(1,150,0);var state=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("irons_spellbooks:scroll_forge")).defaultBlockState();w.setBlockAndUpdate(at,state);
            });
        }
        if(t==560) {
            server(c,p->{var w=p.serverLevel();var at=new BlockPos(1,150,0);var state=w.getBlockState(at);
                p.openMenu(state.getMenuProvider(w,at),buf->buf.writeBlockPos(at));
                var menu=(ScrollForgeMenu)p.containerMenu;var spell=IronIntegration.spell(IronSpellProfile.VOID);
                menu.getInkSlot().set(ink(SpellRarity.EPIC));menu.getBlankScrollSlot().set(new ItemStack(Items.PAPER));
                menu.getFocusSlot().set(BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(stack->spell.getSchoolType().isFocus(stack)).findFirst().orElseThrow());menu.setRecipeSpell(spell);menu.broadcastChanges();
            });
        }
        if(t==575) {
            if(!(c.screen instanceof ScrollForgeScreen screen))throw new IllegalStateException("Native scroll forge screen missing: "+c.screen);
            screen.generateSpellList();
            for(var child:screen.children())if(child instanceof net.minecraft.client.gui.components.Button button && button.getMessage().getString().contains("虚空"))button.onPress();
        }
        if(t==590){if(!(c.screen instanceof ScrollForgeScreen screen) || screen.getMenu().getResultSlot().getItem().isEmpty())throw new IllegalStateException("Forge result did not sync to client");shot(c,"native-scroll-forge");}
        if(t==600)server(c,p->{p.closeContainer();p.openMenu(new SimpleMenuProvider((id,inv,player)->new InscriptionTableMenu(id,inv,ContainerLevelAccess.NULL),Component.translatable("block.irons_spellbooks.inscription_table")));
            var menu=(InscriptionTableMenu)p.containerMenu;menu.getSpellBookSlot().set(item("irons_spellbooks:iron_spell_book"));menu.getScrollSlot().set(IronIntegration.scroll(IronSpellProfile.GRAVEYARD,2));menu.broadcastChanges();
        });
        if(t==625){if(!(c.screen instanceof InscriptionTableScreen))throw new IllegalStateException("Inscription screen missing");shot(c,"native-inscription");}
        if(t==635)server(c,p->{var menu=(InscriptionTableMenu)p.containerMenu;menu.clickMenuButton(p,0);menu.clickMenuButton(p,-1);menu.broadcastChanges();});
        if(t==650)shot(c,"native-inscription-result");
        if(t==660)server(c,p->{p.closeContainer();p.openMenu(new SimpleMenuProvider((id,inv,player)->new ArcaneAnvilMenu(id,inv,ContainerLevelAccess.NULL),Component.translatable("block.irons_spellbooks.arcane_anvil")));
            var menu=(ArcaneAnvilMenu)p.containerMenu;menu.getSlot(0).set(IronIntegration.scroll(IronSpellProfile.ZAP,2));menu.getSlot(1).set(ink(IronIntegration.spell(IronSpellProfile.ZAP).getRarity(3)));menu.createResult();menu.broadcastChanges();
        });
        if(t==685){if(!(c.screen instanceof ArcaneAnvilScreen screen) || screen.getMenu().getSlot(2).getItem().isEmpty())throw new IllegalStateException("Anvil upgrade output missing");shot(c,"native-anvil-upgrade");}
        if(t==700)server(c,p->{p.closeContainer();var w=p.serverLevel();p.teleportTo(w,0,154,-10,0,35);p.getInventory().clearContent();
            p.getInventory().selected=0;p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));p.setItemInHand(InteractionHand.MAIN_HAND,item("irons_spellbooks:ice_staff"));
            var book=item("irons_spellbooks:iron_spell_book");var container=ISpellContainer.create(4,true,true).mutableCopy();
            container.addSpellAtIndex(IronIntegration.spell(IronSpellProfile.ZAP),2,0,true);
            container.addSpellAtIndex(IronIntegration.spell(IronSpellProfile.MIRROR),1,1,true);
            container.addSpellAtIndex(SpellRegistry.FIREBOLT_SPELL.get(),2,2,true);ISpellContainer.set(book,container.toImmutable());Utils.setPlayerSpellbookStack(p,book);
        });
        if(t==730){if(IronClientPreview.selected(c.player).profile!=IronSpellProfile.ZAP)throw new IllegalStateException("Equipped book targeting preview missing");shot(c,"equipped-book-target-preview");}
        if(t==740)server(c,p->{if(!Utils.serverSideInitiateCast(p))throw new IllegalStateException("Actual selected-book cast failed");});
        if(t==755){server(c,p->{bookPassed=MirrorIronSpell.history(p).getSpell()==IronIntegration.spell(IronSpellProfile.ZAP);});io.redspace.ironsspellbooks.player.ClientMagicData.getSpellSelectionManager().makeSelection(2);}
        if(t==770)server(c,p->{if(!Utils.serverSideInitiateCast(p))throw new IllegalStateException("Native Firebolt failed");});
        if(t==805)io.redspace.ironsspellbooks.player.ClientMagicData.getSpellSelectionManager().makeSelection(1);
        if(t==815)server(c,p->{if(!Utils.serverSideInitiateCast(p))throw new IllegalStateException("Actual book Mirror failed");
            var magic=MagicData.getPlayerMagicData(p);mirrorPassed=magic.getCastingSpellId().equals("irons_spellbooks:firebolt") && magic.getCastingSpellLevel()==3;
            System.out.println("ROYALE_IRON_BOOK_MIRROR_CHECK book="+bookPassed+" mirror="+mirrorPassed+" source="+magic.getCastingSpellId()+" level="+magic.getCastingSpellLevel());
        });
        if(t==821)shot(c,"mirror-native-firebolt-cast");
        if(t==870)server(c,p->{if(MagicData.getPlayerMagicData(p).isCasting())throw new IllegalStateException("Mirrored Firebolt did not finish");
            p.teleportTo(p.serverLevel(),0,151,-7,0,0);Utils.setPlayerSpellbookStack(p,ItemStack.EMPTY);
            var original=(Mob)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("irons_spellbooks:summoned_zombie")).create(p.level());original.setPos(-1.5,150,0);original.setNoAi(true);original.setYRot(180);original.setYBodyRot(180);p.serverLevel().addFreshEntity(original);SummonManager.setOwner(original,p);
            var clone=NativeClones.copy(original,p.getUUID(),1,"royalespells:clone",1);clone.setPos(1.5,150,0);cloneId=clone.getUUID();
        });
        if(t==905){boolean synced=false;for(var e:c.level.entitiesForRendering())if(e.getUUID().equals(cloneId) && e instanceof LivingEntity living)synced=VisualState.cloned(living);
            if(!synced)throw new IllegalStateException("Native clone visual flag not synced");shot(c,"native-summon-cyan-clone");
        }
        if(t==940){if(!bookPassed || !mirrorPassed)throw new IllegalStateException("Native selected-book lifecycle checks failed: book="+bookPassed+" mirror="+mirrorPassed);System.out.println("ROYALE_IRON_SYSTEM_CLIENT_COMPLETE");c.stop();}
    }
    private IronSystemClientSmoke(){}
}
