package dev.royalespells.client;
import org.lwjgl.opengl.GL11;
import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;
/** Opt-in regression on the actual GPU depth buffer while drawing a neck-height field. */
public final class RangeDepthAudit {
    public static boolean requested,complete;
    private static FloatBuffer before;
    private static FloatBuffer depth(){var client=net.minecraft.client.Minecraft.getInstance();var data=BufferUtils.createFloatBuffer(64*64);GL11.glReadPixels(client.getMainRenderTarget().width/2-32,client.getMainRenderTarget().height/2-32,64,64,GL11.GL_DEPTH_COMPONENT,GL11.GL_FLOAT,data);return data;}
    public static void before(){if(requested&&!complete)before=depth();}
    public static void after(){
        if(before==null)return;var after=depth();
        for(int i=0;i<before.capacity();i++)if(before.get(i)!=after.get(i))throw new IllegalStateException("Transparent spell field modified scene depth at pixel "+i);
        before=null;complete=true;System.out.println("ROYALE_RANGE_DEPTH_PRESERVED samples=4096");
    }
}
