package dev.royalespells.entity;

import net.minecraft.nbt.*;
import dev.royalespells.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import java.util.function.Consumer;

public class SpellEntity extends Entity {
    private static final EntityDataAccessor<CompoundTag> DATA=SynchedEntityData.defineId(SpellEntity.class,EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> TIME=SynchedEntityData.defineId(SpellEntity.class,EntityDataSerializers.INT);
    public static Consumer<SpellEntity> visualTick=e->{};
    public UUID ownerId,rerollId;
    public boolean decoy,reroll; public boolean preview;
    private final Set<UUID> hit=new HashSet<>();
    private final List<UUID> captured=new ArrayList<>();
    private EarthquakeDestruction earthquake;
    private int quakeBroken;
    public float power(){return entityData.get(DATA).contains("Power")?entityData.get(DATA).getFloat("Power"):1;}
    public void setPower(float power){var data=entityData.get(DATA).copy();data.putFloat("Power",Mth.clamp(power,1,1.1f));entityData.set(DATA,data);}
    private Mob summon(ServerLevel world,Vec3 pos,String kind,boolean decoy){var mob=SpellEngine.summon(world,ownerId,pos,kind,decoy);SpellEngine.empower(mob,power());return mob;}
    public static float zapRadius(int tick){return (float)(tick<21?Spell.ZAP.radius:Spell.ZAP_EVOLUTION.radius);}
    public List<Vec3> zapPoints(){
        List<Vec3> points=new ArrayList<>();
        for(Tag value:entityData.get(DATA).getList("ZapPoints",Tag.TAG_COMPOUND))points.add(getVec((CompoundTag)value,"p"));
        return points;
    }
    private void recordZapStrike(ServerLevel world,int tick){
        CompoundTag data=entityData.get(DATA).copy();ListTag points=new ListTag();
        for(LivingEntity victim:enemies(world,target(),zapRadius(tick)).stream().limit(16).toList()){
            CompoundTag point=new CompoundTag();putVec(point,"p",victim.position().add(0,victim.getBbHeight()*.55,0));points.add(point);
        }
        data.put("ZapPoints",points);entityData.set(DATA,data);
    }
    public int voidStrikeTick(){return entityData.get(DATA).getInt("VoidStrikeTick");}
    public int voidStrength(){return entityData.get(DATA).getInt("VoidStrength");}
    public List<Vec3> voidStrikePoints(){
        List<Vec3> points=new ArrayList<>();
        for(Tag value:entityData.get(DATA).getList("VoidPoints",Tag.TAG_COMPOUND))points.add(getVec((CompoundTag)value,"p"));
        return points;
    }
    private void recordVoidStrike(List<LivingEntity> victims,int tick) {
        CompoundTag data=entityData.get(DATA).copy();ListTag points=new ListTag();
        for(LivingEntity victim:victims){CompoundTag point=new CompoundTag();putVec(point,"p",victim.position().add(0,victim.getBbHeight()*.5,0));points.add(point);}
        data.put("VoidPoints",points);data.putInt("VoidStrikeTick",tick);data.putInt("VoidStrength",victims.size()==1?3:victims.size()<=4?2:1);entityData.set(DATA,data);
    }
    public SpellEntity(EntityType<? extends SpellEntity> type,Level world){super(type,world);noPhysics=true;setNoGravity(true);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){builder.define(DATA,new CompoundTag());builder.define(TIME,0);}
    public static SpellEntity create(ServerLevel world,Spell spell,UUID owner,Vec3 start,Vec3 end) {
        SpellEntity entity=new SpellEntity(RoyaleSpells.SPELL,world);entity.ownerId=owner;
        CompoundTag data=new CompoundTag();data.putInt("spell",spell.ordinal());
        putVec(data,"start",start);putVec(data,"target",end);entity.entityData.set(DATA,data);
        entity.setPos(spell.projectile() || spell.rolling()?start:end);return entity;
    }
    public Spell spell(){return Spell.byId(entityData.get(DATA).getInt("spell"));}
    public Vec3 start(){return getVec(entityData.get(DATA),"start");}
    public Vec3 target(){return getVec(entityData.get(DATA),"target");}
    public void setPreviewTime(int ticks){entityData.set(TIME,ticks);}
    public int time(){return entityData.get(TIME);}
    private static void putVec(CompoundTag nbt,String key,Vec3 v){nbt.putDouble(key+"X",v.x);nbt.putDouble(key+"Y",v.y);nbt.putDouble(key+"Z",v.z);}
    private static Vec3 getVec(CompoundTag nbt,String key){return new Vec3(nbt.getDouble(key+"X"),nbt.getDouble(key+"Y"),nbt.getDouble(key+"Z"));}
    public Vec3 visualPosition(float tick) {
        Spell spell=spell();float t=time()+tick;
        if(spell.rolling())return start().lerp(target(),Mth.clamp(t/spell.duration,0,1));
        if(!spell.projectile())return target();
        float flight=spell==Spell.GIANT_SNOWBALL_EVOLUTION?24:spell.duration;
        float progress=Mth.clamp(t/flight,0,1);
        if(spell==Spell.ROYAL_DELIVERY)return target().add(0,12*(1-progress),0);
        if(spell==Spell.ROCKET || spell==Spell.PARTY_ROCKET)return RocketMotion.position(start(),target(),progress);
        if(spell==Spell.GIANT_SNOWBALL_EVOLUTION && t>=24)
            return target().add(SpellEngine.horizontal(target().subtract(start())).scale(Math.min(1,(t-24)/18)*4)).add(0,0.7,0);
        return start().lerp(target(),progress).add(0,Math.sin(progress*Math.PI)*(spell==Spell.FIREBALL?1.5:6),0);
    }
    @Override public void tick() {
        super.tick();
        if(level().isClientSide){visualTick.accept(this);return;}
        if(!(level() instanceof ServerLevel world))return;
        if(preview && Boolean.getBoolean("royalespells.visualSmoke"))return;
        int t=time()+1;entityData.set(TIME,t);
        if(t==1) {
            if(spell()==Spell.GRAVEYARD) sound(RoyaleSpells.GRAVEYARD_DEPLOY,2.0f,1.0f);
            else sound(SoundEvents.ENCHANTMENT_TABLE_USE,0.6f,spell().evolved()?1.4f:1.0f);
        }
        Vec3 pos=visualPosition(0);
        if(spell().rolling())pos=SpellEngine.ground(world,pos);
        setPos(pos);
        if(reroll && rerollId!=null && world.getEntity(rerollId) instanceof AllyZombie hero) {
            SpellEngine.stun(hero,3);hero.setPos(pos);hero.hurtMarked=true;
        }
        if(spell().rolling())roll(world);
        else if(spell().projectile())projectile(world,t);
        else field(world,t);
        if(t>=spell().duration) {
            if(reroll && rerollId!=null && world.getEntity(rerollId) instanceof AllyZombie hero)hero.heal((hero.getMaxHealth()-hero.getHealth())*0.5f);
            if(spell()==Spell.GIANT_SNOWBALL_EVOLUTION) for(UUID id:captured)if(world.getEntity(id) instanceof LivingEntity e){e.removeEffect(RoyaleSpells.STUN);slow(e,60);}
            discard();
        }
    }
    private List<LivingEntity> enemies(ServerLevel world,Vec3 center,double radius){return SpellEngine.targets(world,ownerId,center,radius,false);}
    private void damage(ServerLevel world,LivingEntity e,float amount){SpellEngine.hit(world,ownerId,e,amount*power());}
    private void area(ServerLevel world,Vec3 center,double radius,float amount,int stun,double knock) {
        for(LivingEntity e:enemies(world,center,radius)) {
            damage(world,e,amount);
            if(stun>0)SpellEngine.stun(e,stun);
            if(knock>0){Vec3 dir=SpellEngine.horizontal(e.position().subtract(center));SpellMotion.impulse(e,new Vec3(dir.x*knock,0.2,dir.z*knock));}
        }
    }
    private void roll(ServerLevel world) {
        for(LivingEntity e:enemies(world,position(),spell().radius)) {
            if(e.getY()>getY()+1.8 || !hit.add(e.getUUID()))continue;
            damage(world,e,spell()==Spell.THE_LOG?8:6);
            Vec3 direction=SpellEngine.horizontal(target().subtract(start()));
            SpellMotion.impulse(e,new Vec3(direction.x*.85,.15,direction.z*.85));
        }
        if(time()==spell().duration && spell()!=Spell.THE_LOG && !reroll) {
            summon(world,position(),spell()==Spell.BARBARIAN_BARREL_HERO?"hero":"barbarian",false);
            sound(SoundEvents.WOOD_BREAK,1,0.8f);
        }
    }
    private void projectile(ServerLevel world,int t) {
        if(spell()==Spell.GIANT_SNOWBALL_EVOLUTION && t>=24) {
            if(t==24) {
                for(LivingEntity e:enemies(world,target(),spell().radius)) {damage(world,e,4);captured.add(e.getUUID());}
                sound(SoundEvents.SNOW_BREAK,1,0.6f);
            }
            for(UUID id:captured) if(world.getEntity(id) instanceof LivingEntity e && e.isAlive()) {
                SpellEngine.stun(e,4);
                Vec3 next=position().add(0,-0.7,0);
                // Do not move a captured troop through a wall or into a solid block.
                if(world.noCollision(e,e.getBoundingBox().move(next.subtract(e.position())))) {
                    e.teleportTo(next.x,next.y,next.z);e.hurtMarked=true;
                }
            }
            return;
        }
        if(t!=spell().duration)return;
        switch(spell()) {
            case FIREBALL -> {area(world,target(),spell().radius,12,0,0.9);burst(world,ParticleTypes.FLAME,45);sound(SoundEvents.GENERIC_EXPLODE.value(),1,1.1f);}
            case ROCKET -> {area(world,target(),2.5,30,0,1.2);burst(world,ParticleTypes.EXPLOSION,12);sound(SoundEvents.GENERIC_EXPLODE.value(),1.5f,0.7f);}
            case PARTY_ROCKET -> {
                var victims=enemies(world,target(),3);for(LivingEntity e:victims)SpellEngine.curse(ownerId,e,power());
                area(world,target(),3,30,0,0.8);burst(world,ParticleTypes.HAPPY_VILLAGER,40);sound(SoundEvents.FIREWORK_ROCKET_BLAST,1,1);
            }
            case GIANT_SNOWBALL -> {area(world,target(),2.5,4,0,1.1);for(LivingEntity e:enemies(world,target(),2.5))slow(e,60);burst(world,ParticleTypes.SNOWFLAKE,40);sound(SoundEvents.SNOW_BREAK,1,0.7f);}
            case GOBLIN_BARREL, GOBLIN_BARREL_EVOLUTION -> {
                for(int i=0;i<3;i++){double a=i*Math.PI*2/3;summon(world,SpellEngine.ground(world,target().add(Math.cos(a),0,Math.sin(a))),"zombie",decoy);}
                sound(SoundEvents.WOOD_BREAK,1,0.7f);burst(world,ParticleTypes.HAPPY_VILLAGER,18);
            }
            case ROYAL_DELIVERY -> {area(world,target(),3,10,0,0.5);summon(world,target(),"recruit",false);sound(SoundEvents.ANVIL_LAND,0.7f,1.2f);}
            default -> {}
        }
    }
    private void field(ServerLevel world,int t) {
        switch(spell()) {
            case ARROWS -> {if(t==4||t==12||t==20){area(world,target(),spell().radius,3,0,0);sound(SoundEvents.ARROW_HIT,0.8f,0.85f);}}
            case ZAP, ZAP_EVOLUTION -> {
                if(t==1 || spell()==Spell.ZAP_EVOLUTION && t==21) {
                    recordZapStrike(world,t);area(world,target(),zapRadius(t),4,10,0);sound(SoundEvents.LIGHTNING_BOLT_IMPACT,0.4f,1.9f);
                }
            }
            case LIGHTNING -> {
                if(t==1)enemies(world,target(),3.5).stream().sorted(Comparator.comparingDouble(LivingEntity::getHealth).reversed()).limit(3).forEach(e->captured.add(e.getUUID()));
                if(t==2||t==8||t==14) {
                    int i=(t-2)/6;
                    if(i<captured.size() && world.getEntity(captured.get(i)) instanceof LivingEntity e && e.isAlive()) {
                        LightningBolt bolt=EntityType.LIGHTNING_BOLT.create(world);
                        if(bolt!=null){bolt.moveTo(e.position());bolt.setVisualOnly(true);world.addFreshEntity(bolt);}
                        damage(world,e,22);SpellEngine.stun(e,10);
                    }
                }
            }
            case POISON -> {if(t%20==1)for(LivingEntity e:enemies(world,target(),3.5)){damage(world,e,2);slow(e,25);}}
            case FREEZE -> {
                if(t==1){area(world,target(),3,2,0,0);sound(SoundEvents.GLASS_BREAK,0.8f,0.65f);}
                for(LivingEntity e:enemies(world,target(),3)){SpellEngine.stun(e,3);e.addEffect(new MobEffectInstance(RoyaleSpells.FROZEN,3,0,false,false,false));}
            }
            case RAGE -> {
                if(t==1)area(world,target(),3.5,3,0,0);
                if(t%5==1)for(LivingEntity e:SpellEngine.targets(world,ownerId,target(),3.5,true)) {
                    e.addEffect(new MobEffectInstance(RoyaleSpells.RAGED,30,0,false,false));
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,30,1,false,false));
                    e.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,30,1,false,false));
                }
            }
            case TORNADO -> {
                for(LivingEntity e:enemies(world,target(),5.5)) {
                    Vec3 d=target().add(0,0.4,0).subtract(e.position());
                    SpellMotion.pull(e,e.getDeltaMovement().scale(0.35).add(d.scale(0.12)));
                    if(t==1||t==16)damage(world,e,2);
                }
            }
            case EARTHQUAKE -> {
                if(earthquake==null)earthquake=new EarthquakeDestruction(world,target());
                quakeBroken+=earthquake.tick(world,ownerId,t);
                if(t%20==1){for(LivingEntity e:enemies(world,target(),3.5))if(e.onGround() || e.getY()<=target().y+0.5){damage(world,e,3);slow(e,25);}
                    sound(SoundEvents.POINTED_DRIPSTONE_BREAK,0.8f,0.5f);}
            }
            case GRAVEYARD -> {
                if(t>=20 && t%12==8) {
                    double a=world.random.nextDouble()*Math.PI*2,r=1.5+world.random.nextDouble()*2;
                    summon(world,SpellEngine.ground(world,target().add(Math.cos(a)*r,0,Math.sin(a)*r)),"skeleton",false);
                    sound(SoundEvents.SKELETON_AMBIENT,0.25f,1.5f);
                }
            }
            case CLONE -> {if(t==1){SpellEngine.cloneAllies(world,ownerId,target(),3,power());burst(world,ParticleTypes.END_ROD,32);}}
            case GOBLIN_CURSE -> {
                for(LivingEntity e:enemies(world,target(),3)) {
                    SpellEngine.curse(ownerId,e,power());slow(e,24);if(t%20==1)damage(world,e,1);
                }
            }
            case VOID -> {
                if(t==16||t==40||t==64) {
                    var list=enemies(world,target(),3);float dmg=list.size()==1?20:list.size()<=4?9:4;
                    recordVoidStrike(list,t);
                    for(LivingEntity e:list)damage(world,e,dmg);
                    sound(SoundEvents.WARDEN_SONIC_BOOM,0.5f,1.5f);
                }
            }
            case VINES -> {
                if(t==1)enemies(world,target(),3).stream().sorted(Comparator.comparingDouble(LivingEntity::getHealth).reversed()).limit(3).forEach(e->captured.add(e.getUUID()));
                for(UUID id:captured)if(world.getEntity(id) instanceof LivingEntity e && e.isAlive()) {
                    SpellEngine.stun(e,3);
                    e.addEffect(new MobEffectInstance(RoyaleSpells.ROOTED,3,0,false,false,false));
                    if(t%20==1)damage(world,e,2);
                    if(!e.onGround()){Vec3 next=e.position().add(0,-0.15,0);if(world.noCollision(e,e.getBoundingBox().move(0,-0.15,0)))e.teleportTo(next.x,next.y,next.z);}
                }
            }
            case HEAL -> {if(t%20==1)for(LivingEntity e:SpellEngine.targets(world,ownerId,target(),3,true))e.heal(2*power());}
            case WARMTH -> {for(LivingEntity e:SpellEngine.targets(world,ownerId,target(),3,true)) {
                e.removeEffect(RoyaleSpells.STUN);e.removeEffect(RoyaleSpells.FROZEN);e.removeEffect(RoyaleSpells.ROOTED);e.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);e.setTicksFrozen(0);
                e.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,25,0,false,false));
            }}
            default -> {}
        }
    }
    private void slow(LivingEntity e,int ticks){e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,ticks,0,false,true));}
    private void sound(SoundEvent sound,float volume,float pitch){level().playSound(null,getX(),getY(),getZ(),sound,SoundSource.PLAYERS,volume,pitch);}
    private void burst(ServerLevel world,net.minecraft.core.particles.ParticleOptions effect,int count){world.sendParticles(effect,target().x,target().y+0.4,target().z,count,1,0.5,1,0.05);}
    @Override protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("QuakeBroken",quakeBroken);
        if(earthquake!=null)nbt.put("QuakeWork",earthquake.writeNbt());
        nbt.put("SpellData",entityData.get(DATA).copy());nbt.putInt("SpellTime",time());
        if(ownerId!=null)nbt.putUUID("Owner",ownerId);if(rerollId!=null)nbt.putUUID("RerollId",rerollId);
        nbt.putBoolean("DevPreview",preview && Boolean.getBoolean("royalespells.visualSmoke"));
        nbt.putBoolean("Decoy",decoy);nbt.putBoolean("Reroll",reroll);
        ListTag hits=new ListTag();hit.forEach(id->hits.add(StringTag.valueOf(id.toString())));nbt.put("Hits",hits);
        ListTag ids=new ListTag();captured.forEach(id->ids.add(StringTag.valueOf(id.toString())));nbt.put("Captured",ids);
    }
    @Override protected void readAdditionalSaveData(CompoundTag nbt) {
        quakeBroken=nbt.getInt("QuakeBroken");earthquake=nbt.contains("QuakeWork")?EarthquakeDestruction.fromNbt(nbt.getCompound("QuakeWork")):null;
        entityData.set(DATA,nbt.getCompound("SpellData"));entityData.set(TIME,nbt.getInt("SpellTime"));
        ownerId=nbt.hasUUID("Owner")?nbt.getUUID("Owner"):null;rerollId=nbt.hasUUID("RerollId")?nbt.getUUID("RerollId"):null;
        preview=nbt.getBoolean("DevPreview") && Boolean.getBoolean("royalespells.visualSmoke");
        decoy=nbt.getBoolean("Decoy");reroll=nbt.getBoolean("Reroll");
        hit.clear();captured.clear();
        for(Tag id:nbt.getList("Hits",Tag.TAG_STRING))hit.add(UUID.fromString(id.getAsString()));
        for(Tag id:nbt.getList("Captured",Tag.TAG_STRING))captured.add(UUID.fromString(id.getAsString()));
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.ServerEntity entry){return new ClientboundAddEntityPacket(this,entry);}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<128*128;}
}





