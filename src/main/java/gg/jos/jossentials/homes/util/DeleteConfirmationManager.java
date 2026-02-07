package gg.jos.jossentials.homes.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DeleteConfirmationManager {
    private final Map<UUID, Map<Integer, Long>> confirmations = new ConcurrentHashMap<>();

    public boolean confirm(UUID playerId, int slot, long windowMillis) {
        long now = System.currentTimeMillis();
        Map<Integer, Long> playerConfirmations = confirmations.computeIfAbsent(playerId, key -> new ConcurrentHashMap<>());
        long cutoff = now - windowMillis;
        playerConfirmations.entrySet().removeIf(entry -> entry.getValue() < cutoff);
        Long last = playerConfirmations.get(slot);
        if (last != null && now - last <= windowMillis) {
            playerConfirmations.remove(slot);
            if (playerConfirmations.isEmpty()) {
                confirmations.remove(playerId);
            }
            return true;
        }
        playerConfirmations.put(slot, now);
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
        if (last == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - last > windowMillis) {
            playerConfirmations.remove(slot);
            if (playerConfirmations.isEmpty()) {
                confirmations.remove(playerId);
            }
            return false;
        }
        return true;
    }
}
