package gg.jos.jossentials.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration database;
    private FileConfiguration features;
    private FileConfiguration homes;
    private FileConfiguration tpa;
    private FileConfiguration workbenches;
    private FileConfiguration spawn;
    private FileConfiguration warps;
    private FileConfiguration messages;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        database = plugin.getConfig();
        features = plugin.getConfig();
        homes = loadConfig("homes.yml", "homes");
        tpa = loadConfig("tpa.yml", "tpa");
        workbenches = loadConfig("workbenches.yml", "workbenches");
        spawn = loadConfig("spawn.yml", "spawn");
        warps = loadConfig("warps.yml", "warps");
        messages = loadConfig("messages.yml", "messages");
    }

    public void reload() {
        plugin.reloadConfig();
        database = plugin.getConfig();
        features = plugin.getConfig();
        homes = loadConfig("homes.yml", "homes");
        tpa = loadConfig("tpa.yml", "tpa");
        workbenches = loadConfig("workbenches.yml", "workbenches");
        spawn = loadConfig("spawn.yml", "spawn");
        warps = loadConfig("warps.yml", "warps");
        messages = loadConfig("messages.yml", "messages");
    }

    public FileConfiguration database() {
        return database;
    }

    public FileConfiguration features() {
        return features;
    }

    public FileConfiguration homes() {
        return homes;
    }

    public FileConfiguration tpa() {
        return tpa;
    }

    public FileConfiguration workbenches() {
        return workbenches;
    }

    public FileConfiguration spawn() {
        return spawn;
    }

    public FileConfiguration warps() {
        return warps;
    }

    public FileConfiguration messages() {
        return messages;
    }

    public File spawnFile() {
        return new File(plugin.getDataFolder(), "spawn.yml");
    }

    private FileConfiguration loadConfig(String fileName, String rootKey) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            if (plugin.getConfig().contains(rootKey)) {
                YamlConfiguration migrated = new YamlConfiguration();
                migrated.set(rootKey, plugin.getConfig().get(rootKey));
                try {
                    migrated.save(file);
                } catch (IOException ex) {
                    plugin.getLogger().warning("failed to migrate " + rootKey + " to " + fileName + ", using defaults.");
                    plugin.saveResource(fileName, false);
                }
            } else {
                plugin.saveResource(fileName, false);
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }
}
