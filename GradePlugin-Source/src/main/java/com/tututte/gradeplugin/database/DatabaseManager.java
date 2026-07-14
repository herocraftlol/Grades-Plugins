package com.tututte.gradeplugin.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
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

        // Chemin du fichier SQLite. Par defaut : plugins/GradePlugin/grades.db
        // (propre a CE serveur). Pour partager les grades entre plusieurs
        // serveurs Paper sur la MEME machine, mets le meme chemin absolu
        // (ex: un dossier partage) dans "sqlite.path" sur chaque serveur.
        String path = cfg.getString("sqlite.path", "plugins/GradePlugin/grades.db");
        File dbFile = new File(path);
        File parent = dbFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        // SQLite = un seul fichier verrouille en ecriture : une seule
        // connexion active suffit et evite les erreurs "database is locked".
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setPoolName("GradePlugin-Pool");
        hikariConfig.setDriverClassName("org.sqlite.JDBC");
        // Autorise SQLite a patienter (au lieu d'echouer immediatement) si un
        // autre serveur Paper ecrit au meme moment sur le meme fichier.
        hikariConfig.addDataSourceProperty("busy_timeout", "5000");

        this.dataSource = new HikariDataSource(hikariConfig);

        // Le mode WAL permet a plusieurs serveurs Paper de LIRE le fichier en
        // meme temps qu'un autre ECRIT dessus, sans se bloquer mutuellement.
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA busy_timeout=5000;");
        } catch (SQLException e) {
            plugin.getLogger().warning("Impossible d'activer le mode WAL SQLite : " + e.getMessage());
        }

        createTables();
    }

    private void createTables() {
        String grades = """
            CREATE TABLE IF NOT EXISTS grades (
                id TEXT PRIMARY KEY,
                display_name TEXT NOT NULL,
                prefix TEXT DEFAULT '',
                suffix TEXT DEFAULT '',
                color TEXT DEFAULT '&f',
                priority INTEGER DEFAULT 0,
                permissions TEXT,
                price REAL DEFAULT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )""";

        String playerGrades = """
            CREATE TABLE IF NOT EXISTS player_grades (
                uuid TEXT NOT NULL,
                grade_id TEXT NOT NULL,
                source TEXT DEFAULT 'admin',
                granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                expires_at TIMESTAMP NULL,
                active INTEGER DEFAULT 1,
                PRIMARY KEY (uuid, grade_id)
            )""";

        String pendingSync = """
            CREATE TABLE IF NOT EXISTS pending_sync (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uuid TEXT NOT NULL,
                processed INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )""";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(grades);
            stmt.execute(playerGrades);
            stmt.execute(pendingSync);
        } catch (SQLException e) {
            plugin.getLogger().severe("Impossible de creer les tables SQLite : " + e.getMessage());
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
