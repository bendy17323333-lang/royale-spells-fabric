package dev.royalespells.client;

import dev.royalespells.*;
import dev.royalespells.entity.SpellEntity;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;
import net.minecraft.particle.*;
import org.joml.Vector3f;

public class RoyaleClient implements ClientModInitializer {
    public void onInitializeClient() {
        if(Boolean.getBoolean("royalespells.buildShowcase"))ShowcaseCapture.install();
        else if(Boolean.getBoolean("royalespells.visualSmoke"))VisualSmoke.install();
        EntityRendererRegistry.register(RoyaleSpells.SPELL,SpellRenderer::new);
        EntityRendererRegistry.register(RoyaleSpells.ZOMBIE,ZombieEntityRenderer::new);
        EntityRendererRegistry.register(RoyaleSpells.SKELETON,SkeletonEntityRenderer::new);
        EntityRendererRegistry.register(RoyaleSpells.BARBARIAN,c->new TroopRenderer<>(c,"barbarian",.45f));
        EntityRendererRegistry.register(RoyaleSpells.RECRUIT,c->new TroopRenderer<>(c,"royal_recruit",.5f));
        EntityRendererRegistry.register(RoyaleSpells.BARBARIAN_HUT,c->new TroopRenderer<>(c,"barbarian_hut",1.7f));
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type,renderer,helper,context)->
            helper.register(new SpellTint.ColorFeature(renderer)));
        ParticleFactoryRegistry.getInstance().register(RoyaleSpells.SPARK,MagicParticle.Factory::new);
        CardRenderer.register();
        SpellEntity.visualTick=RoyaleClient::particles;
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context->{
            var client=MinecraftClient.getInstance();if(client.world==null || context.matrixStack()==null)return;
            var buffers=client.getBufferBuilders().getEntityVertexConsumers();var matrices=context.matrixStack();var camera=context.camera().getPos();
            for(var entity:client.world.getEntities())if(entity instanceof SpellEntity effect && effect.squaredDistanceTo(camera)<96*96) {
                Vec3d at=effect.visualPosition(context.tickCounter().getTickDelta(false));matrices.push();matrices.translate(at.x-camera.x,at.y-camera.y,at.z-camera.z);
                SpellFields.render(effect,context.tickCounter().getTickDelta(false),matrices,buffers);matrices.pop();
            }
            if(Boolean.getBoolean("royalespells.visualSmoke"))RangeDepthAudit.before();
            buffers.draw(SpellLayers.EFFECT);
            if(Boolean.getBoolean("royalespells.visualSmoke"))RangeDepthAudit.after();
        });
        WorldRenderEvents.LAST.register(context->{
            var client=MinecraftClient.getInstance();
            if(client.player==null || client.options.hudHidden || context.matrixStack()==null)return;
            var item=client.player.getMainHandStack().getItem();
            if(!SpellEngine.isCard(item))item=client.player.getOffHandStack().getItem();
            if(!SpellEngine.isCard(item))return;
            double radius=item instanceof SpellItem card?card.spell.radius:1.6;
            Vec3d center=SpellEngine.aim(client.player,32),camera=context.camera().getPos();
            MatrixStack matrices=context.matrixStack();matrices.push();matrices.translate(-camera.x,-camera.y,-camera.z);
            var consumers=client.getBufferBuilders().getEntityVertexConsumers();
            VertexConsumer vertices=consumers.getBuffer(RenderLayer.getLines());
            int rgb=item instanceof SpellItem card?card.spell.color:0x79AEFF;float r=(rgb>>16&255)/255f,g=(rgb>>8&255)/255f,b=(rgb&255)/255f;
            for(int i=0;i<64;i++) {
                double a=i*Math.PI/32,c=(i+1)*Math.PI/32;
                Vec3d from=center.add(Math.cos(a)*radius,0.1,Math.sin(a)*radius);
                Vec3d to=center.add(Math.cos(c)*radius,0.1,Math.sin(c)*radius);
                line(matrices,vertices,from,to,r,g,b,0.9f);
            }
            line(matrices,vertices,center.add(-0.3,0.1,0),center.add(0.3,0.1,0),r,g,b,1);
            line(matrices,vertices,center.add(0,0.1,-0.3),center.add(0,0.1,0.3),r,g,b,1);
            consumers.draw(RenderLayer.getLines());matrices.pop();
        });
    }
    public static void line(MatrixStack matrices,VertexConsumer v,Vec3d a,Vec3d b,float r,float g,float blue,float alpha) {
        Vec3d normal=b.subtract(a).normalize();var entry=matrices.peek();
        v.vertex(entry.getPositionMatrix(),(float)a.x,(float)a.y,(float)a.z).color(r,g,blue,alpha).normal(entry,(float)normal.x,(float)normal.y,(float)normal.z);
        v.vertex(entry.getPositionMatrix(),(float)b.x,(float)b.y,(float)b.z).color(r,g,blue,alpha).normal(entry,(float)normal.x,(float)normal.y,(float)normal.z);
    }
    private static void particles(SpellEntity e) {
        Spell spell=e.spell();var world=e.getWorld();int t=e.time();Vec3d p=e.visualPosition(0);
        if(spell==Spell.ROCKET || spell==Spell.PARTY_ROCKET) {
            double progress=(double)t/spell.duration;
            Vec3d tail=RocketMotion.exhaust(e.start(),e.target(),progress);
            Vec3d exhaust=RocketMotion.direction(e.start(),e.target(),progress).multiply(-.09);
            world.addParticle(ParticleTypes.FLAME,tail.x,tail.y,tail.z,exhaust.x,exhaust.y,exhaust.z);
            if(e.age%2==0)world.addParticle(ParticleTypes.SMOKE,tail.x,tail.y,tail.z,exhaust.x,exhaust.y,exhaust.z);
        } else if(spell==Spell.FIREBALL) {
            world.addParticle(ParticleTypes.FLAME,p.x,p.y-.4,p.z,0,-.035,0);
            if(e.age%2==0)world.addParticle(ParticleTypes.SMOKE,p.x,p.y-.7,p.z,0,-.06,0);
        } else if(spell.rolling() && e.age%3==0) {
            p=SpellEngine.ground(world,p);
            world.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK,world.getBlockState(BlockPos.ofFloored(p).down())),p.x,p.y+.1,p.z,0,.04,0);
        } else if(spell==Spell.TORNADO) {
            for(int i=0;i<7;i++){double h=i*.45,a=t*.55+i*.8,r=.3+h*.55;world.addParticle(ParticleTypes.CLOUD,p.x+Math.cos(a)*r,p.y+h,p.z+Math.sin(a)*r,0,.02,0);}
        } else if(spell==Spell.FREEZE && e.age%3==0 || (spell==Spell.GIANT_SNOWBALL || spell==Spell.GIANT_SNOWBALL_EVOLUTION) && e.age%2==0) {
            for(int i=0;i<3;i++)world.addParticle(ParticleTypes.SNOWFLAKE,p.x+(world.random.nextDouble()-.5)*spell.radius*2,p.y+.3,p.z+(world.random.nextDouble()-.5)*spell.radius*2,0,.015,0);
        } else if(spell==Spell.EARTHQUAKE && e.age%2==0 || spell==Spell.GRAVEYARD && t>=20 && t%12==8) {
            for(int i=0;i<4;i++)world.addParticle(new BlockStateParticleEffect(ParticleTypes.BLOCK,world.getBlockState(BlockPos.ofFloored(p).down())),p.x+(world.random.nextDouble()-.5)*5,p.y+.15,p.z+(world.random.nextDouble()-.5)*5,0,.08,0);
        } else if(spell==Spell.POISON && e.age%4==0) {
            world.addParticle(new DustParticleEffect(new Vector3f(.75f,.43f,.06f),.65f),p.x+(world.random.nextDouble()-.5)*5,p.y+.2,p.z+(world.random.nextDouble()-.5)*5,0,.025,0);
        } else if((spell==Spell.HEAL || spell==Spell.WARMTH) && e.age%8==0)world.addParticle(ParticleTypes.HAPPY_VILLAGER,p.x,p.y+.3,p.z,0,.02,0);
    }
}
