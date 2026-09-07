package dev.royalespells.elixir;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FlowingFluid;

/** Provenance belongs to the source block. A bucket always places the default, non-natural state. */
public final class DarkPoolBlock extends LiquidBlock {
    public static final BooleanProperty NATURAL=BooleanProperty.create("natural");
    public DarkPoolBlock(FlowingFluid fluid,Properties properties){super(fluid,properties);registerDefaultState(defaultBlockState().setValue(NATURAL,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(NATURAL);}
    public static boolean natural(BlockState state){return state.getBlock() instanceof DarkPoolBlock && state.getValue(NATURAL) && state.getFluidState().isSource();}
}
