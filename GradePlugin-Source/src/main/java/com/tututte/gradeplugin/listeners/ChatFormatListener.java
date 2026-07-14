package com.tututte.gradeplugin.listeners;

import com.tututte.gradeplugin.grade.Grade;
import com.tututte.gradeplugin.grade.PlayerGradeManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class ChatFormatListener implements Listener {

    private final JavaPlugin plugin;
    private final PlayerGradeManager playerGradeManager;

    public ChatFormatListener(JavaPlugin plugin, PlayerGradeManager playerGradeManager) {
        this.plugin = plugin;
        this.playerGradeManager = playerGradeManager;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("handle-chat-format", true)) return;

        Grade grade = playerGradeManager.getHighestGrade(event.getPlayer().getUniqueId());

        String prefix = grade != null ? PlayerGradeManager.translateColors(grade.getPrefix()) : "";
        String suffix = grade != null ? PlayerGradeManager.translateColors(grade.getSuffix()) : "";

        String format = plugin.getConfig().getString("chat-format", "%prefix%%player%%suffix% &7: &f%message%");
        String message = LegacyComponentSerializer.legacySection().serialize(event.message());

        String finalLine = format
                .replace("%prefix%", prefix)
                .replace("%player%", event.getPlayer().getName())
                .replace("%suffix%", suffix)
                .replace("%message%", message);

        Component rendered = LegacyComponentSerializer.legacyAmpersand().deserialize(finalLine);
        event.renderer((source, sourceDisplayName, msg, viewer) -> rendered);
    }
}
