package dev.mineclash.zappies.mixin;
import dev.mineclash.zappies.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.*;
import org.liziyowo.mineclash.entity.mob.Zappies;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.EnumSet;
/** Mechanics only. Native model, texture, animation clips and rain tick remain untouched. */
@Mixin(value=Zappies.class,remap=false)
public abstract class ZappiesMixin extends PathfinderMob implements ZappyAccess,OwnableEntity {
    @Unique private ZappyBrain zappies$brain;
    @Unique private static final EntityDataAccessor<CompoundTag> ZAPPIES_ARC=SynchedEntityData.defineId(Zappies.class,EntityDataSerializers.COMPOUND_TAG);
    protected ZappiesMixin(EntityType<? extends PathfinderMob> type,Level world){super(type,world);}
    public ZappyBrain zappyBrain(){if(zappies$brain==null)zappies$brain=new ZappyBrain((Zappies)(Object)this);return zappies$brain;}
    public java.util.UUID getOwnerUUID(){return zappyBrain().owner;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ZAPPIES_ARC,new CompoundTag());}
    public CompoundTag zappyArc(){return entityData.get(ZAPPIES_ARC);}
    public void zappyDischarge(net.minecraft.world.phys.Vec3 end){
        var data=new CompoundTag();data.putLong("at",level().getGameTime());data.putInt("seq",zappyArc().getInt("seq")+1);
        data.putDouble("x",end.x);data.putDouble("y",end.y);data.putDouble("z",end.z);entityData.set(ZAPPIES_ARC,data);
    }
    @Inject(method="registerGoals",at=@At("HEAD"),cancellable=true)
    private void addSquadGoals(CallbackInfo ci){
        goalSelector.addGoal(1,new Goal(){
            {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
            public boolean canUse(){return true;}
            public boolean requiresUpdateEveryTick(){return true;}
            public void tick(){zappyBrain().tick();}
            public void stop(){zappyBrain().cancelWindup();}
        });
        targetSelector.addGoal(0,new HurtByTargetGoal(this));ci.cancel();
    }
    @Inject(method="performRangedAttack",at=@At("HEAD"),cancellable=true)
    private void properElectricAttack(LivingEntity target,float velocity,CallbackInfo ci){ZappyCombat.shoot((Zappies)(Object)this,target);ci.cancel();}
    @Inject(method="die",at=@At("HEAD"))
    private void stopAttackBeforeNativeDeath(net.minecraft.world.damagesource.DamageSource source,CallbackInfo ci){zappyBrain().cancelWindup();}
    // The source attack clip's recoil is at 1.25 seconds. Play that at our 0.8s hit
    // time instead of the native controller's 5x speed / damage-before-animation.
    @ModifyConstant(method="attackController",constant=@Constant(doubleValue=5.0))
    private double alignNativeRecoil(double original){return 1.5625;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);zappyBrain().save(tag);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);zappyBrain().load(tag);}
}
