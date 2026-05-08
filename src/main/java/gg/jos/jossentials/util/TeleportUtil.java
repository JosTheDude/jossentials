package gg.jos.jossentials.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public final class TeleportUtil {
    private static volatile BackLocationRecorder backLocationRecorder;

    private TeleportUtil() {
    }

    public static CompletableFuture<Boolean> teleportAndNormalizeDamageState(Player player, Location destination) {
        return teleportAndNormalizeDamageState(player, destination, true);
    }

    public static CompletableFuture<Boolean> teleportAndNormalizeDamageState(Player player, Location destination, boolean recordBackLocation) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (recordBackLocation && backLocationRecorder != null) {
            backLocationRecorder.record(player, destination);
        }
        player.teleportAsync(destination).thenAccept(success -> {
            if (success) {
                player.setInvulnerable(false);
                player.setNoDamageTicks(0);
            }
            future.complete(success);
        });

        return future;
    }

    public static void setBackLocationRecorder(BackLocationRecorder recorder) {
        backLocationRecorder = recorder;
    }

    public interface BackLocationRecorder {
        void record(Player player, Location destination);
    }
}
