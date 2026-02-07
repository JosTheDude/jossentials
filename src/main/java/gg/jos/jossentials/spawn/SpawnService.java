package gg.jos.jossentials.spawn;

import gg.jos.jossentials.Jossentials;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;

public final class SpawnService {
    private final Jossentials plugin;

    public SpawnService(Jossentials plugin) {
        this.plugin = plugin;
    }

    public Location getSpawnLocation(World worldFallback) {
        FileConfiguration config = plugin.configs().spawn();
        String worldName = config.getString("spawn.world", "");
        if (worldName == null || worldName.isEmpty()) {
            return fallback(worldFallback);
        }
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return fallback(worldFallback);
        }
        double x = config.getDouble("spawn.x", 0.0);
        double y = config.getDouble("spawn.y", 64.0);
        double z = config.getDouble("spawn.z", 0.0);
        float yaw = (float) config.getDouble("spawn.yaw", 0.0);
        float pitch = (float) config.getDouble("spawn.pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    public boolean hasSpawn() {
        String worldName = plugin.configs().spawn().getString("spawn.world", "");
        return worldName != null && !worldName.isEmpty();
    }

    public void setSpawn(Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        FileConfiguration config = plugin.configs().spawn();
        config.set("spawn.world", location.getWorld().getName());
        config.set("spawn.x", location.getX());
        config.set("spawn.y", location.getY());
        config.set("spawn.z", location.getZ());
        config.set("spawn.yaw", location.getYaw());
        config.set("spawn.pitch", location.getPitch());
        try {
            config.save(plugin.configs().spawnFile());
        } catch (IOException ex) {
            plugin.getLogger().warning("failed to save spawn location.");
        }
    }

    private Location fallback(World worldFallback) {
        SpawnSettings settings = SpawnSettings.fromConfig(plugin.configs().spawn());
        if (!settings.fallbackToWorldSpawn || worldFallback == null) {
            return null;
        }
        return worldFallback.getSpawnLocation();
    }
}
