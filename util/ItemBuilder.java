package com.techno.economy.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder name(String name) {
        if (meta != null) {
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));
        }
        return this;
    }

    public ItemBuilder name(Component component) {
        if (meta != null) {
            meta.displayName(component);
        }
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        if (meta != null) {
            List<Component> componentLines = new ArrayList<>();
            for (String line : lines) {
                componentLines.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
            }
            meta.lore(componentLines);
        }
        return this;
    }

    public ItemBuilder lore(String... lines) {
        return lore(List.of(lines));
    }

    public ItemBuilder loreComponents(List<Component> lines) {
        if (meta != null) {
            meta.lore(lines);
        }
        return this;
    }

    public ItemBuilder loreComponents(Component... lines) {
        return loreComponents(List.of(lines));
    }

    public ItemBuilder customModelData(int data) {
        if (meta != null) {
            meta.setCustomModelData(data);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
