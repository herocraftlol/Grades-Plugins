package com.tututte.gradeplugin.tasks;

import com.tututte.gradeplugin.grade.PlayerGradeManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ExpirationTask implements Runnable {

    private final PlayerGradeManager playerGradeManager;

    public ExpirationTask(PlayerGradeManager playerGradeManager) {
        this.playerGradeManager = playerGradeManager;
    }

    @Override
    public void run() {
        // Recharger depuis la base desactive automatiquement les grades expires
        // (voir PlayerGradeManager#loadFromDatabase) et reapplique les permissions.
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerGradeManager.loadFromDatabase(player.getUniqueId());
            playerGradeManager.applyPermissions(player);
        }
    }
}
