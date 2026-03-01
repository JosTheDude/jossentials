package gg.jos.jossentials.homes.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DeleteConfirmationManager {
    private final Map<UUID, Map<Integer, Long>> confirmations = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, Long>> lastDeleteTimes = new ConcurrentHashMap<>();

    public boolean confirm(UUID playerId, int slot, long windowMillis) {
        Map<Integer, Long> playerConfirmations = confirmations.computeIfAbsent(playerId, key -> new ConcurrentHashMap<>());
        Long last = playerConfirmations.get(slot);
        if (last != null) {
            if (windowMillis > 0 && System.currentTimeMillis() - last > windowMillis) {
                playerConfirmations.remove(slot);
                if (playerConfirmations.isEmpty()) {
                    confirmations.remove(playerId);
                }
                return false;
            }
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

    public void markDeleted(UUID playerId, int slot) {
        Map<Integer, Long> playerDeletes = lastDeleteTimes.computeIfAbsent(playerId, key -> new ConcurrentHashMap<>());
        playerDeletes.put(slot, System.currentTimeMillis());
    }

    public long remainingDeleteSetDelayMillis(UUID playerId, int slot, long delayMillis) {
        if (delayMillis <= 0) {
            return 0L;
        }
        Map<Integer, Long> playerDeletes = lastDeleteTimes.get(playerId);
        if (playerDeletes == null) {
            return 0L;
        }
        Long deletedAt = playerDeletes.get(slot);
        if (deletedAt == null) {
            return 0L;
        }
        long elapsed = System.currentTimeMillis() - deletedAt;
        if (elapsed >= delayMillis) {
            playerDeletes.remove(slot);
            if (playerDeletes.isEmpty()) {
                lastDeleteTimes.remove(playerId);
            }
            return 0L;
        }
        return delayMillis - elapsed;
    }

    public void clearDeleteSetDelay(UUID playerId, int slot) {
        Map<Integer, Long> playerDeletes = lastDeleteTimes.get(playerId);
        if (playerDeletes == null) {
            return;
        }
        playerDeletes.remove(slot);
        if (playerDeletes.isEmpty()) {
            lastDeleteTimes.remove(playerId);
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
        if (windowMillis > 0 && System.currentTimeMillis() - last > windowMillis) {
            playerConfirmations.remove(slot);
            if (playerConfirmations.isEmpty()) {
                confirmations.remove(playerId);
            }
            return false;
        }
        return true;
    }
}
