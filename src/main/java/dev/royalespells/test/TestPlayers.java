package dev.royalespells.test;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;
final class TestPlayers {
    static ServerPlayer create(GameTestHelper helper){
        var player=new ServerPlayer(helper.getLevel().getServer(),helper.getLevel(),new GameProfile(UUID.randomUUID(),"RoyaleTest"));
        var connection=new Connection(PacketFlow.SERVERBOUND);new EmbeddedChannel(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection,player);
        player.setGameMode(GameType.CREATIVE);return player;
    }
    private TestPlayers(){}
}
