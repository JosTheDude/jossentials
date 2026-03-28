package gg.jos.jossentials.fly.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import gg.jos.jossentials.fly.FlyFeature;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("%{fly-aliases}")
public final class FlyCommand extends BaseCommand {
    private final FlyFeature feature;
    private final MessageDispatcher messageDispatcher;

    public FlyCommand(FlyFeature feature, MessageDispatcher messageDispatcher) {
        this.feature = feature;
        this.messageDispatcher = messageDispatcher;
    }

    @Default
    @CommandPermission("jossentials.fly")
    @CommandCompletion("@players")
    public void onFly(CommandSender sender, @Optional String targetName) {
        if (!feature.isEnabled()) {
            messageDispatcher.send(sender, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }

        Player target = resolveTarget(sender, targetName);
        if (target == null) {
            return;
        }

        boolean enabled = !target.getAllowFlight();
        feature.setManualFlight(target, enabled);

        if (targetName == null) {
            String messageKey = enabled ? "messages.fly-enabled-self" : "messages.fly-disabled-self";
            String fallback = enabled ? "<green>Flight enabled." : "<red>Flight disabled.";
            messageDispatcher.send(sender, messageKey, fallback);
            return;
        }

        String senderMessageKey = enabled ? "messages.fly-enabled-other" : "messages.fly-disabled-other";
        String senderFallback = enabled
            ? "<green>Enabled flight for <gold>%player%</gold>."
            : "<red>Disabled flight for <gold>%player%</gold>.";
        messageDispatcher.sendWithKey(
            sender,
            senderMessageKey,
            feature.plugin().configs().messages().getString(senderMessageKey, senderFallback).replace("%player%", target.getName())
        );

        if (sender != target) {
            String targetMessageKey = enabled ? "messages.fly-enabled-received" : "messages.fly-disabled-received";
            String targetFallback = enabled ? "<green>Your flight was enabled." : "<red>Your flight was disabled.";
            messageDispatcher.send(target, targetMessageKey, targetFallback);
        }
    }

    private Player resolveTarget(CommandSender sender, String targetName) {
        if (targetName == null) {
            if (sender instanceof Player player) {
                return player;
            }
            String message = feature.plugin().configs().messages().getString(
                "messages.fly-player-required",
                "<red>Console must specify a player for /fly."
            );
            messageDispatcher.sendWithKey(sender, "messages.fly-player-required", message);
            return null;
        }

        if (!sender.hasPermission("jossentials.fly.others")) {
            messageDispatcher.send(sender, "messages.no-permission", "<red>You do not have permission.");
            return null;
        }

        Player target = feature.findOnlinePlayer(targetName);
        if (target != null) {
            return target;
        }

        String message = feature.plugin().configs().messages().getString(
            "messages.player-not-found",
            "<red>Player <gold>%player%</gold> was not found."
        );
        messageDispatcher.sendWithKey(sender, "messages.player-not-found", message.replace("%player%", targetName));
        return null;
    }
}
