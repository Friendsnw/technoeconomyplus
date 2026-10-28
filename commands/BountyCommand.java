package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class BountyCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public BountyCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var gate = plugin.getPermissionGate();
        var lang = plugin.getLanguageManager();
        if (!gate.requirePermission(sender, "technoeconomy.use")) return true;
        if (!gate.requireFeature(sender, "economy.basic")) return true;

        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");

        if (args.length == 0 || args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("menu")) {
            if (sender instanceof Player player) {
                new com.techno.economy.gui.BountyGui(plugin).open(player, 0);
                return true;
            }
            Map<UUID, Double> bounties = plugin.getBountyManager().getAllBounties();
            if (bounties.isEmpty()) {
                sender.sendMessage(lang.getMessage("messages.bounty-list-empty"));
                return true;
            }
            sender.sendMessage(lang.getMessage("messages.bounty-list-header"));
            for (var entry : bounties.entrySet()) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
                String name = op.getName() != null ? op.getName() : "Unknown";
                sender.sendMessage("§8- §c" + name + "§7: §e" + symbol + Format.number(entry.getValue()));
            }
            return true;
        }

        if (args.length >= 2) {
            if (!gate.requirePlayer(sender)) return true;
            Player player = (Player) sender;
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(lang.getMessage("messages.player-not-found", Map.of("player", args[0])));
                return true;
            }
            if (target.getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage(lang.getMessage("messages.bounty-self"));
                return true;
            }

            double amount;
            try {
                amount = Double.parseDouble(args[1]);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                player.sendMessage(lang.getMessage("messages.invalid-amount", Map.of("amount", args[1])));
                return true;
            }

            if (!plugin.getBalanceManager().has(player.getUniqueId(), amount)) {
                player.sendMessage(lang.getMessage("messages.insufficient-funds"));
                return true;
            }

            plugin.getBalanceManager().removeBalance(player.getUniqueId(), amount);
            plugin.getBountyManager().addBounty(target.getUniqueId(), amount);
            plugin.getTransactionLogManager().log(player.getUniqueId(), player.getName(), "BOUNTY", -amount,
                    plugin.getBalanceManager().getBalance(player.getUniqueId()), "Bounty placed on " + target.getName());

            Bukkit.broadcast(lang.getMessage("messages.bounty-broadcast", Map.of(
                    "player", player.getName(),
                    "target", target.getName(),
                    "amount", Format.number(amount),
                    "symbol", symbol
            )));
            return true;
        }

        sender.sendMessage(lang.getMessage("messages.bounty-usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add("list");
            list.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            return list.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }
}
