package gg.jos.jossentials.fly;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class FlySettings {
    private final List<String> aliases;

    public FlySettings(List<String> aliases) {
        this.aliases = aliases;
    }

    public static FlySettings fromConfig(FileConfiguration config) {
        return new FlySettings(aliases(config, "fly.commands.fly.aliases", List.of("fly")));
    }

    public List<String> aliases() {
        return aliases;
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
