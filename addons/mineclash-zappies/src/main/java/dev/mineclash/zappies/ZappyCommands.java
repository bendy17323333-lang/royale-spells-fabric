package dev.mineclash.zappies;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
public final class ZappyCommands {
    public static void register(RegisterCommandsEvent event){
        var root=Commands.literal("zappies").requires(s->s.hasPermission(2));
        for(String name:new String[]{"squad","neutral"})root.then(Commands.literal(name)
            .executes(c->spawn(c.getSource(),ZappiesConfig.level(),name.equals("neutral")))
            .then(Commands.argument("level",IntegerArgumentType.integer(3,16)).executes(c->spawn(c.getSource(),IntegerArgumentType.getInteger(c,"level"),name.equals("neutral")))));
        event.getDispatcher().register(root);
    }
    private static int spawn(CommandSourceStack s,int level,boolean neutral) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var p=s.getPlayerOrException();var f=p.getLookAngle().multiply(1,0,1).normalize();
        var group=ZappySquad.spawn(p.serverLevel(),p.position().add(f.scale(4)),p.getYRot(),neutral?null:p.getUUID(),level);
        if(group.isEmpty()){s.sendFailure(Component.translatable("message.zappiesaddon.no_space"));return 0;}
        s.sendSuccess(()->Component.translatable("message.zappiesaddon.deployed",level),false);return group.size();
    }
}
