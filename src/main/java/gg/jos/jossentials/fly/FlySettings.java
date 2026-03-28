package gg.jos.jossentials.fly;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class FlySettings {
    private final List<String> aliases;
    private final boolean claimFlyEnabled;
    private final String claimFlyPermission;
    private final String claimFlyBypassPermission;
    private final boolean safeGroundEnabled;
    private final int safeGroundSearchRadius;
    private final int safeGroundImmunityTicks;

    public FlySettings(
        List<String> aliases,
        boolean claimFlyEnabled,
        String claimFlyPermission,
        String claimFlyBypassPermission,
        boolean safeGroundEnabled,
        int safeGroundSearchRadius,
        int safeGroundImmunityTicks
    ) {
        this.aliases = aliases;
        this.claimFlyEnabled = claimFlyEnabled;
        this.claimFlyPermission = claimFlyPermission;
        this.claimFlyBypassPermission = claimFlyBypassPermission;
        this.safeGroundEnabled = safeGroundEnabled;
        this.safeGroundSearchRadius = safeGroundSearchRadius;
        this.safeGroundImmunityTicks = safeGroundImmunityTicks;
    }

    public static FlySettings fromConfig(FileConfiguration config) {
        return new FlySettings(
            aliases(config, "fly.commands.fly.aliases", List.of("fly")),
            config.getBoolean("fly.claimfly.enabled", true),
            config.getString("fly.claimfly.permission", "jossentials.fly.claim"),
            config.getString("fly.claimfly.bypass-permission", "jossentials.fly.claim.bypass"),
            config.getBoolean("fly.claimfly.safe-ground.enabled", true),
            Math.max(0, config.getInt("fly.claimfly.safe-ground.search-radius", 4)),
            Math.max(0, config.getInt("fly.claimfly.safe-ground.fall-damage-immunity-ticks", 60))
        );
    }

    public List<String> aliases() {
        return aliases;
    }

    public boolean claimFlyEnabled() {
        return claimFlyEnabled;
    }

    public String claimFlyPermission() {
        return claimFlyPermission;
    }

    public String claimFlyBypassPermission() {
        return claimFlyBypassPermission;
    }

    public boolean safeGroundEnabled() {
        return safeGroundEnabled;
    }

    public int safeGroundSearchRadius() {
        return safeGroundSearchRadius;
    }

    public int safeGroundImmunityTicks() {
        return safeGroundImmunityTicks;
    }

    public String aliasesReplacement() {
        return String.join("|", aliases);
    }

    private static List<String> aliases(FileConfiguration config, String path, List<String> defaults) {
        List<String> configured = config.getStringList(path);
        if (configured == null || configured.isEmpty()) {
            return defaults;
        }

        Set<String> aliases = new LinkedHashSet<>();
        for (String alias : configured) {
            if (alias == null) {
                continue;
            }
            String normalized = alias.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                aliases.add(normalized);
            }
        }

        if (aliases.isEmpty()) {
            return defaults;
        }
        return new ArrayList<>(aliases);
    }
}
