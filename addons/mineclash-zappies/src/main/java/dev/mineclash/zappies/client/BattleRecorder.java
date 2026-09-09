package dev.mineclash.zappies.client;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.gson.GsonBuilder;
import net.minecraft.client.*;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/** Off-screen framebuffer export for the user's explicitly requested videos.
 * No desktop/window input, screen scraping, or other application capture.
 * Timestamped frames preserve real elapsed time even if rendering drops frames.
 */
final class BattleRecorder {
    private static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"Zappies recording writer");t.setDaemon(true);return t;});
    private static final AtomicInteger pending=new AtomicInteger();
    private static final List<Map<String,Object>> frames=Collections.synchronizedList(new ArrayList<>());
    private static long lastFrame;private static int sequence;
    private static volatile boolean active,stopping,done;private static volatile Throwable failure;
    private static final Path dir=Minecraft.getInstance().gameDirectory.toPath();
    static void start(){active=true;}
    static void finish(){active=false;stopping=true;}
    static void render(RenderFrameEvent.Post event){
        try{
            if(failure!=null)throw new IllegalStateException("Video frame export failed",failure);
            if(stopping&&pending.get()==0&&!done){
                Files.writeString(dir.resolve("recorded-frames.json"),new GsonBuilder().setPrettyPrinting().create().toJson(frames));
                Files.writeString(dir.resolve("battle-complete.txt"),"complete");done=true;
            }
            if(!active||pending.get()>=3)return;
            long now=System.nanoTime();if(now-lastFrame<33_333_333)return;lastFrame=now;
            var c=Minecraft.getInstance();if(c.level==null||c.getMainRenderTarget().width<1200)return;
            Files.createDirectories(dir.resolve("video-frames"));String name=String.format(Locale.ROOT,"%06d.png",sequence++);
            long captured=System.currentTimeMillis();var pixels=Screenshot.takeScreenshot(c.getMainRenderTarget());pending.incrementAndGet();
            IO.execute(()->{try{pixels.writeToFile(dir.resolve("video-frames").resolve(name));frames.add(Map.of("file",name,"epoch_ms",captured));}
                catch(Throwable e){failure=e;}finally{pixels.close();pending.decrementAndGet();}});
        }catch(Throwable e){e.printStackTrace();System.err.println("ZAPPIES_QA_FAILURE video export");Minecraft.getInstance().stop();}
    }
}
