package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class ChestShopCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public ChestShopCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.getPermissionGate().requirePlayer(sender)) return true;
        if (!plugin.getPermissionGate().requirePermission(sender, "technoeconomy.use")) return true;
        if (!plugin.getPermissionGate().requireFeature(sender, "economy.chestshop")) return true;

        Player player = (Player) sender;
        player.sendMessage(plugin.getLanguageManager().getMessage("messages.chestshop-usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
