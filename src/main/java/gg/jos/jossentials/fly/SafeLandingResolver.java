package gg.jos.jossentials.fly;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class SafeLandingResolver {
    private SafeLandingResolver() {
    }

    public static Location resolve(Location origin, int searchRadius) {
        World world = origin.getWorld();
        if (world == null) {
            return origin.clone();
        }

        int baseX = origin.getBlockX();
        int baseZ = origin.getBlockZ();
        int maxRadius = Math.max(0, searchRadius);

        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int x = baseX - radius; x <= baseX + radius; x++) {
                for (int z = baseZ - radius; z <= baseZ + radius; z++) {
                    if (radius != 0 && Math.abs(x - baseX) != radius && Math.abs(z - baseZ) != radius) {
                        continue;
                    }
                    Location landing = findSolidLanding(world, origin, x, z);
                    if (landing != null) {
                        return landing;
                    }
                }
            }
        }

        Block highest = world.getHighestBlockAt(baseX, baseZ, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        return new Location(world, baseX + 0.5D, highest.getY() + 1.0D, baseZ + 0.5D, origin.getYaw(), origin.getPitch());
    }

    private static Location findSolidLanding(World world, Location origin, int x, int z) {
        int maxY = Math.min(origin.getBlockY(), world.getMaxHeight() - 3);
        int minY = world.getMinHeight();

        for (int y = maxY; y >= minY; y--) {
            Block stand = world.getBlockAt(x, y, z);
            if (!canStandOn(stand)) {
                continue;
            }

            Block feet = world.getBlockAt(x, y + 1, z);
            Block head = world.getBlockAt(x, y + 2, z);
            if (!canOccupy(feet) || !canOccupy(head)) {
                continue;
            }

            return new Location(world, x + 0.5D, y + 1.0D, z + 0.5D, origin.getYaw(), origin.getPitch());
        }

        return null;
    }

    private static boolean canStandOn(Block block) {
        Material type = block.getType();
        return type.isSolid() && !isDangerous(type);
    }

    private static boolean canOccupy(Block block) {
        Material type = block.getType();
        return block.isPassable() && !block.isLiquid() && !isDangerous(type);
    }

    private static boolean isDangerous(Material type) {
        return switch (type) {
            case CACTUS, CAMPFIRE, FIRE, LAVA, MAGMA_BLOCK, POWDER_SNOW, SOUL_CAMPFIRE, SOUL_FIRE, SWEET_BERRY_BUSH, WITHER_ROSE -> true;
            default -> false;
        };
    }
}
