package dev.royalespells.client;

import dev.royalespells.*;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.render.*;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;

/** Draws the untouched original card PNG directly; it never enters the terrain atlas. */
public final class CardRenderer {
    public static void register() {
        RoyaleSpells.ITEMS.forEach((spell,item)->register(item,spell.id()));
        RoyaleSpells.TROOP_ITEMS.forEach((card,item)->register(item,card.id()));
    }
    private static void register(net.minecraft.item.Item item,String id) {
        BuiltinItemRendererRegistry.INSTANCE.register(item,(stack,mode,matrices,consumers,light,overlay)->{
            var layer=RenderLayer.getEntityCutoutNoCull(RoyaleSpells.id("textures/card/"+id+".png"));
            var v=ItemRenderer.getDirectItemGlintConsumer(consumers,layer,true,stack.hasGlint());
            vertex(matrices,v,.08f,0,.5f,0,1,light,overlay);
            vertex(matrices,v,.92f,0,.5f,1,1,light,overlay);
            vertex(matrices,v,.92f,1,.5f,1,0,light,overlay);
            vertex(matrices,v,.08f,1,.5f,0,0,light,overlay);
        });
    }
    private static void vertex(MatrixStack m,VertexConsumer v,float x,float y,float z,float u,float w,int light,int overlay) {
        var entry=m.peek();
        v.vertex(entry.getPositionMatrix(),x,y,z).color(255,255,255,255).texture(u,w).overlay(overlay).light(light).normal(entry,0,0,1);
    }
}
