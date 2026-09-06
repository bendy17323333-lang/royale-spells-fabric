package dev.royalespells.client;

import dev.royalespells.RoyaleSpells;
import dev.royalespells.entity.AllyZombie;
import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.util.Identifier;

public class WarriorRenderer extends MobEntityRenderer<AllyZombie,BipedEntityModel<AllyZombie>> {
    public WarriorRenderer(EntityRendererFactory.Context context) {
        super(context,new BipedEntityModel<>(model()),.45f);
        addFeature(new HeldItemFeatureRenderer<>(this,context.getHeldItemRenderer()));
    }
    private static ModelPart model() {
        ModelData data=new ModelData();ModelPartData root=data.getRoot();
        var head=root.addChild("head",ModelPartBuilder.create().uv(0,0).cuboid(-4,-8,-4,8,8,8),ModelTransform.NONE);
        head.addChild("hair",ModelPartBuilder.create().uv(0,16).cuboid(-4.2f,-8.3f,-4.2f,8.4f,2,8.4f),ModelTransform.NONE);
        head.addChild("moustache",ModelPartBuilder.create().uv(0,16).cuboid(-3.5f,-2.5f,-4.8f,7,1.5f,1),ModelTransform.NONE);
        head.addChild("eyes",ModelPartBuilder.create().uv(0,80).cuboid(-2.8f,-4.8f,-4.1f,1.2f,1.2f,.2f).uv(0,80).cuboid(1.6f,-4.8f,-4.1f,1.2f,1.2f,.2f),ModelTransform.NONE);
        root.addChild("hat",ModelPartBuilder.create(),ModelTransform.NONE);
        root.addChild("body",ModelPartBuilder.create().uv(0,0).cuboid(-4,0,-2,8,9,4)
            .uv(0,32).cuboid(-4,9,-2,8,3,4),ModelTransform.NONE);
        root.addChild("right_arm",ModelPartBuilder.create().uv(0,0).cuboid(-3,-2,-2,4,12,4),ModelTransform.pivot(-5,2,0));
        root.addChild("left_arm",ModelPartBuilder.create().uv(0,0).cuboid(-1,-2,-2,4,12,4),ModelTransform.pivot(5,2,0));
        root.addChild("right_leg",ModelPartBuilder.create().uv(0,32).cuboid(-2,0,-2,4,12,4),ModelTransform.pivot(-1.9f,12,0));
        root.addChild("left_leg",ModelPartBuilder.create().uv(0,32).cuboid(-2,0,-2,4,12,4),ModelTransform.pivot(1.9f,12,0));
        return TexturedModelData.of(data,128,128).createModel();
    }
    @Override public Identifier getTexture(AllyZombie entity) {
        return RoyaleSpells.id(entity.getType()==RoyaleSpells.RECRUIT?"textures/entity/recruit.png":"textures/entity/barbarian.png");
    }
}
