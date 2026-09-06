package dev.royalespells.test;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Items;
import net.minecraft.test.*;
import net.minecraft.util.math.*;
import java.util.*;

public class SpellCoverageTests implements FabricGameTest {
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="rocket-flight")
    public void rocketNoseAndExhaustFollowActualFlight(TestContext c) {
        Vec3d start=new Vec3d(0,80,0);
        for(Spell spell:List.of(Spell.ROCKET,Spell.PARTY_ROCKET))for(Vec3d end:List.of(
                new Vec3d(12,80,0),new Vec3d(-8,85,10),new Vec3d(0,74,-14),start)) {
            var entity=SpellEntity.create(c.getWorld(),spell,UUID.randomUUID(),start,end);
            for(int tick:new int[]{2,10,19,20,21,30,38}) {
                entity.setPreviewTime(tick);
                double progress=(tick+.25)/spell.duration;
                Vec3d actual=entity.visualPosition(.26f).subtract(entity.visualPosition(.24f)).normalize();
                var nose=new org.joml.Vector3f(0,1,0).rotate(RocketMotion.rotation(start,end,progress));
                c.assertTrue(Float.isFinite(nose.x)&&Float.isFinite(nose.y)&&Float.isFinite(nose.z),"Rocket orientation must remain finite at vertical shots and the apex");
                c.assertTrue(nose.x*actual.x+nose.y*actual.y+nose.z*actual.z>.999,"Nose must align with the actual interpolated projectile trajectory in all flight phases");
                Vec3d tail=RocketMotion.exhaust(start,end,progress).subtract(entity.visualPosition(.25f));
                c.assertTrue(tail.dotProduct(actual)<-1.49,"Fire and smoke must originate behind the rocket, including during descent");
            }
        }
        c.assertTrue(RocketMotion.direction(start,start.add(12,0,0),.1).y>0,"Ascent nose points up");
        c.assertTrue(RocketMotion.direction(start,start.add(12,0,0),.9).y<0,"Descent nose points down");
        var apex=new org.joml.Vector3f(0,1,0).rotate(RocketMotion.rotation(start,start,.5));
        c.assertTrue(Float.isFinite(apex.y)&&apex.y<-.99,"A zero-speed vertical apex must have a stable downward fallback");
        c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="mirror-numbers")
    public void mirrorScalesDamageHealingAndEquippedAttack(TestContext c) {
        UUID owner=UUID.randomUUID();var world=c.getWorld();var target=c.spawnMob(EntityType.IRON_GOLEM,2,2,2);target.setAiDisabled(true);var at=target.getPos();
        var zap=SpellEntity.create(world,Spell.ZAP,owner,at,at);zap.setPower(1.1f);zap.tick();
        c.assertTrue(Math.abs(target.getHealth()-95.6)<.001,"Mirrored Zap must deal 4.4 damage");
        var unit=SpellEngine.summon(world,owner,at.add(1,0,0),"barbarian",false);unit.tick();double before=unit.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE);
        SpellEngine.empower(unit,1.1f);c.assertTrue(Math.abs(unit.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE)-before*1.1)<.001,"Summon attack scaling must include its weapon bonus");
        unit.setHealth(3);var heal=SpellEntity.create(world,Spell.HEAL,owner,at,at);heal.setPower(1.1f);heal.tick();
        c.assertTrue(Math.abs(unit.getHealth()-5.2)<.001,"Mirrored healing must scale by one level");target.discard();cleanup(c,owner);c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="skeleton-iframe")
    public void skeletonSwarmBypassesOnlyItsOwnHitCooldown(TestContext c) {
        var target=c.spawnMob(EntityType.IRON_GOLEM,2,2,2);target.setAiDisabled(true);
        UUID owner=UUID.randomUUID();var first=SpellEngine.summon(c.getWorld(),owner,pos(c),"skeleton",false);var second=SpellEngine.summon(c.getWorld(),owner,pos(c).add(1,0,0),"skeleton",false);
        target.timeUntilRegen=20;float hp=target.getHealth();
        c.assertTrue(first.tryAttack(target) && second.tryAttack(target),"Both skeleton attacks must land in the same tick despite hurt immunity");
        c.assertTrue(Math.abs(target.getHealth()-(hp-3))<.001,"Two stone-sword skeleton hits must total exactly 3 damage");
        c.assertTrue(target.timeUntilRegen>=20,"Other attackers must retain the target's ordinary hurt cooldown");
        c.assertFalse(first.tryAttack(second),"Friendly fire must stay blocked");cleanup(c,owner);target.discard();c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="zap-radii")
    public void evolvedZapExpandsOnlyOnSecondPulse(TestContext c) {
        var target=c.spawnMob(EntityType.IRON_GOLEM,2,2,2);target.setAiDisabled(true);target.setNoGravity(true);
        Vec3d center=target.getPos().add(-2.75,0,0);var effect=SpellEntity.create(c.getWorld(),Spell.ZAP_EVOLUTION,UUID.randomUUID(),center,center);
        effect.tick();c.assertTrue(target.getHealth()==100,"First 2.2-block pulse must not reach 2.75 blocks");
        for(int i=1;i<21;i++)effect.tick();c.assertTrue(target.getHealth()==96,"Second 3-block pulse must hit the expanded annulus");target.discard();c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="visual-markers")
    public void freezeAndVinesHaveDistinctSyncedMarkers(TestContext c) {
        UUID owner=UUID.randomUUID();var target=c.spawnMob(EntityType.IRON_GOLEM,2,2,2);target.setAiDisabled(true);Vec3d at=target.getPos();
        SpellEngine.stun(target,3);c.assertFalse(target.hasStatusEffect(RoyaleSpells.FROZEN),"Zap stun must not visually encase targets in ice");
        var freeze=SpellEntity.create(c.getWorld(),Spell.FREEZE,owner,at,at);freeze.tick();c.assertTrue(target.hasStatusEffect(RoyaleSpells.FROZEN),"Freeze must send an ice visual status to the client");
        var vines=SpellEntity.create(c.getWorld(),Spell.VINES,owner,at,at);vines.tick();c.assertTrue(target.hasStatusEffect(RoyaleSpells.ROOTED),"Vines must send a modeled root visual status");
        target.discard();c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="clone-sync")
    public void cloneFlagAndBaseAttackSurviveReload(TestContext c) {
        UUID owner=UUID.randomUUID();var original=SpellEngine.summon(c.getWorld(),owner,pos(c),"barbarian",false);original.tick();
        SpellEngine.cloneAllies(c.getWorld(),owner,original.getPos(),4);
        var clone=c.getWorld().getEntitiesByClass(AllyZombie.class,original.getBoundingBox().expand(6),e->e.isClone()&&owner.equals(e.ownerId())).get(0);
        var nbt=new net.minecraft.nbt.NbtCompound();clone.writeNbt(nbt);var loaded=RoyaleSpells.BARBARIAN.create(c.getWorld());loaded.readNbt(nbt);
        c.assertTrue(loaded.isClone()&&loaded.getMaxHealth()==1,"Clone status must persist into tracked client data on reload");
        c.assertTrue(clone.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue()==original.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE).getBaseValue(),"Cloning must not add the held weapon damage twice");cleanup(c,owner);c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="mirror-cast")
    public void mirrorRetainsEvolutionAndBoostsSummons(TestContext c) {
        var player=c.createMockCreativeServerPlayerInWorld();player.setPosition(pos(c));player.setPitch(80);UUID owner=player.getUuid();
        c.assertTrue(SpellEngine.cast(player,Spell.GOBLIN_BARREL_EVOLUTION),"Original evolved card casts");
        for(int i=0;i<11;i++)SpellEngine.tick(c.getWorld().getServer());
        c.assertTrue(SpellEngine.cast(player,Spell.MIRROR),"Mirror casts the previous card");
        var mirrors=c.getWorld().getEntitiesByClass(SpellEntity.class,player.getBoundingBox().expand(50),e->owner.equals(e.ownerId)&&e.power()>1);
        c.assertTrue(mirrors.size()==2 && mirrors.stream().allMatch(e->e.spell()==Spell.GOBLIN_BARREL_EVOLUTION),"Mirror keeps the evolved barrel and its decoy effect");
        var effect=mirrors.stream().filter(e->!e.decoy).findFirst().orElseThrow();
        var nbt=new net.minecraft.nbt.NbtCompound();effect.writeNbt(nbt);var loaded=RoyaleSpells.SPELL.create(c.getWorld());loaded.readNbt(nbt);
        c.assertTrue(Math.abs(loaded.power()-1.1)<.001,"Mirror level survives save reload");for(int i=0;i<30;i++)loaded.tick();
        var zombies=c.getWorld().getEntitiesByClass(AllyZombie.class,player.getBoundingBox().expand(50),e->owner.equals(e.ownerId()));
        c.assertTrue(zombies.size()==3 && zombies.stream().allMatch(e->Math.abs(e.getMaxHealth()-11)<.001),"Mirrored summons gain 10 percent max health");
        cleanup(c,owner);c.getWorld().getServer().getPlayerManager().remove(player);player.discard();c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="log-axis")
    public void logAxleRemainsHorizontalAcrossRolls(TestContext c) {
        for(Vec3d travel:List.of(new Vec3d(1,0,0),new Vec3d(0,0,1),new Vec3d(-1,0,-1))) {
            var reference=new org.joml.Vector3f(0,0,1).rotate(LogMotion.rotation(travel,0));
            for(double distance:new double[]{.4,1,2,5}) {
                var axle=new org.joml.Vector3f(0,0,1).rotate(LogMotion.rotation(travel,distance));
                c.assertTrue(axle.distance(reference)<.0001 && Math.abs(axle.y)<.0001,"Log must rotate around its long horizontal axle, never swing its endpoints like a propeller");
                c.assertTrue(Math.abs(axle.x*travel.x+axle.z*travel.z)<.0001,"Axle must be perpendicular to travel");
            }
        }
        var bottom=new org.joml.Vector3f(0,-1,0).rotate(LogMotion.rotation(new Vec3d(1,0,0),.1));
        c.assertTrue(bottom.x<0,"Bottom surface must roll backward relative to forward travel");c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="void-beams")
    public void voidBeamsMatchVictimsAndPersist(TestContext c) {
        UUID owner=UUID.randomUUID();Vec3d at=pos(c);
        var first=c.spawnMob(EntityType.IRON_GOLEM,2,2,2);var second=c.spawnMob(EntityType.IRON_GOLEM,3,2,2);
        first.setAiDisabled(true);second.setAiDisabled(true);first.setNoGravity(true);second.setNoGravity(true);
        var ally=SpellEngine.summon(c.getWorld(),owner,at,"zombie",false);
        var effect=SpellEntity.create(c.getWorld(),Spell.VOID,owner,at,at);
        for(int i=0;i<16;i++)effect.tick();
        c.assertTrue(effect.voidStrikePoints().size()==2 && effect.voidStrength()==2,"Beam endpoints must match enemy victims, excluding allies");
        c.assertTrue(first.getHealth()==91 && second.getHealth()==91,"Visuals must preserve the existing per-victim damage");
        var nbt=new net.minecraft.nbt.NbtCompound();effect.writeNbt(nbt);var loaded=RoyaleSpells.SPELL.create(c.getWorld());loaded.readNbt(nbt);
        c.assertTrue(loaded.voidStrikePoints().equals(effect.voidStrikePoints()) && loaded.voidStrikeTick()==16,"Strike snapshot must survive entity synchronization and save reload");
        second.setPosition(at.add(12,0,0));for(int i=16;i<40;i++)loaded.tick();
        c.assertTrue(loaded.voidStrikePoints().size()==1 && loaded.voidStrength()==3,"Next wave must retarget remaining enemies and use stronger single-target beam");
        first.discard();second.discard();ally.discard();c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="quake-materials")
    public void earthquakeBreaksHouseAndTreeButKeepsStoneAndOutside(TestContext c) {
        var world=c.getWorld();Vec3d at=pos(c);BlockPos base=BlockPos.ofFloored(at);
        var wood=base.up(1);var roof=base.up(5);var trunk=base.add(1,8,0);var leaves=base.add(1,20,0);
        var stone=base.add(-1,1,0);var outside=base.add(5,1,0);var high=base.up(33);var chest=base.add(0,1,1);
        world.setBlockState(wood,net.minecraft.block.Blocks.OAK_PLANKS.getDefaultState());world.setBlockState(roof,net.minecraft.block.Blocks.SPRUCE_STAIRS.getDefaultState());
        world.setBlockState(trunk,net.minecraft.block.Blocks.OAK_LOG.getDefaultState());world.setBlockState(leaves,net.minecraft.block.Blocks.OAK_LEAVES.getDefaultState());
        world.setBlockState(stone,net.minecraft.block.Blocks.STONE.getDefaultState());world.setBlockState(outside,net.minecraft.block.Blocks.OAK_PLANKS.getDefaultState());
        world.setBlockState(high,net.minecraft.block.Blocks.OAK_LOG.getDefaultState());world.setBlockState(chest,net.minecraft.block.Blocks.CHEST.getDefaultState());
        ((net.minecraft.block.entity.ChestBlockEntity)world.getBlockEntity(chest)).setStack(0,new net.minecraft.item.ItemStack(Items.DIAMOND));
        var effect=SpellEntity.create(world,Spell.EARTHQUAKE,UUID.randomUUID(),at,at);for(int i=0;i<20;i++)effect.tick();
        c.assertFalse(world.getBlockState(wood).isAir(),"First wave must crack wood rather than destroy it");
        c.assertTrue(Math.abs(EarthquakeDestruction.progress(world,wood)-1d/3)<.001,"First wave contributes one third");
        for(int i=20;i<40;i++)effect.tick();
        c.assertFalse(world.getBlockState(wood).isAir(),"Second wave must keep wood intact");
        c.assertTrue(Math.abs(EarthquakeDestruction.progress(world,wood)-2d/3)<.001,"Second wave contributes another third");
        for(int i=40;i<60;i++)effect.tick();
        for(BlockPos p:List.of(wood,roof,trunk,leaves,chest))c.assertTrue(world.getBlockState(p).isAir(),"Earthquake must destroy wood and tree blocks: "+p);
        c.assertTrue(world.getBlockState(stone).isOf(net.minecraft.block.Blocks.STONE),"Stone must survive");
        c.assertTrue(Math.abs(EarthquakeDestruction.progress(world,stone)-.9)<.001,"Stone retains 90 percent damage after all three waves");
        c.assertFalse(world.getBlockState(outside).isAir(),"Blocks outside the radius must survive");c.assertFalse(world.getBlockState(high).isAir(),"Blocks above height limit must survive");
        c.assertTrue(world.getEntitiesByClass(net.minecraft.entity.ItemEntity.class,new Box(chest).expand(2),e->e.getStack().isOf(Items.DIAMOND)).size()>0,"Destroyed chest must spill its contents");
        for(BlockPos p:List.of(wood,roof,trunk,leaves,chest,stone,outside,high))world.setBlockState(p,net.minecraft.block.Blocks.AIR.getDefaultState());c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="quake-budget")
    public void earthquakeBudgetSurvivesReload(TestContext c) {
        var world=c.getWorld();Vec3d at=pos(c);BlockPos base=BlockPos.ofFloored(at);List<BlockPos> placed=new ArrayList<>();
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)if(x*x+z*z<=12.25)for(int y=0;y<=32;y++) {
            var p=base.add(x,y,z);world.setBlockState(p,net.minecraft.block.Blocks.OAK_PLANKS.getDefaultState());placed.add(p);
        }
        var effect=SpellEntity.create(world,Spell.EARTHQUAKE,UUID.randomUUID(),at,at);effect.tick();
        c.assertTrue(placed.stream().noneMatch(p->world.getBlockState(p).isAir()),"First pulse never instantly destroys new wood");
        c.assertTrue(placed.stream().filter(p->EarthquakeDestruction.progress(world,p)>0).count()<=EarthquakeDestruction.PER_TICK,"Crack updates must be bounded per tick");
        var nbt=new net.minecraft.nbt.NbtCompound();effect.writeNbt(nbt);var loaded=RoyaleSpells.SPELL.create(world);loaded.readNbt(nbt);
        for(int i=1;i<60;i++)loaded.tick();
        c.assertTrue(placed.stream().allMatch(p->world.getBlockState(p).isAir()),"Entity reload preserves the pass cursor, so all in-range wood gets exactly three contributions");
        placed.forEach(p->world.setBlockState(p,net.minecraft.block.Blocks.AIR.getDefaultState()));c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="quake-retention",tickLimit=210)
    public void quakeCracksPersistBrieflyAndCanBeContinued(TestContext c){
        var w=c.getWorld();var center=pos(c);var p=BlockPos.ofFloored(center).up();var other=p.east();
        w.setBlockState(p,net.minecraft.block.Blocks.STONE.getDefaultState());w.setBlockState(other,net.minecraft.block.Blocks.STONE.getDefaultState());
        var first=SpellEntity.create(w,Spell.EARTHQUAKE,UUID.randomUUID(),center,center);for(int i=0;i<60;i++)first.tick();
        c.assertTrue(EarthquakeDestruction.progress(w,p)>.89,"Cracks remain after the spell entity finishes");
        var second=SpellEntity.create(w,Spell.EARTHQUAKE,UUID.randomUUID(),center.add(-3.4,0,0),center.add(-3.4,0,0));second.tick();
        c.assertTrue(w.getBlockState(p).isAir(),"Another quake can finish the weakened stone");
        c.runAtTick(75,()->c.assertTrue(EarthquakeDestruction.progress(w,other)>.89,"Cracks must not disappear immediately"));
        c.runAtTick(180,()->{c.assertTrue(EarthquakeDestruction.progress(w,other)==0,"Uncontinued cracks expire after the short grace period");w.setBlockState(other,net.minecraft.block.Blocks.AIR.getDefaultState());c.complete();});
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="tuned-radii")
    public void changedRadiiMatchActualHits(TestContext c){
        var w=c.getWorld();var mob=c.spawnMob(EntityType.IRON_GOLEM,2,2,2);mob.setAiDisabled(true);mob.setNoGravity(true);var at=mob.getPos();
        for(Spell spell:List.of(Spell.ZAP,Spell.FIREBALL,Spell.ARROWS)){
            double edge=spell==Spell.ZAP?2.2:spell==Spell.FIREBALL?2.8:4.5;
            c.assertTrue(Math.abs(spell.radius-edge)<.001,"Displayed range matches tuning");
            for(boolean inside:new boolean[]{false,true}){
                mob.setHealth(100);mob.setPosition(at);mob.setVelocity(Vec3d.ZERO);
                var center=at.add(-edge+(inside?.05:-.05),0,0);var fx=SpellEntity.create(w,spell,UUID.randomUUID(),center,center);
                for(int t=0;t<spell.duration;t++)fx.tick();
                c.assertTrue(inside?mob.getHealth()<100:mob.getHealth()==100,"Damage respects the new edge for "+spell+" inside="+inside);
            }
        }
        mob.discard();c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="recording-catalog")
    public void recordingCatalogAndControlsAreIsolated(TestContext c){
        var spells=ShowcaseMap.SCENES.stream().map(ShowcaseMap.Scene::spell).toList();
        c.assertTrue(spells.size()==26&&new HashSet<>(spells).size()==26,"Exactly one scene per included spell");
        for(var spell:Spell.values())c.assertTrue(spells.contains(spell)==(spell!=Spell.HEAL&&spell!=Spell.WARMTH),"Only Heal and Warmth are omitted");
        var p=c.createMockCreativeServerPlayerInWorld();var location=p.getPos();var item=new net.minecraft.item.ItemStack(Items.DIAMOND);p.getInventory().setStack(0,item);
        ShowcaseMap.switchScene(p,1);
        c.assertTrue(p.getPos().equals(location)&&p.getInventory().getStack(0).isOf(Items.DIAMOND),"Scene controls leave an ordinary world's player and inventory alone");
        p.discard();c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="noai-snow",tickLimit=65)
    public void normalSnowballActuallyMovesNoAiTargets(TestContext c){
        var w=c.getWorld();var mob=c.spawnMob(EntityType.HUSK,2,15,2);mob.setAiDisabled(true);mob.setNoGravity(true);var before=mob.getPos();
        var at=before.add(0,0,-.5);w.spawnEntity(SpellEntity.create(w,Spell.GIANT_SNOWBALL,UUID.randomUUID(),at.add(0,0,-6),at));
        c.runAtTick(38,()->{c.assertTrue(mob.getZ()>before.z+1,"Normal Snowball must change position, not just velocity, on a NoAI target");c.assertTrue(mob.isAiDisabled(),"Do not permanently enable the target's AI");mob.discard();c.complete();});
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="noai-tornado",tickLimit=55)
    public void tornadoActuallyPullsNoAiTargets(TestContext c){
        var w=c.getWorld();var mob=c.spawnMob(EntityType.HUSK,2,15,2);mob.setAiDisabled(true);mob.setNoGravity(true);var before=mob.getPos();var at=before.add(3,0,0);
        w.spawnEntity(SpellEntity.create(w,Spell.TORNADO,UUID.randomUUID(),at,at));
        c.runAtTick(29,()->{c.assertTrue(mob.getPos().distanceTo(at)<1,"Tornado must draw the NoAI target into its center");c.assertTrue(mob.getX()>before.x+2,"Pull is visible over multiple world ticks");mob.discard();c.complete();});
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="force-collision",tickLimit=40)
    public void spellImpulseRespectsSolidWallsAndStationaryHuts(TestContext c){
        var w=c.getWorld();var mob=c.spawnMob(EntityType.HUSK,2,15,2);mob.setAiDisabled(true);mob.setNoGravity(true);var start=mob.getPos();var wall=BlockPos.ofFloored(start).south(1);
        for(int x=-1;x<=1;x++)for(int y=-2;y<=4;y++)w.setBlockState(wall.add(x,y,0),net.minecraft.block.Blocks.STONE.getDefaultState());
        SpellMotion.impulse(mob,new Vec3d(0,.2,1.1));
        UUID owner=UUID.randomUUID();var hut=SpellEngine.summon(w,owner,start.add(6,0,0),"barbarian_hut",false);var hutStart=hut.getPos();SpellMotion.impulse(hut,new Vec3d(1,0,0));
        c.runAtTick(22,()->{
            c.assertTrue(mob.getZ()+mob.getWidth()/2<=wall.getZ()+.001,"Spell knockback must stop at the wall");
            c.assertTrue(hut.getPos().distanceTo(hutStart)<.01,"Buildings remain anchored");
            mob.discard();cleanup(c,owner);for(int x=-1;x<=1;x++)for(int y=-2;y<=4;y++)w.setBlockState(wall.add(x,y,0),net.minecraft.block.Blocks.AIR.getDefaultState());c.complete();
        });
    }
    private static Vec3d pos(TestContext c){return Vec3d.ofBottomCenter(c.getAbsolutePos(new BlockPos(2,2,2)));}
    private static void cleanup(TestContext c,UUID owner){
        List<Entity> list=new ArrayList<>();for(Entity e:c.getWorld().iterateEntities())
            if(e instanceof Summoned s && owner.equals(s.ownerId()) || e instanceof SpellEntity fx && owner.equals(fx.ownerId))list.add(e);
        list.forEach(Entity::discard);
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="coverage")
    public void allTwentyEightRegisteredEffectsFinish(TestContext c) {
        UUID owner=UUID.randomUUID();Vec3d at=pos(c);var floor=c.getWorld().getBlockState(BlockPos.ofFloored(at).down());
        c.assertTrue(RoyaleSpells.ITEMS.size()==28,"28 cards including all current spells and evolutions");
        for(Spell spell:Spell.values()) {
            var fx=SpellEntity.create(c.getWorld(),spell,owner,at.add(0,0,-4),at);
            c.getWorld().spawnEntity(fx);
            for(int i=0;i<spell.duration;i++)fx.tick();
            c.assertTrue(fx.isRemoved(),"Effect must complete: "+spell);
            cleanup(c,owner);
            c.assertTrue(c.getWorld().getRecipeManager().get(RoyaleSpells.id(spell.id())).isPresent(),"Crafting recipe missing: "+spell);
        }
        c.assertTrue(c.getWorld().getBlockState(BlockPos.ofFloored(at).down()).equals(floor),"Spells must preserve terrain");c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="vine-coverage")
    public void vinesBindOnlyHighestThree(TestContext c) {
        UUID owner=UUID.randomUUID();List<MobEntity> list=new ArrayList<>();
        for(int i=0;i<4;i++){var e=c.spawnMob(EntityType.IRON_GOLEM,1+i*.3f,2,2);e.setAiDisabled(true);e.setNoGravity(true);e.setHealth(40+15*i);list.add(e);}
        var fx=SpellEntity.create(c.getWorld(),Spell.VINES,owner,pos(c),list.get(1).getPos());fx.tick();
        c.assertFalse(list.get(0).hasStatusEffect(RoyaleSpells.STUN),"Lowest HP must not be rooted");
        for(int i=1;i<4;i++)c.assertTrue(list.get(i).hasStatusEffect(RoyaleSpells.STUN),"Highest three must be rooted");
        list.forEach(Entity::discard);c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="support-coverage")
    public void healingAndWarmthHelpOwnerUnits(TestContext c) {
        UUID owner=UUID.randomUUID();var ally=SpellEngine.summon(c.getWorld(),owner,pos(c),"barbarian",false);ally.setHealth(3);
        var heal=SpellEntity.create(c.getWorld(),Spell.HEAL,owner,pos(c),ally.getPos());for(int i=0;i<60;i++)heal.tick();
        c.assertTrue(ally.getHealth()==9,"Heal must recover 6 HP over 3 pulses");
        SpellEngine.stun(ally,80);ally.setFrozenTicks(140);
        var warmth=SpellEntity.create(c.getWorld(),Spell.WARMTH,owner,pos(c),ally.getPos());warmth.tick();
        c.assertFalse(ally.hasStatusEffect(RoyaleSpells.STUN),"Warmth removes spell freeze");
        c.assertTrue(ally.getFrozenTicks()==0,"Warmth removes environmental freezing");cleanup(c,owner);c.complete();
    }
    @GameTest(templateName=EMPTY_STRUCTURE,batchId="summon-combat",tickLimit=100)
    public void skeletonActuallyFightsWithSword(TestContext c) {
        UUID owner=UUID.randomUUID();var target=c.spawnMob(EntityType.IRON_GOLEM,2,2,2);target.setAiDisabled(true);target.setPosition(SpellEngine.ground(c.getWorld(),target.getPos()));
        var skeleton=SpellEngine.summon(c.getWorld(),owner,target.getPos().add(1,0,0),"skeleton",false);
        float before=target.getHealth();
        c.waitAndRun(60,()->{
            c.assertTrue(target.getHealth()<before,"Summoned skeleton must use real melee AI");
            c.assertTrue(skeleton.getMainHandStack().isOf(Items.STONE_SWORD),"Stone sword remains equipped");
            cleanup(c,owner);target.discard();c.complete();
        });
    }
}


