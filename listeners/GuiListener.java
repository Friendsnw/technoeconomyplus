package com.techno.economy.listeners;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.auction.AuctionHouseHolder;
import com.techno.economy.auction.AuctionHouseManager;
import com.techno.economy.shop.MarketManager;
import com.techno.economy.util.Format;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Map;

public class GuiListener implements Listener {

    private final TechnoEconomyPlus plugin;

    public GuiListener(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (event.getInventory().getHolder() instanceof AuctionHouseHolder ahHolder) {
            event.setCancelled(true);
            handleAuctionHouseClick(event, player, ahHolder);
            return;
        }

        com.techno.economy.menu.MenuHolder holder2 = null;
        if (event.getInventory().getHolder() instanceof com.techno.economy.menu.MenuHolder h) {
            holder2 = h;
        }

        if (holder2 != null) {
            String mn = holder2.getMenuName();
            if ("checks_list".equalsIgnoreCase(mn)) {
                event.setCancelled(true);
                if (event.getRawSlot() == 49) {
                    plugin.getMenuLoader().openMenu(player, "check");
                }
                return;
            }
            if ("casino_history".equalsIgnoreCase(mn)) {
                event.setCancelled(true);
                if (event.getRawSlot() == 49) {
                    plugin.getMenuLoader().openMenu(player, "casino");
                }
                return;
            }
            if ("stock_main".equalsIgnoreCase(mn)) {
                handleStockClick(event, player);
                return;
            }
            if ("baltop_podium".equalsIgnoreCase(mn) || mn.startsWith("baltop_all:")) {
                handleBaltopClick(event, player, mn);
                return;
            }
            if ("admin_main".equalsIgnoreCase(mn)) {
                handleAdminClick(event, player);
                return;
            }
            if (mn.startsWith("bounty_main:")) {
                handleBountyClick(event, player, mn);
                return;
            }
            if (!mn.startsWith("market")) {
                plugin.getMenuLoader().handleClick(event);
                return;
            }
        }

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());

        if (title.contains("TechnoCasino") && title.contains("Slots")) {
            event.setCancelled(true);
            return;
        }

