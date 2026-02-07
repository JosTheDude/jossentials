package gg.jos.jossentials.homes;

import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.db.Database;
import gg.jos.jossentials.db.DatabaseType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class HomesService {
    private final Jossentials plugin;
    private final Database database;
    private final ExecutorService executor;

    public HomesService(Jossentials plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "jossentials-homes-db");
            thread.setDaemon(true);
            return thread;
        });
    }

    public CompletableFuture<Map<Integer, HomeLocation>> loadHomes(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Integer, HomeLocation> homes = new HashMap<>();
            String sql = "SELECT slot, world, x, y, z, yaw, pitch FROM jossentials_homes WHERE player_uuid = ?";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        int slot = rs.getInt("slot");
                        HomeLocation location = new HomeLocation(
                            rs.getString("world"),
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")
                        );
                        homes.put(slot, location);
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load homes for " + playerId, ex);
                throw new RuntimeException(ex);
            }
            return homes;
        }, executor);
    }

    public CompletableFuture<Boolean> setHome(UUID playerId, int slot, HomeLocation location) {
        return CompletableFuture.supplyAsync(() -> {
            String sql;
            if (database.getType() == DatabaseType.MYSQL) {
                sql = "INSERT INTO jossentials_homes (player_uuid, slot, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE world = VALUES(world), x = VALUES(x), y = VALUES(y), z = VALUES(z), yaw = VALUES(yaw), pitch = VALUES(pitch)";
            } else {
                sql = "INSERT INTO jossentials_homes (player_uuid, slot, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT(player_uuid, slot) DO UPDATE SET world = excluded.world, x = excluded.x, y = excluded.y, z = excluded.z, yaw = excluded.yaw, pitch = excluded.pitch";
            }
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                statement.setInt(2, slot);
                statement.setString(3, location.world());
                statement.setDouble(4, location.x());
                statement.setDouble(5, location.y());
                statement.setDouble(6, location.z());
                statement.setFloat(7, location.yaw());
                statement.setFloat(8, location.pitch());
                statement.executeUpdate();
                return true;
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to set home " + slot + " for " + playerId, ex);
                throw new RuntimeException(ex);
            }
        }, executor);
    }

    public CompletableFuture<Boolean> deleteHome(UUID playerId, int slot) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM jossentials_homes WHERE player_uuid = ? AND slot = ?";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerId.toString());
                statement.setInt(2, slot);
                statement.executeUpdate();
                return true;
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete home " + slot + " for " + playerId, ex);
                throw new RuntimeException(ex);
            }
        }, executor);
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
