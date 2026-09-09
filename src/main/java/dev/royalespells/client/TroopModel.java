package dev.royalespells.client;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.royalespells.RoyaleSpells;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

/** Articulated, material-mapped models. The same editable hierarchy is exported for Blockbench. */
public final class TroopModel<T extends LivingEntity> extends EntityModel<T> implements ArmedModel {
    private final Map<String,Part> parts=new LinkedHashMap<>();
    private final String kind;
    private float opacity=1,tintRed=1,tintGreen=1,tintBlue=1;
    private boolean emissivePass,flamePass;
    private float animationAge;
    private static class Part {
        String name,parent;float[] pivot,rotation;float pitch,yaw,roll;boolean visible=true;List<Mesh> boxes=new ArrayList<>();List<Part> children=new ArrayList<>();
    }
    private record Mesh(float[][] faces,float[][] normals,float[][] uvs,int material,float[] color,boolean flat,boolean emissive,boolean flame){}
    private static final int[][] NORMALS={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,-1,0},{0,1,0}};
    public TroopModel(String kind) {
        this.kind=kind;
        try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(RoyaleSpells.id("models/troop/"+kind+".json"))) {
            for(var entry:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("parts")) {
                var json=entry.getAsJsonObject();Part p=new Part();p.name=json.get("name").getAsString();p.parent=json.get("parent").getAsString();p.pivot=array(json.getAsJsonArray("pivot"));p.rotation=array(json.getAsJsonArray("rotation"));
                json.getAsJsonArray("boxes").forEach(box->p.boxes.add(mesh(box.getAsJsonObject())));parts.put(p.name,p);
            }
            for(Part p:parts.values())if(!p.parent.isEmpty())parts.get(p.parent).children.add(p);
        }catch(Exception ex){throw new IllegalStateException("Unable to load troop model "+kind,ex);}
    }
    private static float[] array(JsonArray array){return new float[]{array.get(0).getAsFloat(),array.get(1).getAsFloat(),array.get(2).getAsFloat()};}
    private void pose(String name,float pitch,float yaw,float roll){Part p=parts.get(name);if(p!=null){p.pitch=pitch;p.yaw=yaw;p.roll=roll;}}
    @Override public void setupAnim(T entity,float limbAngle,float limbDistance,float age,float headYaw,float headPitch) {
        animationAge=age;
        parts.values().forEach(p->{p.pitch=0;p.yaw=0;p.roll=0;p.visible=true;});opacity=tintRed=tintGreen=tintBlue=1;
        float walk=Mth.cos(limbAngle*.65f)*limbDistance;float breathe=Mth.sin(age*.07f)*.025f;
        pose("head",headPitch*Mth.DEG_TO_RAD,headYaw*Mth.DEG_TO_RAD,0);
        pose("right_leg",walk,0,0);pose("left_leg",-walk,0,0);
        pose("right_arm",-walk*.7f+breathe,0,.04f);pose("left_arm",walk*.7f-breathe,0,-.04f);
        pose("cape",.12f+Math.abs(walk)*.18f,Mth.sin(age*.055f)*.035f,0);
        pose("banner",0,Mth.sin(age*.07f)*.08f,Mth.sin(age*.09f)*.04f);
        pose("jaw",Math.abs(walk)*.12f,0,0);
        if(attackTime>0){float strike=Mth.sin(attackTime*Mth.PI);pose("right_arm",-strike*1.9f-.2f,0,-strike*.12f);pose("body",0,-strike*.1f,0);}
        if(kind.equals("royal_recruit")){pose("left_arm",-.32f,0,-.05f);pose("right_arm",-.18f-walk*.25f,0,.025f);
            if(entity instanceof dev.royalespells.entity.AllyZombie recruit&&recruit.cardShield()==0&&parts.containsKey("shield"))parts.get("shield").visible=false;
        }
        if(entity instanceof dev.royalespells.entity.ElementalSpirit spirit){
            // No human gait: the elemental body waddles, its little limbs flap,
            // then tuck into the committed jump. Preserve per-entity frozen poses.
            float leap=spirit.leapTicks()>0?1:0,wiggle=Mth.sin(age*.32f)*.10f;
            pose("body",-leap*.32f,headYaw*Mth.DEG_TO_RAD*.35f,wiggle*.35f);
            pose("right_arm",-.25f-leap*.85f,0,-.35f-wiggle);
            pose("left_arm",-.25f-leap*.85f,0,.35f+wiggle);
            pose("right_leg",walk*.7f+leap*.65f,0,0);pose("left_leg",-walk*.7f+leap*.65f,0,0);
            pose("crown",wiggle*.25f,0,wiggle*.25f);pose("tongue",wiggle*.6f,0,0);
        }
        if(entity instanceof dev.royalespells.entity.InfernoDragon dragon){
            // Keep the approved 35-degree flight pose. Mirrored Z rotation flaps
            // body-mounted wings vertically; the static mounts cancel body pitch.
            float angle=infernoWing(age/20f)*Mth.DEG_TO_RAD;
            pose("body",0,0,0);pose("head",0,0,0);
            pose("wing_left",0,0,-angle);pose("wing_right",0,0,angle);
            pose("jaw",dragon.heatTicks()>0?Mth.clamp(dragon.heatTicks()/8f,0,1)*.42f:.04f+Mth.sin(age*.09f)*.015f,0,0);
            pose("tail_1",0,Mth.cos(age*.14f)*.052f,0);pose("tail_2",0,Mth.sin(age*.14f)*.05f,0);
        }
        for(var p:parts.values()){var held=FrozenRender.pose(p,p.pitch,p.yaw,p.roll);p.pitch=held[0];p.yaw=held[1];p.roll=held[2];}
    }
    private static float infernoWing(float seconds){
        float t=seconds-(float)Math.floor(seconds);float[] times={0,.16f,.32f,.64f,.82f,1},angles={20,-32,-18,26,28,20};
        for(int i=1;i<times.length;i++)if(t<=times[i]){float a=(t-times[i-1])/(times[i]-times[i-1]);a=a*a*(3-2*a);return Mth.lerp(a,angles[i-1],angles[i]);}return 20;
    }
    @Override public void translateToHand(HumanoidArm arm,PoseStack matrices){
        Part part=parts.get(arm==HumanoidArm.RIGHT?"right_arm":"left_arm");
        if(part==null)return;
        applyParents(part,matrices);
        // The feature's Y half-turn puts the right-hand grip at X=-1px, not +1px.
        // Cancel that offset and move the handle from the front face into the palm.
        matrices.translate((arm==HumanoidArm.RIGHT?1:-1)/16f,2.5f/16f,1.25f/16f);
    }
    private void applyParents(Part part,PoseStack matrices){
        if(!part.parent.isEmpty())applyParents(parts.get(part.parent),matrices);
        transform(part,matrices,1/16f);
    }
    private void transform(Part p,PoseStack m,float unit){
        m.translate(p.pivot[0]*unit,p.pivot[1]*unit,p.pivot[2]*unit);
        m.mulPose(Axis.ZP.rotation(p.rotation[2]*Mth.DEG_TO_RAD+p.roll));
        m.mulPose(Axis.YP.rotation(p.rotation[1]*Mth.DEG_TO_RAD+p.yaw));
        m.mulPose(Axis.XP.rotation(p.rotation[0]*Mth.DEG_TO_RAD+p.pitch));
    }
    @Override public void renderToBuffer(PoseStack matrices,VertexConsumer vertices,int light,int overlay,int color) {
        float red=(color>>16&255)/255f*tintRed,green=(color>>8&255)/255f*tintGreen,blue=(color&255)/255f*tintBlue,alpha=(color>>>24)/255f*opacity;
        matrices.pushPose();matrices.scale(1/16f,1/16f,1/16f);
        for(Part p:parts.values())if(p.parent.isEmpty())renderPart(p,matrices,vertices,light,overlay,red,green,blue,alpha);
        matrices.popPose();
    }
    /** Optional spirit eye/flame pass uses the entity emissive shader, not fullbright diffuse shading. */
    public void renderEmissive(PoseStack matrices,VertexConsumer vertices){
        emissivePass=true;
        try{renderToBuffer(matrices,vertices,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,0xFFFFFFFF);}
        finally{emissivePass=false;}
    }
    /** Entity textures are not block-atlas sprites, so advance the 2x2 flipbook here.
     * Staff OBJ materials use Minecraft's ordinary .mcmeta sprite animation instead. */
    public void renderFlames(PoseStack matrices,VertexConsumer vertices){
        flamePass=true;
        try{renderToBuffer(matrices,vertices,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,0xFFFFFFFF);}
        finally{flamePass=false;}
    }
    private void renderPart(Part p,PoseStack m,VertexConsumer v,int light,int overlay,float r,float g,float b,float a) {
        if(!p.visible)return;
        m.pushPose();transform(p,m,1);
        for(var box:p.boxes)if(box.flame==flamePass&&(!emissivePass||box.emissive))for(int i=0;i<box.faces.length;i++)face(m,v,box.faces[i],box.normals[i],box.uvs==null?null:box.uvs[i],box.material,box.flat,box.flame,box.emissive?15728880:light,overlay,r*box.color[0],g*box.color[1],b*box.color[2],a);
        for(Part child:p.children)renderPart(child,m,v,light,overlay,r,g,b,a);
        m.popPose();
    }
    private static Mesh mesh(JsonObject json) {
        int material=json.get("material").getAsInt();float[][] faces;
        if(json.has("faces")) {
            var source=json.getAsJsonArray("faces");faces=new float[source.size()][12];
            for(int i=0;i<faces.length;i++)for(int j=0;j<12;j++)faces[i][j]=source.get(i).getAsJsonArray().get(j).getAsFloat();
        } else {
            float[] p=array(json.getAsJsonArray("from")),s=array(json.getAsJsonArray("size"));
            float x=p[0],y=p[1],z=p[2],X=x+s[0],Y=y+s[1],Z=z+s[2];
            faces=new float[][]{{X,y,z,x,y,z,x,Y,z,X,Y,z},{x,y,Z,X,y,Z,X,Y,Z,x,Y,Z},{x,y,z,x,y,Z,x,Y,Z,x,Y,z},
                {X,y,Z,X,y,z,X,Y,z,X,Y,Z},{x,y,z,X,y,z,X,y,Z,x,y,Z},{x,Y,Z,X,Y,Z,X,Y,z,x,Y,z}};
        }
        float[][] normals=new float[faces.length][3];
        for(int i=0;i<faces.length;i++){
            var f=faces[i];var n=new org.joml.Vector3f(f[3]-f[0],f[4]-f[1],f[5]-f[2]).cross(f[6]-f[0],f[7]-f[1],f[8]-f[2]).normalize();
            normals[i]=new float[]{n.x,n.y,n.z};
        }
        float[][] uvs=null;
        if(json.has("uvs")){
            var source=json.getAsJsonArray("uvs");
            if(source.size()!=faces.length)throw new IllegalArgumentException("Mesh UV count does not match face count");
            uvs=new float[faces.length][8];
            for(int i=0;i<faces.length;i++)for(int j=0;j<8;j++)uvs[i][j]=source.get(i).getAsJsonArray().get(j).getAsFloat();
        }
        return new Mesh(faces,normals,uvs,material,json.has("color")?array(json.getAsJsonArray("color")):new float[]{1,1,1},json.has("flat")&&json.get("flat").getAsBoolean(),json.has("emissive")&&json.get("emissive").getAsBoolean(),json.has("flame")&&json.get("flame").getAsBoolean());
    }
    private void face(PoseStack m,VertexConsumer v,float[] xyz,float[] normal,float[] uv,int material,boolean flat,boolean flame,int light,int overlay,float r,float g,float b,float a) {
        float u=(material%4)*.25f+.012f,w=(material/4)*.25f+.012f;
        var entry=m.last();
        int frame=((int)(animationAge/2))&3;
        for(int i=0;i<4;i++){
            float U=uv==null?u+(flat?.03f:i==0||i==3?.226f:0):uv[i*2],V=uv==null?w+(flat?.03f:i>=2?.226f:0):uv[i*2+1];
            if(flame){U+=(frame&1)*.5f;V+=(frame>>1)*.5f;}
            v.addVertex(entry.pose(),xyz[i*3],xyz[i*3+1],xyz[i*3+2]).setColor(r,g,b,a).setUv(U,V).setOverlay(overlay).setLight(light).setNormal(entry,normal[0],normal[1],normal[2]);
        }
    }
}
