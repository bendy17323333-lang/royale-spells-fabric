// SPDX-License-Identifier: MIT
package dev.royalespells.test;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Behavioral tests use real entities, ownership, death callbacks, projectiles
 * and the card/Mirror path. No renderer or GUI overlay can satisfy these tests. */
public final class WitchTests implements FabricGameTest {
    private Vec3 floor(GameTestHelper h){BlockPos p=h.absolutePos(new BlockPos(10,14,10));
        for(int x=-9;x<=9;x++)for(int z=-9;z<=9;z++)for(int y=-1;y<8;y++)h.getLevel().setBlockAndUpdate(p.offset(x,y,z),(y<0?Blocks.STONE:Blocks.AIR).defaultBlockState());return Vec3.atBottomCenterOf(p);}
    private RoyalWitch witch(GameTestHelper h,Vec3 at,UUID owner,boolean evolved,int level){
        var w=RoyaleSpells.WITCH.create(h.getLevel());w.configure(owner,evolved,level);w.setNoAi(true);w.setNoGravity(true);w.setPos(at);h.getLevel().addFreshEntity(w);return w;
    }
    private Mob dummy(GameTestHelper h,Vec3 at){var e=EntityType.COW.create(h.getLevel());e.setPos(at);e.setNoAi(true);e.setNoGravity(true);e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);e.setHealth(500);h.getLevel().addFreshEntity(e);return e;}
    private void near(GameTestHelper h,double a,double b,String why){h.assertTrue(Math.abs(a-b)<.004,why+" actual="+a+", expected="+b);}
    private void tick(Entity e,int count){for(int i=0;i<count&&!e.isRemoved();i++)e.tick();}
    private List<AllySkeleton> skeletons(GameTestHelper h,RoyalWitch w){return h.getLevel().getEntitiesOfClass(AllySkeleton.class,w.getBoundingBox().inflate(25),s->Objects.equals(w.ownerId(),s.ownerId()));}
    private List<WitchBolt> souls(GameTestHelper h,RoyalWitch w){return h.getLevel().getEntitiesOfClass(WitchBolt.class,w.getBoundingBox().inflate(30),s->s.kind()==WitchBolt.HEAL_SOUL&&w.getUUID().equals(s.recipient())&&!s.impacted());}
    private void cleanup(GameTestHelper h,UUID owner){for(var e:com.google.common.collect.ImmutableList.copyOf(h.getLevel().getAllEntities()))if(e instanceof Summoned s&&Objects.equals(owner,s.ownerId())||e instanceof WitchBolt)e.discard();}

    @GameTest(template="royalespells:empty",batch="witch-splash",timeoutTicks=160)
    public void actualProjectileDealsExactSplashAndNeverHurtsAlliesOrKnocksBack(GameTestHelper h){
        Vec3 at=floor(h);StandaloneTests.ready(h,at,()->{UUID owner=UUID.randomUUID();var w=witch(h,at,owner,false,11);
        Mob target=dummy(h,at.add(0,0,4)),inside=dummy(h,at.add(1.49,0,4)),outside=dummy(h,at.add(1.51,0,4));
        var friend=RoyaleSpells.SKELETON.create(h.getLevel());friend.setup(owner,400,false);friend.setPos(at.add(-.7,0,4));friend.setNoAi(true);h.getLevel().addFreshEntity(friend);float friendlyHealth=friend.getHealth();
        var projectile=WitchBolt.launch(h.getLevel(),w,target);h.assertTrue(projectile!=null,"Actual registered projectile spawned");
        // Place inside the primary hit box, making the exact server contact
        // center deterministic for the 1.49/1.51-block radius boundary test.
        projectile.setPos(at.add(0,.8,4));target.invulnerableTime=17;target.setDeltaMovement(Vec3.ZERO);projectile.tick();
        h.assertTrue(projectile.impacted(),"Swept body collision generated a real impact");near(h,target.getHealth(),495,"Primary gets 135 / 27 damage");near(h,inside.getHealth(),495,"1.49 blocks is inside the splash");near(h,outside.getHealth(),500,"1.51 blocks is outside");near(h,friend.getHealth(),friendlyHealth,"Friendly summon excluded");
        near(h,target.getDeltaMovement().lengthSqr(),0,"Magic impact never adds incidental knockback");h.assertTrue(target.invulnerableTime>=17,"Preserves unrelated attacker hurt immunity");
        tick(projectile,15);near(h,target.getHealth(),495,"Sixteen visible impact ticks deal damage only once");cleanup(h,owner);target.discard();inside.discard();outside.discard();h.succeed();
        });
    }
    @GameTest(template="royalespells:empty",batch="witch-ballistics",timeoutTicks=160)
    public void SpellBallTravelsToTargetAndStopsAtSolidTerrain(GameTestHelper h){
        Vec3 at=floor(h);StandaloneTests.ready(h,at,()->{UUID owner=UUID.randomUUID();var w=witch(h,at,owner,false,11);var target=dummy(h,at.add(0,0,5));
        target.setPos(at.add(5.5,4,0));h.assertTrue(w.inAttackRange(target),"Original 5.5-block arena-plane range reaches flying units");
        target.setPos(at.add(5.51,4,0));h.assertFalse(w.inAttackRange(target),"5.51 horizontal blocks is out of range");target.setPos(at.add(0,0,5));
        var p=WitchBolt.launch(h.getLevel(),w,target);p.tick();h.assertFalse(p.impacted(),"Casting does not instantly damage a remote target");near(h,target.getHealth(),500,"Target unharmed while projectile travels");
        tick(p,14);h.assertTrue(p.impacted()&&target.getHealth()<500,"Homing swept projectile reaches the target");
        target.setHealth(500);for(int x=-2;x<=2;x++)for(int y=0;y<4;y++)h.getLevel().setBlockAndUpdate(BlockPos.containing(at).offset(x,y,2),Blocks.STONE.defaultBlockState());
        var blocked=WitchBolt.launch(h.getLevel(),w,target);tick(blocked,6);h.assertTrue(blocked.impacted()&&blocked.position().z<at.z+3,"Collision pins impact to the actual wall surface");near(h,target.getHealth(),500,"No remote splash through solid wall");cleanup(h,owner);target.discard();h.succeed();
        });
    }
    @GameTest(template="royalespells:empty",batch="witch-summoning",timeoutTicks=160)
    public void firstWaveArrivesAtOneSecondThenFourSkeletonsEverySevenSeconds(GameTestHelper h){
        Vec3 at=floor(h);StandaloneTests.ready(h,at,()->{UUID owner=UUID.randomUUID();var w=witch(h,at,owner,true,12);
        tick(w,19);h.assertTrue(skeletons(h,w).isEmpty(),"No premature summons during deployment");w.tick();
        h.assertTrue(w.waveCount()==1&&skeletons(h,w).size()==4&&w.eligibleSkeletons().size()==4,"Exactly four first-wave eligible skeletons");
        for(var s:skeletons(h,w)){near(h,s.getMaxHealth(),89/27.0,"Mirrored skeletons have level-12 health");near(h,s.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue(),89/27.0,"Mirrored skeleton attack profile");h.assertTrue(owner.equals(s.ownerId()),"Summons inherit player's ownership");s.setPos(s.position().add(5,0,0));s.setNoAi(true);}
        tick(w,139);h.assertTrue(w.waveCount()==1,"No second wave before full seven seconds");w.tick();h.assertTrue(w.waveCount()==2&&skeletons(h,w).size()==8,"Four more skeletons at seven-second interval");h.assertTrue(w.eligibleSkeletons().size()==4,"Later wave never refills healing eligibility");cleanup(h,owner);h.succeed();
        });
    }
    @GameTest(template="royalespells:empty",batch="witch-evolution",timeoutTicks=160)
    public void onlyFourOriginalFirstWaveDeathsHealAfterPhysicalSoulArrival(GameTestHelper h){
        Vec3 at=floor(h);StandaloneTests.ready(h,at,()->{UUID owner=UUID.randomUUID();var w=witch(h,at,owner,true,11);tick(w,20);var first=List.copyOf(skeletons(h,w));
        h.assertFalse(w.acceptSoul(UUID.randomUUID(),UUID.randomUUID()),"Forged soul ids rejected");
        for(var s:first){s.setNoAi(true);s.hurt(h.getLevel().damageSources().genericKill(),1000);RoyalWitch.onSkeletonDeath(s);}
        h.assertTrue(w.pendingSoulCount()==4&&souls(h,w).size()==4,"Each genuine death yields only one soul despite repeated callbacks");near(h,w.getHealth(),839/27.0,"Death alone does not heal");
        var orbs=List.copyOf(souls(h,w));for(var orb:orbs)tick(orb,19);near(h,w.getHealth(),839/27.0,"Still unhealed before a one-second flight");
        for(var orb:orbs)orb.tick();near(h,w.getHealth(),1451/27.0,"Four arrivals reach the published maximum health");near(h,w.getMaxHealth(),1451/27.0,"Overheal is real, not just a displayed number");h.assertTrue(w.healedSouls()==4&&w.pendingSoulCount()==0,"All four one-time authorizations consumed");
        for(var orb:orbs)h.assertFalse(w.acceptSoul(orb.skeletonId(),orb.getUUID()),"Arrival cannot be replayed");
        w.summonWave();for(var s:skeletons(h,w))if(s.isAlive()){s.hurt(h.getLevel().damageSources().genericKill(),1000);RoyalWitch.onSkeletonDeath(s);}
        h.assertTrue(souls(h,w).isEmpty(),"Second wave cannot return healing souls");cleanup(h,owner);h.succeed();
        });
    }
    @GameTest(template="royalespells:empty",batch="witch-save",timeoutTicks=160)
    public void firstWaveSoulLedgerAndInFlightSoulSurviveSaveWithoutDuplication(GameTestHelper h){
        Vec3 at=floor(h);StandaloneTests.ready(h,at,()->{UUID owner=UUID.randomUUID();var w=witch(h,at,owner,true,12);tick(w,20);var child=skeletons(h,w).get(0);child.hurt(h.getLevel().damageSources().genericKill(),1000);var orb=souls(h,w).get(0);tick(orb,9);
        CompoundTag witchTag=new CompoundTag(),orbTag=new CompoundTag();w.saveWithoutId(witchTag);orb.saveWithoutId(orbTag);var copy=RoyaleSpells.WITCH.create(h.getLevel());copy.load(witchTag);var orbCopy=RoyaleSpells.WITCH_BOLT.create(h.getLevel());orbCopy.load(orbTag);
        h.assertTrue(copy.pendingSoulCount()==1&&copy.eligibleSkeletons().size()==4&&copy.cardLevel()==12&&copy.ownerId().equals(owner),"Level, owner and exact first-wave ledger survive serialization");h.assertTrue(orbCopy.age()==9&&orbCopy.skeletonId().equals(child.getUUID())&&orbCopy.recipient().equals(w.getUUID()),"In-flight soul preserves elapsed journey and correct mother");
        h.assertFalse(copy.acceptSoul(child.getUUID(),orb.getUUID()),"Saved ledger alone cannot heal before physical arrival");
        w.kill();tick(orb,20);h.assertTrue(orb.isRemoved(),"Soul cannot resurrect a dead mother");cleanup(h,owner);h.succeed();
        });
    }
    @GameTest(template="royalespells:empty",batch="witch-control",timeoutTicks=160)
    public void electricAndFreezePauseWindupAndSummoningWhileSnowSlowsThem(GameTestHelper h){
        Vec3 at=floor(h);StandaloneTests.ready(h,at,()->{UUID owner=UUID.randomUUID();var w=witch(h,at,owner,false,11);tick(w,20);var target=dummy(h,at.add(0,0,4));w.setTarget(target);tick(w,7);
        int attack=w.attackAge(),summon=w.summonClock();h.assertTrue(attack==7,"Windup in progress");
        SpellEngine.electricStun(w,30);tick(w,5);h.assertTrue(w.attackAge()==attack&&w.summonClock()==summon,"Electric stun suspends, not restarts, both clocks");w.removeEffect(RoyaleSpells.ELECTRICAL_STUN);
        w.addEffect(new MobEffectInstance(RoyaleSpells.FROZEN,30));tick(w,5);h.assertTrue(w.attackAge()==attack&&w.summonClock()==summon,"Freeze suspends clocks and attack pose");w.removeEffect(RoyaleSpells.FROZEN);
        w.addEffect(new MobEffectInstance(RoyaleSpells.SNOWBOUND,100));tick(w,10);h.assertTrue(w.attackAge()==13,"Ten snowbound ticks advance six animation/attack steps");
        w.removeEffect(RoyaleSpells.SNOWBOUND);w.tick();h.assertTrue(w.attackAge()==14&&h.getLevel().getEntitiesOfClass(WitchBolt.class,w.getBoundingBox().inflate(10),e->e.kind()==WitchBolt.ATTACK).size()==1,"Resumes the exact paused windup and releases one bolt");cleanup(h,owner);target.discard();h.succeed();
        });
    }
    @GameTest(template="royalespells:empty",batch="witch-clone",timeoutTicks=160)
    public void cloneRetainsEvolutionButCannotManufactureHealingSouls(GameTestHelper h){
        Vec3 at=floor(h);StandaloneTests.ready(h,at,()->{UUID owner=UUID.randomUUID();var original=witch(h,at,owner,true,11);SpellEngine.cloneAllies(h.getLevel(),owner,at,2,1,12);
        var copies=h.getLevel().getEntitiesOfClass(RoyalWitch.class,original.getBoundingBox().inflate(5),w->w.isClone()&&owner.equals(w.ownerId()));h.assertTrue(copies.size()==1,"Creates one witch clone instead of converting to barbarian");var clone=copies.get(0);clone.setNoAi(true);
        h.assertTrue(clone.evolved()&&clone.cardLevel()==12,"Clone preserves evolved visual/type and spell level");near(h,clone.getMaxHealth(),1,"Clone stays one hit");clone.setPos(at.add(5,0,0));tick(clone,20);
        var children=skeletons(h,clone);h.assertTrue(children.size()==4,"Clone summons exactly four children; found="+children.size()+" waves="+clone.waveCount()+" at="+clone.position());h.assertTrue(clone.eligibleSkeletons().isEmpty(),"Clone has no healing authorizations");for(var child:children){h.assertTrue(child.isClone(),"Clone's summons inherit clone state");near(h,child.getHealth(),1,"Cloned offspring stays one hit");child.hurt(h.getLevel().damageSources().genericKill(),1000);}
        h.assertTrue(souls(h,clone).isEmpty(),"Cloned offspring cannot trigger a soul");cleanup(h,owner);h.succeed();
        });
    }
    @GameTest(template="royalespells:empty",batch="witch-card",timeoutTicks=160)
    public void fiveElixirWitchMirrorAndPlayerOrdersUseTheRealCardPath(GameTestHelper h){
        Vec3 at=floor(h);StandaloneTests.ready(h,at,()->{var player=TestPlayers.create(h);player.setGameMode(GameType.SURVIVAL);player.setPos(at.add(0,2,-6));player.setYRot(0);player.setXRot(50);SpellEngine.refill(player);
        h.assertTrue(SpellEngine.deploy(player,TroopCard.WITCH_EVOLUTION),"Evolved witch card deploys");near(h,SpellEngine.elixir(player),5,"Card spends five elixir");
        for(int i=0;i<10;i++)SpellEngine.tick(player.server);SpellEngine.refill(player);player.setYRot(45);h.assertTrue(SpellEngine.cast(player,Spell.MIRROR),"Mirror repeats last witch card");near(h,SpellEngine.elixir(player),4,"Mirror spends six");
        var witches=h.getLevel().getEntitiesOfClass(RoyalWitch.class,player.getBoundingBox().inflate(30),w->player.getUUID().equals(w.ownerId()));h.assertTrue(witches.size()==2&&witches.stream().allMatch(RoyalWitch::evolved),"Both cards have evolved identity");var mirrored=witches.stream().filter(w->w.cardLevel()==12).findFirst().orElseThrow();near(h,mirrored.getHealth(),921/27.0,"Mirror uses level-12 health");near(h,mirrored.projectileDamage(),148/27.0,"Mirror uses exact level-12 damage");
        var peaceful=dummy(h,at.add(3,0,3));h.assertFalse(SummonOrders.allowed(mirrored,player,peaceful),"Never attacks unprovoked peaceful bystander");SummonOrders.order(player,peaceful);h.assertTrue(SummonOrders.priority(mirrored,player)==peaceful&&SummonOrders.allowed(mirrored,player,peaceful),"Player's explicit attack becomes priority");
        cleanup(h,player.getUUID());peaceful.discard();player.server.getPlayerList().remove(player);player.discard();h.succeed();
        });
    }

    @GameTest(template="royalespells:empty",batch="witch-eggs",timeoutTicks=180)
    public void separateEggsSpawnTheirActualVariantAndKeepTheirOwnFaction(GameTestHelper h){
        var at=floor(h);StandaloneTests.ready(h,at,()->{
            var player=TestPlayers.create(h);
            var eggs=List.of(RoyaleSpells.WITCH_EGG,RoyaleSpells.EVOLVED_WITCH_EGG);
            for(int i=0;i<eggs.size();i++){
                var stack=new net.minecraft.world.item.ItemStack(eggs.get(i));
                var expected=i==0?RoyaleSpells.WITCH:RoyaleSpells.EVOLVED_WITCH;
                h.assertTrue(((net.minecraft.world.item.SpawnEggItem)eggs.get(i)).getType(stack.getTag())==expected,"Each egg points to its own entity type");
                var ground=BlockPos.containing(at.add(i*4,0,0)).below();
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
                var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(ground).add(0,.5,0),net.minecraft.core.Direction.UP,ground,false));
                h.assertTrue(eggs.get(i).useOn(context).consumesAction(),"Actual spawn egg interaction succeeds");
                var entities=h.getLevel().getEntitiesOfClass(RoyalWitch.class,new net.minecraft.world.phys.AABB(ground).inflate(3),w->w.getType()==expected);
                h.assertTrue(entities.size()==1,"Exactly one egg actor appears");var w=entities.get(0);w.setNoAi(true);
                h.assertTrue(w.evolved()==(i==1)&&w.getUUID().equals(w.ownerId()),"Variant and neutral self faction survive egg initialization");
                w.summonWave();var children=skeletons(h,w);h.assertTrue(children.size()==4,"Neutral egg mother produces four children");
                for(var child:children)h.assertFalse(SpellEngine.enemy(child.ownerId(),w),"Egg children must never attack their mother");
                cleanup(h,w.ownerId());
            }
            player.server.getPlayerList().remove(player);player.discard();h.succeed();
        });
    }
    @GameTest(template="royalespells:empty",batch="witch-item-use",timeoutTicks=180)
    public void bothWitchItemsDeployInSurvivalAndHaveCraftingRecipes(GameTestHelper h){
        var at=floor(h);StandaloneTests.ready(h,at,()->{
            var player=TestPlayers.create(h);player.setGameMode(GameType.SURVIVAL);player.setPos(at.add(0,2,-6));player.setXRot(50);
            for(var card:List.of(TroopCard.WITCH,TroopCard.WITCH_EVOLUTION)){
                player.setYRot(card==TroopCard.WITCH?0:55);SpellEngine.refill(player);
                var item=RoyaleSpells.TROOP_ITEMS.get(card);var stack=new net.minecraft.world.item.ItemStack(item);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
                h.assertTrue(item.use(h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND).getResult().consumesAction(),"Actual survival item-use path is enabled for "+card);
                near(h,SpellEngine.elixir(player),5,"Both standalone cards cost five elixir");
                h.assertTrue(h.getLevel().getRecipeManager().byKey(RoyaleSpells.id(card.id())).isPresent(),"Crafting recipe always exists for "+card);
                for(int t=0;t<10;t++)SpellEngine.tick(player.server);
            }
            var witches=h.getLevel().getEntitiesOfClass(RoyalWitch.class,player.getBoundingBox().inflate(25),w->player.getUUID().equals(w.ownerId()));
            h.assertTrue(witches.size()==2&&witches.stream().filter(RoyalWitch::evolved).count()==1,"Two actual variants, owned by the survival player");
            cleanup(h,player.getUUID());player.server.getPlayerList().remove(player);player.discard();h.succeed();
        });
    }
}
