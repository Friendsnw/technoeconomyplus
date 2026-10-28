package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StockCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public StockCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.players-only"));
            return true;
        }

        if (!player.hasPermission("technoeconomy.borsa") && !player.hasPermission("technoeconomy.stock")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.no-permission"));
            return true;
        }

        if (!plugin.getPermissionGate().hasPackageFeature("borsa") && !plugin.getPermissionGate().hasPackageCommand("borsa")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.package-upgrade-needed"));
            return true;
        }

        if (args.length == 0) {
            new com.techno.economy.gui.StockGui(plugin).open(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("portfoy") || sub.equals("portfolio")) {
            double totalVal = plugin.getStockManager().getPortfolioValue(player.getUniqueId());
            double pl = plugin.getStockManager().getProfitLoss(player.getUniqueId());
            String color = pl >= 0 ? "§a+" : "§c";

            player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-portfolio-header"));
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-portfolio-total", Map.of("total", String.valueOf(totalVal))));
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-portfolio-pl", Map.of("color", color, "pl", String.valueOf(pl))));

            Map<String, Integer> portfolio = plugin.getStockManager().getPlayerPortfolio(player.getUniqueId());
            if (portfolio.isEmpty()) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-no-shares"));
            } else {
                for (Map.Entry<String, Integer> entry : portfolio.entrySet()) {
                    double currentPrice = plugin.getStockManager().getCompanySharePrice(entry.getKey());
                    player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-portfolio-entry", Map.of(
                            "company", entry.getKey(),
                            "amount", String.valueOf(entry.getValue()),
                            "price", String.valueOf(currentPrice)
                    )));
                }
            }
            return true;
        }

        if (sub.equals("al") || sub.equals("buy")) {
            if (args.length < 3) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-buy-usage"));
                return true;
            }
            String company = args[1];
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (Exception e) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-buy-invalid-amount"));
                return true;
            }

            boolean ok = plugin.getStockManager().buyCompanyShares(player.getUniqueId(), company, amount);
            if (ok) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-buy-success", Map.of(
                        "company", company,
                        "amount", String.valueOf(amount)
                )));
            } else {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-buy-failed"));
            }
            return true;
        }

        if (sub.equals("sat") || sub.equals("sell")) {
            if (args.length < 3) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-sell-usage"));
                return true;
            }
            String company = args[1];
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (Exception e) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-buy-invalid-amount"));
                return true;
            }

            boolean ok = plugin.getStockManager().sellCompanyShares(player.getUniqueId(), company, amount);
            if (ok) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-sell-success", Map.of(
                        "company", company,
                        "amount", String.valueOf(amount)
                )));
            } else {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-sell-failed"));
            }
            return true;
        }

        plugin.getMenuLoader().openMenu(player, "stock");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(List.of("portfoy", "portfolio", "al", "buy", "sat", "sell"));
            list.removeIf(s -> !s.startsWith(args[0].toLowerCase()));
            return list;
        }
        return Collections.emptyList();
    }
}
