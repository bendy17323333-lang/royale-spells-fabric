package dev.royalespells;
import net.minecraft.world.entity.LivingEntity;
/** Vanilla clients do not keep the full active-effect map for every remote mob. */
public interface VisualState {
    byte royaleVisualFlags();
    static boolean raged(LivingEntity e){return (((VisualState)e).royaleVisualFlags()&1)!=0;}
    static boolean frozen(LivingEntity e){return (((VisualState)e).royaleVisualFlags()&2)!=0;}
    static boolean rooted(LivingEntity e){return (((VisualState)e).royaleVisualFlags()&4)!=0;}
    static boolean snowbound(LivingEntity e){return (((VisualState)e).royaleVisualFlags()&16)!=0;}
    static boolean cloned(LivingEntity e){return (((VisualState)e).royaleVisualFlags()&8)!=0 || e instanceof dev.royalespells.entity.Summoned s && s.isClone();}
}
