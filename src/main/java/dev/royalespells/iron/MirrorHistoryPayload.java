package dev.royalespells.iron;

import dev.royalespells.RoyaleSpells;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MirrorHistoryPayload(String spellId,int level) implements CustomPacketPayload {
    public static final Type<MirrorHistoryPayload> TYPE=new Type<>(RoyaleSpells.id("mirror_history"));
    public static final StreamCodec<RegistryFriendlyByteBuf,MirrorHistoryPayload> CODEC=StreamCodec.of(
        (buffer,value)->{buffer.writeUtf(value.spellId,256);buffer.writeVarInt(value.level);},
        buffer->new MirrorHistoryPayload(buffer.readUtf(256),buffer.readVarInt()));
    public Type<MirrorHistoryPayload> type(){return TYPE;}
}
