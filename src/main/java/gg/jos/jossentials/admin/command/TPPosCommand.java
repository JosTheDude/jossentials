package gg.jos.jossentials.admin.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import gg.jos.jossentials.admin.AdminFeature;
import gg.jos.jossentials.admin.CoordinateResolver;
import gg.jos.jossentials.util.MessageDispatcher;
import gg.jos.jossentials.util.TeleportUtil;
import org.bukkit.Location;
import org.bukkit.World;
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
    public void onTeleport(Player player, String x, String y, String z, @Optional String worldName) {
        if (!feature.isEnabled()) {
            messageDispatcher.send(player, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }
        World world = resolveWorld(player, worldName);
        if (worldName != null && world == null) {
            return;
        }

        Location destination = resolveDestination(player, player.getLocation(), world, x, y, z);
        if (destination == null) {
            return;
        }
        TeleportUtil.teleportAndNormalizeDamageState(player, destination).thenAccept(success -> {
            if (!success) {
                messageDispatcher.send(player, "messages.admin-teleport-failed", "<red>Teleport failed.");
                return;
            }

            String message = feature.plugin().configs().messages().getString(
                    "messages.admin-tppos-self",
                    "<green>Teleported to <gold>%x% %y% %z%</gold> in <gold>%world%</gold>."
            );
            messageDispatcher.sendWithKey(player, "messages.admin-tppos-self", format(message, destination));
        });

    }

    @Default
    @CommandPermission("jossentials.tppos.others")
    @CommandCompletion("@players")
    public void onTeleport(CommandSender sender, OnlinePlayer target, String x, String y, String z, @Optional String worldName) {
        if (!feature.isEnabled()) {
            messageDispatcher.send(sender, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }

        World world = resolveWorld(sender, worldName);
        if (worldName != null && world == null) {
            return;
        }

        Location destination = resolveDestination(sender, target.getPlayer().getLocation(), world, x, y, z);
        if (destination == null) {
            return;
        }
        TeleportUtil.teleportAndNormalizeDamageState(target.getPlayer(), destination).thenAccept(success -> {
            if (!success) {
                messageDispatcher.send(sender, "messages.admin-teleport-failed", "<red>Teleport failed.");
                return;
            }

            String senderMessage = feature.plugin().configs().messages().getString(
                    "messages.admin-tppos-other",
                    "<green>Teleported <gold>%player%</gold> to <gold>%x% %y% %z%</gold> in <gold>%world%</gold>."
            );
            messageDispatcher.sendWithKey(
                    sender,
                    "messages.admin-tppos-other",
                    format(senderMessage, destination).replace("%player%", target.getPlayer().getName())
            );

            if (target.getPlayer() != sender) {
                String targetMessage = feature.plugin().configs().messages().getString(
                        "messages.admin-tppos-received",
                        "<green>You were teleported to <gold>%x% %y% %z%</gold> in <gold>%world%</gold>."
                );
                messageDispatcher.sendWithKey(
                        target.getPlayer(),
                        "messages.admin-tppos-received",
                        format(targetMessage, destination)
                );
            }
        });
    }

    private World resolveWorld(CommandSender sender, String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return null;
        }
        World world = feature.plugin().getServer().getWorld(worldName);
        if (world != null) {
            return world;
        }
        String message = feature.plugin().configs().messages().getString("messages.admin-world-not-found", "<red>World <gold>%world%</gold> was not found.");
        messageDispatcher.sendWithKey(sender, "messages.admin-world-not-found", message.replace("%world%", worldName));
        return null;
    }

    private Location resolveDestination(CommandSender sender, Location base, World world, String x, String y, String z) {
        try {
            return CoordinateResolver.resolve(base, world, x, y, z);
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
            .replace("%z%", formatCoord(destination.getZ()))
            .replace("%world%", destination.getWorld() != null ? destination.getWorld().getName() : "unknown");
    }

    private String formatCoord(double value) {
        String raw = String.format(Locale.US, "%.2f", value);
        while (raw.contains(".") && (raw.endsWith("0") || raw.endsWith("."))) {
            raw = raw.substring(0, raw.length() - 1);
        }
        return raw;
    }
}
