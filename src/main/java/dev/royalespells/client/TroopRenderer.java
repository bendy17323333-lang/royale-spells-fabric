package dev.royalespells.client;
import dev.royalespells.RoyaleSpells;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
public final class TroopRenderer<T extends Mob> extends MobRenderer<T,TroopModel<T>> {
    public TroopRenderer(EntityRendererProvider.Context context,String model,float shadow){
        super(context,new TroopModel<>(model),shadow);
        if(model.equals("barbarian"))addLayer(new ItemInHandLayer<>(this,context.getItemInHandRenderer()));
    }
    public ResourceLocation getTextureLocation(T entity){return RoyaleSpells.id("textures/entity/troop_materials.png");}
}
