package com.techno.economy.listeners;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DuelListener implements Listener {

    private final TechnoEconomyPlus plugin;

    public DuelListener(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (plugin.getDuelManager().isInDuel(victim.getUniqueId())) {
            plugin.getDuelManager().handlePvPEnd(victim, "DEATH");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            boolean disconnectLoss = plugin.getConfig().getBoolean("duello.disconnect-loss", true);
            if (disconnectLoss) {
                plugin.getDuelManager().handlePvPEnd(player, "DISCONNECT");
            }
        }
    }
}
