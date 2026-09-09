package dev.royalespells.entity;

import dev.royalespells.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import java.util.UUID;

/** Base Inferno Dragon: one uninterrupted lock, three heat tiers, no splash damage.
 * Flight uses collision-aware vanilla aerial pathfinding. Losing a lock never
 * carries heat into another target (that is the original card's evolution).
 */
public final class InfernoDragon extends PathfinderMob implements Summoned {
    public static final int DEPLOY_TICKS=20, HIT_INTERVAL=8, LIFE_TICKS=900;
    public static final double RANGE=4.0, VERTICAL_REACH=6.0, MIN_HOVER_HEIGHT=3.5;
    // Rank three has 140% base strength. Dividing the requested max-rank
    // bonuses here preserves the existing proportional scaling at every rank.
    private static final float MID_DAMAGE=3.4f+2f/1.4f,HIGH_DAMAGE=12f+1f/1.4f;
    public static final float BASE_HEALTH=48;
    private static final EntityDataAccessor<Integer> LOCK=SynchedEntityData.defineId(InfernoDragon.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HEAT=SynchedEntityData.defineId(InfernoDragon.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DEPLOY=SynchedEntityData.defineId(InfernoDragon.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CLONE=SynchedEntityData.defineId(InfernoDragon.class,EntityDataSerializers.BOOLEAN);
    private UUID owner,lockedUuid;
    private int life=LIFE_TICKS,hitClock;
    private boolean deployed;
    private double minimumHoverY=Double.NEGATIVE_INFINITY;
    private BlockPos lastFloorProbe;
    public InfernoDragon(EntityType<? extends PathfinderMob> type,Level world){
        super(type,world);xpReward=0;setNoGravity(true);moveControl=new FlyingMoveControl(this,20,true);
    }
    public static AttributeSupplier.Builder attributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,BASE_HEALTH)
        .add(Attributes.MOVEMENT_SPEED,.22).add(Attributes.FLYING_SPEED,.22).add(Attributes.ATTACK_DAMAGE,1).add(Attributes.FOLLOW_RANGE,24);}
    @Override protected PathNavigation createNavigation(Level world){
        var nav=new FlyingPathNavigation(this,world);nav.setCanOpenDoors(false);nav.setCanPassDoors(true);nav.setCanFloat(true);return nav;
    }
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(LOCK,0);b.define(HEAT,0);b.define(DEPLOY,DEPLOY_TICKS);b.define(CLONE,false);}
    @Override public UUID ownerId(){return owner;}
    @Override public boolean isClone(){return entityData.get(CLONE);}
    @Override public void setup(UUID owner,int lifetime,boolean clone){this.owner=owner;life=lifetime;entityData.set(CLONE,clone);setPersistenceRequired();setCanPickUpLoot(false);}
    public int heatTicks(){return entityData.get(HEAT);}
    public int deployTicks(){return entityData.get(DEPLOY);}
    public int beamTargetId(){return entityData.get(LOCK);}
    public LivingEntity beamTarget(){var e=level().getEntity(beamTargetId());return e instanceof LivingEntity l&&l.isAlive()?l:null;}
    public static int tier(int heat){return heat>80?2:heat>40?1:0;}
    public static float tierDamage(int heat){return switch(tier(heat)){case 0->1f;case 1->MID_DAMAGE;default->HIGH_DAMAGE;};}
    public float hitDamage(){return (CardBalance.isCard(this)?CardBalance.dragonHit(heatTicks(),CardBalance.level(this)):tierDamage(heatTicks()))*(float)getAttributeValue(Attributes.ATTACK_DAMAGE);}
    public void resetBeam(){entityData.set(LOCK,0);entityData.set(HEAT,0);lockedUuid=null;hitClock=0;}
    private boolean disabled(){return hasEffect(RoyaleSpells.STUN)||hasEffect(RoyaleSpells.FROZEN)||dev.royalespells.pause.ElectricPause.active(this);}
    // Inferno's ramping beam is the intentional exception to ordinary electrical
    // windup suspension. Any provider of the shared electrical tag clears heat
    // immediately, while retaining the AI target for a new tier-one lock later.
    @Override protected void onEffectAdded(net.minecraft.world.effect.MobEffectInstance effect,Entity source){
        super.onEffectAdded(effect,source);resetElectricalBeam();
    }
    @Override protected void onEffectUpdated(net.minecraft.world.effect.MobEffectInstance effect,boolean reapply,Entity source){
        super.onEffectUpdated(effect,reapply,source);resetElectricalBeam();
    }
    private void resetElectricalBeam(){
        if(!level().isClientSide&&dev.royalespells.pause.ElectricPause.active(this)){resetBeam();setDeltaMovement(Vec3.ZERO);}
    }
    @Override public void travel(Vec3 input){
        if(disabled()){setDeltaMovement(Vec3.ZERO);return;}
        if(isEffectiveAi()){
            moveRelative(getSpeed(),input);
            if(!isNoAi()&&!hasEffect(RoyaleSpells.ROOTED)){
                Vec3 velocity=getDeltaMovement();double error=minimumHoverY-getY();
                if(Double.isFinite(minimumHoverY)&&error>.025){
                    // Rise before advancing onto a ledge. Movement still uses
                    // voxel collision: this cannot teleport through a ceiling.
                    double horizontal=error>.35?.18:1;
                    velocity=new Vec3(velocity.x*horizontal,Math.max(velocity.y,Mth.clamp(error*.13,.025,.22)),velocity.z*horizontal);
                }else if(navigation.isDone()){
                    double hovering=minimumHoverY;var locked=beamTarget();
                    if(locked!=null)hovering=Math.max(hovering,locked.getBoundingBox().getCenter().y-1);
                    double correction=Double.isFinite(hovering)?Mth.clamp((hovering-getY())*.10,-.10,.10):0;
                    velocity=new Vec3(velocity.x*.65,correction,velocity.z*.65);
                }else if(Double.isFinite(minimumHoverY)&&getY()+velocity.y<minimumHoverY){
                    velocity=new Vec3(velocity.x,minimumHoverY-getY(),velocity.z);
                }
                setDeltaMovement(velocity);
            }
            move(MoverType.SELF,getDeltaMovement());setDeltaMovement(getDeltaMovement().scale(.88));
        }
        calculateEntityAnimation(false);
    }
    @Override public void tick(){
        if(!level().isClientSide&&!isNoAi()&&!disabled())refreshHoverFloor();
        super.tick();setNoGravity(true);
        if(level().isClientSide){
            // Vanilla client body rotation follows travel/head-idle direction.
            // A firing dragon instead faces its synced victim on both sides.
            var victim=beamTarget();if(victim!=null&&heatTicks()>0&&!disabled())faceVictim(victim);
            return;
        }
        var world=(ServerLevel)level();
        if(dev.royalespells.pause.ElectricPause.active(this)){resetBeam();setDeltaMovement(Vec3.ZERO);return;}
        if(--life<=0){resetBeam();world.sendParticles(ParticleTypes.SMOKE,getX(),getY()+.8,getZ(),9,.35,.4,.35,.025);discard();return;}
        if(!deployed){deployed=true;SpellSounds.play(world,position(),"inferno_dragon","deploy");}
        if(deployTicks()>0){entityData.set(DEPLOY,deployTicks()-1);navigation.stop();setDeltaMovement(Vec3.ZERO);return;}
        if(disabled()){resetBeam();navigation.stop();setDeltaMovement(Vec3.ZERO);return;}
        if(!isNoAi())SummonOrders.tick(this,owner);
        LivingEntity target=getTarget();
        if(!validEnemy(target)){
            resetBeam();
            if(!isNoAi()&&tickCount%5==0){
                var master=CombatCompatibility.resolve(world,owner);
                if(master instanceof LivingEntity living&&distanceToSqr(master)>36)navigateAbove(living);
                else navigation.stop();
            }
            return;
        }
        faceVictim(target);
        if(inBeamRange(target)&&beamVisible(target)){
            navigation.stop();setSpeed(0);setXxa(0);setYya(0);setZza(0);
            // Do not drift across/over the victim after locking and then spin
            // back to compensate. The altitude controller remains independent.
            setDeltaMovement(0,getDeltaMovement().y,0);
            if(!target.getUUID().equals(lockedUuid)){resetBeam();lockedUuid=target.getUUID();entityData.set(LOCK,target.getId());}
            entityData.set(HEAT,Math.min(120,heatTicks()+1));
            if(++hitClock>=(hasEffect(RoyaleSpells.RAGED)?6:HIT_INTERVAL)){hitClock=0;damage(target);}
        }else{
            resetBeam();
            if(!isNoAi()&&tickCount%5==0&&!hasEffect(RoyaleSpells.ROOTED)){
                navigateAbove(target);
            }
        }
    }
    private void faceVictim(LivingEntity victim){
        double dx=victim.getX()-getX(),dz=victim.getZ()-getZ();
        if(dx*dx+dz*dz<.0004)return; // Directly underneath has no useful yaw.
        float wanted=(float)Math.toDegrees(Math.atan2(-dx,dz));
        float facing=getYRot()+Mth.wrapDegrees(wanted-getYRot());
        setYRot(facing);setYBodyRot(facing);setYHeadRot(facing);
    }
    private double supportBelow(Vec3 probe){
        if(!level().hasChunkAt(BlockPos.containing(probe)))return Double.NEGATIVE_INFINITY;
        Vec3 from=probe.add(0,.20,0),to=probe.add(0,-16,0);
        var hit=level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,this));
        return hit.getType()==HitResult.Type.BLOCK?hit.getLocation().y:Double.NEGATIVE_INFINITY;
    }
    private void refreshHoverFloor(){
        BlockPos cell=blockPosition();
        if(cell.equals(lastFloorProbe)&&tickCount%4!=0)return;
        lastFloorProbe=cell;
        double floor=supportBelow(position());
        for(double x:new double[]{-.56,.56})for(double z:new double[]{-.56,.56})floor=Math.max(floor,supportBelow(position().add(x,0,z)));
        Vec3 velocity=getDeltaMovement();double speed=velocity.horizontalDistance();
        if(speed>.015)floor=Math.max(floor,supportBelow(position().add(velocity.x/speed*1.35,1.4,velocity.z/speed*1.35)));
        minimumHoverY=Double.isFinite(floor)?floor+MIN_HOVER_HEIGHT:Double.NEGATIVE_INFINITY;
    }
    private void navigateAbove(LivingEntity victim){
        if(hasEffect(RoyaleSpells.ROOTED))return;
        Vec3 away=position().subtract(victim.position()).multiply(1,0,1);
        if(away.lengthSqr()<.01){double yaw=Math.toRadians(getYRot());away=new Vec3(Math.sin(yaw),0,-Math.cos(yaw));}
        Vec3 slot=victim.position().add(away.normalize().scale(2.4));
        double ground=supportBelow(victim.position());
        double y=Math.max(victim.getBoundingBox().getCenter().y-1,ground+MIN_HOVER_HEIGHT);
        y=Math.max(y,minimumHoverY);
        navigation.moveTo(slot.x,y,slot.z,hasEffect(RoyaleSpells.RAGED)?1.35:1.0);
    }
    private boolean validEnemy(LivingEntity e){return e!=null&&e!=this&&e.isAlive()&&e.isAttackable()&&SpellEngine.enemy(owner,e)
        &&(!(e instanceof net.minecraft.world.entity.player.Player)||level().getServer().isPvpAllowed());}
    public boolean inBeamRange(LivingEntity target){
        Vec3 delta=target.getBoundingBox().getCenter().subtract(position().add(0,1,0));
        // The original card measures its range on the arena plane. Reserve a
        // bounded vertical envelope so higher flight can still hit ground mobs.
        double range=CardBalance.isCard(this)?3.5:RANGE;
        return delta.horizontalDistanceSqr()<=range*range&&Math.abs(delta.y)<=VERTICAL_REACH;
    }
    public boolean beamVisible(LivingEntity target){
        Vec3 start=position().add(0,1,0),end=target.getBoundingBox().getCenter();
        double yaw=Math.toRadians(getYRot());Vec3 mouth=position().add(-Math.sin(yaw)*1.048,.690,Math.cos(yaw)*1.048);
        return level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()==HitResult.Type.MISS
            &&level().clip(new ClipContext(mouth,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getType()==HitResult.Type.MISS;
    }
    private void damage(LivingEntity victim){
        if(!validEnemy(victim))return;
        // An 0.4s beam tick must not be swallowed by vanilla's 0.5s hurt window.
        // Restore that window immediately so unrelated attackers gain no bypass.
        int immunity=victim.invulnerableTime;victim.invulnerableTime=0;
        try{CombatImpact.withoutKnockback(victim,()->{
            if(IronSpellSystem.loaded&&!CardBalance.isCard(this))return dev.royalespells.iron.InfernoDragonSpell.damage(this,victim,hitDamage());
            return victim.hurt(damageSources().indirectMagic(this,CombatCompatibility.resolve((ServerLevel)level(),owner)),hitDamage());
        });}finally{victim.invulnerableTime=Math.max(immunity,victim.invulnerableTime);}
    }
    @Override public void knockback(double strength,double x,double z){if(!CombatImpact.suppressed(this))resetBeam();super.knockback(strength,x,z);}
    @Override public boolean hurt(DamageSource source,float amount){if(source.getEntity() instanceof LivingEntity e&&SpellEngine.friendly(owner,e))return false;return super.hurt(source,amount);}
    @Override public boolean doHurtTarget(Entity target){return false;}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
    @Override protected void checkFallDamage(double y,boolean ground,BlockState state,BlockPos pos){}
    @Override protected void playStepSound(BlockPos pos,BlockState state){}
    @Override protected boolean shouldDropLoot(){return false;}
    @Override public boolean shouldDropExperience(){return false;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(owner!=null)tag.putUUID("SpellOwner",owner);tag.putInt("SpellLife",life);tag.putBoolean("SpellClone",isClone());tag.putInt("DeployTicks",deployTicks());tag.putBoolean("Deployed",deployed);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setup(tag.hasUUID("SpellOwner")?tag.getUUID("SpellOwner"):null,tag.contains("SpellLife")?tag.getInt("SpellLife"):LIFE_TICKS,tag.getBoolean("SpellClone"));entityData.set(DEPLOY,tag.contains("DeployTicks")?Math.max(0,Math.min(DEPLOY_TICKS,tag.getInt("DeployTicks"))):DEPLOY_TICKS);deployed=tag.getBoolean("Deployed");resetBeam();}
}
