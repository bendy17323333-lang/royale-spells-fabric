package dev.royalespells.spirit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.Locale;
/** September 2026 level 11 / mirrored level 12 card stats. One CR HP = 1/27 MC HP. */
public enum SpiritElement {
    FIRE(207,227,2.3,0xFF982E), ICE(110,120,1.5,0x78DBFF),
    ELECTRO(99,109,0,0xAC72FF), HEAL(110,120,1.5,0xFFE25D);
    public final int color;public final double radius;public final float damage;
    private final int mirroredDamage;
    SpiritElement(int damage,int mirroredDamage,double radius,int color){this.damage=damage/27f;this.mirroredDamage=mirroredDamage;this.radius=radius;this.color=color;}
    public float damage(int level){return level>=12?mirroredDamage/27f:damage;}
    public float health(int level){return (level>=12?238:217)/27f;}
    public float healing(int level){return (level>=12?584:532)/27f;}
    public String id(){return name().toLowerCase(Locale.ROOT);}
    public Item reagent(){return switch(this){case FIRE->Items.FIRE_CHARGE;case ICE->Items.PACKED_ICE;case ELECTRO->Items.AMETHYST_SHARD;case HEAL->Items.GLISTERING_MELON_SLICE;};}
    public static SpiritElement parse(String value){for(var e:values())if(e.id().equals(value))return e;return FIRE;}
}
