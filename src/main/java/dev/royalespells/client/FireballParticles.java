// SPDX-License-Identifier: MIT
package dev.royalespells.client;

import dev.royalespells.entity.SpellEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/** Resolve Minecraft's particle sprites at runtime, without bundling its artwork. */
public final class FireballParticles {
    private static boolean visible(ClientLevel world,Vec3 at) {
        var c=Minecraft.getInstance();
        return c.level==world&&c.gameRenderer.getMainCamera().getPosition().distanceToSqr(at)<96*96;
    }
    private static void soot(ClientLevel world,Vec3 at,Vec3 velocity,float size,int life) {
        var particle=Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.LARGE_SMOKE,at.x,at.y,at.z,velocity.x,velocity.y,velocity.z);
        if(particle==null)return;
        float shade=.13f+world.random.nextFloat()*.19f;
        particle.setColor(shade,shade*.93f,shade*.84f);particle.scale(size);particle.setLifetime(life);
    }
    private static void ember(ClientLevel world,Vec3 at,Vec3 velocity,float size,int life) {
        var particle=Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.FIREWORK,at.x,at.y,at.z,velocity.x,velocity.y,velocity.z);
        if(particle==null)return;
        // Reuse the tiny spark's drag, gravity and fade, never a firework burst.
        particle.setColor(1,.38f+world.random.nextFloat()*.42f,.025f);
        particle.setParticleSpeed(velocity.x,velocity.y,velocity.z);particle.scale(size);particle.setLifetime(life);
    }
    public static void flight(SpellEntity e) {
        if(!(e.level() instanceof ClientLevel world)||!visible(world,e.position()))return;
        boolean reduced=Minecraft.getInstance().options.particles().get()==ParticleStatus.MINIMAL;
        Vec3 at=e.visualPosition(-1.8f),back=e.visualPosition(-2.3f).subtract(e.visualPosition(-1.3f)).normalize();
        if(e.tickCount%(reduced?4:2)==0){
            Vec3 jitter=new Vec3((world.random.nextDouble()-.5)*.45,(world.random.nextDouble()-.5)*.35,(world.random.nextDouble()-.5)*.45);
            soot(world,at.add(jitter),back.scale(.035).add(0,.018,0),.8f,18+world.random.nextInt(12));
            ember(world,at.add(jitter),back.scale(.12).add(jitter.scale(.07)),.28f,12+world.random.nextInt(8));
        }
    }
    public static void impact(ClientLevel world,Vec3 at,double radius) {
        double scale=radius/2.8;
        if(!visible(world,at))return;
        boolean reduced=Minecraft.getInstance().options.particles().get()==ParticleStatus.MINIMAL;
        int smokeCount=reduced?8:18,emberCount=reduced?14:48;
        // Independent puffs expand and rise; no uniform orange circle or disc.
        for(int i=0;i<smokeCount;i++) {
            double a=world.random.nextDouble()*Math.PI*2,d=Math.sqrt(world.random.nextDouble())*1.1*scale;
            Vec3 offset=new Vec3(Math.cos(a)*d,.12+world.random.nextDouble()*.45,Math.sin(a)*d);
            soot(world,at.add(offset),new Vec3(Math.cos(a)*(.025+d*.035),.035+world.random.nextDouble()*.075,Math.sin(a)*(.025+d*.035)),
                1.25f+world.random.nextFloat()*1.2f,30+world.random.nextInt(27));
        }
        for(int i=0;i<emberCount;i++) {
            double a=world.random.nextDouble()*Math.PI*2,speed=(.12+world.random.nextDouble()*.42)*scale;
            Vec3 offset=new Vec3((world.random.nextDouble()-.5)*.7,.18+world.random.nextDouble()*.5,(world.random.nextDouble()-.5)*.7);
            ember(world,at.add(offset),new Vec3(Math.cos(a)*speed,.14+world.random.nextDouble()*.35,Math.sin(a)*speed),
                .18f+world.random.nextFloat()*.25f,18+world.random.nextInt(18));
        }
        for(int i=0;i<(reduced?5:12);i++) {
            Vec3 offset=new Vec3((world.random.nextDouble()-.5)*1.8,.18+world.random.nextDouble()*.65,(world.random.nextDouble()-.5)*1.8);
            var p=Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.FLAME,at.x+offset.x,at.y+offset.y,at.z+offset.z,offset.x*.07,.035,offset.z*.07);
            if(p!=null){p.scale(1.15f);p.setLifetime(7+world.random.nextInt(8));}
        }
        if(Boolean.getBoolean("royalespells.fireballSmoke"))System.out.println("FIREBALL_IMPACT_PARTICLES smoke="+smokeCount+" embers="+emberCount+" at="+at);
    }
    private FireballParticles(){}
}
