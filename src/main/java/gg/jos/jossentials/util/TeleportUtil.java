package gg.jos.jossentials.util;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class TeleportUtil {
    private TeleportUtil() {
    }

    public static boolean teleportAndNormalizeDamageState(Player player, Location destination) {
        boolean teleported = player.teleport(destination);
        if (!teleported) {
            return false;
        }
        player.setInvulnerable(false);
        player.setNoDamageTicks(0);
        return true;
    }
}
