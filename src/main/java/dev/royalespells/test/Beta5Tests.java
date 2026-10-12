// SPDX-License-Identifier: MIT
package dev.royalespells.test;

import dev.royalespells.*;
import dev.royalespells.entity.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import java.util.function.Consumer;

/** Boundaries exercised through saved stacks, real item use, real damage/audio
 * events and the optional mod's actual eggs, not through duplicate mock code. */
@GameTestHolder("royalespells") @PrefixGameTestTemplate(false)
public final class Beta5Tests {
    private static Vec3 at(GameTestHelper h){return Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(8,16,8)));}
    private static void near(GameTestHelper h,double got,double expected,String what){h.assertTrue(Math.abs(got-expected)<.006,what+" got="+got+" expected="+expected);}
    private static Mob dummy(GameTestHelper h,Vec3 at){var m=EntityType.IRON_GOLEM.create(h.getLevel());m.setPos(at);m.setNoAi(true);m.setNoGravity(true);m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);m.setHealth(1000);h.getLevel().addFreshEntity(m);return m;}
    private static void clear(GameTestHelper h,UUID owner){for(var e:com.google.common.collect.ImmutableList.copyOf(h.getLevel().getAllEntities()))if(e instanceof Summoned s&&owner.equals(s.ownerId())||e instanceof SpellEntity f&&owner.equals(f.ownerId))e.discard();}

    @GameTest(template="empty",batch="b5-saved-levels")
    public void levelsBelongToEveryCardStackAndSurviveSerialization(GameTestHelper h){
        var cards=new ArrayList<Item>();cards.addAll(RoyaleSpells.ITEMS.values());cards.addAll(RoyaleSpells.TROOP_ITEMS.values());
        for(var item:cards){
            var legacy=new ItemStack(item);CustomData.update(DataComponents.CUSTOM_DATA,legacy,n->n.putString("OtherModNote","preserve"));
            h.assertTrue(CardLevels.get(legacy)==11,"Old card stays at level 11");
            for(int level:new int[]{1,11,16}){
                var changed=CardLevels.withLevel(legacy,level);var loaded=ItemStack.parseOptional(h.getLevel().registryAccess(),(CompoundTag)changed.save(h.getLevel().registryAccess()));
                h.assertTrue(CardLevels.get(loaded)==level,"Every independent spell/troop card retains level on disk: "+item);
                h.assertTrue("preserve".equals(loaded.get(DataComponents.CUSTOM_DATA).copyTag().getString("OtherModNote")),"Unrelated stack data preserved");
            }
            h.assertTrue(CardLevels.get(legacy)==11,"Editing another copy cannot change an old stack");
        }
        h.assertTrue(!CardLevels.supported(new ItemStack(Items.GOAT_HORN)),"Iron horn not converted to a card");
        for(var item:MineClashCards.CARDS)h.assertFalse(CardLevels.supported(new ItemStack(item)),"Native MineClash cards excluded from scaling");h.succeed();
    }
    @GameTest(template="empty",batch="b5-real-damage")
    public void completeCastsScaleDamageButNotRangeOrTiming(GameTestHelper h){
        var at=at(h);UUID owner=UUID.randomUUID();
        for(int level:new int[]{1,11,16})for(Spell spell:List.of(Spell.ZAP,Spell.FIREBALL,Spell.POISON,Spell.VOID)){
            var victim=dummy(h,at);var outside=dummy(h,at.add(CardBalance.stats(spell).radius()+.02,0,0));
            var effect=SpellEntity.create(h.getLevel(),spell,owner,at,at);effect.setCardLevel(level);
            int ticks=effect.duration();for(int t=0;t<ticks;t++)effect.tick();
            double total=switch(spell){case ZAP->192;case FIREBALL->688;case POISON->736;default->2088;};
            double factor=level==1?.3855432894295314:level==11?1:1.61051;
            near(h,1000-victim.getHealth(),total/27*factor,"Actual complete level "+level+" "+spell);
            near(h,outside.getHealth(),1000,"Radius is unchanged by level");
            h.assertTrue(ticks==CardBalance.stats(spell).duration(),"Level cannot change duration");victim.discard();outside.discard();effect.discard();
        }h.succeed();
    }
    @GameTest(template="empty",batch="b5-real-troops")
    public void troopsShieldsHealingAndDescendantsInheritCardLevel(GameTestHelper h){
        var at=at(h);UUID owner=UUID.randomUUID();var w=RoyaleSpells.EVOLVED_WITCH.create(h.getLevel());w.configure(owner,true,16);w.setNoAi(true);w.setPos(at);h.getLevel().addFreshEntity(w);
        near(h,w.getMaxHealth(),839/27.0*1.61051,"Witch health");near(h,w.projectileDamage(),5*1.61051,"Witch attack");w.summonWave();
        var children=h.getLevel().getEntitiesOfClass(AllySkeleton.class,w.getBoundingBox().inflate(10),s->owner.equals(s.ownerId()));
        h.assertTrue(children.size()==4,"Four normal descendants, no level-dependent count");
        for(var child:children){near(h,child.getMaxHealth(),3*1.61051,"Descendant health");near(h,child.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue(),3*1.61051,"Descendant attack");}
        var recruit=(AllyZombie)SpellEngine.summon(h.getLevel(),owner,at.add(4,0,0),"recruit",false);CardBalance.apply(recruit,"recruit",false,1);
        near(h,recruit.cardShield(),240/27.0*.3855432894295314,"Low-level real shield");
        var tag=new CompoundTag();recruit.saveWithoutId(tag);var restored=RoyaleSpells.RECRUIT.create(h.getLevel());restored.load(tag);
        near(h,restored.cardShield(),recruit.cardShield(),"Shield survives reload");h.assertTrue(CardBalance.level(restored)==1,"Troop level survives reload");
        w.setHealth(1);var heal=SpellEntity.create(h.getLevel(),Spell.HEAL,owner,at,at);heal.setCardLevel(16);heal.tick();near(h,w.getHealth(),1+78/27.0*1.61051,"Actual healing uses level");
        clear(h,owner);h.succeed();
    }
    @GameTest(template="empty",batch="b5-real-card-use")
    public void actualCardUseAndMirrorPropagatePerStackLevels(GameTestHelper h){
        var p=TestPlayers.create(h);p.setPos(at(h));p.setXRot(70);UUID owner=p.getUUID();
        var stack=CardLevels.withLevel(new ItemStack(RoyaleSpells.ITEMS.get(Spell.GOBLIN_BARREL_EVOLUTION)),1);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        h.assertTrue(stack.use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult().consumesAction(),"Real evolved card use");
        for(int t=0;t<11;t++)SpellEngine.tick(p.server);
        var mirror=CardLevels.withLevel(new ItemStack(RoyaleSpells.ITEMS.get(Spell.MIRROR)),16);p.setItemInHand(InteractionHand.MAIN_HAND,mirror);
        h.assertTrue(mirror.use(h.getLevel(),p,InteractionHand.MAIN_HAND).getResult().consumesAction(),"Real level-16 Mirror use");
        var all=h.getLevel().getEntitiesOfClass(SpellEntity.class,p.getBoundingBox().inflate(50),e->owner.equals(e.ownerId));
        h.assertTrue(all.size()==4&&all.stream().filter(e->e.cardLevel()==1).count()==2&&all.stream().filter(e->e.cardLevel()==17).count()==2,"Mirror's level + 1 and both decoy levels propagate independently");
        for(var e:all)h.assertTrue(e.spell()==Spell.GOBLIN_BARREL_EVOLUTION,"Mirror preserves evolution");
        clear(h,owner);p.server.getPlayerList().remove(p);p.discard();h.succeed();
    }
    @GameTest(template="empty",batch="b5-level-permissions")
    public void creativeCommandsDoNotGrantSurvivalPlayersAdministrativePowers(GameTestHelper h)throws Exception{
        var p=TestPlayers.create(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(RoyaleSpells.ITEMS.get(Spell.ZAP)));
        var dispatcher=p.server.getCommands().getDispatcher();var source=p.createCommandSourceStack().withPermission(0);
        dispatcher.execute("royalespells level 16",source);h.assertTrue(CardLevels.get(p.getMainHandItem())==16,"Creative player can choose levels without OP");
        for(String command:List.of("royalespells give","royalespells clear","royalespells refill")){
            boolean denied=false;try{dispatcher.execute(command,source);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException ex){denied=true;}h.assertTrue(denied,"Existing admin command remains protected: "+command);
        }
        p.setGameMode(GameType.SURVIVAL);boolean denied=false;try{dispatcher.execute("royalespells level 1",source);}catch(com.mojang.brigadier.exceptions.CommandSyntaxException ex){denied=true;}
        h.assertTrue(denied&&CardLevels.get(p.getMainHandItem())==16,"Survival player cannot relevel cards without OP");
        dispatcher.execute("royalespells level 1",source.withPermission(2));h.assertTrue(CardLevels.get(p.getMainHandItem())==1,"Operator command allowed");
        p.server.getPlayerList().remove(p);p.discard();h.succeed();
    }
    @GameTest(template="empty",batch="b5-void-audio")
    public void voidChoosesSilenceSingleAndGroupFromEachActualPulse(GameTestHelper h){
        for(boolean iron:new boolean[]{false,true}){
            Vec3 at=at(h);var fx=SpellEntity.create(h.getLevel(),Spell.VOID,UUID.randomUUID(),at,at);if(iron)fx.setIronSpell("royalespells:void",1,1);
            var events=new ArrayList<String>();Consumer<PlayLevelSoundEvent.AtPosition> listener=e->{if(e.getLevel()==h.getLevel()&&e.getSound()!=null){String id=e.getSound().value().getLocation().getPath();if(id.startsWith("spell_void_strike"))events.add(id);}};
            NeoForge.EVENT_BUS.addListener(listener);
            try{
                for(int t=0;t<16;t++)fx.tick();h.assertTrue(events.isEmpty(),"Empty first pulse has no strike sound");
                var a=dummy(h,at);for(int t=16;t<(iron?40:36);t++)fx.tick();
                h.assertTrue(events.equals(List.of("spell_void_strike_single")),"One target selects original heavy layer");
                var b=dummy(h,at.add(.8,0,0));for(int t=iron?40:36;t<(iron?64:56);t++)fx.tick();
                h.assertTrue(events.equals(List.of("spell_void_strike_single","spell_void_strike")),"Multiple targets select group cue, without replaying deploy");a.discard();b.discard();
            }finally{NeoForge.EVENT_BUS.unregister(listener);fx.discard();}
        }h.succeed();
    }
    @GameTest(template="empty",batch="b5-mineclash")
    public void optionalMineClashCardsInvokeNativeEggsAndPreserveNativeStats(GameTestHelper h){
        boolean loaded=net.neoforged.fml.ModList.get().isLoaded("mineclash");
        h.assertTrue(MineClashCards.CARDS.size()==15,"All fifteen native egg types have a portrait");
        for(var card:MineClashCards.CARDS)h.assertTrue(card.available()==loaded,"Hidden when native dependency is absent");
        if(!loaded){h.succeed();return;}
        var p=TestPlayers.create(h);var at=at(h);var ground=BlockPos.containing(at).below();
        for(int x=-3;x<=8;x++)for(int z=-3;z<=3;z++)h.getLevel().setBlockAndUpdate(ground.offset(x,0,z),Blocks.STONE.defaultBlockState());
        for(var card:MineClashCards.CARDS){
            var nativeEgg=(SpawnEggItem)BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mineclash",card.entityId+"_spawn_egg"));
            var actual=new ArrayList<Mob>();
            for(int i=0;i<2;i++){
                Item item=i==0?nativeEgg:card;var stack=new ItemStack(item);p.setItemInHand(InteractionHand.MAIN_HAND,stack);var pos=ground.offset(i*5,0,0);
                var result=item.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos).add(0,.5,0),Direction.UP,pos,false)));
                h.assertTrue(result.consumesAction(),"Real item use spawns "+item);
                var spawned=h.getLevel().getEntitiesOfClass(Mob.class,new AABB(pos).inflate(2.4),e->e.getType()==nativeEgg.getType(stack));
                h.assertTrue(spawned.size()==1,"Wrapper preserves native egg count: "+card.entityId);var m=spawned.getFirst();m.setNoAi(true);actual.add(m);
                h.assertFalse(CardBalance.isCard(m),"No Royale level or balance tag placed on MineClash mob");
            }
            var original=actual.getFirst();var wrapped=actual.getLast();
            h.assertTrue(original.getClass()==wrapped.getClass(),"Same native AI implementation class");near(h,wrapped.getMaxHealth(),original.getMaxHealth(),"Native HP unchanged");
            for(var attribute:List.of(Attributes.ATTACK_DAMAGE,Attributes.MOVEMENT_SPEED,Attributes.ARMOR))if(original.getAttribute(attribute)!=null)near(h,wrapped.getAttribute(attribute).getBaseValue(),original.getAttribute(attribute).getBaseValue(),"Native attribute unchanged");
            actual.forEach(Entity::discard);
        }p.server.getPlayerList().remove(p);p.discard();h.succeed();
    }
    @GameTest(template="empty",batch="b5-iron-isolation")
    public void highCardLevelsCannotChangeIronSpellDamageDurationOrRadius(GameTestHelper h){
        for(Spell spell:Spell.values()){
            var fx=SpellEntity.create(h.getLevel(),spell,UUID.randomUUID(),at(h),at(h));fx.setIronSpell("royalespells:"+spell.id(),3,1);
            int time=fx.duration();double radius=fx.radius();float damage=fx.hitAmount(7);
            fx.setCardLevel(17);near(h,fx.radius(),radius,"Iron radius");near(h,fx.hitAmount(7),damage,"Iron damage");h.assertTrue(fx.duration()==time,"Iron duration");fx.discard();
        }h.succeed();
    }
}
