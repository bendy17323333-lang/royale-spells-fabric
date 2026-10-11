package dev.royalespells;
import java.util.Locale;
public enum TroopCard {
    BARBARIAN_HUT(6,"barbarian_hut"),INFERNO_DRAGON(4,"inferno_dragon"),
    FIRE_SPIRIT(1,"fire_spirit"),ICE_SPIRIT(1,"ice_spirit"),ELECTRO_SPIRIT(1,"electro_spirit"),HEAL_SPIRIT(1,"heal_spirit"),
    SKELETON_ARMY_EVOLUTION(3,"skeleton_army_evolution"),WITCH(5,"witch"),WITCH_EVOLUTION(5,"witch_evolution");
    public final int cost;public final String unit;
    TroopCard(int cost,String unit){this.cost=cost;this.unit=unit;}
    public String id(){return name().toLowerCase(Locale.ROOT);}
    public boolean spirit(){return name().endsWith("_SPIRIT");}
}
