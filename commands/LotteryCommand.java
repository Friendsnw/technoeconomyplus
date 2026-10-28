package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.util.Format;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class LotteryCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public LotteryCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        var gate = plugin.getPermissionGate();
        var lang = plugin.getLanguageManager();
        if (!gate.requirePermission(sender, "technoeconomy.use")) return true;
        if (!gate.requireFeature(sender, "economy.games")) return true;

        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        double ticketPrice = plugin.getConfig().getDouble("lottery.ticket-price", 20.0);

        if (args.length == 0) {
            long remainingSec = Math.max(0, plugin.getLotteryManager().getNextDrawTimestamp() - Instant.now().getEpochSecond());
            long mins = remainingSec / 60;
            sender.sendMessage(lang.getMessage("messages.lottery-info", Map.of(
                    "pot", Format.money(plugin.getLotteryManager().getPot(), symbol),
                    "tickets", String.valueOf(plugin.getLotteryManager().getTotalTickets()),
                    "price", Format.money(ticketPrice, symbol),
                    "minutes", String.valueOf(mins)
            )));
            if (sender instanceof Player player) {
                int mine = plugin.getLotteryManager().getPlayerTickets(player.getUniqueId());
                sender.sendMessage(lang.getMessage("messages.lottery-my-tickets", Map.of("count", String.valueOf(mine))));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("buy")) {
            if (!gate.requirePlayer(sender)) return true;
            Player player = (Player) sender;
            int count = 1;
            if (args.length > 1) {
                try {
                    count = Integer.parseInt(args[1]);
                    if (count <= 0) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    player.sendMessage(lang.getMessage("messages.invalid-amount", Map.of("amount", args[1])));
                    return true;
                }
            }

            double totalCost = count * ticketPrice;
            if (!plugin.getBalanceManager().has(player.getUniqueId(), totalCost)) {
                player.sendMessage(lang.getMessage("messages.insufficient-funds"));
                return true;
            }

            plugin.getBalanceManager().removeBalance(player.getUniqueId(), totalCost);
            plugin.getLotteryManager().buyTickets(player.getUniqueId(), count);
            plugin.getTransactionLogManager().log(player.getUniqueId(), player.getName(), "LOTTERY", -totalCost,
                    plugin.getBalanceManager().getBalance(player.getUniqueId()), "Purchased " + count + " lottery tickets");

            player.sendMessage(lang.getMessage("messages.lottery-bought", Map.of(
                    "count", String.valueOf(count),
                    "cost", Format.money(totalCost, symbol)
            )));
            return true;
        }

        sender.sendMessage(lang.getMessage("messages.lottery-usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("buy").stream().filter(s -> s.startsWith(args[0].toLowerCase())).toList();
        }
        return List.of();
    }
}
