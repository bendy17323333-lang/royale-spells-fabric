package dev.mineclash.zappies;

import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.*;
import net.neoforged.neoforge.common.NeoForge;

@Mod(ZappiesAddon.ID)
public final class ZappiesAddon {
    public static final String ID="zappiesaddon";
    private static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(Registries.MOB_EFFECT,ID);
    public static final DeferredHolder<MobEffect,MobEffect> ELECTRICAL_STUN=EFFECTS.register("electrical_stun",()->new MobEffect(MobEffectCategory.HARMFUL,0x8BE5FF){});
    public ZappiesAddon(IEventBus bus,ModContainer container){
        EFFECTS.register(bus);container.registerConfig(ModConfig.Type.COMMON,ZappiesConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(ZappyCommands::register);
        String report=System.getProperty("zappiesaddon.gametestReport");
        if(report!=null)try{net.minecraft.gametest.framework.GlobalTestReporter.replaceWith(new net.minecraft.gametest.framework.JUnitLikeTestReporter(new java.io.File(report)));}catch(Exception ex){throw new RuntimeException(ex);}
    }
}
