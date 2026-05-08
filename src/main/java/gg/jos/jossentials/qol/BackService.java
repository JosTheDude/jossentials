package gg.jos.jossentials.qol;

import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.teleport.WarmupTeleportManager;
import gg.jos.jossentials.util.MessageDispatcher;
import gg.jos.jossentials.util.TeleportUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BackService implements Listener {
    private final Jossentials plugin;
    private final MessageDispatcher messageDispatcher;
    private final WarmupTeleportManager warmupManager;
    private final Map<UUID, Location> previousLocations = new ConcurrentHashMap<>();
    private final Set<UUID> ignoredTeleports = ConcurrentHashMap.newKeySet();

    private volatile BackSettings settings;

    public BackService(Jossentials plugin, MessageDispatcher messageDispatcher, BackSettings settings) {
        this.plugin = plugin;
        this.messageDispatcher = messageDispatcher;
        this.settings = settings;
        this.warmupManager = new WarmupTeleportManager(plugin, () -> this.settings, this::notifyCancelled);
    }

    public void updateSettings(BackSettings settings) {
        this.settings = settings;
    }

    public void teleportBack(Player player) {
        if (!player.isOnline()) {
            return;
        }

        Location destination = previousLocations.get(player.getUniqueId());
        if (destination == null || destination.getWorld() == null) {
            String message = plugin.configs().messages().getString("messages.back-not-available", "<red>You do not have a back location available.");
            messageDispatcher.sendWithKey(player, "messages.back-not-available", message);
            return;
        }

        BackSettings current = settings;
        if (!current.warmupEnabled() || player.hasPermission(current.bypassPermission())) {
            executeTeleport(player, destination.clone());
            return;
        }

        warmupManager.schedule(
            player,
            destination.clone(),
            "back",
            pending -> {
            },
            (pending, secondsLeft) -> {
                Player currentPlayer = plugin.getServer().getPlayer(pending.playerId());
                if (currentPlayer == null || !currentPlayer.isOnline()) {
                    return;
                }
                String message = plugin.configs().messages().getString("messages.back-teleport-warmup", "<yellow>Teleporting back in <gold>%seconds%</gold>s...");
                message = message.replace("%seconds%", String.valueOf(secondsLeft));
                messageDispatcher.sendWithKey(currentPlayer, "messages.back-teleport-warmup", message);
            },
            pending -> {
                Player currentPlayer = plugin.getServer().getPlayer(pending.playerId());
                if (currentPlayer == null || !currentPlayer.isOnline()) {
                    return;
                }
                executeTeleport(currentPlayer, pending.destination().clone());
            }
        );
    }

    public void recordTeleportOrigin(Player player, Location destination) {
        if (!settings.trackTeleports || destination == null || destination.getWorld() == null) {
            return;
        }
        Location origin = player.getLocation();
        if (origin.getWorld() == null || isSameLocation(origin, destination)) {
            return;
        }
        previousLocations.put(player.getUniqueId(), origin.clone());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled() || event.getTo() == null || !settings.trackTeleports) {
            return;
        }

        UUID playerId = event.getPlayer().getUniqueId();
        if (ignoredTeleports.remove(playerId)) {
            return;
        }
        if (isSameLocation(event.getFrom(), event.getTo())) {
            return;
        }
        previousLocations.put(playerId, event.getFrom().clone());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!settings.trackDeath) {
            return;
        }
        previousLocations.put(event.getPlayer().getUniqueId(), event.getPlayer().getLocation().clone());
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
        UUID playerId = event.getPlayer().getUniqueId();
        warmupManager.onQuit(event);
        previousLocations.remove(playerId);
        ignoredTeleports.remove(playerId);
    }

    public void shutdown() {
        warmupManager.shutdown();
        previousLocations.clear();
        ignoredTeleports.clear();
    }

    private void executeTeleport(Player player, Location destination) {
        UUID playerId = player.getUniqueId();
        ignoredTeleports.add(playerId);
        plugin.scheduler().runEntity(player, () ->
            TeleportUtil.teleportAndNormalizeDamageState(player, destination, false).thenAccept(success -> {
                if (!success) {
                    ignoredTeleports.remove(playerId);
                    messageDispatcher.send(player, "messages.admin-teleport-failed", "<red>Teleport failed.");
                    return;
                }
                if (settings.clearAfterUse) {
                    previousLocations.remove(playerId);
                }
                String message = plugin.configs().messages().getString("messages.back-teleported", "<green>Teleported back.");
                messageDispatcher.sendWithKey(player, "messages.back-teleported", message);
            })
        );
    }

    private void notifyCancelled(WarmupTeleportManager.PendingTeleport pending) {
        Player player = plugin.getServer().getPlayer(pending.playerId());
        if (player == null || !player.isOnline()) {
            return;
        }
        String message = plugin.configs().messages().getString("messages.back-teleport-cancelled", "<red>Teleport cancelled.");
        messageDispatcher.sendWithKey(player, "messages.back-teleport-cancelled", message);
    }

    private boolean isSameLocation(Location from, Location to) {
        if (from.getWorld() == null || to.getWorld() == null) {
            return false;
        }
        if (!from.getWorld().equals(to.getWorld())) {
            return false;
        }
        return from.distanceSquared(to) < 0.0001;
    }
}
