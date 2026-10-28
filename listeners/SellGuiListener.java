package com.techno.economy.listeners;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.gui.SellGui;
import com.techno.economy.menu.MenuHolder;
import com.techno.economy.util.Format;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SellGuiListener implements Listener {

    private final TechnoEconomyPlus plugin;

    public SellGuiListener(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    private boolean isSellGui(Inventory inv) {
        return inv.getHolder() instanceof MenuHolder holder && holder.getMenuName().equals("sell_gui");
    }

    private boolean isTrashGui(Inventory inv) {
        return inv.getHolder() instanceof MenuHolder holder && holder.getMenuName().equals("trash");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSellClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();

        if (isTrashGui(top)) {
            int slot = event.getRawSlot();
            if (slot < 0 || slot >= top.getSize()) return;
            boolean isBorder = isBorderSlot(slot);
            if (isBorder) {
                event.setCancelled(true);
                return;
            }
            if (slot >= 0 && slot < top.getSize()) {
                event.setCancelled(true);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    clearTrashSlots(top);
                });
            }
            return;
        }

        if (!isSellGui(top)) return;

        int slot = event.getRawSlot();

        if (slot == SellGui.SELL_BUTTON_SLOT) {
            event.setCancelled(true);
            handleSellAll(player, top);
            return;
        }

        if (slot == SellGui.INFO_SLOT || isFixedSlot(slot)) {
            event.setCancelled(true);
            return;
        }

        if (SellGui.CENTER_SLOTS.contains(slot)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> updateSlotLore(top, slot));
            return;
        }

        if (slot >= top.getSize()) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory top = event.getView().getTopInventory();

        if (isTrashGui(top)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> clearTrashSlots(top));
            return;
        }

        if (!isSellGui(top)) return;

        for (int slot : event.getRawSlots()) {
            if (slot < top.getSize() && !SellGui.CENTER_SLOTS.contains(slot)) {
                event.setCancelled(true);
                return;
            }
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (int slot : event.getRawSlots()) {
                if (slot < top.getSize() && SellGui.CENTER_SLOTS.contains(slot)) {
                    updateSlotLore(top, slot);
                }
            }
        });
    }

    private void clearTrashSlots(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            if (!isBorderSlot(i) && i != 49) {
                inv.setItem(i, null);
            }
        }
    }

    private boolean isBorderSlot(int slot) {
        return slot <= 8 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8;
    }

    private boolean isFixedSlot(int slot) {
        return !SellGui.CENTER_SLOTS.contains(slot) && slot < 54;
    }

    private void updateSlotLore(Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        if (item == null || item.getType() == Material.AIR) return;
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        double buy = plugin.getWorthManager().getBuyPrice(item.getType());
        double sell = plugin.getWorthManager().getSellPrice(item.getType());
        inv.setItem(slot, SellGui.buildItemWithLore(item, buy, sell, symbol));
    }

    private void handleSellAll(Player player, Inventory inv) {
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        List<String> unsellable = new ArrayList<>();
        double totalEarnings = 0;
        int totalCount = 0;

        for (int slot : SellGui.CENTER_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            double price = plugin.getWorthManager().getSellPrice(item.getType());
            if (price <= 0) {
                String name = item.getType().name().toLowerCase().replace("_", " ");
                if (!unsellable.contains(name)) unsellable.add(name);
            }
        }

        if (!unsellable.isEmpty()) {
            String itemList = String.join(", ", unsellable);
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.sell-unsellable-items",
                    Map.of("items", itemList)));
            player.sendTitle("", "§c✖ " + itemList, 10, 40, 10);
            return;
        }

        for (int slot : SellGui.CENTER_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            double price = plugin.getWorthManager().getSellPrice(item.getType());
            double earned = price * item.getAmount();
            totalEarnings += earned;
            totalCount += item.getAmount();
            inv.setItem(slot, null);
        }

        if (totalCount == 0) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.sell-nothing-to-sell"));
            return;
        }

        plugin.getBalanceManager().addBalance(player.getUniqueId(), totalEarnings);
        plugin.getTransactionLogManager().log(player.getUniqueId(), player.getName(), "SELL_GUI",
                totalEarnings, plugin.getBalanceManager().getBalance(player.getUniqueId()),
                "Sold " + totalCount + " items via GUI");

        String amountStr = Format.money(totalEarnings, symbol);
        player.sendMessage(plugin.getLanguageManager().getMessage("messages.sell-all-success",
                Map.of("count", String.valueOf(totalCount), "symbol", symbol, "amount", amountStr)));
        player.sendTitle("", "§a+" + amountStr, 5, 30, 10);
    }
}
