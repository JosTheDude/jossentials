package gg.jos.jossentials.homes;

import gg.jos.jossentials.teleport.WarmupSettings;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumSet;
import java.util.List;

public final class HomesSettings implements WarmupSettings {
    public final boolean deleteConfirmationEnabled;
    public final int deleteConfirmationWindowSeconds;
    public final boolean warmupEnabled;
    public final int warmupSeconds;
    public final boolean cancelOnMove;
    public final boolean cancelOnDamage;
    public final double movementThreshold;
    public final String bypassPermission;
    private final EnumSet<ClickType> setClicks;
    private final EnumSet<ClickType> teleportClicks;
    private final EnumSet<ClickType> deleteClicks;

    public HomesSettings(boolean deleteConfirmationEnabled, int deleteConfirmationWindowSeconds,
                         boolean warmupEnabled, int warmupSeconds,
                         boolean cancelOnMove, boolean cancelOnDamage,
                         double movementThreshold, String bypassPermission,
                         EnumSet<ClickType> setClicks, EnumSet<ClickType> teleportClicks,
                         EnumSet<ClickType> deleteClicks) {
        this.deleteConfirmationEnabled = deleteConfirmationEnabled;
        this.deleteConfirmationWindowSeconds = deleteConfirmationWindowSeconds;
        this.warmupEnabled = warmupEnabled;
        this.warmupSeconds = warmupSeconds;
        this.cancelOnMove = cancelOnMove;
        this.cancelOnDamage = cancelOnDamage;
        this.movementThreshold = movementThreshold;
        this.bypassPermission = bypassPermission;
        this.setClicks = setClicks;
        this.teleportClicks = teleportClicks;
        this.deleteClicks = deleteClicks;
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
        EnumSet<ClickType> setClicks = readClickTypes(config, "homes.gui.actions.set", EnumSet.of(ClickType.LEFT));
        EnumSet<ClickType> teleportClicks = readClickTypes(config, "homes.gui.actions.teleport", EnumSet.of(ClickType.LEFT));
        EnumSet<ClickType> deleteClicks = readClickTypes(config, "homes.gui.actions.delete", EnumSet.of(ClickType.RIGHT));

        return new HomesSettings(
            deleteConfirmationEnabled,
            deleteConfirmationWindowSeconds,
            warmupEnabled,
            warmupSeconds,
            cancelOnMove,
            cancelOnDamage,
            movementThreshold,
            bypassPermission,
            setClicks,
            teleportClicks,
            deleteClicks
        );
    }

    private static EnumSet<ClickType> readClickTypes(FileConfiguration config, String path, EnumSet<ClickType> defaults) {
        List<String> raw = config.getStringList(path);
        if (raw.isEmpty()) {
            String single = config.getString(path, null);
            if (single != null && !single.isBlank()) {
                raw = List.of(single);
            }
        }
        EnumSet<ClickType> result = EnumSet.noneOf(ClickType.class);
        for (String entry : raw) {
            if (entry == null) {
                continue;
            }
            try {
                result.add(ClickType.valueOf(entry.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result.isEmpty() ? defaults : result;
    }

    public boolean isSetClick(ClickType clickType) {
        return setClicks.contains(clickType);
    }

    public boolean isTeleportClick(ClickType clickType) {
        return teleportClicks.contains(clickType);
    }

    public boolean isDeleteClick(ClickType clickType) {
        return deleteClicks.contains(clickType);
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
