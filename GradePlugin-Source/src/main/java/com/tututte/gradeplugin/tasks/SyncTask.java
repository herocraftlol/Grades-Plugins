package com.tututte.gradeplugin.tasks;

import com.tututte.gradeplugin.grade.PlayerGradeManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

/**
 * Verifie regulierement la table pending_sync (alimentee par le site web)
 * et recharge les grades des joueurs concernes s'ils sont connectes.
 */
public class SyncTask implements Runnable {

    private final JavaPlugin plugin;
    private final PlayerGradeManager playerGradeManager;

    public SyncTask(JavaPlugin plugin, PlayerGradeManager playerGradeManager) {
        this.plugin = plugin;
        this.playerGradeManager = playerGradeManager;
    }

    @Override
    public void run() {
        List<UUID> toSync = playerGradeManager.fetchPendingSync();
        if (toSync.isEmpty()) return;

        for (UUID uuid : toSync) {
            playerGradeManager.loadFromDatabase(uuid);

            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    playerGradeManager.applyPermissions(player);
                    player.sendMessage(org.bukkit.ChatColor.GREEN + "Vos grades ont ete mis a jour !");
                }
            });
        }
    }
}
