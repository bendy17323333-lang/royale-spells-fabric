// SPDX-License-Identifier: MIT
package dev.royalespells;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import java.util.List;

/** Per-stack independent card levels. No Iron spell container or player-global level. */
public final class CardLevels {
    public static final int MIN=1,BASE=11,MAX=16,MAX_MIRRORED=17;
    public static boolean supported(ItemStack stack){return SpellEngine.isCard(stack.getItem());}
    public static int unitLevel(int value){return value<=0?BASE:Math.clamp(value,MIN,MAX_MIRRORED);}
    public static int get(ItemStack stack){
        int value=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getInt(CardBalance.TAG);
        return value<=0?BASE:Math.clamp(value,MIN,MAX);
    }
    public static ItemStack withLevel(ItemStack source,int level){
        ItemStack result=source.copy();
        if(supported(result))CustomData.update(DataComponents.CUSTOM_DATA,result,n->n.putInt(CardBalance.TAG,Math.clamp(level,MIN,MAX)));
        return result;
    }
    public static double multiplier(int level){return Math.pow(1.1,unitLevel(level)-BASE);}
    public static void tooltip(ItemStack stack,List<Component> lines){
        lines.add(Component.translatable("tooltip.royalespells.card_level",get(stack)).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip.royalespells.card_level_select").withStyle(ChatFormatting.DARK_GRAY));
    }
    public static void register(net.neoforged.neoforge.event.RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("royalespells").then(Commands.literal("level")
            .requires(s->s.hasPermission(2)||s.getEntity() instanceof Player p&&p.isCreative())
            .then(Commands.argument("level",IntegerArgumentType.integer(MIN,MAX)).executes(ctx->{
                var p=ctx.getSource().getPlayerOrException();var stack=p.getMainHandItem();
                if(!supported(stack)){ctx.getSource().sendFailure(Component.translatable("message.royalespells.hold_card"));return 0;}
                int level=IntegerArgumentType.getInteger(ctx,"level");p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,withLevel(stack,level));
                p.inventoryMenu.broadcastChanges();ctx.getSource().sendSuccess(()->Component.translatable("message.royalespells.level_changed",level),false);return 1;
            }))));
    }
    private CardLevels(){}
}
