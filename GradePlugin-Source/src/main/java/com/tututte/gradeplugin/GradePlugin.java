package com.tututte.gradeplugin;

import com.tututte.gradeplugin.commands.GradeAdminCommand;
import com.tututte.gradeplugin.commands.GradeCommand;
import com.tututte.gradeplugin.database.DatabaseManager;
import com.tututte.gradeplugin.grade.GradeManager;
import com.tututte.gradeplugin.grade.PlayerGradeManager;
import com.tututte.gradeplugin.listeners.ChatFormatListener;
import com.tututte.gradeplugin.listeners.PlayerConnectionListener;
import com.tututte.gradeplugin.tasks.ExpirationTask;
import com.tututte.gradeplugin.tasks.SyncTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class GradePlugin extends JavaPlugin {

    private DatabaseManager databaseManager;
    private GradeManager gradeManager;
    private PlayerGradeManager playerGradeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        gradeManager = new GradeManager(this, databaseManager);
        gradeManager.loadAll();

        playerGradeManager = new PlayerGradeManager(this, databaseManager, gradeManager);

        registerCommands();
        registerListeners();
        scheduleTasks();

        // Charge les grades des joueurs deja connectes (utile en cas de /reload)
        for (var player : Bukkit.getOnlinePlayers()) {
            playerGradeManager.loadFromDatabase(player.getUniqueId());
            playerGradeManager.applyPermissions(player);
        }

        getLogger().info("GradePlugin active avec " + gradeManager.getAllGrades().size() + " grade(s) charge(s).");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    private void registerCommands() {
        getCommand("grade").setExecutor(new GradeCommand(gradeManager, playerGradeManager));
        getCommand("gradeadmin").setExecutor(new GradeAdminCommand(this, gradeManager, playerGradeManager));
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(this, playerGradeManager), this);
        if (getConfig().getBoolean("handle-chat-format", true)) {
            Bukkit.getPluginManager().registerEvents(new ChatFormatListener(this, playerGradeManager), this);
        }
    }

    private void scheduleTasks() {
        long syncInterval = getConfig().getLong("sync-interval-seconds", 10) * 20L;
        long expirationInterval = getConfig().getLong("expiration-check-interval-seconds", 60) * 20L;

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, new SyncTask(this, playerGradeManager), syncInterval, syncInterval);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, new ExpirationTask(playerGradeManager), expirationInterval, expirationInterval);
    }

    public GradeManager getGradeManager() {
        return gradeManager;
    }

    public PlayerGradeManager getPlayerGradeManager() {
        return playerGradeManager;
    }
}
