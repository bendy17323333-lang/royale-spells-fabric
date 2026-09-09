package dev.royalespells.test;
import dev.royalespells.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

/** Registered only when the actual MineClash addon AND Iron's Spells are installed. */
@PrefixGameTestTemplate(false)
public final class ZappiesIntegrationTests {
    @GameTest(template="empty",templateNamespace="royalespells",batch="zappies-inferno-reset")
    public void ActualNativeZappyShotImmediatelyResetsAChargedInfernoDragon(GameTestHelper h){
        var w=h.getLevel();var at=net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(2,15,2)));
        var car=(Mob)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("mineclash:zappies")).create(w);car.moveTo(at);car.setNoAi(true);w.addFreshEntity(car);
        car.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);car.setHealth(1000);
        var dragon=RoyaleSpells.INFERNO_DRAGON.create(w);dragon.setup(UUID.randomUUID(),1000,false);dragon.moveTo(at.add(0,3.5,2));dragon.setNoAi(true);w.addFreshEntity(dragon);dragon.setTarget(car);
        for(int i=0;i<108;i++)dragon.tick();h.assertTrue(dragon.heatTicks()>80,"Precondition: actual tier-three beam before the Zappy shoots");
        ((RangedAttackMob)car).performRangedAttack(dragon,1);
        h.assertTrue(dev.royalespells.pause.ElectricPause.active(dragon)&&dragon.heatTicks()==0&&dragon.beamTargetId()==0,"Native electrical damage plus the addon's status immediately clears all beam state");
        dragon.removeAllEffects();for(int i=0;i<8;i++)dragon.tick();
        h.assertTrue(dragon.heatTicks()==8&&dragon.hitDamage()<2,"After electrical pause the beam starts at tier one");
        car.discard();dragon.discard();h.succeed();
    }
    @GameTest(template="empty",templateNamespace="royalespells",batch="zappies-shared-owner")
    public void originalZappyProtectsSameOwnerAcrossThreeMods(GameTestHelper h)throws Exception{
        var p=TestPlayers.create(h);var w=h.getLevel();var at=net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(new net.minecraft.core.BlockPos(2,15,2)));
        var car=(Mob)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("mineclash:zappies")).create(w);
        var tag=new CompoundTag();car.saveWithoutId(tag);var team=new CompoundTag();team.putUUID("owner",p.getUUID());team.putUUID("squad",UUID.randomUUID());team.putInt("level",11);tag.put("ZappiesAddon",team);car.load(tag);car.setPos(at);car.setNoAi(true);w.addFreshEntity(car);
        var barb=SpellEngine.summon(w,p.getUUID(),at.add(1,0,0),"barbarian",false);barb.setNoAi(true);
        var iron=(Mob)BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("irons_spellbooks:summoned_zombie")).create(w);iron.setPos(at.add(2,0,0));iron.setNoAi(true);w.addFreshEntity(iron);io.redspace.ironsspellbooks.capabilities.magic.SummonManager.setOwner(iron,p);
        try{
            h.assertTrue(car instanceof OwnableEntity&&p.getUUID().equals(((OwnableEntity)car).getOwnerUUID()),"Native ownership interface must expose the squad owner");
            h.assertTrue(SpellEngine.friendly(p.getUUID(),car),"Royal spells recognize the owner's original Zappy");
            float carHp=car.getHealth(),barbHp=barb.getHealth(),ironHp=iron.getHealth();SpellEngine.hit(w,p.getUUID(),car,5);
            ((RangedAttackMob)car).performRangedAttack(barb,1);((RangedAttackMob)car).performRangedAttack(iron,1);
            h.assertTrue(car.getHealth()==carHp&&barb.getHealth()==barbHp&&iron.getHealth()==ironHp,"Royal and Zappy damage both preserve same-owner allies");
            var brain=car.getClass().getMethod("zappyBrain").invoke(car);var friendly=brain.getClass().getMethod("friendly",LivingEntity.class);
            h.assertTrue((boolean)friendly.invoke(brain,barb)&&(boolean)friendly.invoke(brain,iron),"The squad's proactive targeting also recognizes both summon systems");
        }finally{car.discard();barb.discard();iron.discard();p.server.getPlayerList().remove(p);p.discard();}
        h.succeed();
    }
}
