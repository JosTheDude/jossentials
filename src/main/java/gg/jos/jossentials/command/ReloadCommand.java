package gg.jos.jossentials.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.util.ColorUtil;
import org.bukkit.command.CommandSender;

@CommandAlias("jossentials|joss|jessentials")
public final class ReloadCommand extends BaseCommand {
    private final Jossentials plugin;
    private final gg.jos.jossentials.feature.FeatureManager featureManager;
    private final gg.jos.jossentials.util.MessageDispatcher messageDispatcher;

    public ReloadCommand(Jossentials plugin, gg.jos.jossentials.feature.FeatureManager featureManager,
                         gg.jos.jossentials.util.MessageDispatcher messageDispatcher) {
        this.plugin = plugin;
        this.featureManager = featureManager;
        this.messageDispatcher = messageDispatcher;
    }

    @Subcommand("reload")
    @Default
    public void onReload(CommandSender sender) {
        if (!sender.hasPermission("jossentials.reload")) {
            if (sender instanceof org.bukkit.entity.Player player) {
                messageDispatcher.send(player, "messages.no-permission", "<red>You do not have permission.");
            } else {
                String message = plugin.getConfig().getString("messages.no-permission", "<red>You do not have permission.");
                sender.sendMessage(ColorUtil.mini(message));
            }
            return;
        }
        plugin.reloadConfig();
        messageDispatcher.reload();
        featureManager.reloadConfigured();
        if (sender instanceof org.bukkit.entity.Player player) {
            messageDispatcher.send(player, "messages.reload", "<green>Jossentials reloaded.");
        } else {
            String message = plugin.getConfig().getString("messages.reload", "<green>Jossentials reloaded.");
            sender.sendMessage(ColorUtil.mini(message));
        }
    }
}
