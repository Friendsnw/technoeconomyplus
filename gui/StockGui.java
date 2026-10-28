package com.techno.economy.gui;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.menu.MenuHolder;
import com.techno.economy.util.Format;
import com.techno.economy.util.ItemBuilder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StockGui {

    private final TechnoEconomyPlus plugin;
    public static final Material[] COMMODITIES = {
            Material.DIAMOND,
            Material.GOLD_INGOT,
            Material.EMERALD,
            Material.IRON_INGOT,
            Material.NETHERITE_INGOT,
            Material.COPPER_INGOT,
            Material.LAPIS_LAZULI
    };

    public StockGui(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        UUID uuid = player.getUniqueId();
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        var lang = plugin.getLanguageManager();

        String rawTitle = lang.getRawMessage("gui.stock.title");
        if (rawTitle.startsWith("gui.")) rawTitle = "&8&lTechno Stock &7- &fLive Market";
        String title = LegacyComponentSerializer.legacySection().serialize(
                LegacyComponentSerializer.legacyAmpersand().deserialize(rawTitle));

        Inventory inv = Bukkit.createInventory(new MenuHolder("stock_main"), 54, title);

        ItemStack border = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        int[] borderSlots = {0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 48, 50, 52, 53};
        for (int s : borderSlots) {
            inv.setItem(s, border);
        }

        ItemStack header = new ItemBuilder(Material.NETHER_STAR)
                .name(lang.getRawMessage("gui.stock.header-name"))
                .lore(
                        lang.getRawMessage("gui.stock.header-lore1"),
                        lang.getRawMessage("gui.stock.header-lore2")
                ).build();
        inv.setItem(4, header);

        for (int i = 0; i < COMMODITIES.length; i++) {
            Material mat = COMMODITIES[i];
            double price = plugin.getStockManager().getOrePrice(mat);
            String name = formatName(mat.name());

            ItemStack item = new ItemBuilder(mat)
                    .name("&6&l" + name)
                    .lore(
                            "&7" + lang.getRawMessage("gui.stock.price-label") + ": &a" + symbol + Format.number(price),
                            "",
                            "&e" + lang.getRawMessage("gui.stock.buy-click") + " &7(1x)",
                            "&e" + lang.getRawMessage("gui.stock.buy-shift") + " &7(16x)",
                            "&6" + lang.getRawMessage("gui.stock.sell-click") + " &7(1x)",
                            "&6" + lang.getRawMessage("gui.stock.sell-shift") + " &7(All)"
                    ).build();
            inv.setItem(10 + i, item);
        }

        List<String> companies = plugin.getStockManager().getAllStockCompanies();
        Map<String, Integer> ownedMap = plugin.getStockManager().getPlayerShares(uuid);

        int[] companySlots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        for (int i = 0; i < companySlots.length; i++) {
            int slot = companySlots[i];
            if (i < companies.size()) {
                String cName = companies.get(i);
                double sharePrice = plugin.getStockManager().getCompanySharePrice(cName);
                int avail = plugin.getStockManager().getAvailableShares(cName);
                int owned = ownedMap.getOrDefault(cName, 0);

                ItemStack cItem = new ItemBuilder(Material.PAPER)
                        .name("&e&l" + cName)
                        .lore(
                                "&7" + lang.getRawMessage("gui.stock.share-price") + ": &a" + symbol + Format.number(sharePrice),
                                "&7" + lang.getRawMessage("gui.stock.available-shares") + ": &f" + Format.number(avail),
                                "&7" + lang.getRawMessage("gui.stock.owned-shares") + ": &b" + Format.number(owned),
                                "",
                                "&a" + lang.getRawMessage("gui.stock.share-buy-1"),
                                "&a" + lang.getRawMessage("gui.stock.share-buy-10"),
                                "&c" + lang.getRawMessage("gui.stock.share-sell-1"),
                                "&c" + lang.getRawMessage("gui.stock.share-sell-10")
                        ).build();
                inv.setItem(slot, cItem);
            } else {
                inv.setItem(slot, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build());
            }
        }

        double portVal = plugin.getStockManager().getPortfolioValue(uuid);
        double pl = plugin.getStockManager().getProfitLoss(uuid);
        String plColor = pl >= 0 ? "&a+" : "&c";

        ItemStack portfolio = new ItemBuilder(Material.BOOK)
                .name(lang.getRawMessage("gui.stock.portfolio-name"))
                .lore(
                        "&7" + lang.getRawMessage("gui.stock.portfolio-value") + ": &f" + symbol + Format.number(portVal),
                        "&7" + lang.getRawMessage("gui.stock.portfolio-pl") + ": " + plColor + symbol + Format.number(pl),
                        "",
                        "&e" + lang.getRawMessage("gui.stock.portfolio-click")
                ).build();
        inv.setItem(47, portfolio);

        ItemStack close = new ItemBuilder(Material.BARRIER)
                .name(lang.getRawMessage("gui.stock.close-name"))
                .build();
        inv.setItem(49, close);

        ItemStack refresh = new ItemBuilder(Material.CLOCK)
                .name(lang.getRawMessage("gui.stock.refresh-name"))
                .lore("&7" + lang.getRawMessage("gui.stock.refresh-lore"))
                .build();
        inv.setItem(51, refresh);

        player.openInventory(inv);
    }

    private String formatName(String raw) {
        String[] parts = raw.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
