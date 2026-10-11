package dev.royalespells;
import dev.royalespells.entity.*;
import dev.royalespells.army.*;
import dev.royalespells.spirit.SpiritElement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;
import java.util.*;
/** Validated card-only deployment. Costs and Mirror history commit only on success. */
public final class CardDeployment {
    public static Mob deploy(ServerPlayer player,TroopCard card,Vec3 at,int level){
        var world=player.serverLevel();
        if(card==TroopCard.SKELETON_ARMY_EVOLUTION)return army(player,at);
        if(card==TroopCard.WITCH||card==TroopCard.WITCH_EVOLUTION){
            if(!capacity(world,player.getUUID(),1))return null;
            var witch=(card==TroopCard.WITCH_EVOLUTION?RoyaleSpells.EVOLVED_WITCH:RoyaleSpells.WITCH).create(world);if(witch==null)return null;
            witch.configure(player.getUUID(),card==TroopCard.WITCH_EVOLUTION,level);witch.moveTo(at.x,at.y,at.z,player.getYRot(),0);
            return world.noCollision(witch)&&world.addFreshEntity(witch)?witch:null;
        }
        if(!card.spirit())return SpellEngine.summon(world,player.getUUID(),at,card.unit,false);
        if(!capacity(world,player.getUUID(),1))return null;
        var spirit=RoyaleSpells.ELEMENTAL_SPIRIT.create(world);if(spirit==null)return null;
        var element=SpiritElement.parse(card.id().replace("_spirit",""));
        spirit.configure(player.getUUID(),element,1);spirit.setCardLevel(level);spirit.moveTo(at.x,at.y,at.z,player.getYRot(),0);
        if(!world.noCollision(spirit)||!world.addFreshEntity(spirit))return null;
        PersistentState.of(spirit).putInt(CardBalance.TAG,level);return spirit;
    }
    public static boolean capacity(ServerLevel world,UUID owner,int count){int owned=0,total=0;for(var e:world.getAllEntities())if(e instanceof Summoned s){total++;if(Objects.equals(owner,s.ownerId()))owned++;}return owned+count<=SpellEngine.MAX_UNITS_PER_OWNER&&total+count<=256;}
    public static boolean armyAvailable(ServerPlayer player){
        var ledger=ArmyLedger.get(player.server);long now=ArmyLedger.now(player.server);
        String reason=ledger.active(player.getUUID(),now)?"army_alive":ledger.remaining(player.getUUID(),now)>0?"army_cooldown":null;
        if(reason==null)return true;
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.royalespells."+reason),true);return false;
    }
    public static void syncArmyCooldown(ServerPlayer player){
        int ticks=(int)Math.min(Integer.MAX_VALUE,ArmyLedger.get(player.server).remaining(player.getUUID(),ArmyLedger.now(player.server)));
        var item=RoyaleSpells.TROOP_ITEMS.get(TroopCard.SKELETON_ARMY_EVOLUTION);
        if(ticks>0)player.getCooldowns().addCooldown(item,ticks);else player.getCooldowns().removeCooldown(item);
    }
    private static ArmySkeleton army(ServerPlayer player,Vec3 center){
        var world=player.serverLevel();var ledger=ArmyLedger.get(world.getServer());long now=ArmyLedger.now(world.getServer());
        if(!armyAvailable(player))return null;
        if(!capacity(world,player.getUUID(),16))return null;
        var units=new ArrayList<ArmySkeleton>();var forward=SpellEngine.horizontal(player.getViewVector(1));UUID army=UUID.randomUUID();
        for(int i=0;i<16;i++){
            var at=SpellEngine.ground(world,center.add(ArmyFormation.offset(i,forward)));var pos=BlockPos.containing(at);
            if(!world.hasChunkAt(pos)||!world.getWorldBorder().isWithinBounds(pos)||!player.mayInteract(world,pos)||!world.noCollision(AABB.ofSize(at.add(0,.7,0),.48,1.4,.48)))return null;
            var unit=RoyaleSpells.ARMY_SKELETON.create(world);if(unit==null)return null;
            unit.enlist(player.getUUID(),army,i==0,3,3,12000);unit.formationSlot(i);unit.moveTo(at.x,at.y,at.z,player.getYRot(),0);unit.setYBodyRot(player.getYRot());units.add(unit);
        }
        // Commit the lease only after every member is in the world. A rejected
        // spawn must neither spend elixir nor consume the shared army cooldown.
        for(var unit:units)if(!world.addFreshEntity(unit)){units.forEach(Entity::discard);return null;}
        ledger.start(player.getUUID(),army,units.get(0).getUUID(),now+12000,now+300);
        EvolutionBurst.deploy(world,center,2.1f);world.playSound(null,units.get(0).blockPosition(),ArmySounds.DEPLOY,net.minecraft.sounds.SoundSource.PLAYERS,.9f,1);
        player.getCooldowns().addCooldown(RoyaleSpells.TROOP_ITEMS.get(TroopCard.SKELETON_ARMY_EVOLUTION),300);
        return units.get(0);
    }
    private CardDeployment(){}
}
