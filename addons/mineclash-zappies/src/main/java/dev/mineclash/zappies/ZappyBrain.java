package dev.mineclash.zappies;
import dev.mineclash.zappies.pause.ElectricPause;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.liziyowo.mineclash.entity.mob.Zappies;
import java.util.*;
/** Server-owned windup/recovery counters. Stun pauses this goal without calling stop().
 * A normal volley is 16 windup ticks + (interval - 16) recovery ticks. */
public final class ZappyBrain {
    public static final int FIRST_HIT=16,STUN_TICKS=10;
    public static final double RANGE=4.5,VERTICAL_RANGE=6.0;
    public final Zappies mob;
    public UUID owner,squad;
    public int slot,level=11,deploy,windup=-1,recovery,shots;
    private boolean configured;
    private LivingEntity locked;
    private int pathClock,acquireClock;
    public ZappyBrain(Zappies mob){this.mob=mob;}
    public static ZappyBrain of(Zappies e){return ((ZappyAccess)e).zappyBrain();}
    public void configure(int rank,UUID owner,UUID squad,int slot,int deploy){
        level=ZappiesConfig.clampLevel(rank);this.owner=owner;this.squad=squad;this.slot=slot;this.deploy=deploy;
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(ZappiesConfig.HP[level-3]*ZappiesConfig.scale());
        mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(ZappiesConfig.DAMAGE[level-3]*ZappiesConfig.scale());
        // Measured with the native MoveControl: this attribute affects both input
        // and acceleration. Account for accumulated velocity and ground friction.
        mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.1522);
        mob.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(1);
        mob.setHealth(mob.getMaxHealth());mob.setPersistenceRequired();configured=true;
    }
    public void initialize(){if(!configured)configure(ZappiesConfig.level(),owner,squad,slot,0);}
    public boolean friendly(LivingEntity other){
        if(other==mob||mob.isAlliedTo(other))return true;
        if(owner!=null&&owner.equals(other.getUUID()))return true;
        if(other instanceof Zappies z){var b=of(z);if(squad!=null&&squad.equals(b.squad)||owner!=null&&owner.equals(b.owner))return true;}
        UUID otherOwner=ZappyOwnership.of(other);
        if(owner!=null&&owner.equals(otherOwner))return true;
        var master=master();
        if(master!=null&&otherOwner!=null&&mob.level() instanceof ServerLevel world){var otherMaster=world.getEntity(otherOwner);if(otherMaster!=null&&master.isAlliedTo(otherMaster))return true;}
        return master!=null&&master.isAlliedTo(other);
    }
    public LivingEntity master(){return owner!=null&&mob.level() instanceof ServerLevel w&&w.getEntity(owner) instanceof LivingEntity e?e:null;}
    public boolean valid(LivingEntity e){return e!=null&&e.isAlive()&&e.isAttackable()&&!friendly(e)
        &&!(e instanceof Player p&&(p.isCreative()||p.isSpectator()||!p.getServer().isPvpAllowed()));}
    public boolean inRange(LivingEntity e){return e!=null&&horizontal(mob.position(),e.position())<=RANGE*RANGE
        &&Math.abs(e.getBoundingBox().getCenter().y-(mob.getY()+.8))<=VERTICAL_RANGE;}
    public boolean canShoot(LivingEntity e){return valid(e)&&inRange(e)&&mob.hasLineOfSight(e);}
    private static double horizontal(Vec3 a,Vec3 b){return Math.pow(a.x-b.x,2)+Math.pow(a.z-b.z,2);}
    public void tick(){
        initialize();
        if(ElectricPause.active(mob)||!mob.isAlive())return;
        if(deploy>0){deploy--;mob.getNavigation().stop();return;}
        LivingEntity target=mob.getTarget(),master=master();
        if(master!=null){var ordered=master.getLastHurtMob();if(valid(ordered)&&master.tickCount-master.getLastHurtMobTimestamp()<200&&mob.distanceToSqr(ordered)<1024)target=ordered;
            else if(valid(master.getLastHurtByMob())&&master.tickCount-master.getLastHurtByMobTimestamp()<200)target=master.getLastHurtByMob();}
        if(!valid(target)&&++acquireClock>=5){acquireClock=0;target=mob.level().getEntitiesOfClass(LivingEntity.class,mob.getBoundingBox().inflate(18),
            e->valid(e)&&mob.hasLineOfSight(e)&&(e instanceof Enemy||e instanceof Mob m&&m.getTarget()!=null&&(m.getTarget()==mob||m.getTarget()==master)))
            .stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);}
        if(!valid(target))target=null;
        if(mob.getTarget()!=target)mob.setTarget(target);
        if(windup>=0){
            if(target!=locked||!canShoot(locked)){cancelWindup();return;}
            face(locked);mob.getNavigation().stop();
            if(++windup>=FIRST_HIT){ZappyCombat.shoot(mob,locked);shots++;windup=-1;recovery=Math.max(4,ZappiesConfig.interval()-FIRST_HIT);}
            return;
        }
        if(recovery>0)recovery--;
        if(target!=null){
            face(target);
            if(canShoot(target)){
                mob.getNavigation().stop();
                if(recovery<=0){locked=target;windup=0;mob.triggerAnim("attack","attack");}
            }else if(++pathClock>=8){
                pathClock=0;Vec3 point=combatSlot(target);
                // The pathfinder's arrival tolerance varies with hitbox and grid
                // rounding. If it says "arrived" outside weapon range, take the
                // remaining approach toward the target; stop as soon as range is met.
                if(mob.getNavigation().isDone()&&mob.position().distanceToSqr(point)<4)mob.getNavigation().moveTo(target,1);
                else mob.getNavigation().moveTo(point.x,point.y,point.z,1);
            }
        }else if(master!=null&&mob.distanceToSqr(master)>9){
            if(++pathClock>=10){pathClock=0;Vec3 dir=master.getLookAngle().multiply(1,0,1).normalize();
                Vec3 behind=master.position().subtract(dir.scale(slot==0?2.3:3.7));
                double side=slot==1?-1.35:slot==2?1.35:0;
                mob.getNavigation().moveTo(behind.x+dir.z*side,behind.y,behind.z-dir.x*side,1);}
        }else mob.getNavigation().stop();
    }
    public void cancelWindup(){if(windup>=0)mob.stopTriggeredAnim("attack","attack");windup=-1;locked=null;}
    private void face(LivingEntity e){mob.getLookControl().setLookAt(e,30,30);double dx=e.getX()-mob.getX(),dz=e.getZ()-mob.getZ();float yaw=(float)Math.toDegrees(Math.atan2(-dx,dz));mob.setYRot(yaw);mob.setYBodyRot(yaw);mob.setYHeadRot(yaw);}
    private Vec3 combatSlot(LivingEntity target){
        Vec3 from=mob.position();
        if(squad!=null){var team=mob.level().getEntitiesOfClass(Zappies.class,mob.getBoundingBox().inflate(32),z->z.isAlive()&&squad.equals(of(z).squad));
            var lead=team.stream().min(Comparator.comparingInt(z->of(z).slot)).orElse(mob);from=lead.position();}
        Vec3 away=from.subtract(target.position()).multiply(1,0,1).normalize();if(away.lengthSqr()<.1)away=new Vec3(0,0,-1);
        // Navigation stops near a node, not exactly at it. Aim comfortably inside
        // weapon range so a rear car cannot park just outside 4.5 blocks forever.
        double back=slot==0?2.5:3,side=slot==1?-1.5:slot==2?1.5:0;
        Vec3 p=target.position().add(away.scale(back)).add(away.z*side,0,-away.x*side);
        return new Vec3(p.x,mob.getY(),p.z);
    }
    public void save(CompoundTag root){var t=new CompoundTag();t.putBoolean("configured",configured);t.putInt("level",level);t.putInt("slot",slot);t.putInt("deploy",deploy);
        if(owner!=null)t.putUUID("owner",owner);if(squad!=null)t.putUUID("squad",squad);root.put("ZappiesAddon",t);}
    public void load(CompoundTag root){if(!root.contains("ZappiesAddon"))return;var t=root.getCompound("ZappiesAddon");configured=t.getBoolean("configured");level=t.getInt("level");slot=t.getInt("slot");deploy=t.getInt("deploy");owner=t.hasUUID("owner")?t.getUUID("owner"):null;squad=t.hasUUID("squad")?t.getUUID("squad"):null;}
}
