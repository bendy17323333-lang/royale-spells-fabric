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

public class RoyaleClient implements net.fabricmc.api.ClientModInitializer {
    @Override @SuppressWarnings({"rawtypes","unchecked"}) public void onInitializeClient() {
        InfernoDragonAudio.install();
        WitchMagicRenderer.install();
        net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry.ITEM.register((stack,tint)->tint==0?0xE3D9BF:0x842BDC,RoyaleSpells.NEUTRAL_ARMY_EGG);
        net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry.getInstance().register(RoyaleSpells.SPARK,MagicParticle.Factory::new);
        net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry.getInstance().register(RoyaleSpells.GRAVE_MOTE,GraveMoteParticle.Factory::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(RoyaleSpells.SPELL,SpellRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(RoyaleSpells.ZOMBIE,ZombieRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(RoyaleSpells.SKELETON,RoyaleSkeletonRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(RoyaleSpells.ARMY_SKELETON,RoyaleSkeletonRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(RoyaleSpells.ELEMENTAL_SPIRIT,SpiritRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(RoyaleSpells.INFERNO_DRAGON,InfernoDragonRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(RoyaleSpells.SPIRIT_ARC,SpiritArcRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(RoyaleSpells.EVOLUTION_BURST,EvolutionBurstRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(RoyaleSpells.WITCH_BOLT,WitchMagicRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(RoyaleSpells.WITCH,WitchRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(RoyaleSpells.EVOLVED_WITCH,WitchRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(RoyaleSpells.BARBARIAN,c->new TroopRenderer<>(c,"barbarian",.45f));
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(RoyaleSpells.RECRUIT,c->new TroopRenderer<>(c,"royal_recruit",.5f));
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(RoyaleSpells.BARBARIAN_HUT,c->new TroopRenderer<>(c,"barbarian_hut",1.7f));
        net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type,renderer,helper,context)->helper.register(new SpellTint.ColorFeature(renderer)));
        CardRenderer.register();
        if(Boolean.getBoolean("royalespells.visualSmoke")&&!Boolean.getBoolean("royalespells.witchSmoke"))StandaloneClientSmoke.install();
        if(Boolean.getBoolean("royalespells.witchSmoke"))WitchClientSmoke.install();
        SpellEntity.visualTick=RoyaleClient::particles;SpellEntity.impactVisual=ProjectileVisuals::impact;
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_TRANSLUCENT.register(this::fields);
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.LAST.register(TargetPreview::render);
    }
    private void fields(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext context) {
        var client=Minecraft.getInstance();if(client.level==null)return;
        var buffers=client.renderBuffers().bufferSource();var matrices=context.matrixStack();var camera=context.camera().getPosition();float partial=context.tickDelta();
        for(var entity:client.level.entitiesForRendering())if(entity.distanceToSqr(camera)<96*96 && (entity instanceof SpellEntity || entity instanceof dev.royalespells.entity.EvolutionBurst || entity instanceof dev.royalespells.entity.SpiritArc || entity instanceof dev.royalespells.entity.InfernoDragon)) {
            Vec3 at=entity instanceof SpellEntity effect?effect.visualPosition(partial):entity.getPosition(partial);matrices.pushPose();matrices.translate(at.x-camera.x,at.y-camera.y,at.z-camera.z);
            if(entity instanceof SpellEntity effect){SpellFields.render(effect,partial,matrices,buffers);ProjectileVisuals.flight(effect,partial,matrices,buffers);}
            else if(entity instanceof dev.royalespells.entity.SpiritArc arc)SpiritArcRenderer.draw(arc,partial,matrices,buffers);
            else if(entity instanceof dev.royalespells.entity.InfernoDragon dragon)InfernoDragonRenderer.beam(dragon,partial,matrices,buffers);
            else EvolutionBurstRenderer.draw((dev.royalespells.entity.EvolutionBurst)entity,partial,matrices,buffers);
            matrices.popPose();
        }
        // Fabric supplies the camera rotation in this stack already; translate only once.
        ProjectileVisuals.impacts(client.level,partial,matrices,camera,buffers);
        buffers.endBatch(SpellLayers.FIRE_TRAIL);buffers.endBatch(SpellLayers.EFFECT);
    }
    public static void line(PoseStack matrices,VertexConsumer v,Vec3 a,Vec3 b,float r,float g,float blue,float alpha) {
        Vec3 normal=b.subtract(a).normalize();var entry=matrices.last();
        v.vertex(entry.pose(),(float)a.x,(float)a.y,(float)a.z).color(r,g,blue,alpha).normal(entry.normal(),(float)normal.x,(float)normal.y,(float)normal.z).endVertex();
        v.vertex(entry.pose(),(float)b.x,(float)b.y,(float)b.z).color(r,g,blue,alpha).normal(entry.normal(),(float)normal.x,(float)normal.y,(float)normal.z).endVertex();
    }
    private static void particles(SpellEntity e) {
        Spell spell=e.spell();var world=e.level();int t=e.time();Vec3 p=e.visualPosition(0);
        if(spell==Spell.GRAVEYARD && e.tickCount%2==0 && world.random.nextFloat()<FieldAnimation.opacity(spell,t,e.duration())) {
            for(int i=0;i<2;i++) {
                double angle=world.random.nextDouble()*Math.PI*2,radius=Math.sqrt(world.random.nextDouble())*e.radius()*FieldAnimation.opening(spell,t);
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
            FireballParticles.flight(e);
        } else if(spell.rolling() && e.tickCount%3==0) {
            p=SpellEngine.ground(world,p);
            world.addParticle(new BlockParticleOption(ParticleTypes.BLOCK,world.getBlockState(BlockPos.containing(p).below())),p.x,p.y+.1,p.z,0,.04,0);
        } else if(spell==Spell.TORNADO) {
            for(int i=0;i<7;i++){double h=i*.45,a=t*.55+i*.8,r=.3+h*.55;world.addParticle(ParticleTypes.CLOUD,p.x+Math.cos(a)*r,p.y+h,p.z+Math.sin(a)*r,0,.02,0);}
        } else if((spell==Spell.GIANT_SNOWBALL || spell==Spell.GIANT_SNOWBALL_EVOLUTION) && e.tickCount%2==0) {
            Vec3 tail=e.visualPosition(-.8f).add(0,.5,0),side=e.castDirection().cross(new Vec3(0,1,0));
            for(int i=0;i<3;i++){Vec3 at=tail.add(side.scale((world.random.nextDouble()-.5)*.85));world.addParticle(ParticleTypes.SNOWFLAKE,at.x,at.y,at.z,side.x*.02,.012,side.z*.02);}
        } else if(spell==Spell.FREEZE && e.tickCount%3==0) {
            for(int i=0;i<3;i++)world.addParticle(ParticleTypes.SNOWFLAKE,p.x+(world.random.nextDouble()-.5)*e.radius()*2,p.y+.3,p.z+(world.random.nextDouble()-.5)*e.radius()*2,0,.015,0);
        } else if(spell==Spell.EARTHQUAKE && e.tickCount%2==0 || spell==Spell.GRAVEYARD && t>=20 && t%12==8) {
            for(int i=0;i<4;i++)world.addParticle(new BlockParticleOption(ParticleTypes.BLOCK,world.getBlockState(BlockPos.containing(p).below())),p.x+(world.random.nextDouble()-.5)*e.radius()*1.414,p.y+.15,p.z+(world.random.nextDouble()-.5)*e.radius()*1.414,0,.08,0);
        } else if(spell==Spell.POISON && e.tickCount%4==0) {
            world.addParticle(new DustParticleOptions(new Vector3f(.75f,.43f,.06f),.65f),p.x+(world.random.nextDouble()-.5)*e.radius()*1.414,p.y+.2,p.z+(world.random.nextDouble()-.5)*e.radius()*1.414,0,.025,0);
        } else if((spell==Spell.HEAL || spell==Spell.WARMTH) && e.tickCount%8==0)world.addParticle(ParticleTypes.HAPPY_VILLAGER,p.x,p.y+.3,p.z,0,.02,0);
    }
}
