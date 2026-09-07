package dev.royalespells.entity;

import dev.royalespells.*;
import dev.royalespells.army.ArmySounds;
import dev.royalespells.elixir.DarkPoolBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Owns its input stacks for the entire persisted ritual; interruption refunds them exactly once. */
public final class RitualEntity extends Entity {
    public static final int DURATION=140;
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(RitualEntity.class,EntityDataSerializers.INT),MODE=SynchedEntityData.defineId(RitualEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DISPLAY=SynchedEntityData.defineId(RitualEntity.class,EntityDataSerializers.ITEM_STACK);
    private ItemStack first=ItemStack.EMPTY,second=ItemStack.EMPTY;private BlockPos source=BlockPos.ZERO;private boolean settled;
    public RitualEntity(EntityType<? extends RitualEntity> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(AGE,0);b.define(MODE,0);b.define(DISPLAY,ItemStack.EMPTY);}
    public int age(){return entityData.get(AGE);}public int mode(){return entityData.get(MODE);}public ItemStack display(){return entityData.get(DISPLAY);}
    public ItemStack input(){return first.copy();}public ItemStack catalyst(){return second.copy();}
    public static boolean begin(ItemEntity item,ItemEntity catalyst,BlockPos source,int mode){
        if(item.level().isClientSide||item.getItem().isEmpty()||!DarkPoolBlock.natural(item.level().getBlockState(source)))return false;
        if(!IronSpellSystem.loaded||mode<0||mode>1)return false;
        if(mode==0&&!dev.royalespells.iron.ArmyMagic.skeletonScroll(item.getItem()))return false;
        if(mode==1&&(!item.getItem().is(net.minecraft.world.item.Items.GOAT_HORN)||dev.royalespells.iron.ArmyMagic.horn(item.getItem())||catalyst==null||!dev.royalespells.iron.ArmyMagic.armyScroll(catalyst.getItem())))return false;
        if(!item.level().getEntitiesOfClass(RitualEntity.class,new net.minecraft.world.phys.AABB(source).inflate(2.5)).isEmpty())return false;
        var ritual=RoyaleSpells.RITUAL.create(item.level());if(ritual==null)return false;
        ritual.first=item.getItem().copyWithCount(1);ritual.second=catalyst==null?ItemStack.EMPTY:catalyst.getItem().copyWithCount(1);
        ritual.source=source.immutable();ritual.entityData.set(MODE,mode);ritual.entityData.set(DISPLAY,ritual.first.copy());ritual.setPos(Vec3.atBottomCenterOf(source).add(0,.9,0));
        if(!item.level().addFreshEntity(ritual))return false;
        consume(item);if(catalyst!=null)consume(catalyst);
        ritual.playSound(ArmySounds.CHANGE,.65f,.72f);return true;
    }
    private static void consume(ItemEntity item){var remainder=item.getItem().copy();remainder.shrink(1);item.setItem(remainder);if(remainder.isEmpty())item.discard();}
    private void output(ItemStack stack,int index){
        if(stack.isEmpty())return;var item=new ItemEntity(level(),getX()+index*.35,getY()+.6,getZ(),stack.copy());item.setDeltaMovement(index*.025,.22,0);item.setPickUpDelay(10);if(index!=0)item.getPersistentData().putLong("RoyaleRitualGrace",level().getGameTime()+200);level().addFreshEntity(item);
    }
    public void refund(){if(settled||level().isClientSide)return;settled=true;output(first,-1);output(second,1);first=second=ItemStack.EMPTY;discard();}
    @Override public void tick(){
        super.tick();if(level().isClientSide){
            if(age()<130)for(int i=0;i<3;i++){double a=age()*.15+i*Math.PI*2/3,r=1.2*(1-Math.min(1,age()/110.0))+.2;level().addParticle(RoyaleSpells.GRAVE_MOTE,getX()+Math.cos(a)*r,getY()+.1+Math.min(1,age()/100.0)*1.3,getZ()+Math.sin(a)*r,-Math.cos(a)*.025,.04,-Math.sin(a)*.025);}return;
        }
        if(!IronSpellSystem.loaded||!DarkPoolBlock.natural(level().getBlockState(source))){refund();return;}
        entityData.set(AGE,age()+1);
        if(age()==105){playSound(ArmySounds.HORN,.6f,1);entityData.set(DISPLAY,mode()==0?dev.royalespells.iron.ArmyMagic.evolvedScroll():dev.royalespells.iron.ArmyMagic.enchantedHorn(first));}
        if(age()>=DURATION&&!settled){
            var result=mode()==0?dev.royalespells.iron.ArmyMagic.evolvedScroll():dev.royalespells.iron.ArmyMagic.enchantedHorn(first);
            settled=true;output(result,0);first=second=ItemStack.EMPTY;discard();
        }
    }
    @Override protected void addAdditionalSaveData(CompoundTag n){n.putInt("Age",age());n.putInt("Mode",mode());n.putLong("Source",source.asLong());n.putBoolean("Settled",settled);if(!first.isEmpty())n.put("First",first.save(registryAccess()));if(!second.isEmpty())n.put("Second",second.save(registryAccess()));}
    @Override protected void readAdditionalSaveData(CompoundTag n){entityData.set(AGE,n.getInt("Age"));entityData.set(MODE,n.getInt("Mode"));source=BlockPos.of(n.getLong("Source"));settled=n.getBoolean("Settled");first=ItemStack.parseOptional(registryAccess(),n.getCompound("First"));second=ItemStack.parseOptional(registryAccess(),n.getCompound("Second"));entityData.set(DISPLAY,first.copy());}
    @Override public boolean isPickable(){return false;}
    @Override public void remove(RemovalReason reason){if(reason.shouldDestroy()&&!settled&&!level().isClientSide){refund();return;}super.remove(reason);}
}
