package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class JobsCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;
    private static final List<String> JOBS = List.of("miner", "farmer", "hunter", "builder", "fisherman", "none");

    public JobsCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var gate = plugin.getPermissionGate();
        var lang = plugin.getLanguageManager();
        if (!gate.requirePlayer(sender)) return true;
        if (!gate.requirePermission(sender, "technoeconomy.use")) return true;
        if (!gate.requireFeature(sender, "economy.jobs")) return true;
        if (!plugin.getFeatureManager().isEnabled("jobs")) {
            sender.sendMessage(lang.getMessage("messages.feature-disabled"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            if (!plugin.getMenuLoader().openMenu(player, "jobs")) {
                String current = plugin.getJobsManager().getJob(player.getUniqueId());
                player.sendMessage(lang.getMessage("messages.jobs-current", Map.of("job", current)));
                player.sendMessage(lang.getMessage("messages.jobs-usage"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("join")) {
            if (args.length < 2) {
                player.sendMessage(lang.getMessage("messages.jobs-usage"));
                return true;
            }
            String chosen = args[1].toLowerCase();
            if (!JOBS.contains(chosen)) {
                player.sendMessage(lang.getMessage("messages.jobs-invalid", Map.of("job", args[1])));
                return true;
            }
            plugin.getJobsManager().setJob(player.getUniqueId(), chosen);
            player.sendMessage(lang.getMessage("messages.jobs-joined", Map.of("job", chosen)));
            return true;
        }

        if (!plugin.getMenuLoader().openMenu(player, "jobs")) {
            player.sendMessage(lang.getMessage("messages.jobs-usage"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("menu", "join").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            return JOBS.stream().filter(s -> s.startsWith(args[1].toLowerCase())).toList();
        }
        return List.of();
    }
}
