package dev.royalespells.entity;

import dev.royalespells.*;
import dev.royalespells.spirit.SpiritElement;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;
import java.util.*;

/** A short-lived, target-seeking summon. A single committed leap ends in one burst. */
public final class ElementalSpirit extends PathfinderMob implements Summoned {
    private static final EntityDataAccessor<String> ELEMENT=SynchedEntityData.defineId(ElementalSpirit.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> LEAP=SynchedEntityData.defineId(ElementalSpirit.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CHAIN=SynchedEntityData.defineId(ElementalSpirit.class,EntityDataSerializers.BOOLEAN);
    // Official Dec 2025 cadence and Aug 2026 chain range, mapped to 20 TPS / blocks.
    public static final int CHAIN_INTERVAL=5,CHAIN_TARGETS=9;
    public static final double CHAIN_RANGE=3;
    private UUID owner,leapTarget;private int life=400,lastStep=-20;private float power=1;private boolean spent,deployed;
    private final Set<UUID> chainVisited=new LinkedHashSet<>();
    private UUID chainLast;private Vec3 chainFrom=Vec3.ZERO;private int chainWait;private long chainExpires;
    public ElementalSpirit(EntityType<? extends PathfinderMob> type,Level level){super(type,level);xpReward=0;}
    public static AttributeSupplier.Builder attributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,4).add(Attributes.MOVEMENT_SPEED,.32).add(Attributes.ATTACK_DAMAGE,2).add(Attributes.FOLLOW_RANGE,20);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ELEMENT,"fire");b.define(LEAP,0);b.define(CHAIN,false);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));}
    public SpiritElement element(){return SpiritElement.parse(entityData.get(ELEMENT));}
    public int leapTicks(){return entityData.get(LEAP);}public float power(){return power;}
    public boolean chaining(){return entityData.get(CHAIN);}
    public int chainedTargets(){return chainVisited.size();}
    public void configure(UUID owner,SpiritElement element,float power){setup(owner,400,false);entityData.set(ELEMENT,element.id());this.power=Math.max(.05f,Math.min(64,power));getAttribute(Attributes.MAX_HEALTH).setBaseValue(4*this.power);setHealth(getMaxHealth());}
    @Override public UUID ownerId(){return owner;}
    @Override public void setup(UUID owner,int life,boolean clone){this.owner=owner;this.life=life;setPersistenceRequired();setCanPickUpLoot(false);}
    @Override public boolean isClone(){return false;}
    @Override protected boolean shouldDropLoot(){return false;}
    @Override public boolean shouldDropExperience(){return false;}
    @Override public boolean fireImmune(){return element()==SpiritElement.FIRE;}
    @Override public boolean hurt(DamageSource source,float amount){if(chaining()||source.getEntity() instanceof LivingEntity e&&SpellEngine.friendly(owner,e))return false;return super.hurt(source,amount);}
    @Override public boolean isAttackable(){return !chaining()&&super.isAttackable();}
    @Override public boolean canBeSeenAsEnemy(){return !chaining()&&super.canBeSeenAsEnemy();}
    @Override public boolean isPushable(){return !chaining()&&super.isPushable();}
    @Override public boolean doHurtTarget(Entity victim){return false;}
    private boolean visible(LivingEntity e){return e.isAlive()&&hasLineOfSight(e);}
    private boolean hostile(LivingEntity e){return e!=this&&e.isAttackable()&&SpellEngine.enemy(owner,e)&&(!(e instanceof net.minecraft.world.entity.player.Player)||!(level() instanceof ServerLevel w)||w.getServer().isPvpAllowed());}
    private LivingEntity destination(ServerLevel world){
        SummonOrders.tick(this,owner);
        if(element()==SpiritElement.HEAL){
            var wounded=world.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(12,5,12),e->e!=this&&e.getHealth()<e.getMaxHealth()&&SpellEngine.friendly(owner,e)&&visible(e));
            if(!wounded.isEmpty())return wounded.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        }
        return getTarget()!=null&&visible(getTarget())?getTarget():null;
    }
    public boolean beginLeap(LivingEntity target){
        if(level().isClientSide||spent||leapTicks()>0||!visible(target)||distanceToSqr(target)>12.25||hasEffect(RoyaleSpells.STUN)||hasEffect(RoyaleSpells.FROZEN))return false;
        leapTarget=target.getUUID();entityData.set(LEAP,1);getNavigation().stop();
        var delta=target.position().subtract(position());var flat=new Vec3(delta.x,0,delta.z);double horizontal=flat.length();
        // Commit the takeoff before vanilla travel chooses its friction. Leaving
        // onGround true cut the first horizontal step almost in half after a run-up.
        // 7.2 is the sum of about eleven airborne steps at vanilla's 0.91 drag;
        // this is a ballistic launch toward the current position, not homing.
        getMoveControl().setWantedPosition(getX(),getY(),getZ(),0);
        setOnGround(false);
        var velocity=horizontal<.001?Vec3.ZERO:flat.scale(Math.min(.5,horizontal/7.2)/horizontal);
        setDeltaMovement(velocity.x,.42+Math.max(0,Math.min(.18,delta.y*.12)),velocity.z);hasImpulse=true;
        sound("jump");
        return true;
    }
    @Override public void tick(){
        Vec3 previousCenter=getBoundingBox().getCenter();
        super.tick();
        if(level().isClientSide){if(!chaining()&&tickCount%3==0&&!VisualState.frozen(this))trail();return;}
        var world=(ServerLevel)level();
        // The spirit has already discharged. Keep only its bounded, saved chain
        // state until all links finish; no AI, collision pushing or second burst.
        if(chaining()){tickChain(world);return;}
        if(--life<=0||spent){discard();return;}
        if(!deployed){deployed=true;sound("deploy");}
        if(hasEffect(RoyaleSpells.STUN)||hasEffect(RoyaleSpells.FROZEN)){getNavigation().stop();return;}
        if(leapTicks()>0){
            int t=leapTicks()+1;entityData.set(LEAP,t);
            // Sweep the actual travelled body, including the first airborne tick.
            // A close/small enemy used to be skipped by the four-tick grace period;
            // a dead original target also prevented contact with its neighbours.
            var contact=leapContact(world,previousCenter);
            if(contact!=null){detonate(contact);return;}
            // A blocked/missed jump never deals remote damage or jumps through a wall.
            if(t>7&&onGround()||t>=30){detonate(null);return;}
        }else if(!isNoAi()){
            var target=destination(world);
            if(target!=null){getLookControl().setLookAt(target,30,30);if(distanceToSqr(target)<=9)beginLeap(target);else if(tickCount%5==0)getNavigation().moveTo(target,1.25);}
        }
    }
    private void sound(String phase){SpellSounds.play(level(),position(),"spirit_"+element().id(),phase);}
    @Override protected void playStepSound(net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState block){
        if(!chaining()&&!spent&&leapTicks()==0&&tickCount-lastStep>=10){lastStep=tickCount;sound("step");}
    }
    private LivingEntity leapContact(ServerLevel world,Vec3 from){
        Vec3 to=getBoundingBox().getCenter();
        double reach=getBbWidth()*.5+.24,vertical=getBbHeight()*.5+.24;
        var swept=new AABB(from,to).inflate(reach,vertical,reach);
        return world.getEntitiesOfClass(LivingEntity.class,swept,e->e!=this&&e.isAlive()&&e.isAttackable()
            &&(hostile(e)||element()==SpiritElement.HEAL&&e.getHealth()<e.getMaxHealth()&&SpellEngine.friendly(owner,e))
            &&visible(e)&&touches(e.getBoundingBox().inflate(reach,vertical,reach),from,to))
            .stream().min(Comparator.comparingDouble((LivingEntity e)->contactDistance(e,from,to,reach,vertical)).thenComparingInt(Entity::getId)).orElse(null);
    }
    private static boolean touches(AABB box,Vec3 from,Vec3 to){return box.contains(from)||box.contains(to)||box.clip(from,to).isPresent();}
    private static double contactDistance(LivingEntity e,Vec3 from,Vec3 to,double reach,double vertical){
        var box=e.getBoundingBox().inflate(reach,vertical,reach);
        return box.contains(from)?0:box.clip(from,to).orElse(to).distanceToSqr(from);
    }
    private void trail(){
        var c=element().color;level().addParticle(new DustParticleOptions(new Vector3f((c>>16&255)/255f,(c>>8&255)/255f,(c&255)/255f),.6f),getX(),getY()+.2,getZ(),0,.015,0);
        if(element()==SpiritElement.FIRE){
            double a=tickCount*2.399963,x=Math.cos(a)*.26,z=Math.sin(a)*.26;
            level().addParticle(ParticleTypes.FLAME,getX()+x,getY()+.08+random.nextDouble()*.48,getZ()+z,x*.015,.035,z*.015);
        }
    }
    private void hit(LivingEntity target,float amount){
        if(!hostile(target))return;
        if(IronSpellSystem.loaded)dev.royalespells.iron.SpiritSpells.damage(this,target,amount);
        else CombatImpact.withoutKnockback(target,()->{SpellEngine.hit((ServerLevel)level(),owner,target,amount);return null;});
    }
    public void detonate(LivingEntity primary){
        if(!(level() instanceof ServerLevel world)||spent)return;spent=true;getNavigation().stop();
        var kind=element();
        if(kind==SpiritElement.ELECTRO){
            if(primary==null||!hostile(primary)){discard();return;}
            entityData.set(CHAIN,true);setInvisible(true);setNoAi(true);setNoGravity(true);setDeltaMovement(Vec3.ZERO);
            chainExpires=world.getGameTime()+CHAIN_INTERVAL*(CHAIN_TARGETS+1);
            chainFrom=getBoundingBox().getCenter();chainHit(world,primary);return;
        }else{
            double radius=kind==SpiritElement.FIRE?2.4:2.2;
            for(var target:SpellEngine.targets(world,owner,position(),radius,false))if(hostile(target)&&visible(target)&&distanceToSqr(target)<=radius*radius){
                hit(target,kind.damage*power);
                if(kind==SpiritElement.ICE){SpellEngine.stun(target,20);target.addEffect(new MobEffectInstance(RoyaleSpells.FROZEN,20,0,false,false));}
            }
        }
        if(kind==SpiritElement.HEAL)for(var target:world.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(3,3,3),e->e!=this&&SpellEngine.friendly(owner,e)&&visible(e)&&e.distanceToSqr(this)<=9)){
            if(IronSpellSystem.loaded)dev.royalespells.iron.SpiritSpells.heal(this,target,4*power);else target.heal(4*power);
        }
        burst(world,kind);discard();
    }
    private void chainHit(ServerLevel world,LivingEntity victim){
        chainVisited.add(victim.getUUID());chainLast=victim.getUUID();
        Vec3 end=victim.getBoundingBox().getCenter();
        hit(victim,SpiritElement.ELECTRO.damage*power);SpellEngine.stun(victim,10);
        SpiritArc.link(world,chainFrom,end);
        world.sendParticles(ParticleTypes.ELECTRIC_SPARK,end.x,end.y,end.z,5,.14,.2,.14,.025);
        SpellSounds.play(world,end,"spirit_electro","impact");
        chainFrom=end;chainWait=CHAIN_INTERVAL;
        if(chainVisited.size()>=CHAIN_TARGETS)discard();
    }
    private void tickChain(ServerLevel world){
        setDeltaMovement(Vec3.ZERO);getNavigation().stop();
        if(world.getGameTime()>=chainExpires||chainVisited.size()>=CHAIN_TARGETS){discard();return;}
        if(--chainWait>0)return;
        // A killed link remains a valid origin; a surviving unit may have moved.
        if(chainLast!=null&&world.getEntity(chainLast) instanceof LivingEntity previous&&previous.isAlive())chainFrom=previous.getBoundingBox().getCenter();
        Vec3 origin=chainFrom;double r=CHAIN_RANGE;
        var next=world.getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(origin.add(-r,-r,-r),origin.add(r,r,r)),
            e->!chainVisited.contains(e.getUUID())&&hostile(e)&&e.getBoundingBox().getCenter().distanceToSqr(origin)<=r*r&&chainVisible(world,origin,e))
            .stream().min(Comparator.comparingDouble((LivingEntity e)->e.getBoundingBox().getCenter().distanceToSqr(origin)).thenComparingInt(Entity::getId)).orElse(null);
        if(next==null){discard();return;}chainHit(world,next);
    }
    private boolean chainVisible(ServerLevel world,Vec3 from,LivingEntity to){
        return world.clip(new net.minecraft.world.level.ClipContext(from,to.getBoundingBox().getCenter(),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,this)).getType()==net.minecraft.world.phys.HitResult.Type.MISS;
    }
    private void burst(ServerLevel w,SpiritElement kind){
        int color=kind.color;var dust=new DustParticleOptions(new Vector3f((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f),1.1f);
        for(int i=0;i<36;i++){double a=i*Math.PI/18;w.sendParticles(dust,getX()+Math.cos(a)*1.7,getY()+.2,getZ()+Math.sin(a)*1.7,1,.08,.1,.08,.02);}
        w.sendParticles(kind==SpiritElement.FIRE?ParticleTypes.FLAME:kind==SpiritElement.ICE?ParticleTypes.SNOWFLAKE:kind==SpiritElement.HEAL?ParticleTypes.HAPPY_VILLAGER:ParticleTypes.ELECTRIC_SPARK,getX(),getY()+.4,getZ(),18,.55,.5,.55,.05);
        sound("impact");
    }
    @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);if(owner!=null)n.putUUID("SpellOwner",owner);n.putInt("SpellLife",life);n.putString("SpiritElement",element().id());n.putFloat("SpiritPower",power);n.putBoolean("Spent",spent);n.putInt("Leap",leapTicks());if(leapTarget!=null)n.putUUID("LeapTarget",leapTarget);
        n.putBoolean("SpiritDeployed",deployed);n.putBoolean("ChainActive",chaining());n.putInt("ChainWait",chainWait);n.putLong("ChainExpires",chainExpires);if(chainLast!=null)n.putUUID("ChainLast",chainLast);
        n.putDouble("ChainX",chainFrom.x);n.putDouble("ChainY",chainFrom.y);n.putDouble("ChainZ",chainFrom.z);
        var visited=new net.minecraft.nbt.ListTag();chainVisited.forEach(id->visited.add(net.minecraft.nbt.NbtUtils.createUUID(id)));n.put("ChainVisited",visited);
    }
    @Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);owner=n.hasUUID("SpellOwner")?n.getUUID("SpellOwner"):null;life=n.contains("SpellLife")?n.getInt("SpellLife"):400;entityData.set(ELEMENT,SpiritElement.parse(n.getString("SpiritElement")).id());power=n.contains("SpiritPower")?Math.max(.05f,Math.min(64,n.getFloat("SpiritPower"))):1;spent=n.getBoolean("Spent");entityData.set(LEAP,n.getInt("Leap"));leapTarget=n.hasUUID("LeapTarget")?n.getUUID("LeapTarget"):null;
        deployed=n.contains("SpiritDeployed")?n.getBoolean("SpiritDeployed"):true;
        chainVisited.clear();for(var value:n.getList("ChainVisited",net.minecraft.nbt.Tag.TAG_INT_ARRAY)){if(chainVisited.size()>=CHAIN_TARGETS)break;chainVisited.add(net.minecraft.nbt.NbtUtils.loadUUID(value));}
        entityData.set(CHAIN,n.getBoolean("ChainActive")&&element()==SpiritElement.ELECTRO&&!chainVisited.isEmpty());chainWait=Math.max(1,Math.min(CHAIN_INTERVAL,n.getInt("ChainWait")));chainExpires=n.getLong("ChainExpires");chainLast=n.hasUUID("ChainLast")?n.getUUID("ChainLast"):null;chainFrom=new Vec3(n.getDouble("ChainX"),n.getDouble("ChainY"),n.getDouble("ChainZ"));
        if(chaining()){setInvisible(true);setNoAi(true);setNoGravity(true);spent=true;}
    }
}
