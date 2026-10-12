// SPDX-License-Identifier: MIT
package dev.royalespells;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import java.util.*;

/** Cosmetic creative wrappers. The actual MineClash SpawnEggItem remains the
 * authority for spawning, NBT, troop counts and behavior; no reimplemented AI. */
public final class MineClashCards {
    public static final List<Card> CARDS=new ArrayList<>();
    private static final String[] IDS={"knight_zombie","skeleton","royal_recruits","ice_spirit","elite_musketeer","zappies",
        "pekka","mini_pekka","super_mini_pekka","golem","valkyrie","monk","mega_knight","guard","general_gerry"};
    public static void register(){
        for(String id:IDS)CARDS.add(Registry.register(BuiltInRegistries.ITEM,RoyaleSpells.id("mineclash_"+id),new Card(id)));
    }
    public static void creative(CreativeModeTab.Output output){CARDS.stream().filter(Card::available).forEach(output::accept);}
    public static final class Card extends Item {
        public final String entityId;
        private Card(String id){super(new Properties().stacksTo(1));entityId=id;}
        private SpawnEggItem egg(){
            Item item=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mineclash",entityId+"_spawn_egg"));
            return item instanceof SpawnEggItem spawn?spawn:null;
        }
        public boolean available(){return ModList.get().isLoaded("mineclash")&&egg()!=null;}
        private boolean usable(Player player){
            if(player==null)return false;
            if(!available()){player.displayClientMessage(Component.translatable("message.royalespells.mineclash_missing"),true);return false;}
            return true;
        }
        @Override public InteractionResult useOn(UseOnContext context){
            return usable(context.getPlayer())?egg().useOn(context):InteractionResult.FAIL;
        }
        @Override public InteractionResultHolder<ItemStack> use(Level world,Player player,InteractionHand hand){
            return usable(player)?egg().use(world,player,hand):InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flags){
            lines.add(Component.translatable("tooltip.royalespells.mineclash_card").withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.royalespells.mineclash_creative").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
    private MineClashCards(){}
}
