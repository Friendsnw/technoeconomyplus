package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SellWandCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public SellWandCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var gate = plugin.getPermissionGate();
        var lang = plugin.getLanguageManager();
        if (!gate.requirePermission(sender, "technoeconomy.admin")) return true;
        if (!gate.requireFeature(sender, "economy.admin")) return true;

        if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(lang.getMessage("messages.invalid-usage", Map.of("usage", "/sellwand give <player> <uses>")));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(lang.getMessage("messages.player-not-found", Map.of("player", args[1])));
            return true;
        }

        int uses;
        try {
            uses = Integer.parseInt(args[2]);
            if (uses <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            sender.sendMessage(lang.getMessage("messages.invalid-amount", Map.of("amount", args[2])));
            return true;
        }

        ItemStack wand = plugin.getSellWandManager().createSellWand(uses);
        target.getInventory().addItem(wand);
        sender.sendMessage(lang.getMessage("messages.sellwand-given", Map.of(
                "player", target.getName(),
                "uses", String.valueOf(uses)
        )));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("give").stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String partial = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(partial)).collect(Collectors.toList());
        }
        return List.of();
    }
}
