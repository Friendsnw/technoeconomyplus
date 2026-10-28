package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class CoinflipCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public CoinflipCommand(TechnoEconomyPlus plugin) {
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
        if (args.length < 2) {
            player.sendMessage(lang.getMessage("messages.coinflip-usage"));
            return true;
        }

        double bet;
        try {
            bet = Double.parseDouble(args[0]);
            if (bet <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(lang.getMessage("messages.invalid-amount", Map.of("amount", args[0])));
            return true;
        }

        String guess = args[1].toLowerCase();
        if (!guess.equals("heads") && !guess.equals("tails")) {
            player.sendMessage(lang.getMessage("messages.coinflip-usage"));
            return true;
        }

        if (!plugin.getBalanceManager().has(player.getUniqueId(), bet)) {
            player.sendMessage(lang.getMessage("messages.insufficient-funds"));
            return true;
        }

        plugin.getBalanceManager().removeBalance(player.getUniqueId(), bet);
        plugin.getTransactionLogManager().log(player.getUniqueId(), player.getName(), "COINFLIP", -bet,
                plugin.getBalanceManager().getBalance(player.getUniqueId()), "Coinflip bet (" + guess + ")");

        plugin.getCasinoManager().playCoinflip(player, bet, guess);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2) {
            return List.of("heads", "tails").stream().filter(s -> s.startsWith(args[1].toLowerCase())).toList();
        }
        return List.of();
    }
}
