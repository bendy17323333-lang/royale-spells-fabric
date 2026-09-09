package dev.mineclash.zappies.mixin;
import java.util.*;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
/** Optional Iron hooks have no effect on the standalone MineClash dependency. */
public final class ZappyMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String p){}
    public String getRefMapperConfig(){return null;}
    public boolean shouldApplyMixin(String target,String mixin){
        if(mixin.contains("PauseIron"))return net.neoforged.fml.loading.LoadingModList.get().getMods().stream().anyMatch(m->m.getModId().equals("irons_spellbooks"));
        return true;
    }
    public void acceptTargets(Set<String>a,Set<String>b){}
    public List<String> getMixins(){return null;}
    public void preApply(String a,ClassNode b,String c,IMixinInfo d){}
    public void postApply(String a,ClassNode b,String c,IMixinInfo d){}
}
