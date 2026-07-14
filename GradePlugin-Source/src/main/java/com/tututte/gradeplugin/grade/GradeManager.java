package com.tututte.gradeplugin.grade;

import com.tututte.gradeplugin.database.DatabaseManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GradeManager {

    private final JavaPlugin plugin;
    private final DatabaseManager db;
    private final Map<String, Grade> grades = new ConcurrentHashMap<>();

    public GradeManager(JavaPlugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    /** Charge tous les grades depuis MySQL. Cree les grades par defaut si la table est vide. */
    public void loadAll() {
        grades.clear();

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM grades");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String perms = rs.getString("permissions");
                List<String> permList = (perms == null || perms.isBlank())
                        ? new ArrayList<>()
                        : Arrays.asList(perms.split(","));

                Double price = rs.getObject("price") != null ? rs.getDouble("price") : null;

                Grade grade = new Grade(
                        rs.getString("id"),
                        rs.getString("display_name"),
                        rs.getString("prefix"),
                        rs.getString("suffix"),
                        rs.getString("color"),
                        rs.getInt("priority"),
                        new ArrayList<>(permList),
                        price
                );
                grades.put(grade.getId(), grade);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors du chargement des grades : " + e.getMessage());
        }

        if (grades.isEmpty()) {
            createDefaultGrades();
        }
    }

    private void createDefaultGrades() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("default-grades");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String path = "default-grades." + key + ".";
            String displayName = plugin.getConfig().getString(path + "display-name", key);
            String prefix = plugin.getConfig().getString(path + "prefix", "");
            String suffix = plugin.getConfig().getString(path + "suffix", "");
            String color = plugin.getConfig().getString(path + "color", "&f");
            int priority = plugin.getConfig().getInt(path + "priority", 0);
            List<String> permissions = plugin.getConfig().getStringList(path + "permissions");
            Double price = plugin.getConfig().isSet(path + "price")
                    ? plugin.getConfig().getDouble(path + "price")
                    : null;

            Grade grade = new Grade(key, displayName, prefix, suffix, color, priority, permissions, price);
            saveGrade(grade);
            grades.put(key, grade);
        }

        plugin.getLogger().info("Grades par defaut crees en base de donnees (" + grades.size() + ").");
    }

    /** Cree ou met a jour un grade en base et dans le cache. */
    public void saveGrade(Grade grade) {
        String sql = """
            INSERT INTO grades (id, display_name, prefix, suffix, color, priority, permissions, price)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                display_name = VALUES(display_name),
                prefix = VALUES(prefix),
                suffix = VALUES(suffix),
                color = VALUES(color),
                priority = VALUES(priority),
                permissions = VALUES(permissions),
                price = VALUES(price)
            """;

        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, grade.getId());
            ps.setString(2, grade.getDisplayName());
            ps.setString(3, grade.getPrefix());
            ps.setString(4, grade.getSuffix());
            ps.setString(5, grade.getColor());
            ps.setInt(6, grade.getPriority());
            ps.setString(7, String.join(",", grade.getPermissions()));
            if (grade.getPrice() != null) {
                ps.setDouble(8, grade.getPrice());
            } else {
                ps.setNull(8, java.sql.Types.DECIMAL);
            }
            ps.executeUpdate();
            grades.put(grade.getId(), grade);
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur lors de la sauvegarde du grade " + grade.getId() + " : " + e.getMessage());
        }
    }

    public Grade getGrade(String id) {
        return grades.get(id);
    }

    public boolean exists(String id) {
        return grades.containsKey(id);
    }

    public Collection<Grade> getAllGrades() {
        return grades.values();
    }
}
