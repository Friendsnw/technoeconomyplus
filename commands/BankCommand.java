package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.util.Format;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class BankCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public BankCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var gate = plugin.getPermissionGate();
        var lang = plugin.getLanguageManager();
        if (!gate.requirePlayer(sender)) return true;
        if (!gate.requirePermission(sender, "technoeconomy.use")) return true;
        if (!gate.requireFeature(sender, "economy.bank")) return true;

        Player player = (Player) sender;
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");

        if (args.length == 0) {
            if (plugin.getMenuLoader().openMenu(player, "bank")) {
                return true;
            }
            double bankBal = plugin.getBankManager().getBankBalance(player.getUniqueId());
            player.sendMessage(lang.getMessage("messages.bank-balance", Map.of("symbol", symbol, "amount", Format.number(bankBal))));
            player.sendMessage(lang.getMessage("messages.bank-usage"));
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("interest")) {
            long remainingSec = Math.max(0, plugin.getBankManager().getNextInterestTimestamp() - Instant.now().getEpochSecond());
            long mins = remainingSec / 60;
            double rate = plugin.getConfig().getDouble("bank.interest-rate", 0.02) * 100;
            player.sendMessage(lang.getMessage("messages.bank-interest-info", Map.of(
                    "minutes", String.valueOf(mins),
                    "rate", String.valueOf(rate)
            )));
            return true;
        }

        if (sub.equals("deposit") || sub.equals("withdraw")) {
            if (args.length < 2) {
                player.sendMessage(lang.getMessage("messages.invalid-usage", Map.of("usage", "/bank " + sub + " <amount>")));
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

            if (sub.equals("deposit")) {
                if (!plugin.getBalanceManager().has(player.getUniqueId(), amount)) {
                    player.sendMessage(lang.getMessage("messages.insufficient-funds"));
                    return true;
                }
                plugin.getBalanceManager().removeBalance(player.getUniqueId(), amount);
                plugin.getBankManager().deposit(player.getUniqueId(), amount);
                plugin.getTransactionLogManager().log(player.getUniqueId(), player.getName(), "BANK", -amount,
                        plugin.getBalanceManager().getBalance(player.getUniqueId()), "Deposit to bank");
                player.sendMessage(lang.getMessage("messages.bank-deposit-success", Map.of("symbol", symbol, "amount", Format.number(amount))));
            } else {
                boolean success = plugin.getBankManager().withdraw(player.getUniqueId(), amount);
                if (!success) {
                    player.sendMessage(lang.getMessage("messages.bank-insufficient"));
                    return true;
                }
                plugin.getBalanceManager().addBalance(player.getUniqueId(), amount);
                plugin.getTransactionLogManager().log(player.getUniqueId(), player.getName(), "BANK", amount,
                        plugin.getBalanceManager().getBalance(player.getUniqueId()), "Withdraw from bank");
                player.sendMessage(lang.getMessage("messages.bank-withdraw-success", Map.of("symbol", symbol, "amount", Format.number(amount))));
            }
            return true;
        }

        player.sendMessage(lang.getMessage("messages.bank-usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("deposit", "withdraw", "interest").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }
}
