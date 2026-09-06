package dev.royalespells;

import java.util.Locale;

public enum Spell {
    ARROWS(3,4.5,24,0xDED3AE), FIREBALL(4,2.8,25,0xFF7628), ZAP(2,2.2,12,0x9FEFFF),
    LIGHTNING(6,3.5,24,0x86BFFF), ROCKET(6,2.5,40,0xFFB44F), POISON(4,3.5,160,0xEDAC32),
    FREEZE(4,3,80,0x9AE8FF), RAGE(2,3.5,120,0xE458FA), THE_LOG(2,2,30,0xAB793F),
    TORNADO(3,5.5,30,0xB9E0DC), EARTHQUAKE(3,3.5,60,0xC9A36C), GIANT_SNOWBALL(2,2.5,24,0xDBF8FF),
    GOBLIN_BARREL(3,2.5,30,0x69C66E), BARBARIAN_BARREL(2,1.5,22,0xE4B94C),
    ROYAL_DELIVERY(3,3,60,0x6FB4F1), GRAVEYARD(5,4,200,0xAA7FE7),
    CLONE(3,3,16,0x83C4FF), MIRROR(1,3,16,0xCEBCFF), GOBLIN_CURSE(2,3,120,0x77DE49),
    VOID(5,3,80,0xF84A16), VINES(3,3,50,0x449345),
    ZAP_EVOLUTION(2,3,28,0xCE9AFF), GIANT_SNOWBALL_EVOLUTION(2,2.5,42,0xB0A5FF),
    GOBLIN_BARREL_EVOLUTION(3,2.5,30,0xB16CF1),
    HEAL(1,3,60,0xFFF183), WARMTH(1,3,100,0xFFBD6A), PARTY_ROCKET(5,3,40,0x82F280),
    BARBARIAN_BARREL_HERO(2,1.5,22,0xF5D075);

    public final int cost, duration, color;
    public final double radius;
    Spell(int cost, double radius, int duration, int color) {
        this.cost=cost; this.radius=radius; this.duration=duration; this.color=color;
    }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public boolean evolved() { return name().endsWith("_EVOLUTION"); }
    public Spell base() {
        return switch(this) {
            case ZAP_EVOLUTION -> ZAP;
            case GIANT_SNOWBALL_EVOLUTION -> GIANT_SNOWBALL;
            case GOBLIN_BARREL_EVOLUTION -> GOBLIN_BARREL;
            default -> this;
        };
    }
    public boolean projectile() {
        return switch(this) {
            case FIREBALL, ROCKET, PARTY_ROCKET, GIANT_SNOWBALL, GIANT_SNOWBALL_EVOLUTION,
                 GOBLIN_BARREL, GOBLIN_BARREL_EVOLUTION, ROYAL_DELIVERY -> true;
            default -> false;
        };
    }
    public boolean rolling() { return this==THE_LOG || this==BARBARIAN_BARREL || this==BARBARIAN_BARREL_HERO; }
    public static Spell byId(int id) { return values()[Math.max(0,Math.min(values().length-1,id))]; }
}

