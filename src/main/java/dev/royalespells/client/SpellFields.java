package dev.royalespells.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.royalespells.*;
import dev.royalespells.entity.SpellEntity;
import net.minecraft.client.renderer.MultiBufferSource;
public final class SpellFields {
    private static void point(PoseStack m,VertexConsumer v,double x,double y,double z,float r,float g,float b,float a){v.addVertex(m.last().pose(),(float)x,(float)y,(float)z).setColor(r,g,b,a);}
    private static void ring(PoseStack m,VertexConsumer v,double inner,double outer,float r,float g,float b,float alpha){
        for(int i=0;i<80;i++){double a=i*Math.PI/40,c=(i+1)*Math.PI/40;
            point(m,v,Math.cos(a)*inner,.08,Math.sin(a)*inner,r,g,b,alpha);point(m,v,Math.cos(c)*inner,.08,Math.sin(c)*inner,r,g,b,alpha);
            point(m,v,Math.cos(c)*outer,.08,Math.sin(c)*outer,r,g,b,alpha);point(m,v,Math.cos(a)*outer,.08,Math.sin(a)*outer,r,g,b,alpha);
        }
    }
    private static void softRing(PoseStack m,VertexConsumer v,double inner,double outer,float r,float g,float b,float inside,float outside) {
        for(int i=0;i<80;i++){double a=i*Math.PI/40,c=(i+1)*Math.PI/40;
            point(m,v,Math.cos(a)*inner,.085,Math.sin(a)*inner,r,g,b,inside);point(m,v,Math.cos(c)*inner,.085,Math.sin(c)*inner,r,g,b,inside);
            point(m,v,Math.cos(c)*outer,.085,Math.sin(c)*outer,r,g,b,outside);point(m,v,Math.cos(a)*outer,.085,Math.sin(a)*outer,r,g,b,outside);
        }
    }
    public static void render(SpellEntity e,float delta,PoseStack m,MultiBufferSource buffers){
        Spell spell=e.spell();float time=e.time()+delta;
        if(AmbientSpellRenderer.render(e,delta,m,buffers))return;
        if(spell==Spell.ZAP || spell==Spell.ZAP_EVOLUTION){
            ZapRenderer.render(e,delta,m,buffers);return;
        }
        if(spell==Spell.RAGE || spell==Spell.GRAVEYARD) {
            float opacity=FieldAnimation.opacity(spell,time,e.duration());double radius=e.radius()*FieldAnimation.radius(spell,time,e.duration());
            var v=buffers.getBuffer(SpellLayers.EFFECT);
            if(spell==Spell.RAGE) {
                softRing(m,v,0,radius*.86f,.57f,.07f,.81f,.17f*opacity,.2f*opacity);
                softRing(m,v,radius*.86,radius,.8f,.22f,1,.2f*opacity,.44f*opacity);
                softRing(m,v,radius,radius+.12,.92f,.47f,1,.44f*opacity,0);
                if(time<8)ring(m,v,Math.max(0,radius-.055),radius+.055,.99f,.7f,1,.65f*opacity*(1-time/8));
            } else {
                softRing(m,v,0,radius*.86,.19f,.035f,.27f,.12f*opacity,.17f*opacity);
                softRing(m,v,radius*.86,radius,.59f,.22f,.84f,.17f*opacity,.39f*opacity);
                softRing(m,v,radius,radius+.1,.76f,.43f,1,.39f*opacity,0);
            }
            return;
        }
        float r,g,b,a;
        switch(spell){
            case RAGE -> {r=.52f;g=.12f;b=.8f;a=.16f;}
            case FREEZE -> {r=.55f;g=.87f;b=1;a=.13f;}
            case POISON -> {r=.8f;g=.46f;b=.04f;a=.17f;}
            case GRAVEYARD -> {r=.23f;g=.13f;b=.35f;a=.13f;}
            case GOBLIN_CURSE -> {r=.2f;g=.58f;b=.07f;a=.12f;}
            case CLONE -> {r=.1f;g=.95f;b=1;a=.18f*(1-time/spell.duration);}
            default -> {return;}
        }
        var v=buffers.getBuffer(SpellLayers.EFFECT);ring(m,v,0,e.radius(),r,g,b,a);ring(m,v,e.radius()-.035,e.radius()+.035,r,g,b,.5f);
    }
}
