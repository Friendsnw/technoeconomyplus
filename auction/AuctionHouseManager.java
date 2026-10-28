package com.techno.economy.auction;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.util.Format;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class AuctionHouseManager {

    private final TechnoEconomyPlus plugin;
    private final List<AuctionItem> listings = new ArrayList<>();
    private BukkitTask expireTask;
    private YamlConfiguration menuConfig;

    public static class AuctionItem {
        public String id;
        public UUID seller;
        public String sellerName;
        public ItemStack item;
        public double price;
        public double highestBid;
        public UUID highestBidder;
        public long expiresAt;

        public AuctionItem(String id, UUID seller, String sellerName, ItemStack item, double price, double highestBid, UUID highestBidder, long expiresAt) {
            this.id = id;
            this.seller = seller;
            this.sellerName = sellerName;
            this.item = item;
            this.price = price;
            this.highestBid = highestBid;
            this.highestBidder = highestBidder;
            this.expiresAt = expiresAt;
        }
    }

    public AuctionHouseManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        listings.clear();
        File menuDir = new File(plugin.getDataFolder(), "menus");
        if (!menuDir.exists()) menuDir.mkdirs();
        File menuFile = new File(menuDir, "auctionhouse.yml");
        if (!menuFile.exists()) {
            try {
                plugin.saveResource("menus/auctionhouse.yml", false);
            } catch (Exception ignored) {}
        }
        if (menuFile.exists()) {
            menuConfig = YamlConfiguration.loadConfiguration(menuFile);
        }
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn != null) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM techno_auctions;");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            String id = rs.getString("id");
                            UUID seller = UUID.fromString(rs.getString("seller_uuid"));
                            String sellerName = rs.getString("seller_name");
                            String b64 = rs.getString("item_base64");
                            ItemStack is = deserializeItem(b64);
                            double price = rs.getDouble("price");
                            double highestBid = rs.getDouble("highest_bid");
                            String bidderStr = rs.getString("highest_bidder");
                            UUID bidder = bidderStr != null ? UUID.fromString(bidderStr) : null;
                            long expiresAt = rs.getLong("expires_at");
                            if (is != null) {
                                listings.add(new AuctionItem(id, seller, sellerName, is, price, highestBid, bidder, expiresAt));
                            }
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        startExpireTask();
    }

    private void startExpireTask() {
        if (expireTask != null) expireTask.cancel();
        expireTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            Iterator<AuctionItem> it = listings.iterator();
            while (it.hasNext()) {
                AuctionItem item = it.next();
                if (item.expiresAt <= now) {
                    it.remove();
                    deleteListingAsync(item.id);
                    Player sellerPlayer = Bukkit.getPlayer(item.seller);
                    if (sellerPlayer != null && sellerPlayer.isOnline()) {
                        sellerPlayer.getInventory().addItem(item.item);
                        sellerPlayer.sendMessage(plugin.getLanguageManager().getMessage("messages.ah-expired-returned"));
                    }
                }
            }
        }, 20L * 30, 20L * 30);
    }

    public List<AuctionItem> getListings() {
        return listings;
    }

    public void addListing(UUID seller, String sellerName, ItemStack item, double price, long durationSeconds) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        long expiresAt = System.currentTimeMillis() + (durationSeconds * 1000L);
        AuctionItem ai = new AuctionItem(id, seller, sellerName, item, price, 0.0, null, expiresAt);
        listings.add(ai);
        saveListingAsync(ai);
    }

    public AuctionItem getListing(String id) {
        for (AuctionItem item : listings) {
            if (item.id.equals(id)) return item;
        }
        return null;
    }

    public void removeListing(String id) {
        listings.removeIf(item -> item.id.equals(id));
        deleteListingAsync(id);
    }

    public void openAuctionHouse(Player player, int page) {
        openAuctionMenu(player, page);
    }

    public void listItem(Player player, ItemStack item, double price) {
        addListing(player.getUniqueId(), player.getName(), item, price, 86400L);
    }

    public void openAuctionMenu(Player player, int page) {
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        int size = 54;
        int itemsPerPage = 45;
        int totalPages = listings.isEmpty() ? 1 : (int) Math.ceil((double) listings.size() / itemsPerPage);
        page = Math.max(0, Math.min(page, totalPages - 1));

        String rawTitle;
        if (menuConfig != null && menuConfig.contains("menu_title")) {
            rawTitle = menuConfig.getString("menu_title", "&8Auction House - Page {page}").replace("{page}", String.valueOf(page + 1));
        } else {
            rawTitle = "&8Auction House - Page " + (page + 1);
        }

        Inventory inv = Bukkit.createInventory(
                new AuctionHouseHolder(AuctionHouseHolder.Type.BROWSE, page),
                size,
                LegacyComponentSerializer.legacyAmpersand().deserialize(rawTitle)
        );

        ItemStack borderPane = buildPane(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack emptyPane = buildPane(Material.GRAY_STAINED_GLASS_PANE);

        for (int i = 0; i < 45; i++) inv.setItem(i, emptyPane);
        for (int i = 45; i < 54; i++) inv.setItem(i, borderPane);

        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, listings.size());

        for (int i = start; i < end; i++) {
            AuctionItem ai = listings.get(i);
            ItemStack display = buildListingDisplay(ai, player, symbol);
            inv.setItem(i - start, display);
        }

        if (page > 0) {
            String prevName = (menuConfig != null && menuConfig.contains("items.previous_page.display_name"))
                    ? menuConfig.getString("items.previous_page.display_name")
                    : "&e< Previous Page";
            inv.setItem(45, buildItem(Material.ARROW, prevName));
        } else {
            inv.setItem(45, borderPane);
        }

        String myListingsName = (menuConfig != null && menuConfig.contains("items.my_listings.display_name"))
                ? menuConfig.getString("items.my_listings.display_name")
                : "&eMy Listings";
        List<String> myListingsLore = (menuConfig != null && menuConfig.contains("items.my_listings.lore"))
                ? menuConfig.getStringList("items.my_listings.lore")
                : List.of("&7View and manage your active listings.", "&7Click to inspect.");
        inv.setItem(47, buildItemFromList(Material.CHEST, myListingsName, myListingsLore));

        String closeName = (menuConfig != null && menuConfig.contains("items.close.display_name"))
                ? menuConfig.getString("items.close.display_name")
                : "&cClose";
        inv.setItem(49, buildItem(Material.BARRIER, closeName));

        String howName = (menuConfig != null && menuConfig.contains("items.how_to_sell.display_name"))
                ? menuConfig.getString("items.how_to_sell.display_name")
                : "&aHow to List?";
        List<String> howLore = (menuConfig != null && menuConfig.contains("items.how_to_sell.lore"))
                ? menuConfig.getStringList("items.how_to_sell.lore")
                : List.of("&7Hold an item in hand and run:", "&e/ah sell <price>");
        inv.setItem(51, buildItemFromList(Material.GOLD_INGOT, howName, howLore));

        if (page < totalPages - 1) {
            String nextName = (menuConfig != null && menuConfig.contains("items.next_page.display_name"))
                    ? menuConfig.getString("items.next_page.display_name")
                    : "&eNext Page >";
            inv.setItem(53, buildItem(Material.ARROW, nextName));
        } else {
            inv.setItem(53, borderPane);
        }

        player.openInventory(inv);
    }

    public void openMyListingsMenu(Player player, int page) {
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        List<AuctionItem> myItems = listings.stream()
                .filter(ai -> ai.seller.equals(player.getUniqueId()))
                .toList();

        int size = 54;
        int itemsPerPage = 45;
        int totalPages = myItems.isEmpty() ? 1 : (int) Math.ceil((double) myItems.size() / itemsPerPage);
        page = Math.max(0, Math.min(page, totalPages - 1));

        String myTitle = (menuConfig != null && menuConfig.contains("my_listings_menu.menu_title"))
                ? menuConfig.getString("my_listings_menu.menu_title").replace("{page}", String.valueOf(page + 1))
                : "&8My Listings";

        Inventory inv = Bukkit.createInventory(
                new AuctionHouseHolder(AuctionHouseHolder.Type.MY_LISTINGS, page),
                size,
                LegacyComponentSerializer.legacyAmpersand().deserialize(myTitle)
        );

        ItemStack borderPane = buildPane(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack emptyPane = buildPane(Material.GRAY_STAINED_GLASS_PANE);

        for (int i = 0; i < 45; i++) inv.setItem(i, emptyPane);
        for (int i = 45; i < 54; i++) inv.setItem(i, borderPane);

        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, myItems.size());

        for (int i = start; i < end; i++) {
            AuctionItem ai = myItems.get(i);
            ItemStack display = buildListingDisplay(ai, player, symbol);
            inv.setItem(i - start, display);
        }

        if (page > 0) {
            inv.setItem(45, buildItem(Material.ARROW, "&e< Previous Page"));
        }

        String backName = (menuConfig != null && menuConfig.contains("my_listings_menu.back_button.display_name"))
                ? menuConfig.getString("my_listings_menu.back_button.display_name")
                : "&eBack to All Listings";
        inv.setItem(49, buildItem(Material.BOOK, backName));

        if (page < totalPages - 1) {
            inv.setItem(53, buildItem(Material.ARROW, "&eNext Page >"));
        }

        player.openInventory(inv);
    }

    public void openConfirmMenu(Player player, AuctionItem item, int returnPage) {
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");

        String confTitle = (menuConfig != null && menuConfig.contains("confirm_menu.menu_title"))
                ? menuConfig.getString("confirm_menu.menu_title")
                : "&8Purchase Confirmation";

        Inventory inv = Bukkit.createInventory(
                new AuctionHouseHolder(AuctionHouseHolder.Type.CONFIRM, returnPage, item.id),
                27,
                LegacyComponentSerializer.legacyAmpersand().deserialize(confTitle)
        );

        ItemStack pane = buildPane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        ItemStack display = item.item.clone();
        ItemMeta dm = display.getItemMeta();
        if (dm != null) {
            if (dm.hasDisplayName()) {
                dm.displayName(dm.displayName().decoration(TextDecoration.ITALIC, false));
            }
            List<Component> lore = new ArrayList<>();
            if (dm.hasLore() && dm.lore() != null) {
                for (Component c : dm.lore()) {
                    lore.add(c.decoration(TextDecoration.ITALIC, false));
                }
            }
            lore.add(Component.empty());
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&7Seller: &f" + item.sellerName)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&7Price: &a" + symbol + Format.number(item.price))
                    .decoration(TextDecoration.ITALIC, false));
            dm.lore(lore);
            display.setItemMeta(dm);
        }
        inv.setItem(4, display);

        boolean hasEnough = plugin.getBalanceManager().has(player.getUniqueId(), item.price);

        if (hasEnough) {
            String confName = (menuConfig != null && menuConfig.contains("confirm_menu.confirm_button.display_name"))
                    ? menuConfig.getString("confirm_menu.confirm_button.display_name")
                    : "&aConfirm Purchase";
            inv.setItem(11, buildItem(Material.LIME_WOOL,
                    confName,
                    "&7Price: &a" + symbol + Format.number(item.price),
                    "",
                    "&eClick to confirm!"
            ));
        } else {
            String noName = (menuConfig != null && menuConfig.contains("confirm_menu.insufficient_funds_button.display_name"))
                    ? menuConfig.getString("confirm_menu.insufficient_funds_button.display_name")
                    : "&cInsufficient Funds";
            inv.setItem(11, buildItem(Material.RED_WOOL,
                    noName,
                    "&7Required: &a" + symbol + Format.number(item.price),
                    "&7Your Balance: &c" + symbol + Format.number(plugin.getBalanceManager().getBalance(player.getUniqueId())),
                    "",
                    "&cYou cannot afford this item!"
            ));
        }

        String cancelName = (menuConfig != null && menuConfig.contains("confirm_menu.cancel_button.display_name"))
                ? menuConfig.getString("confirm_menu.cancel_button.display_name")
                : "&cCancel";
        inv.setItem(15, buildItem(Material.RED_WOOL,
                cancelName,
                "&7Cancel and return."
        ));

        player.openInventory(inv);
    }

    private ItemStack buildListingDisplay(AuctionItem ai, Player viewer, String symbol) {
        ItemStack display = ai.item.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(display.getType());
        if (meta != null) {
            String originalName = meta.hasDisplayName()
                    ? LegacyComponentSerializer.legacySection().serialize(meta.displayName()).replace("§", "")
                    : formatMatName(display.getType());

            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&f" + originalName)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            if (meta.hasLore() && meta.lore() != null) {
                for (Component c : meta.lore()) {
                    lore.add(c.decoration(TextDecoration.ITALIC, false));
                }
            }
            lore.add(Component.empty());
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&7Seller: &f" + ai.sellerName)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&7Price: &a" + symbol + Format.number(ai.price))
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&7Time Left: &e" + formatTimeRemaining(ai.expiresAt))
                    .decoration(TextDecoration.ITALIC, false));

            boolean isOwner = viewer.getUniqueId().equals(ai.seller);
            boolean isAdmin = viewer.hasPermission("technoeconomy.admin") || viewer.isOp();

            if (isOwner || isAdmin) {
                lore.add(Component.empty());
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&cRight-Click to cancel and reclaim item!")
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.empty());
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&eLeft-Click to buy!")
                        .decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE);
            for (var f : ItemFlag.values()) {
                try { meta.addItemFlags(f); } catch (Exception ignored) {}
            }
            try { meta.setAttributeModifiers(null); } catch (Exception ignored) {}
            display.setItemMeta(meta);
        }
        return display;
    }

    private ItemStack buildItemFromList(Material mat, String name, List<String> loreLines) {
        return buildItem(mat, name, loreLines.toArray(new String[0]));
    }

    private String formatMatName(Material mat) {
        String name = mat.name().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String w : name.split(" ")) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }

    private String formatTimeRemaining(long expiresAt) {
        long diff = expiresAt - System.currentTimeMillis();
        if (diff <= 0) return "0s";
        long hours = diff / 3600000L;
        long minutes = (diff % 3600000L) / 60000L;
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m";
    }

    private ItemStack buildPane(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name)
                .decoration(TextDecoration.ITALIC, false));
        if (loreLines.length > 0) {
            List<Component> lore = new ArrayList<>();
            for (String l : loreLines) {
                if (l.isEmpty()) lore.add(Component.empty());
                else lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(l)
                        .decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
        }
        for (var f : ItemFlag.values()) {
            try { meta.addItemFlags(f); } catch (Exception ignored) {}
        }
        item.setItemMeta(meta);
        return item;
    }

    public String serializeItem(ItemStack item) {
        try {
            return Base64.getEncoder().encodeToString(item.serializeAsBytes());
        } catch (Exception e) {
            return "";
        }
    }

    public ItemStack deserializeItem(String b64) {
        if (b64 == null || b64.isEmpty()) return null;
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(b64));
        } catch (Exception e) {
            return null;
        }
    }

    private void saveListingAsync(AuctionItem ai) {
        if (!plugin.isEnabled()) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                if (conn == null) return;
                boolean isRemote = plugin.getDatabaseManager().isRemote();
                String sql = isRemote
                        ? "INSERT INTO techno_auctions (id, seller_uuid, seller_name, item_base64, price, highest_bid, highest_bidder, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE price=VALUES(price);"
                        : "INSERT OR REPLACE INTO techno_auctions (id, seller_uuid, seller_name, item_base64, price, highest_bid, highest_bidder, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, ai.id);
                    ps.setString(2, ai.seller.toString());
                    ps.setString(3, ai.sellerName);
                    ps.setString(4, serializeItem(ai.item));
                    ps.setDouble(5, ai.price);
                    ps.setDouble(6, ai.highestBid);
                    ps.setString(7, ai.highestBidder != null ? ai.highestBidder.toString() : null);
                    ps.setLong(8, ai.expiresAt);
                    ps.executeUpdate();
                }
            } catch (Exception ignored) {}
        });
    }

    private void deleteListingAsync(String id) {
        if (!plugin.isEnabled()) return;
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                if (conn == null) return;
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM techno_auctions WHERE id = ?")) {
                    ps.setString(1, id);
                    ps.executeUpdate();
                }
            } catch (Exception ignored) {}
        });
    }

    public void save() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return;
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM techno_auctions;")) {
                del.executeUpdate();
            }
            boolean isRemote = plugin.getDatabaseManager().isRemote();
            String sql = isRemote
                    ? "INSERT INTO techno_auctions (id, seller_uuid, seller_name, item_base64, price, highest_bid, highest_bidder, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?);"
                    : "INSERT OR REPLACE INTO techno_auctions (id, seller_uuid, seller_name, item_base64, price, highest_bid, highest_bidder, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (AuctionItem ai : listings) {
                    ps.setString(1, ai.id);
                    ps.setString(2, ai.seller.toString());
                    ps.setString(3, ai.sellerName);
                    ps.setString(4, serializeItem(ai.item));
                    ps.setDouble(5, ai.price);
                    ps.setDouble(6, ai.highestBid);
                    ps.setString(7, ai.highestBidder != null ? ai.highestBidder.toString() : null);
                    ps.setLong(8, ai.expiresAt);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (Exception ignored) {}
    }

    public void saveAsync() {
        if (!plugin.isEnabled()) {
            save();
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    public void shutdown() {
        if (expireTask != null) expireTask.cancel();
        save();
    }
}
