package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class AhCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public AhCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var gate = plugin.getPermissionGate();
        var lang = plugin.getLanguageManager();
        if (!gate.requirePlayer(sender)) return true;
        if (!gate.requirePermission(sender, "technoeconomy.use")) return true;
        if (!gate.requireFeature(sender, "economy.ah")) return true;

        Player player = (Player) sender;
        if (args.length == 0) {
            plugin.getAuctionHouseManager().openAuctionHouse(player, 0);
            return true;
        }

        if (args[0].equalsIgnoreCase("sell")) {
            if (args.length < 2) {
                player.sendMessage(lang.getMessage("messages.ah-sell-usage"));
                return true;
            }

            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                player.sendMessage(lang.getMessage("messages.worth-no-item"));
                return true;
            }

            double price;
            try {
                price = Double.parseDouble(args[1]);
                if (price <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                player.sendMessage(lang.getMessage("messages.invalid-amount", Map.of("amount", args[1])));
                return true;
            }

            ItemStack toList = hand.clone();
            player.getInventory().setItemInMainHand(null);
            plugin.getAuctionHouseManager().listItem(player, toList, price);

            String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
            player.sendMessage(lang.getMessage("messages.ah-listed", Map.of(
                    "item", toList.getType().name(),
                    "price", String.valueOf(price),
                    "symbol", symbol
            )));
            return true;
        }

        plugin.getAuctionHouseManager().openAuctionHouse(player, 0);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("sell").stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }
}
