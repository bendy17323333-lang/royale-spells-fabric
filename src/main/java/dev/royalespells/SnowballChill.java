package dev.royalespells;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Snowball's visual slow is distinct from poison/earthquake and ordinary
 * Slowness. Keep the established damage, displacement and 60-tick slow intact. */
public final class SnowballChill {
    public static final float ANIMATION_RATE=.65f;
    public static final int DURATION=60;
    public static void apply(LivingEntity entity) {apply(entity,false);}
    public static void apply(LivingEntity entity,boolean card) {
        entity.addEffect(new MobEffectInstance(card?RoyaleSpells.CARD_SLOW:MobEffects.MOVEMENT_SLOWDOWN,DURATION,card?6:0,false,false));
        entity.addEffect(new MobEffectInstance(RoyaleSpells.SNOWBOUND,DURATION,0,false,false,true));
    }
    private SnowballChill(){}
}
