package dev.royalespells.client;

import net.minecraft.client.render.*;

/** Transparent geometry must test depth, but must NEVER write an invisible occluder. */
public abstract class SpellLayers extends RenderLayer {
    private SpellLayers(){super("unused",VertexFormats.POSITION_COLOR,VertexFormat.DrawMode.QUADS,256,false,true,()->{},()->{});}
    public static final RenderLayer EFFECT=of("royale_spell_transparent",VertexFormats.POSITION_COLOR,VertexFormat.DrawMode.QUADS,4096,false,true,
        MultiPhaseParameters.builder().program(COLOR_PROGRAM).transparency(TRANSLUCENT_TRANSPARENCY)
            .cull(DISABLE_CULLING).depthTest(LEQUAL_DEPTH_TEST).writeMaskState(COLOR_MASK).build(false));
}
