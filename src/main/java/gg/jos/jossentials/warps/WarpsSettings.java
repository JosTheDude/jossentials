package gg.jos.jossentials.warps;

import gg.jos.jossentials.teleport.WarmupSettings;
import org.bukkit.configuration.file.FileConfiguration;

public final class WarpsSettings implements WarmupSettings {
    public final boolean warmupEnabled;
    public final int warmupSeconds;
    public final boolean cancelOnMove;
    public final boolean cancelOnDamage;
    public final double movementThreshold;
    public final String bypassPermission;

    public WarpsSettings(boolean warmupEnabled, int warmupSeconds,
                         boolean cancelOnMove, boolean cancelOnDamage,
                         double movementThreshold, String bypassPermission) {
        this.warmupEnabled = warmupEnabled;
        this.warmupSeconds = warmupSeconds;
        this.cancelOnMove = cancelOnMove;
        this.cancelOnDamage = cancelOnDamage;
        this.movementThreshold = movementThreshold;
        this.bypassPermission = bypassPermission;
    }

    public static WarpsSettings fromConfig(FileConfiguration config) {
        boolean warmupEnabled = config.getBoolean("warps.teleport.warmup.enabled", true);
        int warmupSeconds = config.getInt("warps.teleport.warmup.seconds", 3);
        boolean cancelOnMove = config.getBoolean("warps.teleport.cancel-on-move", true);
        boolean cancelOnDamage = config.getBoolean("warps.teleport.cancel-on-damage", true);
        double movementThreshold = config.getDouble("warps.teleport.movement-threshold", 0.1);
        String bypassPermission = config.getString("warps.teleport.bypass-permission", "jossentials.warp.teleport.bypass");

        return new WarpsSettings(
            warmupEnabled,
            warmupSeconds,
            cancelOnMove,
            cancelOnDamage,
            movementThreshold,
            bypassPermission
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
