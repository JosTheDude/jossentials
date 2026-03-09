package gg.jos.jossentials.admin;

import org.bukkit.Location;
import org.bukkit.World;

public final class CoordinateResolver {
    private CoordinateResolver() {
    }

    public static Location resolve(Location base, World overrideWorld, String xInput, String yInput, String zInput) {
        if (base == null) {
            throw new IllegalArgumentException("base location is required");
        }
        World world = overrideWorld != null ? overrideWorld : base.getWorld();
        if (world == null) {
            throw new IllegalArgumentException("world is required");
        }
        double x = parse(xInput, base.getX());
        double y = parse(yInput, base.getY());
        double z = parse(zInput, base.getZ());
        return new Location(world, x, y, z, base.getYaw(), base.getPitch());
    }

    private static double parse(String input, double base) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("coordinate is missing");
        }
        String trimmed = input.trim();
        if ("~".equals(trimmed)) {
            return base;
        }
        if (trimmed.startsWith("~")) {
            return base + parseNumber(trimmed.substring(1));
        }
        return parseNumber(trimmed);
    }

    private static double parseNumber(String input) {
        if (input == null || input.isBlank()) {
            return 0.0D;
        }
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("invalid coordinate", ex);
        }
    }
}
