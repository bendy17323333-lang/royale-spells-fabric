package dev.royalespells.elixir;

import dev.royalespells.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.registries.*;
import java.util.*;
import java.util.function.Supplier;

/** Real finite source/flowing fluids. Buckets move a source; bottles consume one source. */
public final class ElixirContent {
    private static final DeferredRegister<FluidType> TYPES=DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES,RoyaleSpells.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(Registries.FLUID,RoyaleSpells.MOD_ID);
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(Registries.BLOCK,RoyaleSpells.MOD_ID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(Registries.ITEM,RoyaleSpells.MOD_ID);
    public static final Pool ELIXIR=new Pool("elixir",false,0xEEE450EC);
    public static final Pool DARK=new Pool("dark_elixir",true,0xFF524060);
    public static final List<String> GRADES=List.of("common","uncommon","rare","epic","legendary");
    public static final List<Supplier<Item>> ELIXIR_INKS=new ArrayList<>();
    public static final Supplier<Item> DARK_BOTTLE=bottle("dark_elixir_bottle",true,3);
    public static final Supplier<Item> DARK_CONCENTRATE=bottle("dark_elixir_concentrate",true,4);
    static {for(int i=0;i<GRADES.size();i++)ELIXIR_INKS.add(bottle(i==0?"elixir_bottle":"elixir_"+GRADES.get(i),false,i));}
    public static final class Pool {
        public final String id;public final boolean dark;public final int color;
        public final DeferredHolder<FluidType,FluidType> type;
        public final DeferredHolder<Fluid,BaseFlowingFluid.Source> source;
        public final DeferredHolder<Fluid,BaseFlowingFluid.Flowing> flowing;
        public final DeferredHolder<Block,LiquidBlock> block;
        public final DeferredHolder<Item,BucketItem> bucket;
        Pool(String id,boolean dark,int color) {
            this.id=id;this.dark=dark;this.color=color;
            type=TYPES.register(id,()->new FluidType(FluidType.Properties.create().density(dark?1600:1100).viscosity(dark?4000:1800)
                .lightLevel(dark?2:6).canConvertToSource(false).sound(SoundActions.BUCKET_FILL,SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY,SoundEvents.BUCKET_EMPTY)));
            source=FLUIDS.register(id,()->new BaseFlowingFluid.Source(properties()));
            flowing=FLUIDS.register("flowing_"+id,()->new BaseFlowingFluid.Flowing(properties()));
            block=BLOCKS.register(id+"_pool",()->dark?new DarkPoolBlock(source.get(),BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).lightLevel(s->2)):new LiquidBlock(source.get(),BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).lightLevel(s->6)));
            bucket=ITEMS.register(id+"_bucket",()->new BucketItem(source.get(),new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
        }
        BaseFlowingFluid.Properties properties(){return new BaseFlowingFluid.Properties(type,source,flowing).block(block).bucket(bucket).tickRate(dark?35:15).slopeFindDistance(2).levelDecreasePerBlock(2);}
        public Item bottle(){return dark?DARK_BOTTLE.get():ELIXIR_INKS.getFirst().get();}
    }
    private static Supplier<Item> bottle(String id,boolean dark,int grade) {
        return ITEMS.register(id,()->IronSpellSystem.elixirInk(dark,grade));
    }
    public static void install(IEventBus bus) {
        TYPES.register(bus);FLUIDS.register(bus);BLOCKS.register(bus);ITEMS.register(bus);
        ElixirConfig.install();NeoForge.EVENT_BUS.addListener(ElixirContent::harvest);
    }
    private static void harvest(PlayerInteractEvent.RightClickItem event) {
        if(!event.getItemStack().is(Items.GLASS_BOTTLE))return;
        var player=event.getEntity();var level=event.getLevel();var from=player.getEyePosition();
        var hit=level.clip(new ClipContext(from,from.add(player.getViewVector(1).scale(player.blockInteractionRange())),ClipContext.Block.OUTLINE,ClipContext.Fluid.SOURCE_ONLY,player));
        if(hit.getType()!=HitResult.Type.BLOCK)return;
        BlockPos pos=hit.getBlockPos();var fluid=level.getFluidState(pos);
        Pool pool=fluid.is(ELIXIR.source.get())?ELIXIR:fluid.is(DARK.source.get())?DARK:null;
        if(pool==null || !fluid.isSource() || !player.mayInteract(level,pos) || !player.mayUseItemAt(pos,hit.getDirection(),event.getItemStack()))return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if(level.isClientSide)return;
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
        player.setItemInHand(event.getHand(),ItemUtils.createFilledResult(event.getItemStack(),player,new ItemStack(pool.bottle())));
        level.playSound(null,pos,SoundEvents.BOTTLE_FILL,SoundSource.PLAYERS,.8f,pool.dark?.8f:1.1f);
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(Items.GLASS_BOTTLE));
    }
    public static void creative(net.minecraft.world.item.CreativeModeTab.Output entries) {
        entries.accept(ELIXIR.bucket.get());entries.accept(DARK.bucket.get());ELIXIR_INKS.forEach(i->entries.accept(i.get()));
        entries.accept(DARK_BOTTLE.get());entries.accept(DARK_CONCENTRATE.get());
    }
    private ElixirContent(){}
}
