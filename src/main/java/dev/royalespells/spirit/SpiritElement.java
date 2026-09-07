package dev.royalespells.spirit;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.Locale;

/** Stable names are used in saves; enum order is not a persistence contract. */
public enum SpiritElement {
    // Every base spirit clears a base six-health graveyard skeleton. Fire keeps
    // the damage advantage; utility spirits still scale through native spell power.
    FIRE("fire",32,10,0xFF982E), ICE("ice",28,7,0x78DBFF),
    ELECTRO("lightning",36,7,0xAC72FF), HEAL("holy",32,7,0xFFE25D);
    public final String school;public final int mana,color;public final float damage;
    SpiritElement(String school,int mana,float damage,int color){this.school=school;this.mana=mana;this.damage=damage;this.color=color;}
    public String id(){return name().toLowerCase(Locale.ROOT);}
    public String spellId(){return "summon_"+id()+"_spirit";}
    public String staffId(){return "furnace_staff_"+id();}
    public Item reagent(){return switch(this){case FIRE->Items.FIRE_CHARGE;case ICE->Items.PACKED_ICE;case ELECTRO->Items.AMETHYST_SHARD;case HEAL->Items.GLISTERING_MELON_SLICE;};}
    public static SpiritElement parse(String value){for(var e:values())if(e.id().equals(value))return e;return FIRE;}
}
