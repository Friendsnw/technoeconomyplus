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

public class PaydayCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public PaydayCommand(TechnoEconomyPlus plugin) {
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
        long now = System.currentTimeMillis();
        long intervalMillis = plugin.getConfig().getLong("payday.interval-minutes", 60L) * 60L * 1000L;
        long lastClaim = plugin.getDatabaseManager().getLastPaydayClaim(player.getUniqueId());
        long elapsed = now - lastClaim;

        if (elapsed < intervalMillis) {
            long remainingSeconds = (intervalMillis - elapsed) / 1000L;
            long minutes = remainingSeconds / 60;
            long seconds = remainingSeconds % 60;
            String timeStr = minutes > 0 ? (minutes + "m " + seconds + "s") : (seconds + "s");
            player.sendMessage(lang.getMessage("messages.payday-cooldown", Map.of("time", timeStr)));
            return true;
        }

        double salary = plugin.getConfig().getDouble("payday.amount", 50.0);
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");

        plugin.getDatabaseManager().setLastPaydayClaim(player.getUniqueId(), now);
        plugin.getBalanceManager().addBalance(player.getUniqueId(), salary);
        plugin.getTransactionLogManager().log(player.getUniqueId(), player.getName(), "PAYDAY", salary,
                plugin.getBalanceManager().getBalance(player.getUniqueId()), "Manual payday claim");

        player.sendMessage(lang.getMessage("messages.payday-claimed", Map.of(
                "amount", Format.money(salary, symbol)
        )));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
