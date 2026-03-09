package gg.jos.jossentials.admin.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import gg.jos.jossentials.admin.AdminFeature;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@CommandAlias("%{admin-near-aliases}")
@CommandPermission("jossentials.near")
public final class NearCommand extends BaseCommand {
    private final AdminFeature feature;
    private final MessageDispatcher messageDispatcher;

    public NearCommand(AdminFeature feature, MessageDispatcher messageDispatcher) {
        this.feature = feature;
        this.messageDispatcher = messageDispatcher;
    }

    @Default
    public void onNear(Player player, @Optional Integer radiusInput) {
        if (!feature.isEnabled()) {
            messageDispatcher.send(player, "messages.feature-disabled", "<red>This feature is disabled.");
            return;
        }

        int radius = radiusInput != null ? radiusInput : feature.settings().defaultNearRadius();
        if (radius < 1 || radius > feature.settings().maxNearRadius()) {
            String message = feature.plugin().configs().messages().getString(
                "messages.admin-near-radius-invalid",
                "<red>Radius must be between <gold>1</gold> and <gold>%max%</gold>."
            );
            messageDispatcher.sendWithKey(
                player,
                "messages.admin-near-radius-invalid",
                message.replace("%max%", String.valueOf(feature.settings().maxNearRadius()))
            );
            return;
        }

        List<PlayerDistance> nearby = player.getWorld().getPlayers().stream()
            .filter(other -> other != player)
            .map(other -> new PlayerDistance(other, player.getLocation().distance(other.getLocation())))
            .filter(entry -> entry.distance() <= radius)
            .sorted(Comparator.comparingDouble(PlayerDistance::distance))
            .toList();

        if (nearby.isEmpty()) {
            String message = feature.plugin().configs().messages().getString(
                "messages.admin-near-none",
                "<yellow>No players found within <gold>%radius%</gold> blocks."
            );
            messageDispatcher.sendWithKey(player, "messages.admin-near-none", message.replace("%radius%", String.valueOf(radius)));
            return;
        }

        String players = nearby.stream()
            .map(entry -> entry.player().getName() + " (" + String.format(Locale.US, "%.1f", entry.distance()) + "m)")
            .collect(Collectors.joining(", "));
        String message = feature.plugin().configs().messages().getString(
            "messages.admin-near-list",
            "<green>Nearby players within <gold>%radius%</gold>: <white>%players%</white>."
        );
        messageDispatcher.sendWithKey(
            player,
            "messages.admin-near-list",
            message.replace("%radius%", String.valueOf(radius)).replace("%players%", players)
        );
    }

    private record PlayerDistance(Player player, double distance) {
    }
}
