package gg.jos.jossentials.admin;

import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.db.Database;
import gg.jos.jossentials.db.DatabaseType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class SeenService {
    private final Jossentials plugin;
    private final Database database;
    private final ExecutorService executor;
    private final Map<UUID, SeenRecord> recordsByUuid = new ConcurrentHashMap<>();
    private final Map<String, SeenRecord> recordsByName = new ConcurrentHashMap<>();

    public SeenService(Jossentials plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "jossentials-seen-db");
            thread.setDaemon(true);
            return thread;
        });
    }

    public CompletableFuture<Void> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            List<SeenRecord> records = new ArrayList<>();
            String sql = "SELECT player_uuid, player_name, last_login FROM jossentials_seen";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    records.add(new SeenRecord(
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("player_name"),
                        rs.getLong("last_login")
                    ));
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load seen records", ex);
                throw new RuntimeException(ex);
            }
            return records;
        }, executor).thenAccept(records -> {
            recordsByUuid.clear();
            recordsByName.clear();
            for (SeenRecord record : records) {
                cache(record);
            }
        });
    }

    public CompletableFuture<SeenRecord> findByName(String playerName) {
        String normalized = normalize(playerName);
        if (normalized == null) {
            return CompletableFuture.completedFuture(null);
        }

        SeenRecord cached = recordsByName.get(normalized);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT player_uuid, player_name, last_login FROM jossentials_seen WHERE player_name_lower = ? ORDER BY last_login DESC LIMIT 1";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, normalized);
                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    return new SeenRecord(
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("player_name"),
                        rs.getLong("last_login")
                    );
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load seen record for " + playerName, ex);
                throw new RuntimeException(ex);
            }
        }, executor).thenApply(record -> {
            if (record != null) {
                cache(record);
            }
            return record;
        });
    }

    public CompletableFuture<Void> recordLogin(UUID playerId, String playerName, long lastLogin) {
        if (playerId == null || playerName == null || playerName.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }

        SeenRecord record = new SeenRecord(playerId, playerName, lastLogin);
        cache(record);

        return CompletableFuture.runAsync(() -> {
            String sql;
            if (database.getType() == DatabaseType.MYSQL) {
                sql = "INSERT INTO jossentials_seen (player_uuid, player_name, player_name_lower, last_login) VALUES (?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), player_name_lower = VALUES(player_name_lower), last_login = VALUES(last_login)";
            } else {
                sql = "INSERT INTO jossentials_seen (player_uuid, player_name, player_name_lower, last_login) VALUES (?, ?, ?, ?) "
                    + "ON CONFLICT(player_uuid) DO UPDATE SET player_name = excluded.player_name, player_name_lower = excluded.player_name_lower, last_login = excluded.last_login";
            }
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                statement.setString(2, playerName);
                statement.setString(3, normalize(playerName));
                statement.setLong(4, lastLogin);
                statement.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save seen record for " + playerName, ex);
                throw new RuntimeException(ex);
            }
        }, executor);
    }

    public List<String> names() {
        List<String> names = new ArrayList<>();
        for (SeenRecord record : recordsByUuid.values()) {
            names.add(record.playerName());
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public void shutdown() {
        executor.shutdownNow();
        recordsByUuid.clear();
        recordsByName.clear();
    }

    private void cache(SeenRecord record) {
        recordsByUuid.put(record.playerId(), record);
        String normalized = normalize(record.playerName());
        SeenRecord existing = normalized != null ? recordsByName.get(normalized) : null;
        if (normalized != null && (existing == null || record.lastLogin() >= existing.lastLogin())) {
            recordsByName.put(normalized, record);
        }
    }

    private String normalize(String input) {
        if (input == null) {
            return null;
        }
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
