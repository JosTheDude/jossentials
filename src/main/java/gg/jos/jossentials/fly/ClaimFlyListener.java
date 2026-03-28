package gg.jos.jossentials.fly;

import gg.jos.jossentials.util.MessageDispatcher;
import gg.jos.jossentials.util.TeleportUtil;
import me.angeschossen.lands.api.flags.type.Flags;
import me.angeschossen.lands.api.land.Area;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClaimFlyListener implements Listener {
    private final FlyFeature feature;
    private final MessageDispatcher messageDispatcher;
    private final Set<UUID> claimFlyPlayers = ConcurrentHashMap.newKeySet();

    public ClaimFlyListener(FlyFeature feature, MessageDispatcher messageDispatcher) {
        this.feature = feature;
        this.messageDispatcher = messageDispatcher;
    }

    public void recheckOnlinePlayers() {
        for (Player player : feature.plugin().getServer().getOnlinePlayers()) {
            feature.plugin().scheduler().runEntityLater(player, () -> evaluate(player), 1L);
        }
    }

    public void shutdown() {
        for (UUID playerId : claimFlyPlayers) {
            Player player = feature.plugin().getServer().getPlayer(playerId);
            if (player == null || !player.isOnline() || feature.canNaturallyFly(player)) {
                continue;
            }
            player.setFlying(false);
            player.setAllowFlight(false);
            player.setFallDistance(0.0F);
        }
        claimFlyPlayers.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        feature.plugin().scheduler().runEntityLater(player, () -> evaluate(player), 1L);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getWorld() == to.getWorld()
            && (from.getBlockX() >> 4) == (to.getBlockX() >> 4)
            && (from.getBlockZ() >> 4) == (to.getBlockZ() >> 4)) {
            return;
        }

        Player player = event.getPlayer();
        feature.plugin().scheduler().runEntityLater(player, () -> evaluate(player), 1L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        claimFlyPlayers.remove(event.getPlayer().getUniqueId());
    }

    private void evaluate(Player player) {
        if (!player.isOnline()) {
            claimFlyPlayers.remove(player.getUniqueId());
            return;
        }

        FlySettings settings = feature.settings();
        if (settings == null || !settings.claimFlyEnabled() || feature.plugin().landsIntegration() == null) {
            claimFlyPlayers.remove(player.getUniqueId());
            return;
        }

        UUID playerId = player.getUniqueId();
        boolean bypass = hasBypass(player, settings);
        boolean naturalFlight = feature.canNaturallyFly(player);
        boolean permitted = player.hasPermission(settings.claimFlyPermission());
        if (!permitted || bypass || naturalFlight) {
            if (claimFlyPlayers.remove(playerId) && !bypass && !naturalFlight) {
                player.setFlying(false);
                player.setAllowFlight(false);
                player.setFallDistance(0.0F);
            }
            return;
        }

        Area area = feature.plugin().landsIntegration().getArea(player.getLocation());
        boolean allowed = area != null && area.hasRoleFlag(player, Flags.FLY, Material.AIR, false);

        if (allowed) {
            boolean firstAllowedTick = claimFlyPlayers.add(playerId);
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
            }
            if (firstAllowedTick) {
                messageDispatcher.send(player, "messages.claimfly-enabled", "<green>Claim fly enabled.");
            }
            return;
        }

        claimFlyPlayers.remove(playerId);
        if (!player.getAllowFlight()) {
            return;
        }

        boolean wasFlying = player.isFlying();
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setFallDistance(0.0F);

        if (wasFlying && settings.safeGroundEnabled()) {
            Location landing = SafeLandingResolver.resolve(player.getLocation(), settings.safeGroundSearchRadius());
            TeleportUtil.teleportAndProtect(player, landing, settings.safeGroundImmunityTicks()).thenAccept(success -> {
                if (success) {
                    feature.plugin().scheduler().runEntity(player, () ->
                        messageDispatcher.send(player, "messages.claimfly-grounded", "<yellow>You left claim fly range and were brought safely to the ground.")
                    );
                }
            });
            return;
        }

        messageDispatcher.send(player, "messages.claimfly-disabled", "<red>Claim fly disabled.");
    }

    private boolean hasBypass(Player player, FlySettings settings) {
        String permission = settings.claimFlyBypassPermission();
        return permission != null && !permission.isBlank() && player.hasPermission(permission);
    }
}
