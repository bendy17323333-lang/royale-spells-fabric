package dev.royalespells.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;

/** Atlas-only upload adaptation. Original generated PNGs remain unchanged on disk.
 * Odd 627px animation frames would otherwise reduce the entire block atlas to mip 0.
 * Nearest-neighbor upload keeps the pixel-art palette; entity UVs still use the original PNG.
 */
public record FurnaceSpriteSource(ResourceLocation resource,boolean animated) implements SpriteSource {
    public static final MapCodec<FurnaceSpriteSource> CODEC=RecordCodecBuilder.mapCodec(builder->builder.group(
            ResourceLocation.CODEC.fieldOf("resource").forGetter(FurnaceSpriteSource::resource),
            Codec.BOOL.optionalFieldOf("animated",false).forGetter(FurnaceSpriteSource::animated)
    ).apply(builder,FurnaceSpriteSource::new));
    public static final SpriteSourceType TYPE=new SpriteSourceType(CODEC);

    @Override public SpriteSourceType type(){return TYPE;}

    @Override public void run(ResourceManager manager,Output output){
        var imageResource=manager.getResource(TEXTURE_ID_CONVERTER.idToFile(resource));
        if(imageResource.isEmpty()){
            LogUtils.getLogger().error("Missing furnace sprite source {}",resource);
            return;
        }
        output.add(resource,loader->{
            NativeImage upload=null;
            try(var stream=imageResource.get().open();var original=NativeImage.read(stream)){
                // 1024px sheet = 512px per frame for the four-frame fire texture.
                // This code runs once per resource reload, never once per render frame.
                int size=1024;
                upload=new NativeImage(size,size,false);
                for(int y=0;y<size;y++)for(int x=0;x<size;x++){
                    int sx=Math.min(original.getWidth()-1,(int)((x+.5)*original.getWidth()/size));
                    int sy=Math.min(original.getHeight()-1,(int)((y+.5)*original.getHeight()/size));
                    upload.setPixelRGBA(x,y,original.getPixelRGBA(sx,sy));
                }
                int frame=animated?size/2:size;
                var metadata=animated?new ResourceMetadata.Builder().put(AnimationMetadataSection.SERIALIZER,
                        new AnimationMetadataSection(java.util.List.of(),frame,frame,2,false)).build():ResourceMetadata.EMPTY;
                return new SpriteContents(resource,new FrameSize(frame,frame),upload,metadata);
            }catch(Exception error){
                if(upload!=null)upload.close();
                LogUtils.getLogger().error("Unable to prepare furnace sprite {}",resource,error);
                return null;
            }
        });
    }
}
