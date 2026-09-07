package dev.royalespells.army;

import dev.royalespells.*;
import dev.royalespells.entity.ArmySkeleton;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.*;
import java.util.*;

public final class ArmyFormation {
    public static Vec3 offset(int i,Vec3 forward){var side=forward.cross(new Vec3(0,1,0));return i==0?forward.scale(-1.25):side.scale(((i-1)%5-2)*.85).add(forward.scale(((i-1)/5)*.85-.4));}
    public static List<ArmySkeleton> neutral(ServerLevel world,Vec3 center,float yaw){
        UUID faction=UUID.randomUUID(),army=UUID.randomUUID();var units=new ArrayList<ArmySkeleton>();var forward=Vec3.directionFromRotation(0,yaw);
        for(int i=0;i<16;i++){
            var at=SpellEngine.ground(world,center.add(offset(i,forward)));var pos=net.minecraft.core.BlockPos.containing(at);
            if(!world.hasChunkAt(pos)||!world.getWorldBorder().isWithinBounds(pos)||!world.noCollision(AABB.ofSize(at.add(0,.7,0),.48,1.4,.48)))return List.of();
            var unit=RoyaleSpells.ARMY_SKELETON.create(world);if(unit==null)return List.of();
            unit.enlist(faction,army,i==0,4,1.8f,12000);unit.formationSlot(i);unit.setNeutralArmy();unit.moveTo(at,yaw,0);unit.setYBodyRot(yaw);units.add(unit);
        }
        var ledger=ArmyLedger.get(world.getServer());ledger.start(faction,army,units.getFirst().getUUID(),ArmyLedger.now(world.getServer())+12000,0);
        for(var unit:units)if(!world.addFreshEntity(unit)){ledger.finish(faction,army);units.forEach(Entity::discard);return List.of();}
        dev.royalespells.entity.EvolutionBurst.deploy(world,SpellEngine.ground(world,center),2.1f);
        world.playSound(null,units.getFirst().blockPosition(),ArmySounds.DEPLOY,net.minecraft.sounds.SoundSource.NEUTRAL,.9f,1);
        return units;
    }
    private ArmyFormation(){}
}
