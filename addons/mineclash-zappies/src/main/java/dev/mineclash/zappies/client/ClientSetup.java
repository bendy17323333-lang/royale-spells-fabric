package dev.mineclash.zappies.client;

import dev.mineclash.zappies.ZappiesAddon;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid=ZappiesAddon.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->{
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ZappyArcRenderer::render);
        if(BattleShowcase.ENABLED){net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(BattleRecorder::render);net.minecraft.client.Minecraft.getInstance().getSoundManager().addListener(new BattleAudioRecorder());}
        if(Boolean.getBoolean("zappiesaddon.smoke"))AnimationProbe.install();
    });}
}
