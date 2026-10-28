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

public class EcoCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public EcoCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getPermissionGate().requirePermission(sender, "technoeconomy.admin")) return true;

        if (args.length == 0 || args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("admin") || args[0].equalsIgnoreCase("menu")) {
            if (sender instanceof Player player) {
                new com.techno.economy.gui.AdminGui(plugin).open(player);
                return true;
            }
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.eco-usage"));
            return true;
        }

        String sub = args[0].toLowerCase();
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        var lang = plugin.getLanguageManager();

        switch (sub) {
            case "backup" -> handleBackup(sender);
            case "reload" -> handleReload(sender);
            case "give", "take", "set", "reset" -> {
                if (args.length < 2) {
                    sender.sendMessage(lang.getMessage("messages.eco-usage"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(lang.getMessage("messages.player-not-found", Map.of("player", args[1])));
                    return true;
                }

                if (sub.equals("reset")) {
                    double def = plugin.getConfig().getDouble("economy.starting-balance", 100.0);
                    plugin.getBalanceManager().setBalance(target.getUniqueId(), def);
                    plugin.getTransactionLogManager().log(target.getUniqueId(), target.getName(), "ADMIN_RESET", def, def, "Reset by " + sender.getName());
                    sender.sendMessage(lang.getMessage("messages.eco-reset", Map.of("player", target.getName(), "symbol", symbol, "amount", Format.number(def))));
                    return true;
                }

                if (args.length < 3) {
                    sender.sendMessage(lang.getMessage("messages.eco-usage"));
                    return true;
                }

                double amount;
                try {
                    amount = Double.parseDouble(args[2]);
                    if (amount < 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    sender.sendMessage(lang.getMessage("messages.invalid-amount", Map.of("amount", args[2])));
                    return true;
                }

                if (sub.equals("give")) {
                    plugin.getBalanceManager().addBalance(target.getUniqueId(), amount);
                    plugin.getTransactionLogManager().log(target.getUniqueId(), target.getName(), "ADMIN_GIVE", amount,
                            plugin.getBalanceManager().getBalance(target.getUniqueId()), "Admin give by " + sender.getName());
                    sender.sendMessage(lang.getMessage("messages.balance-given", Map.of("player", target.getName(), "symbol", symbol, "amount", Format.number(amount))));
                } else if (sub.equals("take")) {
                    plugin.getBalanceManager().removeBalance(target.getUniqueId(), amount);
                    plugin.getTransactionLogManager().log(target.getUniqueId(), target.getName(), "ADMIN_TAKE", -amount,
                            plugin.getBalanceManager().getBalance(target.getUniqueId()), "Admin take by " + sender.getName());
                    sender.sendMessage(lang.getMessage("messages.eco-taken", Map.of("player", target.getName(), "symbol", symbol, "amount", Format.number(amount))));
                } else if (sub.equals("set")) {
                    plugin.getBalanceManager().setBalance(target.getUniqueId(), amount);
                    plugin.getTransactionLogManager().log(target.getUniqueId(), target.getName(), "ADMIN_SET", amount,
                            amount, "Admin set by " + sender.getName());
                    sender.sendMessage(lang.getMessage("messages.balance-set", Map.of("player", target.getName(), "symbol", symbol, "amount", Format.number(amount))));
                }
            }
            default -> sender.sendMessage(lang.getMessage("messages.eco-usage"));
        }

        return true;
    }

    private void handleBackup(CommandSender sender) {
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.eco-backup-starting"));
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean ok = plugin.getBackupManager().backupAll();
            String key = ok ? "messages.eco-backup-success" : "messages.eco-backup-failed";
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    sender.sendMessage(plugin.getLanguageManager().getMessage(key))
            );
        });
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getLanguageManager().load();
        plugin.getWorthManager().load();
        plugin.getMarketManager().load();
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.eco-reload-success"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("technoeconomy.admin")) {
            for (String sub : List.of("give", "take", "set", "reset", "backup", "reload")) {
                if (sub.startsWith(args[0].toLowerCase())) completions.add(sub);
            }
        } else if (args.length == 2 && List.of("give", "take", "set", "reset").contains(args[0].toLowerCase())) {
            String p = args[1].toLowerCase();
            completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(p))
                    .collect(Collectors.toList()));
        }
        return completions;
    }
}
