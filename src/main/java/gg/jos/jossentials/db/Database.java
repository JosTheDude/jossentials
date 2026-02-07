package gg.jos.jossentials.db;

import gg.jos.jossentials.Jossentials;

import java.io.File;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

public final class Database {
    private final Jossentials plugin;
    private final DatabaseType type;
    private final HikariDataSource dataSource;

    public Database(Jossentials plugin) {
        this.plugin = plugin;
        String rawType = plugin.getConfig().getString("database.type", "sqlite");
        this.type = "mysql".equalsIgnoreCase(rawType) ? DatabaseType.MYSQL : DatabaseType.SQLITE;
        loadDriver();
        this.dataSource = createDataSource();
    }

    public DatabaseType getType() {
        return type;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void initialize() throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(createTableSql());
        }
    }

    private void loadDriver() {
        try {
            if (type == DatabaseType.MYSQL) {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } else {
                Class.forName("org.sqlite.JDBC");
            }
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Database driver not found for " + type + ". Ensure libraries are available.", ex);
        }
    }

    private HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        if (type == DatabaseType.MYSQL) {
            String host = plugin.getConfig().getString("database.mysql.host", "localhost");
            int port = plugin.getConfig().getInt("database.mysql.port", 3306);
            String database = plugin.getConfig().getString("database.mysql.database", "jossentials");
            boolean useSsl = plugin.getConfig().getBoolean("database.mysql.use-ssl", false);
            String username = plugin.getConfig().getString("database.mysql.username", "root");
            String password = plugin.getConfig().getString("database.mysql.password", "");
            String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=" + useSsl
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=UTC";
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
        } else {
            String fileName = plugin.getConfig().getString("database.sqlite.file", "homes.db");
            long busyTimeoutMs = plugin.getConfig().getLong("database.sqlite.busy-timeout-ms", 5000L);
            File file = new File(plugin.getDataFolder(), fileName);
            config.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
            config.addDataSourceProperty("busy_timeout", String.valueOf(Math.max(0L, busyTimeoutMs)));
        }

        config.setPoolName("jossentials-db");
        if (type == DatabaseType.SQLITE) {
            config.setMaximumPoolSize(1);
            config.setMinimumIdle(1);
        } else {
            config.setMaximumPoolSize(plugin.getConfig().getInt("database.pool.maximum-pool-size", 10));
            config.setMinimumIdle(plugin.getConfig().getInt("database.pool.minimum-idle", 2));
        }
        config.setConnectionTimeout(
            TimeUnit.SECONDS.toMillis(plugin.getConfig().getLong("database.pool.connection-timeout-seconds", 10))
        );
        config.setIdleTimeout(
            TimeUnit.SECONDS.toMillis(plugin.getConfig().getLong("database.pool.idle-timeout-seconds", 600))
        );
        config.setMaxLifetime(
            TimeUnit.MINUTES.toMillis(plugin.getConfig().getLong("database.pool.max-lifetime-minutes", 30))
        );

        if (type == DatabaseType.MYSQL) {
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
        }

        return new HikariDataSource(config);
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private String createTableSql() {
        if (type == DatabaseType.MYSQL) {
            return "CREATE TABLE IF NOT EXISTS jossentials_homes ("
                + "player_uuid VARCHAR(36) NOT NULL,"
                + "slot INT NOT NULL,"
                + "world VARCHAR(128) NOT NULL,"
                + "x DOUBLE NOT NULL,"
                + "y DOUBLE NOT NULL,"
                + "z DOUBLE NOT NULL,"
                + "yaw FLOAT NOT NULL,"
                + "pitch FLOAT NOT NULL,"
                + "PRIMARY KEY (player_uuid, slot)"
                + ")";
        }
        return "CREATE TABLE IF NOT EXISTS jossentials_homes ("
            + "player_uuid TEXT NOT NULL,"
            + "slot INTEGER NOT NULL,"
            + "world TEXT NOT NULL,"
            + "x REAL NOT NULL,"
            + "y REAL NOT NULL,"
            + "z REAL NOT NULL,"
            + "yaw REAL NOT NULL,"
            + "pitch REAL NOT NULL,"
            + "PRIMARY KEY (player_uuid, slot)"
            + ")";
    }
}
