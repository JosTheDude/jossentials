package gg.jos.jossentials.command.helper;

import co.aikar.commands.BaseCommand;
import gg.jos.jossentials.feature.Feature;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.command.CommandSender;

public class JossCommand<T extends Feature> extends BaseCommand {
    protected final T feature;
    protected final MessageDispatcher messageDispatcher;

    public JossCommand(T feature, MessageDispatcher messageDispatcher) {
        this.feature = feature;
        this.messageDispatcher = messageDispatcher;
    }

    protected boolean featureEnabled(CommandSender sender) {
        if (!feature.isEnabled()) {
            messageDispatcher.send(sender, "messages.feature-disabled", "<red>This feature is disabled.");
            return false;
        }
        return true;
    }

    protected boolean permission(CommandSender sender, String perm) {
        if (!sender.hasPermission(perm)) {
            messageDispatcher.send(sender, "messages.no-permission", "<#f38ba8>You do not have permission.");
            return false;
        }
        return true;
    }

    protected boolean playerOnly(CommandSender sender) {
        if (!(sender instanceof org.bukkit.entity.Player)) {
            messageDispatcher.send(sender, "messages.player-only", "<#f38ba8>Only players can use this command.");
            return false;
        }
        return true;
    }

}
