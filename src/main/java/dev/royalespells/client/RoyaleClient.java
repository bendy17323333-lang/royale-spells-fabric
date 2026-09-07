package dev.royalespells.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.royalespells.*;
import dev.royalespells.entity.SpellEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

@net.neoforged.fml.common.Mod(value=RoyaleSpells.MOD_ID,dist=net.neoforged.api.distmarker.Dist.CLIENT)
public class RoyaleClient {
    public RoyaleClient(net.neoforged.bus.api.IEventBus bus) {
        ElixirClient.install(bus);bus.addListener(this::renderers);bus.addListener(this::layers);
        bus.addListener((net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent event)->event.register(RoyaleSpells.id("furnace_material"),FurnaceSpriteSource.TYPE));
        bus.addListener((net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event)->event.register((stack,tint)->tint==0?0xFFE3D9BF:0xFF842BDC,RoyaleSpells.NEUTRAL_ARMY_EGG));
        bus.addListener((net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event)->event.registerSpriteSet(RoyaleSpells.SPARK,MagicParticle.Factory::new));
        bus.addListener((net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event)->event.registerSpriteSet(RoyaleSpells.GRAVE_MOTE,GraveMoteParticle.Factory::new));
        bus.addListener(CardRenderer::register);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::fields);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(TargetPreview::render);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(this::customEntityOverlays);
        SpellEntity.visualTick=RoyaleClient::particles;
        if(Boolean.getBoolean("royalespells.buildShowcase"))ShowcaseCapture.install();
        else if(Boolean.getBoolean("royalespells.visualSmoke"))VisualSmoke.install();
    }
    private void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RoyaleSpells.SPELL,SpellRenderer::new);
        event.registerEntityRenderer(RoyaleSpells.ZOMBIE,ZombieRenderer::new);
        event.registerEntityRenderer(RoyaleSpells.SKELETON,RoyaleSkeletonRenderer::new);
        event.registerEntityRenderer(RoyaleSpells.ARMY_SKELETON,RoyaleSkeletonRenderer::new);
        event.registerEntityRenderer(RoyaleSpells.ELEMENTAL_SPIRIT,SpiritRenderer::new);
        event.registerEntityRenderer(RoyaleSpells.SPIRIT_ARC,SpiritArcRenderer::new);
        event.registerEntityRenderer(RoyaleSpells.RITUAL,RitualRenderer::new);
        event.registerEntityRenderer(RoyaleSpells.EVOLUTION_BURST,EvolutionBurstRenderer::new);
        if(IronSpellSystem.loaded)IronSkeletonClient.register(event);
        event.registerEntityRenderer(RoyaleSpells.BARBARIAN,c->new TroopRenderer<>(c,"barbarian",.45f));
        event.registerEntityRenderer(RoyaleSpells.RECRUIT,c->new TroopRenderer<>(c,"royal_recruit",.5f));
        event.registerEntityRenderer(RoyaleSpells.BARBARIAN_HUT,c->new TroopRenderer<>(c,"barbarian_hut",1.7f));
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    private void layers(net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers event) {
        for(var type:event.getEntityTypes())if(event.getRenderer(type) instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer renderer)renderer.addLayer(new SpellTint.ColorFeature(renderer));
        for(var skin:event.getSkins()){net.minecraft.client.renderer.entity.LivingEntityRenderer renderer=event.getSkin(skin);renderer.addLayer(new SpellTint.ColorFeature(renderer));}
    }
    private void fields(net.neoforged.neoforge.client.event.RenderLevelStageEvent context) {
            if(context.getStage()!=net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES)return;
            var client=Minecraft.getInstance();if(client.level==null || context.getPoseStack()==null)return;
            var buffers=client.renderBuffers().bufferSource();var matrices=TargetPreview.matrices(context);var camera=context.getCamera().getPosition();
            float partial=context.getPartialTick().getGameTimeDeltaPartialTick(false);
            for(var entity:client.level.entitiesForRendering())if(entity.distanceToSqr(camera)<96*96 && (entity instanceof SpellEntity || entity instanceof dev.royalespells.entity.EvolutionBurst || entity instanceof dev.royalespells.entity.SpiritArc)) {
                Vec3 at=entity instanceof SpellEntity effect?effect.visualPosition(partial):entity.position();matrices.pushPose();matrices.translate(at.x-camera.x,at.y-camera.y,at.z-camera.z);
                if(entity instanceof SpellEntity effect)SpellFields.render(effect,partial,matrices,buffers);
                else if(entity instanceof dev.royalespells.entity.SpiritArc arc)SpiritArcRenderer.draw(arc,partial,matrices,buffers);
                else EvolutionBurstRenderer.draw((dev.royalespells.entity.EvolutionBurst)entity,partial,matrices,buffers);
                matrices.popPose();
            }
            if(Boolean.getBoolean("royalespells.visualSmoke"))RangeDepthAudit.before();
            buffers.endBatch(SpellLayers.EFFECT);
            if(Boolean.getBoolean("royalespells.visualSmoke"))RangeDepthAudit.after();
    }
    private void customEntityOverlays(net.neoforged.neoforge.client.event.RenderLevelStageEvent context) {
        if(context.getStage()!=net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        var client=Minecraft.getInstance();if(client.level==null)return;
        var dispatcher=client.getEntityRenderDispatcher();var buffers=client.renderBuffers().bufferSource();
        var camera=context.getCamera().getPosition();float delta=context.getPartialTick().getGameTimeDeltaPartialTick(false);
        for(var entity:client.level.entitiesForRendering())if(entity instanceof net.minecraft.world.entity.LivingEntity living
            && (VisualState.frozen(living)||VisualState.rooted(living)) && living.distanceToSqr(camera)<96*96
            && !(dispatcher.getRenderer(living) instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer)) {
            // GeckoLib and other custom renderers do not pass through vanilla's living-renderer mixin.
            Vec3 at=living.getPosition(delta);var matrices=TargetPreview.matrices(context);matrices.pushPose();
            matrices.translate(at.x-camera.x,at.y-camera.y,at.z-camera.z);
            SpellOverlays.render(living,delta,matrices,buffers,dispatcher.getPackedLightCoords(living,delta));matrices.popPose();
        }
    }
    public static void line(PoseStack matrices,VertexConsumer v,Vec3 a,Vec3 b,float r,float g,float blue,float alpha) {
        Vec3 normal=b.subtract(a).normalize();var entry=matrices.last();
        v.addVertex(entry.pose(),(float)a.x,(float)a.y,(float)a.z).setColor(r,g,blue,alpha).setNormal(entry,(float)normal.x,(float)normal.y,(float)normal.z);
        v.addVertex(entry.pose(),(float)b.x,(float)b.y,(float)b.z).setColor(r,g,blue,alpha).setNormal(entry,(float)normal.x,(float)normal.y,(float)normal.z);
    }
    private static void particles(SpellEntity e) {
        Spell spell=e.spell();var world=e.level();int t=e.time();Vec3 p=e.visualPosition(0);
        if(spell==Spell.GRAVEYARD && e.tickCount%2==0 && world.random.nextFloat()<FieldAnimation.opacity(spell,t,e.duration())) {
            for(int i=0;i<2;i++) {
                double angle=world.random.nextDouble()*Math.PI*2,radius=Math.sqrt(world.random.nextDouble())*spell.radius*FieldAnimation.opening(spell,t);
                world.addParticle(RoyaleSpells.GRAVE_MOTE,p.x+Math.cos(angle)*radius,p.y+.12+world.random.nextDouble()*.25,p.z+Math.sin(angle)*radius,Math.cos(angle)*.006,.015,Math.sin(angle)*.006);
            }
        }
        if(spell==Spell.ROCKET || spell==Spell.PARTY_ROCKET) {
            double progress=(double)t/spell.duration;
            Vec3 tail=RocketMotion.exhaust(e.start(),e.target(),progress);
            Vec3 exhaust=RocketMotion.direction(e.start(),e.target(),progress).scale(-.09);
            world.addParticle(ParticleTypes.FLAME,tail.x,tail.y,tail.z,exhaust.x,exhaust.y,exhaust.z);
            if(e.tickCount%2==0)world.addParticle(ParticleTypes.SMOKE,tail.x,tail.y,tail.z,exhaust.x,exhaust.y,exhaust.z);
        } else if(spell==Spell.FIREBALL) {
            world.addParticle(ParticleTypes.FLAME,p.x,p.y-.4,p.z,0,-.035,0);
            if(e.tickCount%2==0)world.addParticle(ParticleTypes.SMOKE,p.x,p.y-.7,p.z,0,-.06,0);
        } else if(spell.rolling() && e.tickCount%3==0) {
            p=SpellEngine.ground(world,p);
            world.addParticle(new BlockParticleOption(ParticleTypes.BLOCK,world.getBlockState(BlockPos.containing(p).below())),p.x,p.y+.1,p.z,0,.04,0);
        } else if(spell==Spell.TORNADO) {
            for(int i=0;i<7;i++){double h=i*.45,a=t*.55+i*.8,r=.3+h*.55;world.addParticle(ParticleTypes.CLOUD,p.x+Math.cos(a)*r,p.y+h,p.z+Math.sin(a)*r,0,.02,0);}
        } else if(spell==Spell.FREEZE && e.tickCount%3==0 || (spell==Spell.GIANT_SNOWBALL || spell==Spell.GIANT_SNOWBALL_EVOLUTION) && e.tickCount%2==0) {
            for(int i=0;i<3;i++)world.addParticle(ParticleTypes.SNOWFLAKE,p.x+(world.random.nextDouble()-.5)*spell.radius*2,p.y+.3,p.z+(world.random.nextDouble()-.5)*spell.radius*2,0,.015,0);
        } else if(spell==Spell.EARTHQUAKE && e.tickCount%2==0 || spell==Spell.GRAVEYARD && t>=20 && t%12==8) {
            for(int i=0;i<4;i++)world.addParticle(new BlockParticleOption(ParticleTypes.BLOCK,world.getBlockState(BlockPos.containing(p).below())),p.x+(world.random.nextDouble()-.5)*5,p.y+.15,p.z+(world.random.nextDouble()-.5)*5,0,.08,0);
        } else if(spell==Spell.POISON && e.tickCount%4==0) {
            world.addParticle(new DustParticleOptions(new Vector3f(.75f,.43f,.06f),.65f),p.x+(world.random.nextDouble()-.5)*5,p.y+.2,p.z+(world.random.nextDouble()-.5)*5,0,.025,0);
        } else if((spell==Spell.HEAL || spell==Spell.WARMTH) && e.tickCount%8==0)world.addParticle(ParticleTypes.HAPPY_VILLAGER,p.x,p.y+.3,p.z,0,.02,0);
    }
}
