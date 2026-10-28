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
import java.util.stream.Collectors;

public class PayCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public PayCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var gate = plugin.getPermissionGate();
        var lang = plugin.getLanguageManager();
        if (!gate.requirePlayer(sender)) return true;
        if (!gate.requirePermission(sender, "technoeconomy.pay")) return true;
        if (!gate.requireFeature(sender, "economy.basic")) return true;

        Player player = (Player) sender;
        if (args.length < 2) {
            player.sendMessage(lang.getMessage("messages.invalid-usage", Map.of("usage", "/pay <player> <amount>")));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(lang.getMessage("messages.player-not-found", Map.of("player", args[0])));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(lang.getMessage("messages.pay-self"));
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

        double taxPercent = plugin.getConfig().getDouble("tax.pay-tax-percent", 0.0);
        double taxMin = plugin.getConfig().getDouble("tax.pay-tax-free-minimum", 100.0);
        double tax = 0.0;
        if (taxPercent > 0 && amount > taxMin) {
            tax = (amount * (taxPercent / 100.0));
        }

        double received = amount - tax;
        plugin.getBalanceManager().removeBalance(player.getUniqueId(), amount);
        plugin.getBalanceManager().addBalance(target.getUniqueId(), received);

        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        plugin.getTransactionLogManager().log(player.getUniqueId(), player.getName(), "PAY", -amount,
                plugin.getBalanceManager().getBalance(player.getUniqueId()), "Sent to " + target.getName() + " (Tax: " + tax + ")");
        plugin.getTransactionLogManager().log(target.getUniqueId(), target.getName(), "PAY", received,
                plugin.getBalanceManager().getBalance(target.getUniqueId()), "Received from " + player.getName());

        player.sendMessage(lang.getMessage("messages.pay-sent", Map.of(
                "player", target.getName(),
                "amount", Format.number(amount),
                "symbol", symbol,
                "tax", Format.number(tax)
        )));

        target.sendMessage(lang.getMessage("messages.pay-received", Map.of(
                "player", player.getName(),
                "amount", Format.number(received),
                "symbol", symbol
        )));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String p = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(p) && !n.equalsIgnoreCase(sender.getName()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
