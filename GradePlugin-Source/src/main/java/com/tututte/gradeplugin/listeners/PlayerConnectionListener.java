package com.tututte.gradeplugin.listeners;

import com.tututte.gradeplugin.grade.PlayerGradeManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerConnectionListener implements Listener {

    private final JavaPlugin plugin;
    private final PlayerGradeManager playerGradeManager;

    public PlayerConnectionListener(JavaPlugin plugin, PlayerGradeManager playerGradeManager) {
        this.plugin = plugin;
        this.playerGradeManager = playerGradeManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Chargement DB en asynchrone, application des permissions sur le thread principal
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            playerGradeManager.loadFromDatabase(event.getPlayer().getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (event.getPlayer().isOnline()) {
                    playerGradeManager.applyPermissions(event.getPlayer());
                }
            });
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerGradeManager.clearAttachment(event.getPlayer());
    }
}
