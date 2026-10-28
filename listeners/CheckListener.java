package com.techno.economy.listeners;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class CheckListener implements Listener {

    private final TechnoEconomyPlus plugin;

    public CheckListener(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.PAPER) return;

        if (!item.hasItemMeta() || !item.getItemMeta().getPersistentDataContainer().has(plugin.getCheckManager().getCheckKey(), PersistentDataType.STRING)) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        boolean redeemed = plugin.getCheckManager().redeemCheck(player, item);

        if (redeemed) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.check-redeemed"));
        } else {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.check-invalid"));
        }
    }
}
