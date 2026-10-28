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

public class WorthCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public WorthCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var gate = plugin.getPermissionGate();
        var lang = plugin.getLanguageManager();
        if (!gate.requirePlayer(sender)) return true;
        if (!gate.requirePermission(sender, "technoeconomy.use")) return true;

        Player player = (Player) sender;
        Material mat;
        if (args.length > 0) {
            try {
                mat = Material.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(lang.getMessage("messages.invalid-item"));
                return true;
            }
        } else {
            ItemStack inHand = player.getInventory().getItemInMainHand();
            if (inHand == null || inHand.getType() == Material.AIR) {
                player.sendMessage(lang.getMessage("messages.worth-no-item"));
                return true;
            }
            mat = inHand.getType();
        }

        double buy = plugin.getWorthManager().getBuyPrice(mat);
        double sell = plugin.getWorthManager().getSellPrice(mat);
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        String notSellable = lang.getRawMessage("messages.worth-not-sellable");

        player.sendMessage(lang.getMessage("messages.worth-info", Map.of(
                "item", mat.name().toLowerCase().replace("_", " "),
                "buy", (buy > 0 ? Format.money(buy, symbol) : notSellable),
                "sell", (sell > 0 ? Format.money(sell, symbol) : notSellable)
        )));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
