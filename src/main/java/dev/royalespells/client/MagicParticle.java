package dev.royalespells.client;

import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;

public class MagicParticle extends SpriteBillboardParticle {
    protected MagicParticle(ClientWorld world,double x,double y,double z,double dx,double dy,double dz,SpriteProvider sprite) {
        super(world,x,y,z,dx,dy,dz);setSprite(sprite);velocityX=dx;velocityY=dy;velocityZ=dz;
        maxAge=18;scale=.16f+random.nextFloat()*.16f;collidesWithWorld=false;
    }
    @Override public ParticleTextureSheet getType(){return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;}
    @Override public int getBrightness(float tickDelta){return 0xF000F0;}
    @Override public void tick(){super.tick();alpha=1-(float)age/maxAge;angle+=.05f;}
    public record Factory(SpriteProvider sprites) implements ParticleFactory<DefaultParticleType> {
        public Particle createParticle(DefaultParticleType type,ClientWorld world,double x,double y,double z,double dx,double dy,double dz) {
            return new MagicParticle(world,x,y,z,dx,dy,dz,sprites);
        }
    }
}
