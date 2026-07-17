package com.tututte.gradeplugin.listeners;

import com.tututte.gradeplugin.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Alimente la table MySQL "players" (skin/pseudo/stats affichés sur le site
 * web via GET /boutique/players). À installer/activer sur CHAQUE serveur
 * Paper du réseau BungeeCord : comme tous écrivent dans la même base, la
 * liste et les stats sont automatiquement unifiées pour tout le réseau.
 * Toutes les requêtes SQL sont exécutées de façon asynchrone (aucun impact
 * sur le TPS du serveur).
 */
public class PlayerTrackingListener implements Listener {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final String serverName;

    // Heure de connexion de chaque joueur en ligne, pour calculer la duree
    // de session (temps de jeu) au moment de la deconnexion.
    private final Map<UUID, Long> joinTimestamps = new ConcurrentHashMap<>();

    public PlayerTrackingListener(JavaPlugin plugin, DatabaseManager databaseManager, String serverName) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.serverName = serverName;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        joinTimestamps.put(p.getUniqueId(), System.currentTimeMillis());

        runAsync(() -> {
            String sql = """
                INSERT INTO players (uuid, username, online, server, first_join, last_seen)
                VALUES (?, ?, 1, ?, NOW(), NOW())
                ON DUPLICATE KEY UPDATE
                    username = VALUES(username),
                    online = 1,
                    server = VALUES(server),
                    last_seen = NOW()
                """;
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, p.getUniqueId().toString());
                stmt.setString(2, p.getName());
                stmt.setString(3, serverName);
                stmt.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().warning("PlayerTracking (join) : " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        Long joinedAt = joinTimestamps.remove(p.getUniqueId());
        long sessionMinutes = joinedAt != null
                ? Math.max(0, (System.currentTimeMillis() - joinedAt) / 60000L)
                : 0;

        runAsync(() -> {
            String sql = """
                UPDATE players
                SET online = 0, last_seen = NOW(), playtime_minutes = playtime_minutes + ?
                WHERE uuid = ?
                """;
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, sessionMinutes);
                stmt.setString(2, p.getUniqueId().toString());
                stmt.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().warning("PlayerTracking (quit) : " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        incrementStat(victim.getUniqueId(), "deaths");

        Player killer = victim.getKiller();
        if (killer != null) {
            incrementStat(killer.getUniqueId(), "kills");
        }
    }

    private void incrementStat(UUID uuid, String column) {
        // "column" est toujours une constante interne ("kills"/"deaths"), jamais
        // une valeur venant du joueur : pas de risque d'injection SQL ici.
        runAsync(() -> {
            String sql = "UPDATE players SET " + column + " = " + column + " + 1, last_seen = NOW() WHERE uuid = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            } catch (Exception e) {
                plugin.getLogger().warning("PlayerTracking (" + column + ") : " + e.getMessage());
            }
        });
    }

    private void runAsync(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }
}
