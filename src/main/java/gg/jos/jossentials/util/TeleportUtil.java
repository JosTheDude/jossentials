package gg.jos.jossentials.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public final class TeleportUtil {
    private TeleportUtil() {
    }

    public static CompletableFuture<Boolean> teleportAndNormalizeDamageState(Player player, Location destination) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        player.teleportAsync(destination).thenAccept(success -> {
            if (success) {
                player.setInvulnerable(false);
                player.setNoDamageTicks(0);
            }
            future.complete(success);
        });

        return future;
    }

    public static CompletableFuture<Boolean> teleportAndProtect(Player player, Location destination, int noDamageTicks) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        player.teleportAsync(destination).thenAccept(success -> {
            if (success) {
                player.setInvulnerable(false);
                player.setFallDistance(0.0F);
                player.setNoDamageTicks(Math.max(0, noDamageTicks));
            }
            future.complete(success);
        });

        return future;
    }
}
