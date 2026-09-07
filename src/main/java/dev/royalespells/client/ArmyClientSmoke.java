package dev.royalespells.client;

import dev.royalespells.*;
import dev.royalespells.army.*;
import dev.royalespells.entity.*;
import dev.royalespells.elixir.*;
import dev.royalespells.iron.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.Consumer;

/** Opt-in, isolated packaged client evidence. Never runs in a normal player instance. */
final class ArmyClientSmoke {
    private static boolean queued;private static volatile boolean ready;private static int tick;private static UUID general,normal;private static float mana;
    private static final BlockPos POOL=new BlockPos(0,149,12);private static int voidAudio,farAudio,attackFrames;private static boolean audioRange=true;private static float maxAttack;
    private static void server(Minecraft c,Consumer<ServerPlayer> work){c.getSingleplayerServer().execute(()->{try{work.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst());}catch(Throwable t){t.printStackTrace();System.err.println("ROYALE_ARMY_CLIENT_FAILURE");c.execute(c::stop);}});}
    private static void shot(Minecraft c,String name){Screenshot.grab(c.gameDirectory,"army-"+name+".png",c.getMainRenderTarget(),m->{});System.out.println("ROYALE_ARMY_FRAME "+name);}
    private static void face(ServerPlayer p,Vec3 camera,Vec3 at){var d=at.subtract(camera.add(0,p.getEyeHeight(),0));p.teleportTo(p.serverLevel(),camera.x,camera.y,camera.z,(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))));}
    private static void gallery(ServerPlayer p){
        var w=p.serverLevel();for(var e:com.google.common.collect.ImmutableList.copyOf(w.getAllEntities()))if(e instanceof SpellEntity||e instanceof Summoned)e.discard();
        for(int x=-14;x<=14;x++)for(int z=-12;z<=24;z++){for(int y=150;y<157;y++)w.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());w.setBlockAndUpdate(new BlockPos(x,149,z),Blocks.SMOOTH_STONE.defaultBlockState());}
        var s=(AllySkeleton)SpellEngine.summon(w,p.getUUID(),new Vec3(-1,150,0),"skeleton",false);s.setup(p.getUUID(),12000,false);s.setNoAi(true);s.setYRot(180);s.setYBodyRot(180);s.setYHeadRot(180);normal=s.getUUID();
        UUID id=UUID.randomUUID();var g=RoyaleSpells.ARMY_SKELETON.create(w);g.enlist(p.getUUID(),id,true,4,1.8f,12000);g.moveTo(new Vec3(1,150,0),180,0);g.setYBodyRot(180);g.setYHeadRot(180);g.setNoAi(true);w.addFreshEntity(g);general=g.getUUID();ArmyLedger.get(p.server).start(p.getUUID(),id,general,ArmyLedger.now(p.server)+12000,0);
        TowerPools.basin(w,POOL,new net.minecraft.world.level.levelgen.structure.BoundingBox(-20,140,-20,20,160,25),true);
        p.getInventory().clearContent();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);w.setDayTime(6000);face(p,new Vec3(0,149.5,-3.4),new Vec3(0,150.75,0));
    }
    static void tick(Minecraft c){
        c.getToasts().clear();c.gui.getChat().clearMessages(false);
        if(!queued){queued=true;c.options.hideGui=true;server(c,p->{ElixirClientSmoke.tower(p);face(p,Vec3.atBottomCenterOf(ElixirClientSmoke.dark).add(0,1,-3),Vec3.atCenterOf(ElixirClientSmoke.dark));ready=true;});return;}
        if(!ready)return;int t=++tick;
        if(t==90)shot(c,"deep-natural-crypt");
        if(t==100)server(c,ArmyClientSmoke::gallery);
        if(t==160)shot(c,"skeleton-and-general-front");
        if(t==170)server(c,p->face(p,new Vec3(2.5,149.6,2.8),new Vec3(1,150.85,0)));
        if(t==205)shot(c,"general-banner-back");
        if(t==215)server(c,p->{face(p,new Vec3(1,149.7,-2),new Vec3(1,151,0));p.serverLevel().setDayTime(18000);});
        if(t==245)shot(c,"general-glowing-eyes");
        if(t==255)server(c,p->{p.serverLevel().setDayTime(6000);face(p,new Vec3(3,151,7),Vec3.atCenterOf(POOL).add(0,1,0));var i=new ItemEntity(p.level(),.5,150,12.5,IronIntegration.scroll(IronSpellProfile.GRAVEYARD,3));p.serverLevel().addFreshEntity(i);});
        if(t==325)shot(c,"scroll-ritual-ascending");
        if(t==375)shot(c,"scroll-ritual-awakening");
        if(t==425)server(c,p->{var drops=p.serverLevel().getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(POOL).inflate(5));if(drops.stream().noneMatch(i->ArmyMagic.armyScroll(i.getItem())))throw new IllegalStateException("Ritual did not produce the native evolved scroll");for(var item:drops)if(ArmyMagic.armyScroll(item.getItem())){item.setPos(.5,149.9,12.5);item.setDeltaMovement(Vec3.ZERO);}p.serverLevel().addFreshEntity(new ItemEntity(p.level(),.5,150,12.5,new ItemStack(Items.GOAT_HORN)));System.out.println("ROYALE_ARMY_SCROLL_CONVERSION_VERIFIED");});
        if(t==510)shot(c,"horn-fusion");
        if(t==775)server(c,p->{var drops=p.serverLevel().getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(POOL).inflate(6));var horn=drops.stream().filter(i->ArmyMagic.horn(i.getItem())).findFirst().orElseThrow(()->new IllegalStateException("No enchanted horn after fusion"));p.setItemInHand(InteractionHand.MAIN_HAND,horn.getItem().copy());horn.discard();for(var id:List.of(general,normal)){var e=p.serverLevel().getEntity(id);if(e!=null)e.discard();}p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();face(p,new Vec3(0,151,-6),new Vec3(0,150,2));io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(p).setMana(1);mana=1;});
        if(t==815){c.options.hideGui=false;c.gameMode.useItem(c.player,InteractionHand.MAIN_HAND);}
        if(t==850)server(c,p->{var data=io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(p);if(!ArmyLedger.get(p.server).active(p.getUUID(),ArmyLedger.now(p.server)))throw new IllegalStateException("Client horn use did not summon");var army=p.serverLevel().getEntitiesOfClass(ArmySkeleton.class,p.getBoundingBox().inflate(50));if(army.size()!=16)throw new IllegalStateException("Wrong army count "+army.size());var g=army.stream().filter(ArmySkeleton::general).findFirst().orElseThrow();general=g.getUUID();var s=army.stream().filter(e->!e.general()).findFirst().orElseThrow();s.hurt(p.damageSources().generic(),100);if(!s.ghost())throw new IllegalStateException("Defeated skeleton did not become spectral");System.out.println("ROYALE_ARMY_HORN_NATIVE_CLIENT_INPUT count=16 mana="+data.getMana()+" ghost=true");face(p,new Vec3(5,152,-5),g.position().add(0,.8,0));});
        if(t==895)shot(c,"horn-summoned-army");
        if(t==910)server(c,p->{
            c.execute(()->c.options.hideGui=true);var w=p.serverLevel();var g=(ArmySkeleton)w.getEntity(general);
            int index=0;for(var s:w.getEntitiesOfClass(ArmySkeleton.class,p.getBoundingBox().inflate(50))){s.setNoAi(true);s.getNavigation().stop();s.setDeltaMovement(Vec3.ZERO);if(!s.general()){s.teleportTo(8+(index%5)*.85,150,4+(index/5)*.85);index++;}}
            g.teleportTo(0,150,0);g.setYRot(180);g.setYBodyRot(180);g.setYHeadRot(180);g.setTarget(null);
            var target=EntityType.HUSK.create(w);target.moveTo(g.position().add(0,0,-1.3));target.setNoAi(true);target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);w.addFreshEntity(target);g.setTarget(target);
            face(p,g.position().add(2.6,-.1,-2.2),g.position().add(0,.8,0));
        });
        if(t==925)server(c,p->{var g=(ArmySkeleton)p.serverLevel().getEntity(general);if(!g.beginStrike(g.getTarget()))throw new IllegalStateException("Staff attack did not begin: age="+g.age()+" phase="+g.strikeProgress(0));System.out.println("ROYALE_ARMY_STRIKE_STARTED age="+g.age());});
        if(t>=926&&t<=985&&t%2==0)for(var e:c.level.entitiesForRendering())if(e instanceof ArmySkeleton g&&g.getUUID().equals(general)){
            float phase=g.strikeProgress(0);if(phase>0){maxAttack=Math.max(maxAttack,phase);attackFrames++;shot(c,"attack-"+t);System.out.println("ROYALE_ARMY_CLIENT_STRIKE phase="+phase);}
        }
        if(t==990){if(attackFrames<4||maxAttack<.6f){System.err.println("ROYALE_ARMY_CLIENT_FAILURE no visible strike: frames="+attackFrames+" phase="+maxAttack);c.stop();return;}System.out.println("ROYALE_ARMY_STRIKE_ANIMATION_VERIFIED frames="+attackFrames+" phase="+maxAttack);}
        if(t==995)server(c,p->{var g=(ArmySkeleton)p.serverLevel().getEntity(general);if(g.getTarget().getHealth()>=100)throw new IllegalStateException("Animated strike did not damage its target");System.out.println("ROYALE_ARMY_WINDUP_STRIKE_VERIFIED");g.getTarget().discard();g.discard();});
        if(t==1030)server(c,p->{if(!p.serverLevel().getEntitiesOfClass(ArmySkeleton.class,p.getBoundingBox().inflate(50)).isEmpty())throw new IllegalStateException("Army survived general removal");System.out.println("ROYALE_ARMY_GENERAL_REMOVAL_VERIFIED");});
        if(t==1040)server(c,p->{
            var nativeSkeleton=new io.redspace.ironsspellbooks.entity.mobs.SummonedSkeleton(p.level(),p,false);nativeSkeleton.moveTo(0,150,0,180,0);nativeSkeleton.setYBodyRot(180);nativeSkeleton.setYHeadRot(180);nativeSkeleton.setNoAi(true);nativeSkeleton.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BOW));nativeSkeleton.setAggressive(true);p.serverLevel().addFreshEntity(nativeSkeleton);
            face(p,new Vec3(0,149.7,-3),new Vec3(0,150.8,0));
        });
        if(t==1090)shot(c,"native-iron-skeleton-bow");
        if(t==1110)c.getSoundManager().addListener((sound,event,range)->{
            String id=sound.getLocation().toString();if(id.equals("royalespells:spell_void_strike")||id.equals("royalespells:spell_fireball_impact")){
                if(id.endsWith("void_strike"))voidAudio++;else farAudio++;audioRange&=Math.abs(range-64)<.01;
                System.out.println("ROYALE_AUDIO_CLIENT_RECEIVED "+id+" range="+range+" gain="+sound.getVolume()+" distance="+Math.sqrt(c.player.distanceToSqr(sound.getX(),sound.getY(),sound.getZ())));
            }
        });
        if(t==1120)server(c,p->{p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);face(p,new Vec3(0,150,-40),new Vec3(0,150,0));});
        if(t==1150)server(c,p->{var at=new Vec3(0,150,0);p.serverLevel().addFreshEntity(SpellEntity.create(p.serverLevel(),Spell.VOID,p.getUUID(),at,at));});
        if(t==1250){if(voidAudio!=3||!audioRange){System.err.println("ROYALE_ARMY_CLIENT_FAILURE distant Void audio="+voidAudio+" range="+audioRange);c.stop();return;}System.out.println("ROYALE_VOID_THREE_DISTANT_IMPACTS_VERIFIED");}
        if(t==1260)server(c,p->SpellSounds.play(p.level(),p.position().add(56,0,0),Spell.FIREBALL,"impact"));
        if(t==1290){if(farAudio!=1||!audioRange){System.err.println("ROYALE_ARMY_CLIENT_FAILURE 56-block audio="+farAudio);c.stop();return;}System.out.println("ROYALE_SPELL_AUDIO_56_BLOCKS_VERIFIED");}
        if(t==1300){System.out.println("ROYALE_ARMY_CLIENT_COMPLETE");c.stop();}
        if(t>1800){System.err.println("ROYALE_ARMY_CLIENT_TIMEOUT");c.stop();}
    }
}
