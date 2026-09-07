package dev.royalespells.client;
import dev.royalespells.*;
import dev.royalespells.elixir.*;
import dev.royalespells.iron.*;
import net.minecraft.client.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.neoforged.neoforge.client.model.data.ModelData;
import java.util.*;
import java.util.function.Consumer;
/** Opt-in screenshots from the packaged client and original Iron's tower generator. */
final class ElixirClientSmoke {
    private static volatile boolean ready;
    private static boolean queued;
    private static int tick;
    static BlockPos surface,dark;
    private static final BlockPos FORGE=new BlockPos(3,150,0),ANVIL=new BlockPos(6,150,0);
    private static void server(Minecraft c,Consumer<ServerPlayer> action){c.getSingleplayerServer().execute(()->action.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst()));}
    private static void shot(Minecraft c,String name){Screenshot.grab(c.gameDirectory,"elixir-"+name+".png",c.getMainRenderTarget(),m->{});System.out.println("ROYALE_ELIXIR_FRAME "+name);}
    private static void face(ServerPlayer p,BlockPos at,double dx,double dy,double dz) {
        p.teleportTo(p.serverLevel(),at.getX()+.5+dx,at.getY()+dy,at.getZ()+.5+dz,0,0);
        var d=net.minecraft.world.phys.Vec3.atCenterOf(at).subtract(p.getEyePosition());
        p.connection.teleport(p.getX(),p.getY(),p.getZ(),(float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));
    }
    static void tower(ServerPlayer p) {
        var w=p.serverLevel();var generator=w.getChunkSource().getGenerator();var structure=w.registryAccess().registryOrThrow(Registries.STRUCTURE).get(TowerPools.TOWER);
        StructureStart chosen=null;
        for(int i=0;i<32 && chosen==null;i++) {
            var chunk=new ChunkPos(12+i*3,12);
            var start=structure.generate(w.registryAccess(),generator,generator.getBiomeSource(),w.getChunkSource().randomState(),w.getStructureManager(),w.getSeed(),chunk,0,w,b->true);
            if(!start.isValid())continue;
            var site=TowerPools.surfaceSite(start,w,generator);if(site==null)continue;
            for(var piece:start.getPieces())if(piece instanceof PoolElementStructurePiece pp && pp.getElement() instanceof SinglePoolElement element
                && ((dev.royalespells.mixin.PoolTemplateAccess)element).royaleTemplate().left().map(Object::toString).orElse("").equals("irons_spellbooks:pyromancer_tower/basement")) {
                chosen=start;surface=site;dark=TowerPools.basementCenter(pp);break;
            }
        }
        if(chosen==null)throw new IllegalStateException("Could not find valid native tower and surface pool terrain");
        double a=ElixirConfig.ELIXIR_CHANCE.get(),b=ElixirConfig.DARK_CHANCE.get();ElixirConfig.ELIXIR_CHANCE.set(1d);ElixirConfig.DARK_CHANCE.set(1d);
        try {
            var box=chosen.getBoundingBox();
            for(int x=(box.minX()>>4)-1;x<=(box.maxX()>>4)+1;x++)for(int z=(box.minZ()>>4)-1;z<=(box.maxZ()>>4)+1;z++)w.getChunk(x,z);
            for(int x=box.minX()>>4;x<=box.maxX()>>4;x++)for(int z=box.minZ()>>4;z<=box.maxZ()>>4;z++)chosen.placeInChunk(w,w.structureManager(),generator,w.random,new BoundingBox(x*16,w.getMinBuildHeight(),z*16,x*16+15,w.getMaxBuildHeight()-1,z*16+15),new ChunkPos(x,z));
        } finally {ElixirConfig.ELIXIR_CHANCE.set(a);ElixirConfig.DARK_CHANCE.set(b);}
        if(!w.getFluidState(surface).is(ElixirContent.ELIXIR.source.get()) || !w.getFluidState(dark).is(ElixirContent.DARK.source.get()))throw new IllegalStateException("Original tower generation did not contain both pools");
        System.out.println("ROYALE_ELIXIR_NATIVE_TOWER surface="+surface+" basement="+dark+" pieces="+chosen.getPieces().size());
        p.getInventory().clearContent();p.getInventory().add(new ItemStack(ElixirContent.ELIXIR.bucket.get()));p.getInventory().add(new ItemStack(ElixirContent.DARK.bucket.get()));
        ElixirContent.ELIXIR_INKS.forEach(i->p.getInventory().add(new ItemStack(i.get())));p.getInventory().add(new ItemStack(ElixirContent.DARK_BOTTLE.get()));p.getInventory().add(new ItemStack(ElixirContent.DARK_CONCENTRATE.get()));
        face(p,surface,-5,5,-7);ready=true;
    }
    static void tick(Minecraft c) {
        c.getToasts().clear();c.gui.getChat().clearMessages(false);
        if(!queued){queued=true;c.options.hideGui=false;c.options.guiScale().set(3);c.resizeDisplay();server(c,ElixirClientSmoke::tower);return;}
        if(!ready)return;int t=++tick;
        if(t==120)shot(c,"tower-and-elixir-pool");
        if(t==130)server(c,p->face(p,surface,0,3,-4));
        if(t==175)shot(c,"elixir-source-closeup");
        if(t==185)server(c,p->face(p,dark,0,1,-2));
        if(t==250)shot(c,"original-basement-dark-pool");
        if(t==260)server(c,p->{p.teleportTo(p.serverLevel(),0,151,-3,0,20);p.serverLevel().setBlockAndUpdate(FORGE,io.redspace.ironsspellbooks.registries.BlockRegistry.SCROLL_FORGE_BLOCK.get().defaultBlockState());});
        if(t==300 && c.level.getBlockEntity(FORGE)==null){tick--;return;}
        if(t==300)server(c,p->{p.openMenu((MenuProvider)p.serverLevel().getBlockEntity(FORGE),FORGE);
            var menu=(io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu)p.containerMenu;var spell=IronIntegration.spell(IronSpellProfile.ZAP_EVOLUTION);
            menu.getInkSlot().set(new ItemStack(ElixirContent.DARK_BOTTLE.get(),4));menu.getBlankScrollSlot().set(new ItemStack(Items.PAPER,4));menu.getFocusSlot().set(BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(s->spell.getSchoolType().isFocus(s)).findFirst().orElseThrow().copyWithCount(4));menu.setRecipeSpell(spell);
        });
        if(t==315 && c.screen instanceof io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeScreen screen) {
            var expected=IronIntegration.spell(IronSpellProfile.ZAP_EVOLUTION).getDisplayName(c.player).getString();
            var button=screen.children().stream().filter(child->child instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals(expected)).map(child->(net.minecraft.client.gui.components.Button)child).findFirst().orElseThrow();
            button.onPress();
        }
        if(t==335){if(!(c.screen instanceof io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeScreen))throw new IllegalStateException("Real forge UI did not open");var menu=((io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeScreen)c.screen).getMenu();
            if(menu.getResultSlot().getItem().isEmpty())throw new IllegalStateException("Client-selected elixir forge recipe has no output");
            System.out.println("ROYALE_ELIXIR_CLIENT_FORGE_RESULT "+io.redspace.ironsspellbooks.api.spells.ISpellContainer.get(menu.getResultSlot().getItem()).getSpellAtIndex(0));shot(c,"royale-only-forge-list");}
        if(t==345)server(c,p->{var menu=(io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu)p.containerMenu;menu.getInkSlot().set(new ItemStack(io.redspace.ironsspellbooks.item.InkItem.getInkForRarity(io.redspace.ironsspellbooks.api.spells.SpellRarity.EPIC)));});
        if(t==380)shot(c,"native-ink-keeps-native-spells");
        if(t==390)server(c,p->{p.closeContainer();p.serverLevel().setBlockAndUpdate(ANVIL,io.redspace.ironsspellbooks.registries.BlockRegistry.ARCANE_ANVIL_BLOCK.get().defaultBlockState());p.openMenu(new SimpleMenuProvider((id,inv,who)->new io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu(id,inv,net.minecraft.world.inventory.ContainerLevelAccess.create(p.serverLevel(),ANVIL)),net.minecraft.network.chat.Component.translatable("block.irons_spellbooks.arcane_anvil")));
            var menu=(io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu)p.containerMenu;menu.getSlot(0).set(IronIntegration.scroll(IronSpellProfile.ZAP,10));menu.getSlot(1).set(new ItemStack(ElixirContent.DARK_CONCENTRATE.get()));menu.createResult();
        });
        if(t==435){
            var output=c.player.containerMenu.getSlot(2).getItem();var data=io.redspace.ironsspellbooks.api.spells.ISpellContainer.get(output).getSpellAtIndex(0);
            if(data.getSpell()!=IronIntegration.spell(IronSpellProfile.ZAP_EVOLUTION) || data.getLevel()!=5)throw new IllegalStateException("Client evolution output failed to sync");
            System.out.println("ROYALE_ELIXIR_CLIENT_EVOLVED "+data);shot(c,"max-zap-evolution-anvil");
        }
        if(t==445)server(c,p->p.closeContainer());
        if(t==485){System.out.println("ROYALE_ELIXIR_CLIENT_COMPLETE");c.stop();}
    }
}
