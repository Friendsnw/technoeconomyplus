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

public class DailyCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public DailyCommand(TechnoEconomyPlus plugin) {
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
        if (args.length == 0 && plugin.getMenuLoader().openMenu(player, "daily")) {
            return true;
        }

        if (!plugin.getDailyManager().canClaim(player.getUniqueId())) {
            player.sendMessage(lang.getMessage("messages.daily-already-claimed"));
            return true;
        }

        double reward = plugin.getDailyManager().claim(player.getUniqueId());
        plugin.getBalanceManager().addBalance(player.getUniqueId(), reward);
        int streak = plugin.getDailyManager().getStreak(player.getUniqueId());
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");

        plugin.getTransactionLogManager().log(player.getUniqueId(), player.getName(), "DAILY", reward,
                plugin.getBalanceManager().getBalance(player.getUniqueId()), "Daily reward (Streak: " + streak + ")");

        player.sendMessage(lang.getMessage("messages.daily-claimed", Map.of(
                "amount", Format.number(reward),
                "symbol", symbol,
                "streak", String.valueOf(streak)
        )));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
