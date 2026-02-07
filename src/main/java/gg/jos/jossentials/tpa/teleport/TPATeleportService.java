package gg.jos.jossentials.tpa.teleport;

import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.tpa.TPASettings;
import gg.jos.jossentials.teleport.WarmupTeleportManager;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public final class TPATeleportService implements Listener {
    private final Jossentials plugin;
    private final MessageDispatcher messageDispatcher;
    private final WarmupTeleportManager warmupManager;
    private volatile TPASettings settings;

    public TPATeleportService(Jossentials plugin, MessageDispatcher messageDispatcher, TPASettings settings) {
        this.plugin = plugin;
        this.messageDispatcher = messageDispatcher;
        this.settings = settings;
        this.warmupManager = new WarmupTeleportManager(plugin, () -> this.settings, this::notifyCancelled);
    }

    public void updateSettings(TPASettings settings) {
        this.settings = settings;
    }

    public void teleport(Player requester, Player target) {
        if (!requester.isOnline()) {
            return;
        }
        TPASettings current = settings;
        Location destination = target.getLocation();
        if (!current.warmupEnabled() || requester.hasPermission(current.bypassPermission())) {
            requester.teleport(destination);
            String message = plugin.getConfig().getString("messages.tpa-teleported", "<green>Teleported to <gold>%target%</gold>.");
            messageDispatcher.sendWithKey(requester, "messages.tpa-teleported", message.replace("%target%", target.getName()));
            return;
        }

        warmupManager.schedule(
            requester,
            destination,
            target.getName(),
            pending -> {
                Player currentPlayer = Bukkit.getPlayer(pending.playerId());
                if (currentPlayer == null || !currentPlayer.isOnline()) {
                    return;
                }
                String warmupMessage = plugin.getConfig().getString("messages.tpa-teleport-warmup", "<yellow>Teleporting in <gold>%seconds%</gold>s...");
                warmupMessage = warmupMessage
                    .replace("%seconds%", String.valueOf(current.warmupSeconds()))
                    .replace("%target%", String.valueOf(pending.payload()));
                messageDispatcher.sendWithKey(currentPlayer, "messages.tpa-teleport-warmup", warmupMessage);
            },
            pending -> {
                Player currentPlayer = Bukkit.getPlayer(pending.playerId());
                if (currentPlayer == null || !currentPlayer.isOnline()) {
                    return;
                }
                currentPlayer.teleport(pending.destination());
                String message = plugin.getConfig().getString("messages.tpa-teleported", "<green>Teleported to <gold>%target%</gold>.");
                messageDispatcher.sendWithKey(currentPlayer, "messages.tpa-teleported", message.replace("%target%", String.valueOf(pending.payload())));
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

    public void shutdown() {
        warmupManager.shutdown();
    }

    private void notifyCancelled(WarmupTeleportManager.PendingTeleport pending) {
        Player player = Bukkit.getPlayer(pending.playerId());
        if (player == null || !player.isOnline()) {
            return;
        }
        String message = plugin.getConfig().getString("messages.tpa-teleport-cancelled", "<red>Teleport cancelled.");
        messageDispatcher.sendWithKey(player, "messages.tpa-teleport-cancelled", message);
    }
}
