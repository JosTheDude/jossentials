package gg.jos.jossentials.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class WarmupTeleportManager {
    private final JavaPlugin plugin;
    private final Supplier<WarmupSettings> settingsSupplier;
    private final Consumer<PendingTeleport> onCancelNotify;
    private final Map<UUID, PendingTeleport> pending = new ConcurrentHashMap<>();

    public WarmupTeleportManager(JavaPlugin plugin, Supplier<WarmupSettings> settingsSupplier,
                                 Consumer<PendingTeleport> onCancelNotify) {
        this.plugin = plugin;
        this.settingsSupplier = settingsSupplier;
        this.onCancelNotify = onCancelNotify;
    }

    public void schedule(Player player, Location destination, Object payload,
                         Consumer<PendingTeleport> onWarmupStart,
                         Consumer<PendingTeleport> onComplete) {
        cancel(player.getUniqueId(), false);
        WarmupSettings settings = settingsSupplier.get();
        PendingTeleport request = new PendingTeleport(player.getUniqueId(), destination, player.getLocation(), payload);
        pending.put(player.getUniqueId(), request);
        if (onWarmupStart != null) {
            onWarmupStart.accept(request);
        }

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingTeleport pendingTeleport = pending.remove(player.getUniqueId());
            if (pendingTeleport == null) {
                return;
            }
            Player current = Bukkit.getPlayer(pendingTeleport.playerId);
            if (current == null || !current.isOnline()) {
                return;
            }
            if (onComplete != null) {
                onComplete.accept(pendingTeleport);
            }
        }, Math.max(1, settings.warmupSeconds()) * 20L);

        request.task = task;
    }

    public void onMove(PlayerMoveEvent event) {
        PendingTeleport currentTeleport = pending.get(event.getPlayer().getUniqueId());
        if (currentTeleport == null) {
            return;
        }
        WarmupSettings current = settingsSupplier.get();
        if (!current.cancelOnMove()) {
            return;
        }
        if (currentTeleport.startLocation.getWorld() == null || event.getTo() == null) {
            return;
        }
        if (!currentTeleport.startLocation.getWorld().equals(event.getTo().getWorld())) {
            cancel(event.getPlayer().getUniqueId(), true);
            return;
        }
        double threshold = current.movementThreshold();
        if (currentTeleport.startLocation.distanceSquared(event.getTo()) > threshold * threshold) {
            cancel(event.getPlayer().getUniqueId(), true);
        }
    }

    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        WarmupSettings current = settingsSupplier.get();
        if (!current.cancelOnDamage()) {
            return;
        }
        if (pending.containsKey(player.getUniqueId())) {
            cancel(player.getUniqueId(), true);
        }
    }

    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer().getUniqueId(), false);
    }

    public void cancel(UUID playerId, boolean notify) {
        PendingTeleport current = pending.remove(playerId);
        if (current == null) {
            return;
        }
        if (current.task != null) {
            current.task.cancel();
        }
        if (notify && onCancelNotify != null) {
            onCancelNotify.accept(current);
        }
    }

    public boolean isPending(UUID playerId) {
        return pending.containsKey(playerId);
    }

    public void shutdown() {
        for (PendingTeleport pendingTeleport : pending.values()) {
            if (pendingTeleport.task != null) {
                pendingTeleport.task.cancel();
            }
        }
        pending.clear();
    }

    public static final class PendingTeleport {
        private final UUID playerId;
        private final Location destination;
        private final Location startLocation;
        private final Object payload;
        private BukkitTask task;

        private PendingTeleport(UUID playerId, Location destination, Location startLocation, Object payload) {
            this.playerId = playerId;
            this.destination = destination;
            this.startLocation = startLocation;
            this.payload = payload;
        }

        public UUID playerId() {
            return playerId;
        }

        public Location destination() {
            return destination;
        }

        public Location startLocation() {
            return startLocation;
        }

        public Object payload() {
            return payload;
        }
    }
}
