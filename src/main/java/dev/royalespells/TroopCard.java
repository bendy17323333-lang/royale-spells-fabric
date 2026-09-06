package dev.royalespells;
import java.util.Locale;
public enum TroopCard {
    BARBARIAN_HUT(6,"barbarian_hut");
    public final int cost;public final String unit;
    TroopCard(int cost,String unit){this.cost=cost;this.unit=unit;}
    public String id(){return name().toLowerCase(Locale.ROOT);}
}
