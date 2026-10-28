package com.techno.economy.gui;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.menu.MenuHolder;
import com.techno.economy.util.Format;
import com.techno.economy.util.ItemBuilder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BountyGui {

    private final TechnoEconomyPlus plugin;
    private static final int PAGE_SIZE = 28;
    private static final int[] BOUNTY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public BountyGui(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        var lang = plugin.getLanguageManager();

        Map<UUID, Double> bounties = plugin.getBountyManager().getAllBounties();
        List<Map.Entry<UUID, Double>> list = new ArrayList<>(bounties.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        int totalPages = Math.max(1, (int) Math.ceil((double) list.size() / PAGE_SIZE));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        String rawTitle = lang.getRawMessage("gui.bounty.title");
        if (rawTitle.startsWith("gui.")) rawTitle = "&c&lActive Bounties &8- &7Page %page%/%total%";
        rawTitle = rawTitle.replace("%page%", String.valueOf(page + 1)).replace("%total%", String.valueOf(totalPages));
        String title = LegacyComponentSerializer.legacySection().serialize(
                LegacyComponentSerializer.legacyAmpersand().deserialize(rawTitle));

        Inventory inv = Bukkit.createInventory(new MenuHolder("bounty_main:" + page), 54, title);

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        inv.setItem(9, border);
        inv.setItem(17, border);
        inv.setItem(18, border);
        inv.setItem(26, border);
        inv.setItem(27, border);
        inv.setItem(35, border);
        inv.setItem(36, border);
        inv.setItem(44, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);

        ItemStack header = new ItemBuilder(Material.TARGET)
                .name(lang.getRawMessage("gui.bounty.header-name"))
                .lore(
                        lang.getRawMessage("gui.bounty.header-lore1"),
                        lang.getRawMessage("gui.bounty.header-lore2")
                ).build();
        inv.setItem(4, header);

        if (list.isEmpty()) {
            ItemStack empty = new ItemBuilder(Material.BARRIER)
                    .name(lang.getRawMessage("gui.bounty.empty-title"))
                    .lore("&7" + lang.getRawMessage("gui.bounty.empty-desc"))
                    .build();
            inv.setItem(22, empty);
        } else {
            int start = page * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, list.size());

            for (int i = start; i < end; i++) {
                int slotIdx = i - start;
                if (slotIdx >= BOUNTY_SLOTS.length) break;
                int slot = BOUNTY_SLOTS[slotIdx];

                Map.Entry<UUID, Double> entry = list.get(i);
                UUID targetUuid = entry.getKey();
                OfflinePlayer op = Bukkit.getOfflinePlayer(targetUuid);
                String name = op.getName() != null ? op.getName() : "Unknown";
                boolean isOnline = op.isOnline();

                List<String> lore = new ArrayList<>();
                lore.add("&7" + lang.getRawMessage("gui.bounty.reward-label") + ": &e" + symbol + Format.number(entry.getValue()));
                lore.add("&7" + lang.getRawMessage("gui.bounty.status-label") + ": " + (isOnline ? "&aOnline" : "&7Offline"));
                lore.add("");
                lore.add("&e" + lang.getRawMessage("gui.bounty.click-track"));

                ItemStack head = createPlayerHead(op, "&c&l" + name, lore);
                inv.setItem(slot, head);
            }
        }

        if (page > 0) {
            ItemStack prev = new ItemBuilder(Material.ARROW)
                    .name(lang.getRawMessage("gui.bounty.prev-page"))
                    .build();
            inv.setItem(45, prev);
        }

        ItemStack placeBounty = new ItemBuilder(Material.ANVIL)
                .name(lang.getRawMessage("gui.bounty.place-name"))
                .lore(
                        lang.getRawMessage("gui.bounty.place-lore1"),
                        lang.getRawMessage("gui.bounty.place-lore2")
                ).build();
        inv.setItem(49, placeBounty);

        ItemStack close = new ItemBuilder(Material.BARRIER)
                .name(lang.getRawMessage("gui.bounty.close-name"))
                .build();
        inv.setItem(47, close);

        ItemStack refresh = new ItemBuilder(Material.CLOCK)
                .name(lang.getRawMessage("gui.bounty.refresh-name"))
                .lore("&7" + lang.getRawMessage("gui.bounty.refresh-lore"))
                .build();
        inv.setItem(51, refresh);

        if (page < totalPages - 1) {
            ItemStack next = new ItemBuilder(Material.ARROW)
                    .name(lang.getRawMessage("gui.bounty.next-page"))
                    .build();
            inv.setItem(53, next);
        }

        player.openInventory(inv);
    }

    private ItemStack createPlayerHead(OfflinePlayer player, String displayName, List<String> loreLines) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(displayName)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            java.util.List<net.kyori.adventure.text.Component> compList = new ArrayList<>();
            for (String l : loreLines) {
                compList.add(LegacyComponentSerializer.legacyAmpersand().deserialize(l)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            }
            meta.lore(compList);
            skull.setItemMeta(meta);
        }
        return skull;
    }
}
