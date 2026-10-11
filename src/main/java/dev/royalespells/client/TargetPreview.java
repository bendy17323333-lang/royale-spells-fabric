package dev.royalespells.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.royalespells.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import java.util.ArrayList;
import java.util.List;

public final class TargetPreview {
    record Contour(List<Vec3> points,float r,float g,float b) {}
    static List<Contour> contours=List.of();

    public static void render(WorldRenderContext event) {
        contours=List.of();
        var client=Minecraft.getInstance();var player=client.player;
        if(player==null || client.options.hideGui || client.level==null || client.screen!=null || player.isSpectator())return;
        Spell spell=null;TroopCard troop=null;var item=player.getMainHandItem().getItem();
        if(!SpellEngine.isCard(item))item=player.getOffhandItem().getItem();
        if(item instanceof SpellItem card)spell=card.spell;
        else if(item instanceof TroopItem card)troop=card.card;
        else return;
        boolean hut=troop==TroopCard.BARBARIAN_HUT,army=troop==TroopCard.SKELETON_ARMY_EVOLUTION,dragon=troop==TroopCard.INFERNO_DRAGON;
        float delta=event.tickDelta();
        Vec3 center=SpellEngine.aim(player,32,delta),forward=SpellEngine.horizontal(player.getViewVector(delta));
        if(player.isShiftKeyDown() && (spell==Spell.RAGE || spell==Spell.HEAL || spell==Spell.WARMTH || spell==Spell.CLONE))center=player.getPosition(delta);
        // Only the anchor follows the landing surface. Sampling terrain for each vertex
        // stretches both circles and crosses vertically over steps, leaves and cliff edges.
        center=center.add(0,.05,0);
        var shapes=new ArrayList<Contour>();
        if(spell!=null && spell.rolling()) {
            Vec3 start=SpellEngine.ground(player.level(),player.getPosition(delta).add(forward.scale(1.1))).add(0,.08,0);
            double length=CardBalance.range(spell);
            double width=CardBalance.stats(spell).radius();
            shapes.add(new Contour(TargetGeometry.rectangle(start,forward,length,width),.4f,.95f,1));
            shapes.add(new Contour(List.of(start,start.add(forward.scale(length))),.85f,.98f,1));
        } else {
            double radius=dragon?.65:army?2.7:hut?1.6:troop!=null?.5:spell==Spell.ZAP_EVOLUTION?2.5:CardBalance.stats(spell).radius();
            shapes.add(new Contour(TargetGeometry.circle(center,radius),.4f,.95f,1));
            if(spell==Spell.ZAP_EVOLUTION)shapes.add(new Contour(TargetGeometry.circle(center,CardBalance.stats(spell).radius()),.8f,.58f,1));
            if(spell==Spell.GOBLIN_BARREL_EVOLUTION) {
                Vec3 other=TargetGeometry.decoyTarget(player.level(),center,forward).add(0,.05,0);
                shapes.add(new Contour(TargetGeometry.circle(other,radius),.84f,.58f,1));
            }
        }
        contours=List.copyOf(shapes);
        var camera=event.camera().getPosition();var matrices=event.matrixStack();matrices.pushPose();matrices.translate(-camera.x,-camera.y,-camera.z);
        var buffers=client.renderBuffers().bufferSource();
        for(var type:List.of(SpellLayers.TARGET_GHOST,SpellLayers.TARGET_VISIBLE)) {
            var lines=buffers.getBuffer(type);float opacity=type==SpellLayers.TARGET_GHOST?.28f:.9f;
            for(var shape:contours)for(int i=1;i<shape.points.size();i++)
                RoyaleClient.line(matrices,lines,shape.points.get(i-1),shape.points.get(i),shape.r,shape.g,shape.b,opacity);
            buffers.endBatch(type);
        }
        matrices.popPose();
    }
    private TargetPreview(){}
}
