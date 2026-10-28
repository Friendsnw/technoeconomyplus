package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CheckCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public CheckCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.players-only"));
            return true;
        }

        if (!player.hasPermission("technoeconomy.cek") && !player.hasPermission("technoeconomy.check")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.no-permission"));
            return true;
        }

        if (!plugin.getPermissionGate().hasPackageFeature("cek") && !plugin.getPermissionGate().hasPackageCommand("cek")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.package-upgrade-needed"));
            return true;
        }

        if (args.length == 0) {
            plugin.getMenuLoader().openMenu(player, "check");
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("yaz") || sub.equals("write")) {
            if (args.length < 2) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.check-write-usage"));
                return true;
            }

            double amount;
            try {
                amount = Double.parseDouble(args[1]);
            } catch (Exception e) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.invalid-amount", Map.of("amount", args[1])));
                return true;
            }

            if (player.getInventory().firstEmpty() == -1) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.check-inventory-full"));
                return true;
            }

            ItemStack checkItem = plugin.getCheckManager().createCheck(player, amount);
            if (checkItem != null) {
                player.getInventory().addItem(checkItem);
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.check-created", Map.of("amount", String.valueOf(amount))));
            } else {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.check-create-failed"));
            }
            return true;
        }

        plugin.getMenuLoader().openMenu(player, "check");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(List.of("yaz", "write"));
            list.removeIf(s -> !s.startsWith(args[0].toLowerCase()));
            return list;
        }
        return Collections.emptyList();
    }
}
