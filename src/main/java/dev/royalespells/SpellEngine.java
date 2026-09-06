package dev.royalespells;

import dev.royalespells.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class SpellEngine {
    private SpellEngine(){}
    public static final int MAX_UNITS_PER_OWNER=64;
    private static final net.minecraft.resources.ResourceLocation MIRROR_ATTACK=RoyaleSpells.id("mirror_attack");
    private static final Map<UUID,State> PLAYERS=new HashMap<>();
    private static final Map<UUID,Curse> CURSES=new HashMap<>();
    private static long clock;
    private static class State { double elixir=10; Spell last; TroopCard lastTroop; int cooldown; }
    private record Curse(UUID owner,long until,float power){}
    public static void clear(){PLAYERS.clear();CURSES.clear();clock=0;}
    public static void refill(ServerPlayer player){PLAYERS.computeIfAbsent(player.getUUID(),id->new State()).elixir=10;}
    public static void resetForRecording(ServerPlayer player){if(ShowcaseMap.enabled(player.serverLevel()))PLAYERS.remove(player.getUUID());}
    public static void tick(MinecraftServer server) {
        clock++;
        for(ServerPlayer p:server.getPlayerList().getPlayers()) {
            State s=PLAYERS.computeIfAbsent(p.getUUID(),id->new State());
            s.elixir=Math.min(10,s.elixir+0.025); if(s.cooldown>0)s.cooldown--;
            if(clock%10==0 && (isCard(p.getMainHandItem().getItem()) || isCard(p.getOffhandItem().getItem()))) {
                String bar="◆".repeat((int)s.elixir)+"◇".repeat(10-(int)s.elixir);
                p.displayClientMessage(Component.translatable("message.royalespells.elixir",bar,String.format(java.util.Locale.ROOT,"%.1f",s.elixir)),true);
            }
        }
        if(clock%100==0) {
            CURSES.entrySet().removeIf(e->e.getValue().until<clock);
            PLAYERS.keySet().removeIf(id->server.getPlayerList().getPlayer(id)==null);
        }
    }
    public static boolean isCard(Item item){return item instanceof SpellItem || item instanceof TroopItem;}
    public static boolean deploy(ServerPlayer player,TroopCard card){return deploy(player,card,false);}
    private static boolean deploy(ServerPlayer player,TroopCard card,boolean mirror) {
        State state=PLAYERS.computeIfAbsent(player.getUUID(),id->new State());int cost=card.cost+(mirror?1:0);
        if(state.cooldown>0 || player.hasEffect(RoyaleSpells.STUN))return false;
        if(!player.isCreative() && state.elixir<cost)return insufficient(player);
        Vec3 at=ground(player.level(),aim(player,32));
        if(!player.serverLevel().getWorldBorder().isWithinBounds(BlockPos.containing(at)) || !player.serverLevel().hasChunkAt(BlockPos.containing(at)) || !player.mayInteract(player.level(),BlockPos.containing(at)))return false;
        var unit=summon(player.serverLevel(),player.getUUID(),at,card.unit,false);
        if(unit==null){player.displayClientMessage(Component.translatable("message.royalespells.deployment_blocked"),true);return false;}
        unit.setYRot(player.getYRot());unit.setYBodyRot(player.getYRot());
        if(unit instanceof RoyaleUnit deployed && deployed.building())deployed.lockFacing(player.getYRot());
        if(mirror)empower(unit,1.1f);
        if(!player.isCreative())state.elixir-=cost;
        state.cooldown=10;if(!mirror){state.lastTroop=card;state.last=null;}
        player.swing(player.getUsedItemHand(),true);return true;
    }
    public static boolean cast(ServerPlayer player,Spell requested) {
        State state=PLAYERS.computeIfAbsent(player.getUUID(),id->new State());
        if(state.cooldown>0 || player.hasEffect(RoyaleSpells.STUN))return false;
        Spell spell=requested;
        int cost=spell.cost;
        if(spell==Spell.MIRROR) {
            if(state.lastTroop!=null)return deploy(player,state.lastTroop,true);
            if(state.last==null) {player.displayClientMessage(Component.translatable("message.royalespells.no_mirror"),true);return false;}
            spell=state.last;cost=spell.cost+1;
        }
        ServerLevel world=player.serverLevel();
        if(requested==Spell.BARBARIAN_BARREL_HERO && player.isShiftKeyDown()) {
            AllyZombie hero=world.getEntitiesOfClass(AllyZombie.class,player.getBoundingBox().inflate(40),
                e->e.hero && player.getUUID().equals(e.ownerId()) && e.isAlive()).stream().findFirst().orElse(null);
            if(hero==null || hero.nextReroll>world.getGameTime()) {player.displayClientMessage(Component.translatable("message.royalespells.no_hero"),true);return false;}
            if(!player.isCreative() && state.elixir<1)return insufficient(player);
            Vec3 dir=horizontal(player.getViewVector(1));
            SpellEntity effect=SpellEntity.create(world,spell,player.getUUID(),hero.position(),hero.position().add(dir.scale(3)));
            effect.rerollId=hero.getUUID();effect.reroll=true;
            if(!world.addFreshEntity(effect))return false;
            hero.nextReroll=world.getGameTime()+200;
            if(!player.isCreative())state.elixir-=1;state.cooldown=10;return true;
        }
        if(!player.isCreative() && state.elixir+0.00001<cost)return insufficient(player);
        int active=0;for(Entity e:world.getAllEntities())if(e instanceof SpellEntity)active++;
        if(active>=256) {player.displayClientMessage(Component.translatable("message.royalespells.limit"),true);return false;}
        Vec3 target=aim(player,32);
        Vec3 start=player.getEyePosition().subtract(0,0.25,0);
        if(spell.rolling()) {
            start=player.position().add(horizontal(player.getViewVector(1)).scale(1.1));
            target=start.add(horizontal(player.getViewVector(1)).scale(spell==Spell.THE_LOG?10:5));
        }
        SpellEntity effect=SpellEntity.create(world,spell,player.getUUID(),start,target);
        if(requested==Spell.MIRROR)effect.setPower(1.1f);
        if(!world.addFreshEntity(effect))return false;
        if(spell==Spell.GOBLIN_BARREL_EVOLUTION) {
            Vec3 side=horizontal(player.getViewVector(1)).cross(new Vec3(0,1,0)).scale(5);
            Vec3 decoyTarget=ground(world,target.add(side));
            SpellEntity decoy=SpellEntity.create(world,spell,player.getUUID(),start,decoyTarget);decoy.setPower(effect.power());decoy.decoy=true;world.addFreshEntity(decoy);
        }
        if(!player.isCreative()) state.elixir-=cost;
        state.cooldown=10;if(requested!=Spell.MIRROR){state.last=spell;state.lastTroop=null;}
        player.swing(player.getUsedItemHand(),true);
        return true;
    }
    private static boolean insufficient(ServerPlayer p){p.displayClientMessage(Component.translatable("message.royalespells.no_elixir"),true);return false;}
    public static Vec3 aim(Player p,double range) {
        Vec3 eye=p.getEyePosition(),end=eye.add(p.getViewVector(1).scale(range));
        HitResult block=p.pick(range,1,false);
        Vec3 hit=block.getType()==HitResult.Type.MISS?end:block.getLocation();
        double distance=eye.distanceToSqr(hit);
        var entityHit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(p,eye,end,p.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1),
            e->e instanceof LivingEntity && !e.isSpectator() && e.isAlive(),distance);
        if(entityHit!=null)hit=entityHit.getEntity().position();
        else if(block.getType()==HitResult.Type.BLOCK)hit=hit.add(0,0.07,0);
        else hit=ground(p.level(),hit);
        return hit;
    }
    public static Vec3 horizontal(Vec3 v){Vec3 h=new Vec3(v.x,0,v.z);return h.lengthSqr()<0.001?new Vec3(0,0,1):h.normalize();}
    public static Vec3 ground(net.minecraft.world.level.Level world,Vec3 pos) {
        Vec3 from=pos.add(0,2,0),to=pos.subtract(0,12,0);
        for(int y=Mth.floor(from.y);y>=Mth.floor(to.y);y--) {
            BlockPos block=new BlockPos(Mth.floor(pos.x),y,Mth.floor(pos.z));
            var shape=world.getBlockState(block).getCollisionShape(world,block);
            if(shape.isEmpty())continue;
            var hit=shape.clip(from,to,block);
            if(hit!=null)return hit.getLocation().add(0,0.05,0);
        }
        return pos;
    }
    public static boolean friendly(UUID owner,LivingEntity entity) {
        if(owner==null)return false;
        if(owner.equals(entity.getUUID()))return true;
        UUID otherOwner=CombatCompatibility.ownerOf(entity);
        if(owner.equals(otherOwner))return true;
        if(entity.level() instanceof ServerLevel world) {
            Entity caster=CombatCompatibility.resolve(world,owner);
            if(caster!=null && caster.isAlliedTo(entity))return true;
            Entity other=CombatCompatibility.resolve(world,otherOwner);
            if(caster!=null && other!=null && caster.isAlliedTo(other))return true;
        }
        return false;
    }
    public static boolean enemy(UUID owner,LivingEntity entity) {
        return entity.isAlive() && !entity.isSpectator() && !(entity instanceof Player p && p.isCreative()) && !friendly(owner,entity);
    }
    public static List<LivingEntity> targets(ServerLevel world,UUID owner,Vec3 center,double radius,boolean allies) {
        return world.getEntitiesOfClass(LivingEntity.class,new AABB(center.add(-radius,-3,-radius),center.add(radius,5,radius)),
            e->e.isAlive() && !e.isSpectator() && (allies?friendly(owner,e):enemy(owner,e)) &&
                Mth.square(e.getX()-center.x)+Mth.square(e.getZ()-center.z)<=radius*radius);
    }
    public static void hit(ServerLevel world,UUID owner,LivingEntity target,float damage) {
        if(!enemy(owner,target))return;
        Entity caster=owner==null?null:world.getEntity(owner);
        if(target instanceof Player && !world.getServer().isPvpAllowed())return;
        target.invulnerableTime=0; target.hurt(world.damageSources().indirectMagic(caster,caster),damage);
    }
    public static void stun(LivingEntity target,int ticks) {
        SpellMotion.cancel(target);
        target.addEffect(new MobEffectInstance(RoyaleSpells.STUN,ticks,0,false,false,true));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,ticks,9,false,false));
        target.setDeltaMovement(Vec3.ZERO);target.hurtMarked=true;
        if(target instanceof Mob mob)mob.getNavigation().stop();
    }
    public static void curse(UUID owner,LivingEntity target){curse(owner,target,1);}
    public static void curse(UUID owner,LivingEntity target,float power){CURSES.put(target.getUUID(),new Curse(owner,clock+24,power));}
    public static void onDeath(LivingEntity entity) {
        Curse curse=CURSES.remove(entity.getUUID());
        if(curse!=null && curse.until>=clock && entity.level() instanceof ServerLevel world)
            empower(summon(world,curse.owner,entity.position(),"zombie",false),curse.power);
    }
    public static Mob summon(ServerLevel world,UUID owner,Vec3 pos,String kind,boolean decoy) {
        int owned=0,total=0;
        for(Entity entity:world.getAllEntities())if(entity instanceof Summoned s){total++;if(Objects.equals(owner,s.ownerId()))owned++;}
        if(owned>=MAX_UNITS_PER_OWNER || total>=256)return null;
        Mob mob=switch(kind) {
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
            unit.getAttribute(Attributes.MAX_HEALTH).setBaseValue(65);unit.setHealth(65);
            unit.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0);
            unit.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0);
            unit.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            unit.moveTo(pos.x,pos.y,pos.z,world.random.nextFloat()*360,0);
            if(unit.building())unit.lockFacing(unit.getYRot());
            if(!world.noCollision(unit))return null;
            return world.addFreshEntity(unit)?unit:null;
        }
        ((Summoned)mob).setup(owner,kind.equals("skeleton")?400:600,false);
        boolean zombie=kind.equals("zombie");
        if(mob instanceof AllyZombie z){z.setBaby(zombie);z.hero=kind.equals("hero");}
        double hp=kind.equals("skeleton")?6:zombie?(decoy?3:10):kind.equals("recruit")?24:20;
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);mob.setHealth((float)hp);
        mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(zombie?0.23:0.27);
        mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(decoy?2:kind.equals("skeleton")?1.5:5);
        mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(kind.equals("skeleton")?Items.STONE_SWORD:zombie?Items.WOODEN_SWORD:Items.IRON_SWORD));
        if(kind.equals("recruit")) {
            mob.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));
            mob.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));
            mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,600,1));
        }
        for(EquipmentSlot slot:EquipmentSlot.values())mob.setDropChance(slot,0);
        mob.moveTo(pos.x,pos.y,pos.z,world.random.nextFloat()*360,0);
        for(int i=0;i<4 && !world.noCollision(mob);i++)mob.setPos(mob.position().add(0,1,0));
        if(!world.noCollision(mob))return null;
        return world.addFreshEntity(mob)?mob:null;
    }
    public static void unitTick(Mob mob,UUID owner) {
        if(!(mob.level() instanceof ServerLevel world) || mob.tickCount%10!=0)return;
        LivingEntity target=mob.getTarget();
        if(target==null || !enemy(owner,target) || mob.distanceToSqr(target)>24*24) {
            target=targets(world,owner,mob.position(),16,false).stream()
                .filter(e->mob.hasLineOfSight(e)).min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
            mob.setTarget(target);
        }
        if(target==null && owner!=null && world.getEntity(owner) instanceof LivingEntity master && mob.distanceToSqr(master)>36)
            mob.getNavigation().moveTo(master,1.1);
    }
    public static void cloneAllies(ServerLevel world,UUID owner,Vec3 pos,double radius) {
        cloneAllies(world,owner,pos,radius,1);
    }
    public static void empower(Mob mob,float power) {
        if(mob==null || power==1)return;
        if(mob instanceof RoyaleUnit unit)unit.setPower(unit.power()*power);
        var health=mob.getAttribute(Attributes.MAX_HEALTH);health.setBaseValue(health.getBaseValue()*power);mob.setHealth(mob.getMaxHealth());
        var attack=mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if(mob instanceof AllySkeleton)attack.setBaseValue(attack.getBaseValue()*power);
        else attack.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(MIRROR_ATTACK,power-1,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
    public static void cloneAllies(ServerLevel world,UUID owner,Vec3 pos,double radius,float power) {
        for(LivingEntity original:targets(world,owner,pos,radius,true)) {
            if(!(original instanceof Summoned summoned) || summoned.isClone() || original instanceof RoyaleUnit unit && unit.building())continue;
            String kind=original instanceof RoyaleUnit unit?unit.kind():original instanceof AllySkeleton?"skeleton":original.getType()==RoyaleSpells.ZOMBIE?"zombie":
                original.getType()==RoyaleSpells.RECRUIT?"recruit":"barbarian";
            Mob clone=summon(world,owner,original.position().add(0.7,0,0),kind,false);
            if(clone!=null) {
                ((Summoned)clone).setup(owner,400,true);
                clone.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1);clone.setHealth(1);
                var attack=clone.getAttribute(Attributes.ATTACK_DAMAGE);var source=original.getAttribute(Attributes.ATTACK_DAMAGE);
                attack.setBaseValue(source.getBaseValue()*(clone instanceof AllySkeleton?power:1));
                if(!(clone instanceof AllySkeleton)) {
                    var boost=source.getModifier(MIRROR_ATTACK);double factor=(boost==null?1:1+boost.amount())*power;
                    if(factor>1)attack.addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(MIRROR_ATTACK,factor-1,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                }
                clone.removeEffect(MobEffects.ABSORPTION);
                if(original instanceof RoyaleUnit sourceUnit && clone instanceof RoyaleUnit cloneUnit)cloneUnit.setPower(sourceUnit.power()*power);
            }
        }
    }
}


