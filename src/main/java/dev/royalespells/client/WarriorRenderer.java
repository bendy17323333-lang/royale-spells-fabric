package dev.royalespells.client;

import net.minecraft.client.model.*;
import dev.royalespells.RoyaleSpells;
import dev.royalespells.entity.AllyZombie;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;

public class WarriorRenderer extends MobRenderer<AllyZombie,HumanoidModel<AllyZombie>> {
    public WarriorRenderer(EntityRendererProvider.Context context) {
        super(context,new HumanoidModel<>(model()),.45f);
        addLayer(new ItemInHandLayer<>(this,context.getItemInHandRenderer()));
    }
    private static ModelPart model() {
        MeshDefinition data=new MeshDefinition();PartDefinition root=data.getRoot();
        var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-8,-4,8,8,8),PartPose.ZERO);
        head.addOrReplaceChild("hair",CubeListBuilder.create().texOffs(0,16).addBox(-4.2f,-8.3f,-4.2f,8.4f,2,8.4f),PartPose.ZERO);
        head.addOrReplaceChild("moustache",CubeListBuilder.create().texOffs(0,16).addBox(-3.5f,-2.5f,-4.8f,7,1.5f,1),PartPose.ZERO);
        head.addOrReplaceChild("eyes",CubeListBuilder.create().texOffs(0,80).addBox(-2.8f,-4.8f,-4.1f,1.2f,1.2f,.2f).texOffs(0,80).addBox(1.6f,-4.8f,-4.1f,1.2f,1.2f,.2f),PartPose.ZERO);
        root.addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO);
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-4,0,-2,8,9,4)
            .texOffs(0,32).addBox(-4,9,-2,8,3,4),PartPose.ZERO);
        root.addOrReplaceChild("right_arm",CubeListBuilder.create().texOffs(0,0).addBox(-3,-2,-2,4,12,4),PartPose.offset(-5,2,0));
        root.addOrReplaceChild("left_arm",CubeListBuilder.create().texOffs(0,0).addBox(-1,-2,-2,4,12,4),PartPose.offset(5,2,0));
        root.addOrReplaceChild("right_leg",CubeListBuilder.create().texOffs(0,32).addBox(-2,0,-2,4,12,4),PartPose.offset(-1.9f,12,0));
        root.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(0,32).addBox(-2,0,-2,4,12,4),PartPose.offset(1.9f,12,0));
        return LayerDefinition.create(data,128,128).bakeRoot();
    }
    @Override public ResourceLocation getTextureLocation(AllyZombie entity) {
        return RoyaleSpells.id(entity.getType()==RoyaleSpells.RECRUIT?"textures/entity/recruit.png":"textures/entity/barbarian.png");
    }
}
