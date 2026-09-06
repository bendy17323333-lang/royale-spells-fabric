package dev.royalespells.client;

import dev.royalespells.*;
import dev.royalespells.entity.SpellEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;

public final class SpellFields {
    private static void point(MatrixStack m,VertexConsumer v,double x,double y,double z,float r,float g,float b,float a){v.vertex(m.peek().getPositionMatrix(),(float)x,(float)y,(float)z).color(r,g,b,a).next();}
    private static void ring(MatrixStack m,VertexConsumer v,double inner,double outer,float r,float g,float b,float alpha){
        for(int i=0;i<80;i++){double a=i*Math.PI/40,c=(i+1)*Math.PI/40;
            point(m,v,Math.cos(a)*inner,.08,Math.sin(a)*inner,r,g,b,alpha);point(m,v,Math.cos(c)*inner,.08,Math.sin(c)*inner,r,g,b,alpha);
            point(m,v,Math.cos(c)*outer,.08,Math.sin(c)*outer,r,g,b,alpha);point(m,v,Math.cos(a)*outer,.08,Math.sin(a)*outer,r,g,b,alpha);
        }
    }
    private static void bolt(MatrixStack m,VertexConsumer v,double x,double z,double h,float width,float alpha,int seed){
        Vec3d last=new Vec3d(x,0,z);
        for(int i=1;i<=9;i++) {
            Vec3d next=new Vec3d(x+Math.sin(i*2.7+seed)*.22,h*i/9,z+Math.cos(i*1.9+seed)*.17);
            for(int face=0;face<4;face++){
                double a=face*Math.PI/2+Math.PI/4,c=a+Math.PI/2;
                point(m,v,last.x+Math.cos(a)*width,last.y,last.z+Math.sin(a)*width,.7f,.87f,1,alpha);
                point(m,v,last.x+Math.cos(c)*width,last.y,last.z+Math.sin(c)*width,.7f,.87f,1,alpha);
                point(m,v,next.x+Math.cos(c)*width,next.y,next.z+Math.sin(c)*width,.85f,.95f,1,alpha);
                point(m,v,next.x+Math.cos(a)*width,next.y,next.z+Math.sin(a)*width,.85f,.95f,1,alpha);
            }last=next;
        }
    }
    public static void render(SpellEntity e,float delta,MatrixStack m,VertexConsumerProvider buffers){
        Spell spell=e.spell();float time=e.time()+delta;
        if(spell==Spell.ZAP || spell==Spell.ZAP_EVOLUTION){
            float age=time-(time>=21?21:1);if(age<0||age>6)return;
            float alpha=1-age/6;double radius=SpellEntity.zapRadius(e.time());var v=buffers.getBuffer(SpellLayers.EFFECT);
            bolt(m,v,0,0,7,.065f,alpha,0);
            for(int i=0;i<4;i++){double a=i*Math.PI/2+.3;bolt(m,v,Math.cos(a)*radius*.65,Math.sin(a)*radius*.65,2.7,.035f,alpha*.7f,i+1);}
            ring(m,v,radius-.05,radius+.035,.42f,.76f,1,alpha*.7f);return;
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
        var v=buffers.getBuffer(SpellLayers.EFFECT);ring(m,v,0,spell.radius,r,g,b,a);ring(m,v,spell.radius-.035,spell.radius+.035,r,g,b,.5f);
    }
}

