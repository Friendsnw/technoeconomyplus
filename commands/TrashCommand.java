package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class TrashCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public TrashCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.players-only"));
            return true;
        }
        if (!player.hasPermission("technoeconomy.use")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.no-permission"));
            return true;
        }
        if (!plugin.getFeatureManager().isEnabled("trash")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.feature-disabled"));
            return true;
        }
        plugin.getMenuLoader().openMenu(player, "trash");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
