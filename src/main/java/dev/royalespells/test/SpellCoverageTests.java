package dev.royalespells.test;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

@GameTestHolder("royalespells")
@PrefixGameTestTemplate(false)
public class SpellCoverageTests {
    private static final String EMPTY_STRUCTURE="empty";
    @GameTest(template=EMPTY_STRUCTURE,batch="rocket-flight")
    public void rocketNoseAndExhaustFollowActualFlight(GameTestHelper c) {
        Vec3 start=new Vec3(0,80,0);
        for(Spell spell:List.of(Spell.ROCKET,Spell.PARTY_ROCKET))for(Vec3 end:List.of(
                new Vec3(12,80,0),new Vec3(-8,85,10),new Vec3(0,74,-14),start)) {
            var entity=SpellEntity.create(c.getLevel(),spell,UUID.randomUUID(),start,end);
            for(int tick:new int[]{2,10,19,20,21,30,38}) {
                entity.setPreviewTime(tick);
                double progress=(tick+.25)/spell.duration;
                Vec3 actual=entity.visualPosition(.26f).subtract(entity.visualPosition(.24f)).normalize();
                var nose=new org.joml.Vector3f(0,1,0).rotate(RocketMotion.rotation(start,end,progress));
                c.assertTrue(Float.isFinite(nose.x)&&Float.isFinite(nose.y)&&Float.isFinite(nose.z),"Rocket orientation must remain finite at vertical shots and the apex");
                c.assertTrue(nose.x*actual.x+nose.y*actual.y+nose.z*actual.z>.999,"Nose must align with the actual interpolated projectile trajectory in all flight phases");
                Vec3 tail=RocketMotion.exhaust(start,end,progress).subtract(entity.visualPosition(.25f));
                c.assertTrue(tail.dot(actual)<-1.49,"Fire and smoke must originate behind the rocket, including during descent");
            }
        }
        c.assertTrue(RocketMotion.direction(start,start.add(12,0,0),.1).y>0,"Ascent nose points up");
        c.assertTrue(RocketMotion.direction(start,start.add(12,0,0),.9).y<0,"Descent nose points down");
        var apex=new org.joml.Vector3f(0,1,0).rotate(RocketMotion.rotation(start,start,.5));
        c.assertTrue(Float.isFinite(apex.y)&&apex.y<-.99,"A zero-speed vertical apex must have a stable downward fallback");
        c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="mirror-numbers")
    public void mirrorScalesDamageHealingAndEquippedAttack(GameTestHelper c) {
        UUID owner=UUID.randomUUID();var world=c.getLevel();var target=c.spawnWithNoFreeWill(EntityType.IRON_GOLEM,2,2,2);target.setNoAi(true);var at=target.position();
        var zap=SpellEntity.create(world,Spell.ZAP,owner,at,at);zap.setPower(1.1f);zap.tick();
        c.assertTrue(Math.abs(target.getHealth()-95.6)<.001,"Mirrored Zap must deal 4.4 damage");
        var unit=SpellEngine.summon(world,owner,at.add(1,0,0),"barbarian",false);unit.tick();double before=unit.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        SpellEngine.empower(unit,1.1f);c.assertTrue(Math.abs(unit.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)-before*1.1)<.001,"Summon attack scaling must include its weapon bonus");
        unit.setHealth(3);var heal=SpellEntity.create(world,Spell.HEAL,owner,at,at);heal.setPower(1.1f);heal.tick();
        c.assertTrue(Math.abs(unit.getHealth()-5.2)<.001,"Mirrored healing must scale by one level");target.discard();cleanup(c,owner);c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="skeleton-iframe")
    public void skeletonSwarmBypassesOnlyItsOwnHitCooldown(GameTestHelper c) {
        var target=c.spawnWithNoFreeWill(EntityType.IRON_GOLEM,2,2,2);target.setNoAi(true);
        UUID owner=UUID.randomUUID();var first=SpellEngine.summon(c.getLevel(),owner,pos(c),"skeleton",false);var second=SpellEngine.summon(c.getLevel(),owner,pos(c).add(1,0,0),"skeleton",false);
        target.invulnerableTime=20;float hp=target.getHealth();
        c.assertTrue(first.doHurtTarget(target) && second.doHurtTarget(target),"Both skeleton attacks must land in the same tick despite hurt immunity");
        c.assertTrue(Math.abs(target.getHealth()-(hp-3))<.001,"Two stone-sword skeleton hits must total exactly 3 damage");
        c.assertTrue(target.invulnerableTime>=20,"Other attackers must retain the target's ordinary hurt cooldown");
        c.assertFalse(first.doHurtTarget(second),"Friendly fire must stay blocked");cleanup(c,owner);target.discard();c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="zap-radii")
    public void evolvedZapExpandsOnlyOnSecondPulse(GameTestHelper c) {
        var target=c.spawnWithNoFreeWill(EntityType.IRON_GOLEM,2,2,2);target.setNoAi(true);target.setNoGravity(true);
        Vec3 center=target.position().add(-2.75,0,0);var effect=SpellEntity.create(c.getLevel(),Spell.ZAP_EVOLUTION,UUID.randomUUID(),center,center);
        effect.tick();c.assertTrue(target.getHealth()==100,"First 2.2-block pulse must not reach 2.75 blocks");
        for(int i=1;i<21;i++)effect.tick();c.assertTrue(target.getHealth()==96,"Second 3-block pulse must hit the expanded annulus");target.discard();c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="visual-markers")
    public void freezeAndVinesHaveDistinctSyncedMarkers(GameTestHelper c) {
        UUID owner=UUID.randomUUID();var target=c.spawnWithNoFreeWill(EntityType.IRON_GOLEM,2,2,2);target.setNoAi(true);Vec3 at=target.position();
        SpellEngine.stun(target,3);c.assertFalse(target.hasEffect(RoyaleSpells.FROZEN),"Zap stun must not visually encase targets in ice");
        var freeze=SpellEntity.create(c.getLevel(),Spell.FREEZE,owner,at,at);freeze.tick();c.assertTrue(target.hasEffect(RoyaleSpells.FROZEN),"Freeze must send an ice visual status to the client");
        var vines=SpellEntity.create(c.getLevel(),Spell.VINES,owner,at,at);vines.tick();c.assertTrue(target.hasEffect(RoyaleSpells.ROOTED),"Vines must send a modeled root visual status");
        target.discard();c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="clone-sync")
    public void cloneFlagAndBaseAttackSurviveReload(GameTestHelper c) {
        UUID owner=UUID.randomUUID();var original=SpellEngine.summon(c.getLevel(),owner,pos(c),"barbarian",false);original.tick();
        SpellEngine.cloneAllies(c.getLevel(),owner,original.position(),4);
        var clone=c.getLevel().getEntitiesOfClass(AllyZombie.class,original.getBoundingBox().inflate(6),e->e.isClone()&&owner.equals(e.ownerId())).get(0);
        var nbt=new net.minecraft.nbt.CompoundTag();clone.saveWithoutId(nbt);var loaded=RoyaleSpells.BARBARIAN.create(c.getLevel());loaded.load(nbt);
        c.assertTrue(loaded.isClone()&&loaded.getMaxHealth()==1,"Clone status must persist into tracked client data on reload");
        c.assertTrue(clone.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue()==original.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getBaseValue(),"Cloning must not add the held weapon damage twice");cleanup(c,owner);c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="mirror-cast")
    public void mirrorRetainsEvolutionAndBoostsSummons(GameTestHelper c) {
        var player=TestPlayers.create(c);player.setPos(pos(c));player.setXRot(80);UUID owner=player.getUUID();
        c.assertTrue(SpellEngine.cast(player,Spell.GOBLIN_BARREL_EVOLUTION),"Original evolved card casts");
        for(int i=0;i<11;i++)SpellEngine.tick(c.getLevel().getServer());
        c.assertTrue(SpellEngine.cast(player,Spell.MIRROR),"Mirror casts the previous card");
        var mirrors=c.getLevel().getEntitiesOfClass(SpellEntity.class,player.getBoundingBox().inflate(50),e->owner.equals(e.ownerId)&&e.power()>1);
        c.assertTrue(mirrors.size()==2 && mirrors.stream().allMatch(e->e.spell()==Spell.GOBLIN_BARREL_EVOLUTION),"Mirror keeps the evolved barrel and its decoy effect");
        var effect=mirrors.stream().filter(e->!e.decoy).findFirst().orElseThrow();
        var nbt=new net.minecraft.nbt.CompoundTag();effect.saveWithoutId(nbt);var loaded=RoyaleSpells.SPELL.create(c.getLevel());loaded.load(nbt);
        c.assertTrue(Math.abs(loaded.power()-1.1)<.001,"Mirror level survives save reload");for(int i=0;i<30;i++)loaded.tick();
        var zombies=c.getLevel().getEntitiesOfClass(AllyZombie.class,player.getBoundingBox().inflate(50),e->owner.equals(e.ownerId()));
        c.assertTrue(zombies.size()==3 && zombies.stream().allMatch(e->Math.abs(e.getMaxHealth()-11)<.001),"Mirrored summons gain 10 percent max health");
        cleanup(c,owner);c.getLevel().getServer().getPlayerList().remove(player);player.discard();c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="log-axis")
    public void logAxleRemainsHorizontalAcrossRolls(GameTestHelper c) {
        for(Vec3 travel:List.of(new Vec3(1,0,0),new Vec3(0,0,1),new Vec3(-1,0,-1))) {
            var reference=new org.joml.Vector3f(0,0,1).rotate(LogMotion.rotation(travel,0));
            for(double distance:new double[]{.4,1,2,5}) {
                var axle=new org.joml.Vector3f(0,0,1).rotate(LogMotion.rotation(travel,distance));
                c.assertTrue(axle.distance(reference)<.0001 && Math.abs(axle.y)<.0001,"Log must rotate around its long horizontal axle, never swing its endpoints like a propeller");
                c.assertTrue(Math.abs(axle.x*travel.x+axle.z*travel.z)<.0001,"Axle must be perpendicular to travel");
            }
        }
        var bottom=new org.joml.Vector3f(0,-1,0).rotate(LogMotion.rotation(new Vec3(1,0,0),.1));
        c.assertTrue(bottom.x<0,"Bottom surface must roll backward relative to forward travel");c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="void-beams")
    public void voidBeamsMatchVictimsAndPersist(GameTestHelper c) {
        UUID owner=UUID.randomUUID();Vec3 at=pos(c);
        var first=c.spawnWithNoFreeWill(EntityType.IRON_GOLEM,2,2,2);var second=c.spawnWithNoFreeWill(EntityType.IRON_GOLEM,3,2,2);
        first.setNoAi(true);second.setNoAi(true);first.setNoGravity(true);second.setNoGravity(true);
        var ally=SpellEngine.summon(c.getLevel(),owner,at,"zombie",false);
        var effect=SpellEntity.create(c.getLevel(),Spell.VOID,owner,at,at);
        for(int i=0;i<16;i++)effect.tick();
        c.assertTrue(effect.voidStrikePoints().size()==2 && effect.voidStrength()==2,"Beam endpoints must match enemy victims, excluding allies");
        c.assertTrue(first.getHealth()==91 && second.getHealth()==91,"Visuals must preserve the existing per-victim damage");
        var nbt=new net.minecraft.nbt.CompoundTag();effect.saveWithoutId(nbt);var loaded=RoyaleSpells.SPELL.create(c.getLevel());loaded.load(nbt);
        c.assertTrue(loaded.voidStrikePoints().equals(effect.voidStrikePoints()) && loaded.voidStrikeTick()==16,"Strike snapshot must survive entity synchronization and save reload");
        second.setPos(at.add(12,0,0));for(int i=16;i<40;i++)loaded.tick();
        c.assertTrue(loaded.voidStrikePoints().size()==1 && loaded.voidStrength()==3,"Next wave must retarget remaining enemies and use stronger single-target beam");
        first.discard();second.discard();ally.discard();c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="quake-materials")
    public void earthquakeBreaksHouseAndTreeButKeepsStoneAndOutside(GameTestHelper c) {
        var world=c.getLevel();Vec3 at=pos(c);BlockPos base=BlockPos.containing(at);
        var wood=base.above(1);var roof=base.above(5);var trunk=base.offset(1,8,0);var leaves=base.offset(1,20,0);
        var stone=base.offset(-1,1,0);var outside=base.offset(5,1,0);var high=base.above(33);var chest=base.offset(0,1,1);
        world.setBlockAndUpdate(wood,net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState());world.setBlockAndUpdate(roof,net.minecraft.world.level.block.Blocks.SPRUCE_STAIRS.defaultBlockState());
        world.setBlockAndUpdate(trunk,net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState());world.setBlockAndUpdate(leaves,net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState());
        world.setBlockAndUpdate(stone,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());world.setBlockAndUpdate(outside,net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState());
        world.setBlockAndUpdate(high,net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState());world.setBlockAndUpdate(chest,net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
        ((net.minecraft.world.level.block.entity.ChestBlockEntity)world.getBlockEntity(chest)).setItem(0,new net.minecraft.world.item.ItemStack(Items.DIAMOND));
        var effect=SpellEntity.create(world,Spell.EARTHQUAKE,UUID.randomUUID(),at,at);for(int i=0;i<20;i++)effect.tick();
        c.assertFalse(world.getBlockState(wood).isAir(),"First wave must crack wood rather than destroy it");
        c.assertTrue(Math.abs(EarthquakeDestruction.progress(world,wood)-1d/3)<.001,"First wave contributes one third");
        for(int i=20;i<40;i++)effect.tick();
        c.assertFalse(world.getBlockState(wood).isAir(),"Second wave must keep wood intact");
        c.assertTrue(Math.abs(EarthquakeDestruction.progress(world,wood)-2d/3)<.001,"Second wave contributes another third");
        for(int i=40;i<60;i++)effect.tick();
        for(BlockPos p:List.of(wood,roof,trunk,leaves,chest))c.assertTrue(world.getBlockState(p).isAir(),"Earthquake must destroy wood and tree blocks: "+p);
        c.assertTrue(world.getBlockState(stone).is(net.minecraft.world.level.block.Blocks.STONE),"Stone must survive");
        c.assertTrue(Math.abs(EarthquakeDestruction.progress(world,stone)-.9)<.001,"Stone retains 90 percent damage after all three waves");
        c.assertFalse(world.getBlockState(outside).isAir(),"Blocks outside the radius must survive");c.assertFalse(world.getBlockState(high).isAir(),"Blocks above height limit must survive");
        c.assertTrue(world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(chest).inflate(2),e->e.getItem().is(Items.DIAMOND)).size()>0,"Destroyed chest must spill its contents");
        for(BlockPos p:List.of(wood,roof,trunk,leaves,chest,stone,outside,high))world.setBlockAndUpdate(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="quake-budget")
    public void earthquakeBudgetSurvivesReload(GameTestHelper c) {
        var world=c.getLevel();Vec3 at=pos(c);BlockPos base=BlockPos.containing(at);List<BlockPos> placed=new ArrayList<>();
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)if(x*x+z*z<=12.25)for(int y=0;y<=32;y++) {
            var p=base.offset(x,y,z);world.setBlockAndUpdate(p,net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState());placed.add(p);
        }
        var effect=SpellEntity.create(world,Spell.EARTHQUAKE,UUID.randomUUID(),at,at);effect.tick();
        c.assertTrue(placed.stream().noneMatch(p->world.getBlockState(p).isAir()),"First pulse never instantly destroys new wood");
        c.assertTrue(placed.stream().filter(p->EarthquakeDestruction.progress(world,p)>0).count()<=EarthquakeDestruction.PER_TICK,"Crack updates must be bounded per tick");
        var nbt=new net.minecraft.nbt.CompoundTag();effect.saveWithoutId(nbt);var loaded=RoyaleSpells.SPELL.create(world);loaded.load(nbt);
        for(int i=1;i<60;i++)loaded.tick();
        c.assertTrue(placed.stream().allMatch(p->world.getBlockState(p).isAir()),"Entity reload preserves the pass cursor, so all in-range wood gets exactly three contributions");
        placed.forEach(p->world.setBlockAndUpdate(p,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()));c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="quake-retention",timeoutTicks=210)
    public void quakeCracksPersistBrieflyAndCanBeContinued(GameTestHelper c){
        var w=c.getLevel();var center=pos(c);var p=BlockPos.containing(center).above();var other=p.east();
        w.setBlockAndUpdate(p,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());w.setBlockAndUpdate(other,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var first=SpellEntity.create(w,Spell.EARTHQUAKE,UUID.randomUUID(),center,center);for(int i=0;i<60;i++)first.tick();
        c.assertTrue(EarthquakeDestruction.progress(w,p)>.89,"Cracks remain after the spell entity finishes");
        var second=SpellEntity.create(w,Spell.EARTHQUAKE,UUID.randomUUID(),center.add(-3.4,0,0),center.add(-3.4,0,0));second.tick();
        c.assertTrue(w.getBlockState(p).isAir(),"Another quake can finish the weakened stone");
        c.runAtTickTime(75,()->c.assertTrue(EarthquakeDestruction.progress(w,other)>.89,"Cracks must not disappear immediately"));
        c.runAtTickTime(180,()->{c.assertTrue(EarthquakeDestruction.progress(w,other)==0,"Uncontinued cracks expire after the short grace period");w.setBlockAndUpdate(other,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());c.succeed();});
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="tuned-radii")
    public void changedRadiiMatchActualHits(GameTestHelper c){
        var w=c.getLevel();var mob=c.spawnWithNoFreeWill(EntityType.IRON_GOLEM,2,2,2);mob.setNoAi(true);mob.setNoGravity(true);var at=mob.position();
        for(Spell spell:List.of(Spell.ZAP,Spell.FIREBALL,Spell.ARROWS)){
            double edge=spell==Spell.ZAP?2.2:spell==Spell.FIREBALL?2.8:4.5;
            c.assertTrue(Math.abs(spell.radius-edge)<.001,"Displayed range matches tuning");
            for(boolean inside:new boolean[]{false,true}){
                mob.setHealth(100);mob.setPos(at);mob.setDeltaMovement(Vec3.ZERO);
                var center=at.add(-edge+(inside?.05:-.05),0,0);var fx=SpellEntity.create(w,spell,UUID.randomUUID(),center,center);
                for(int t=0;t<spell.duration;t++)fx.tick();
                c.assertTrue(inside?mob.getHealth()<100:mob.getHealth()==100,"Damage respects the new edge for "+spell+" inside="+inside);
            }
        }
        mob.discard();c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="recording-catalog")
    public void recordingCatalogAndControlsAreIsolated(GameTestHelper c){
        var spells=ShowcaseMap.SCENES.stream().map(ShowcaseMap.Scene::spell).toList();
        c.assertTrue(spells.size()==26&&new HashSet<>(spells).size()==26,"Exactly one scene per included spell");
        for(var spell:Spell.values())c.assertTrue(spells.contains(spell)==(spell!=Spell.HEAL&&spell!=Spell.WARMTH),"Only Heal and Warmth are omitted");
        var p=TestPlayers.create(c);var location=p.position();var item=new net.minecraft.world.item.ItemStack(Items.DIAMOND);p.getInventory().setItem(0,item);
        ShowcaseMap.switchScene(p,1);
        c.assertTrue(p.position().equals(location)&&p.getInventory().getItem(0).is(Items.DIAMOND),"Scene controls leave an ordinary world's player and inventory alone");
        p.discard();c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="noai-snow",timeoutTicks=240)
    public void normalSnowballActuallyMovesNoAiTargets(GameTestHelper c){
        var w=c.getLevel();var mob=c.spawnWithNoFreeWill(EntityType.HUSK,2,15,2);mob.setNoAi(true);mob.setNoGravity(true);var before=mob.position();
        // Randomized 1.21 test origins can put the projectile outside the template's ticking chunk.
        var forced=new ArrayList<ChunkPos>();var origin=new ChunkPos(mob.blockPosition());
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var chunk=new ChunkPos(origin.x+x,origin.z+z);if(w.setChunkForced(chunk.x,chunk.z,true))forced.add(chunk);}
        var at=before.add(0,0,-.5);var fx=SpellEntity.create(w,Spell.GIANT_SNOWBALL,UUID.randomUUID(),at.add(0,0,-6),at);w.addFreshEntity(fx);
        c.succeedWhen(()->{c.assertTrue(fx.time()>=Spell.GIANT_SNOWBALL.duration,"Wait for projectile chunks to begin ticking and the actual impact");c.assertTrue(mob.getZ()>before.z+1,"Normal Snowball must change position, not just velocity, on a NoAI target: ticks="+fx.time()+" before="+before+" after="+mob.position());c.assertTrue(mob.isNoAi(),"Do not permanently enable the target's AI");mob.discard();for(var chunk:forced)w.setChunkForced(chunk.x,chunk.z,false);});
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="noai-tornado",timeoutTicks=55)
    public void tornadoActuallyPullsNoAiTargets(GameTestHelper c){
        var w=c.getLevel();var mob=c.spawnWithNoFreeWill(EntityType.HUSK,2,15,2);mob.setNoAi(true);mob.setNoGravity(true);var before=mob.position();var at=before.add(3,0,0);
        w.addFreshEntity(SpellEntity.create(w,Spell.TORNADO,UUID.randomUUID(),at,at));
        c.runAtTickTime(29,()->{c.assertTrue(mob.position().distanceTo(at)<1,"Tornado must draw the NoAI target into its center");c.assertTrue(mob.getX()>before.x+2,"Pull is visible over multiple world ticks");mob.discard();c.succeed();});
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="force-collision",timeoutTicks=40)
    public void spellImpulseRespectsSolidWallsAndStationaryHuts(GameTestHelper c){
        var w=c.getLevel();var mob=c.spawnWithNoFreeWill(EntityType.HUSK,2,15,2);mob.setNoAi(true);mob.setNoGravity(true);var start=mob.position();var wall=BlockPos.containing(start).south(1);
        for(int x=-1;x<=1;x++)for(int y=-2;y<=4;y++)w.setBlockAndUpdate(wall.offset(x,y,0),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        SpellMotion.impulse(mob,new Vec3(0,.2,1.1));
        UUID owner=UUID.randomUUID();var hut=SpellEngine.summon(w,owner,start.add(6,0,0),"barbarian_hut",false);var hutStart=hut.position();SpellMotion.impulse(hut,new Vec3(1,0,0));
        c.runAtTickTime(22,()->{
            c.assertTrue(mob.getZ()+mob.getBbWidth()/2<=wall.getZ()+.001,"Spell knockback must stop at the wall");
            c.assertTrue(hut.position().distanceTo(hutStart)<.01,"Buildings remain anchored");
            mob.discard();cleanup(c,owner);for(int x=-1;x<=1;x++)for(int y=-2;y<=4;y++)w.setBlockAndUpdate(wall.offset(x,y,0),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());c.succeed();
        });
    }
    private static Vec3 pos(GameTestHelper c){return Vec3.atBottomCenterOf(c.absolutePos(new BlockPos(2,2,2)));}
    private static void cleanup(GameTestHelper c,UUID owner){
        List<Entity> list=new ArrayList<>();for(Entity e:c.getLevel().getAllEntities())
            if(e instanceof Summoned s && owner.equals(s.ownerId()) || e instanceof SpellEntity fx && owner.equals(fx.ownerId))list.add(e);
        list.forEach(Entity::discard);
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="coverage")
    public void allTwentyEightRegisteredEffectsFinish(GameTestHelper c) {
        UUID owner=UUID.randomUUID();Vec3 at=pos(c);var floor=c.getLevel().getBlockState(BlockPos.containing(at).below());
        c.assertTrue(RoyaleSpells.ITEMS.size()==28,"28 cards including all current spells and evolutions");
        for(Spell spell:Spell.values()) {
            var fx=SpellEntity.create(c.getLevel(),spell,owner,at.add(0,0,-4),at);
            c.getLevel().addFreshEntity(fx);
            for(int i=0;i<spell.duration;i++)fx.tick();
            c.assertTrue(fx.isRemoved(),"Effect must complete: "+spell);
            cleanup(c,owner);
            c.assertTrue(c.getLevel().getRecipeManager().byKey(RoyaleSpells.id(spell.id())).isPresent(),"Crafting recipe missing: "+spell);
        }
        c.assertTrue(c.getLevel().getBlockState(BlockPos.containing(at).below()).equals(floor),"Spells must preserve terrain");c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="vine-coverage")
    public void vinesBindOnlyHighestThree(GameTestHelper c) {
        UUID owner=UUID.randomUUID();List<Mob> list=new ArrayList<>();
        for(int i=0;i<4;i++){var e=c.spawnWithNoFreeWill(EntityType.IRON_GOLEM,1+i*.3f,2,2);e.setNoAi(true);e.setNoGravity(true);e.setHealth(40+15*i);list.add(e);}
        var fx=SpellEntity.create(c.getLevel(),Spell.VINES,owner,pos(c),list.get(1).position());fx.tick();
        c.assertFalse(list.get(0).hasEffect(RoyaleSpells.STUN),"Lowest HP must not be rooted");
        for(int i=1;i<4;i++)c.assertTrue(list.get(i).hasEffect(RoyaleSpells.STUN),"Highest three must be rooted");
        list.forEach(Entity::discard);c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="support-coverage")
    public void healingAndWarmthHelpOwnerUnits(GameTestHelper c) {
        UUID owner=UUID.randomUUID();var ally=SpellEngine.summon(c.getLevel(),owner,pos(c),"barbarian",false);ally.setHealth(3);
        var heal=SpellEntity.create(c.getLevel(),Spell.HEAL,owner,pos(c),ally.position());for(int i=0;i<60;i++)heal.tick();
        c.assertTrue(ally.getHealth()==9,"Heal must recover 6 HP over 3 pulses");
        SpellEngine.stun(ally,80);ally.setTicksFrozen(140);
        var warmth=SpellEntity.create(c.getLevel(),Spell.WARMTH,owner,pos(c),ally.position());warmth.tick();
        c.assertFalse(ally.hasEffect(RoyaleSpells.STUN),"Warmth removes spell freeze");
        c.assertTrue(ally.getTicksFrozen()==0,"Warmth removes environmental freezing");cleanup(c,owner);c.succeed();
    }
    @GameTest(template=EMPTY_STRUCTURE,batch="summon-combat",timeoutTicks=100)
    public void skeletonActuallyFightsWithSword(GameTestHelper c) {
        UUID owner=UUID.randomUUID();var target=c.spawnWithNoFreeWill(EntityType.IRON_GOLEM,2,2,2);target.setNoAi(true);target.setPos(SpellEngine.ground(c.getLevel(),target.position()));
        var skeleton=SpellEngine.summon(c.getLevel(),owner,target.position().add(1,0,0),"skeleton",false);
        float before=target.getHealth();
        c.runAfterDelay(60,()->{
            c.assertTrue(target.getHealth()<before,"Summoned skeleton must use real melee AI");
            c.assertTrue(skeleton.getMainHandItem().is(Items.STONE_SWORD),"Stone sword remains equipped");
            cleanup(c,owner);target.discard();c.succeed();
        });
    }
}


