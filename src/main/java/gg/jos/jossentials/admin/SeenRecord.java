package gg.jos.jossentials.admin;

import java.util.UUID;

public record SeenRecord(UUID playerId, String playerName, long lastLogin) {
}
