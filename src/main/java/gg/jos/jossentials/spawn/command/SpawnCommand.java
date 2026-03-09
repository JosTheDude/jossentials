package gg.jos.jossentials.spawn.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import gg.jos.jossentials.spawn.SpawnFeature;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("spawn")
@CommandPermission("jossentials.spawn")
public final class SpawnCommand extends BaseCommand {
    private final SpawnFeature feature;
    private final MessageDispatcher messageDispatcher;

    public SpawnCommand(SpawnFeature feature, MessageDispatcher messageDispatcher) {
        this.feature = feature;
        this.messageDispatcher = messageDispatcher;
    }

    @Default
    public void onSpawn(Player player) {
        if (!feature.isEnabled()) {
            messageDispatcher.send(player, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }
        feature.teleport(player);
    }

    @Default
    @CommandPermission("jossentials.spawn.others")
    @CommandCompletion("@players")
    public void onSpawn(CommandSender sender, OnlinePlayer target) {
        if (!feature.isEnabled()) {
            messageDispatcher.send(sender, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }
        if (!feature.teleport(target.getPlayer(), sender)) {
            return;
        }
        if (sender == target.getPlayer()) {
            return;
        }
        String message = feature.plugin().configs().messages().getString("messages.spawn-other-sent", "<green>Sent <gold>%player%</gold> to spawn.");
        messageDispatcher.sendWithKey(sender, "messages.spawn-other-sent", message.replace("%player%", target.getPlayer().getName()));
    }
}
