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

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BaltopGui {

    private final TechnoEconomyPlus plugin;
    private static final int PAGE_SIZE = 28;
    private static final int[] RANK_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    public BaltopGui(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void openPodium(Player player) {
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        var lang = plugin.getLanguageManager();

        String rawTitle = lang.getRawMessage("gui.baltop.podium-title");
        if (rawTitle.startsWith("gui.")) rawTitle = "&6&lTop Balances &8- &ePodium";
        String title = LegacyComponentSerializer.legacySection().serialize(
                LegacyComponentSerializer.legacyAmpersand().deserialize(rawTitle));

        Inventory inv = Bukkit.createInventory(new MenuHolder("baltop_podium"), 54, title);

        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, border);
        }

        ItemStack topInfo = new ItemBuilder(Material.BEACON)
                .name(lang.getRawMessage("gui.baltop.podium-header"))
                .lore(
                        lang.getRawMessage("gui.baltop.podium-desc1"),
                        lang.getRawMessage("gui.baltop.podium-desc2")
                ).build();
        inv.setItem(4, topInfo);

        List<Map.Entry<UUID, Double>> top = plugin.getBalanceManager().getCachedBaltop();

        if (!top.isEmpty()) {
            Map.Entry<UUID, Double> first = top.get(0);
            OfflinePlayer op1 = Bukkit.getOfflinePlayer(first.getKey());
            String name1 = op1.getName() != null ? op1.getName() : "Unknown";
            ItemStack head1 = createPlayerHead(op1,
                    "&6&l1. &e" + name1,
                    List.of(
                            "&7" + lang.getRawMessage("gui.baltop.balance-label") + ": &a" + symbol + Format.number(first.getValue()),
                            "&6" + lang.getRawMessage("gui.baltop.first-place")
                    ));
            inv.setItem(13, head1);
        } else {
            inv.setItem(13, new ItemBuilder(Material.SKELETON_SKULL).name("&7" + lang.getRawMessage("gui.baltop.empty-slot")).build());
        }

        if (top.size() > 1) {
            Map.Entry<UUID, Double> second = top.get(1);
            OfflinePlayer op2 = Bukkit.getOfflinePlayer(second.getKey());
            String name2 = op2.getName() != null ? op2.getName() : "Unknown";
            ItemStack head2 = createPlayerHead(op2,
                    "&f&l2. &f" + name2,
                    List.of(
                            "&7" + lang.getRawMessage("gui.baltop.balance-label") + ": &a" + symbol + Format.number(second.getValue()),
                            "&f" + lang.getRawMessage("gui.baltop.second-place")
                    ));
            inv.setItem(21, head2);
        } else {
            inv.setItem(21, new ItemBuilder(Material.SKELETON_SKULL).name("&7" + lang.getRawMessage("gui.baltop.empty-slot")).build());
        }

        if (top.size() > 2) {
            Map.Entry<UUID, Double> third = top.get(2);
            OfflinePlayer op3 = Bukkit.getOfflinePlayer(third.getKey());
            String name3 = op3.getName() != null ? op3.getName() : "Unknown";
            ItemStack head3 = createPlayerHead(op3,
                    "&c&l3. &6" + name3,
                    List.of(
                            "&7" + lang.getRawMessage("gui.baltop.balance-label") + ": &a" + symbol + Format.number(third.getValue()),
                            "&c" + lang.getRawMessage("gui.baltop.third-place")
                    ));
            inv.setItem(23, head3);
        } else {
            inv.setItem(23, new ItemBuilder(Material.SKELETON_SKULL).name("&7" + lang.getRawMessage("gui.baltop.empty-slot")).build());
        }

        inv.setItem(22, new ItemBuilder(Material.GOLD_BLOCK).name("&6#1 " + lang.getRawMessage("gui.baltop.pedestal")).build());
        inv.setItem(31, new ItemBuilder(Material.GOLD_BLOCK).name("&6#1 " + lang.getRawMessage("gui.baltop.pedestal")).build());

        inv.setItem(30, new ItemBuilder(Material.IRON_BLOCK).name("&f#2 " + lang.getRawMessage("gui.baltop.pedestal")).build());
        inv.setItem(32, new ItemBuilder(Material.COPPER_BLOCK).name("&c#3 " + lang.getRawMessage("gui.baltop.pedestal")).build());

        int myRank = -1;
        UUID myUuid = player.getUniqueId();
        for (int i = 0; i < top.size(); i++) {
            if (top.get(i).getKey().equals(myUuid)) {
                myRank = i + 1;
                break;
            }
        }
        double myBal = plugin.getBalanceManager().getBalance(myUuid);

        ItemStack myInfo = new ItemBuilder(Material.BOOK)
                .name(lang.getRawMessage("gui.baltop.my-rank-title"))
                .lore(
                        "&7" + lang.getRawMessage("gui.baltop.your-rank") + ": &e" + (myRank > 0 ? ("#" + myRank) : lang.getRawMessage("gui.baltop.unranked")),
                        "&7" + lang.getRawMessage("gui.baltop.your-balance") + ": &a" + symbol + Format.number(myBal),
                        "&7" + lang.getRawMessage("gui.baltop.total-tracked") + ": &f" + top.size()
                ).build();
        inv.setItem(40, myInfo);

        ItemStack allRankings = new ItemBuilder(Material.EMERALD_BLOCK)
                .name(lang.getRawMessage("gui.baltop.all-rankings-name"))
                .lore(
                        lang.getRawMessage("gui.baltop.all-rankings-lore1"),
                        lang.getRawMessage("gui.baltop.all-rankings-lore2")
                ).build();
        inv.setItem(49, allRankings);

        ItemStack close = new ItemBuilder(Material.BARRIER)
                .name(lang.getRawMessage("gui.baltop.close-name"))
                .build();
        inv.setItem(45, close);

        player.openInventory(inv);
    }

    public void openAllRankings(Player player, int page) {
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        var lang = plugin.getLanguageManager();

        List<Map.Entry<UUID, Double>> top = plugin.getBalanceManager().getCachedBaltop();
        int totalPages = Math.max(1, (int) Math.ceil((double) top.size() / PAGE_SIZE));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        String rawTitle = lang.getRawMessage("gui.baltop.all-title");
        if (rawTitle.startsWith("gui.")) rawTitle = "&6&lAll Balances &8- &7Page %page%/%total%";
        rawTitle = rawTitle.replace("%page%", String.valueOf(page + 1)).replace("%total%", String.valueOf(totalPages));
        String title = LegacyComponentSerializer.legacySection().serialize(
                LegacyComponentSerializer.legacyAmpersand().deserialize(rawTitle));

        Inventory inv = Bukkit.createInventory(new MenuHolder("baltop_all:" + page), 54, title);

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

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, top.size());

        for (int i = start; i < end; i++) {
            int slotIdx = i - start;
            if (slotIdx >= RANK_SLOTS.length) break;
            int slot = RANK_SLOTS[slotIdx];

            Map.Entry<UUID, Double> entry = top.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            String name = op.getName() != null ? op.getName() : "Unknown";
            int rank = i + 1;

            String rankColor = rank == 1 ? "&6" : (rank == 2 ? "&f" : (rank == 3 ? "&c" : "&e"));
            double bankBal = plugin.getBankManager() != null ? plugin.getBankManager().getBankBalance(entry.getKey()) : 0.0;
            var clan = plugin.getClanManager() != null ? plugin.getClanManager().getClanByPlayer(entry.getKey()) : null;

            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add("&7" + lang.getRawMessage("gui.baltop.balance-label") + ": &a" + symbol + Format.number(entry.getValue()));
            if (bankBal > 0) {
                lore.add("&7" + lang.getRawMessage("gui.baltop.bank-label") + ": &f" + symbol + Format.number(bankBal));
            }
            if (clan != null) {
                lore.add("&7" + lang.getRawMessage("gui.baltop.clan-label") + ": &6[" + clan.getTag() + "] " + clan.getName());
            }

            ItemStack head = createPlayerHead(op, rankColor + "#" + rank + " &f" + name, lore);
            inv.setItem(slot, head);
        }

        if (page > 0) {
            ItemStack prev = new ItemBuilder(Material.ARROW)
                    .name(lang.getRawMessage("gui.baltop.prev-page"))
                    .build();
            inv.setItem(45, prev);
        }

        ItemStack backPodium = new ItemBuilder(Material.TOTEM_OF_UNDYING)
                .name(lang.getRawMessage("gui.baltop.back-podium-name"))
                .lore("&7" + lang.getRawMessage("gui.baltop.back-podium-lore"))
                .build();
        inv.setItem(49, backPodium);

        if (page < totalPages - 1) {
            ItemStack next = new ItemBuilder(Material.ARROW)
                    .name(lang.getRawMessage("gui.baltop.next-page"))
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
            java.util.List<net.kyori.adventure.text.Component> compList = new java.util.ArrayList<>();
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
