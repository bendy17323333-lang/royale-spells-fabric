package dev.royalespells.iron;

import dev.royalespells.Spell;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.resources.ResourceLocation;
import java.util.Locale;
import static io.redspace.ironsspellbooks.api.spells.SpellRarity.*;

/** Base balance. Iron's normal per-spell JSON config can override its supported parameters. */
public enum IronSpellProfile {
    ARROWS("evocation",COMMON,35,4,10,12),
    FIREBALL("fire",RARE,55,12,18,24),
    ZAP("lightning",COMMON,20,2,4,0),
    LIGHTNING("lightning",EPIC,95,15,28,28),
    ROCKET("fire",RARE,110,18,30,40),
    POISON("nature",EPIC,65,10,20,20),
    FREEZE("ice",EPIC,75,10,15,20),
    RAGE("blood",EPIC,40,6,20,12),
    THE_LOG("evocation",LEGENDARY,30,8,8,8),
    TORNADO("evocation",EPIC,55,8,18,16),
    EARTHQUAKE("nature",RARE,55,8,18,20),
    GIANT_SNOWBALL("ice",COMMON,25,3,7,10),
    GOBLIN_BARREL("blood",EPIC,65,10,30,22),
    BARBARIAN_BARREL("evocation",EPIC,50,8,24,16),
    ROYAL_DELIVERY("evocation",COMMON,65,7,28,22),
    GRAVEYARD("blood",LEGENDARY,110,15,50,35),
    CLONE("ender",EPIC,70,12,35,24),
    MIRROR("ender",LEGENDARY,30,-10,16,0),
    GOBLIN_CURSE("blood",EPIC,60,10,24,20),
    VOID("ender",EPIC,110,15,28,32),
    VINES("nature",EPIC,65,10,15,22),
    ZAP_EVOLUTION("lightning",EPIC,48,8,10,8),
    GIANT_SNOWBALL_EVOLUTION("ice",EPIC,50,8,14,14),
    GOBLIN_BARREL_EVOLUTION("blood",EPIC,95,12,35,28),
    HEAL("holy",RARE,40,10,22,18),
    WARMTH("holy",UNCOMMON,25,4,16,12),
    PARTY_ROCKET("fire",EPIC,125,18,35,40),
    BARBARIAN_BARREL_HERO("evocation",LEGENDARY,90,15,35,24),
    BARBARIAN_HUT("evocation",RARE,120,15,60,35),
    SKELETON_ARMY_EVOLUTION("blood",LEGENDARY,125,0,15,40),
    INFERNO_DRAGON("fire",LEGENDARY,100,15,40,20);

    public final String school;
    public final SpellRarity rarity;
    public final int mana,manaPerLevel,cooldown,castTicks;
    IronSpellProfile(String school,SpellRarity rarity,int mana,int manaPerLevel,int cooldown,int castTicks) {
        this.school=school;this.rarity=rarity;this.mana=mana;this.manaPerLevel=manaPerLevel;this.cooldown=cooldown;this.castTicks=castTicks;
    }
    public String id(){return name().toLowerCase(Locale.ROOT);}
    public ResourceLocation schoolId(){return ResourceLocation.fromNamespaceAndPath("irons_spellbooks",school);}
    public int maxLevel(){return this==SKELETON_ARMY_EVOLUTION?1:switch(rarity){case COMMON->10;case UNCOMMON->8;case RARE->6;case EPIC->5;case LEGENDARY->3;};}
    public Spell card(){return this==BARBARIAN_HUT||this==SKELETON_ARMY_EVOLUTION||this==INFERNO_DRAGON?null:Spell.valueOf(name());}
    /** Growth follows role and available levels, rather than a uniform 12 percent. */
    public int powerPerLevel() { return switch(this) {
        case FIREBALL -> 30;
        case ROCKET, PARTY_ROCKET, LIGHTNING, POISON, ZAP_EVOLUTION, GIANT_SNOWBALL_EVOLUTION -> 25;
        case ARROWS -> 22;
        case ZAP, GIANT_SNOWBALL -> 14;
        case HEAL -> 35;
        case GOBLIN_BARREL, BARBARIAN_BARREL, GRAVEYARD, GOBLIN_BARREL_EVOLUTION, BARBARIAN_BARREL_HERO, BARBARIAN_HUT -> 18;
        case CLONE -> 10;
        case MIRROR, WARMTH -> 0;
        default -> 20;
    }; }
    public float damageMultiplier() { return this==VOID?.6f:1; }
    public boolean durationScales(){return this==FREEZE || this==RAGE || this==WARMTH || this==VINES;}
}
