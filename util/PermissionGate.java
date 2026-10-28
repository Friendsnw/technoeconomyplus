package com.techno.economy.util;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PermissionGate {

    private final TechnoEconomyPlus plugin;

    public PermissionGate(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public boolean requirePermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.no-permission"));
            return false;
        }
        return true;
    }

    public boolean requireFeature(CommandSender sender, String feature) {
        if (!plugin.getLicenseManager().hasPackageFeature(feature)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.license-feature-locked"));
            return false;
        }
        return true;
    }

    public boolean requireCommand(CommandSender sender, String command) {
        if (!plugin.getLicenseManager().hasPackageCommand(command)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.license-feature-locked"));
            return false;
        }
        return true;
    }

    public boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.players-only"));
            return false;
        }
        return true;
    }

    public boolean requireFeatureAndPermission(CommandSender sender, String permission, String feature) {
        return requirePermission(sender, permission) && requireFeature(sender, feature);
    }

    public boolean requireCommandAndPermission(CommandSender sender, String permission, String command) {
        return requirePermission(sender, permission) && requireCommand(sender, command);
    }

    public boolean hasPackageFeature(String feature) {
        return plugin.getLicenseManager().hasPackageFeature(feature);
    }

    public boolean hasPackageCommand(String command) {
        return plugin.getLicenseManager().hasPackageCommand(command);
    }

    public boolean isCommandEnabled(String command) {
        return plugin.getConfig().getBoolean("commands." + command.toLowerCase(), true);
    }

    public boolean checkCommandEnabled(CommandSender sender, String command) {
        if (!isCommandEnabled(command)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.command-disabled"));
            return false;
        }
        return true;
    }
}
