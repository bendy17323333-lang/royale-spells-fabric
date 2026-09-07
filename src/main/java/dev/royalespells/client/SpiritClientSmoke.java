package dev.royalespells.client;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import dev.royalespells.iron.SpiritSpells;
import dev.royalespells.spirit.SpiritElement;
import net.minecraft.client.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.Consumer;

/** Opt-in client-only gallery in a disposable world; never enabled during ordinary play. */
final class SpiritClientSmoke {
    private static int tick,leapFrames,resizeWait;private static volatile boolean ready;private static UUID liveTarget,nightSpirit;
    private static final List<UUID> chainVictims=new ArrayList<>(),armyUnits=new ArrayList<>();
    private static final Set<Integer> seenArcs=new HashSet<>();private static float armyBefore;
    private static final List<UUID> models=new ArrayList<>();
    private static final List<UUID> audioTargets=new ArrayList<>();
    private static final List<ElementalSpirit> audioSpirits=new ArrayList<>();
    private static final Set<String> audioPlayed=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static void server(Minecraft c,Consumer<ServerPlayer> work){c.getSingleplayerServer().execute(()->{try{work.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst());}catch(Throwable e){e.printStackTrace();System.err.println("ROYALE_SPIRIT_CLIENT_FAILURE");c.execute(c::stop);}});}
    private static void shot(Minecraft c,String name){var frame=c.getMainRenderTarget();if(frame.width<1280||frame.height<720)throw new IllegalStateException("Unusable QA framebuffer: "+frame.width+"x"+frame.height);Screenshot.grab(c.gameDirectory,"spirit-"+name+".png",frame,m->{});System.out.println("ROYALE_SPIRIT_FRAME "+name+" size="+frame.width+"x"+frame.height);}
    private static void camera(ServerPlayer p,Vec3 eye,Vec3 at){var d=at.subtract(eye);p.teleportTo(p.serverLevel(),eye.x,eye.y-p.getEyeHeight(),eye.z,(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))));}
    static void tick(Minecraft c){
        // Only this opt-in disposable client may restore its own window. A
        // minimized 1x1 framebuffer is not evidence of a successful visual run.
        if(c.getMainRenderTarget().width<1280||c.getMainRenderTarget().height<720){
            if(resizeWait++%20==0){org.lwjgl.glfw.GLFW.glfwRestoreWindow(c.getWindow().getWindow());c.getWindow().setWindowed(1600,1000);c.resizeDisplay();System.out.println("ROYALE_SPIRIT_RESIZE "+c.getWindow().getWidth()+"x"+c.getWindow().getHeight());}
            if(resizeWait>200)throw new IllegalStateException("QA window could not provide a usable framebuffer");return;
        }
        resizeWait=0;
        c.getToasts().clear();c.gui.getChat().clearMessages(false);
        if(tick>0&&!ready)return;
        if(tick++==0){
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.sound.PlaySoundSourceEvent event)->{
                var id=event.getSound().getLocation();if(id.getNamespace().equals("royalespells")&&id.getPath().startsWith("spirit_"))audioPlayed.add(id.getPath());
            });
            for(var kind:SpiritElement.values())for(var phase:List.of("deploy","step","jump","impact"))if(c.getSoundManager().getSoundEvent(RoyaleSpells.id("spirit_"+kind.id()+"_"+phase))==null)throw new IllegalStateException("Missing original spirit sound event");
            c.options.hideGui=true;server(c,p->{
            var w=p.serverLevel();for(int x=-1;x<=0;x++)for(int z=-1;z<=1;z++)w.setChunkForced(x,z,true);
            for(var entity:com.google.common.collect.ImmutableList.copyOf(w.getAllEntities()))if(entity instanceof Summoned||entity instanceof SpellEntity)entity.discard();
            p.getInventory().clearContent();int i=0;for(var kind:SpiritElement.values()){
                var e=RoyaleSpells.ELEMENTAL_SPIRIT.create(w);e.configure(p.getUUID(),kind,1);e.setup(p.getUUID(),6000,false);e.moveTo((i++-1.5)*1.5,150,0,180,0);e.setYBodyRot(180);e.setYHeadRot(180);e.setNoAi(true);w.addFreshEntity(e);models.add(e.getUUID());
                p.addItem(SpiritSpells.staff(kind).getDefaultInstance());
            }p.getInventory().selected=0;camera(p,new Vec3(0,151.8,-6),new Vec3(0,150.5,0));ready=true;
        });return;}
        if(!ready)return;
        if(tick==2&&Boolean.getBoolean("royalespells.spiritAudioOnly"))tick=2000;
        if(tick==100)shot(c,"all-four-front");
        if(tick==115)server(c,p->camera(p,new Vec3(-4,152,-4),new Vec3(0,150.5,0)));
        if(tick==165)shot(c,"all-four-angle");
        for(int i=0;i<4;i++){
            final int index=i;int start=180+i*85;
            if(tick==start)server(c,p->{var e=(ElementalSpirit)p.serverLevel().getEntity(models.get(index));camera(p,e.position().add(0,1,-1.65),e.position().add(0,.5,0));});
            if(tick==start+50)shot(c,"close-"+SpiritElement.values()[i].id());
        }
        if(tick==535){c.options.hideGui=false;server(c,p->{camera(p,new Vec3(0,151.8,-6),new Vec3(0,150.5,0));p.setItemInHand(InteractionHand.MAIN_HAND,SpiritSpells.staff(SpiritElement.FIRE).getDefaultInstance());});}
        if(tick==585)shot(c,"staff-in-hand");
        if(tick==600){c.options.setCameraType(CameraType.THIRD_PERSON_FRONT);}
        if(tick==655)shot(c,"staff-third-person");
        if(tick==670){c.options.setCameraType(CameraType.FIRST_PERSON);c.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(c.player));}
        if(tick==710)shot(c,"staff-inventory");
        if(tick==725){c.setScreen(null);c.options.hideGui=true;server(c,p->{
            for(var id:models){var e=p.serverLevel().getEntity(id);if(e!=null)e.discard();}
            camera(p,new Vec3(0,151.65,-4),new Vec3(0,150.5,1));p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            p.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA).setBaseValue(300);
            io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(p).setMana(300);
            var target=EntityType.COW.create(p.serverLevel());target.moveTo(0,150,1,180,0);target.setNoAi(true);target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);p.serverLevel().addFreshEntity(target);liveTarget=target.getUUID();SummonOrders.order(p,target);
        });}
        if(tick==750)server(c,p->{var event=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem(p,InteractionHand.MAIN_HAND);net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);if(!io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(p).isCasting())throw new IllegalStateException("Packaged staff did not start native cast");});
        if(tick>=755&&tick<865)for(var entity:c.level.entitiesForRendering())if(entity instanceof ElementalSpirit spirit&&spirit.leapTicks()>0){leapFrames++;if(leapFrames==2||leapFrames==5)shot(c,"live-jump-"+leapFrames);}
        if(tick==870){shot(c,"after-live-burst");server(c,p->{var target=(LivingEntity)p.serverLevel().getEntity(liveTarget);if(target==null||target.getHealth()>=100)throw new IllegalStateException("Packaged spirit did not damage real target");System.out.println("ROYALE_SPIRIT_NATIVE_LIVE_HIT health="+target.getHealth());});}
        // Asset regressions need every OBJ/skin variant in the packaged client.
        for(int i=1;i<4;i++){
            final var element=SpiritElement.values()[i];int start=900+(i-1)*75;
            if(tick==start){c.options.hideGui=false;server(c,p->{p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.setItemInHand(InteractionHand.MAIN_HAND,SpiritSpells.staff(element).getDefaultInstance());});}
            if(tick==start+50)shot(c,"staff-"+element.id()+"-in-hand");
        }
        if(tick==1150){c.options.hideGui=true;server(c,p->{
            p.serverLevel().setDayTime(18000);var e=RoyaleSpells.ELEMENTAL_SPIRIT.create(p.serverLevel());e.configure(p.getUUID(),SpiritElement.ICE,1);e.setup(p.getUUID(),6000,false);e.moveTo(0,150,0,180,0);e.setYBodyRot(180);e.setYHeadRot(180);e.setNoAi(true);e.setNoGravity(true);p.serverLevel().addFreshEntity(e);nightSpirit=e.getUUID();p.getAbilities().flying=true;p.onUpdateAbilities();camera(p,new Vec3(0,151,-1.9),new Vec3(0,150.55,0));
        });}
        if(tick==1180)server(c,p->camera(p,new Vec3(0,151,-1.9),new Vec3(0,150.55,0)));
        if(tick==1200){shot(c,"ice-glow-night");server(c,p->{var e=p.serverLevel().getEntity(nightSpirit);if(e==null)throw new IllegalStateException("Night ice model disappeared");System.out.println("ROYALE_SPIRIT_NIGHT_POSITION entity="+e.position()+" player="+p.position());});}
        if(tick==1250){c.options.hideGui=false;c.options.guiScale().set(4);c.resizeDisplay();server(c,p->{
            var w=p.serverLevel();w.setDayTime(6000);for(var entity:com.google.common.collect.ImmutableList.copyOf(w.getAllEntities()))if(entity instanceof Mob)entity.discard();
            p.getInventory().clearContent();p.getInventory().selected=0;p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(0));
            p.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("irons_spellbooks:ice_staff"))));
            var book=new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("irons_spellbooks:iron_spell_book")));
            var spells=io.redspace.ironsspellbooks.api.spells.ISpellContainer.create(5,true,true).mutableCopy();int i=0;
            for(var kind:SpiritElement.values())spells.addSpellAtIndex(SpiritSpells.spell(kind),1,i++,true);
            spells.addSpellAtIndex(io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBOLT_SPELL.get(),2,4,true);
            io.redspace.ironsspellbooks.api.spells.ISpellContainer.set(book,spells.toImmutable());io.redspace.ironsspellbooks.api.util.Utils.setPlayerSpellbookStack(p,book);
            camera(p,new Vec3(0,153,-7),new Vec3(0,150,1));
        });}
        if(tick==1300){
            var selection=io.redspace.ironsspellbooks.player.ClientMagicData.getSpellSelectionManager();
            if(selection.getSpellCount()<5)throw new IllegalStateException("Mixed spirit/native spellbook did not synchronize");selection.makeSelection(0);
            long portraits=selection.getAllSpells().stream().filter(s->IronCardUi.ours(s.spellData.getSpell().getSpellIconResource())).count();
            if(portraits!=4)throw new IllegalStateException("Four spirit cards must use the existing portrait UI, count="+portraits);
            for(var kind:SpiritElement.values()){String key=SpiritSpells.spell(kind).getComponentId();if(!net.minecraft.client.resources.language.I18n.exists(key))throw new IllegalStateException("Untranslated spirit spell name: "+key);}
            System.out.println("ROYALE_SPIRIT_ORIGINAL_CARDS portraits="+portraits+" native=1 ratio=302/363");
        }
        if(tick==1310)shot(c,"original-card-bar");
        if(tick==1320)io.redspace.ironsspellbooks.gui.overlays.SpellWheelOverlay.instance.open();
        if(tick==1340)shot(c,"original-card-wheel");
        if(tick==1350)io.redspace.ironsspellbooks.gui.overlays.SpellWheelOverlay.instance.close();
        if(tick==1400){c.options.hideGui=true;server(c,p->{
            p.setItemInHand(InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
            double[][] positions={{-5,0},{-2.6,.5},{-.2,0},{2.2,.5},{4.6,0},{4.6,2.4},{2.2,2.9},{-.2,2.4},{-2.6,2.9}};
            for(var xy:positions){var v=EntityType.COW.create(p.serverLevel());v.moveTo(xy[0],150,xy[1]);v.setNoAi(true);v.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);v.setHealth(100);p.serverLevel().addFreshEntity(v);chainVictims.add(v.getUUID());}
            camera(p,new Vec3(5,154,-6),new Vec3(0,150.5,1));
        });}
        if(tick==1460)server(c,p->{var e=RoyaleSpells.ELEMENTAL_SPIRIT.create(p.serverLevel());e.configure(p.getUUID(),SpiritElement.ELECTRO,1);e.moveTo(-5,150,-1.5);e.setNoAi(true);p.serverLevel().addFreshEntity(e);e.detonate((LivingEntity)p.serverLevel().getEntity(chainVictims.getFirst()));});
        if(tick>=1460&&tick<1520)for(var e:c.level.entitiesForRendering())if(e instanceof SpiritArc arc&&arc.age()>=2&&seenArcs.add(e.getId())){
            if(Set.of(1,3,6,9).contains(seenArcs.size()))shot(c,"chain-link-"+seenArcs.size());
        }
        if(tick>=1450&&tick<=1520&&tick%2==0)shot(c,String.format(java.util.Locale.ROOT,"chain-motion-%03d",(tick-1450)/2));
        if(tick==1530)server(c,p->{
            long hits=chainVictims.stream().map(id->(LivingEntity)p.serverLevel().getEntity(id)).filter(v->v!=null&&v.getHealth()<100).count();
            if(hits!=9)throw new IllegalStateException("Packaged sequential chain hit "+hits+" targets");
            System.out.println("ROYALE_SPIRIT_LIVE_CHAIN victims="+hits+" arcs="+seenArcs.size());
            for(var id:chainVictims){var e=p.serverLevel().getEntity(id);if(e!=null)e.discard();}
        });
        if(tick==1600)server(c,p->{
            var list=dev.royalespells.army.ArmyFormation.neutral(p.serverLevel(),new Vec3(0,150,-3),0);if(list.size()!=16)throw new IllegalStateException("Packaged formation blocked");list.forEach(e->armyUnits.add(e.getUUID()));
            var target=EntityType.COW.create(p.serverLevel());target.moveTo(0,150,4.5);target.setNoAi(true);target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(500);target.setHealth(500);p.serverLevel().addFreshEntity(target);liveTarget=target.getUUID();
            camera(p,new Vec3(7,155,-8),new Vec3(0,150.6,1));
        });
        if(tick==1640)shot(c,"army-guards-approach");
        if(tick==1680)server(c,p->camera(p,new Vec3(5,153.5,-2),new Vec3(0,150.8,4)));
        if(tick==1710)shot(c,"army-general-protected");
        if(tick==1740)server(c,p->{
            var target=(LivingEntity)p.serverLevel().getEntity(liveTarget);armyBefore=target.getHealth();int i=0;
            for(var id:armyUnits){var s=(ArmySkeleton)p.serverLevel().getEntity(id);if(!s.general()&&i++%2==0){s.hurt(p.damageSources().generic(),100);s.moveTo((i%3-1)*.7,150,-2-(i/3)*.5);}}
        });
        if(tick==1760)shot(c,"army-ghosts-returning");
        if(tick==1820)shot(c,"army-ghosts-flanking");
        if(tick==1880)shot(c,"army-mixed-melee");
        if(tick==1900)server(c,p->{var target=(LivingEntity)p.serverLevel().getEntity(liveTarget);if(armyBefore-target.getHealth()<20)throw new IllegalStateException("Live mixed army did not resume attacks");System.out.println("ROYALE_SPIRIT_LIVE_ARMY damageAfterConversion="+(armyBefore-target.getHealth()));});
        if(tick==1910)server(c,p->{for(var id:armyUnits){var e=p.serverLevel().getEntity(id);if(e instanceof ArmySkeleton s&&s.general()){s.hurt(p.damageSources().generic(),100);s.hurt(p.damageSources().generic(),100);}}});
        if(tick==1960){shot(c,"army-living-survivors");server(c,p->{
            long alive=armyUnits.stream().map(p.serverLevel()::getEntity).filter(e->e instanceof ArmySkeleton s&&!s.general()&&!s.ghost()&&s.isAlive()&&s.dissolve()==0).count();
            long ghosts=armyUnits.stream().map(p.serverLevel()::getEntity).filter(e->e instanceof ArmySkeleton s&&s.ghost()&&!s.isRemoved()).count();
            if(alive!=7||ghosts!=0)throw new IllegalStateException("General death survivors="+alive+" ghosts="+ghosts);System.out.println("ROYALE_BETA2_SURVIVORS living="+alive+" ghosts="+ghosts);
        });}
        if(tick==2000)server(c,p->{
            for(var e:com.google.common.collect.ImmutableList.copyOf(p.serverLevel().getAllEntities()))if(e instanceof Mob)e.discard();
            int i=0;for(var kind:SpiritElement.values()){
                double x=(i++-1.5)*5;var victim=EntityType.HUSK.create(p.serverLevel());victim.moveTo(x,150,4);victim.setNoAi(true);victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);victim.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(0);victim.setHealth(100);p.serverLevel().addFreshEntity(victim);audioTargets.add(victim.getUUID());
                var spirit=RoyaleSpells.ELEMENTAL_SPIRIT.create(p.serverLevel());spirit.configure(p.getUUID(),kind,1);spirit.moveTo(x,150,-2);p.serverLevel().addFreshEntity(spirit);spirit.setTarget(victim);audioSpirits.add(spirit);
            }camera(p,new Vec3(4,156,-9),new Vec3(0,150.5,1));
        });
        if(Boolean.getBoolean("royalespells.traceSpiritFlight")&&tick>=2000&&tick<=2060&&tick%2==0)server(c,p->{for(var e:audioSpirits)if(e.element()==SpiritElement.ELECTRO)System.out.println("ROYALE_BETA2_ELECTRO_FLIGHT age="+e.tickCount+" pos="+e.position()+" velocity="+e.getDeltaMovement()+" leap="+e.leapTicks()+" chain="+e.chainedTargets()+" target="+(e.getTarget()==null?"none":e.getTarget().position())+" removed="+e.isRemoved());});
        if(tick==2040)shot(c,"four-spirit-charge");
        if(tick==2170)server(c,p->{for(int i=0;i<audioTargets.size();i++){var e=(LivingEntity)p.serverLevel().getEntity(audioTargets.get(i));var spirit=audioSpirits.get(i);if(e==null||e.getHealth()>=100)throw new IllegalStateException("Original-audio spirit missed: "+spirit.element()+" hp="+(e==null?"missing":e.getHealth())+" pos="+spirit.position()+" leap="+spirit.leapTicks()+" chain="+spirit.chainedTargets());}System.out.println("ROYALE_BETA2_FOUR_LIVE_HITS count=4");});
        if(tick==2220){
            for(var kind:SpiritElement.values())for(var phase:List.of("deploy","step","jump","impact"))if(!audioPlayed.contains("spirit_"+kind.id()+"_"+phase))throw new IllegalStateException("Original sound never reached an actual audio channel: "+kind.id()+"/"+phase+"; heard="+audioPlayed);
            System.out.println("ROYALE_BETA2_AUDIO_PLAYED events="+new TreeSet<>(audioPlayed));
            if(Boolean.getBoolean("royalespells.spiritAudioOnly")){System.out.println("ROYALE_SPIRIT_AUDIO_DIAGNOSTIC_COMPLETE");c.stop();return;}
            if(leapFrames==0||seenArcs.size()!=9){System.err.println("ROYALE_SPIRIT_CLIENT_FAILURE jump/arc synchronization");c.stop();return;}System.out.println("ROYALE_SPIRIT_CLIENT_COMPLETE models="+models.size()+" leapFrames="+leapFrames+" arcs="+seenArcs.size());c.stop();
        }
        if(tick>2450){System.err.println("ROYALE_SPIRIT_CLIENT_TIMEOUT");c.stop();}
    }
}
