package gg.jos.jossentials.warps.teleport;

import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.teleport.WarmupTeleportManager;
import gg.jos.jossentials.util.MessageDispatcher;
import gg.jos.jossentials.util.SchedulerAdapter;
import gg.jos.jossentials.util.TeleportUtil;
import gg.jos.jossentials.warps.WarpsSettings;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public final class WarpsTeleportService implements Listener {
    private final Jossentials plugin;
    private final MessageDispatcher messageDispatcher;
    private final SchedulerAdapter scheduler;
    private final WarmupTeleportManager warmupManager;
    private volatile WarpsSettings settings;

    public WarpsTeleportService(Jossentials plugin, MessageDispatcher messageDispatcher, WarpsSettings settings) {
        this.plugin = plugin;
        this.messageDispatcher = messageDispatcher;
        this.scheduler = plugin.scheduler();
        this.settings = settings;
        this.warmupManager = new WarmupTeleportManager(plugin, () -> this.settings, this::notifyCancelled);
    }

    public void updateSettings(WarpsSettings settings) {
        this.settings = settings;
    }

    public void teleport(Player player, Location destination, String warpName) {
        if (!player.isOnline()) {
            return;
        }
        WarpsSettings current = settings;
        if (!current.warmupEnabled() || player.hasPermission(current.bypassPermission())) {
            scheduler.runEntity(player, () -> {
                if (TeleportUtil.teleportAndNormalizeDamageState(player, destination)) {
                    String message = plugin.configs().messages().getString("messages.warp-teleported", "<green>Teleported to <gold>%warp%</gold>.");
                    messageDispatcher.sendWithKey(player, "messages.warp-teleported", message.replace("%warp%", warpName));
                }
            });
            return;
        }

        warmupManager.schedule(
            player,
            destination,
            warpName,
            pending -> {
                Player currentPlayer = plugin.getServer().getPlayer(pending.playerId());
                if (currentPlayer == null || !currentPlayer.isOnline()) {
                    return;
                }
            },
            (pending, secondsLeft) -> {
                Player currentPlayer = plugin.getServer().getPlayer(pending.playerId());
                if (currentPlayer == null || !currentPlayer.isOnline()) {
                    return;
                }
                String warmupMessage = plugin.configs().messages().getString("messages.warp-teleport-warmup", "<yellow>Teleporting in <gold>%seconds%</gold>s...");
                warmupMessage = warmupMessage
                    .replace("%seconds%", String.valueOf(secondsLeft))
                    .replace("%warp%", String.valueOf(pending.payload()));
                messageDispatcher.sendWithKey(currentPlayer, "messages.warp-teleport-warmup", warmupMessage);
            },
            pending -> {
                Player currentPlayer = plugin.getServer().getPlayer(pending.playerId());
                if (currentPlayer == null || !currentPlayer.isOnline()) {
                    return;
                }
                if (TeleportUtil.teleportAndNormalizeDamageState(currentPlayer, pending.destination())) {
                    String message = plugin.configs().messages().getString("messages.warp-teleported", "<green>Teleported to <gold>%warp%</gold>.");
                    messageDispatcher.sendWithKey(currentPlayer, "messages.warp-teleported", message.replace("%warp%", String.valueOf(pending.payload())));
                }
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
        Player player = plugin.getServer().getPlayer(pending.playerId());
        if (player == null || !player.isOnline()) {
            return;
        }
        String message = plugin.configs().messages().getString("messages.warp-teleport-cancelled", "<red>Teleport cancelled.");
        messageDispatcher.sendWithKey(player, "messages.warp-teleport-cancelled", message.replace("%warp%", String.valueOf(pending.payload())));
    }
}
