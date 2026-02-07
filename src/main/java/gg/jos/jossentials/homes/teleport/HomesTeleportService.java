package gg.jos.jossentials.homes.teleport;

import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.homes.HomesSettings;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import gg.jos.jossentials.teleport.WarmupTeleportManager;
import org.bukkit.Bukkit;

import java.util.UUID;

public final class HomesTeleportService implements Listener {
    private final Jossentials plugin;
    private final MessageDispatcher messageDispatcher;
    private final WarmupTeleportManager warmupManager;
    private volatile HomesSettings settings;

    public HomesTeleportService(Jossentials plugin, MessageDispatcher messageDispatcher, HomesSettings settings) {
        this.plugin = plugin;
        this.messageDispatcher = messageDispatcher;
        this.settings = settings;
        this.warmupManager = new WarmupTeleportManager(plugin, () -> this.settings, this::notifyCancelled);
    }

    public void updateSettings(HomesSettings settings) {
        this.settings = settings;
    }

    public void teleport(Player player, Location destination, int slot) {
        if (!player.isOnline()) {
            return;
        }
        HomesSettings current = settings;
        if (!current.warmupEnabled() || player.hasPermission(current.bypassPermission())) {
            player.teleport(destination);
            String message = plugin.getConfig().getString("messages.home-teleported", "<green>Teleported to home <gold>%slot%</gold>.");
            messageDispatcher.sendWithKey(player, "messages.home-teleported", message.replace("%slot%", String.valueOf(slot)));
            return;
        }

        warmupManager.schedule(
            player,
            destination,
            slot,
            pending -> {
                Player currentPlayer = Bukkit.getPlayer(pending.playerId());
                if (currentPlayer == null || !currentPlayer.isOnline()) {
                    return;
                }
                String warmupMessage = plugin.getConfig().getString("messages.home-teleport-warmup", "<yellow>Teleporting in <gold>%seconds%</gold>s...");
                warmupMessage = warmupMessage
                    .replace("%seconds%", String.valueOf(current.warmupSeconds()))
                    .replace("%slot%", String.valueOf(slot));
                messageDispatcher.sendWithKey(currentPlayer, "messages.home-teleport-warmup", warmupMessage);
            },
            pending -> {
                Player currentPlayer = Bukkit.getPlayer(pending.playerId());
                if (currentPlayer == null || !currentPlayer.isOnline()) {
                    return;
                }
                currentPlayer.teleport(pending.destination());
                String message = plugin.getConfig().getString("messages.home-teleported", "<green>Teleported to home <gold>%slot%</gold>.");
                messageDispatcher.sendWithKey(currentPlayer, "messages.home-teleported", message.replace("%slot%", String.valueOf(pending.payload())));
            }
        );
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        warmupManager.onMove(event);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        warmupManager.onDamage(event);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        warmupManager.onQuit(event);
    }

    public void cancel(UUID playerId, boolean notify) {
        warmupManager.cancel(playerId, notify);
    }

    private void notifyCancelled(WarmupTeleportManager.PendingTeleport pending) {
        Player player = Bukkit.getPlayer(pending.playerId());
        if (player == null || !player.isOnline()) {
            return;
        }
        String message = plugin.getConfig().getString("messages.home-teleport-cancelled", "<red>Teleport cancelled.");
        message = message.replace("%slot%", String.valueOf(pending.payload()));
        messageDispatcher.sendWithKey(player, "messages.home-teleport-cancelled", message);
    }
}
