package dev.royalespells.client;

import dev.royalespells.*;
import dev.royalespells.entity.SpellEntity;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;

public class SpellRenderer extends EntityRenderer<SpellEntity> {
    public SpellRenderer(EntityRendererFactory.Context context){super(context);}
    @Override public boolean shouldRender(SpellEntity e,Frustum frustum,double x,double y,double z) {
        if(e.spell()==Spell.VOID || e.spell()==Spell.ZAP || e.spell()==Spell.ZAP_EVOLUTION)return e.shouldRender(x,y,z) && frustum.isVisible(new Box(e.target().add(-4,0,-4),e.target().add(4,16,4)));
        return super.shouldRender(e,frustum,x,y,z);
    }
    @Override public Identifier getTexture(SpellEntity entity){return net.minecraft.screen.PlayerScreenHandler.BLOCK_ATLAS_TEXTURE;}
    private void box(MatrixStack m,VertexConsumerProvider v,int light,Block block,double x,double y,double z,float sx,float sy,float sz) {
        m.push();m.translate(x,y,z);m.scale(sx,sy,sz);
        var state=block.getDefaultState();if(block==Blocks.OAK_LOG)state=state.with(PillarBlock.AXIS,Direction.Axis.Z);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(state,m,v,light,OverlayTexture.DEFAULT_UV);m.pop();
    }
    @Override public void render(SpellEntity e,float yaw,float delta,MatrixStack m,VertexConsumerProvider v,int light) {
        m.push();Vec3d shown=e.visualPosition(delta);if(e.spell().rolling())shown=SpellEngine.ground(e.getWorld(),shown);
        Vec3d offset=shown.subtract(e.getLerpedPos(delta));m.translate(offset.x,offset.y,offset.z);
        Spell spell=e.spell();float spin=(e.time()+delta)*18;
        // Ground fields are composited after the opaque world and all living models.
        Vec3d dir=SpellEngine.horizontal(e.target().subtract(e.start()));
        switch(spell) {
            case THE_LOG -> {
                m.translate(0,.8,0);
                // Local X is travel; local Z is the log's axle. Roll without slipping.
                double distance=e.target().subtract(e.start()).horizontalLength()*MathHelper.clamp((e.time()+delta)/spell.duration,0,1);
                m.multiply(LogMotion.rotation(e.target().subtract(e.start()),distance));
                box(m,v,light,Blocks.OAK_LOG,-.6,-.5,-1.65,1.2f,1,3.3f);
                for(int j=-1;j<=1;j++)for(int i=0;i<4;i++) {
                    m.push();m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i*90));
                    box(m,v,light,Blocks.IRON_BLOCK,-.12,.46,j-.1,.24f,.35f,.24f);m.pop();
                }
            }
            case BARBARIAN_BARREL, BARBARIAN_BARREL_HERO, GOBLIN_BARREL, GOBLIN_BARREL_EVOLUTION -> {
                m.translate(0,.65,0);m.multiply(RotationAxis.POSITIVE_Y.rotation((float)-Math.atan2(dir.z,dir.x)));
                m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spin));box(m,v,light,Blocks.BARREL,-.6,-.6,-.6,1.2f,1.2f,1.2f);
                if(spell.evolved())box(m,v,0xF000F0,Blocks.AMETHYST_BLOCK,-.14,.58,-.14,.28f,.28f,.28f);
            }
            case ROCKET, PARTY_ROCKET -> {
                m.multiply(RocketMotion.rotation(e.start(),e.target(),(e.time()+delta)/spell.duration));
                // Rough timber stave body, mismatched metal straps, wooden fins and exposed explosives.
                for(int i=0;i<8;i++){m.push();m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i*45));box(m,v,light,i%3==0?Blocks.SPRUCE_PLANKS:Blocks.OAK_PLANKS,-.2,-.85,.34,.4f,1.8f+(i%2)*.08f,.18f);m.pop();}
                for(float y:new float[]{-.65f,.55f})for(int i=0;i<8;i++){m.push();m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i*45));box(m,v,light,Blocks.IRON_BLOCK,-.22,y,.49,.44f,.13f,.07f);m.pop();}
                box(m,v,light,Blocks.POLISHED_ANDESITE,-.39,.98,-.39,.78f,.23f,.78f);
                box(m,v,light,Blocks.IRON_BLOCK,-.29,1.2,-.29,.58f,.25f,.58f);
                box(m,v,light,Blocks.ANVIL,-.16,1.43,-.16,.32f,.3f,.32f);
                for(int i=0;i<3;i++){m.push();m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i*120+15));box(m,v,light,Blocks.OAK_PLANKS,.35,-.95,-.09,.65f,.72f-i*.08f,.18f);box(m,v,light,Blocks.TNT,-.15,-1.18,.12,.3f,.56f,.3f);m.pop();}
                box(m,v,light,Blocks.SANDSTONE,-.55,-.1,-.08,1.1f,.1f,.16f);
                box(m,v,light,Blocks.IRON_BLOCK,-.24,.08,.51,.45f,.35f,.04f);
                box(m,v,0xF000F0,Blocks.ORANGE_STAINED_GLASS,-.18,-1.45,-.18,.36f,.48f,.36f);
            }            case GIANT_SNOWBALL, GIANT_SNOWBALL_EVOLUTION -> {
                m.translate(0,.5,0);m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(spin));
                float size=spell.evolved()&&e.time()>=24?2.4f:1.3f;m.scale(size,size,size);
                box(m,v,light,Blocks.SNOW_BLOCK,-.5,-.35,-.35,1,.7f,.7f);
                box(m,v,light,Blocks.SNOW_BLOCK,-.35,-.5,-.35,.7f,1,.7f);
                box(m,v,light,Blocks.SNOW_BLOCK,-.35,-.35,-.5,.7f,.7f,1);
            }
            case FIREBALL -> {m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(spin));box(m,v,0xF000F0,Blocks.MAGMA_BLOCK,-.45,-.45,-.45,.9f,.9f,.9f);}
            case ROYAL_DELIVERY -> {
                box(m,v,light,Blocks.BARREL,-.65,0,-.65,1.3f,1.3f,1.3f);
                box(m,v,light,Blocks.BLUE_CONCRETE,-.68,.55,-.68,1.36f,.2f,1.36f);
                box(m,v,light,Blocks.GOLD_BLOCK,-.25,1.3,-.25,.5f,.18f,.5f);
            }
            case ARROWS -> {
                for(int i=0;i<14;i++) {
                    double a=i*2.4,r=(.7+(i%4)*.9)*spell.radius/4,h=5-((e.time()+delta+i*2)%8)*.6;
                    box(m,v,light,Blocks.OAK_PLANKS,Math.cos(a)*r,h,Math.sin(a)*r,.045f,.75f,.045f);
                    box(m,v,light,Blocks.IRON_BLOCK,Math.cos(a)*r-.04,h-.08,Math.sin(a)*r-.04,.12f,.15f,.12f);
                }
            }
            case VOID -> VoidRenderer.render(e,delta,m,v);
            default -> {}
        }
        m.pop();super.render(e,yaw,delta,m,v,light);
    }
}

