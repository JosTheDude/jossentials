package gg.jos.jossentials.homes.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DeleteConfirmationManager {
    private final Map<UUID, Map<Integer, Long>> confirmations = new ConcurrentHashMap<>();

    public boolean confirm(UUID playerId, int slot, long windowMillis) {
        Map<Integer, Long> playerConfirmations = confirmations.computeIfAbsent(playerId, key -> new ConcurrentHashMap<>());
        Long last = playerConfirmations.get(slot);
        if (last != null) {
            playerConfirmations.remove(slot);
            if (playerConfirmations.isEmpty()) {
                confirmations.remove(playerId);
            }
            return true;
        }
        playerConfirmations.put(slot, System.currentTimeMillis());
        return false;
    }

    public void clear(UUID playerId, int slot) {
        Map<Integer, Long> playerConfirmations = confirmations.get(playerId);
        if (playerConfirmations != null) {
            playerConfirmations.remove(slot);
            if (playerConfirmations.isEmpty()) {
                confirmations.remove(playerId);
            }
        }
    }

    public boolean isPending(UUID playerId, int slot, long windowMillis) {
        Map<Integer, Long> playerConfirmations = confirmations.get(playerId);
        if (playerConfirmations == null) {
            return false;
        }
        Long last = playerConfirmations.get(slot);
        return last != null;
    }
}
