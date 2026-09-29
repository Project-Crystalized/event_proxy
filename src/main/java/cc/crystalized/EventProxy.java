package cc.crystalized;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

public class EventProxy {
    public static final MinecraftChannelIdentifier CRYSTAL_CHANNEL = MinecraftChannelIdentifier.from("crystalized:main");
    public static final MinecraftChannelIdentifier CRYSTALIZED_ESSENTIALS = MinecraftChannelIdentifier.from("crystalized:essentials");
    public final ProxyServer server;
    public static Logger logger;
    public static BanCommand ban_command;
    public static UnbanCommand unban_command;

    @Inject
    public EventProxy(ProxyServer server, Logger logger) {
        this.server = server;
        EventProxy.logger = logger;
    }

    @Subscribe
    public void onCommand(CommandExecuteEvent e) {
        if (e.getCommand().startsWith("server")) {
            if (e.getCommandSource() instanceof Player) {
                if (!(is_admin((Player) e.getCommandSource()))) {
                    e.setResult(CommandExecuteEvent.CommandResult.denied());
                }
            } else {
                e.setResult(CommandExecuteEvent.CommandResult.denied());
            }
        }
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getChannelRegistrar().register(CRYSTAL_CHANNEL);
        server.getChannelRegistrar().register(CRYSTALIZED_ESSENTIALS);

        CommandManager commandManager = server.getCommandManager();

        CommandMeta commandMetahub = commandManager.metaBuilder("hub").aliases("l", "lobby").plugin(this).build();
        commandManager.register(commandMetahub, AdminCommands.createHubCommand(server));

        CommandMeta commandMetaban = commandManager.metaBuilder("ban").plugin(this).build();
        ban_command = new BanCommand(server);
        commandManager.register(commandMetaban, ban_command);

        CommandMeta commandMetaunban = commandManager.metaBuilder("unban").plugin(this).build();
        unban_command = new UnbanCommand(server);
        commandManager.register(commandMetaunban, unban_command);

        CommandMeta commandMetaBroadcast = commandManager.metaBuilder("broadcast").plugin(this).build();
        commandManager.register(commandMetaBroadcast, AdminCommands.createBroadcastCommand(server));

        CommandMeta commandMetaMsg = commandManager.metaBuilder("msg").plugin(this).build();
        commandManager.register(commandMetaMsg, AdminCommands.createMsgCommand(server));

        CommandMeta commandMetaSend = commandManager.metaBuilder("send").plugin(this).build();
        commandManager.register(commandMetaSend, AdminCommands.createSendCommand(server));

        CommandMeta commandMetaPlayerinfo = commandManager.metaBuilder("playerinfo").plugin(this).build();
        commandManager.register(commandMetaPlayerinfo, AdminCommands.createPlayerinfoCommand(server, this));
    }


    public static boolean is_mod(Player p) {
        if (p.getUsername().equals("cooltexture")
                || p.getUsername().equals("Callum_Is_Bad")
                || p.getUsername().equals(".CallumIsBad6502")
                || p.getUsername().equals("LadyCat_")
                || p.getUsername().equals("___mira___")
                || p.getUsername().equals("MISHEROP")
                || p.getUsername().equals("Delieve")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean is_admin(Player p) {
        if (p.getUsername().equals("cooltexture")
                || p.getUsername().equals("Callum_Is_Bad")
                || p.getUsername().equals(".CallumIsBad6502")
                || p.getUsername().equals("LadyCat_")
                || p.getUsername().equals("___mira___")
                || p.getUsername().equals("MISHEROP")
                || p.getUsername().equals("Delieve")) {
            return true;
        } else {
            return false;
        }
    }
}
