package gg.jos.jossentials.warps;

import gg.jos.jossentials.Jossentials;
import gg.jos.jossentials.db.Database;
import gg.jos.jossentials.db.DatabaseType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.regex.Pattern;

public final class WarpsService {
    private static final int MAX_NAME_LENGTH = 32;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    private final Jossentials plugin;
    private final Database database;
    private final ExecutorService executor;
    private final Map<String, WarpLocation> warps = new ConcurrentHashMap<>();
    private final AtomicBoolean loaded = new AtomicBoolean(false);

    public WarpsService(Jossentials plugin, Database database) {
        this.plugin = plugin;
        this.database = database;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "jossentials-warps-db");
            thread.setDaemon(true);
            return thread;
        });
    }

    public CompletableFuture<Map<String, WarpLocation>> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, WarpLocation> loadedWarps = new HashMap<>();
            String sql = "SELECT name, world, x, y, z, yaw, pitch FROM jossentials_warps";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    WarpLocation location = new WarpLocation(
                        rs.getString("world"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                    );
                    loadedWarps.put(name, location);
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load warps", ex);
                throw new RuntimeException(ex);
            }
            return loadedWarps;
        }, executor).whenComplete((loadedWarps, throwable) -> {
            if (throwable != null) {
                return;
            }
            warps.clear();
            warps.putAll(loadedWarps);
            loaded.set(true);
        });
    }

    public boolean isLoaded() {
        return loaded.get();
    }

    public boolean isValidName(String name) {
        String normalized = normalizeName(name);
        if (normalized == null || normalized.isEmpty()) {
            return false;
        }
        if (normalized.length() > MAX_NAME_LENGTH) {
            return false;
        }
        return NAME_PATTERN.matcher(normalized).matches();
    }

    public String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public WarpLocation getWarp(String name) {
        String normalized = normalizeName(name);
        if (normalized == null) {
            return null;
        }
        return warps.get(normalized);
    }

    public List<String> getWarpNames() {
        if (warps.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>(warps.keySet());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public CompletableFuture<Boolean> setWarp(String name, WarpLocation location) {
        String normalized = normalizeName(name);
        if (normalized == null || location == null) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.supplyAsync(() -> {
            String sql;
            if (database.getType() == DatabaseType.MYSQL) {
                sql = "INSERT INTO jossentials_warps (name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE world = VALUES(world), x = VALUES(x), y = VALUES(y), z = VALUES(z), yaw = VALUES(yaw), pitch = VALUES(pitch)";
            } else {
                sql = "INSERT INTO jossentials_warps (name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?) "
                    + "ON CONFLICT(name) DO UPDATE SET world = excluded.world, x = excluded.x, y = excluded.y, z = excluded.z, yaw = excluded.yaw, pitch = excluded.pitch";
            }
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, normalized);
                statement.setString(2, location.world());
                statement.setDouble(3, location.x());
                statement.setDouble(4, location.y());
                statement.setDouble(5, location.z());
                statement.setFloat(6, location.yaw());
                statement.setFloat(7, location.pitch());
                statement.executeUpdate();
                return true;
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to set warp " + normalized, ex);
                throw new RuntimeException(ex);
            }
        }, executor).whenComplete((success, throwable) -> {
            if (throwable != null) {
                return;
            }
            if (Boolean.TRUE.equals(success)) {
                warps.put(normalized, location);
            }
        });
    }

    public CompletableFuture<Boolean> deleteWarp(String name) {
        String normalized = normalizeName(name);
        if (normalized == null) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.supplyAsync(() -> {
            String sql = "DELETE FROM jossentials_warps WHERE name = ?";
            try (Connection connection = database.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, normalized);
                statement.executeUpdate();
                return true;
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete warp " + normalized, ex);
                throw new RuntimeException(ex);
            }
        }, executor).whenComplete((success, throwable) -> {
            if (throwable != null) {
                return;
            }
            if (Boolean.TRUE.equals(success)) {
                warps.remove(normalized);
            }
        });
    }

    public void shutdown() {
        executor.shutdownNow();
        warps.clear();
    }
}
