package dev.royalespells.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import java.util.OptionalDouble;

/** Transparent geometry must test depth, but must NEVER write an invisible occluder. */
public abstract class SpellLayers extends RenderType {
    private SpellLayers(){super("unused",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,256,false,true,()->{},()->{});}
    public static final RenderType EFFECT=create("royale_spell_transparent",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,4096,false,true,
        CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    // Self-lit material: do not apply directional entity lighting to a fire core.
    public static final RenderType FIRE_CORE=create("royale_fire_core",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,8192,false,false,
        CompositeState.builder().setShaderState(new ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader))
            .setTextureState(new TextureStateShard(dev.royalespells.RoyaleSpells.id("textures/entity/fireball_core.png"),false,false))
            .setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false));
    public static final RenderType FIRE_TRAIL=create("royale_fire_trail",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,16384,false,true,
        CompositeState.builder().setShaderState(new ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader))
            .setTextureState(new TextureStateShard(dev.royalespells.RoyaleSpells.id("textures/entity/fireball_plume.png"),false,false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    public static final RenderType SPECTRAL=create("royale_spectral_skeleton",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,65536,false,true,
        CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER).setTextureState(new TextureStateShard(RoyaleSkeletonRenderer.TEXTURE,false,false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    public static final RenderType ARMY_GEAR=create("royale_army_gear",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,32768,false,true,
        CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER).setTextureState(new TextureStateShard(dev.royalespells.RoyaleSpells.id("textures/entity/troop_materials.png"),false,false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false));
    public static final RenderType ARMY_GEAR_FADE=create("royale_army_gear_fade",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,32768,false,true,
        CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER).setTextureState(new TextureStateShard(dev.royalespells.RoyaleSpells.id("textures/entity/troop_materials.png"),false,false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    // A faint complete silhouette stays readable behind terrain. Neither pass owns scene depth.
    public static final RenderType TARGET_GHOST=target("royale_target_ghost",NO_DEPTH_TEST);
    public static final RenderType TARGET_VISIBLE=target("royale_target_visible",LEQUAL_DEPTH_TEST);
    private static RenderType target(String name,DepthTestStateShard depth) {
        return create(name,DefaultVertexFormat.POSITION_COLOR_NORMAL,VertexFormat.Mode.LINES,4096,
            CompositeState.builder().setShaderState(RENDERTYPE_LINES_SHADER)
                .setLineState(new LineStateShard(OptionalDouble.of(2.5)))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
                .setDepthTestState(depth).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    }
}
