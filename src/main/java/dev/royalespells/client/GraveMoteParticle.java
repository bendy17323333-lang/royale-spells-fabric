package dev.royalespells.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

/** A rising, winding soul wisp with its own painted sprite; never a portal/Enderman particle. */
public final class GraveMoteParticle extends TextureSheetParticle {
    private final float phase;
    protected GraveMoteParticle(ClientLevel world,double x,double y,double z,double dx,double dy,double dz,SpriteSet sprites) {
        super(world,x,y,z,dx,dy,dz);pickSprite(sprites);hasPhysics=false;
        lifetime=38+random.nextInt(28);quadSize=.14f+random.nextFloat()*.15f;
        xd=dx;yd=.01+random.nextDouble()*.014;zd=dz;phase=random.nextFloat()*6.28f;
        rCol=.78f+random.nextFloat()*.22f;gCol=.72f+random.nextFloat()*.25f;bCol=1;alpha=0;
        roll=(random.nextFloat()-.5f)*.45f;
    }
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    @Override public int getLightColor(float delta){return 0xF000F0;}
    @Override public void tick() {
        super.tick();float life=(float)age/lifetime;alpha=(float)Math.sin(Math.PI*life)*.65f;
        xd=xd*.95+Math.sin(age*.12+phase)*.0015;zd=zd*.95+Math.cos(age*.1+phase)*.0015;yd*=.996;
        oRoll=roll;roll+=(float)Math.sin(age*.07+phase)*.006f;
    }
    public record Factory(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        public Particle createParticle(SimpleParticleType type,ClientLevel world,double x,double y,double z,double dx,double dy,double dz){return new GraveMoteParticle(world,x,y,z,dx,dy,dz,sprites);}
    }
}
