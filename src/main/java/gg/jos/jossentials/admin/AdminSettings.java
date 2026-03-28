package gg.jos.jossentials.admin;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AdminSettings {
    private final List<String> tpAliases;
    private final List<String> tpPosAliases;
    private final List<String> nearAliases;
    private final List<String> flyAliases;
    private final List<String> speedAliases;
    private final List<String> seenAliases;
    private final int defaultNearRadius;
    private final int maxNearRadius;
    private final int maxSpeed;

    public AdminSettings(
        List<String> tpAliases,
        List<String> tpPosAliases,
        List<String> nearAliases,
        List<String> flyAliases,
        List<String> speedAliases,
        List<String> seenAliases,
        int defaultNearRadius,
        int maxNearRadius,
        int maxSpeed
    ) {
        this.tpAliases = tpAliases;
        this.tpPosAliases = tpPosAliases;
        this.nearAliases = nearAliases;
        this.flyAliases = flyAliases;
        this.speedAliases = speedAliases;
        this.seenAliases = seenAliases;
        this.defaultNearRadius = defaultNearRadius;
        this.maxNearRadius = maxNearRadius;
        this.maxSpeed = maxSpeed;
    }

    public static AdminSettings fromConfig(FileConfiguration config) {
        int defaultNearRadius = Math.max(1, config.getInt("admin.near.default-radius", 40));
        int maxNearRadius = Math.max(defaultNearRadius, config.getInt("admin.near.max-radius", 200));
        int maxSpeed = Math.max(1, config.getInt("admin.speed.max", 10));

        return new AdminSettings(
            aliases(config, "admin.commands.tp.aliases", List.of("tp", "teleport")),
            aliases(config, "admin.commands.tppos.aliases", List.of("tppos", "teleportpos")),
            aliases(config, "admin.commands.near.aliases", List.of("near")),
            aliases(config, "admin.commands.fly.aliases", List.of("fly")),
            aliases(config, "admin.commands.speed.aliases", List.of("speed")),
            aliases(config, "admin.commands.seen.aliases", List.of("seen", "lastseen")),
            defaultNearRadius,
            maxNearRadius,
            maxSpeed
        );
    }

    public List<String> tpAliases() {
        return tpAliases;
    }

    public List<String> tpPosAliases() {
        return tpPosAliases;
    }

    public List<String> nearAliases() {
        return nearAliases;
    }

    public List<String> flyAliases() {
        return flyAliases;
    }

    public List<String> speedAliases() {
        return speedAliases;
    }

    public List<String> seenAliases() {
        return seenAliases;
    }

    public int defaultNearRadius() {
        return defaultNearRadius;
    }

    public int maxNearRadius() {
        return maxNearRadius;
    }

    public int maxSpeed() {
        return maxSpeed;
    }

    public String tpAliasesReplacement() {
        return String.join("|", tpAliases);
    }

    public String tpPosAliasesReplacement() {
        return String.join("|", tpPosAliases);
    }

    public String nearAliasesReplacement() {
        return String.join("|", nearAliases);
    }

    public String flyAliasesReplacement() {
        return String.join("|", flyAliases);
    }

    public String speedAliasesReplacement() {
        return String.join("|", speedAliases);
    }

    public String seenAliasesReplacement() {
        return String.join("|", seenAliases);
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
