package gg.jos.jossentials.homes.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import gg.jos.jossentials.homes.feature.HomesFeature;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@CommandAlias("homes")
public final class HomesCommand extends BaseCommand {
    private final HomesFeature homesFeature;
    private final MessageDispatcher messageDispatcher;

    public HomesCommand(HomesFeature homesFeature, MessageDispatcher messageDispatcher) {
        this.homesFeature = homesFeature;
        this.messageDispatcher = messageDispatcher;
    }

    @Default
    public void onHomes(Player player) {
        if (!homesFeature.isEnabled()) {
            messageDispatcher.send(player, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }
        homesFeature.open(player);
    }

    @Default
    @CommandPermission("jossentials.homes.others")
    @CommandCompletion("@players")
    public void onHomes(Player player, String targetName) {
        if (!homesFeature.isEnabled()) {
            messageDispatcher.send(player, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }

        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        if (onlineTarget == null) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.getName().equalsIgnoreCase(targetName)) {
                    onlineTarget = onlinePlayer;
                    break;
                }
            }
        }

        if (onlineTarget != null) {
            if (onlineTarget.getUniqueId().equals(player.getUniqueId())) {
                homesFeature.open(player);
                return;
            }
            homesFeature.open(player, onlineTarget.getUniqueId(), onlineTarget.getName());
            return;
        }

        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayerIfCached(targetName);
        if (offlineTarget == null) {
            offlineTarget = Bukkit.getOfflinePlayer(targetName);
        }

        if (offlineTarget == null || (!offlineTarget.isOnline() && !offlineTarget.hasPlayedBefore())) {
            String message = homesFeature.plugin().configs().messages().getString("messages.player-not-found", "<red>Player <gold>%player%</gold> was not found.");
            messageDispatcher.sendWithKey(player, "messages.player-not-found", message.replace("%player%", targetName));
            return;
        }

        if (offlineTarget.getUniqueId().equals(player.getUniqueId())) {
            homesFeature.open(player);
            return;
        }

        String resolvedName = offlineTarget.getName() == null || offlineTarget.getName().isBlank() ? targetName : offlineTarget.getName();
        homesFeature.open(player, offlineTarget.getUniqueId(), resolvedName);
    }
}
