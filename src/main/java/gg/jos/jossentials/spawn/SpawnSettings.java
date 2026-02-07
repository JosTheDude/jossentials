package gg.jos.jossentials.spawn;

import gg.jos.jossentials.teleport.WarmupSettings;
import org.bukkit.configuration.file.FileConfiguration;

public final class SpawnSettings implements WarmupSettings {
    public final boolean warmupEnabled;
    public final int warmupSeconds;
    public final boolean cancelOnMove;
    public final boolean cancelOnDamage;
    public final double movementThreshold;
    public final String bypassPermission;
    public final boolean fallbackToWorldSpawn;

    public SpawnSettings(boolean warmupEnabled, int warmupSeconds, boolean cancelOnMove, boolean cancelOnDamage,
                         double movementThreshold, String bypassPermission, boolean fallbackToWorldSpawn) {
        this.warmupEnabled = warmupEnabled;
        this.warmupSeconds = warmupSeconds;
        this.cancelOnMove = cancelOnMove;
        this.cancelOnDamage = cancelOnDamage;
        this.movementThreshold = movementThreshold;
        this.bypassPermission = bypassPermission;
        this.fallbackToWorldSpawn = fallbackToWorldSpawn;
    }

    public static SpawnSettings fromConfig(FileConfiguration config) {
        boolean warmupEnabled = config.getBoolean("spawn.teleport.warmup.enabled", true);
        int warmupSeconds = config.getInt("spawn.teleport.warmup.seconds", 3);
        boolean cancelOnMove = config.getBoolean("spawn.teleport.cancel-on-move", true);
        boolean cancelOnDamage = config.getBoolean("spawn.teleport.cancel-on-damage", true);
        double movementThreshold = config.getDouble("spawn.teleport.movement-threshold", 0.1);
        String bypassPermission = config.getString("spawn.teleport.bypass-permission", "jossentials.spawn.teleport.bypass");
        boolean fallbackToWorldSpawn = config.getBoolean("spawn.fallback-to-world-spawn", true);

        return new SpawnSettings(
            warmupEnabled,
            warmupSeconds,
            cancelOnMove,
            cancelOnDamage,
            movementThreshold,
            bypassPermission,
            fallbackToWorldSpawn
        );
    }

    @Override
    public boolean warmupEnabled() {
        return warmupEnabled;
    }

    @Override
    public int warmupSeconds() {
        return warmupSeconds;
    }

    @Override
    public boolean cancelOnMove() {
        return cancelOnMove;
    }

    @Override
    public boolean cancelOnDamage() {
        return cancelOnDamage;
    }

    @Override
    public double movementThreshold() {
        return movementThreshold;
    }

    @Override
    public String bypassPermission() {
        return bypassPermission;
    }
}
