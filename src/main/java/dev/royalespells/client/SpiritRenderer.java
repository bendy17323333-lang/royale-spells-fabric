package dev.royalespells.client;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.royalespells.RoyaleSpells;
import dev.royalespells.entity.ElementalSpirit;
import dev.royalespells.spirit.SpiritElement;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Four articulated spirits; Ice retains the user-supplied MineClash skin and glow mask. */
public final class SpiritRenderer extends MobRenderer<ElementalSpirit,TroopModel<ElementalSpirit>> {
    private final Map<SpiritElement,TroopModel<ElementalSpirit>> models=new EnumMap<>(SpiritElement.class);
    public SpiritRenderer(EntityRendererProvider.Context context){
        super(context,new TroopModel<>("spirit_fire"),.22f);for(var e:SpiritElement.values())models.put(e,new TroopModel<>("spirit_"+e.id()));
        addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<ElementalSpirit,TroopModel<ElementalSpirit>>(this){
            @Override public void render(PoseStack poses,MultiBufferSource buffers,int light,ElementalSpirit e,float limb,float amount,float partial,float age,float yaw,float pitch){
                if(!e.isInvisible()){
                    if(e.element()==SpiritElement.ICE){
                        // The source mask is authored in the original 128px UV layout.
                        // Keep diffuse lighting on the body and illuminate only mask pixels.
                        getParentModel().renderToBuffer(poses,buffers.getBuffer(net.minecraft.client.renderer.RenderType.eyes(RoyaleSpells.id("textures/entity/spirit_ice_mineclash_glowmask.png"))),15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,0xFFFFFFFF);
                    }else getParentModel().renderEmissive(poses,buffers.getBuffer(net.minecraft.client.renderer.RenderType.eyes(getTextureLocation(e))));
                    // An RGBA flame needs alpha blending. The additive eyes shader
                    // can add the RGB of fully transparent pixels and show a bright rectangle.
                    if(e.element()==SpiritElement.FIRE){
                        var flameType=net.minecraft.client.renderer.RenderType.entityTranslucentEmissive(RoyaleSpells.id("textures/entity/spirit_flame.png"));
                        getParentModel().renderFlames(poses,buffers.getBuffer(flameType));
                    }
                }
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(ElementalSpirit entity){return RoyaleSpells.id(entity.element()==SpiritElement.ICE?"textures/entity/spirit_ice_mineclash.png":"textures/entity/spirit_materials.png");}
    @Override public void render(ElementalSpirit entity,float yaw,float partial,PoseStack matrices,MultiBufferSource buffers,int light){
        if(entity.chaining())return;
        model=models.get(entity.element());super.render(entity,yaw,partial,matrices,buffers,light);
    }
}
