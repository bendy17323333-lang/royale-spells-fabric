// SPDX-License-Identifier: MIT
package dev.royalespells.entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** The shared visual/combat contract; implementors retain their own entity superclass. */
public interface WitchActor extends WitchAppearance {
    WitchBehavior behavior();
    default Mob mob(){return (Mob)this;}
    default UUID ownerId(){return behavior().ownerId();}
    default boolean isClone(){return behavior().isClone();}
    default boolean evolved(){return behavior().evolved();}
    default void setEvolved(boolean value){behavior().setEvolved(value);}
    default int cardLevel(){return behavior().cardLevel();}
    default int attackAge(){return behavior().attackAge();}
    default int summonAge(){return behavior().summonAge();}
    default int healFlash(){return behavior().healFlash();}
    default int waveCount(){return behavior().waveCount();}
    default int healedSouls(){return behavior().healedSouls();}
    default int pendingSoulCount(){return behavior().pendingSoulCount();}
    default int summonClock(){return behavior().summonClock();}
    default Set<UUID> eligibleSkeletons(){return behavior().eligibleSkeletons();}
    default float initialHealth(){return behavior().initialHealth();}
    default float soulHealing(){return behavior().soulHealing();}
    default float evolutionHealthCap(){return behavior().evolutionHealthCap();}
    default float projectileDamage(){return behavior().projectileDamage();}
    default Vec3 muzzle(){return behavior().muzzle();}
    default boolean inAttackRange(LivingEntity victim){return behavior().inAttackRange(victim);}
    default boolean validEnemy(LivingEntity victim){return behavior().validEnemy(victim);}
    default void configure(UUID owner,boolean evolved,int level){behavior().configure(owner,evolved,level);}
    default void setup(UUID owner,int life,boolean clone){behavior().setup(owner,life,clone);}
    default void summonWave(){behavior().summonWave();}
    default boolean acceptSoul(UUID skeleton,UUID soul){return behavior().acceptSoul(skeleton,soul);}
    default void abandonSoul(UUID skeleton,UUID soul){behavior().abandonSoul(skeleton,soul);}
    default boolean residentEnemy(LivingEntity victim){return false;}
    default AllySkeleton createSkeleton(ServerLevel world){return dev.royalespells.RoyaleSpells.SKELETON.create(world);}
}
