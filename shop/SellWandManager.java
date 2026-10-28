package com.techno.economy.shop;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.util.ItemBuilder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class SellWandManager {

    private final TechnoEconomyPlus plugin;
    private final NamespacedKey usesKey;

    public SellWandManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
        this.usesKey = new NamespacedKey(plugin, "sellwand_uses");
    }

    public ItemStack createSellWand(int uses) {
        ItemStack wand = new ItemBuilder(Material.BLAZE_ROD)
                .name("§6§lTechno Sell Wand §7(" + uses + " uses)")
                .lore("§7Right click any chest or container", "§7to instantly sell all sellable items!", "", "§eRemaining uses: §f" + uses)
                .build();

        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, uses);
            wand.setItemMeta(meta);
        }
        return wand;
    }

    public boolean isSellWand(ItemStack item) {
        if (item == null || item.getType() != Material.BLAZE_ROD || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(usesKey, PersistentDataType.INTEGER);
    }

    public int getUses(ItemStack item) {
        if (!isSellWand(item)) return 0;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(usesKey, PersistentDataType.INTEGER, 0);
    }

    public void decrementUses(Player player, ItemStack item) {
        int current = getUses(item);
        if (current <= 1) {
            player.getInventory().setItemInMainHand(null);
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.sellwand-broken"));
        } else {
            int newUses = current - 1;
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(usesKey, PersistentDataType.INTEGER, newUses);
            meta.displayName(new ItemBuilder(Material.BLAZE_ROD).name("§6§lTechno Sell Wand §7(" + newUses + " uses)").build().getItemMeta().displayName());
            meta.lore(List.of(
                    new ItemBuilder(Material.BLAZE_ROD).name("§7Right click any chest or container").build().getItemMeta().displayName(),
                    new ItemBuilder(Material.BLAZE_ROD).name("§7to instantly sell all sellable items!").build().getItemMeta().displayName(),
                    new ItemBuilder(Material.BLAZE_ROD).name(" ").build().getItemMeta().displayName(),
                    new ItemBuilder(Material.BLAZE_ROD).name("§eRemaining uses: §f" + newUses).build().getItemMeta().displayName()
            ));
            item.setItemMeta(meta);
        }
    }
}
