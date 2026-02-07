package gg.jos.jossentials.spawn;

import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.teleport.WarmupTeleportManager;
import gg.jos.jossentials.util.MessageDispatcher;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class SpawnTeleportService implements Listener {
    private final Jossentials plugin;
    private final MessageDispatcher messageDispatcher;
    private final WarmupTeleportManager warmupManager;
    private volatile SpawnSettings settings;

    public SpawnTeleportService(Jossentials plugin, MessageDispatcher messageDispatcher, SpawnSettings settings) {
        this.plugin = plugin;
        this.messageDispatcher = messageDispatcher;
        this.settings = settings;
        this.warmupManager = new WarmupTeleportManager(plugin, () -> this.settings, this::notifyCancelled);
    }

    public void updateSettings(SpawnSettings settings) {
        this.settings = settings;
    }

    public void teleport(Player player, Location destination) {
        if (!player.isOnline()) {
            return;
        }
        SpawnSettings current = settings;
        if (!current.warmupEnabled() || player.hasPermission(current.bypassPermission())) {
            plugin.scheduler().runEntity(player, () -> {
                player.teleport(destination);
                String message = plugin.configs().messages().getString("messages.spawn-teleported", "<green>Teleported to spawn.");
                messageDispatcher.sendWithKey(player, "messages.spawn-teleported", message);
            });
            return;
        }

        warmupManager.schedule(
            player,
            destination,
            "spawn",
            pending -> {
            },
            (pending, secondsLeft) -> {
                Player currentPlayer = plugin.getServer().getPlayer(pending.playerId());
                if (currentPlayer == null || !currentPlayer.isOnline()) {
                    return;
                }
                String warmupMessage = plugin.configs().messages().getString("messages.spawn-teleport-warmup", "<yellow>Teleporting in <gold>%seconds%</gold>s...");
                warmupMessage = warmupMessage.replace("%seconds%", String.valueOf(secondsLeft));
                messageDispatcher.sendWithKey(currentPlayer, "messages.spawn-teleport-warmup", warmupMessage);
            },
            pending -> {
                Player currentPlayer = plugin.getServer().getPlayer(pending.playerId());
                if (currentPlayer == null || !currentPlayer.isOnline()) {
                    return;
                }
                currentPlayer.teleport(pending.destination());
                String message = plugin.configs().messages().getString("messages.spawn-teleported", "<green>Teleported to spawn.");
                messageDispatcher.sendWithKey(currentPlayer, "messages.spawn-teleported", message);
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

    public void shutdown() {
        warmupManager.shutdown();
    }

    private void notifyCancelled(WarmupTeleportManager.PendingTeleport pending) {
        Player player = plugin.getServer().getPlayer(pending.playerId());
        if (player == null || !player.isOnline()) {
            return;
        }
        String message = plugin.configs().messages().getString("messages.spawn-teleport-cancelled", "<red>Teleport cancelled.");
        messageDispatcher.sendWithKey(player, "messages.spawn-teleport-cancelled", message);
    }
}
