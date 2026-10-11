package dev.royalespells;
import java.util.Locale;
public enum TroopCard {
    BARBARIAN_HUT(6,"barbarian_hut"),
    INFERNO_DRAGON(4,"inferno_dragon"),WITCH(5,"witch"),WITCH_EVOLUTION(5,"witch_evolution");
    public final int cost;public final String unit;
    TroopCard(int cost,String unit){this.cost=cost;this.unit=unit;}
    public boolean witch(){return this==WITCH||this==WITCH_EVOLUTION;}
    public String id(){return name().toLowerCase(Locale.ROOT);}
}
