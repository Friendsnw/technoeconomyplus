package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.company.CompanyManager;
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

public class CompanyCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public CompanyCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.players-only"));
            return true;
        }

        if (!player.hasPermission("technoeconomy.sirket") && !player.hasPermission("technoeconomy.company")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.no-permission"));
            return true;
        }

        if (!plugin.getPermissionGate().hasPackageFeature("sirket") && !plugin.getPermissionGate().hasPackageCommand("sirket")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.package-upgrade-needed"));
            return true;
        }

        if (args.length == 0) {
            plugin.getMenuLoader().openMenu(player, "company");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("kur") || sub.equals("create")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-create-usage"));
                return true;
            }
            String name = args[1];
            boolean created = plugin.getCompanyManager().createCompany(player.getUniqueId(), player.getName(), name);
            if (created) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-created", Map.of("name", name)));
            } else {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-create-failed"));
            }
            return true;
        }

        if (sub.equals("bilgi") || sub.equals("info")) {
            CompanyManager.CompanyData c = (args.length >= 2) ? plugin.getCompanyManager().getCompany(args[1]) : plugin.getCompanyManager().getPlayerCompany(player.getUniqueId());
            if (c == null) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-not-found"));
                return true;
            }
            double marketCap = plugin.getCompanyManager().getMarketCap(c.getName());
            double sharePrice = plugin.getStockManager().getCompanySharePrice(c.getName());
            List<CompanyManager.WorkerData> workers = plugin.getCompanyManager().getWorkers(c.getName());

            player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-details-header", Map.of("name", c.getName())));
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-owner", Map.of("owner", c.getOwnerName())));
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-vault", Map.of("balance", String.valueOf(c.getBalance()))));
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-tax-debt", Map.of("debt", String.valueOf(c.getTaxDebt()))));
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-market-cap", Map.of("cap", String.valueOf(marketCap))));
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-share-price", Map.of("price", String.valueOf(sharePrice))));
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-workers-count", Map.of("count", String.valueOf(workers.size()))));
            return true;
        }

        if (sub.equals("iseal") || sub.equals("hire")) {
            CompanyManager.CompanyData c = plugin.getCompanyManager().getPlayerCompany(player.getUniqueId());
            if (c == null) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-none-owned"));
                return true;
            }
            if (args.length < 3) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-hire-usage"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.player-not-found", Map.of("player", args[1])));
                return true;
            }
            double salary;
            try {
                salary = Double.parseDouble(args[2]);
            } catch (Exception e) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.invalid-amount", Map.of("amount", args[2])));
                return true;
            }

            boolean ok = plugin.getCompanyManager().addWorker(c.getName(), target.getUniqueId(), target.getName(), salary);
            if (ok) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-hire-success", Map.of(
                        "player", target.getName(),
                        "salary", String.valueOf(salary)
                )));
                target.sendMessage(plugin.getLanguageManager().getMessage("messages.company-hired-notice", Map.of(
                        "company", c.getName(),
                        "salary", String.valueOf(salary)
                )));
            } else {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-hire-failed"));
            }
            return true;
        }

        if (sub.equals("cikar") || sub.equals("fire")) {
            CompanyManager.CompanyData c = plugin.getCompanyManager().getPlayerCompany(player.getUniqueId());
            if (c == null) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-none-owned"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-fire-usage"));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.player-not-found", Map.of("player", args[1])));
                return true;
            }

            boolean ok = plugin.getCompanyManager().removeWorker(c.getName(), target.getUniqueId());
            if (ok) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-fire-success"));
            } else {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-fire-failed"));
            }
            return true;
        }

        if (sub.equals("yatir") || sub.equals("deposit")) {
            CompanyManager.CompanyData c = plugin.getCompanyManager().getPlayerCompany(player.getUniqueId());
            if (c == null) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-none-owned"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-deposit-usage"));
                return true;
            }
            double amount;
            try {
                amount = Double.parseDouble(args[1]);
            } catch (Exception e) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.invalid-amount", Map.of("amount", args[1])));
                return true;
            }
            if (!plugin.getBalanceManager().has(player.getUniqueId(), amount)) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.insufficient-funds"));
                return true;
            }

            plugin.getBalanceManager().withdraw(player.getUniqueId(), amount);
            plugin.getCompanyManager().deposit(c.getName(), amount);
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-deposited", Map.of("amount", String.valueOf(amount))));
            return true;
        }

        if (sub.equals("cek") || sub.equals("withdraw")) {
            CompanyManager.CompanyData c = plugin.getCompanyManager().getPlayerCompany(player.getUniqueId());
            if (c == null) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-none-owned"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-withdraw-usage"));
                return true;
            }
            double amount;
            try {
                amount = Double.parseDouble(args[1]);
            } catch (Exception e) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.invalid-amount", Map.of("amount", args[1])));
                return true;
            }

            boolean ok = plugin.getCompanyManager().withdraw(c.getName(), amount);
            if (ok) {
                plugin.getBalanceManager().deposit(player.getUniqueId(), amount);
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-withdrawn", Map.of("amount", String.valueOf(amount))));
            } else {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-insufficient-vault"));
            }
            return true;
        }

        if (sub.equals("vergioda") || sub.equals("paytax")) {
            CompanyManager.CompanyData c = plugin.getCompanyManager().getPlayerCompany(player.getUniqueId());
            if (c == null) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-none-owned"));
                return true;
            }
            double amount = c.getTaxDebt();
            if (args.length >= 2) {
                try {
                    amount = Double.parseDouble(args[1]);
                } catch (Exception ignored) {}
            }
            if (amount <= 0) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-no-tax-debt"));
                return true;
            }

            boolean ok = plugin.getCompanyManager().payTaxes(c.getName(), amount);
            if (ok) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-tax-paid", Map.of("amount", String.valueOf(amount))));
            } else {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.company-tax-pay-failed"));
            }
            return true;
        }

        plugin.getMenuLoader().openMenu(player, "company");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(List.of("kur", "create", "bilgi", "info", "iseal", "hire", "cikar", "fire", "yatir", "deposit", "cek", "withdraw", "vergioda", "paytax"));
            list.removeIf(s -> !s.startsWith(args[0].toLowerCase()));
            return list;
        }
        return Collections.emptyList();
    }
}
