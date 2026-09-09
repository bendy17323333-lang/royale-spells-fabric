package dev.mineclash.zappies;
import net.minecraft.world.entity.*;
import net.neoforged.fml.ModList;
import java.util.*;
/** Use the vanilla ownership contract first; optional mods keep their own AI. */
public final class ZappyOwnership {
    private static final ClassValue<Optional<java.lang.reflect.Method>> ROYALE_OWNER=new ClassValue<>(){
        protected Optional<java.lang.reflect.Method> computeValue(Class<?> type){
            try{return Optional.of(type.getMethod("ownerId"));}catch(NoSuchMethodException ignored){return Optional.empty();}
        }
    };
    public static UUID of(Entity e){
        if(e instanceof OwnableEntity own)return own.getOwnerUUID();
        if(ModList.get().isLoaded("irons_spellbooks")){UUID owner=Iron.owner(e);if(owner!=null)return owner;}
        if(e.getType().builtInRegistryHolder().key().location().getNamespace().equals("royalespells")){
            try{var method=ROYALE_OWNER.get(e.getClass());return method.isPresent()?(UUID)method.get().invoke(e):null;}
            catch(ReflectiveOperationException ignored){return null;}
        }
        return null;
    }
    private static final class Iron {
        static UUID owner(Entity e){if(e instanceof io.redspace.ironsspellbooks.entity.mobs.IMagicSummon s&&s.getSummoner()!=null)return s.getSummoner().getUUID();return null;}
    }
    private ZappyOwnership(){}
}
