package gg.jos.jossentials.qol;

import gg.jos.jossentials.teleport.WarmupSettings;
import org.bukkit.configuration.file.FileConfiguration;

public final class BackSettings implements WarmupSettings {
    public final boolean trackTeleports;
    public final boolean trackDeath;
    public final boolean clearAfterUse;
    public final boolean warmupEnabled;
    public final int warmupSeconds;
    public final boolean cancelOnMove;
    public final boolean cancelOnDamage;
    public final double movementThreshold;
    public final String bypassPermission;

    public BackSettings(boolean trackTeleports, boolean trackDeath, boolean clearAfterUse,
                        boolean warmupEnabled, int warmupSeconds, boolean cancelOnMove,
                        boolean cancelOnDamage, double movementThreshold, String bypassPermission) {
        this.trackTeleports = trackTeleports;
        this.trackDeath = trackDeath;
        this.clearAfterUse = clearAfterUse;
        this.warmupEnabled = warmupEnabled;
        this.warmupSeconds = warmupSeconds;
        this.cancelOnMove = cancelOnMove;
        this.cancelOnDamage = cancelOnDamage;
        this.movementThreshold = movementThreshold;
        this.bypassPermission = bypassPermission;
    }

    public static BackSettings fromConfig(FileConfiguration config) {
        boolean trackTeleports = config.getBoolean("qol.back.track-teleports", true);
        boolean trackDeath = config.getBoolean("qol.back.track-death", true);
        boolean clearAfterUse = config.getBoolean("qol.back.clear-after-use", true);
        boolean warmupEnabled = config.getBoolean("qol.back.teleport.warmup.enabled", true);
        int warmupSeconds = config.getInt("qol.back.teleport.warmup.seconds", 3);
        boolean cancelOnMove = config.getBoolean("qol.back.teleport.cancel-on-move", true);
        boolean cancelOnDamage = config.getBoolean("qol.back.teleport.cancel-on-damage", true);
        double movementThreshold = config.getDouble("qol.back.teleport.movement-threshold", 0.1);
        String bypassPermission = config.getString("qol.back.teleport.bypass-permission", "jossentials.back.teleport.bypass");

        return new BackSettings(
            trackTeleports,
            trackDeath,
            clearAfterUse,
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
