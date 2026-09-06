package dev.royalespells;

import net.minecraft.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Two distinct hotbar controls, active only in the bundled recording world. */
public final class SceneControlItem extends Item {
    private final int direction;
    public SceneControlItem(int direction){super(new Properties().stacksTo(1));this.direction=direction;}
    @Override public InteractionResultHolder<ItemStack> use(Level world,Player user,InteractionHand hand){
        var stack=user.getItemInHand(hand);
        if(!world.isClientSide&&user instanceof ServerPlayer player)ShowcaseMap.switchScene(player,user.isShiftKeyDown()?0:direction);
        return InteractionResultHolder.sidedSuccess(stack,world.isClientSide);
    }
}
