package com.techno.economy.shop;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.util.Format;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChestShopManager {

    private final TechnoEconomyPlus plugin;
    private final ConcurrentHashMap<String, ChestShopData> shops = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<ArmorStand>> holograms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> rotationIndex = new ConcurrentHashMap<>();
    private BukkitTask rotationTask;
    private YamlConfiguration menuConfig;

    public static class ShopProduct {
        public String id;
        public ItemStack item;
        public double price;
        public int stock;

        public ShopProduct(String id, ItemStack item, double price, int stock) {
            this.id = id;
            this.item = item;
            this.price = price;
            this.stock = stock;
        }
    }

    public static class ChestShopData {
        public UUID owner;
        public String marketName;
        public String world;
        public int x, y, z;
        public List<ShopProduct> products = new ArrayList<>();

        public ChestShopData(UUID owner, String marketName, String world, int x, int y, int z) {
            this.owner = owner;
            this.marketName = marketName;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public ShopProduct getProduct(String id) {
            for (ShopProduct p : products) {
                if (p.id.equals(id)) return p;
            }
            return null;
        }

        public int getTotalStock() {
            int total = 0;
            for (ShopProduct p : products) total += p.stock;
            return total;
        }
    }

    public ChestShopManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        shops.clear();
        removeAllHolograms();
        File menuDir = new File(plugin.getDataFolder(), "menus");
        if (!menuDir.exists()) menuDir.mkdirs();
        File menuFile = new File(menuDir, "chestshop.yml");
        if (!menuFile.exists()) {
            try {
                plugin.saveResource("menus/chestshop.yml", false);
            } catch (Exception ignored) {}
        }
        if (menuFile.exists()) {
            menuConfig = YamlConfiguration.loadConfiguration(menuFile);
        }
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn != null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT loc, owner_uuid, market_name, world, x, y, z FROM techno_chestshops;");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            String locKey = rs.getString("loc");
                            UUID owner = UUID.fromString(rs.getString("owner_uuid"));
                            String marketName = rs.getString("market_name");
                            String world = rs.getString("world");
                            int x = rs.getInt("x");
                            int y = rs.getInt("y");
                            int z = rs.getInt("z");
                            ChestShopData data = new ChestShopData(owner, marketName, world, x, y, z);
                            shops.put(locKey, data);
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}

                for (String locKey : shops.keySet()) {
                    ChestShopData data = shops.get(locKey);
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT product_id, item_base64, price, stock FROM techno_chestshop_products WHERE loc = ?;")) {
                        ps.setString(1, locKey);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                try {
                                    String pid = rs.getString("product_id");
                                    String b64 = rs.getString("item_base64");
                                    double price = rs.getDouble("price");
                                    int stock = rs.getInt("stock");
                                    ItemStack item = deserializeItem(b64);
                                    if (item != null) {
                                        data.products.add(new ShopProduct(pid, item, price, stock));
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            spawnAllHolograms();
            startRotationTask();
        }, 40L);
    }

    private void startRotationTask() {
        if (rotationTask != null) {
            rotationTask.cancel();
        }
        rotationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<String, ChestShopData> entry : shops.entrySet()) {
                String key = entry.getKey();
                ChestShopData shop = entry.getValue();
                if (shop.products.isEmpty()) continue;
                int idx = rotationIndex.getOrDefault(key, 0);
                idx = (idx + 1) % shop.products.size();
                rotationIndex.put(key, idx);
                updateHologramDisplay(key, shop, idx);
            }
        }, 40L, 40L);
    }

    public void spawnAllHolograms() {
        for (Map.Entry<String, ChestShopData> entry : shops.entrySet()) {
            Location loc = stringToLoc(entry.getKey());
            if (loc != null && loc.getWorld() != null) {
                spawnHolograms(entry.getKey(), loc, entry.getValue());
            }
        }
    }

    public void spawnHolograms(String key, Location loc, ChestShopData shop) {
        removeHolograms(key);
        if (loc.getWorld() == null) return;
        purgeGhostHolograms(loc);

        String ownerName = getOwnerName(shop.owner);
        List<ArmorStand> stands = new ArrayList<>();

        Location nameLoc = loc.clone().add(0.5, 1.6, 0.5);
        ArmorStand nameStand = spawnStand(nameLoc, "§6§l" + shop.marketName + " §8| §7" + ownerName);
        if (nameStand != null) stands.add(nameStand);

        if (!shop.products.isEmpty()) {
            String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
            ShopProduct first = shop.products.get(0);
            String itemName = getItemDisplayName(first.item);
            Location priceLoc = loc.clone().add(0.5, 1.2, 0.5);
            ArmorStand priceStand = spawnStand(priceLoc, "§a" + symbol + Format.number(first.price) + " §8| §f" + itemName);
            if (priceStand != null) stands.add(priceStand);
        }

        holograms.put(key, stands);
    }

    private void updateHologramDisplay(String key, ChestShopData shop, int productIdx) {
        List<ArmorStand> stands = holograms.get(key);
        if (stands == null || stands.size() < 2) return;
        if (shop.products.isEmpty()) return;

        ShopProduct product = shop.products.get(productIdx);
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        String itemName = getItemDisplayName(product.item);
        String stockText = product.stock > 0 ? "§f" + product.stock + " adet" : "§cTukendi";

        ArmorStand priceStand = stands.get(1);
        if (priceStand != null && priceStand.isValid()) {
            priceStand.customName(LegacyComponentSerializer.legacySection().deserialize(
                    "§a" + symbol + Format.number(product.price) + " §8| §f" + itemName + " §8[" + stockText + "§8]"
            ));
        }
    }

    private ArmorStand spawnStand(Location loc, String name) {
        try {
            return loc.getWorld().spawn(loc, ArmorStand.class, as -> {
                as.setInvisible(true);
                as.setMarker(true);
                as.setSmall(true);
                as.setCustomNameVisible(true);
                as.setGravity(false);
                as.setInvulnerable(true);
                as.addScoreboardTag("techno_chestshop");
                as.customName(LegacyComponentSerializer.legacySection().deserialize(name));
            });
        } catch (Exception e) {
            return null;
        }
    }

    private void purgeGhostHolograms(Location loc) {
        if (loc.getWorld() == null) return;
        for (Entity e : loc.getWorld().getNearbyEntities(loc.clone().add(0.5, 1.4, 0.5), 1.5, 1.5, 1.5)) {
            if (e instanceof ArmorStand as && as.getScoreboardTags().contains("techno_chestshop")) {
                as.remove();
            }
        }
    }

    public void removeHolograms(String key) {
        List<ArmorStand> stands = holograms.remove(key);
        if (stands != null) {
            for (ArmorStand as : stands) {
                if (as != null && as.isValid()) as.remove();
            }
        }
    }

    public void removeAllHolograms() {
        for (List<ArmorStand> stands : holograms.values()) {
            if (stands != null) {
                for (ArmorStand as : stands) {
                    if (as != null && as.isValid()) as.remove();
                }
            }
        }
        holograms.clear();
        if (rotationTask != null) {
            rotationTask.cancel();
            rotationTask = null;
        }
    }

    public void createShop(Location loc, UUID owner, String marketName) {
        String key = locToString(loc);
        ChestShopData data = new ChestShopData(owner, marketName, loc.getWorld().getName(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        shops.put(key, data);
        spawnHolograms(key, loc, data);
        saveShopAsync(key, data);
    }

    public void addProduct(String locKey, ShopProduct product) {
        ChestShopData shop = shops.get(locKey);
        if (shop == null) return;
        shop.products.add(product);
        Location loc = stringToLoc(locKey);
        if (loc != null) spawnHolograms(locKey, loc, shop);
        saveProductAsync(locKey, product);
    }

    public void updateProduct(String locKey, ShopProduct product) {
        saveProductAsync(locKey, product);
        ChestShopData shop = shops.get(locKey);
        if (shop == null) return;
        Location loc = stringToLoc(locKey);
        if (loc != null) spawnHolograms(locKey, loc, shop);
    }

    public void removeProduct(String locKey, String productId) {
        ChestShopData shop = shops.get(locKey);
        if (shop == null) return;
        shop.products.removeIf(p -> p.id.equals(productId));
        deleteProductAsync(locKey, productId);
        Location loc = stringToLoc(locKey);
        if (loc != null) spawnHolograms(locKey, loc, shop);
    }

    public void removeShop(Location loc) {
        String key = locToString(loc);
        shops.remove(key);
        rotationIndex.remove(key);
        removeHolograms(key);
        purgeGhostHolograms(loc);
        deleteShopAsync(key);
    }

    public boolean isShop(Location loc) {
        return shops.containsKey(locToString(loc));
    }

    public ChestShopData getShop(Location loc) {
        return shops.get(locToString(loc));
    }

    public ChestShopData getShopByKey(String key) {
        return shops.get(key);
    }

    public String locToString(Location loc) {
        if (loc == null || loc.getWorld() == null) return "";
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    public Location stringToLoc(String str) {
        String[] p = str.split(",");
        if (p.length < 4) return null;
        World w = Bukkit.getWorld(p[0]);
        if (w == null) return null;
        try {
            return new Location(w, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
        } catch (Exception e) {
            return null;
        }
    }

    private String getOwnerName(UUID uuid) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        return op.getName() != null ? op.getName() : "Owner";
    }

    public String getItemDisplayName(ItemStack item) {
        if (item == null) return "Unknown";
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return LegacyComponentSerializer.legacySection().serialize(item.getItemMeta().displayName()).replace("§", "");
        }
        String name = item.getType().name().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(" ");
            }
        }
        return sb.toString().trim();
    }

    public void openAdminMenu(Player player, String locKey, ChestShopData shop) {
        String title = (menuConfig != null && menuConfig.contains("admin_menu.menu_title"))
                ? menuConfig.getString("admin_menu.menu_title").replace("{shop_name}", shop.marketName)
                : "Shop Management: " + shop.marketName;
        Inventory inv = Bukkit.createInventory(
                new ChestShopHolder(ChestShopHolder.Type.ADMIN, locKey),
                54,
                LegacyComponentSerializer.legacyAmpersand().deserialize("&8" + title)
        );

        ItemStack bg = buildPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);

        String ownerName = getOwnerName(shop.owner);
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");

        String infoName = (menuConfig != null && menuConfig.contains("admin_menu.info_button.display_name"))
                ? menuConfig.getString("admin_menu.info_button.display_name")
                : "&6Shop Info";
        ItemStack info = buildItem(Material.BOOK,
                infoName,
                "&7Owner: &f" + ownerName,
                "&7Location: &f" + shop.x + ", " + shop.y + ", " + shop.z,
                "&7Total Stock: &a" + shop.getTotalStock(),
                "&7Total Products: &b" + shop.products.size()
        );
        inv.setItem(4, info);

        String addName = (menuConfig != null && menuConfig.contains("admin_menu.add_product_button.display_name"))
                ? menuConfig.getString("admin_menu.add_product_button.display_name")
                : "&aAdd Product";
        ItemStack addProduct = buildItem(Material.LIME_DYE,
                addName,
                "&7Add a new product",
                "&7to this chest shop.",
                "",
                "&eClick!"
        );
        inv.setItem(19, addProduct);

        String delName = (menuConfig != null && menuConfig.contains("admin_menu.delete_shop_button.display_name"))
                ? menuConfig.getString("admin_menu.delete_shop_button.display_name")
                : "&cDelete Shop";
        ItemStack deleteShop = buildItem(Material.TNT,
                delName,
                "&7Permanently removes this shop.",
                "",
                "&cClick to delete!"
        );
        inv.setItem(25, deleteShop);

        if (!shop.products.isEmpty()) {
            int slot = 27;
            for (ShopProduct p : shop.products) {
                if (slot >= 45) break;
                ItemStack display = buildProductItem(p, ownerName, symbol, true);
                inv.setItem(slot, display);
                slot++;
            }
        }

        String closeName = (menuConfig != null && menuConfig.contains("admin_menu.close_button.display_name"))
                ? menuConfig.getString("admin_menu.close_button.display_name")
                : "&cClose";
        ItemStack close = buildItem(Material.BARRIER, closeName);
        inv.setItem(49, close);

        player.openInventory(inv);
    }

    public void openShopMenu(Player player, String locKey, ChestShopData shop, int scrollIndex) {
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        String ownerName = getOwnerName(shop.owner);

        String title = (menuConfig != null && menuConfig.contains("shop_menu.menu_title"))
                ? menuConfig.getString("shop_menu.menu_title").replace("{shop_name}", shop.marketName)
                : "Shop: " + shop.marketName;
        Inventory inv = Bukkit.createInventory(
                new ChestShopHolder(ChestShopHolder.Type.BROWSE, locKey, scrollIndex),
                27,
                LegacyComponentSerializer.legacyAmpersand().deserialize("&8" + title)
        );

        ItemStack bg = buildPane(Material.BLUE_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, bg);

        List<ShopProduct> products = shop.products;
        int maxVisible = 7;
        int maxScroll = Math.max(0, products.size() - maxVisible);
        scrollIndex = Math.max(0, Math.min(scrollIndex, maxScroll));

        for (int i = 0; i < maxVisible; i++) {
            int idx = scrollIndex + i;
            int slot = 10 + i;
            if (idx < products.size()) {
                ShopProduct p = products.get(idx);
                inv.setItem(slot, buildProductItem(p, ownerName, symbol, false));
            } else {
                inv.setItem(slot, buildPane(Material.LIGHT_BLUE_STAINED_GLASS_PANE));
            }
        }

        if (scrollIndex > 0) {
            inv.setItem(9, buildItem(Material.PLAYER_HEAD, "&e< Previous"));
        } else {
            inv.setItem(9, buildPane(Material.BLUE_STAINED_GLASS_PANE));
        }

        if (scrollIndex + maxVisible < products.size()) {
            inv.setItem(17, buildItem(Material.PLAYER_HEAD, "&eNext >"));
        } else {
            inv.setItem(17, buildPane(Material.BLUE_STAINED_GLASS_PANE));
        }

        inv.setItem(22, buildItem(Material.BARRIER, "&cClose"));

        player.openInventory(inv);
    }

    public void openConfirmMenu(Player player, String locKey, ChestShopData shop, ShopProduct product) {
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        String ownerName = getOwnerName(shop.owner);

        String title = (menuConfig != null && menuConfig.contains("confirm_menu.menu_title"))
                ? menuConfig.getString("confirm_menu.menu_title")
                : "Purchase Confirmation";
        Inventory inv = Bukkit.createInventory(
                new ChestShopHolder(ChestShopHolder.Type.CONFIRM, locKey, product.id),
                27,
                LegacyComponentSerializer.legacyAmpersand().deserialize("&8" + title)
        );

        ItemStack pane = buildPane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        ItemStack display = buildProductItem(product, ownerName, symbol, false);
        inv.setItem(4, display);

        boolean hasEnough = plugin.getBalanceManager().has(player.getUniqueId(), product.price);

        if (product.stock > 0 && hasEnough) {
            ItemStack confirm = buildItem(Material.LIME_WOOL,
                    "&aConfirm Purchase",
                    "&7Price: &a" + symbol + Format.number(product.price),
                    "&7Stock Left: &f" + product.stock,
                    "",
                    "&eClick to buy!"
            );
            inv.setItem(11, confirm);
        } else if (!hasEnough) {
            ItemStack noMoney = buildItem(Material.RED_WOOL,
                    "&cInsufficient Funds",
                    "&7Required: &a" + symbol + Format.number(product.price),
                    "&7Your Balance: &c" + symbol + Format.number(plugin.getBalanceManager().getBalance(player.getUniqueId())),
                    "",
                    "&cYou do not have enough funds!"
            );
            inv.setItem(11, noMoney);
        } else {
            ItemStack noStock = buildItem(Material.RED_WOOL,
                    "&cOut of Stock",
                    "&7This product is out of stock."
            );
            inv.setItem(11, noStock);
        }

        ItemStack cancel = buildItem(Material.RED_WOOL,
                "&cCancel",
                "&7Cancel and return."
        );
        inv.setItem(15, cancel);

        player.openInventory(inv);
    }

    public void openAddProductMenu(Player player, String locKey, ChestShopData shop) {
        String title = (menuConfig != null && menuConfig.contains("add_product_menu.menu_title"))
                ? menuConfig.getString("add_product_menu.menu_title")
                : "Add Product";
        Inventory inv = Bukkit.createInventory(
                new ChestShopHolder(ChestShopHolder.Type.ADD_PRODUCT, locKey),
                27,
                LegacyComponentSerializer.legacyAmpersand().deserialize("&8" + title)
        );

        ItemStack pane = buildPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        inv.setItem(13, null);

        String infoName = (menuConfig != null && menuConfig.contains("add_product_menu.info_button.display_name"))
                ? menuConfig.getString("add_product_menu.info_button.display_name")
                : "&ePlace Item in Center Slot";
        ItemStack info = buildItem(Material.HOPPER,
                infoName,
                "&7Put the item you want to sell",
                "&7into the empty center slot.",
                "&7Placed quantity will be initial stock."
        );
        inv.setItem(4, info);

        String confName = (menuConfig != null && menuConfig.contains("add_product_menu.confirm_button.display_name"))
                ? menuConfig.getString("add_product_menu.confirm_button.display_name")
                : "&aSet Price & Save";
        ItemStack confirmBtn = buildItem(Material.LIME_CONCRETE,
                confName,
                "&7Click here after placing",
                "&7the item to enter price."
        );
        inv.setItem(15, confirmBtn);

        String cancelName = (menuConfig != null && menuConfig.contains("add_product_menu.cancel_button.display_name"))
                ? menuConfig.getString("add_product_menu.cancel_button.display_name")
                : "&cCancel";
        ItemStack cancelBtn = buildItem(Material.BARRIER, cancelName);
        inv.setItem(11, cancelBtn);

        player.openInventory(inv);
    }

    public ItemStack buildProductItem(ShopProduct p, String ownerName, String symbol, boolean adminMode) {
        boolean outOfStock = p.stock <= 0;
        ItemStack display = outOfStock ? new ItemStack(Material.BEDROCK) : p.item.clone();
        display.setAmount(1);
        ItemMeta meta = display.getItemMeta();
        if (meta == null) meta = Bukkit.getItemFactory().getItemMeta(display.getType());
        if (meta == null) return display;

        String itemName = getItemDisplayName(p.item);
        if (outOfStock) {
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&c" + itemName)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&f" + itemName)
                    .decoration(TextDecoration.ITALIC, false));
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&7Price: &a" + symbol + Format.number(p.price))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&7Stock: &f" + p.stock)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&7Seller: &f" + ownerName)
                .decoration(TextDecoration.ITALIC, false));

        if (outOfStock) {
            lore.add(Component.empty());
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&cOut of stock!")
                    .decoration(TextDecoration.ITALIC, false));
        }

        if (adminMode) {
            lore.add(Component.empty());
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&6Right-Click: Remove Product")
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&eMiddle-Click: Change Price")
                    .decoration(TextDecoration.ITALIC, false));
        } else if (!outOfStock) {
            lore.add(Component.empty());
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&eClick: Purchase")
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE);
        for (var f : ItemFlag.values()) {
            try { meta.addItemFlags(f); } catch (Exception ignored) {}
        }
        try { meta.setAttributeModifiers(null); } catch (Exception ignored) {}

        display.setItemMeta(meta);
        return display;
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
                if (l.isEmpty()) {
                    lore.add(Component.empty());
                } else {
                    lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(l)
                            .decoration(TextDecoration.ITALIC, false));
                }
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
            byte[] bytes = item.serializeAsBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            return item.getType().name() + ":1";
        }
    }

    public ItemStack deserializeItem(String data) {
        if (data == null || data.isEmpty()) return null;
        try {
            if (data.contains(":") && !data.contains("=")) {
                String[] parts = data.split(":");
                return new ItemStack(Material.valueOf(parts[0]), Integer.parseInt(parts[1]));
            }
            byte[] bytes = Base64.getDecoder().decode(data);
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    public void saveShopSync(String locKey, ChestShopData data) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return;
            boolean remote = plugin.getDatabaseManager().isRemote();
            String sql = remote
                    ? "INSERT INTO techno_chestshops (loc, owner_uuid, market_name, world, x, y, z) VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE market_name=VALUES(market_name)"
                    : "INSERT OR REPLACE INTO techno_chestshops (loc, owner_uuid, market_name, world, x, y, z) VALUES (?,?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, locKey);
                ps.setString(2, data.owner.toString());
                ps.setString(3, data.marketName);
                ps.setString(4, data.world);
                ps.setInt(5, data.x);
                ps.setInt(6, data.y);
                ps.setInt(7, data.z);
                ps.executeUpdate();
            }
        } catch (Exception ignored) {}
    }

    public void saveProductSync(String locKey, ShopProduct product) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return;
            boolean remote = plugin.getDatabaseManager().isRemote();
            String sql = remote
                    ? "INSERT INTO techno_chestshop_products (loc, product_id, item_base64, price, stock) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE price=VALUES(price), stock=VALUES(stock)"
                    : "INSERT OR REPLACE INTO techno_chestshop_products (loc, product_id, item_base64, price, stock) VALUES (?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, locKey);
                ps.setString(2, product.id);
                ps.setString(3, serializeItem(product.item));
                ps.setDouble(4, product.price);
                ps.setInt(5, product.stock);
                ps.executeUpdate();
            }
        } catch (Exception ignored) {}
    }

    private void saveShopAsync(String locKey, ChestShopData data) {
        if (!plugin.isEnabled()) {
            saveShopSync(locKey, data);
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> saveShopSync(locKey, data));
    }

    public void saveProductAsync(String locKey, ShopProduct product) {
        if (!plugin.isEnabled()) {
            saveProductSync(locKey, product);
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> saveProductSync(locKey, product));
    }

    public void deleteProductSync(String locKey, String productId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return;
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM techno_chestshop_products WHERE loc = ? AND product_id = ?")) {
                ps.setString(1, locKey);
                ps.setString(2, productId);
                ps.executeUpdate();
            }
        } catch (Exception ignored) {}
    }

    private void deleteProductAsync(String locKey, String productId) {
        if (!plugin.isEnabled()) {
            deleteProductSync(locKey, productId);
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> deleteProductSync(locKey, productId));
    }

    public void deleteShopSync(String locKey) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return;
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM techno_chestshops WHERE loc = ?")) {
                ps.setString(1, locKey);
                ps.executeUpdate();
            } catch (Exception ignored) {}
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM techno_chestshop_products WHERE loc = ?")) {
                ps.setString(1, locKey);
                ps.executeUpdate();
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    private void deleteShopAsync(String locKey) {
        if (!plugin.isEnabled()) {
            deleteShopSync(locKey);
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> deleteShopSync(locKey));
    }

    public void saveAsync() {
        if (!plugin.isEnabled()) {
            save();
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    public void save() {
        for (Map.Entry<String, ChestShopData> entry : shops.entrySet()) {
            ChestShopData data = entry.getValue();
            saveShopSync(entry.getKey(), data);
            for (ShopProduct p : data.products) {
                saveProductSync(entry.getKey(), p);
            }
        }
    }

    public void shutdown() {
        if (rotationTask != null) {
            rotationTask.cancel();
            rotationTask = null;
        }
        removeAllHolograms();
        save();
    }
}
