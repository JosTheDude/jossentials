package gg.jos.jossentials.admin.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import gg.jos.jossentials.admin.AdminFeature;
import gg.jos.jossentials.admin.SeenRecord;
import gg.jos.jossentials.admin.SeenFormatter;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("%{admin-seen-aliases}")
@CommandPermission("jossentials.seen")
public final class SeenCommand extends BaseCommand {
    private final AdminFeature feature;
    private final MessageDispatcher messageDispatcher;

    public SeenCommand(AdminFeature feature, MessageDispatcher messageDispatcher) {
        this.feature = feature;
        this.messageDispatcher = messageDispatcher;
    }

    @Default
    @CommandCompletion("@seenplayers")
    public void onSeen(CommandSender sender, String playerName) {
        if (!feature.isEnabled()) {
            messageDispatcher.send(sender, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }

        Player player = feature.findOnlinePlayer(playerName);
        if (player != null) {
            String message = feature.plugin().configs().messages().getString(
                "messages.admin-seen-online",
                "<green><gold>%player%</gold> is online right now."
            );
            messageDispatcher.sendWithKey(sender, "messages.admin-seen-online", message.replace("%player%", player.getName()));
            return;
        }

        if (feature.seenService() == null) {
            messageDispatcher.send(sender, "messages.admin-seen-load-failed", "<red>Could not load seen data right now.");
            return;
        }

        feature.seenService().findByName(playerName).whenComplete((record, throwable) ->
            feature.plugin().scheduler().runGlobal(() -> handleLookup(sender, playerName, record, throwable))
        );
    }

    private void handleLookup(CommandSender sender, String playerName, SeenRecord record, Throwable throwable) {
        if (sender instanceof Player player && !player.isOnline()) {
            return;
        }
        if (throwable != null) {
            messageDispatcher.send(sender, "messages.admin-seen-load-failed", "<red>Could not load seen data right now.");
            return;
        }
        if (record == null || record.lastLogin() <= 0L) {
            String message = feature.plugin().configs().messages().getString(
                "messages.admin-seen-never",
                "<yellow>No seen data is available for <gold>%player%</gold>."
            );
            messageDispatcher.sendWithKey(sender, "messages.admin-seen-never", message.replace("%player%", playerName));
            return;
        }

        String message = feature.plugin().configs().messages().getString(
            "messages.admin-seen-offline",
            "<green><gold>%player%</gold> last joined <gold>%elapsed%</gold> ago (<gold>%timestamp%</gold>)."
        );
        messageDispatcher.sendWithKey(
            sender,
            "messages.admin-seen-offline",
            message
                .replace("%player%", record.playerName())
                .replace("%elapsed%", SeenFormatter.formatElapsed(record.lastLogin()))
                .replace("%timestamp%", SeenFormatter.formatTimestamp(record.lastLogin()))
        );
    }
}
