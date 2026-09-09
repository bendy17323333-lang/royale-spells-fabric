package dev.royalespells.pause;
import java.util.*;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
/** The MineClash addon is the single driver when both JARs are present. */
public final class PauseMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String p){}
    public String getRefMapperConfig(){return null;}
    public boolean shouldApplyMixin(String target,String mixin){
        var mods=net.neoforged.fml.loading.LoadingModList.get().getMods();
        if(mods.stream().anyMatch(m->m.getModId().equals("zappiesaddon")))return false;
        if(mixin.contains("PauseIron"))return mods.stream().anyMatch(m->m.getModId().equals("irons_spellbooks"));
        if(mixin.contains("PauseGeo"))return mods.stream().anyMatch(m->m.getModId().equals("geckolib"));
        return true;
    }
    public void acceptTargets(Set<String>a,Set<String>b){}
    public List<String> getMixins(){return null;}
    public void preApply(String a,ClassNode b,String c,IMixinInfo d){}
    public void postApply(String a,ClassNode b,String c,IMixinInfo d){}
}
