package dev.royalespells.client;

import net.minecraft.client.particle.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;

public class MagicParticle extends TextureSheetParticle {
    protected MagicParticle(ClientLevel world,double x,double y,double z,double dx,double dy,double dz,SpriteSet sprite) {
        super(world,x,y,z,dx,dy,dz);pickSprite(sprite);xd=dx;yd=dy;zd=dz;
        lifetime=18;quadSize=.16f+random.nextFloat()*.16f;hasPhysics=false;
    }
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    @Override public int getLightColor(float tickDelta){return 0xF000F0;}
    @Override public void tick(){super.tick();alpha=1-(float)age/lifetime;roll+=.05f;}
    public record Factory(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        public Particle createParticle(SimpleParticleType type,ClientLevel world,double x,double y,double z,double dx,double dy,double dz) {
            return new MagicParticle(world,x,y,z,dx,dy,dz,sprites);
        }
    }
}
