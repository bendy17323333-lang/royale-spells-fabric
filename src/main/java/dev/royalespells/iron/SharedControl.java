package dev.royalespells.iron;
import net.minecraft.server.level.ServerPlayer;
import io.redspace.ironsspellbooks.api.magic.MagicData;
public final class SharedControl {
    public static void sync(ServerPlayer player,int ticks){var cds=MagicData.getPlayerMagicData(player).getPlayerCooldowns();for(var profile:new IronSpellProfile[]{IronSpellProfile.FREEZE,IronSpellProfile.VINES}){var spell=IronIntegration.spell(profile);if(ticks>0)cds.addCooldown(spell,ticks);else cds.removeCooldown(spell.getSpellId());}cds.syncToPlayer(player);}
    private SharedControl(){}
}
