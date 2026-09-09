// SPDX-License-Identifier: MIT
package dev.royalespells;

import dev.royalespells.entity.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Card-only reference profile, checked against the live September 8, 2026 patch.
 * CR level 11 Skeleton = 81 HP; our baseline = 3 HP. Keep fractions internally.
 * Spell's original fields remain the Iron's Spells profile. See CARD-BALANCE.md
 * for sources, per-hit versus total damage, legacy-event exceptions and units. */
public final class CardBalance {
    public static final String REVISION="2026-09-08", TAG="RoyaleCardLevel";
    public static final float SKELETON_HEALTH=3;
    public static float convert(double original){return (float)(original/27.0);}
    public static double value(int level,double normal,double mirrored){return level>=12?mirrored:normal;}
    public record Stats(double radius,int duration,double hit11,double hit12) {
        public float hit(int level){return convert(value(level,hit11,hit12));}
    }
    /** Damage here is ONE impact/wave/tick, never a whole multi-hit cast. */
    public static Stats stats(Spell spell){return switch(spell){
        case ARROWS -> new Stats(3.5,24,122,134);
        case FIREBALL -> new Stats(2.5,25,688,755);
        case ZAP -> new Stats(2.5,12,192,210);
        case ZAP_EVOLUTION -> new Stats(3,28,192,210);
        case LIGHTNING -> new Stats(3.5,24,1057,1160);
        case ROCKET -> new Stats(2,40,1484,1629);
        case POISON -> new Stats(3.5,160,92,101);
        case FREEZE -> new Stats(3,70,148,162);
        case RAGE -> new Stats(3,90,179,196);
        case THE_LOG -> new Stats(1.95,30,268,295);
        case TORNADO -> new Stats(5.5,22,84,92);
        case EARTHQUAKE -> new Stats(3.5,60,81,89);
        case GIANT_SNOWBALL -> new Stats(2.5,24,179,196);
        case GIANT_SNOWBALL_EVOLUTION -> new Stats(2.5,42,179,196);
        case GOBLIN_BARREL, GOBLIN_BARREL_EVOLUTION -> new Stats(1.5,30,0,0);
        case BARBARIAN_BARREL, BARBARIAN_BARREL_HERO -> new Stats(1.3,22,232,255);
        case ROYAL_DELIVERY -> new Stats(3,60,384,421);
        case GRAVEYARD -> new Stats(4,180,0,0);
        case CLONE, MIRROR -> new Stats(3,16,0,0);
        case GOBLIN_CURSE -> new Stats(3,120,35,39);
        case VOID -> new Stats(2.5,80,696,764);
        case VINES -> new Stats(2.5,40,153,168);
        // Removed/event-only cards use their last published event rules, not
        // fictional "current ladder" values. Healing is applied once per second.
        case HEAL -> new Stats(4,40,78,86);
        case WARMTH -> new Stats(4,100,25,28);
        case PARTY_ROCKET -> new Stats(4,40,0,0);
    };}
    public static double range(Spell spell){return spell==Spell.THE_LOG?10.1:4.5;}
    public static float voidHit(int count,int level){return convert(count==1?value(level,696,764):count<=4?value(level,294,323):value(level,153,168));}
    public static float earthquakeBuildingHit(int level){return convert(value(level,283,311));}
    public static boolean isCard(Entity entity){return entity.getPersistentData().contains(TAG);}
    public static int level(Entity entity){return Math.max(11,entity.getPersistentData().getInt(TAG));}
    public static float dragonHit(int heat,int level){return convert(switch(InfernoDragon.tier(heat)){
        case 0 -> value(level,35,39);case 1 -> value(level,120,132);default -> value(level,422,463);
    });}
    /** Apply only at an explicitly card-originated summon, before mirror or
     * equipment modifiers. Visual swords/helmets must add no hidden vanilla stats. */
    public static void apply(Mob mob,String kind,boolean decoy,int level){
        if(mob==null)return;
        double health,attack;int interval;
        switch(kind){
            case "skeleton" -> {health=value(level,81,89);attack=health;interval=22;}
            case "zombie" -> {health=decoy?value(level,81,89):value(level,202,221);attack=decoy?value(level,66,72):value(level,125,137);interval=22;}
            case "recruit" -> {health=value(level,547,601);attack=value(level,133,146);interval=26;}
            case "barbarian", "hero" -> {health=value(level,716,786);attack=value(level,192,210);interval=28;}
            case "barbarian_hut" -> {health=value(level,1164,1278);attack=0;interval=0;}
            case "inferno_dragon" -> {health=value(level,1295,1421);attack=27;interval=8;}
            default -> throw new IllegalArgumentException("No card troop profile: "+kind);
        }
        mob.getPersistentData().putInt(TAG,level);mob.getPersistentData().putInt("RoyaleCardAttackTicks",interval);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(convert(health));mob.setHealth(mob.getMaxHealth());
        mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(convert(attack));
        if(mob.getAttribute(Attributes.ARMOR)!=null)mob.getAttribute(Attributes.ARMOR).setBaseValue(0);
        for(var slot:EquipmentSlot.values())if(!mob.getItemBySlot(slot).isEmpty())mob.getItemBySlot(slot).set(DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);
        if(mob instanceof AllyZombie z)z.cardShield(0);
        if(mob instanceof AllyZombie recruit&&kind.equals("recruit")){
            mob.removeEffect(net.minecraft.world.effect.MobEffects.ABSORPTION);mob.setAbsorptionAmount(0);
            recruit.cardShield(convert(value(level,240,264)));
        }
    }
    private CardBalance(){}
}
