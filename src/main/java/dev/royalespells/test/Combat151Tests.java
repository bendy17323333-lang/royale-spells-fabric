package dev.royalespells.test;
import dev.royalespells.*;
import dev.royalespells.army.*;
import dev.royalespells.entity.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import java.util.function.Consumer;

@GameTestHolder("royalespells") @PrefixGameTestTemplate(false)
public class Combat151Tests {
    private Vec3 at(GameTestHelper c){return Vec3.atBottomCenterOf(c.absolutePos(new BlockPos(5,15,5)));}
    private ServerPlayer player(GameTestHelper c){var p=TestPlayers.create(c);p.setPos(at(c));return p;}
    private <T extends Mob>T mob(GameTestHelper c,EntityType<T> type,Vec3 at){var m=type.create(c.getLevel());m.setPos(at);m.setNoAi(true);m.setNoGravity(true);m.setOnGround(true);c.getLevel().addFreshEntity(m);return m;}
    private void cleanup(ServerPlayer p,Entity... entities){for(var e:entities)e.discard();p.server.getPlayerList().remove(p);p.discard();}
    @GameTest(template="empty",batch="151-orders")
    public void ordersOverrideHostilesButDoNotHuntUnprovokedCreatures(GameTestHelper c){
        var p=player(c);var base=at(c);var cow=mob(c,EntityType.COW,base.add(1,0,0));var golem=mob(c,EntityType.IRON_GOLEM,base.add(2,0,0));var zombie=mob(c,EntityType.HUSK,base.add(5,0,0));
        var troop=(Mob)SpellEngine.summon(c.getLevel(),p.getUUID(),base,"barbarian",false);troop.setNoAi(true);troop.tickCount=10;
        SummonOrders.tick(troop,p.getUUID());c.assertTrue(troop.getTarget()==zombie,"A farther hostile wins over nearby peaceful cow and neutral golem");
        SummonOrders.order(p,cow);SummonOrders.tick(troop,p.getUUID());c.assertTrue(troop.getTarget()==cow,"Owner's new attack immediately overrides the existing hostile target");
        p.getPersistentData().remove("RoyaleAttackOrder");troop.setTarget(cow);c.assertTrue(troop.getTarget()==null,"AI cannot reacquire the passive after the order ends");
        zombie.discard();golem.setTarget(p);SummonOrders.tick(troop,p.getUUID());c.assertTrue(troop.getTarget()==golem,"A neutral that attacks the player is now a valid defensive target");
        golem.setTarget(null);SummonOrders.tick(troop,p.getUUID());c.assertTrue(troop.getTarget()==null,"No hunting after hostility ends");
        float hp=cow.getHealth();SpellEngine.hit(c.getLevel(),p.getUUID(),cow,2);c.assertTrue(cow.getHealth()<hp,"Aimed player spells still damage passive mobs");cleanup(p,troop,cow,golem);c.succeed();
    }
    @GameTest(template="empty",batch="151-spell-impact")
    public void spellDamageAndSkeletonAttacksHaveNoIncidentalImpulse(GameTestHelper c){
        var p=player(c);var cow=mob(c,EntityType.COW,at(c).add(1,0,0));var baseline=new Vec3(.03,0,.02);cow.setDeltaMovement(baseline);
        SpellEngine.hit(c.getLevel(),p.getUUID(),cow,2);c.assertTrue(cow.getDeltaMovement().equals(baseline),"Magic damage preserves prior velocity exactly");
        var skeleton=SpellEngine.summon(c.getLevel(),p.getUUID(),at(c),"skeleton",false);skeleton.doHurtTarget(cow);c.assertTrue(cow.getDeltaMovement().equals(baseline),"Weak skeleton sword hit does not push");
        cow.invulnerableTime=0;cow.hurt(c.getLevel().damageSources().mobAttack(skeleton),1);c.assertTrue(cow.getDeltaMovement().equals(baseline),"Any small-skeleton damage source is covered");
        var wild=mob(c,EntityType.HUSK,at(c));cow.invulnerableTime=0;cow.hurt(c.getLevel().damageSources().mobAttack(wild),1);
        c.assertFalse(cow.getDeltaMovement().equals(baseline),"Unrelated vanilla mob attacks retain normal knockback");cleanup(p,cow,skeleton,wild);c.succeed();
    }
    @GameTest(template="empty",batch="151-speed")
    public void skeletonSpeedBoostIsExactlyOnceAcrossSaveAndReload(GameTestHelper c){
        var skeleton=SpellEngine.summon(c.getLevel(),UUID.randomUUID(),at(c),"skeleton",false);
        c.assertTrue(Math.abs(skeleton.getAttributeValue(Attributes.MOVEMENT_SPEED)-.3375)<.00001,"Graveyard .27 base becomes .3375");
        var n=new net.minecraft.nbt.CompoundTag();skeleton.saveWithoutId(n);skeleton.discard();var loaded=RoyaleSpells.SKELETON.create(c.getLevel());loaded.load(n);c.getLevel().addFreshEntity(loaded);SummonOrders.boostSkeleton(loaded);
        c.assertTrue(Math.abs(loaded.getAttributeValue(Attributes.MOVEMENT_SPEED)-.3375)<.00001,"Save and repeated apply do not compound the bonus");loaded.discard();c.succeed();
    }
    private List<ArmySkeleton> neutral(GameTestHelper c){var base=at(c);for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)c.getLevel().setBlockAndUpdate(BlockPos.containing(base).offset(x,-1,z),Blocks.STONE.defaultBlockState());return ArmyFormation.neutral(c.getLevel(),base,0);}
    @GameTest(template="empty",batch="151-displacement-whitelist")
    public void barrelsAndRoyalDeliveryDealDamageWithoutAddingPush(GameTestHelper c){
        var base=at(c);UUID owner=UUID.randomUUID();
        c.getLevel().setBlockAndUpdate(BlockPos.containing(base).below(),Blocks.STONE.defaultBlockState());
        for(var spell:List.of(Spell.BARBARIAN_BARREL,Spell.BARBARIAN_BARREL_HERO,Spell.ROYAL_DELIVERY)){
            var target=mob(c,EntityType.COW,base);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);target.setDeltaMovement(Vec3.ZERO);
            var fx=SpellEntity.create(c.getLevel(),spell,owner,base,base);for(int i=0;i<=spell.duration;i++)fx.tick();
            c.assertTrue(target.getHealth()<100&&target.getDeltaMovement().lengthSqr()<1e-10,"Damage without displaced velocity: "+spell+" hp="+target.getHealth()+" velocity="+target.getDeltaMovement());target.discard();fx.discard();
            for(var e:com.google.common.collect.ImmutableList.copyOf(c.getLevel().getAllEntities()))if(e instanceof Summoned s&&owner.equals(s.ownerId()))e.discard();
        }c.succeed();
    }
    @GameTest(template="empty",batch="151-neutral-egg")
    public void eggDeploysSixteenNonPlayerUnitsInOneFriendlyFaction(GameTestHelper c){
        var p=player(c);for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)c.getLevel().setBlockAndUpdate(BlockPos.containing(at(c)).offset(x,-1,z),Blocks.STONE.defaultBlockState());
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(RoyaleSpells.NEUTRAL_ARMY_EGG));var pos=BlockPos.containing(at(c)).below();
        var result=RoyaleSpells.NEUTRAL_ARMY_EGG.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)));
        var list=c.getLevel().getEntitiesOfClass(ArmySkeleton.class,new AABB(pos).inflate(7));
        c.assertTrue(result.consumesAction()&&list.size()==16,"One actual item use creates 15 members and their general");
        var g=list.stream().filter(ArmySkeleton::general).findFirst().orElseThrow();
        c.assertTrue(list.stream().allMatch(s->s.neutralArmy()&&s.supported()&&CombatCompatibility.resolve(c.getLevel(),s.ownerId())==null&&SpellEngine.friendly(g.ownerId(),s)),"No player owner, one friendly faction with a valid lease");
        c.assertTrue(list.stream().filter(s->!s.general()).mapToDouble(g::distanceTo).min().orElseThrow()<1,"General starts less than one block from nearest member");
        c.assertFalse(ArmyLedger.get(p.server).active(p.getUUID(),ArmyLedger.now(p.server)),"Egg does not occupy player's summon slot");list.forEach(Entity::discard);cleanup(p);c.succeed();
    }
    @GameTest(template="empty",batch="151-ghost-impact")
    public void lethalPlayerSprintHitCreatesGhostWithoutEitherKnockbackImpulse(GameTestHelper c){
        var p=player(c);var list=neutral(c);c.assertTrue(list.size()==16,"Formation available");var s=list.stream().filter(e->!e.general()).findFirst().orElseThrow();
        p.setPos(s.position().add(0,0,-1));p.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(100);p.getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(2);p.setSprinting(true);s.setDeltaMovement(Vec3.ZERO);s.setOnGround(true);
        p.attack(s);c.assertTrue(s.ghost()&&s.isAlive(),"The real sprint attack performs the purple conversion");c.assertTrue(s.getDeltaMovement().lengthSqr()<1e-10,"Neither hurt knockback nor Player.attack's later bonus moves the new ghost");
        s.knockback(2,1,0);c.assertTrue(s.getDeltaMovement().lengthSqr()<1e-10,"Invulnerable ghosts also reject subsequent impulse attempts");list.forEach(Entity::discard);cleanup(p);c.succeed();
    }
    @GameTest(template="empty",batch="151-shield-audio")
    public void originalShieldBreakCueFiresOnlyOnTheBreakingHit(GameTestHelper c){
        var list=neutral(c);var g=list.stream().filter(ArmySkeleton::general).findFirst().orElseThrow();int[] sounds={0};
        Consumer<net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition> listener=e->{if(e.getLevel()==c.getLevel()&&e.getSound()!=null&&e.getSound().value()==ArmySounds.SHIELD_BREAK)sounds[0]++;};
        NeoForge.EVENT_BUS.addListener(listener);try{g.hurt(c.getLevel().damageSources().generic(),1);c.assertTrue(sounds[0]==0,"Shield still intact");g.hurt(c.getLevel().damageSources().generic(),8);g.hurt(c.getLevel().damageSources().generic(),1);c.assertTrue(sounds[0]==1,"Exactly one original shield-loss sound");}finally{NeoForge.EVENT_BUS.unregister(listener);list.forEach(Entity::discard);}c.succeed();
    }
    @GameTest(template="empty",batch="151-lightning-audio")
    public void lightningOneAndThreeVictimsProduceOneAndThreeTimedCues(GameTestHelper c){
        var base=at(c);var targets=new ArrayList<Mob>();var ticks=new ArrayList<Integer>();var fx=new SpellEntity[1];
        Consumer<net.neoforged.neoforge.event.PlayLevelSoundEvent.AtPosition> listener=e->{if(e.getLevel()==c.getLevel()&&e.getSound()!=null&&e.getSound().value().getLocation().equals(RoyaleSpells.id("spell_lightning_deploy")))ticks.add(fx[0].time());};
        NeoForge.EVENT_BUS.addListener(listener);try{for(int count:new int[]{1,3}){
            for(int i=0;i<count;i++){var mob=mob(c,EntityType.COW,base.add(i*.7,0,0));mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);mob.setHealth(100);targets.add(mob);}
            fx[0]=SpellEntity.create(c.getLevel(),Spell.LIGHTNING,UUID.randomUUID(),base,base);for(int i=0;i<24;i++)fx[0].tick();
            c.assertTrue(ticks.equals(count==1?List.of(2):List.of(2,8,14)),"Only actual strike ticks produce sounds: "+ticks);ticks.clear();targets.forEach(Entity::discard);targets.clear();
        }}finally{NeoForge.EVENT_BUS.unregister(listener);targets.forEach(Entity::discard);if(fx[0]!=null)fx[0].discard();}c.succeed();
    }
}
