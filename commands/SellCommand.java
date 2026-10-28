package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.util.Format;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class SellCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public SellCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var gate = plugin.getPermissionGate();
        var lang = plugin.getLanguageManager();
        if (!gate.requirePlayer(sender)) return true;
        if (!gate.requirePermission(sender, "technoeconomy.use")) return true;
        if (!gate.requireFeature(sender, "economy.basic")) return true;

        Player player = (Player) sender;
        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            new com.techno.economy.gui.SellGui(plugin).open(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");

        if (sub.equals("hand")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType() == Material.AIR) {
                player.sendMessage(lang.getMessage("messages.worth-no-item"));
                return true;
            }
            double unitPrice = plugin.getWorthManager().getSellPrice(item.getType());
            if (unitPrice <= 0) {
                player.sendMessage(lang.getMessage("messages.sell-not-sellable"));
                return true;
            }

            int amountToSell = item.getAmount();
            if (args.length > 1) {
                try {
                    int custom = Integer.parseInt(args[1]);
                    if (custom > 0 && custom <= item.getAmount()) {
                        amountToSell = custom;
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            double total = unitPrice * amountToSell;
            item.setAmount(item.getAmount() - amountToSell);
            plugin.getBalanceManager().addBalance(player.getUniqueId(), total);
            plugin.getTransactionLogManager().log(player.getUniqueId(), player.getName(), "SELL", total,
                    plugin.getBalanceManager().getBalance(player.getUniqueId()), "Sold " + amountToSell + "x " + item.getType().name());

            player.sendMessage(lang.getMessage("messages.sell-success", Map.of(
                    "count", String.valueOf(amountToSell),
                    "amount", Format.money(total, symbol)
            )));
            return true;
        }

        if (sub.equals("all")) {
            double totalEarnings = 0;
            int totalItems = 0;

            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack is = player.getInventory().getItem(i);
                if (is != null && is.getType() != Material.AIR) {
                    double unit = plugin.getWorthManager().getSellPrice(is.getType());
                    if (unit > 0) {
                        int amt = is.getAmount();
                        totalEarnings += unit * amt;
                        totalItems += amt;
                        player.getInventory().setItem(i, null);
                    }
                }
            }

            if (totalItems == 0) {
                player.sendMessage(lang.getMessage("messages.sell-none-found"));
                return true;
            }

            plugin.getBalanceManager().addBalance(player.getUniqueId(), totalEarnings);
            plugin.getTransactionLogManager().log(player.getUniqueId(), player.getName(), "SELL_ALL", totalEarnings,
                    plugin.getBalanceManager().getBalance(player.getUniqueId()), "Sold all inventory (" + totalItems + " items)");

            player.sendMessage(lang.getMessage("messages.sell-success", Map.of(
                    "count", String.valueOf(totalItems),
                    "amount", Format.money(totalEarnings, symbol)
            )));
            return true;
        }

        player.sendMessage(lang.getMessage("messages.sell-usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("hand", "all").stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }
}
