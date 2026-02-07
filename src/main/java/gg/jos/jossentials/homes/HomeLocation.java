package gg.jos.jossentials.homes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public record HomeLocation(String world, double x, double y, double z, float yaw, float pitch) {

    public static HomeLocation fromLocation(Location location) {
        return new HomeLocation(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public Location toLocation() {
        World worldObj = Bukkit.getWorld(world);
        if (worldObj == null) {
            return null;
        }
        return new Location(worldObj, x, y, z, yaw, pitch);
    }
}
