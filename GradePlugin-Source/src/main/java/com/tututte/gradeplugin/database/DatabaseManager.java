package com.tututte.gradeplugin.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        FileConfiguration cfg = plugin.getConfig();

        // Une seule base MySQL partagee entre TOUS tes serveurs Paper et ton
        // site web. Renseigne les memes identifiants sur chaque serveur.
        String host = cfg.getString("mysql.host", "127.0.0.1");
        int port = cfg.getInt("mysql.port", 3306);
        String database = cfg.getString("mysql.database", "grades_db");
        String user = cfg.getString("mysql.user", "grades_user");
        String password = cfg.getString("mysql.password", "");
        int poolSize = cfg.getInt("mysql.pool-size", 6);
        boolean useSSL = cfg.getBoolean("mysql.useSSL", false);

        String jdbcUrl = String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=%s&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8mb4",
                host, port, database, useSSL
        );

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(user);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(poolSize);
        hikariConfig.setPoolName("GradePlugin-Pool");
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        // Evite de garder des connexions mortes si MySQL redemarre / coupe le reseau
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setMaxLifetime(280000); // un peu moins que le wait_timeout par defaut de MySQL (28 min)

        this.dataSource = new HikariDataSource(hikariConfig);

        createTables();
    }

    private void createTables() {
        String grades = """
            CREATE TABLE IF NOT EXISTS grades (
                id VARCHAR(64) PRIMARY KEY,
                display_name VARCHAR(64) NOT NULL,
                prefix VARCHAR(64) DEFAULT '',
                suffix VARCHAR(64) DEFAULT '',
                color VARCHAR(8) DEFAULT '&f',
                priority INT DEFAULT 0,
                permissions TEXT,
                price DECIMAL(10,2) DEFAULT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""";

        String playerGrades = """
            CREATE TABLE IF NOT EXISTS player_grades (
                uuid VARCHAR(36) NOT NULL,
                grade_id VARCHAR(64) NOT NULL,
                source VARCHAR(32) DEFAULT 'admin',
                granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                expires_at TIMESTAMP NULL DEFAULT NULL,
                active TINYINT(1) DEFAULT 1,
                PRIMARY KEY (uuid, grade_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""";

        String pendingSync = """
            CREATE TABLE IF NOT EXISTS pending_sync (
                id INT AUTO_INCREMENT PRIMARY KEY,
                uuid VARCHAR(36) NOT NULL,
                processed TINYINT(1) DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(grades);
            stmt.execute(playerGrades);
            stmt.execute(pendingSync);
        } catch (SQLException e) {
            plugin.getLogger().severe("Impossible de creer les tables MySQL : " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
