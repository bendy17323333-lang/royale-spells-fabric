package dev.royalespells.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Changes only our portrait cards; native Iron's icons and frames use their original calls. */
public final class IronCardUi {
    public enum Surface { BAR, WHEEL }
    private record Slot(int x,int y,ResourceLocation icon) {}
    private static final Map<ResourceLocation,Float> ASPECT=new HashMap<>();
    private static final List<Slot> slots=new ArrayList<>();
    private static int backgroundIndex,iconIndex;
    private static boolean currentRoyal;
    public static boolean ours(ResourceLocation icon){return icon.getNamespace().equals("royalespells") && icon.getPath().startsWith("textures/gui/spell_icons/");}
    public static void begin(Surface surface) {
        if(surface==Surface.BAR){slots.clear();backgroundIndex=0;iconIndex=0;}
        currentRoyal=false;
    }
    private static float aspect(ResourceLocation icon) {
        return ASPECT.computeIfAbsent(icon,key->{
            try(var stream=Minecraft.getInstance().getResourceManager().open(key);var image=NativeImage.read(stream)) {
                return image.getWidth()/(float)image.getHeight();
            }catch(java.io.IOException e){return 302f/363f;}
        });
    }
    private static void card(GuiGraphics gui,ResourceLocation icon,float cx,float cy,float height,boolean selected) {
        float width=height*aspect(icon);
        var pose=gui.pose();pose.pushPose();pose.translate(cx,cy,0);pose.scale(width/16,height/16,1);
        gui.blit(icon,-8,-8,0,0,16,16,16,16);pose.popPose();
        String path=icon.getPath();String id=path.substring(path.lastIndexOf('/')+1,path.length()-4);
        float cooldown=ClientMagicData.getCooldownPercent(SpellRegistry.getSpell(ResourceLocation.fromNamespaceAndPath("royalespells",id)));
        // Stay in the overlay's immediate blit pass. GuiGraphics.fill flushes a
        // RenderType and disables blending, making later native borders opaque.
        if(cooldown>0) {
            int pixels=(int)Math.ceil(16*cooldown);
            pose.pushPose();pose.translate(cx,cy,0);pose.scale(width/16,height/16,1);
            gui.blit(ResourceLocation.fromNamespaceAndPath("irons_spellbooks","textures/gui/icons.png"),-8,8-pixels,47,87,16,pixels);pose.popPose();
        }
        if(selected) {
            var oldShader=RenderSystem.getShader();
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);
            var vertex=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
            var matrix=pose.last().pose();float y=cy+height/2+1;
            vertex.addVertex(matrix,cx-2,y,0).setColor(184,245,255,255);
            vertex.addVertex(matrix,cx-2,y+1,0).setColor(184,245,255,255);
            vertex.addVertex(matrix,cx+3,y+1,0).setColor(184,245,255,255);
            vertex.addVertex(matrix,cx+3,y,0).setColor(184,245,255,255);
            BufferUploader.drawWithShader(vertex.buildOrThrow());RenderSystem.setShader(()->oldShader);
        }
    }
    public static void icon(Surface surface,GuiGraphics gui,ResourceLocation texture,int x,int y,float u,float v,int width,int height,int tw,int th) {
        currentRoyal=ours(texture);
        int index=surface==Surface.BAR?iconIndex++:0;
        if(!currentRoyal){gui.blit(texture,x,y,u,v,width,height,tw,th);return;}
        var manager=ClientMagicData.getSpellSelectionManager();
        boolean selected=surface==Surface.BAR && index==manager.getGlobalSelectionIndex();
        card(gui,texture,x+width/2f,y+height/2f,surface==Surface.WHEEL?24:22,selected);
    }
    public static void small(Surface surface,GuiGraphics gui,ResourceLocation texture,int x,int y,int u,int v,int width,int height) {
        if(surface==Surface.BAR) {
            if(u==66 && v==84 && width==22 && height==22) {
                var spells=ClientMagicData.getSpellSelectionManager().getAllSpells();
                int index=backgroundIndex++;
                if(index<spells.size()) {
                    var icon=spells.get(index).spellData.getSpell().getSpellIconResource();
                    if(ours(icon)){slots.add(new Slot(x,y,icon));return;}
                }
            }
            for(var slot:slots) {
                if(width==22 && height==22 && x==slot.x && y==slot.y)return;
                if(u==47 && v==87 && width==16 && x==slot.x+3 && y>=slot.y+2 && y<=slot.y+19)return;
            }
        } else if(surface==Surface.WHEEL && currentRoyal) {
            if(v==106 && width==32 && height==32 || u==47 && v==87 && width==16)return;
        }
        gui.blit(texture,x,y,u,v,width,height);
    }
    private IronCardUi(){}
}
