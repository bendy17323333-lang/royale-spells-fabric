package dev.royalespells;

import dev.royalespells.entity.*;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import java.util.*;

public final class SpellEngine {
    private SpellEngine(){}
    public static final int MAX_UNITS_PER_OWNER=64;
    private static final UUID MIRROR_ATTACK=UUID.fromString("b27ad4dd-7a38-40cf-8cc4-7ddad5b601b0");
    private static final Map<UUID,State> PLAYERS=new HashMap<>();
    private static final Map<UUID,Curse> CURSES=new HashMap<>();
    private static long clock;
    private static class State { double elixir=10; Spell last; TroopCard lastTroop; int cooldown; }
    private record Curse(UUID owner,long until,float power){}
    public static void clear(){PLAYERS.clear();CURSES.clear();clock=0;}
    public static void refill(ServerPlayerEntity player){PLAYERS.computeIfAbsent(player.getUuid(),id->new State()).elixir=10;}
    public static void tick(MinecraftServer server) {
        clock++;
        for(ServerPlayerEntity p:server.getPlayerManager().getPlayerList()) {
            State s=PLAYERS.computeIfAbsent(p.getUuid(),id->new State());
            s.elixir=Math.min(10,s.elixir+0.025); if(s.cooldown>0)s.cooldown--;
            if(clock%10==0 && (isCard(p.getMainHandStack().getItem()) || isCard(p.getOffHandStack().getItem()))) {
                String bar="◆".repeat((int)s.elixir)+"◇".repeat(10-(int)s.elixir);
                p.sendMessage(Text.translatable("message.royalespells.elixir",bar,String.format(java.util.Locale.ROOT,"%.1f",s.elixir)),true);
            }
        }
        if(clock%100==0) {
            CURSES.entrySet().removeIf(e->e.getValue().until<clock);
            PLAYERS.keySet().removeIf(id->server.getPlayerManager().getPlayer(id)==null);
        }
    }
    public static boolean isCard(Item item){return item instanceof SpellItem || item instanceof TroopItem;}
    public static boolean deploy(ServerPlayerEntity player,TroopCard card){return deploy(player,card,false);}
    private static boolean deploy(ServerPlayerEntity player,TroopCard card,boolean mirror) {
        State state=PLAYERS.computeIfAbsent(player.getUuid(),id->new State());int cost=card.cost+(mirror?1:0);
        if(state.cooldown>0 || player.hasStatusEffect(RoyaleSpells.STUN))return false;
        if(!player.isCreative() && state.elixir<cost)return insufficient(player);
        Vec3d at=ground(player.getWorld(),aim(player,32));
        if(!player.getServerWorld().getWorldBorder().contains(BlockPos.ofFloored(at)) || !player.getServerWorld().isChunkLoaded(BlockPos.ofFloored(at)) || !player.canModifyAt(player.getWorld(),BlockPos.ofFloored(at)))return false;
        var unit=summon(player.getServerWorld(),player.getUuid(),at,card.unit,false);
        if(unit==null){player.sendMessage(Text.translatable("message.royalespells.deployment_blocked"),true);return false;}
        unit.setYaw(player.getYaw());unit.setBodyYaw(player.getYaw());
        if(unit instanceof RoyaleUnit deployed && deployed.building())deployed.lockFacing(player.getYaw());
        if(mirror)empower(unit,1.1f);
        if(!player.isCreative())state.elixir-=cost;
        state.cooldown=10;if(!mirror){state.lastTroop=card;state.last=null;}
        player.swingHand(player.getActiveHand(),true);return true;
    }
    public static boolean cast(ServerPlayerEntity player,Spell requested) {
        State state=PLAYERS.computeIfAbsent(player.getUuid(),id->new State());
        if(state.cooldown>0 || player.hasStatusEffect(RoyaleSpells.STUN))return false;
        Spell spell=requested;
        int cost=spell.cost;
        if(spell==Spell.MIRROR) {
            if(state.lastTroop!=null)return deploy(player,state.lastTroop,true);
            if(state.last==null) {player.sendMessage(Text.translatable("message.royalespells.no_mirror"),true);return false;}
            spell=state.last;cost=spell.cost+1;
        }
        ServerWorld world=player.getServerWorld();
        if(requested==Spell.BARBARIAN_BARREL_HERO && player.isSneaking()) {
            AllyZombie hero=world.getEntitiesByClass(AllyZombie.class,player.getBoundingBox().expand(40),
                e->e.hero && player.getUuid().equals(e.ownerId()) && e.isAlive()).stream().findFirst().orElse(null);
            if(hero==null || hero.nextReroll>world.getTime()) {player.sendMessage(Text.translatable("message.royalespells.no_hero"),true);return false;}
            if(!player.isCreative() && state.elixir<1)return insufficient(player);
            Vec3d dir=horizontal(player.getRotationVec(1));
            SpellEntity effect=SpellEntity.create(world,spell,player.getUuid(),hero.getPos(),hero.getPos().add(dir.multiply(3)));
            effect.rerollId=hero.getUuid();effect.reroll=true;
            if(!world.spawnEntity(effect))return false;
            hero.nextReroll=world.getTime()+200;
            if(!player.isCreative())state.elixir-=1;state.cooldown=10;return true;
        }
        if(!player.isCreative() && state.elixir+0.00001<cost)return insufficient(player);
        int active=0;for(Entity e:world.iterateEntities())if(e instanceof SpellEntity)active++;
        if(active>=256) {player.sendMessage(Text.translatable("message.royalespells.limit"),true);return false;}
        Vec3d target=aim(player,32);
        Vec3d start=player.getEyePos().subtract(0,0.25,0);
        if(spell.rolling()) {
            start=player.getPos().add(horizontal(player.getRotationVec(1)).multiply(1.1));
            target=start.add(horizontal(player.getRotationVec(1)).multiply(spell==Spell.THE_LOG?10:5));
        }
        SpellEntity effect=SpellEntity.create(world,spell,player.getUuid(),start,target);
        if(requested==Spell.MIRROR)effect.setPower(1.1f);
        if(!world.spawnEntity(effect))return false;
        if(spell==Spell.GOBLIN_BARREL_EVOLUTION) {
            Vec3d side=horizontal(player.getRotationVec(1)).crossProduct(new Vec3d(0,1,0)).multiply(5);
            Vec3d decoyTarget=ground(world,target.add(side));
            SpellEntity decoy=SpellEntity.create(world,spell,player.getUuid(),start,decoyTarget);decoy.setPower(effect.power());decoy.decoy=true;world.spawnEntity(decoy);
        }
        if(!player.isCreative()) state.elixir-=cost;
        state.cooldown=10;if(requested!=Spell.MIRROR){state.last=spell;state.lastTroop=null;}
        player.swingHand(player.getActiveHand(),true);
        return true;
    }
    private static boolean insufficient(ServerPlayerEntity p){p.sendMessage(Text.translatable("message.royalespells.no_elixir"),true);return false;}
    public static Vec3d aim(PlayerEntity p,double range) {
        Vec3d eye=p.getEyePos(),end=eye.add(p.getRotationVec(1).multiply(range));
        HitResult block=p.raycast(range,1,false);
        Vec3d hit=block.getType()==HitResult.Type.MISS?end:block.getPos();
        double distance=eye.squaredDistanceTo(hit);
        var entityHit=net.minecraft.entity.projectile.ProjectileUtil.raycast(p,eye,end,p.getBoundingBox().stretch(end.subtract(eye)).expand(1),
            e->e instanceof LivingEntity && !e.isSpectator() && e.isAlive(),distance);
        if(entityHit!=null)hit=entityHit.getEntity().getPos();
        else if(block.getType()==HitResult.Type.BLOCK)hit=hit.add(0,0.07,0);
        else hit=ground(p.getWorld(),hit);
        return hit;
    }
    public static Vec3d horizontal(Vec3d v){Vec3d h=new Vec3d(v.x,0,v.z);return h.lengthSquared()<0.001?new Vec3d(0,0,1):h.normalize();}
    public static Vec3d ground(net.minecraft.world.World world,Vec3d pos) {
        Vec3d from=pos.add(0,2,0),to=pos.subtract(0,12,0);
        for(int y=MathHelper.floor(from.y);y>=MathHelper.floor(to.y);y--) {
            BlockPos block=new BlockPos(MathHelper.floor(pos.x),y,MathHelper.floor(pos.z));
            var shape=world.getBlockState(block).getCollisionShape(world,block);
            if(shape.isEmpty())continue;
            var hit=shape.raycast(from,to,block);
            if(hit!=null)return hit.getPos().add(0,0.05,0);
        }
        return pos;
    }
    public static boolean friendly(UUID owner,LivingEntity entity) {
        if(owner==null)return false;
        if(owner.equals(entity.getUuid()))return true;
        if(entity instanceof Summoned s && owner.equals(s.ownerId()))return true;
        if(entity instanceof TameableEntity t && owner.equals(t.getOwnerUuid()))return true;
        if(entity.getWorld() instanceof ServerWorld world) {
            Entity caster=world.getEntity(owner);
            if(caster==null && world.getServer()!=null)caster=world.getServer().getPlayerManager().getPlayer(owner);
            if(caster!=null && caster.isTeammate(entity))return true;
            if(entity instanceof Summoned s && s.ownerId()!=null) {
                Entity other=world.getEntity(s.ownerId());
                if(caster!=null && other!=null && caster.isTeammate(other))return true;
            }
        }
        return false;
    }
    public static boolean enemy(UUID owner,LivingEntity entity) {
        return entity.isAlive() && !entity.isSpectator() && !(entity instanceof PlayerEntity p && p.isCreative()) && !friendly(owner,entity);
    }
    public static List<LivingEntity> targets(ServerWorld world,UUID owner,Vec3d center,double radius,boolean allies) {
        return world.getEntitiesByClass(LivingEntity.class,new Box(center.add(-radius,-3,-radius),center.add(radius,5,radius)),
            e->e.isAlive() && !e.isSpectator() && (allies?friendly(owner,e):enemy(owner,e)) &&
                MathHelper.square(e.getX()-center.x)+MathHelper.square(e.getZ()-center.z)<=radius*radius);
    }
    public static void hit(ServerWorld world,UUID owner,LivingEntity target,float damage) {
        if(!enemy(owner,target))return;
        Entity caster=owner==null?null:world.getEntity(owner);
        if(target instanceof PlayerEntity && !world.getServer().isPvpEnabled())return;
        target.timeUntilRegen=0; target.damage(world.getDamageSources().indirectMagic(caster,caster),damage);
    }
    public static void stun(LivingEntity target,int ticks) {
        target.addStatusEffect(new StatusEffectInstance(RoyaleSpells.STUN,ticks,0,false,false,true));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,ticks,9,false,false));
        target.setVelocity(Vec3d.ZERO);target.velocityModified=true;
        if(target instanceof MobEntity mob)mob.getNavigation().stop();
    }
    public static void curse(UUID owner,LivingEntity target){curse(owner,target,1);}
    public static void curse(UUID owner,LivingEntity target,float power){CURSES.put(target.getUuid(),new Curse(owner,clock+24,power));}
    public static void onDeath(LivingEntity entity) {
        Curse curse=CURSES.remove(entity.getUuid());
        if(curse!=null && curse.until>=clock && entity.getWorld() instanceof ServerWorld world)
            empower(summon(world,curse.owner,entity.getPos(),"zombie",false),curse.power);
    }
    public static MobEntity summon(ServerWorld world,UUID owner,Vec3d pos,String kind,boolean decoy) {
        int owned=0,total=0;
        for(Entity entity:world.iterateEntities())if(entity instanceof Summoned s){total++;if(Objects.equals(owner,s.ownerId()))owned++;}
        if(owned>=MAX_UNITS_PER_OWNER || total>=256)return null;
        MobEntity mob=switch(kind) {
            case "skeleton" -> RoyaleSpells.SKELETON.create(world);
            case "barbarian", "hero" -> RoyaleSpells.BARBARIAN.create(world);
            case "recruit" -> RoyaleSpells.RECRUIT.create(world);
            case "barbarian_hut" -> RoyaleSpells.BARBARIAN_HUT.create(world);
            case "zombie" -> RoyaleSpells.ZOMBIE.create(world);
            default -> null;
        };
        if(mob==null)return null;
        if(mob instanceof RoyaleUnit unit) {
            unit.setup(owner,600,false);
            unit.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(65);unit.setHealth(65);
            unit.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(0);
            unit.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0);
            unit.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1);
            unit.refreshPositionAndAngles(pos.x,pos.y,pos.z,world.random.nextFloat()*360,0);
            if(unit.building())unit.lockFacing(unit.getYaw());
            if(!world.isSpaceEmpty(unit))return null;
            return world.spawnEntity(unit)?unit:null;
        }
        ((Summoned)mob).setup(owner,kind.equals("skeleton")?400:600,false);
        boolean zombie=kind.equals("zombie");
        if(mob instanceof AllyZombie z){z.setBaby(zombie);z.hero=kind.equals("hero");}
        double hp=kind.equals("skeleton")?6:zombie?(decoy?3:10):kind.equals("recruit")?24:20;
        mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(hp);mob.setHealth((float)hp);
        mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(zombie?0.23:0.27);
        mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(decoy?2:kind.equals("skeleton")?1.5:5);
        mob.equipStack(EquipmentSlot.MAINHAND,new ItemStack(kind.equals("skeleton")?Items.STONE_SWORD:zombie?Items.WOODEN_SWORD:Items.IRON_SWORD));
        if(kind.equals("recruit")) {
            mob.equipStack(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));
            mob.equipStack(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION,600,1));
        }
        for(EquipmentSlot slot:EquipmentSlot.values())mob.setEquipmentDropChance(slot,0);
        mob.refreshPositionAndAngles(pos.x,pos.y,pos.z,world.random.nextFloat()*360,0);
        for(int i=0;i<4 && !world.isSpaceEmpty(mob);i++)mob.setPosition(mob.getPos().add(0,1,0));
        if(!world.isSpaceEmpty(mob))return null;
        return world.spawnEntity(mob)?mob:null;
    }
    public static void unitTick(MobEntity mob,UUID owner) {
        if(!(mob.getWorld() instanceof ServerWorld world) || mob.age%10!=0)return;
        LivingEntity target=mob.getTarget();
        if(target==null || !enemy(owner,target) || mob.squaredDistanceTo(target)>24*24) {
            target=targets(world,owner,mob.getPos(),16,false).stream()
                .filter(e->mob.canSee(e)).min(Comparator.comparingDouble(mob::squaredDistanceTo)).orElse(null);
            mob.setTarget(target);
        }
        if(target==null && owner!=null && world.getEntity(owner) instanceof LivingEntity master && mob.squaredDistanceTo(master)>36)
            mob.getNavigation().startMovingTo(master,1.1);
    }
    public static void cloneAllies(ServerWorld world,UUID owner,Vec3d pos,double radius) {
        cloneAllies(world,owner,pos,radius,1);
    }
    public static void empower(MobEntity mob,float power) {
        if(mob==null || power==1)return;
        if(mob instanceof RoyaleUnit unit)unit.setPower(unit.power()*power);
        var health=mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);health.setBaseValue(health.getBaseValue()*power);mob.setHealth(mob.getMaxHealth());
        var attack=mob.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if(mob instanceof AllySkeleton)attack.setBaseValue(attack.getBaseValue()*power);
        else attack.addPersistentModifier(new net.minecraft.entity.attribute.EntityAttributeModifier(MIRROR_ATTACK,"Mirror level",power-1,net.minecraft.entity.attribute.EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
    }
    public static void cloneAllies(ServerWorld world,UUID owner,Vec3d pos,double radius,float power) {
        for(LivingEntity original:targets(world,owner,pos,radius,true)) {
            if(!(original instanceof Summoned summoned) || summoned.isClone() || original instanceof RoyaleUnit unit && unit.building())continue;
            String kind=original instanceof RoyaleUnit unit?unit.kind():original instanceof AllySkeleton?"skeleton":original.getType()==RoyaleSpells.ZOMBIE?"zombie":
                original.getType()==RoyaleSpells.RECRUIT?"recruit":"barbarian";
            MobEntity clone=summon(world,owner,original.getPos().add(0.7,0,0),kind,false);
            if(clone!=null) {
                ((Summoned)clone).setup(owner,400,true);
                clone.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(1);clone.setHealth(1);
                var attack=clone.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);var source=original.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                attack.setBaseValue(source.getBaseValue()*(clone instanceof AllySkeleton?power:1));
                if(!(clone instanceof AllySkeleton)) {
                    var boost=source.getModifier(MIRROR_ATTACK);double factor=(boost==null?1:1+boost.getValue())*power;
                    if(factor>1)attack.addPersistentModifier(new net.minecraft.entity.attribute.EntityAttributeModifier(MIRROR_ATTACK,"Mirror level",factor-1,net.minecraft.entity.attribute.EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
                }
                clone.removeStatusEffect(StatusEffects.ABSORPTION);
                if(original instanceof RoyaleUnit sourceUnit && clone instanceof RoyaleUnit cloneUnit)cloneUnit.setPower(sourceUnit.power()*power);
            }
        }
    }
}


