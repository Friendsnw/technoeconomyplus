package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class MarketCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public MarketCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var gate = plugin.getPermissionGate();
        var lang = plugin.getLanguageManager();
        if (!gate.requirePlayer(sender)) return true;
        if (!gate.requirePermission(sender, "technoeconomy.use")) return true;
        if (!gate.requireFeature(sender, "economy.market")) return true;

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
            if (!gate.requirePermission(sender, "technoeconomy.admin")) return true;
            if (args.length >= 6 && args[1].equalsIgnoreCase("add")) {
                String id = args[2].toLowerCase();
                try {
                    Material mat = Material.valueOf(args[3].toUpperCase());
                    double buy = Double.parseDouble(args[4]);
                    double sell = Double.parseDouble(args[5]);
                    String cat = args.length > 6 ? args[6] : "misc";
                    plugin.getMarketManager().addItem(id, mat, cat, buy, sell);
                    player.sendMessage(lang.getMessage("messages.market-admin-added", Map.of("item", id)));
                } catch (Exception e) {
                    player.sendMessage(lang.getMessage("messages.market-admin-usage"));
                }
                return true;
            } else if (args.length >= 3 && args[1].equalsIgnoreCase("remove")) {
                boolean removed = plugin.getMarketManager().removeItem(args[2]);
                if (removed) {
                    player.sendMessage(lang.getMessage("messages.market-admin-removed", Map.of("item", args[2])));
                } else {
                    player.sendMessage(lang.getMessage("messages.market-admin-not-found"));
                }
                return true;
            }
            player.sendMessage(lang.getMessage("messages.market-admin-usage"));
            return true;
        }

        String cat = args.length > 0 ? args[0] : null;
        int page = 0;
        if (args.length > 1) {
            try {
                page = Math.max(0, Integer.parseInt(args[1]) - 1);
            } catch (NumberFormatException ignored) {
            }
        }

        if (cat == null) {
            plugin.getMarketManager().openCategoryMenu(player);
        } else {
            plugin.getMarketManager().openMarket(player, cat, page);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("all", "minerals", "building", "farming", "misc", "admin").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return List.of("add", "remove").stream().filter(s -> s.startsWith(args[1].toLowerCase())).toList();
        }
        return List.of();
    }
}
