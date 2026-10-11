package dev.royalespells.army;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/** One test egg deploys the complete independent faction, never a lone unsupported member. */
public final class NeutralArmyEgg extends Item {
    public NeutralArmyEgg(){super(new Item.Properties());}
    @Override public InteractionResult useOn(UseOnContext context){
        if(!(context.getLevel() instanceof ServerLevel world))return InteractionResult.SUCCESS;
        var player=context.getPlayer();if(player!=null&&!player.mayInteract(world,context.getClickedPos()))return InteractionResult.FAIL;
        var center=Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
        if(ArmyFormation.neutral(world,center,context.getRotation()).isEmpty())return InteractionResult.FAIL;
        if(player==null||!player.isCreative())context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
