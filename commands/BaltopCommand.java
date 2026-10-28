package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BaltopCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public BaltopCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var gate = plugin.getPermissionGate();
        var lang = plugin.getLanguageManager();
        if (!gate.requirePermission(sender, "technoeconomy.use")) return true;
        if (!gate.requireCommand(sender, "baltop")) return true;

        if (sender instanceof Player player) {
            if (args.length == 0 || args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("podium")) {
                new com.techno.economy.gui.BaltopGui(plugin).openPodium(player);
                return true;
            }
            if (args[0].equalsIgnoreCase("all") || args[0].equalsIgnoreCase("liste")) {
                new com.techno.economy.gui.BaltopGui(plugin).openAllRankings(player, 0);
                return true;
            }
        }

        int page = 1;
        if (args.length > 0) {
            try {
                page = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException ignored) {
            }
        }

        List<Map.Entry<UUID, Double>> top = plugin.getBalanceManager().getCachedBaltop();
        int pageSize = 10;
        int totalPages = Math.max(1, (int) Math.ceil((double) top.size() / pageSize));
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, top.size());
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");

        sender.sendMessage(lang.getMessage("messages.baltop-header", Map.of("page", String.valueOf(page), "total", String.valueOf(totalPages))));

        for (int i = start; i < end; i++) {
            Map.Entry<UUID, Double> entry = top.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            String name = op.getName() != null ? op.getName() : "Unknown";
            sender.sendMessage(lang.getMessage("messages.baltop-entry", Map.of(
                    "rank", String.valueOf(i + 1),
                    "player", name,
                    "symbol", symbol,
                    "amount", Format.money(entry.getValue(), symbol)
            )));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
