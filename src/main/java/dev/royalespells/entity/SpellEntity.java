package dev.royalespells.entity;

import dev.royalespells.*;
import net.minecraft.entity.*;
import net.minecraft.entity.data.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.*;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import java.util.*;
import java.util.function.Consumer;

public class SpellEntity extends Entity {
    private static final TrackedData<NbtCompound> DATA=DataTracker.registerData(SpellEntity.class,TrackedDataHandlerRegistry.NBT_COMPOUND);
    private static final TrackedData<Integer> TIME=DataTracker.registerData(SpellEntity.class,TrackedDataHandlerRegistry.INTEGER);
    public static Consumer<SpellEntity> visualTick=e->{};
    public UUID ownerId,rerollId;
    public boolean decoy,reroll; public boolean preview;
    private final Set<UUID> hit=new HashSet<>();
    private final List<UUID> captured=new ArrayList<>();
    private EarthquakeDestruction earthquake;
    private int quakeBroken;
    public float power(){return dataTracker.get(DATA).contains("Power")?dataTracker.get(DATA).getFloat("Power"):1;}
    public void setPower(float power){var data=dataTracker.get(DATA).copy();data.putFloat("Power",MathHelper.clamp(power,1,1.1f));dataTracker.set(DATA,data);}
    private MobEntity summon(ServerWorld world,Vec3d pos,String kind,boolean decoy){var mob=SpellEngine.summon(world,ownerId,pos,kind,decoy);SpellEngine.empower(mob,power());return mob;}
    public static float zapRadius(int tick){return tick<21?2.5f:3f;}
    public int voidStrikeTick(){return dataTracker.get(DATA).getInt("VoidStrikeTick");}
    public int voidStrength(){return dataTracker.get(DATA).getInt("VoidStrength");}
    public List<Vec3d> voidStrikePoints(){
        List<Vec3d> points=new ArrayList<>();
        for(NbtElement value:dataTracker.get(DATA).getList("VoidPoints",NbtElement.COMPOUND_TYPE))points.add(getVec((NbtCompound)value,"p"));
        return points;
    }
    private void recordVoidStrike(List<LivingEntity> victims,int tick) {
        NbtCompound data=dataTracker.get(DATA).copy();NbtList points=new NbtList();
        for(LivingEntity victim:victims){NbtCompound point=new NbtCompound();putVec(point,"p",victim.getPos().add(0,victim.getHeight()*.5,0));points.add(point);}
        data.put("VoidPoints",points);data.putInt("VoidStrikeTick",tick);data.putInt("VoidStrength",victims.size()==1?3:victims.size()<=4?2:1);dataTracker.set(DATA,data);
    }
    public SpellEntity(EntityType<? extends SpellEntity> type,World world){super(type,world);noClip=true;setNoGravity(true);}
    @Override protected void initDataTracker(){dataTracker.startTracking(DATA,new NbtCompound());dataTracker.startTracking(TIME,0);}
    public static SpellEntity create(ServerWorld world,Spell spell,UUID owner,Vec3d start,Vec3d end) {
        SpellEntity entity=new SpellEntity(RoyaleSpells.SPELL,world);entity.ownerId=owner;
        NbtCompound data=new NbtCompound();data.putInt("spell",spell.ordinal());
        putVec(data,"start",start);putVec(data,"target",end);entity.dataTracker.set(DATA,data);
        entity.setPosition(spell.projectile() || spell.rolling()?start:end);return entity;
    }
    public Spell spell(){return Spell.byId(dataTracker.get(DATA).getInt("spell"));}
    public Vec3d start(){return getVec(dataTracker.get(DATA),"start");}
    public Vec3d target(){return getVec(dataTracker.get(DATA),"target");}
    public void setPreviewTime(int ticks){dataTracker.set(TIME,ticks);}
    public int time(){return dataTracker.get(TIME);}
    private static void putVec(NbtCompound nbt,String key,Vec3d v){nbt.putDouble(key+"X",v.x);nbt.putDouble(key+"Y",v.y);nbt.putDouble(key+"Z",v.z);}
    private static Vec3d getVec(NbtCompound nbt,String key){return new Vec3d(nbt.getDouble(key+"X"),nbt.getDouble(key+"Y"),nbt.getDouble(key+"Z"));}
    public Vec3d visualPosition(float tick) {
        Spell spell=spell();float t=time()+tick;
        if(spell.rolling())return start().lerp(target(),MathHelper.clamp(t/spell.duration,0,1));
        if(!spell.projectile())return target();
        float flight=spell==Spell.GIANT_SNOWBALL_EVOLUTION?24:spell.duration;
        float progress=MathHelper.clamp(t/flight,0,1);
        if(spell==Spell.ROYAL_DELIVERY)return target().add(0,12*(1-progress),0);
        if(spell==Spell.ROCKET || spell==Spell.PARTY_ROCKET)return RocketMotion.position(start(),target(),progress);
        if(spell==Spell.GIANT_SNOWBALL_EVOLUTION && t>=24)
            return target().add(SpellEngine.horizontal(target().subtract(start())).multiply(Math.min(1,(t-24)/18)*4)).add(0,0.7,0);
        return start().lerp(target(),progress).add(0,Math.sin(progress*Math.PI)*(spell==Spell.FIREBALL?1.5:6),0);
    }
    @Override public void tick() {
        super.tick();
        if(getWorld().isClient){visualTick.accept(this);return;}
        if(!(getWorld() instanceof ServerWorld world))return;
        if(preview && Boolean.getBoolean("royalespells.visualSmoke"))return;
        int t=time()+1;dataTracker.set(TIME,t);
        if(t==1) {
            if(spell()==Spell.GRAVEYARD) sound(RoyaleSpells.GRAVEYARD_DEPLOY,2.0f,1.0f);
            else sound(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,0.6f,spell().evolved()?1.4f:1.0f);
        }
        Vec3d pos=visualPosition(0);
        if(spell().rolling())pos=SpellEngine.ground(world,pos);
        setPosition(pos);
        if(reroll && rerollId!=null && world.getEntity(rerollId) instanceof AllyZombie hero) {
            SpellEngine.stun(hero,3);hero.setPosition(pos);hero.velocityModified=true;
        }
        if(spell().rolling())roll(world);
        else if(spell().projectile())projectile(world,t);
        else field(world,t);
        if(t>=spell().duration) {
            if(reroll && rerollId!=null && world.getEntity(rerollId) instanceof AllyZombie hero)hero.heal((hero.getMaxHealth()-hero.getHealth())*0.5f);
            if(spell()==Spell.GIANT_SNOWBALL_EVOLUTION) for(UUID id:captured)if(world.getEntity(id) instanceof LivingEntity e){e.removeStatusEffect(RoyaleSpells.STUN);slow(e,60);}
            discard();
        }
    }
    private List<LivingEntity> enemies(ServerWorld world,Vec3d center,double radius){return SpellEngine.targets(world,ownerId,center,radius,false);}
    private void damage(ServerWorld world,LivingEntity e,float amount){SpellEngine.hit(world,ownerId,e,amount*power());}
    private void area(ServerWorld world,Vec3d center,double radius,float amount,int stun,double knock) {
        for(LivingEntity e:enemies(world,center,radius)) {
            damage(world,e,amount);
            if(stun>0)SpellEngine.stun(e,stun);
            if(knock>0){Vec3d dir=SpellEngine.horizontal(e.getPos().subtract(center));e.addVelocity(dir.x*knock,0.2,dir.z*knock);e.velocityModified=true;}
        }
    }
    private void roll(ServerWorld world) {
        for(LivingEntity e:enemies(world,getPos(),spell().radius)) {
            if(e.getY()>getY()+1.8 || !hit.add(e.getUuid()))continue;
            damage(world,e,spell()==Spell.THE_LOG?8:6);
            Vec3d direction=SpellEngine.horizontal(target().subtract(start()));
            e.addVelocity(direction.x*0.85,0.15,direction.z*0.85);e.velocityModified=true;
        }
        if(time()==spell().duration && spell()!=Spell.THE_LOG && !reroll) {
            summon(world,getPos(),spell()==Spell.BARBARIAN_BARREL_HERO?"hero":"barbarian",false);
            sound(SoundEvents.BLOCK_WOOD_BREAK,1,0.8f);
        }
    }
    private void projectile(ServerWorld world,int t) {
        if(spell()==Spell.GIANT_SNOWBALL_EVOLUTION && t>=24) {
            if(t==24) {
                for(LivingEntity e:enemies(world,target(),spell().radius)) {damage(world,e,4);captured.add(e.getUuid());}
                sound(SoundEvents.BLOCK_SNOW_BREAK,1,0.6f);
            }
            for(UUID id:captured) if(world.getEntity(id) instanceof LivingEntity e && e.isAlive()) {
                SpellEngine.stun(e,4);
                Vec3d next=getPos().add(0,-0.7,0);
                // Do not move a captured troop through a wall or into a solid block.
                if(world.isSpaceEmpty(e,e.getBoundingBox().offset(next.subtract(e.getPos())))) {
                    e.requestTeleport(next.x,next.y,next.z);e.velocityModified=true;
                }
            }
            return;
        }
        if(t!=spell().duration)return;
        switch(spell()) {
            case FIREBALL -> {area(world,target(),2.5,12,0,0.9);burst(world,ParticleTypes.FLAME,45);sound(SoundEvents.ENTITY_GENERIC_EXPLODE,1,1.1f);}
            case ROCKET -> {area(world,target(),2.5,30,0,1.2);burst(world,ParticleTypes.EXPLOSION,12);sound(SoundEvents.ENTITY_GENERIC_EXPLODE,1.5f,0.7f);}
            case PARTY_ROCKET -> {
                var victims=enemies(world,target(),3);for(LivingEntity e:victims)SpellEngine.curse(ownerId,e,power());
                area(world,target(),3,30,0,0.8);burst(world,ParticleTypes.HAPPY_VILLAGER,40);sound(SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST,1,1);
            }
            case GIANT_SNOWBALL -> {area(world,target(),2.5,4,0,1.1);for(LivingEntity e:enemies(world,target(),2.5))slow(e,60);burst(world,ParticleTypes.SNOWFLAKE,40);sound(SoundEvents.BLOCK_SNOW_BREAK,1,0.7f);}
            case GOBLIN_BARREL, GOBLIN_BARREL_EVOLUTION -> {
                for(int i=0;i<3;i++){double a=i*Math.PI*2/3;summon(world,SpellEngine.ground(world,target().add(Math.cos(a),0,Math.sin(a))),"zombie",decoy);}
                sound(SoundEvents.BLOCK_WOOD_BREAK,1,0.7f);burst(world,ParticleTypes.HAPPY_VILLAGER,18);
            }
            case ROYAL_DELIVERY -> {area(world,target(),3,10,0,0.5);summon(world,target(),"recruit",false);sound(SoundEvents.BLOCK_ANVIL_LAND,0.7f,1.2f);}
            default -> {}
        }
    }
    private void field(ServerWorld world,int t) {
        switch(spell()) {
            case ARROWS -> {if(t==4||t==12||t==20){area(world,target(),4,3,0,0);sound(SoundEvents.ENTITY_ARROW_HIT,0.8f,0.85f);}}
            case ZAP, ZAP_EVOLUTION -> {
                if(t==1 || spell()==Spell.ZAP_EVOLUTION && t==21) {
                    area(world,target(),zapRadius(t),4,10,0);sound(SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT,0.4f,1.9f);
                }
            }
            case LIGHTNING -> {
                if(t==1)enemies(world,target(),3.5).stream().sorted(Comparator.comparingDouble(LivingEntity::getHealth).reversed()).limit(3).forEach(e->captured.add(e.getUuid()));
                if(t==2||t==8||t==14) {
                    int i=(t-2)/6;
                    if(i<captured.size() && world.getEntity(captured.get(i)) instanceof LivingEntity e && e.isAlive()) {
                        LightningEntity bolt=EntityType.LIGHTNING_BOLT.create(world);
                        if(bolt!=null){bolt.refreshPositionAfterTeleport(e.getPos());bolt.setCosmetic(true);world.spawnEntity(bolt);}
                        damage(world,e,22);SpellEngine.stun(e,10);
                    }
                }
            }
            case POISON -> {if(t%20==1)for(LivingEntity e:enemies(world,target(),3.5)){damage(world,e,2);slow(e,25);}}
            case FREEZE -> {
                if(t==1){area(world,target(),3,2,0,0);sound(SoundEvents.BLOCK_GLASS_BREAK,0.8f,0.65f);}
                for(LivingEntity e:enemies(world,target(),3)){SpellEngine.stun(e,3);e.addStatusEffect(new StatusEffectInstance(RoyaleSpells.FROZEN,3,0,false,false,false));}
            }
            case RAGE -> {
                if(t==1)area(world,target(),3.5,3,0,0);
                if(t%5==1)for(LivingEntity e:SpellEngine.targets(world,ownerId,target(),3.5,true)) {
                    e.addStatusEffect(new StatusEffectInstance(RoyaleSpells.RAGED,30,0,false,false));
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,30,1,false,false));
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE,30,1,false,false));
                }
            }
            case TORNADO -> {
                for(LivingEntity e:enemies(world,target(),5.5)) {
                    Vec3d d=target().add(0,0.4,0).subtract(e.getPos());
                    e.setVelocity(e.getVelocity().multiply(0.35).add(d.multiply(0.12)));e.velocityModified=true;
                    if(t==1||t==16)damage(world,e,2);
                }
            }
            case EARTHQUAKE -> {
                if(earthquake==null)earthquake=new EarthquakeDestruction(world,target());
                quakeBroken+=earthquake.tick(world,ownerId,quakeBroken);
                if(t%20==1){for(LivingEntity e:enemies(world,target(),3.5))if(e.isOnGround() || e.getY()<=target().y+0.5){damage(world,e,3);slow(e,25);}
                    sound(SoundEvents.BLOCK_POINTED_DRIPSTONE_BREAK,0.8f,0.5f);}
            }
            case GRAVEYARD -> {
                if(t>=20 && t%12==8) {
                    double a=world.random.nextDouble()*Math.PI*2,r=1.5+world.random.nextDouble()*2;
                    summon(world,SpellEngine.ground(world,target().add(Math.cos(a)*r,0,Math.sin(a)*r)),"skeleton",false);
                    sound(SoundEvents.ENTITY_SKELETON_AMBIENT,0.25f,1.5f);
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
                    sound(SoundEvents.ENTITY_WARDEN_SONIC_BOOM,0.5f,1.5f);
                }
            }
            case VINES -> {
                if(t==1)enemies(world,target(),3).stream().sorted(Comparator.comparingDouble(LivingEntity::getHealth).reversed()).limit(3).forEach(e->captured.add(e.getUuid()));
                for(UUID id:captured)if(world.getEntity(id) instanceof LivingEntity e && e.isAlive()) {
                    SpellEngine.stun(e,3);
                    e.addStatusEffect(new StatusEffectInstance(RoyaleSpells.ROOTED,3,0,false,false,false));
                    if(t%20==1)damage(world,e,2);
                    if(!e.isOnGround()){Vec3d next=e.getPos().add(0,-0.15,0);if(world.isSpaceEmpty(e,e.getBoundingBox().offset(0,-0.15,0)))e.requestTeleport(next.x,next.y,next.z);}
                }
            }
            case HEAL -> {if(t%20==1)for(LivingEntity e:SpellEngine.targets(world,ownerId,target(),3,true))e.heal(2*power());}
            case WARMTH -> {for(LivingEntity e:SpellEngine.targets(world,ownerId,target(),3,true)) {
                e.removeStatusEffect(RoyaleSpells.STUN);e.removeStatusEffect(RoyaleSpells.FROZEN);e.removeStatusEffect(RoyaleSpells.ROOTED);e.removeStatusEffect(StatusEffects.SLOWNESS);e.setFrozenTicks(0);
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE,25,0,false,false));
            }}
            default -> {}
        }
    }
    private void slow(LivingEntity e,int ticks){e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,ticks,0,false,true));}
    private void sound(SoundEvent sound,float volume,float pitch){getWorld().playSound(null,getX(),getY(),getZ(),sound,SoundCategory.PLAYERS,volume,pitch);}
    private void burst(ServerWorld world,net.minecraft.particle.ParticleEffect effect,int count){world.spawnParticles(effect,target().x,target().y+0.4,target().z,count,1,0.5,1,0.05);}
    @Override protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("QuakeBroken",quakeBroken);
        nbt.put("SpellData",dataTracker.get(DATA).copy());nbt.putInt("SpellTime",time());
        if(ownerId!=null)nbt.putUuid("Owner",ownerId);if(rerollId!=null)nbt.putUuid("RerollId",rerollId);
        nbt.putBoolean("DevPreview",preview && Boolean.getBoolean("royalespells.visualSmoke"));
        nbt.putBoolean("Decoy",decoy);nbt.putBoolean("Reroll",reroll);
        NbtList hits=new NbtList();hit.forEach(id->hits.add(NbtString.of(id.toString())));nbt.put("Hits",hits);
        NbtList ids=new NbtList();captured.forEach(id->ids.add(NbtString.of(id.toString())));nbt.put("Captured",ids);
    }
    @Override protected void readCustomDataFromNbt(NbtCompound nbt) {
        quakeBroken=nbt.getInt("QuakeBroken");earthquake=null;
        dataTracker.set(DATA,nbt.getCompound("SpellData"));dataTracker.set(TIME,nbt.getInt("SpellTime"));
        ownerId=nbt.containsUuid("Owner")?nbt.getUuid("Owner"):null;rerollId=nbt.containsUuid("RerollId")?nbt.getUuid("RerollId"):null;
        preview=nbt.getBoolean("DevPreview") && Boolean.getBoolean("royalespells.visualSmoke");
        decoy=nbt.getBoolean("Decoy");reroll=nbt.getBoolean("Reroll");
        hit.clear();captured.clear();
        for(NbtElement id:nbt.getList("Hits",NbtElement.STRING_TYPE))hit.add(UUID.fromString(id.asString()));
        for(NbtElement id:nbt.getList("Captured",NbtElement.STRING_TYPE))captured.add(UUID.fromString(id.asString()));
    }
    @Override public Packet<ClientPlayPacketListener> createSpawnPacket(){return new EntitySpawnS2CPacket(this);}
    @Override public boolean shouldRender(double distance){return distance<128*128;}
}





