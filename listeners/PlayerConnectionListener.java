package com.techno.economy.listeners;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Map;
import java.util.Set;

public class PlayerConnectionListener implements Listener {

    private final TechnoEconomyPlus plugin;
    private static final Set<String> MANAGED_COMMANDS = Set.of(
            "balance", "eco", "pay", "baltop", "bank", "daily", "bounty", "jobs",
            "casino", "coinflip", "lottery", "worth", "sell", "sellwand", "market",
            "shop", "ah", "chestshop", "payday", "trash", "hud", "stock", "company",
            "check", "duel", "technoeconomyplus"
    );

    private static final Map<String, String> ALIAS_MAP = Map.of(
            "tep", "technoeconomyplus",
            "borsa", "stock",
            "sirket", "company",
            "cek", "check",
            "duello", "duel",
            "cop", "trash"
    );

    public PlayerConnectionListener(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean isAdmin = player.isOp() || player.hasPermission("technoeconomyplus.admin");
        if (!isAdmin) return;

        boolean checkEnabled = plugin.getConfig().getBoolean("updates.check", true);
        if (!checkEnabled) return;

        if (plugin.getLicenseManager().isUpdateAvailable()) {
            player.sendMessage(plugin.getLicenseManager().getUpdateNoticeMessage(false));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        if (msg.startsWith("/")) msg = msg.substring(1);
        String[] parts = msg.split(" ", 2);
        if (parts.length == 0) return;

        String cmd = parts[0].toLowerCase();
        if (cmd.contains(":")) {
            cmd = cmd.substring(cmd.indexOf(':') + 1);
        }

        String mapped = ALIAS_MAP.getOrDefault(cmd, cmd);
        if (!MANAGED_COMMANDS.contains(mapped)) return;

        if (!plugin.getPermissionGate().isCommandEnabled(mapped)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.getLanguageManager().getMessage("messages.command-disabled"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerCommand(ServerCommandEvent event) {
        String msg = event.getCommand();
        if (msg.startsWith("/")) msg = msg.substring(1);
        String[] parts = msg.split(" ", 2);
        if (parts.length == 0) return;

        String cmd = parts[0].toLowerCase();
        if (cmd.contains(":")) {
            cmd = cmd.substring(cmd.indexOf(':') + 1);
        }

        String mapped = ALIAS_MAP.getOrDefault(cmd, cmd);
        if (!MANAGED_COMMANDS.contains(mapped)) return;

        if (!plugin.getPermissionGate().isCommandEnabled(mapped)) {
            event.setCancelled(true);
            event.getSender().sendMessage(plugin.getLanguageManager().getMessage("messages.command-disabled"));
        }
    }
}
