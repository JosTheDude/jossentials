package gg.jos.jossentials.homes;

import gg.jos.jossentials.teleport.WarmupSettings;
import org.bukkit.configuration.file.FileConfiguration;

public final class HomesSettings implements WarmupSettings {
    public final boolean deleteConfirmationEnabled;
    public final int deleteConfirmationWindowSeconds;
    public final boolean warmupEnabled;
    public final int warmupSeconds;
    public final boolean cancelOnMove;
    public final boolean cancelOnDamage;
    public final double movementThreshold;
    public final String bypassPermission;

    public HomesSettings(boolean deleteConfirmationEnabled, int deleteConfirmationWindowSeconds,
                         boolean warmupEnabled, int warmupSeconds,
                         boolean cancelOnMove, boolean cancelOnDamage,
                         double movementThreshold, String bypassPermission) {
        this.deleteConfirmationEnabled = deleteConfirmationEnabled;
        this.deleteConfirmationWindowSeconds = deleteConfirmationWindowSeconds;
        this.warmupEnabled = warmupEnabled;
        this.warmupSeconds = warmupSeconds;
        this.cancelOnMove = cancelOnMove;
        this.cancelOnDamage = cancelOnDamage;
        this.movementThreshold = movementThreshold;
        this.bypassPermission = bypassPermission;
    }

    public static HomesSettings fromConfig(FileConfiguration config) {
        boolean deleteConfirmationEnabled = config.getBoolean("homes.delete-confirmation.enabled", true);
        int deleteConfirmationWindowSeconds = config.getInt("homes.delete-confirmation.window-seconds", 5);
        boolean warmupEnabled = config.getBoolean("homes.teleport.warmup.enabled", true);
        int warmupSeconds = config.getInt("homes.teleport.warmup.seconds", 3);
        boolean cancelOnMove = config.getBoolean("homes.teleport.cancel-on-move", true);
        boolean cancelOnDamage = config.getBoolean("homes.teleport.cancel-on-damage", true);
        double movementThreshold = config.getDouble("homes.teleport.movement-threshold", 0.1);
        String bypassPermission = config.getString("homes.teleport.bypass-permission", "jossentials.homes.teleport.bypass");

        return new HomesSettings(
            deleteConfirmationEnabled,
            deleteConfirmationWindowSeconds,
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
