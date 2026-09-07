package dev.royalespells.client;
import dev.royalespells.elixir.ElixirContent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.*;
public final class ElixirClient {
    public static void install(IEventBus bus) {
        bus.addListener((RegisterClientExtensionsEvent e)->{
            for(var pool:new ElixirContent.Pool[]{ElixirContent.ELIXIR,ElixirContent.DARK})e.registerFluidType(new IClientFluidTypeExtensions(){
                @Override public ResourceLocation getStillTexture(){return ResourceLocation.withDefaultNamespace("block/water_still");}
                @Override public ResourceLocation getFlowingTexture(){return ResourceLocation.withDefaultNamespace("block/water_flow");}
                @Override public int getTintColor(){return pool.color;}
            },pool.type.get());
        });
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e)->e.enqueueWork(()->{
            for(var pool:new ElixirContent.Pool[]{ElixirContent.ELIXIR,ElixirContent.DARK}) {
                ItemBlockRenderTypes.setRenderLayer(pool.source.get(),RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(pool.flowing.get(),RenderType.translucent());
            }
        }));
        bus.addListener((net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item e)->{
            for(var pool:new ElixirContent.Pool[]{ElixirContent.ELIXIR,ElixirContent.DARK})e.register((stack,tint)->tint==1?(pool.color|0xFF000000):-1,pool.bucket.get());
            for(int i=0;i<ElixirContent.ELIXIR_INKS.size();i++) {
                final int grade=i; e.register((stack,tint)->tint==0?(new int[]{0xFFEA69EF,0xFFD653E8,0xFFC735DF,0xFFAE29D4,0xFF9613CC})[grade]:-1,ElixirContent.ELIXIR_INKS.get(i).get());
            }
            e.register((stack,tint)->tint==0?0xFF332047:-1,ElixirContent.DARK_BOTTLE.get(),ElixirContent.DARK_CONCENTRATE.get());
        });
    }
    private ElixirClient(){}
}
