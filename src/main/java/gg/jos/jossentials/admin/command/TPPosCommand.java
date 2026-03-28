package gg.jos.jossentials.admin.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import gg.jos.jossentials.admin.AdminFeature;
import gg.jos.jossentials.admin.CoordinateResolver;
import gg.jos.jossentials.util.MessageDispatcher;
import gg.jos.jossentials.util.TeleportUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;

@CommandAlias("%{admin-tppos-aliases}")
public final class TPPosCommand extends BaseCommand {
    private final AdminFeature feature;
    private final MessageDispatcher messageDispatcher;

    public TPPosCommand(AdminFeature feature, MessageDispatcher messageDispatcher) {
        this.feature = feature;
        this.messageDispatcher = messageDispatcher;
    }

    @Default
    @CommandPermission("jossentials.tppos")
    @CommandCompletion("@admintpposcoords @admintpposcoords @admintpposcoords @players")
    public void onTeleport(CommandSender sender, String x, String y, String z, @Optional String targetName) {
        if (!feature.isEnabled()) {
            messageDispatcher.send(sender, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }

        Player destinationPlayer = resolveTarget(sender, targetName);
        if (destinationPlayer == null) {
            return;
        }

        Location destination = resolveDestination(sender, destinationPlayer.getLocation(), x, y, z);
        if (destination == null) {
            return;
        }
        if (!TeleportUtil.teleportAndNormalizeDamageState(destinationPlayer, destination)) {
            messageDispatcher.send(sender, "messages.admin-teleport-failed", "<red>Teleport failed.");
            return;
        }

        if (targetName == null) {
            String message = feature.plugin().configs().messages().getString(
                "messages.admin-tppos-self",
                "<green>Teleported to <gold>%x% %y% %z%</gold>."
            );
            messageDispatcher.sendWithKey(sender, "messages.admin-tppos-self", format(message, destination));
            return;
        }

        String senderMessage = feature.plugin().configs().messages().getString(
            "messages.admin-tppos-other",
            "<green>Teleported <gold>%player%</gold> to <gold>%x% %y% %z%</gold>."
        );
        messageDispatcher.sendWithKey(
            sender,
            "messages.admin-tppos-other",
            format(senderMessage, destination).replace("%player%", destinationPlayer.getName())
        );

        if (sender != destinationPlayer) {
            String targetMessage = feature.plugin().configs().messages().getString(
                "messages.admin-tppos-received",
                "<green>You were teleported to <gold>%x% %y% %z%</gold>."
            );
            messageDispatcher.sendWithKey(
                destinationPlayer,
                "messages.admin-tppos-received",
                format(targetMessage, destination)
            );
        }
    }

    private Player resolveTarget(CommandSender sender, String targetName) {
        if (targetName == null) {
            if (sender instanceof Player player) {
                return player;
            }
            String message = feature.plugin().configs().messages().getString(
                "messages.admin-tppos-player-required",
                "<red>Console must specify a player for /tppos."
            );
            messageDispatcher.sendWithKey(sender, "messages.admin-tppos-player-required", message);
            return null;
        }

        if (!sender.hasPermission("jossentials.tppos.others")) {
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

    private Location resolveDestination(CommandSender sender, Location base, String x, String y, String z) {
        try {
            return CoordinateResolver.resolve(base, null, x, y, z);
        } catch (IllegalArgumentException ex) {
            String message = feature.plugin().configs().messages().getString(
                "messages.admin-invalid-coordinates",
                "<red>Invalid coordinates. Use numbers or relative coordinates like <gold>~ ~1 ~-5</gold>."
            );
            messageDispatcher.sendWithKey(sender, "messages.admin-invalid-coordinates", message);
            return null;
        }
    }

    private String format(String template, Location destination) {
        return template
            .replace("%x%", formatCoord(destination.getX()))
            .replace("%y%", formatCoord(destination.getY()))
            .replace("%z%", formatCoord(destination.getZ()));
    }

    private String formatCoord(double value) {
        String raw = String.format(Locale.US, "%.2f", value);
        while (raw.contains(".") && (raw.endsWith("0") || raw.endsWith("."))) {
            raw = raw.substring(0, raw.length() - 1);
        }
        return raw;
    }
}
