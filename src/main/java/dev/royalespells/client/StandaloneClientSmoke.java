package dev.royalespells.client;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import dev.royalespells.spirit.SpiritElement;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;

/** Opt-in framebuffer checks in a new flat development world, never a user's save. */
public final class StandaloneClientSmoke {
    private static int stage,ticks,ready;
    private static final String WORLD="Standalone-Fabric-QA-"+System.currentTimeMillis();
    public static void install(){ClientTickEvents.END_CLIENT_TICK.register(StandaloneClientSmoke::tick);}
    private static void capture(Minecraft client,String name){Screenshot.grab(client.gameDirectory,name+".png",client.getMainRenderTarget(),m->System.out.println("FABRIC_QA_FRAME "+name+" "+m.getString()));}
    private static void select(Minecraft c,int slot){c.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(slot));c.player.getInventory().selected=slot;}
    private static void tick(Minecraft c){
        ticks++;c.options.pauseOnLostFocus=false;
        if(stage==0&&ticks>60&&c.screen instanceof TitleScreen&&c.getOverlay()==null){
            stage=1;c.options.renderDistance().set(6);c.options.simulationDistance().set(5);c.options.framerateLimit().set(60);
            var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
            c.createWorldOpenFlows().createFreshLevel(WORLD,new LevelSettings(WORLD,GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(642155L,false,false),access->access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        }else if(stage==1&&c.level!=null&&c.player!=null&&c.getSingleplayerServer()!=null){
            stage=2;c.getSingleplayerServer().execute(()->setup(c));
        }else if(stage==2&&c.level!=null&&c.player!=null){
            ready++;
            if(c.screen instanceof net.minecraft.client.gui.screens.PauseScreen)c.setScreen(null);
            if(ready==30)capture(c,"fabric18-vanilla-baseline");
            if(ready==40)c.getSingleplayerServer().execute(()->models(c));
            if(ready==90){System.out.println("QA_LIGHT time="+c.level.getDayTime()+" sky="+c.level.getBrightness(LightLayer.SKY,new BlockPos(0,-60,0))+" block="+c.level.getBrightness(LightLayer.BLOCK,new BlockPos(0,-60,0))+" dark="+c.level.getSkyDarken(c.getFrameTime())+" color="+java.util.Arrays.toString(com.mojang.blaze3d.systems.RenderSystem.getShaderColor()));capture(c,"fabric18-models");}
            if(ready==100)c.getSingleplayerServer().execute(()->c.getSingleplayerServer().getPlayerList().getPlayers().get(0).setGameMode(GameType.SURVIVAL));
            if(ready==108)c.setScreen(new InventoryScreen(c.player));
            if(ready==120)capture(c,"fabric18-cards");
            if(ready==130)c.setScreen(null);
            if(ready==140)select(c,0);
            if(ready==155)capture(c,"fabric18-circle-preview");
            if(ready==160)select(c,1);
            if(ready==175)capture(c,"fabric18-log-preview");
            if(ready==180)select(c,2);
            if(ready==195)capture(c,"fabric18-double-preview");
            if(ready==205)c.getSingleplayerServer().execute(()->effects(c));
            if(ready==215)select(c,8);
            if(ready==250)capture(c,"fabric18-effects");
            if(ready==290){System.out.println("FABRIC18_CLIENT_QA_COMPLETE entities="+c.level.getEntityCount());c.stop();stage=3;}
        }
    }
    private static void setup(Minecraft c){
        var server=c.getSingleplayerServer();var w=server.overworld();ServerPlayer p=server.getPlayerList().getPlayers().get(0);
        for(int x=-18;x<=18;x++)for(int z=-14;z<=20;z++){w.setBlockAndUpdate(new BlockPos(x,-61,z),Blocks.SMOOTH_STONE.defaultBlockState());for(int y=-60;y<=-47;y++)w.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
        w.setDayTime(6000);p.teleportTo(w,0,-56,-10,0,23);p.getAbilities().flying=true;p.onUpdateAbilities();
    }
    private static void models(Minecraft c){
        var w=c.getSingleplayerServer().overworld();var p=c.getSingleplayerServer().getPlayerList().getPlayers().get(0);
        p.getInventory().clearContent();int slot=0;for(var spell:new Spell[]{Spell.VOID,Spell.THE_LOG,Spell.GOBLIN_BARREL_EVOLUTION})p.getInventory().setItem(slot++,new ItemStack(RoyaleSpells.ITEMS.get(spell)));
        for(var card:new TroopCard[]{TroopCard.FIRE_SPIRIT,TroopCard.ICE_SPIRIT,TroopCard.ELECTRO_SPIRIT,TroopCard.HEAL_SPIRIT,TroopCard.SKELETON_ARMY_EVOLUTION})p.getInventory().setItem(slot++,new ItemStack(RoyaleSpells.TROOP_ITEMS.get(card)));
        p.getInventory().setItem(9,new ItemStack(RoyaleSpells.TROOP_ITEMS.get(TroopCard.INFERNO_DRAGON)));p.inventoryMenu.broadcastChanges();
        int i=0;for(var element:SpiritElement.values()){
            var e=RoyaleSpells.ELEMENTAL_SPIRIT.create(w);e.configure(p.getUUID(),element,1);e.setup(p.getUUID(),12000,false);e.setPos(-4.5+i++*3,-60,-1);e.setYRot(180);e.setYBodyRot(180);e.setYHeadRot(180);e.setNoAi(true);w.addFreshEntity(e);
        }
        var army=CardDeployment.deploy(p,TroopCard.SKELETON_ARMY_EVOLUTION,new Vec3(-5,-60,6),11);
        for(var e:w.getAllEntities())if(e instanceof ArmySkeleton unit)unit.setNoAi(true);
        String[] kinds={"barbarian","recruit","skeleton"};for(i=0;i<kinds.length;i++){var e=SpellEngine.summon(w,p.getUUID(),new Vec3(1+i*2.3,-60,5),kinds[i],false);CardBalance.apply(e,kinds[i],false,11);e.setNoAi(true);e.setYRot(180);e.setYBodyRot(180);e.setYHeadRot(180);}
        var dragon=SpellEngine.summon(w,p.getUUID(),new Vec3(1,-60,10),"inferno_dragon",false);CardBalance.apply(dragon,"inferno_dragon",false,11);dragon.setNoAi(true);dragon.setYRot(180);dragon.setYBodyRot(180);dragon.setYHeadRot(180);
        // A stepped surface exposes camera/terrain distortion of placement outlines.
        w.setBlockAndUpdate(new BlockPos(0,-60,0),Blocks.OAK_LEAVES.defaultBlockState());w.setBlockAndUpdate(new BlockPos(0,-59,0),Blocks.OAK_LEAVES.defaultBlockState());
    }
    private static void effects(Minecraft c){
        var w=c.getSingleplayerServer().overworld();var p=c.getSingleplayerServer().getPlayerList().getPlayers().get(0);
        for(var e:w.getAllEntities())if(e instanceof ElementalSpirit spirit){if(spirit.element()==SpiritElement.ICE){SpellEngine.stun(spirit,160);spirit.addEffect(new net.minecraft.world.effect.MobEffectInstance(RoyaleSpells.FROZEN,160));}if(spirit.element()==SpiritElement.HEAL)SnowballChill.apply(spirit,true);}
        Spell[] spells={Spell.VOID,Spell.POISON,Spell.GRAVEYARD,Spell.FIREBALL,Spell.ZAP_EVOLUTION,Spell.GIANT_SNOWBALL_EVOLUTION};
        for(int i=0;i<spells.length;i++){Vec3 at=new Vec3((i%3-1)*5,-60,2+i/3*6);var e=SpellEntity.create(w,spells[i],p.getUUID(),at.add(0,0,-5),at);e.preview=true;e.setPreviewTime(spells[i]==Spell.FIREBALL?16:spells[i]==Spell.ZAP_EVOLUTION?14:spells[i]==Spell.GIANT_SNOWBALL_EVOLUTION?24:32);w.addFreshEntity(e);}
    }
    private StandaloneClientSmoke(){}
}
