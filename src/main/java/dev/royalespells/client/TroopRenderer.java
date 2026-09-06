package dev.royalespells.client;
import dev.royalespells.RoyaleSpells;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
public final class TroopRenderer<T extends MobEntity> extends MobEntityRenderer<T,TroopModel<T>> {
    public TroopRenderer(EntityRendererFactory.Context context,String model,float shadow){
        super(context,new TroopModel<>(model),shadow);
        if(model.equals("barbarian"))addFeature(new HeldItemFeatureRenderer<>(this,context.getHeldItemRenderer()));
    }
    public Identifier getTexture(T entity){return RoyaleSpells.id("textures/entity/troop_materials.png");}
}
