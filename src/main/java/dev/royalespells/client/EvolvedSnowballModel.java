// SPDX-License-Identifier: MIT
package dev.royalespells.client;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.royalespells.RoyaleSpells;
import dev.royalespells.entity.SpellEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import java.util.*;

/** Loads the saved Blockbench sculpture; never regenerates replacement geometry.
 * Closed opaque square snow with narrow bevels and separately shaded crystal planes. */
public final class EvolvedSnowballModel {
    private record Face(float[] positions,float[] normals,float[] uv,int color){}
    private record Mesh(List<Face> snow,List<Face> crystals){}
    private static Mesh model;
    private static final ResourceLocation SNOW=ResourceLocation.withDefaultNamespace("textures/block/snow.png");
    private static final ResourceLocation CRYSTAL=ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");
    public static void reload(){model=null;}
    private static Mesh model(){
        if(model==null){
            try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(RoyaleSpells.id("models/effects/evolved_snowball.json"))){
                var data=JsonParser.parseReader(reader).getAsJsonObject();
                model=new Mesh(faces(data.getAsJsonArray("snow")),faces(data.getAsJsonArray("crystals")));
            }catch(Exception ex){throw new IllegalStateException("Unable to load Blockbench evolved snowball",ex);}
        }
        return model;
    }
    private static List<Face> faces(JsonArray list){
        var result=new ArrayList<Face>();
        for(var element:list){var f=element.getAsJsonObject();result.add(new Face(array(f,"positions",12),array(f,"normals",12),array(f,"uv",8),f.get("color").getAsInt()));}
        return List.copyOf(result);
    }
    private static float[] array(JsonObject face,String key,int size){
        var data=face.getAsJsonArray(key);if(data.size()!=size)throw new IllegalArgumentException("Invalid snowball "+key);
        var result=new float[size];for(int i=0;i<size;i++){result[i]=data.get(i).getAsFloat();if(!Float.isFinite(result[i]))throw new IllegalArgumentException("Non-finite snowball vertex");}return result;
    }
    public static void render(SpellEntity e,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        Mesh mesh=model();float age=e.time()+partial;
        float grow=(float)Mth.smoothstep(Mth.clamp((age-22)/4,0,1)),size=Mth.lerp(grow,1.3f,2.4f);
        pose.pushPose();pose.translate(0,.5,0);
        var forward=e.castDirection();pose.mulPose(Axis.YP.rotation((float)Math.atan2(forward.x,forward.z)));
        float angle=age<24?age*13:24*13+(age-24)*(float)(4.0/18/1.2*180/Math.PI);
        pose.mulPose(Axis.XP.rotationDegrees(angle));pose.scale(size,size,size);
        draw(mesh.snow,pose,buffers.getBuffer(RenderType.entitySolid(SNOW)),light);
        draw(mesh.crystals,pose,buffers.getBuffer(RenderType.entitySolid(CRYSTAL)),light);
        pose.popPose();
    }
    private static void draw(List<Face> faces,PoseStack pose,VertexConsumer v,int light){
        for(Face f:faces)for(int i=0;i<4;i++){
            int p=i*3,u=i*2;
            v.addVertex(pose.last().pose(),f.positions[p],f.positions[p+1],f.positions[p+2])
                .setColor(0xff000000|f.color).setUv(f.uv[u],f.uv[u+1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose.last(),f.normals[p],f.normals[p+1],f.normals[p+2]);
        }
    }
    private EvolvedSnowballModel(){}
}
