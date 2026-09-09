package dev.mineclash.zappies.pause;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;
/** Shared data-tag contract, with one active driver when both addons are loaded. */
public final class ElectricPause {
    private static final ClassValue<java.util.Optional<java.lang.reflect.Method>> OTHER_DRIVER=new ClassValue<>(){
        protected java.util.Optional<java.lang.reflect.Method> computeValue(Class<?> type){
            try{return java.util.Optional.of(type.getMethod("electricallyPaused"));}
            catch(NoSuchMethodException ignored){return java.util.Optional.empty();}
        }
    };
    public static final TagKey<MobEffect> TAG=TagKey.create(Registries.MOB_EFFECT,ResourceLocation.parse("c:electrical_action_pause"));
    public static boolean active(LivingEntity e){
        if(!e.isAlive())return false;
        if(e.level().isClientSide){
            if(e instanceof PauseState s)return s.electricallyPaused();
            // Only needed when the other standalone JAR owns the shared driver.
            try{var method=OTHER_DRIVER.get(e.getClass());return method.isPresent()&&(boolean)method.get().invoke(e);}
            catch(ReflectiveOperationException ignored){return false;}
        }
        return e.getActiveEffects().stream().anyMatch(i->i.getEffect().is(TAG));
    }
    private ElectricPause(){}
}
