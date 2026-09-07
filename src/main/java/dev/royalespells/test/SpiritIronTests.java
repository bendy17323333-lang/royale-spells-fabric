package dev.royalespells.test;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import dev.royalespells.iron.*;
import dev.royalespells.spirit.SpiritElement;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Optional tests use the upstream menu and casting manager, not recipe/cast substitutes. */
@PrefixGameTestTemplate(false)
public class SpiritIronTests {
    private ServerPlayer player(GameTestHelper c){var p=TestPlayers.create(c);var at=c.absolutePos(new BlockPos(7,15,7));for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){c.getLevel().setBlockAndUpdate(at.offset(x,-1,z),Blocks.STONE.defaultBlockState());for(int y=0;y<5;y++)c.getLevel().setBlockAndUpdate(at.offset(x,y,z),Blocks.AIR.defaultBlockState());}p.setPos(Vec3.atBottomCenterOf(at));p.setYRot(0);p.setXRot(25);p.setGameMode(GameType.SURVIVAL);p.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA).setBaseValue(300);MagicData.getPlayerMagicData(p).setMana(300);return p;}
    private void cleanup(ServerPlayer p){for(var e:com.google.common.collect.ImmutableList.copyOf(p.serverLevel().getAllEntities()))if(e instanceof Summoned s&&p.getUUID().equals(s.ownerId()))e.discard();p.server.getPlayerList().remove(p);p.discard();}
    @GameTest(template="empty",templateNamespace="royalespells",batch="160-anvil")
    public void everyElementCanBeRetunedAtRealAnvilWithoutLosingItemData(GameTestHelper c){
        var p=player(c);var menu=new ArcaneAnvilMenu(31,p.getInventory(),ContainerLevelAccess.NULL);
        for(var from:SpiritElement.values())for(var to:SpiritElement.values()){
            var input=SpiritSpells.staff(from).getDefaultInstance();input.set(DataComponents.CUSTOM_NAME,Component.literal("Furnace QA"));input.setDamageValue(77);
            var enchant=c.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING);input.enchant(enchant,2);
            menu.getSlot(0).set(input);menu.getSlot(1).set(new ItemStack(to.reagent(),3));menu.createResult();var result=menu.getSlot(2).getItem().copy();
            if(from==to){c.assertTrue(result.isEmpty(),"No wasted same-element transformation");continue;}
            c.assertTrue(result.is(SpiritSpells.staff(to))&&result.getCount()==1,"All twelve directional attunements use the correct item");
            c.assertTrue(result.getHoverName().getString().equals("Furnace QA")&&result.getDamageValue()==77&&result.getEnchantments().getLevel(enchant)==2,"Name, wear and enchantment survive");
            c.assertTrue(ISpellContainer.get(result).getSpellAtIndex(0).getSpell()==SpiritSpells.spell(to),"Bound native spell actually changes");
            menu.getSlot(2).onTake(p,result);c.assertTrue(menu.getSlot(0).getItem().isEmpty()&&menu.getSlot(1).getItem().getCount()==2,"Taking consumes one staff and one reagent");
        }cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="160-staff-native")
    public void rightClickRunsNativeManaWindupAndSharedCooldown(GameTestHelper c){
        var p=player(c);var data=MagicData.getPlayerMagicData(p);p.setItemInHand(InteractionHand.MAIN_HAND,SpiritSpells.staff(SpiritElement.FIRE).getDefaultInstance());
        var use=new PlayerInteractEvent.RightClickItem(p,InteractionHand.MAIN_HAND);NeoForge.EVENT_BUS.post(use);
        c.assertTrue(use.isCanceled()&&use.getCancellationResult()==InteractionResult.CONSUME&&data.isCasting(),"Actual right click enters native windup");
        c.assertTrue(c.getLevel().getEntitiesOfClass(ElementalSpirit.class,p.getBoundingBox().inflate(10),e->p.getUUID().equals(e.ownerId())).isEmpty(),"No summon before windup completes");
        var manager=new MagicManager();for(int i=0;i<30&&data.isCasting();i++)manager.tick(p.serverLevel());
        var spirits=c.getLevel().getEntitiesOfClass(ElementalSpirit.class,p.getBoundingBox().inflate(10),e->p.getUUID().equals(e.ownerId()));c.assertTrue(spirits.size()==1&&spirits.getFirst().element()==SpiritElement.FIRE,"One bound fire spirit, not selected book spell");
        c.assertTrue(data.getMana()<300&&data.getMana()>250,"Native SWORD mana cost charged once");
        for(var kind:SpiritElement.values()){c.assertTrue(data.getPlayerCooldowns().isOnCooldown(SpiritSpells.spell(kind)),"Every element shares native cooldown");p.setItemInHand(InteractionHand.MAIN_HAND,SpiritSpells.staff(kind).getDefaultInstance());var retry=new PlayerInteractEvent.RightClickItem(p,InteractionHand.MAIN_HAND);NeoForge.EVENT_BUS.post(retry);c.assertTrue(retry.getCancellationResult()==InteractionResult.FAIL&&!data.isCasting(),"Switching staffs cannot bypass cooldown");}
        c.assertTrue(SpiritSpells.remaining(p)>0&&SpiritSpells.remaining(p)<=120,"Persisted cooldown agrees with six-second default");cleanup(p);c.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="160-staff-cancel")
    public void cancelWindupAndEmptyManaDoNotSpawnOrStartSharedCooldown(GameTestHelper c){
        var p=player(c);var d=MagicData.getPlayerMagicData(p);p.setItemInHand(InteractionHand.OFF_HAND,SpiritSpells.staff(SpiritElement.ICE).getDefaultInstance());
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(p,InteractionHand.OFF_HAND));c.assertTrue(d.isCasting(),"Off-hand staff casts");
        NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(p,InteractionHand.OFF_HAND));c.assertFalse(d.isCasting(),"Second use cancels native windup");c.assertTrue(d.getMana()==300&&SpiritSpells.remaining(p)==0,"Cancel does not consume mana or shared cooldown");
        d.setMana(0);var event=new PlayerInteractEvent.RightClickItem(p,InteractionHand.OFF_HAND);NeoForge.EVENT_BUS.post(event);c.assertTrue(event.getCancellationResult()==InteractionResult.FAIL&&!d.isCasting(),"No free casts at zero mana");
        c.assertTrue(c.getLevel().getEntitiesOfClass(ElementalSpirit.class,p.getBoundingBox().inflate(10),e->p.getUUID().equals(e.ownerId())).isEmpty(),"No phantom summons");cleanup(p);c.succeed();
    }
}
