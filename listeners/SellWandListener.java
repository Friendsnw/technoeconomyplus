package com.techno.economy.listeners;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.util.Format;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class SellWandListener implements Listener {

    private final TechnoEconomyPlus plugin;

    public SellWandListener(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();

        if (!plugin.getSellWandManager().isSellWand(inHand)) return;

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Container container)) return;

        event.setCancelled(true);

        double totalEarned = 0;
        int totalItems = 0;

        for (int i = 0; i < container.getInventory().getSize(); i++) {
            ItemStack is = container.getInventory().getItem(i);
            if (is != null && is.getType() != Material.AIR) {
                double unitPrice = plugin.getWorthManager().getSellPrice(is.getType());
                if (unitPrice > 0) {
                    int count = is.getAmount();
                    totalEarned += unitPrice * count;
                    totalItems += count;
                    container.getInventory().setItem(i, null);
                }
            }
        }

        if (totalItems == 0) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.sellwand-empty"));
            return;
        }

        plugin.getBalanceManager().addBalance(player.getUniqueId(), totalEarned);
        plugin.getSellWandManager().decrementUses(player, inHand);
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");

        plugin.getTransactionLogManager().log(player.getUniqueId(), player.getName(), "SELL_WAND", totalEarned,
                plugin.getBalanceManager().getBalance(player.getUniqueId()), "Sold container (" + totalItems + " items)");

        player.sendMessage(plugin.getLanguageManager().getMessage("messages.sellwand-success", Map.of(
                "count", String.valueOf(totalItems),
                "amount", Format.money(totalEarned, symbol)
        )));
    }
}
