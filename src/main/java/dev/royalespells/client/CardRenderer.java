package dev.royalespells.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.royalespells.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;

/** Draws the untouched original card PNG directly; it never enters the terrain atlas. */
public final class CardRenderer extends BlockEntityWithoutLevelRenderer {
    private static final java.util.Map<net.minecraft.resources.ResourceLocation,Float> ASPECT=new java.util.HashMap<>();
    public static void reload(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event){
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->ASPECT.clear());
    }
    private CardRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static void register(RegisterClientExtensionsEvent event) {
        var extension=new IClientItemExtensions(){
            private CardRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new CardRenderer();return renderer;}
        };
        RoyaleSpells.ITEMS.values().forEach(item->event.registerItem(extension,item));
        RoyaleSpells.TROOP_ITEMS.values().forEach(item->event.registerItem(extension,item));
        MineClashCards.CARDS.forEach(item->event.registerItem(extension,item));
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext mode,PoseStack matrices,MultiBufferSource consumers,int light,int overlay) {
            String id=stack.getItem() instanceof SpellItem card?card.spell.id():stack.getItem() instanceof TroopItem card?card.card.id():stack.getItem() instanceof MineClashCards.Card card?"mineclash_"+card.entityId:null;
            if(id==null)return;
            var texture=RoyaleSpells.id("textures/card/"+id+".png");
            float ratio=ASPECT.computeIfAbsent(texture,key->{
                try(var in=Minecraft.getInstance().getResourceManager().open(key);var image=com.mojang.blaze3d.platform.NativeImage.read(in)){
                    return image.getWidth()/(float)image.getHeight();
                }catch(java.io.IOException ex){return 302f/363f;}
            });
            float height=Math.min(1,1/ratio),width=height*ratio,left=(1-width)/2,bottom=(1-height)/2;
            var layer=RenderType.entityCutoutNoCull(texture);
            var v=ItemRenderer.getFoilBufferDirect(consumers,layer,true,stack.hasFoil());
            vertex(matrices,v,left,bottom,.5f,0,1,light,overlay);
            vertex(matrices,v,left+width,bottom,.5f,1,1,light,overlay);
            vertex(matrices,v,left+width,bottom+height,.5f,1,0,light,overlay);
            vertex(matrices,v,left,bottom+height,.5f,0,0,light,overlay);
    }
    private static void vertex(PoseStack m,VertexConsumer v,float x,float y,float z,float u,float w,int light,int overlay) {
        var entry=m.last();
        v.addVertex(entry.pose(),x,y,z).setColor(255,255,255,255).setUv(u,w).setOverlay(overlay).setLight(light).setNormal(entry,0,0,1);
    }
}
