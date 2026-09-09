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
    public static java.util.function.BiConsumer<SpellEntity,Byte> impactVisual=(e,status)->{};
    public UUID ownerId,rerollId;
    public boolean decoy,reroll; public boolean preview;
    private final Set<UUID> hit=new HashSet<>();
    private final List<UUID> captured=new ArrayList<>();
    private EarthquakeDestruction earthquake;
    private int quakeBroken;
    public float power(){return entityData.get(DATA).contains("Power")?entityData.get(DATA).getFloat("Power"):1;}
    public void setPower(float power){var data=entityData.get(DATA).copy();data.putFloat("Power",Mth.clamp(power,.05f,64));entityData.set(DATA,data);}
    public String ironSpellId(){return entityData.get(DATA).getString("IronSpell");}
    public int ironLevel(){return Math.max(1,entityData.get(DATA).getInt("IronLevel"));}
    public boolean cardCast(){return ironSpellId().isEmpty();}
    public int cardLevel(){return Math.max(11,entityData.get(DATA).getInt("CardLevel"));}
    public void setCardLevel(int level){var data=entityData.get(DATA).copy();data.putInt("CardLevel",level>=12?12:11);entityData.set(DATA,data);}
    public double radius(){return cardCast()?CardBalance.stats(spell()).radius():spell().radius;}
    public float hitAmount(float ironAmount){return cardCast()?CardBalance.stats(spell()).hit(cardLevel()):ironAmount;}
    public float strikeRadius(int tick){return cardCast()?(float)(spell()==Spell.ZAP_EVOLUTION&&tick>=21?3:2.5):zapRadius(tick);}
    public int duration(){var data=entityData.get(DATA);return Math.round((cardCast()?CardBalance.stats(spell()).duration():spell().duration)*(data.contains("DurationScale")?data.getFloat("DurationScale"):1));}
    public void setIronSpell(String id,int level,float durationScale) {
        var data=entityData.get(DATA).copy();data.putString("IronSpell",id);data.putInt("IronLevel",level);
        data.putFloat("DurationScale",Mth.clamp(durationScale,.5f,1.75f));entityData.set(DATA,data);
    }
    private Mob summon(ServerLevel world,Vec3 pos,String kind,boolean decoy){
        var mob=SpellEngine.summon(world,ownerId,pos,kind,decoy);
        if(cardCast())CardBalance.apply(mob,kind,decoy,cardLevel());SpellEngine.empower(mob,power());
        IronSpellSystem.summon(mob,ownerId,ironSpellId(),ironLevel());return mob;
    }
    public static float zapRadius(int tick){return (float)(tick<21?Spell.ZAP.radius:Spell.ZAP_EVOLUTION.radius);}
    public List<Vec3> zapPoints(){
        List<Vec3> points=new ArrayList<>();
        for(Tag value:entityData.get(DATA).getList("ZapPoints",Tag.TAG_COMPOUND))points.add(getVec((CompoundTag)value,"p"));
        return points;
    }
    private void recordZapStrike(ServerLevel world,int tick){
        CompoundTag data=entityData.get(DATA).copy();ListTag points=new ListTag();
        for(LivingEntity victim:enemies(world,target(),strikeRadius(tick)).stream().limit(16).toList()){
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
        // Capture once at cast time, including almost vertical casts. Turning the
        // camera later must not steer an existing projectile or its rolling axis.
        Entity caster=owner==null?null:world.getEntity(owner);
        entity.setCastDirection(caster instanceof LivingEntity living?new Vec3(-Math.sin(living.getYRot()*Mth.DEG_TO_RAD),0,Math.cos(living.getYRot()*Mth.DEG_TO_RAD)):end.subtract(start));
        entity.setPos(spell.projectile() || spell.rolling()?start:end);return entity;
    }
    public Spell spell(){return Spell.byId(entityData.get(DATA).getInt("spell"));}
    public Vec3 start(){return getVec(entityData.get(DATA),"start");}
    public Vec3 target(){return getVec(entityData.get(DATA),"target");}
    public Vec3 castDirection(){var data=entityData.get(DATA);return SpellEngine.horizontal(data.contains("FacingX")?getVec(data,"Facing"):target().subtract(start()));}
    public void setCastDirection(Vec3 direction){var data=entityData.get(DATA).copy();putVec(data,"Facing",SpellEngine.horizontal(direction));entityData.set(DATA,data);}
    @Override public void handleEntityEvent(byte status){if(status==64||status==65)impactVisual.accept(this,status);else super.handleEntityEvent(status);}
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
            return target().add(castDirection().scale(Math.min(1,(t-24)/18)*4)).add(0,0.7,0);
        return start().lerp(target(),progress).add(0,Math.sin(progress*Math.PI)*(spell==Spell.FIREBALL?1.5:6),0);
    }
    @Override public void tick() {
        if(isRemoved())return;
        super.tick();
        if(level().isClientSide){visualTick.accept(this);return;}
        if(!(level() instanceof ServerLevel world))return;
        if(preview && Boolean.getBoolean("royalespells.visualSmoke"))return;
        int t=time()+1;entityData.set(TIME,t);
        if(t==1) {
            if(!decoy){if(spell()!=Spell.LIGHTNING)SpellSounds.play(world,spell().projectile()||spell().rolling()?start():target(),spell(),reroll?"reroll":"deploy");SpellSounds.play(world,start(),spell(),"travel");}
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
        if(t>=duration()) {
            SpellSounds.play(world,target(),spell(),"end");
            if(reroll && rerollId!=null && world.getEntity(rerollId) instanceof AllyZombie hero)hero.heal((hero.getMaxHealth()-hero.getHealth())*0.5f);
            if(spell()==Spell.GIANT_SNOWBALL_EVOLUTION) {
                world.broadcastEntityEvent(this,(byte)65);
                for(UUID id:captured)if(world.getEntity(id) instanceof LivingEntity e){e.removeEffect(RoyaleSpells.STUN);SnowballChill.apply(e,cardCast());}
            }
            discard();
        }
    }
    private List<LivingEntity> enemies(ServerLevel world,Vec3 center,double radius){return SpellEngine.targets(world,ownerId,center,radius,false);}
    private void damage(ServerLevel world,LivingEntity e,float amount){IronSpellSystem.damage(this,e,amount*power());}
    private void area(ServerLevel world,Vec3 center,double radius,float amount,int stun,double knock) {
        for(LivingEntity e:enemies(world,center,radius)) {
            damage(world,e,amount);
            if(stun>0){if(spell()==Spell.ZAP||spell()==Spell.ZAP_EVOLUTION)SpellEngine.electricStun(e,stun);else SpellEngine.stun(e,stun);}
            if(knock>0){Vec3 dir=SpellEngine.horizontal(e.position().subtract(center));SpellMotion.impulse(e,new Vec3(dir.x*knock,0.2,dir.z*knock));}
        }
    }
    private void roll(ServerLevel world) {
        for(LivingEntity e:enemies(world,position(),radius())) {
            if(e.getY()>getY()+1.8)continue;
            if(cardCast()){double along=e.position().subtract(start()).dot(SpellEngine.horizontal(target().subtract(start())));if(along<0||along>target().subtract(start()).horizontalDistance())continue;}
            if(!hit.add(e.getUUID()))continue;
            damage(world,e,hitAmount(spell()==Spell.THE_LOG?8:6));
            if(hit.size()==1 || time()%4==0)SpellSounds.play(world,position(),spell(),"hit");
            Vec3 direction=SpellEngine.horizontal(target().subtract(start()));
            if(spell()==Spell.THE_LOG)SpellMotion.impulse(e,new Vec3(direction.x*.85,.15,direction.z*.85));
        }
        if(time()==spell().duration && spell()!=Spell.THE_LOG && !reroll) {
            summon(world,position(),spell()==Spell.BARBARIAN_BARREL_HERO?"hero":"barbarian",false);
            SpellSounds.play(world,position(),spell(),"impact");
        }
    }
    private void projectile(ServerLevel world,int t) {
        if(spell()==Spell.GIANT_SNOWBALL_EVOLUTION && t>=24) {
            if(t==24) {
                world.broadcastEntityEvent(this,(byte)64);
                for(LivingEntity e:enemies(world,target(),radius())) {damage(world,e,hitAmount(4));captured.add(e.getUUID());}
                SpellSounds.play(world,target(),spell(),"impact");if(!captured.isEmpty())SpellSounds.play(world,target(),spell(),"capture");
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
            case FIREBALL -> {area(world,target(),radius(),hitAmount(12),0,0.9);world.broadcastEntityEvent(this,(byte)64);SpellSounds.play(world,target(),spell(),"impact");}
            case ROCKET -> {area(world,target(),radius(),hitAmount(30),0,1.2);burst(world,ParticleTypes.EXPLOSION,12);SpellSounds.play(world,target(),spell(),"impact");}
            case PARTY_ROCKET -> {
                var victims=enemies(world,target(),radius());for(LivingEntity e:victims)SpellEngine.curse(ownerId,e,power(),ironSpellId(),ironLevel(),cardCast()?cardLevel():0);
                if(cardCast()){for(LivingEntity victim:victims)damage(world,victim,victim.getHealth()+victim.getAbsorptionAmount());}
                else area(world,target(),radius(),30,0,0.8);burst(world,ParticleTypes.HAPPY_VILLAGER,40);SpellSounds.play(world,target(),spell(),"impact");SpellSounds.play(world,target(),spell(),"party");
            }
            case GIANT_SNOWBALL -> {area(world,target(),radius(),hitAmount(4),0,1.1);for(LivingEntity e:enemies(world,target(),radius()))SnowballChill.apply(e,cardCast());world.broadcastEntityEvent(this,(byte)64);burst(world,ParticleTypes.SNOWFLAKE,32);SpellSounds.play(world,target(),spell(),"impact");}
            case GOBLIN_BARREL, GOBLIN_BARREL_EVOLUTION -> {
                boolean deployed=false;
                for(int i=0;i<3;i++){double a=i*Math.PI*2/3;deployed|=summon(world,SpellEngine.ground(world,target().add(Math.cos(a),0,Math.sin(a))),"zombie",decoy)!=null;}
                if(deployed&&spell()==Spell.GOBLIN_BARREL_EVOLUTION)EvolutionBurst.deploy(world,SpellEngine.ground(world,target()),1.5f);
                SpellSounds.play(world,target(),spell(),"impact");burst(world,ParticleTypes.HAPPY_VILLAGER,18);
            }
            case ROYAL_DELIVERY -> {area(world,target(),radius(),hitAmount(10),0,0);summon(world,target(),"recruit",false);SpellSounds.play(world,target(),spell(),"impact");SpellSounds.play(world,target(),spell(),"summon");}
            default -> {}
        }
    }
    private void field(ServerLevel world,int t) {
        switch(spell()) {
            case ARROWS -> {if(t==4||t==(ironSpellId().isEmpty()?12:14)||t==(ironSpellId().isEmpty()?20:24)){area(world,target(),radius(),hitAmount(3),0,0);SpellSounds.play(world,target(),spell(),"strike");}}
            case ZAP, ZAP_EVOLUTION -> {
                if(t==1 || spell()==Spell.ZAP_EVOLUTION && t==21) {
                    recordZapStrike(world,t);area(world,target(),strikeRadius(t),hitAmount(4),10,0);if(t==21)SpellSounds.play(world,target(),spell(),"strike");
                }
            }
            case LIGHTNING -> {
                if(t==1)enemies(world,target(),radius()).stream().sorted(Comparator.comparingDouble(LivingEntity::getHealth).reversed()).limit(3).forEach(e->captured.add(e.getUUID()));
                if(t==2||t==8||t==14) {
                    int i=(t-2)/6;
                    if(i<captured.size() && world.getEntity(captured.get(i)) instanceof LivingEntity e && e.isAlive()) {
                        LightningBolt bolt=EntityType.LIGHTNING_BOLT.create(world);
                        if(bolt!=null){bolt.moveTo(e.position());bolt.setVisualOnly(true);bolt.setSilent(true);world.addFreshEntity(bolt);}
                        damage(world,e,hitAmount(22));SpellEngine.electricStun(e,10);SpellSounds.play(world,e.position(),spell(),"deploy");
                    }
                }
            }
            case POISON -> {if(t%20==1)for(LivingEntity e:enemies(world,target(),radius())){damage(world,e,hitAmount(2));if(!cardCast())slow(e,25);}}
            case FREEZE -> {
                if(t==1){area(world,target(),radius(),hitAmount(2),0,0);}
                for(LivingEntity e:enemies(world,target(),radius())){int remaining=cardCast()?Math.min(3,duration()-t+1):3;SpellEngine.stun(e,remaining);e.addEffect(new MobEffectInstance(RoyaleSpells.FROZEN,remaining,0,false,false,false));}
            }
            case RAGE -> {
                if(t==1)area(world,target(),radius(),hitAmount(3),0,0);
                if(t%5==1)for(LivingEntity e:SpellEngine.targets(world,ownerId,target(),radius(),true)) {
                    e.addEffect(new MobEffectInstance(RoyaleSpells.RAGED,30,0,false,false));
                    if(cardCast())e.addEffect(new MobEffectInstance(RoyaleSpells.CARD_SPEED,30,0,false,false));
                    else e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,30,1,false,false));
                    e.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,30,1,false,false));
                }
            }
            case TORNADO -> {
                for(LivingEntity e:enemies(world,target(),radius())) {
                    Vec3 d=target().add(0,0.4,0).subtract(e.position());
                    SpellMotion.pull(e,e.getDeltaMovement().scale(0.35).add(d.scale(0.12)));
                    if(t==1||t==(cardCast()?11:16))damage(world,e,hitAmount(2));
                }
            }
            case EARTHQUAKE -> {
                if(earthquake==null)earthquake=new EarthquakeDestruction(world,target());
                quakeBroken+=earthquake.tick(world,ownerId,t);
                if(t%20==1){for(LivingEntity e:enemies(world,target(),radius()))if(e.onGround() || e.getY()<=target().y+0.5){damage(world,e,cardCast()&&e instanceof RoyaleUnit hut&&hut.building()?CardBalance.earthquakeBuildingHit(cardLevel()):hitAmount(3));if(cardCast())e.addEffect(new MobEffectInstance(RoyaleSpells.CARD_SLOW,25,9,false,false));else slow(e,25);}
                    SpellSounds.play(world,target(),spell(),"strike");}
            }
            case GRAVEYARD -> {
                if(cardCast()?t>=20&&t<=163&&(t-20)%13==0:t>=20&&t%12==8) {
                    // The current card has a fixed spawn pattern. Eight repeated peripheral
                    // anchors adapt its 3.3-tile average spawn radius to block terrain.
                    int slot=(t-20)/13;int[] order={0,4,2,6,1,5,3,7,0,4,2,6};
                    double a=cardCast()?order[Math.min(slot,11)]*Math.PI/4:world.random.nextDouble()*Math.PI*2,r=cardCast()?3.3:1.5+world.random.nextDouble()*2;
                    summon(world,SpellEngine.ground(world,target().add(Math.cos(a)*r,0,Math.sin(a)*r)),"skeleton",false);
                }
            }
            case CLONE -> {if(t==1){SpellEngine.cloneAllies(world,ownerId,target(),radius(),power(),ironSpellId(),ironLevel(),cardCast()?cardLevel():0);burst(world,ParticleTypes.END_ROD,32);}}
            case GOBLIN_CURSE -> {
                for(LivingEntity e:enemies(world,target(),radius())) {
                    SpellEngine.curse(ownerId,e,power(),ironSpellId(),ironLevel(),cardCast()?cardLevel():0);slow(e,24);if(t%20==1)damage(world,e,hitAmount(1));
                }
            }
            case VOID -> {
                if(t==16||t==(cardCast()?36:40)||t==(cardCast()?56:64)) {
                    SpellSounds.play(world,target(),spell(),"strike");
                    var list=enemies(world,target(),radius());float dmg=cardCast()?CardBalance.voidHit(list.size(),cardLevel()):list.size()==1?20:list.size()<=4?9:4;
                    recordVoidStrike(list,t);
                    for(LivingEntity e:list)damage(world,e,dmg);
                }
            }
            case VINES -> {
                if(t==1)enemies(world,target(),radius()).stream().sorted(Comparator.comparingDouble(LivingEntity::getHealth).reversed()).limit(3).forEach(e->captured.add(e.getUUID()));
                for(UUID id:captured)if(world.getEntity(id) instanceof LivingEntity e && e.isAlive()) {
                    if(t==1)SpellSounds.play(world,e.position(),spell(),"hit");
                    SpellEngine.stun(e,3);
                    e.addEffect(new MobEffectInstance(RoyaleSpells.ROOTED,3,0,false,false,false));
                    if(t%20==1)damage(world,e,hitAmount(2));
                    if(!e.onGround()){Vec3 next=e.position().add(0,-0.15,0);if(world.noCollision(e,e.getBoundingBox().move(0,-0.15,0)))e.teleportTo(next.x,next.y,next.z);}
                }
            }
            case HEAL -> {if(t%20==1)for(LivingEntity e:SpellEngine.targets(world,ownerId,target(),radius(),true))IronSpellSystem.heal(this,e,hitAmount(2)*power());}
            case WARMTH -> {for(LivingEntity e:SpellEngine.targets(world,ownerId,target(),radius(),true)) {
                if(cardCast()&&t%20==1)IronSpellSystem.heal(this,e,hitAmount(0)*power());
                e.removeEffect(RoyaleSpells.STUN);e.removeEffect(RoyaleSpells.FROZEN);e.removeEffect(RoyaleSpells.ROOTED);e.removeEffect(RoyaleSpells.SNOWBOUND);e.removeEffect(RoyaleSpells.CARD_SLOW);e.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);e.setTicksFrozen(0);
                e.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,25,0,false,false));
            }}
            default -> {}
        }
    }
    private void slow(LivingEntity e,int ticks){e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,ticks,0,false,true));}
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



