package dev.royalespells.test;
import dev.royalespells.*;
import dev.royalespells.elixir.*;
import dev.royalespells.iron.*;
import io.redspace.ironsspellbooks.api.registry.*;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.*;

@PrefixGameTestTemplate(false)
public class ElixirTests {
    private ServerPlayer player(GameTestHelper c) {
        var p=TestPlayers.create(c);p.setGameMode(GameType.SURVIVAL);p.setPos(Vec3.atBottomCenterOf(c.absolutePos(new BlockPos(2,15,2))));p.getInventory().clearContent();return p;
    }
    private void cleanup(ServerPlayer p){p.server.getPlayerList().remove(p);p.discard();}
    private void lookAt(ServerPlayer p,BlockPos at) {
        var d=Vec3.atCenterOf(at).subtract(p.getEyePosition());p.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));p.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="elixir-harvest")
    public void bottlesConsumeFiniteSourcesAndRespectTheUsedHand(GameTestHelper c) {
        var p=player(c);var at=p.blockPosition().offset(0,-1,3);
        for(var pool:new ElixirContent.Pool[]{ElixirContent.ELIXIR,ElixirContent.DARK}) {
            c.getLevel().setBlockAndUpdate(at,pool.block.get().defaultBlockState());lookAt(p,at);
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.GLASS_BOTTLE,2));
            var event=new PlayerInteractEvent.RightClickItem(p,InteractionHand.OFF_HAND);NeoForge.EVENT_BUS.post(event);
            c.assertTrue(event.isCanceled() && p.getOffhandItem().getCount()==1 && p.getMainHandItem().is(Items.STICK),"Harvest uses one bottle in the actual hand");
            c.assertTrue(p.getInventory().contains(new ItemStack(pool.bottle())) && c.getLevel().getFluidState(at).isEmpty(),"Bottle delivered, source removed");
            var second=new PlayerInteractEvent.RightClickItem(p,InteractionHand.OFF_HAND);NeoForge.EVENT_BUS.post(second);
            c.assertFalse(second.isCanceled(),"An empty basin cannot be harvested twice");
        }
        cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="elixir-bucket")
    public void realBucketsCanPickUpAndReplaceBothFluids(GameTestHelper c) {
        var p=player(c);var at=p.blockPosition().offset(0,-1,3);
        for(var pool:new ElixirContent.Pool[]{ElixirContent.ELIXIR,ElixirContent.DARK}) {
            c.getLevel().setBlockAndUpdate(at,pool.block.get().defaultBlockState());lookAt(p,at);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));
            var result=Items.BUCKET.use(c.getLevel(),p,InteractionHand.MAIN_HAND);
            c.assertTrue(result.getObject().is(pool.bucket.get()) && c.getLevel().getFluidState(at).isEmpty(),"Vanilla bucket pickup yields correct bucket");
            var bucket=pool.bucket.get();p.setItemInHand(InteractionHand.MAIN_HAND,result.getObject());
            c.assertTrue(bucket.emptyContents(p,c.getLevel(),at,null),"Real bucket can place its fluid");
            c.assertTrue(c.getLevel().getFluidState(at).is(pool.source.get()),"Placed source retains fluid type");
            c.getLevel().setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());
        }
        cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="elixir-finite",timeoutTicks=180)
    public void neighboringSourcesNeverRegenerateHarvestedElixir(GameTestHelper c) {
        var at=c.absolutePos(new BlockPos(3,14,3));
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)c.getLevel().setBlockAndUpdate(at.offset(dx,-1,dz),Blocks.STONE.defaultBlockState());
        for(var pool:new ElixirContent.Pool[]{ElixirContent.ELIXIR,ElixirContent.DARK}) {
            var base=pool.dark?at.offset(0,0,8):at;
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)c.getLevel().setBlockAndUpdate(base.offset(dx,-1,dz),Blocks.STONE.defaultBlockState());
            c.getLevel().setBlockAndUpdate(base.east(),pool.block.get().defaultBlockState());c.getLevel().setBlockAndUpdate(base.west(),pool.block.get().defaultBlockState());
            c.getLevel().setBlockAndUpdate(base.north(),pool.block.get().defaultBlockState());
        }
        c.runAtTickTime(150,()->{c.assertFalse(c.getLevel().getFluidState(at).isSource(),"Elixir has no infinite-source recipe");c.assertFalse(c.getLevel().getFluidState(at.offset(0,0,8)).isSource(),"Dark elixir has no infinite-source recipe");c.succeed();});
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="elixir-forge")
    public void allRoyalSpellsForgeWithBothMaterialsAndRejectOtherSchools(GameTestHelper c) {
        var p=player(c);var at=p.blockPosition().east(4);
        c.getLevel().setBlockAndUpdate(at,io.redspace.ironsspellbooks.registries.BlockRegistry.SCROLL_FORGE_BLOCK.get().defaultBlockState());
        var menu=new ScrollForgeMenu(20,p.getInventory(),c.getLevel().getBlockEntity(at));
        var materials=new ArrayList<Item>();ElixirContent.ELIXIR_INKS.forEach(i->materials.add(i.get()));materials.add(ElixirContent.DARK_BOTTLE.get());materials.add(ElixirContent.DARK_CONCENTRATE.get());
        for(var profile:IronSpellProfile.values())for(var material:materials) {
            var spell=IronIntegration.spell(profile);var ink=(ElixirInkItem)material;
            var focus=BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(s->spell.getSchoolType().isFocus(s)).findFirst().orElseThrow();
            menu.getInkSlot().set(new ItemStack(material,2));menu.getBlankScrollSlot().set(new ItemStack(Items.PAPER,2));menu.getFocusSlot().set(focus.copyWithCount(2));menu.setRecipeSpell(spell);
            var output=menu.getResultSlot().getItem().copy();
            if(!spell.allowCrafting() || spell.getMinRarity()>ink.grade)c.assertTrue(output.isEmpty(),"Low-grade material cannot skip rarity: "+profile);
            else {
                c.assertFalse(output.isEmpty(),"Royal elixir crafting: "+profile+" "+ink.grade);
                c.assertTrue(ISpellContainer.get(output).getSpellAtIndex(0).getLevel()==spell.getMinLevelForRarity(ink.getRarity()),"Forge respects ink grade");
                menu.getResultSlot().onTake(p,output);
                c.assertTrue(menu.getInkSlot().getItem().getCount()==1 && menu.getBlankScrollSlot().getItem().getCount()==1 && menu.getFocusSlot().getItem().getCount()==1,"Exactly one set consumed");
            }
        }
        var nativeSpell=SpellRegistry.FIREBOLT_SPELL.get();menu.getInkSlot().set(new ItemStack(ElixirContent.DARK_CONCENTRATE.get()));menu.getBlankScrollSlot().set(new ItemStack(Items.PAPER));
        menu.getFocusSlot().set(BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(s->nativeSpell.getSchoolType().isFocus(s)).findFirst().orElseThrow());menu.setRecipeSpell(nativeSpell);
        c.assertTrue(menu.getResultSlot().getItem().isEmpty(),"Spoofing a native selection cannot spend Royal-only ink");
        menu.getInkSlot().set(new ItemStack(io.redspace.ironsspellbooks.item.InkItem.getInkForRarity(SpellRarity.COMMON)));menu.setRecipeSpell(nativeSpell);
        c.assertFalse(menu.getResultSlot().getItem().isEmpty(),"Native ink still crafts native Iron's spells");cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="elixir-evolution")
    public void arcaneAnvilEvolvesAllThreeCardsAndConsumesOneSet(GameTestHelper c) {
        var p=player(c);var menu=new ArcaneAnvilMenu(21,p.getInventory(),ContainerLevelAccess.NULL);
        for(var profile:List.of(IronSpellProfile.ZAP,IronSpellProfile.GIANT_SNOWBALL,IronSpellProfile.GOBLIN_BARREL)) {
            var spell=IronIntegration.spell(profile);
            for(int level:new int[]{1,spell.getMaxLevel()}) {
                menu.getSlot(0).set(IronIntegration.scroll(profile,level).copyWithCount(2));menu.getSlot(1).set(new ItemStack(ElixirContent.DARK_CONCENTRATE.get(),3));menu.createResult();
                var out=menu.getSlot(2).getItem().copy();c.assertFalse(out.isEmpty(),"Evolution output");var data=ISpellContainer.get(out).getSpellAtIndex(0);
                c.assertTrue(data.getSpell().getSpellId().equals("royalespells:"+profile.id()+"_evolution"),"Correct evolved card");
                c.assertTrue(data.getLevel()==(level==1?1:data.getSpell().getMaxLevel()),"Fresh and fully upgraded progress preserved");
                menu.getSlot(2).onTake(p,out);c.assertTrue(menu.getSlot(0).getItem().getCount()==1 && menu.getSlot(1).getItem().getCount()==2,"One card and one concentrate consumed");
            }
        }
        var nativeScroll=new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());ISpellContainer.createScrollContainer(SpellRegistry.FIREBOLT_SPELL.get(),1,nativeScroll);
        menu.getSlot(0).set(nativeScroll);menu.getSlot(1).set(new ItemStack(ElixirContent.ELIXIR_INKS.getFirst().get()));menu.createResult();
        c.assertTrue(menu.getSlot(2).getItem().isEmpty(),"Elixir cannot upgrade another mod's spell");cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="elixir-recipe")
    public void workbenchConcentrationRecipesConsumeThreeBottles(GameTestHelper c) {
        var inputs=new ArrayList<Item>();for(int i=0;i<4;i++)inputs.add(ElixirContent.ELIXIR_INKS.get(i).get());inputs.add(ElixirContent.DARK_BOTTLE.get());
        for(var item:inputs) {
            var input=CraftingInput.of(3,1,List.of(new ItemStack(item),new ItemStack(item),new ItemStack(item)));
            var recipe=c.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,c.getLevel()).orElseThrow();
            var out=recipe.value().assemble(input,c.getLevel().registryAccess());c.assertTrue(out.getCount()==1 && out.getItem() instanceof ElixirInkItem,"3 bottles produce one stronger bottle");
            var shortInput=CraftingInput.of(2,1,List.of(new ItemStack(item),new ItemStack(item)));
            c.assertFalse(recipe.value().matches(shortInput,c.getLevel()),"Cannot concentrate with only two bottles");
        }
        c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="elixir-structure",timeoutTicks=220)
    public void originalBasementGetsNineSourcesInEveryRotationWithoutLosingLoot(GameTestHelper c) {
        var w=c.getLevel();double old=ElixirConfig.DARK_CHANCE.get();ElixirConfig.DARK_CHANCE.set(1d);
        try {
            int index=0;
            for(var rotation:Rotation.values()) {
                var pos=c.absolutePos(new BlockPos(100+index++*90,10,20));var manager=w.getStructureManager();
                var element=StructurePoolElement.single("irons_spellbooks:pyromancer_tower/basement").apply(StructureTemplatePool.Projection.RIGID);
                var box=element.getBoundingBox(manager,pos,rotation);
                var piece=new PoolElementStructurePiece(manager,element,pos,0,rotation,box,LiquidSettings.IGNORE_WATERLOGGING);
                var start=new StructureStart(w.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TowerPools.TOWER),new ChunkPos(pos),0,new PiecesContainer(List.of(piece)));
                var center=TowerPools.basementCenter(piece);
                for(int cx=box.minX()>>4;cx<=box.maxX()>>4;cx++)for(int cz=box.minZ()>>4;cz<=box.maxZ()>>4;cz++) {
                    var clip=new BoundingBox(cx*16,w.getMinBuildHeight(),cz*16,cx*16+15,w.getMaxBuildHeight()-1,cz*16+15);
                    start.placeInChunk(w,w.structureManager(),w.getChunkSource().getGenerator(),net.minecraft.util.RandomSource.create(3),clip,new ChunkPos(cx,cz));
                }
                int sources=0;for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(w.getFluidState(center.offset(x,0,z)).is(ElixirContent.DARK.source.get()))sources++;
                c.assertTrue(sources==9,"Exactly nine sources across chunk edges after rotation: "+rotation);
                c.assertTrue(DarkPoolBlock.natural(w.getBlockState(center)),"Deep natural pool keeps ritual provenance");
                for(int y=-8;y<=0;y++){
                    var shaft=StructureTemplate.transform(new BlockPos(5,y,14),Mirror.NONE,rotation,BlockPos.ZERO).offset(pos);
                    c.assertTrue(w.getBlockState(shaft).is(Blocks.LADDER),"Continuous ladder connects the deeper crypt: "+rotation);
                }
                for(int z=14;z<=16;z++)for(int y=1;y<=3;y++){
                    var entrance=StructureTemplate.transform(new BlockPos(5,y,z),Mirror.NONE,rotation,BlockPos.ZERO).offset(pos);
                    c.assertTrue(w.getBlockState(entrance).isAir(),"Entrance has player headroom: "+rotation);
                }
                var chest=StructureTemplate.transform(new BlockPos(26,1,18),Mirror.NONE,rotation,BlockPos.ZERO).offset(pos);
                c.assertTrue(w.getBlockEntity(chest)!=null,"Original basement loot container survives: "+rotation);
            }
        } finally {ElixirConfig.DARK_CHANCE.set(old);}
        c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="elixir-seed")
    public void chancesAreStableIndependentAndConfigurable(GameTestHelper c) {
        int elixir=0,dark=0;
        for(int i=0;i<1000;i++) {
            boolean a=TowerPools.roll(9191,i,-i,false,.65),b=TowerPools.roll(9191,i,-i,true,.35);
            if(a)elixir++;if(b)dark++;
            c.assertTrue(a==TowerPools.roll(9191,i,-i,false,.65),"Chunk order does not change chance");
            c.assertFalse(TowerPools.roll(9191,i,-i,true,0),"Zero disables pools");c.assertTrue(TowerPools.roll(9191,i,-i,true,1),"One guarantees the roll");
        }
        c.assertTrue(elixir>580 && elixir<720 && dark>280 && dark<420,"Both configured probabilities have mixed outcomes: "+elixir+", "+dark);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="elixir-basin-seal",timeoutTicks=160)
    public void surfaceBasinContainsEverySourceAfterFluidTicks(GameTestHelper c) {
        var at=c.absolutePos(new BlockPos(5,18,5));
        TowerPools.basin(c.getLevel(),at,new BoundingBox(at.getX()-8,at.getY()-3,at.getZ()-8,at.getX()+8,at.getY()+4,at.getZ()+8),false);
        c.runAtTickTime(120,()->{
            int count=0;
            for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++) {
                var state=c.getLevel().getFluidState(at.offset(x,0,z));if(state.is(ElixirContent.ELIXIR.source.get()))count++;
                if(x*x+z*z>4)c.assertTrue(state.isEmpty(),"No fluid escapes the circular basin: "+x+","+z);
            }
            c.assertTrue(count==13,"Thirteen original sources remain in the sealed basin");c.succeed();
        });
    }
}
