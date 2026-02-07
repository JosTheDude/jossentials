package gg.jos.jossentials.tpa;

import gg.jos.jossentials.teleport.WarmupSettings;
import org.bukkit.configuration.file.FileConfiguration;

public final class TPASettings implements WarmupSettings {
    public final int requestExpirySeconds;
    public final boolean warmupEnabled;
    public final int warmupSeconds;
    public final boolean cancelOnMove;
    public final boolean cancelOnDamage;
    public final double movementThreshold;
    public final String bypassPermission;

    public TPASettings(int requestExpirySeconds,
                       boolean warmupEnabled, int warmupSeconds,
                       boolean cancelOnMove, boolean cancelOnDamage,
                       double movementThreshold, String bypassPermission) {
        this.requestExpirySeconds = requestExpirySeconds;
        this.warmupEnabled = warmupEnabled;
        this.warmupSeconds = warmupSeconds;
        this.cancelOnMove = cancelOnMove;
        this.cancelOnDamage = cancelOnDamage;
        this.movementThreshold = movementThreshold;
        this.bypassPermission = bypassPermission;
    }

    public static TPASettings fromConfig(FileConfiguration config) {
        int requestExpirySeconds = config.getInt("tpa.request-expiry-seconds", 60);
        boolean warmupEnabled = config.getBoolean("tpa.teleport.warmup.enabled", true);
        int warmupSeconds = config.getInt("tpa.teleport.warmup.seconds", 3);
        boolean cancelOnMove = config.getBoolean("tpa.teleport.cancel-on-move", true);
        boolean cancelOnDamage = config.getBoolean("tpa.teleport.cancel-on-damage", true);
        double movementThreshold = config.getDouble("tpa.teleport.movement-threshold", 0.1);
        String bypassPermission = config.getString("tpa.teleport.bypass-permission", "jossentials.tpa.teleport.bypass");

        return new TPASettings(
            requestExpirySeconds,
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
