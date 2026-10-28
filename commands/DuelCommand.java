package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DuelCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public DuelCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.players-only"));
            return true;
        }

        if (!player.hasPermission("technoeconomy.duello") && !player.hasPermission("technoeconomy.duel")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.no-permission"));
            return true;
        }

        if (!plugin.getPermissionGate().hasPackageFeature("duello") && !plugin.getPermissionGate().hasPackageCommand("duello")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.package-upgrade-needed"));
            return true;
        }

        if (args.length == 0) {
            plugin.getMenuLoader().openMenu(player, "duel");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("kabul") || sub.equals("accept")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.duel-accept-usage"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.player-not-found", Map.of("player", args[1])));
                return true;
            }

            boolean accepted = plugin.getDuelManager().acceptOffer(player, target);
            if (!accepted) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.duel-accept-failed"));
            }
            return true;
        }

        if (sub.equals("istatistik") || sub.equals("stats")) {
            int wins = plugin.getDuelManager().getWins(player.getUniqueId());
            int losses = plugin.getDuelManager().getLosses(player.getUniqueId());
            double total = plugin.getDuelManager().getTotalWinnings(player.getUniqueId());

            player.sendMessage(plugin.getLanguageManager().getMessage("messages.duel-stats-header"));
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.duel-stats-wins", Map.of("wins", String.valueOf(wins))));
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.duel-stats-losses", Map.of("losses", String.valueOf(losses))));
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.duel-stats-winnings", Map.of("total", String.valueOf(total))));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target != null && args.length >= 2) {
            double amount;
            try {
                amount = Double.parseDouble(args[1]);
            } catch (Exception e) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.duel-invalid-wager"));
                return true;
            }

            String type = args.length >= 3 ? args[2].toUpperCase() : "PVP";
            boolean sent = plugin.getDuelManager().sendOffer(player, target, amount, type);
            if (sent) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.duel-offer-sent", Map.of(
                        "target", target.getName(),
                        "amount", String.valueOf(amount),
                        "type", type
                )));
            } else {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.duel-offer-failed"));
            }
            return true;
        }

        plugin.getMenuLoader().openMenu(player, "duel");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(List.of("kabul", "accept", "istatistik", "stats"));
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getName().equalsIgnoreCase(sender.getName())) {
                    list.add(p.getName());
                }
            }
            list.removeIf(s -> !s.startsWith(args[0].toLowerCase()));
            return list;
        }
        if (args.length == 3) {
            List<String> types = new ArrayList<>(List.of("PVP", "COINFLIP"));
            types.removeIf(s -> !s.startsWith(args[2].toUpperCase()));
            return types;
        }
        return Collections.emptyList();
    }
}
