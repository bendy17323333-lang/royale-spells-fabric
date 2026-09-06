package dev.royalespells;

import net.minecraft.item.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraft.util.*;

/** Two distinct hotbar controls, active only in the bundled recording world. */
public final class SceneControlItem extends Item {
    private final int direction;
    public SceneControlItem(int direction){super(new Settings().maxCount(1));this.direction=direction;}
    @Override public TypedActionResult<ItemStack> use(World world,PlayerEntity user,Hand hand){
        var stack=user.getStackInHand(hand);
        if(!world.isClient&&user instanceof ServerPlayerEntity player)ShowcaseMap.switchScene(player,user.isSneaking()?0:direction);
        return TypedActionResult.success(stack,world.isClient);
    }
}
