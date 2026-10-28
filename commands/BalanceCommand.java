package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BalanceCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public BalanceCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        var lang = plugin.getLanguageManager();
        var gate = plugin.getPermissionGate();

        if (args.length == 0) {
            if (!gate.requirePlayer(sender)) return true;
            if (!gate.requirePermission(sender, "technoeconomy.use")) return true;
            if (!gate.requireCommand(sender, "balance")) return true;
            Player player = (Player) sender;
            double bal = plugin.getBalanceManager().getBalance(player.getUniqueId());
            sender.sendMessage(lang.getMessage("messages.balance", Map.of(
                    "symbol", symbol, "amount", Format.number(bal)
            )));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("history")) {
            if (!gate.requirePlayer(sender)) return true;
            if (!gate.requirePermission(sender, "technoeconomy.use")) return true;
            Player player = (Player) sender;
            List<String> hist = plugin.getTransactionLogManager().getHistory(player.getUniqueId());
            if (hist.isEmpty()) {
                sender.sendMessage(lang.getMessage("messages.history-empty"));
                return true;
            }
            sender.sendMessage(lang.getMessage("messages.history-header"));
            for (int i = 0; i < Math.min(10, hist.size()); i++) {
                sender.sendMessage("§8- §7" + hist.get(i));
            }
            return true;
        }

        if (sub.equals("give")) {
            if (!gate.requirePermission(sender, "technoeconomy.admin")) return true;
            if (!gate.requireCommand(sender, "balance give")) return true;
            if (args.length < 3) {
                sender.sendMessage(lang.getMessage("messages.invalid-usage",
                        Map.of("usage", "/balance give <player> <amount>")));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(lang.getMessage("messages.player-not-found", Map.of("player", args[1])));
                return true;
            }
            double amount = parseAmount(args[2]);
            if (amount <= 0) {
                sender.sendMessage(lang.getMessage("messages.invalid-amount", Map.of("amount", args[2])));
                return true;
            }
            plugin.getBalanceManager().addBalance(target.getUniqueId(), amount);
            plugin.getTransactionLogManager().log(target.getUniqueId(), target.getName(), "ADMIN_GIVE", amount,
                    plugin.getBalanceManager().getBalance(target.getUniqueId()), "Given by " + sender.getName());
            sender.sendMessage(lang.getMessage("messages.balance-given", Map.of(
                    "player", target.getName(), "symbol", symbol, "amount", Format.number(amount)
            )));
            return true;
        }

        if (sub.equals("set")) {
            if (!gate.requirePermission(sender, "technoeconomy.admin")) return true;
            if (!gate.requireCommand(sender, "balance set")) return true;
            if (args.length < 3) {
                sender.sendMessage(lang.getMessage("messages.invalid-usage",
                        Map.of("usage", "/balance set <player> <amount>")));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(lang.getMessage("messages.player-not-found", Map.of("player", args[1])));
                return true;
            }
            double amount = parseAmount(args[2]);
            if (amount < 0) {
                sender.sendMessage(lang.getMessage("messages.invalid-amount", Map.of("amount", args[2])));
                return true;
            }
            plugin.getBalanceManager().setBalance(target.getUniqueId(), amount);
            plugin.getTransactionLogManager().log(target.getUniqueId(), target.getName(), "ADMIN_SET", amount,
                    amount, "Set by " + sender.getName());
            sender.sendMessage(lang.getMessage("messages.balance-set", Map.of(
                    "player", target.getName(), "symbol", symbol, "amount", Format.number(amount)
            )));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target != null) {
            if (!gate.requirePermission(sender, "technoeconomy.admin")) return true;
            double bal = plugin.getBalanceManager().getBalance(target.getUniqueId());
            sender.sendMessage(lang.getMessage("messages.balance-other", Map.of(
                    "player", target.getName(), "symbol", symbol, "amount", Format.number(bal)
            )));
            return true;
        }

        sender.sendMessage(lang.getMessage("messages.invalid-usage",
                Map.of("usage", "/balance [give|set|history] [player] [amount]")));
        return true;
    }

    private double parseAmount(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("history");
            if (sender.hasPermission("technoeconomy.admin")) {
                completions.addAll(List.of("give", "set"));
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            }
            return completions.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).toList();
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if ((sub.equals("give") || sub.equals("set")) && sender.hasPermission("technoeconomy.admin")) {
                String partial = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(partial))
                        .collect(Collectors.toList());
            }
        }
        return completions;
    }
}
