package gg.jos.jossentials.teleport;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.util.SchedulerAdapter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class WarmupTeleportManager {
    private final Jossentials plugin;
    private final SchedulerAdapter scheduler;
    private final Supplier<WarmupSettings> settingsSupplier;
    private final Consumer<PendingTeleport> onCancelNotify;
    private final Map<UUID, PendingTeleport> pending = new ConcurrentHashMap<>();

    public WarmupTeleportManager(Jossentials plugin, Supplier<WarmupSettings> settingsSupplier,
                                 Consumer<PendingTeleport> onCancelNotify) {
        this.plugin = plugin;
        this.scheduler = plugin.scheduler();
        this.settingsSupplier = settingsSupplier;
        this.onCancelNotify = onCancelNotify;
    }

    public void schedule(Player player, Location destination, Object payload,
                         Consumer<PendingTeleport> onWarmupStart,
                         BiConsumer<PendingTeleport, Integer> onWarmupTick,
                         Consumer<PendingTeleport> onComplete) {
        cancel(player.getUniqueId(), false);
        WarmupSettings settings = settingsSupplier.get();
        int totalSeconds = Math.max(1, settings.warmupSeconds());
        PendingTeleport request = new PendingTeleport(player.getUniqueId(), destination, player.getLocation(), payload, totalSeconds);
        pending.put(player.getUniqueId(), request);
        if (onWarmupStart != null) {
            onWarmupStart.accept(request);
        }

        SchedulerAdapter.TaskHandle countdownTask = scheduler.runEntityTimer(player, () -> {
            PendingTeleport pendingTeleport = pending.get(player.getUniqueId());
            if (pendingTeleport == null) {
                return;
            }
            if (pendingTeleport.remainingSeconds <= 0) {
                if (pendingTeleport.countdownTask != null) {
                    pendingTeleport.countdownTask.cancel();
                }
                return;
            }
            if (onWarmupTick != null) {
                onWarmupTick.accept(pendingTeleport, pendingTeleport.remainingSeconds);
            }
            pendingTeleport.remainingSeconds -= 1;
            if (pendingTeleport.remainingSeconds <= 0 && pendingTeleport.countdownTask != null) {
                pendingTeleport.countdownTask.cancel();
            }
        }, 0L, 20L);

        SchedulerAdapter.TaskHandle task = scheduler.runEntityLater(player, () -> {
            PendingTeleport pendingTeleport = pending.remove(player.getUniqueId());
            if (pendingTeleport == null) {
                return;
            }
            if (pendingTeleport.countdownTask != null) {
                pendingTeleport.countdownTask.cancel();
            }
            Player current = plugin.getServer().getPlayer(pendingTeleport.playerId);
            if (current == null || !current.isOnline()) {
                return;
            }
            if (onComplete != null) {
                onComplete.accept(pendingTeleport);
            }
        }, totalSeconds * 20L);

        request.task = task;
        request.countdownTask = countdownTask;
    }

    public void onMove(PlayerMoveEvent event) {
        if (event.isCancelled()) {
            return;
        }
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
        if (event.isCancelled()) {
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
        if (current.countdownTask != null) {
            current.countdownTask.cancel();
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
            if (pendingTeleport.countdownTask != null) {
                pendingTeleport.countdownTask.cancel();
            }
        }
        pending.clear();
    }

    public static final class PendingTeleport {
        private final UUID playerId;
        private final Location destination;
        private final Location startLocation;
        private final Object payload;
        private int remainingSeconds;
        private SchedulerAdapter.TaskHandle task;
        private SchedulerAdapter.TaskHandle countdownTask;

        private PendingTeleport(UUID playerId, Location destination, Location startLocation, Object payload,
                                int remainingSeconds) {
            this.playerId = playerId;
            this.destination = destination;
            this.startLocation = startLocation;
            this.payload = payload;
            this.remainingSeconds = remainingSeconds;
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
