package dev.royalespells.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.royalespells.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.util.ArrayList;
import java.util.List;

public final class TargetPreview {
    record Contour(List<Vec3> points,float r,float g,float b) {}
    static List<Contour> contours=List.of();

    public static PoseStack matrices(RenderLevelStageEvent event) {
        // LevelRenderer keeps camera rotation on the GPU stack until AFTER_LEVEL.
        // Applying it to vertices during particles/entities would rotate the effect twice.
        PoseStack matrices=new PoseStack();
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_LEVEL)matrices.mulPose(event.getModelViewMatrix());
        return matrices;
    }
    public static void render(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;
        contours=List.of();
        var client=Minecraft.getInstance();var player=client.player;
        if(player==null || client.options.hideGui || client.level==null || client.screen!=null || player.isSpectator())return;
        boolean cardMode=false;Spell spell=null;boolean hut=false,army=false,dragon=false;var item=player.getMainHandItem().getItem();
        if(!SpellEngine.isCard(item))item=player.getOffhandItem().getItem();
        if(item instanceof SpellItem card){spell=card.spell;cardMode=true;}
        else if(item instanceof TroopItem card){cardMode=true;hut=card.card==TroopCard.BARBARIAN_HUT;dragon=card.card==TroopCard.INFERNO_DRAGON;}
        else if(IronSpellSystem.loaded) {
            var selected=IronClientPreview.selected(player);if(selected==null)return;
            spell=selected.profile.card();hut=selected.profile==dev.royalespells.iron.IronSpellProfile.BARBARIAN_HUT;army=selected instanceof dev.royalespells.iron.EvolvedArmySpell;
            dragon=selected.profile==dev.royalespells.iron.IronSpellProfile.INFERNO_DRAGON;
        } else return;
        if(spell==null&&!hut&&!army&&!dragon)return;
        float delta=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 center=SpellEngine.aim(player,32,delta),forward=SpellEngine.horizontal(player.getViewVector(delta));
        if(player.isShiftKeyDown() && (spell==Spell.RAGE || spell==Spell.HEAL || spell==Spell.WARMTH || spell==Spell.CLONE))center=player.getPosition(delta);
        // Only the anchor follows the landing surface. Sampling terrain for each vertex
        // stretches both circles and crosses vertically over steps, leaves and cliff edges.
        center=center.add(0,.05,0);
        var shapes=new ArrayList<Contour>();
        if(spell!=null && spell.rolling()) {
            Vec3 start=SpellEngine.ground(player.level(),player.getPosition(delta).add(forward.scale(1.1))).add(0,.08,0);
            double length=cardMode?CardBalance.range(spell):spell==Spell.THE_LOG?10:5;
            double width=cardMode?CardBalance.stats(spell).radius():spell.radius;
            shapes.add(new Contour(cardMode?TargetGeometry.rectangle(start,forward,length,width):TargetGeometry.rolling(start,forward,length,width),.4f,.95f,1));
            shapes.add(new Contour(List.of(start,start.add(forward.scale(length))),.85f,.98f,1));
        } else {
            double radius=dragon?.65:army?2.7:hut?1.6:spell==Spell.ZAP_EVOLUTION?(cardMode?2.5:Spell.ZAP.radius):cardMode?CardBalance.stats(spell).radius():spell.radius;
            shapes.add(new Contour(TargetGeometry.circle(center,radius),.4f,.95f,1));
            if(spell==Spell.ZAP_EVOLUTION)shapes.add(new Contour(TargetGeometry.circle(center,spell.radius),.8f,.58f,1));
            if(spell==Spell.GOBLIN_BARREL_EVOLUTION) {
                Vec3 other=TargetGeometry.decoyTarget(player.level(),center,forward).add(0,.05,0);
                shapes.add(new Contour(TargetGeometry.circle(other,radius),.84f,.58f,1));
            }
        }
        contours=List.copyOf(shapes);
        var camera=event.getCamera().getPosition();var matrices=matrices(event);matrices.translate(-camera.x,-camera.y,-camera.z);
        var buffers=client.renderBuffers().bufferSource();
        if(Boolean.getBoolean("royalespells.targetPreviewSmoke"))TargetPreviewSmoke.beforeDraw();
        for(var type:List.of(SpellLayers.TARGET_GHOST,SpellLayers.TARGET_VISIBLE)) {
            var lines=buffers.getBuffer(type);float opacity=type==SpellLayers.TARGET_GHOST?.28f:.9f;
            for(var shape:contours)for(int i=1;i<shape.points.size();i++)
                RoyaleClient.line(matrices,lines,shape.points.get(i-1),shape.points.get(i),shape.r,shape.g,shape.b,opacity);
            buffers.endBatch(type);
        }
        if(Boolean.getBoolean("royalespells.targetPreviewSmoke"))TargetPreviewSmoke.afterDraw();
    }
    private TargetPreview(){}
}
