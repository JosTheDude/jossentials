package gg.jos.jossentials.admin.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import gg.jos.jossentials.admin.AdminFeature;
import gg.jos.jossentials.util.MessageDispatcher;
import gg.jos.jossentials.util.TeleportUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("%{admin-tp-aliases}")
public final class TPCommand extends BaseCommand {
    private final AdminFeature feature;
    private final MessageDispatcher messageDispatcher;

    public TPCommand(AdminFeature feature, MessageDispatcher messageDispatcher) {
        this.feature = feature;
        this.messageDispatcher = messageDispatcher;
    }

    @Default
    @CommandPermission("jossentials.tp")
    @CommandCompletion("@players")
    public void onTeleport(Player player, OnlinePlayer target) {
        if (!feature.isEnabled()) {
            messageDispatcher.send(player, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }
        if (!TeleportUtil.teleportAndNormalizeDamageState(player, target.getPlayer().getLocation())) {
            messageDispatcher.send(player, "messages.admin-teleport-failed", "<red>Teleport failed.");
            return;
        }
        String message = feature.plugin().configs().messages().getString("messages.admin-tp-self", "<green>Teleported to <gold>%target%</gold>.");
        messageDispatcher.sendWithKey(player, "messages.admin-tp-self", message.replace("%target%", target.getPlayer().getName()));
    }

    @Default
    @CommandPermission("jossentials.tp.others")
    @CommandCompletion("@players @players")
    public void onTeleport(CommandSender sender, OnlinePlayer player, OnlinePlayer target) {
        if (!feature.isEnabled()) {
            messageDispatcher.send(sender, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }
        if (!TeleportUtil.teleportAndNormalizeDamageState(player.getPlayer(), target.getPlayer().getLocation())) {
            messageDispatcher.send(sender, "messages.admin-teleport-failed", "<red>Teleport failed.");
            return;
        }

        String senderMessage = feature.plugin().configs().messages().getString(
            "messages.admin-tp-other",
            "<green>Teleported <gold>%player%</gold> to <gold>%target%</gold>."
        );
        messageDispatcher.sendWithKey(
            sender,
            "messages.admin-tp-other",
            senderMessage.replace("%player%", player.getPlayer().getName()).replace("%target%", target.getPlayer().getName())
        );

        if (player.getPlayer() != sender) {
            String targetMessage = feature.plugin().configs().messages().getString(
                "messages.admin-tp-received",
                "<green>You were teleported to <gold>%target%</gold>."
            );
            messageDispatcher.sendWithKey(
                player.getPlayer(),
                "messages.admin-tp-received",
                targetMessage.replace("%target%", target.getPlayer().getName())
            );
        }
    }
}
