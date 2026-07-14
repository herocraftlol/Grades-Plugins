package com.tututte.gradeplugin.commands;

import com.tututte.gradeplugin.grade.Grade;
import com.tututte.gradeplugin.grade.GradeManager;
import com.tututte.gradeplugin.grade.PlayerGrade;
import com.tututte.gradeplugin.grade.PlayerGradeManager;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;

public class GradeCommand implements CommandExecutor {

    private final GradeManager gradeManager;
    private final PlayerGradeManager playerGradeManager;

    public GradeCommand(GradeManager gradeManager, PlayerGradeManager playerGradeManager) {
        this.gradeManager = gradeManager;
        this.playerGradeManager = playerGradeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                listAvailableGrades(sender);
                return true;
            }
            showPlayerGrades(sender, player.getUniqueId(), player.getName());
            return true;
        }

        if (args[0].equalsIgnoreCase("liste") || args[0].equalsIgnoreCase("list")) {
            listAvailableGrades(sender);
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        showPlayerGrades(sender, target.getUniqueId(), target.getName() != null ? target.getName() : args[0]);
        return true;
    }

    private void showPlayerGrades(CommandSender sender, java.util.UUID uuid, String name) {
        List<PlayerGrade> grades = playerGradeManager.getCached(uuid);
        if (grades.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + name + " n'a aucun grade actif.");
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "Grades de " + name + " :");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());

        for (PlayerGrade pg : grades) {
            Grade g = gradeManager.getGrade(pg.getGradeId());
            String displayName = g != null ? g.getDisplayName() : pg.getGradeId();
            String expiry = pg.isPermanent() ? "permanent" : "expire le " + fmt.format(pg.getExpiresAt());
            sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.WHITE + displayName + ChatColor.GRAY + " (" + expiry + ")");
        }
    }

    private void listAvailableGrades(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Grades disponibles :");
        for (Grade g : gradeManager.getAllGrades()) {
            String priceInfo = g.getPrice() != null
                    ? ChatColor.GREEN + " - " + g.getPrice() + "€ sur le site"
                    : ChatColor.GRAY + " - obtenable via competition";
            sender.sendMessage(ChatColor.WHITE + g.getDisplayName() + priceInfo);
        }
    }
}
