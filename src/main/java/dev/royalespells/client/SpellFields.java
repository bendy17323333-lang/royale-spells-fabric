package dev.royalespells.client;

import dev.royalespells.*;
import dev.royalespells.entity.SpellEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;

public final class SpellFields {
    private static void point(MatrixStack m,VertexConsumer v,double x,double y,double z,float r,float g,float b,float a){v.vertex(m.peek().getPositionMatrix(),(float)x,(float)y,(float)z).color(r,g,b,a);}
    private static void ring(MatrixStack m,VertexConsumer v,double inner,double outer,float r,float g,float b,float alpha){
        for(int i=0;i<80;i++){double a=i*Math.PI/40,c=(i+1)*Math.PI/40;
            point(m,v,Math.cos(a)*inner,.08,Math.sin(a)*inner,r,g,b,alpha);point(m,v,Math.cos(c)*inner,.08,Math.sin(c)*inner,r,g,b,alpha);
            point(m,v,Math.cos(c)*outer,.08,Math.sin(c)*outer,r,g,b,alpha);point(m,v,Math.cos(a)*outer,.08,Math.sin(a)*outer,r,g,b,alpha);
        }
    }
    public static void render(SpellEntity e,float delta,MatrixStack m,VertexConsumerProvider buffers){
        Spell spell=e.spell();float time=e.time()+delta;
        if(spell==Spell.ZAP || spell==Spell.ZAP_EVOLUTION){
            ZapRenderer.render(e,delta,m,buffers);return;
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

