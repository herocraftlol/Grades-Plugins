package com.tututte.gradeplugin.grade;

import com.tututte.gradeplugin.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerGradeManager {

    private final JavaPlugin plugin;
    private final DatabaseManager db;
    private final GradeManager gradeManager;

    // uuid -> liste de grades actifs (triee par priorite)
    private final Map<UUID, List<PlayerGrade>> cache = new ConcurrentHashMap<>();
    // uuid -> attachment de permissions actuellement appliquee
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();

    public PlayerGradeManager(JavaPlugin plugin, DatabaseManager db, GradeManager gradeManager) {
        this.plugin = plugin;
        this.db = db;
        this.gradeManager = gradeManager;
    }

    /** Charge les grades d'un joueur depuis MySQL vers le cache. A appeler de maniere asynchrone. */
    public List<PlayerGrade> loadFromDatabase(UUID uuid) {
        List<PlayerGrade> result = new ArrayList<>();
        String sql = "SELECT grade_id, source, granted_at, expires_at FROM player_grades " +
                "WHERE uuid = ? AND active = 1";

        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp expiresTs = rs.getTimestamp("expires_at");
                    Instant expires = expiresTs != null ? expiresTs.toInstant() : null;
                    Instant granted = rs.getTimestamp("granted_at").toInstant();

                    PlayerGrade pg = new PlayerGrade(rs.getString("grade_id"), rs.getString("source"), granted, expires);
                    if (!pg.isExpired()) {
                        result.add(pg);
                    } else {
                        deactivateExpired(uuid, pg.getGradeId());
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors du chargement des grades de " + uuid + " : " + e.getMessage());
        }

        result.sort(Comparator.comparingInt(pg -> {
            Grade g = gradeManager.getGrade(pg.getGradeId());
            return g != null ? -g.getPriority() : 0;
        }));

        cache.put(uuid, result);
        return result;
    }

    private void deactivateExpired(UUID uuid, String gradeId) {
        String sql = "UPDATE player_grades SET active = 0 WHERE uuid = ? AND grade_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, gradeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de la desactivation d'un grade expire : " + e.getMessage());
        }
    }

    /** Attribue un grade a un joueur (utilise par /gradeadmin give). */
    public void grantGrade(UUID uuid, String gradeId, String source, Long durationDays) {
        String sql = """
            INSERT INTO player_grades (uuid, grade_id, source, expires_at, active)
            VALUES (?, ?, ?, ?, 1)
            ON DUPLICATE KEY UPDATE
                source = VALUES(source),
                expires_at = VALUES(expires_at),
                active = 1,
                granted_at = CURRENT_TIMESTAMP
            """;

        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, gradeId);
            ps.setString(3, source);
            if (durationDays != null) {
                Timestamp expires = Timestamp.from(Instant.now().plusSeconds(durationDays * 86400));
                ps.setTimestamp(4, expires);
            } else {
                ps.setNull(4, java.sql.Types.TIMESTAMP);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de l'attribution du grade : " + e.getMessage());
            return;
        }

        loadFromDatabase(uuid);
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) applyPermissions(player);
        });
    }

    /** Retire un grade a un joueur. */
    public void revokeGrade(UUID uuid, String gradeId) {
        String sql = "UPDATE player_grades SET active = 0 WHERE uuid = ? AND grade_id = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, gradeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors du retrait du grade : " + e.getMessage());
            return;
        }

        loadFromDatabase(uuid);
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) applyPermissions(player);
        });
    }

    /** Recupere les uuid en attente de synchronisation (attribues depuis le site web). */
    public List<UUID> fetchPendingSync() {
        List<UUID> pendingUuids = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();

        String select = "SELECT id, uuid FROM pending_sync WHERE processed = 0 LIMIT 200";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(select);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt("id"));
                pendingUuids.add(UUID.fromString(rs.getString("uuid")));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de la lecture de pending_sync : " + e.getMessage());
            return pendingUuids;
        }

        if (!ids.isEmpty()) {
            String placeholders = String.join(",", ids.stream().map(i -> "?").toArray(String[]::new));
            String update = "UPDATE pending_sync SET processed = 1 WHERE id IN (" + placeholders + ")";
            try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(update)) {
                for (int i = 0; i < ids.size(); i++) {
                    ps.setInt(i + 1, ids.get(i));
                }
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Erreur lors du marquage de pending_sync : " + e.getMessage());
            }
        }

        return pendingUuids;
    }

    public List<PlayerGrade> getCached(UUID uuid) {
        return cache.getOrDefault(uuid, new ArrayList<>());
    }

    /** Le grade le plus prioritaire du joueur, ou null s'il n'en a aucun. */
    public Grade getHighestGrade(UUID uuid) {
        List<PlayerGrade> playerGrades = getCached(uuid);
        if (playerGrades.isEmpty()) return null;
        return gradeManager.getGrade(playerGrades.get(0).getGradeId());
    }

    /** Applique les permissions du grade le plus prioritaire au joueur (sur ce serveur). */
    public void applyPermissions(Player player) {
        UUID uuid = player.getUniqueId();

        PermissionAttachment old = attachments.remove(uuid);
        if (old != null) {
            player.removeAttachment(old);
        }

        Grade grade = getHighestGrade(uuid);
        if (grade == null) return;

        PermissionAttachment attachment = player.addAttachment(plugin);
        for (String perm : grade.getPermissions()) {
            attachment.setPermission(perm, true);
        }
        attachments.put(uuid, attachment);

        if (plugin.getConfig().getBoolean("handle-tablist", true)) {
            String coloredName = translateColors(grade.getColor()) + player.getName();
            player.playerListName(net.kyori.adventure.text.Component.text(coloredName));
        }
    }

    public void clearAttachment(Player player) {
        PermissionAttachment old = attachments.remove(player.getUniqueId());
        if (old != null) {
            player.removeAttachment(old);
        }
        cache.remove(player.getUniqueId());
    }

    public static String translateColors(String text) {
        if (text == null) return "";
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text);
    }
}
