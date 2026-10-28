package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.util.Format;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class CasinoCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public CasinoCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var gate = plugin.getPermissionGate();
        var lang = plugin.getLanguageManager();
        if (!gate.requirePlayer(sender)) return true;
        if (!gate.requirePermission(sender, "technoeconomy.use")) return true;
        if (!gate.requireFeature(sender, "economy.games")) return true;

        Player player = (Player) sender;
        if (args.length == 0) {
            if (plugin.getMenuLoader().openMenu(player, "casino")) {
                return true;
            }
            player.sendMessage(lang.getMessage("messages.slots-usage"));
            return true;
        }

        if (args[0].equalsIgnoreCase("history")) {
            plugin.getCasinoManager().openCasinoHistoryMenu(player);
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("slots")) {
            player.sendMessage(lang.getMessage("messages.slots-usage"));
            return true;
        }

        double bet;
        try {
            bet = Double.parseDouble(args[1]);
            if (bet <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(lang.getMessage("messages.invalid-amount", Map.of("amount", args[1])));
            return true;
        }

        if (!plugin.getBalanceManager().has(player.getUniqueId(), bet)) {
            player.sendMessage(lang.getMessage("messages.insufficient-funds"));
            return true;
        }

        plugin.getBalanceManager().removeBalance(player.getUniqueId(), bet);
        plugin.getTransactionLogManager().log(player.getUniqueId(), player.getName(), "CASINO", -bet,
                plugin.getBalanceManager().getBalance(player.getUniqueId()), "Slots bet placed");

        plugin.getCasinoManager().openSlots(player, bet);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("slots").stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }
}
