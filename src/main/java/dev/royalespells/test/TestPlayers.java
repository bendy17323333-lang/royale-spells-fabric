package dev.royalespells.test;

import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.CommonListenerCookie;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.UUID;

final class TestPlayers {
    static ServerPlayer create(GameTestHelper helper) {
        // Vanilla's helper omits NeoForge channel setup. Configure the supported mock network before login.
        var cookie=CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),"RoyaleTest"),false);
        var player=new ServerPlayer(helper.getLevel().getServer(),helper.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        var connection=new Connection(PacketFlow.SERVERBOUND);new EmbeddedChannel(connection);
        NetworkRegistry.configureMockConnection(connection);
        helper.getLevel().getServer().getPlayerList().placeNewPlayer(connection,player,cookie);
        player.setGameMode(GameType.CREATIVE);return player;
    }
    private TestPlayers(){}
}
