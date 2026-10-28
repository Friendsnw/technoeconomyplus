package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class HudCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public HudCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean isAdmin = (sender instanceof ConsoleCommandSender) || sender.hasPermission("technoeconomy.admin") || sender.isOp();
        if (!isAdmin) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.no-permission"));
            return true;
        }

        boolean current = plugin.getHudManager().isGlobalEnabled();

        if (args.length == 0 || args[0].equalsIgnoreCase("toggle")) {
            boolean newState = !current;
            plugin.getHudManager().setGlobalHudEnabled(newState);
            sender.sendMessage(plugin.getLanguageManager().getMessage(newState ? "messages.hud-global-enabled" : "messages.hud-global-disabled"));
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            if (current) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("messages.hud-already-on"));
            } else {
                plugin.getHudManager().setGlobalHudEnabled(true);
                sender.sendMessage(plugin.getLanguageManager().getMessage("messages.hud-global-enabled"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("off")) {
            if (!current) {
                sender.sendMessage(plugin.getLanguageManager().getMessage("messages.hud-already-off"));
            } else {
                plugin.getHudManager().setGlobalHudEnabled(false);
                sender.sendMessage(plugin.getLanguageManager().getMessage("messages.hud-global-disabled"));
            }
            return true;
        }

        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.hud-usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("toggle", "on", "off");
        }
        return List.of();
    }
}
