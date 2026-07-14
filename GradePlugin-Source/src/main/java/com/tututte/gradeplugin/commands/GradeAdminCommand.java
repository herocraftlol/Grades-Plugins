package com.tututte.gradeplugin.commands;

import com.tututte.gradeplugin.grade.Grade;
import com.tututte.gradeplugin.grade.GradeManager;
import com.tututte.gradeplugin.grade.PlayerGradeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;

public class GradeAdminCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final GradeManager gradeManager;
    private final PlayerGradeManager playerGradeManager;

    public GradeAdminCommand(JavaPlugin plugin, GradeManager gradeManager, PlayerGradeManager playerGradeManager) {
        this.plugin = plugin;
        this.gradeManager = gradeManager;
        this.playerGradeManager = playerGradeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /gradeadmin <give|remove|reload|create|list>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "reload" -> handleReload(sender);
            case "create" -> handleCreate(sender, args);
            case "list" -> handleList(sender);
            default -> sender.sendMessage(ChatColor.RED + "Sous-commande inconnue. Usage: /gradeadmin <give|remove|reload|create|list>");
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /gradeadmin give <joueur> <grade> [duree_jours]");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String gradeId = args[2];

        if (!gradeManager.exists(gradeId)) {
            sender.sendMessage(ChatColor.RED + "Le grade '" + gradeId + "' n'existe pas.");
            return;
        }

        Long duration = null;
        if (args.length >= 4) {
            try {
                duration = Long.parseLong(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "La duree doit etre un nombre de jours.");
                return;
            }
        }

        final Long finalDuration = duration;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                playerGradeManager.grantGrade(target.getUniqueId(), gradeId, "admin", finalDuration));

        sender.sendMessage(ChatColor.GREEN + "Grade '" + gradeId + "' attribue a " + args[1] + ".");
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /gradeadmin remove <joueur> <grade>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        String gradeId = args[2];

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                playerGradeManager.revokeGrade(target.getUniqueId(), gradeId));

        sender.sendMessage(ChatColor.GREEN + "Grade '" + gradeId + "' retire a " + args[1] + ".");
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, gradeManager::loadAll);
        sender.sendMessage(ChatColor.GREEN + "Configuration et grades recharges.");
    }

    private void handleCreate(CommandSender sender, String[] args) {
        // /gradeadmin create <id> <displayName> <color> <priority> <prefix> [perm1,perm2,...] [prix]
        if (args.length < 6) {
            sender.sendMessage(ChatColor.RED + "Usage: /gradeadmin create <id> <nom> <couleur &x> <priorite> <prefixe> [permissions_separees_par_virgule] [prix]");
            return;
        }

        String id = args[1];
        String name = args[2];
        String color = args[3];
        int priority;
        try {
            priority = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "La priorite doit etre un nombre.");
            return;
        }
        String prefix = args[5];
        java.util.List<String> perms = args.length >= 7
                ? new ArrayList<>(Arrays.asList(args[6].split(",")))
                : new ArrayList<>();
        Double price = null;
        if (args.length >= 8) {
            try {
                price = Double.parseDouble(args[7]);
            } catch (NumberFormatException ignored) {
            }
        }

        Grade grade = new Grade(id, name, prefix, "", color, priority, perms, price);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> gradeManager.saveGrade(grade));

        sender.sendMessage(ChatColor.GREEN + "Grade '" + id + "' cree.");
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Grades configures (" + gradeManager.getAllGrades().size() + ") :");
        for (Grade g : gradeManager.getAllGrades()) {
            sender.sendMessage(ChatColor.WHITE + "- " + g.getId() + " (" + g.getDisplayName() + ", priorite " + g.getPriority() + ")");
        }
    }
}