        if (title.contains("TechnoMarket")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            String menuName = holder2 != null ? holder2.getMenuName() : "";

            if (menuName.equals("market_categories")) {
                if (clicked.getType() == Material.BARRIER) { player.closeInventory(); return; }
                if (clicked.getType() == Material.COMPASS) {
                    plugin.getMarketManager().openMarket(player, "all", 0); return;
                }
                String catName = plugin.getMarketManager().getCategories().stream()
                        .filter(c -> {
                            var im = clicked.getItemMeta();
                            if (im == null) return false;
                            String dn = PlainTextComponentSerializer.plainText().serialize(im.displayName());
                            return dn.toLowerCase().contains(c.replace("_", " ").toLowerCase());
                        }).findFirst().orElse(null);
                if (catName != null) plugin.getMarketManager().openMarket(player, catName, 0);
                return;
            }

            if (menuName.startsWith("market:")) {
                String[] parts = menuName.split(":");
                String cat = parts.length > 1 ? parts[1] : "all";
                int pg = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

                if (clicked.getType() == Material.BARRIER) { player.closeInventory(); return; }
                if (clicked.getType() == Material.BOOK) { plugin.getMarketManager().openCategoryMenu(player); return; }
                if (clicked.getType() == Material.ARROW) {
                    int totalPages = (int) Math.ceil((double) plugin.getMarketManager().getItems().stream()
                            .filter(i -> cat.equalsIgnoreCase("all") || i.category.equalsIgnoreCase(cat)).count() / 45.0);
                    if (event.getRawSlot() == 45 && pg > 0) plugin.getMarketManager().openMarket(player, cat, pg - 1);
                    if (event.getRawSlot() == 53 && pg < totalPages - 1) plugin.getMarketManager().openMarket(player, cat, pg + 1);
                    return;
                }
                MarketManager.MarketItem mItem = plugin.getMarketManager().getItemByMaterial(clicked.getType());
                if (mItem != null) {
                    String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
                    if (event.isLeftClick()) {
                        if (!plugin.getBalanceManager().has(player.getUniqueId(), mItem.buyPrice)) {
                            player.sendMessage(plugin.getLanguageManager().getMessage("messages.insufficient-funds"));
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                            return;
                        }
                        plugin.getBalanceManager().removeBalance(player.getUniqueId(), mItem.buyPrice);
                        player.getInventory().addItem(new ItemStack(mItem.material, 1));
                        plugin.getTransactionLogManager().log(player.getUniqueId(), player.getName(), "MARKET_BUY", -mItem.buyPrice,
                                plugin.getBalanceManager().getBalance(player.getUniqueId()), "Market buy 1x " + mItem.material.name());
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.market-buy-success", Map.of(
                                "item", mItem.material.name(), "price", String.valueOf(mItem.buyPrice), "symbol", symbol)));
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
                    } else if (event.isRightClick()) {
                        if (!player.getInventory().containsAtLeast(new ItemStack(mItem.material), 1)) {
                            player.sendMessage(plugin.getLanguageManager().getMessage("messages.market-no-items"));
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                            return;
                        }
                        player.getInventory().removeItem(new ItemStack(mItem.material, 1));
                        plugin.getBalanceManager().addBalance(player.getUniqueId(), mItem.sellPrice);
                        plugin.getTransactionLogManager().log(player.getUniqueId(), player.getName(), "MARKET_SELL", mItem.sellPrice,
                                plugin.getBalanceManager().getBalance(player.getUniqueId()), "Market sell 1x " + mItem.material.name());
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.market-sell-success", Map.of(
                                "item", mItem.material.name(), "price", String.valueOf(mItem.sellPrice), "symbol", symbol)));
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                    }
                }
            }
        }
    }

    private void handleAuctionHouseClick(InventoryClickEvent event, Player player, AuctionHouseHolder holder) {
        int rawSlot = event.getRawSlot();
        AuctionHouseManager ahm = plugin.getAuctionHouseManager();

        if (holder.getType() == AuctionHouseHolder.Type.CONFIRM) {
            if (rawSlot == 15) {
                Bukkit.getScheduler().runTask(plugin, () -> ahm.openAuctionHouse(player, holder.getPage()));
                return;
            }
            if (rawSlot == 11) {
                AuctionHouseManager.AuctionItem ai = ahm.getListing(holder.getListingId());
                if (ai == null) {
                    player.closeInventory();
                    return;
                }
                if (!plugin.getBalanceManager().has(player.getUniqueId(), ai.price)) {
                    player.sendMessage(plugin.getLanguageManager().getMessage("messages.insufficient-funds"));
                    player.closeInventory();
                    return;
                }

                plugin.getBalanceManager().removeBalance(player.getUniqueId(), ai.price);
                plugin.getBalanceManager().addBalance(ai.seller, ai.price);
                player.getInventory().addItem(ai.item);
                ahm.removeListing(ai.id);

                String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
                plugin.getTransactionLogManager().log(player.getUniqueId(), player.getName(), "AH_BUY", -ai.price,
                        plugin.getBalanceManager().getBalance(player.getUniqueId()), "Bought AH item " + ai.item.getType().name());
                plugin.getTransactionLogManager().log(ai.seller, ai.sellerName, "AH_SOLD", ai.price,
                        plugin.getBalanceManager().getBalance(ai.seller), "AH item sold to " + player.getName());

                player.sendMessage(plugin.getLanguageManager().getMessage("messages.ah-bought", Map.of(
                        "item", ai.item.getType().name(),
                        "price", String.valueOf(ai.price),
                        "symbol", symbol
                )));

                Player sellerPlayer = Bukkit.getPlayer(ai.seller);
                if (sellerPlayer != null && sellerPlayer.isOnline()) {
                    sellerPlayer.sendMessage(plugin.getLanguageManager().getMessage("messages.chestshop-sold-notify", Map.of(
                            "item", ai.item.getType().name(),
                            "buyer", player.getName(),
                            "price", Format.money(ai.price, symbol)
                    )));
                }
                player.closeInventory();
            }
            return;
        }

        if (holder.getType() == AuctionHouseHolder.Type.MY_LISTINGS) {
            if (rawSlot == 45 && holder.getPage() > 0) {
                Bukkit.getScheduler().runTask(plugin, () -> ahm.openMyListingsMenu(player, holder.getPage() - 1));
                return;
            }
            if (rawSlot == 49) {
                Bukkit.getScheduler().runTask(plugin, () -> ahm.openAuctionHouse(player, 0));
                return;
            }
            if (rawSlot == 53) {
                Bukkit.getScheduler().runTask(plugin, () -> ahm.openMyListingsMenu(player, holder.getPage() + 1));
                return;
            }
            if (rawSlot >= 0 && rawSlot < 45) {
                List<AuctionHouseManager.AuctionItem> myItems = ahm.getListings().stream()
                        .filter(ai -> ai.seller.equals(player.getUniqueId()))
                        .toList();
                int idx = holder.getPage() * 45 + rawSlot;
                if (idx < myItems.size()) {
                    AuctionHouseManager.AuctionItem itemToCancel = myItems.get(idx);
                    ahm.removeListing(itemToCancel.id);
                    player.getInventory().addItem(itemToCancel.item);
                    player.sendMessage("§aListing cancelled and item returned to your inventory.");
                    Bukkit.getScheduler().runTask(plugin, () -> ahm.openMyListingsMenu(player, holder.getPage()));
                }
            }
            return;
        }

        if (holder.getType() == AuctionHouseHolder.Type.BROWSE) {
            if (rawSlot == 49) {
                player.closeInventory();
                return;
            }
            if (rawSlot == 47) {
                Bukkit.getScheduler().runTask(plugin, () -> ahm.openMyListingsMenu(player, 0));
                return;
            }
            if (rawSlot == 45 && holder.getPage() > 0) {
                Bukkit.getScheduler().runTask(plugin, () -> ahm.openAuctionHouse(player, holder.getPage() - 1));
                return;
            }
            if (rawSlot == 53) {
                Bukkit.getScheduler().runTask(plugin, () -> ahm.openAuctionHouse(player, holder.getPage() + 1));
                return;
            }
            if (rawSlot >= 0 && rawSlot < 45) {
                int idx = holder.getPage() * 45 + rawSlot;
                var listings = ahm.getListings();
                if (idx < listings.size()) {
                    AuctionHouseManager.AuctionItem ai = listings.get(idx);
                    boolean isOwner = player.getUniqueId().equals(ai.seller);
                    boolean isAdmin = player.hasPermission("technoeconomy.admin") || player.isOp();

                    if (isOwner || (isAdmin && event.getClick().isRightClick())) {
                        if (event.getClick().isRightClick()) {
                            ahm.removeListing(ai.id);
                            player.getInventory().addItem(ai.item);
                            player.sendMessage("§aListing cancelled and item reclaimed.");
                            Bukkit.getScheduler().runTask(plugin, () -> ahm.openAuctionHouse(player, holder.getPage()));
                            return;
                        }
                    }

                    if (isOwner) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.ah-cant-buy-own"));
                        return;
                    }

                    Bukkit.getScheduler().runTask(plugin, () -> ahm.openConfirmMenu(player, ai, holder.getPage()));
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof AuctionHouseHolder) {
            event.setCancelled(true);
        }
    }

    private void handleStockClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (slot == 47) {
            double portVal = plugin.getStockManager().getPortfolioValue(player.getUniqueId());
            double pl = plugin.getStockManager().getProfitLoss(player.getUniqueId());
            String color = pl >= 0 ? "§a+" : "§c";
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-portfolio-total", Map.of("total", String.valueOf(portVal))));
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-portfolio-pl", Map.of("color", color, "pl", String.valueOf(pl))));
            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.0f, 1.0f);
            return;
        }

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        if (slot == 51) {
            new com.techno.economy.gui.StockGui(plugin).open(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
            return;
        }

        for (int i = 0; i < com.techno.economy.gui.StockGui.COMMODITIES.length; i++) {
            if (slot == 10 + i) {
                Material mat = com.techno.economy.gui.StockGui.COMMODITIES[i];
                double price = plugin.getStockManager().getOrePrice(mat);
                boolean isRight = event.getClick().isRightClick();
                boolean isShift = event.getClick().isShiftClick();

                if (!isRight) {
                    int count = isShift ? 16 : 1;
                    double totalCost = price * count;
                    if (!plugin.getBalanceManager().has(player.getUniqueId(), totalCost)) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.insufficient-funds"));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                        return;
                    }
                    if (player.getInventory().firstEmpty() == -1) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.inventory-full"));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                        return;
                    }
                    plugin.getBalanceManager().withdraw(player.getUniqueId(), totalCost);
                    plugin.getTransactionLogManager().log(player.getUniqueId(), "STOCK_COMMODITY_BUY", totalCost, "Commodity: " + mat.name() + " Count: " + count);
                    player.getInventory().addItem(new ItemStack(mat, count));
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                    player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-commodity-bought", Map.of("amount", String.valueOf(count), "item", mat.name(), "price", Format.number(totalCost))));
                    new com.techno.economy.gui.StockGui(plugin).open(player);
                    return;
                } else {
                    int available = 0;
                    for (ItemStack stack : player.getInventory().getContents()) {
                        if (stack != null && stack.getType() == mat) {
                            available += stack.getAmount();
                        }
                    }
                    if (available <= 0) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-no-items", Map.of("item", mat.name())));
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                        return;
                    }
                    int toSell = isShift ? available : 1;
                    int remainingToRemove = toSell;
                    for (ItemStack stack : player.getInventory().getContents()) {
                        if (stack != null && stack.getType() == mat) {
                            int take = Math.min(stack.getAmount(), remainingToRemove);
                            stack.setAmount(stack.getAmount() - take);
                            remainingToRemove -= take;
                            if (remainingToRemove <= 0) break;
                        }
                    }
                    double reward = price * toSell;
                    plugin.getBalanceManager().deposit(player.getUniqueId(), reward);
                    plugin.getTransactionLogManager().log(player.getUniqueId(), "STOCK_COMMODITY_SELL", reward, "Commodity: " + mat.name() + " Count: " + toSell);
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-commodity-sold", Map.of("amount", String.valueOf(toSell), "item", mat.name(), "price", Format.number(reward))));
                    new com.techno.economy.gui.StockGui(plugin).open(player);
                    return;
                }
            }
        }

        int[] companySlots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        for (int cSlot : companySlots) {
            if (slot == cSlot) {
                var meta = item.getItemMeta();
                if (meta == null || meta.displayName() == null) return;
                String displayName = PlainTextComponentSerializer.plainText().serialize(meta.displayName()).trim();
                String companyName = displayName.replace("§e§l", "").replace("&e&l", "").replace("[", "").replace("]", "").trim();
                if (companyName.isEmpty()) return;

                boolean isRight = event.getClick().isRightClick();
                boolean isShift = event.getClick().isShiftClick();
                int amount = isShift ? 10 : 1;

                if (!isRight) {
                    boolean ok = plugin.getStockManager().buyCompanyShares(player.getUniqueId(), companyName, amount);
                    if (ok) {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-buy-success", Map.of("company", companyName, "amount", String.valueOf(amount))));
                        new com.techno.economy.gui.StockGui(plugin).open(player);
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-buy-failed"));
                    }
                } else {
                    boolean ok = plugin.getStockManager().sellCompanyShares(player.getUniqueId(), companyName, amount);
                    if (ok) {
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-sell-success", Map.of("company", companyName, "amount", String.valueOf(amount))));
                        new com.techno.economy.gui.StockGui(plugin).open(player);
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.stock-sell-failed"));
                    }
                }
                return;
            }
        }
    }

    private void handleBaltopClick(InventoryClickEvent event, Player player, String menuName) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (menuName.equals("baltop_podium")) {
            if (slot == 45) {
                player.closeInventory();
                return;
            }
            if (slot == 49) {
                new com.techno.economy.gui.BaltopGui(plugin).openAllRankings(player, 0);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            }
            return;
        }

        if (menuName.startsWith("baltop_all:")) {
            int page = 0;
            try {
                page = Integer.parseInt(menuName.substring("baltop_all:".length()));
            } catch (Exception ignored) {}

            if (slot == 45 && page > 0) {
                new com.techno.economy.gui.BaltopGui(plugin).openAllRankings(player, page - 1);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                return;
            }
            if (slot == 49) {
                new com.techno.economy.gui.BaltopGui(plugin).openPodium(player);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                return;
            }
            if (slot == 53) {
                new com.techno.economy.gui.BaltopGui(plugin).openAllRankings(player, page + 1);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            }
        }
    }

    private void handleAdminClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 46) {
            try {
                plugin.reloadAllFiles();
                player.sendMessage("§a[TechnoEconomy] All configuration and language files reloaded!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            } catch (Exception ex) {
                player.sendMessage("§c[TechnoEconomy] Failed to reload files: " + ex.getMessage());
            }
            new com.techno.economy.gui.AdminGui(plugin).open(player);
            return;
        }
        if (slot == 47) {
            player.sendMessage("§e[TechnoEconomy] Starting full backup...");
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                boolean ok = plugin.getBackupManager().backupAll();
                player.sendMessage(ok ? "§a[TechnoEconomy] Backup completed successfully!" : "§c[TechnoEconomy] Backup failed!");
            });
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }
        if (slot == 51) {
            player.closeInventory();
            player.performCommand("technoeconomyplus license");
            return;
        }

        switch (slot) {
            case 10 -> {
                player.closeInventory();
                player.performCommand("balance");
            }
            case 12 -> {
                plugin.getMenuLoader().openMenu(player, "bank");
            }
            case 13 -> {
                new com.techno.economy.gui.BaltopGui(plugin).openPodium(player);
            }
            case 14 -> {
                plugin.getMenuLoader().openMenu(player, "check");
            }
            case 15 -> {
                new com.techno.economy.gui.BountyGui(plugin).open(player, 0);
            }
            case 16 -> {
                plugin.getMarketManager().openCategoryMenu(player);
            }
            case 19 -> {
                plugin.getMenuLoader().openMenu(player, "shop");
            }
            case 20 -> {
                plugin.getAuctionHouseManager().openAuctionHouse(player, 0);
            }
            case 22 -> {
                new com.techno.economy.gui.StockGui(plugin).open(player);
            }
            case 24 -> {
                plugin.getMenuLoader().openMenu(player, "casino");
            }
            case 27 -> {
                new com.techno.economy.gui.DuelGui(plugin).open(player);
            }
            case 28 -> {
                new com.techno.economy.gui.ClanGui(plugin).openMainMenu(player);
            }
            case 30 -> {
                plugin.getMenuLoader().openMenu(player, "daily");
            }
            case 32 -> {
                plugin.getMenuLoader().openMenu(player, "jobs");
            }
            default -> player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
    }

    private void handleBountyClick(InventoryClickEvent event, Player player, String menuName) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        int page = 0;
        try {
            page = Integer.parseInt(menuName.substring("bounty_main:".length()));
        } catch (Exception ignored) {}

        if (slot == 45 && page > 0) {
            new com.techno.economy.gui.BountyGui(plugin).open(player, page - 1);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }
        if (slot == 47) {
            player.closeInventory();
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.bounty-usage"));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 1.0f);
            return;
        }
        if (slot == 51) {
            new com.techno.economy.gui.BountyGui(plugin).open(player, page);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }
        if (slot == 53) {
            new com.techno.economy.gui.BountyGui(plugin).open(player, page + 1);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item != null && item.getType() == Material.PLAYER_HEAD) {
            var meta = (SkullMeta) item.getItemMeta();
            if (meta != null && meta.getOwningPlayer() != null) {
                OfflinePlayer target = meta.getOwningPlayer();
                if (target.isOnline() && target.getPlayer() != null) {
                    player.setCompassTarget(target.getPlayer().getLocation());
                    player.sendMessage(plugin.getLanguageManager().getMessage("messages.bounty-tracking", Map.of("target", target.getName() != null ? target.getName() : "Unknown")));
                    player.playSound(player.getLocation(), Sound.ITEM_LODESTONE_COMPASS_LOCK, 1.0f, 1.0f);
                } else {
                    player.sendMessage(plugin.getLanguageManager().getMessage("messages.bounty-target-offline"));
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
                }
            }
        }
    }
}
