package com.techno.economy.gui;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.menu.MenuHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SellGui {

    public static final Set<Integer> CENTER_SLOTS = Set.of(
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    );

    public static final int SELL_BUTTON_SLOT = 49;
    public static final int INFO_SLOT = 4;

    private final TechnoEconomyPlus plugin;

    public SellGui(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        Component title = LegacyComponentSerializer.legacyAmpersand().deserialize("&8&l💰 Sell Items");
        Inventory inv = Bukkit.createInventory(new MenuHolder("sell_gui"), 54, title);

        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.displayName(Component.empty());
            border.setItemMeta(borderMeta);
        }

        int[] borderSlots = {0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52,53};
        for (int s : borderSlots) {
            if (s != SELL_BUTTON_SLOT && s != INFO_SLOT) inv.setItem(s, border.clone());
        }

        ItemStack infoItem = new ItemStack(Material.GOLD_INGOT);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&e&lSell Window"));
            List<Component> lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&7Place items in center slots."));
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&7Each item shows its sell value."));
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&cRed = cannot be sold."));
            infoMeta.lore(lore);
            infoItem.setItemMeta(infoMeta);
        }
        inv.setItem(INFO_SLOT, infoItem);

        ItemStack sellBtn = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta sellMeta = sellBtn.getItemMeta();
        if (sellMeta != null) {
            sellMeta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&a&l✔ Sell All"));
            List<Component> lore = new ArrayList<>();
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&7Click to sell all items here."));
            sellMeta.lore(lore);
            sellBtn.setItemMeta(sellMeta);
        }
        inv.setItem(SELL_BUTTON_SLOT, sellBtn);

        player.openInventory(inv);
    }

    public static ItemStack buildItemWithLore(ItemStack original, double buy, double sell, String symbol) {
        if (original == null || original.getType() == Material.AIR) return original;
        ItemStack copy = original.clone();
        copy.setAmount(original.getAmount());
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) return copy;

        List<Component> lore = new ArrayList<>();
        if (sell > 0) {
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    "&7Buy: &f" + symbol + String.format("%.2f", buy) + "  &7Sell: &a" + symbol + String.format("%.2f", sell)));
        } else {
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&c&lCANNOT SELL"));
        }
        meta.lore(lore);
        copy.setItemMeta(meta);
        return copy;
    }
}
