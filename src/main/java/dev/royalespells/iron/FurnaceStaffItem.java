package dev.royalespells.iron;

import dev.royalespells.RoyaleSpells;
import dev.royalespells.spirit.SpiritElement;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.UniqueItem;
import io.redspace.ironsspellbooks.item.weapons.StaffItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import java.util.List;

/** Native casting implement with a locked elemental summon, retained through attunement. */
public final class FurnaceStaffItem extends StaffItem implements UniqueItem {
    public final SpiritElement element;
    public FurnaceStaffItem(SpiritElement element){
        super(new Item.Properties().stacksTo(1).durability(512).rarity(Rarity.RARE).fireResistant().attributes(ItemAttributeModifiers.builder()
            .add(AttributeRegistry.SPELL_POWER,new AttributeModifier(RoyaleSpells.id("furnace_spell_power"),.1,AttributeModifier.Operation.ADD_MULTIPLIED_BASE),EquipmentSlotGroup.MAINHAND)
            .add(AttributeRegistry.MAX_MANA,new AttributeModifier(RoyaleSpells.id("furnace_mana"),25,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND).build()));
        this.element=element;
    }
    public void prepare(ItemStack stack){
        var spell=SpiritSpells.spell(element);
        if(!ISpellContainer.isSpellContainer(stack)||ISpellContainer.get(stack).getSpellAtIndex(0).getSpell()!=spell)
            ISpellContainer.createImbuedContainer(spell,1,stack);
    }
    @Override public ItemStack getDefaultInstance(){var stack=super.getDefaultInstance();prepare(stack);return stack;}
    @Override public void inventoryTick(ItemStack stack,Level world,Entity entity,int slot,boolean selected){if(!world.isClientSide)prepare(stack);super.inventoryTick(stack,world,entity,slot,selected);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        super.appendHoverText(stack,context,lines,flag);
        lines.add(Component.translatable("tooltip.royalespells.furnace_staff"));
        lines.add(Component.translatable("tooltip.royalespells.furnace_attune"));
        lines.add(Component.translatable("tooltip.royalespells.furnace_reagent",element.reagent().getDescription()));
    }
}
