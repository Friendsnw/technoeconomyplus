package com.techno.economy.shop;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.menu.MenuHolder;
import com.techno.economy.util.Format;
import com.techno.economy.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MarketManager {

    private final TechnoEconomyPlus plugin;
    private final List<MarketItem> items = new ArrayList<>();
    private final List<String> categories = new ArrayList<>();
    private File marketFile;
    private YamlConfiguration marketConfig;

    public static class MarketItem {
        public String id;
        public Material material;
        public String category;
        public double buyPrice;
        public double sellPrice;

        public MarketItem(String id, Material material, String category, double buyPrice, double sellPrice) {
            this.id = id;
            this.material = material;
            this.category = category;
            this.buyPrice = buyPrice;
            this.sellPrice = sellPrice;
        }
    }

    public MarketManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        items.clear();
        categories.clear();
        File dir = new File(plugin.getDataFolder(), "settings");
        if (!dir.exists()) dir.mkdirs();

        marketFile = new File(dir, "market.yml");
        if (!marketFile.exists()) {
            try (java.io.InputStream in = plugin.getResource("settings/market.yml")) {
                if (in != null) {
                    java.nio.file.Files.copy(in, marketFile.toPath());
                } else {
                    marketFile.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create settings/market.yml: " + e.getMessage());
            }
        }
        marketConfig = YamlConfiguration.loadConfiguration(marketFile);
        for (String key : marketConfig.getKeys(false)) {
            try {
                Material mat = Material.valueOf(marketConfig.getString(key + ".material"));
                String cat = marketConfig.getString(key + ".category", "misc");
                double buy = marketConfig.getDouble(key + ".buy", 10.0);
                double sell = marketConfig.getDouble(key + ".sell", 5.0);
                items.add(new MarketItem(key, mat, cat, buy, sell));
                if (!categories.contains(cat)) categories.add(cat);
            } catch (Exception ignored) {}
        }

        if (items.isEmpty()) loadDefaults();
    }

    private void loadDefaults() {
        addDefault("diamond", Material.DIAMOND, "minerals", 200, 100);
        addDefault("emerald", Material.EMERALD, "minerals", 100, 50);
        addDefault("gold_ingot", Material.GOLD_INGOT, "minerals", 50, 25);
        addDefault("iron_ingot", Material.IRON_INGOT, "minerals", 20, 10);
        addDefault("coal", Material.COAL, "minerals", 6, 3);
        addDefault("lapis_lazuli", Material.LAPIS_LAZULI, "minerals", 8, 4);
        addDefault("redstone", Material.REDSTONE, "minerals", 5, 2.5);
        addDefault("quartz", Material.QUARTZ, "minerals", 4, 2);
        addDefault("netherite_ingot", Material.NETHERITE_INGOT, "minerals", 2000, 1200);
        addDefault("netherite_scrap", Material.NETHERITE_SCRAP, "minerals", 500, 300);
        addDefault("copper_ingot", Material.COPPER_INGOT, "minerals", 8, 4);
        addDefault("amethyst_shard", Material.AMETHYST_SHARD, "minerals", 12, 6);
        addDefault("raw_iron", Material.RAW_IRON, "minerals", 10, 5);
        addDefault("raw_gold", Material.RAW_GOLD, "minerals", 25, 12);
        addDefault("raw_copper", Material.RAW_COPPER, "minerals", 4, 2);
        addDefault("diamond_ore", Material.DIAMOND_ORE, "minerals", 350, 180);
        addDefault("emerald_ore", Material.EMERALD_ORE, "minerals", 180, 90);
        addDefault("ancient_debris", Material.ANCIENT_DEBRIS, "minerals", 1500, 900);

        addDefault("oak_log", Material.OAK_LOG, "building", 4, 2);
        addDefault("spruce_log", Material.SPRUCE_LOG, "building", 4, 2);
        addDefault("birch_log", Material.BIRCH_LOG, "building", 4, 2);
        addDefault("jungle_log", Material.JUNGLE_LOG, "building", 4, 2);
        addDefault("acacia_log", Material.ACACIA_LOG, "building", 4, 2);
        addDefault("dark_oak_log", Material.DARK_OAK_LOG, "building", 4, 2);
        addDefault("mangrove_log", Material.MANGROVE_LOG, "building", 5, 2.5);
        addDefault("cherry_log", Material.CHERRY_LOG, "building", 5, 2.5);
        addDefault("stone", Material.STONE, "building", 1, 0.5);
        addDefault("cobblestone", Material.COBBLESTONE, "building", 0.5, 0.25);
        addDefault("granite", Material.GRANITE, "building", 1.5, 0.75);
        addDefault("diorite", Material.DIORITE, "building", 1.5, 0.75);
        addDefault("andesite", Material.ANDESITE, "building", 1.5, 0.75);
        addDefault("deepslate", Material.DEEPSLATE, "building", 2, 1);
        addDefault("tuff", Material.TUFF, "building", 1.5, 0.75);
        addDefault("calcite", Material.CALCITE, "building", 1.5, 0.75);
        addDefault("basalt", Material.BASALT, "building", 2, 1);
        addDefault("blackstone", Material.BLACKSTONE, "building", 2, 1);
        addDefault("end_stone", Material.END_STONE, "building", 5, 2.5);
        addDefault("obsidian", Material.OBSIDIAN, "building", 15, 8);
        addDefault("crying_obsidian", Material.CRYING_OBSIDIAN, "building", 20, 10);
        addDefault("glass", Material.GLASS, "building", 2, 1);
        addDefault("sand", Material.SAND, "building", 1, 0.5);
        addDefault("gravel", Material.GRAVEL, "building", 1, 0.5);
        addDefault("bricks", Material.BRICKS, "building", 3, 1.5);
        addDefault("nether_brick", Material.NETHER_BRICK, "building", 3, 1.5);
        addDefault("quartz_block", Material.QUARTZ_BLOCK, "building", 10, 5);
        addDefault("sandstone", Material.SANDSTONE, "building", 2, 1);
        addDefault("red_sandstone", Material.RED_SANDSTONE, "building", 2, 1);
        addDefault("prismarine", Material.PRISMARINE, "building", 8, 4);
        addDefault("terracotta", Material.TERRACOTTA, "building", 2, 1);
        addDefault("purpur_block", Material.PURPUR_BLOCK, "building", 12, 6);

        addDefault("wheat", Material.WHEAT, "farming", 3, 1.5);
        addDefault("carrot", Material.CARROT, "farming", 3, 1.5);
        addDefault("potato", Material.POTATO, "farming", 2, 1);
        addDefault("beetroot", Material.BEETROOT, "farming", 3, 1.5);
        addDefault("pumpkin", Material.PUMPKIN, "farming", 5, 2.5);
        addDefault("melon_slice", Material.MELON_SLICE, "farming", 2, 1);
        addDefault("sugar_cane", Material.SUGAR_CANE, "farming", 2, 1);
        addDefault("bamboo", Material.BAMBOO, "farming", 1, 0.5);
        addDefault("cactus", Material.CACTUS, "farming", 1.5, 0.75);
        addDefault("sweet_berries", Material.SWEET_BERRIES, "farming", 4, 2);
        addDefault("glow_berries", Material.GLOW_BERRIES, "farming", 6, 3);
        addDefault("bread", Material.BREAD, "farming", 8, 4);
        addDefault("cooked_beef", Material.COOKED_BEEF, "farming", 10, 5);
        addDefault("cooked_chicken", Material.COOKED_CHICKEN, "farming", 8, 4);
        addDefault("cooked_pork", Material.COOKED_PORKCHOP, "farming", 9, 4.5);
        addDefault("cooked_mutton", Material.COOKED_MUTTON, "farming", 8, 4);
        addDefault("cooked_rabbit", Material.COOKED_RABBIT, "farming", 9, 4.5);
        addDefault("cooked_salmon", Material.COOKED_SALMON, "farming", 7, 3.5);
        addDefault("cooked_cod", Material.COOKED_COD, "farming", 6, 3);
        addDefault("golden_apple", Material.GOLDEN_APPLE, "farming", 50, 30);
        addDefault("golden_carrot", Material.GOLDEN_CARROT, "farming", 20, 10);
        addDefault("honey_bottle", Material.HONEY_BOTTLE, "farming", 15, 7.5);
        addDefault("mushroom_stew", Material.MUSHROOM_STEW, "farming", 12, 6);
        addDefault("cake", Material.CAKE, "farming", 25, 12.5);
        addDefault("cookie", Material.COOKIE, "farming", 3, 1.5);
        addDefault("pumpkin_pie", Material.PUMPKIN_PIE, "farming", 8, 4);
        addDefault("dried_kelp", Material.DRIED_KELP, "farming", 1.5, 0.75);
        addDefault("apple", Material.APPLE, "farming", 5, 2.5);
        addDefault("cocoa_beans", Material.COCOA_BEANS, "farming", 4, 2);
        addDefault("nether_wart", Material.NETHER_WART, "farming", 8, 4);
        addDefault("chorus_fruit", Material.CHORUS_FRUIT, "farming", 10, 5);
        addDefault("beef", Material.BEEF, "farming", 5, 2.5);
        addDefault("leather", Material.LEATHER, "farming", 8, 4);
        addDefault("rabbit_hide", Material.RABBIT_HIDE, "farming", 4, 2);
        addDefault("rabbit_foot", Material.RABBIT_FOOT, "farming", 20, 10);
        addDefault("wool", Material.WHITE_WOOL, "farming", 4, 2);
        addDefault("string", Material.STRING, "farming", 3, 1.5);

        addDefault("ender_pearl", Material.ENDER_PEARL, "mob_drops", 15, 8);
        addDefault("blaze_rod", Material.BLAZE_ROD, "mob_drops", 20, 10);
        addDefault("gunpowder", Material.GUNPOWDER, "mob_drops", 8, 4);
        addDefault("bone", Material.BONE, "mob_drops", 2, 1);
        addDefault("slime_ball", Material.SLIME_BALL, "mob_drops", 8, 4);
        addDefault("spider_eye", Material.SPIDER_EYE, "mob_drops", 5, 2.5);
        addDefault("rotten_flesh", Material.ROTTEN_FLESH, "mob_drops", 1, 0.5);
        addDefault("feather", Material.FEATHER, "mob_drops", 2, 1);
        addDefault("arrow", Material.ARROW, "mob_drops", 1, 0.5);
        addDefault("ink_sac", Material.INK_SAC, "mob_drops", 5, 2.5);
        addDefault("glowstone_dust", Material.GLOWSTONE_DUST, "mob_drops", 6, 3);
        addDefault("magma_cream", Material.MAGMA_CREAM, "mob_drops", 12, 6);
        addDefault("ghast_tear", Material.GHAST_TEAR, "mob_drops", 30, 15);
        addDefault("nether_star", Material.NETHER_STAR, "mob_drops", 5000, 3000);
        addDefault("wither_skeleton_skull", Material.WITHER_SKELETON_SKULL, "mob_drops", 1000, 600);
        addDefault("shulker_shell", Material.SHULKER_SHELL, "mob_drops", 150, 80);
        addDefault("phantom_membrane", Material.PHANTOM_MEMBRANE, "mob_drops", 25, 12.5);
        addDefault("dragon_breath", Material.DRAGON_BREATH, "mob_drops", 100, 60);
        addDefault("elytra", Material.ELYTRA, "mob_drops", 3000, 2000);
        addDefault("prismarine_shard", Material.PRISMARINE_SHARD, "mob_drops", 8, 4);
        addDefault("prismarine_crystals", Material.PRISMARINE_CRYSTALS, "mob_drops", 10, 5);
        addDefault("nautilus_shell", Material.NAUTILUS_SHELL, "mob_drops", 40, 20);
        addDefault("heart_of_the_sea", Material.HEART_OF_THE_SEA, "mob_drops", 500, 300);
        addDefault("totem_of_undying", Material.TOTEM_OF_UNDYING, "mob_drops", 800, 500);
        addDefault("trident", Material.TRIDENT, "mob_drops", 600, 350);
        addDefault("blaze_powder", Material.BLAZE_POWDER, "mob_drops", 10, 5);
        addDefault("glow_ink_sac", Material.GLOW_INK_SAC, "mob_drops", 8, 4);
        addDefault("bat_membrane", Material.PHANTOM_MEMBRANE, "mob_drops", 20, 10);

        addDefault("experience_bottle", Material.EXPERIENCE_BOTTLE, "potions", 20, 10);
        addDefault("glass_bottle", Material.GLASS_BOTTLE, "potions", 2, 1);
        addDefault("fermented_spider_eye", Material.FERMENTED_SPIDER_EYE, "potions", 15, 7.5);
        addDefault("glistering_melon", Material.GLISTERING_MELON_SLICE, "potions", 15, 7.5);
        addDefault("sugar", Material.SUGAR, "potions", 2, 1);
        addDefault("pufferfish", Material.PUFFERFISH, "potions", 20, 10);
        addDefault("turtle_helmet_mat", Material.SCUTE, "potions", 50, 25);
        addDefault("brewing_stand", Material.BREWING_STAND, "potions", 30, 15);

        addDefault("oak_planks", Material.OAK_PLANKS, "wood", 2, 1);
        addDefault("spruce_planks", Material.SPRUCE_PLANKS, "wood", 2, 1);
        addDefault("birch_planks", Material.BIRCH_PLANKS, "wood", 2, 1);
        addDefault("jungle_planks", Material.JUNGLE_PLANKS, "wood", 2, 1);
        addDefault("acacia_planks", Material.ACACIA_PLANKS, "wood", 2, 1);
        addDefault("dark_oak_planks", Material.DARK_OAK_PLANKS, "wood", 2, 1);
        addDefault("mangrove_planks", Material.MANGROVE_PLANKS, "wood", 2.5, 1.25);
        addDefault("cherry_planks", Material.CHERRY_PLANKS, "wood", 2.5, 1.25);
        addDefault("bamboo_planks", Material.BAMBOO_PLANKS, "wood", 2, 1);
        addDefault("oak_stick", Material.STICK, "wood", 0.5, 0.25);
        addDefault("charcoal", Material.CHARCOAL, "wood", 3, 1.5);
        addDefault("paper", Material.PAPER, "wood", 1, 0.5);
        addDefault("book", Material.BOOK, "wood", 5, 2.5);
        addDefault("bookshelf", Material.BOOKSHELF, "wood", 20, 10);
        addDefault("chest", Material.CHEST, "wood", 10, 5);
        addDefault("crafting_table", Material.CRAFTING_TABLE, "wood", 8, 4);
        addDefault("furnace", Material.FURNACE, "wood", 15, 7.5);
        addDefault("ladder", Material.LADDER, "wood", 1, 0.5);
        addDefault("torch", Material.TORCH, "wood", 0.5, 0.25);
        addDefault("wooden_fence", Material.OAK_FENCE, "wood", 1.5, 0.75);

        addDefault("iron_sword", Material.IRON_SWORD, "tools", 80, 40);
        addDefault("iron_pickaxe", Material.IRON_PICKAXE, "tools", 100, 50);
        addDefault("iron_axe", Material.IRON_AXE, "tools", 80, 40);
        addDefault("iron_shovel", Material.IRON_SHOVEL, "tools", 60, 30);
        addDefault("iron_hoe", Material.IRON_HOE, "tools", 60, 30);
        addDefault("diamond_sword", Material.DIAMOND_SWORD, "tools", 500, 250);
        addDefault("diamond_pickaxe", Material.DIAMOND_PICKAXE, "tools", 600, 300);
        addDefault("diamond_axe", Material.DIAMOND_AXE, "tools", 500, 250);
        addDefault("diamond_shovel", Material.DIAMOND_SHOVEL, "tools", 400, 200);
        addDefault("diamond_hoe", Material.DIAMOND_HOE, "tools", 400, 200);
        addDefault("bow", Material.BOW, "tools", 60, 30);
        addDefault("crossbow", Material.CROSSBOW, "tools", 100, 50);
        addDefault("fishing_rod", Material.FISHING_ROD, "tools", 40, 20);
        addDefault("shears", Material.SHEARS, "tools", 30, 15);
        addDefault("flint_and_steel", Material.FLINT_AND_STEEL, "tools", 30, 15);
        addDefault("compass", Material.COMPASS, "tools", 20, 10);
        addDefault("clock", Material.CLOCK, "tools", 30, 15);
        addDefault("spyglass", Material.SPYGLASS, "tools", 80, 40);
        addDefault("saddle", Material.SADDLE, "tools", 80, 40);
        addDefault("name_tag", Material.NAME_TAG, "tools", 60, 30);
        addDefault("lead", Material.LEAD, "tools", 20, 10);
        addDefault("bucket", Material.BUCKET, "tools", 15, 7.5);
        addDefault("water_bucket", Material.WATER_BUCKET, "tools", 20, 10);
        addDefault("lava_bucket", Material.LAVA_BUCKET, "tools", 30, 15);
        addDefault("shield", Material.SHIELD, "tools", 80, 40);

        addDefault("iron_helmet", Material.IRON_HELMET, "armor", 120, 60);
        addDefault("iron_chestplate", Material.IRON_CHESTPLATE, "armor", 180, 90);
        addDefault("iron_leggings", Material.IRON_LEGGINGS, "armor", 160, 80);
        addDefault("iron_boots", Material.IRON_BOOTS, "armor", 100, 50);
        addDefault("diamond_helmet", Material.DIAMOND_HELMET, "armor", 600, 300);
        addDefault("diamond_chestplate", Material.DIAMOND_CHESTPLATE, "armor", 900, 450);
        addDefault("diamond_leggings", Material.DIAMOND_LEGGINGS, "armor", 800, 400);
        addDefault("diamond_boots", Material.DIAMOND_BOOTS, "armor", 500, 250);
        addDefault("golden_helmet", Material.GOLDEN_HELMET, "armor", 80, 40);
        addDefault("golden_chestplate", Material.GOLDEN_CHESTPLATE, "armor", 120, 60);
        addDefault("golden_leggings", Material.GOLDEN_LEGGINGS, "armor", 100, 50);
        addDefault("golden_boots", Material.GOLDEN_BOOTS, "armor", 70, 35);
        addDefault("chainmail_helmet", Material.CHAINMAIL_HELMET, "armor", 150, 75);
        addDefault("chainmail_chestplate", Material.CHAINMAIL_CHESTPLATE, "armor", 200, 100);
        addDefault("chainmail_leggings", Material.CHAINMAIL_LEGGINGS, "armor", 175, 87.5);
        addDefault("chainmail_boots", Material.CHAINMAIL_BOOTS, "armor", 120, 60);
        addDefault("turtle_helmet", Material.TURTLE_HELMET, "armor", 300, 150);

        addDefault("enchanting_table", Material.ENCHANTING_TABLE, "misc", 200, 100);
        addDefault("anvil", Material.ANVIL, "misc", 150, 75);
        addDefault("grindstone", Material.GRINDSTONE, "misc", 50, 25);
        addDefault("smithing_table", Material.SMITHING_TABLE, "misc", 40, 20);
        addDefault("stonecutter", Material.STONECUTTER, "misc", 30, 15);
        addDefault("loom", Material.LOOM, "misc", 25, 12.5);
        addDefault("cartography_table", Material.CARTOGRAPHY_TABLE, "misc", 25, 12.5);
        addDefault("beacon", Material.BEACON, "misc", 8000, 5000);
        addDefault("conduit", Material.CONDUIT, "misc", 2000, 1200);
        addDefault("bell", Material.BELL, "misc", 200, 100);
        addDefault("lodestone", Material.LODESTONE, "misc", 300, 150);
        addDefault("respawn_anchor", Material.RESPAWN_ANCHOR, "misc", 500, 300);
        addDefault("end_crystal", Material.END_CRYSTAL, "misc", 300, 150);
        addDefault("map", Material.MAP, "misc", 5, 2.5);
        addDefault("firework_rocket", Material.FIREWORK_ROCKET, "misc", 10, 5);
        addDefault("lantern", Material.LANTERN, "misc", 8, 4);
        addDefault("soul_lantern", Material.SOUL_LANTERN, "misc", 8, 4);
        addDefault("sea_lantern", Material.SEA_LANTERN, "misc", 15, 7.5);
        addDefault("glowstone", Material.GLOWSTONE, "misc", 12, 6);
        addDefault("tnt", Material.TNT, "misc", 25, 12.5);
        addDefault("flint", Material.FLINT, "misc", 2, 1);
        addDefault("clay", Material.CLAY, "misc", 3, 1.5);
        addDefault("flower_pot", Material.FLOWER_POT, "misc", 5, 2.5);
        addDefault("painting", Material.PAINTING, "misc", 10, 5);
        addDefault("item_frame", Material.ITEM_FRAME, "misc", 10, 5);
        addDefault("glow_item_frame", Material.GLOW_ITEM_FRAME, "misc", 20, 10);
        addDefault("nether_quartz_ore", Material.NETHER_QUARTZ_ORE, "misc", 8, 4);
        addDefault("soul_sand", Material.SOUL_SAND, "misc", 5, 2.5);
        addDefault("soul_soil", Material.SOUL_SOIL, "misc", 5, 2.5);
        addDefault("bone_meal", Material.BONE_MEAL, "misc", 3, 1.5);
        addDefault("dye_red", Material.RED_DYE, "misc", 3, 1.5);
        addDefault("dye_blue", Material.BLUE_DYE, "misc", 3, 1.5);
        addDefault("dye_yellow", Material.YELLOW_DYE, "misc", 3, 1.5);
        addDefault("dye_green", Material.GREEN_DYE, "misc", 3, 1.5);
        addDefault("dye_black", Material.BLACK_DYE, "misc", 5, 2.5);
        addDefault("dye_white", Material.WHITE_DYE, "misc", 3, 1.5);
        addDefault("dye_purple", Material.PURPLE_DYE, "misc", 3, 1.5);
        addDefault("dye_orange", Material.ORANGE_DYE, "misc", 3, 1.5);
        addDefault("dye_cyan", Material.CYAN_DYE, "misc", 3, 1.5);
        addDefault("dye_pink", Material.PINK_DYE, "misc", 3, 1.5);
        addDefault("dye_magenta", Material.MAGENTA_DYE, "misc", 3, 1.5);
        addDefault("dye_lime", Material.LIME_DYE, "misc", 3, 1.5);
        addDefault("dye_light_blue", Material.LIGHT_BLUE_DYE, "misc", 3, 1.5);
        addDefault("dye_gray", Material.GRAY_DYE, "misc", 3, 1.5);
        addDefault("dye_light_gray", Material.LIGHT_GRAY_DYE, "misc", 3, 1.5);
        addDefault("dye_brown", Material.BROWN_DYE, "misc", 3, 1.5);

        if (!categories.isEmpty()) save();
    }

    private void addDefault(String id, Material mat, String cat, double buy, double sell) {
        items.add(new MarketItem(id, mat, cat, buy, sell));
        if (!categories.contains(cat)) categories.add(cat);
    }

    public void addItem(String id, Material mat, String cat, double buy, double sell) {
        items.add(new MarketItem(id, mat, cat, buy, sell));
        if (!categories.contains(cat)) categories.add(cat);
        saveAsync();
    }

    public boolean removeItem(String id) {
        boolean removed = items.removeIf(i -> i.id.equalsIgnoreCase(id));
        if (removed) saveAsync();
        return removed;
    }

    public void openCategoryMenu(Player player) {
        String title = "TechnoMarket - Categories";
        int rows = (int) Math.ceil(categories.size() / 7.0) + 2;
        rows = Math.max(3, Math.min(rows, 6));
        int size = rows * 9;

        Inventory inv = Bukkit.createInventory(
                new MenuHolder("market_categories"),
                size,
                LegacyComponentSerializer.legacyAmpersand().deserialize("&6" + title)
        );

        ItemStack bg = buildPane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < size; i++) inv.setItem(i, bg);

        Map<String, Long> counts = items.stream()
                .collect(Collectors.groupingBy(i -> i.category, Collectors.counting()));

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        int si = 0;
        for (String cat : categories) {
            if (si >= slots.length) break;
            Material icon = getCategoryIcon(cat);
            long count = counts.getOrDefault(cat, 0L);
            String catDisplay = capitalize(cat.replace("_", " "));
            ItemStack item = buildItem(icon,
                    "&e" + catDisplay,
                    "&7Items: &f" + count,
                    "",
                    "&eClick to Browse"
            );
            inv.setItem(slots[si], item);
            si++;
        }

        ItemStack allItem = buildItem(Material.COMPASS,
                "&aShow All",
                "&7Browse all items.",
                "",
                "&eClick"
        );
        inv.setItem(4, allItem);

        inv.setItem(size - 1, buildItem(Material.BARRIER, "&cClose"));

        player.openInventory(inv);
    }

    public void openMarket(Player player, String category, int page) {
        String catDisplay = category.equalsIgnoreCase("all") ? "All" : capitalize(category.replace("_", " "));
        String title = "TechnoMarket - " + catDisplay;

        Inventory inv = Bukkit.createInventory(
                new MenuHolder("market:" + category + ":" + page),
                54,
                LegacyComponentSerializer.legacyAmpersand().deserialize("&6" + title + " &8(P." + (page + 1) + ")")
        );

        ItemStack bg = buildPane(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack sideBg = buildPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) inv.setItem(i, bg);
        for (int i = 45; i < 54; i++) inv.setItem(i, sideBg);

        List<MarketItem> filtered = items.stream()
                .filter(i -> category.equalsIgnoreCase("all") || i.category.equalsIgnoreCase(category))
                .collect(Collectors.toList());

        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        int start = page * 45;
        int end = Math.min(start + 45, filtered.size());
        int totalPages = filtered.isEmpty() ? 1 : (int) Math.ceil((double) filtered.size() / 45);

        for (int i = start; i < end; i++) {
            MarketItem it = filtered.get(i);
            String itemName = capitalize(it.material.name().replace("_", " "));
            ItemStack is = buildItem(it.material,
                    "&f" + itemName,
                    "&7Buy: &a" + symbol + Format.number(it.buyPrice),
                    "&7Sell: &e" + symbol + Format.number(it.sellPrice),
                    "",
                    "&aLeft-Click: Buy",
                    "&cRight-Click: Sell"
            );
            inv.setItem(i - start, is);
        }

        if (page > 0) {
            inv.setItem(45, buildItem(Material.ARROW, "&e< Previous"));
        }
        if (page < totalPages - 1) {
            inv.setItem(53, buildItem(Material.ARROW, "&eNext >"));
        }

        inv.setItem(49, buildItem(Material.BOOK,
                "&6Categories",
                "&eReturn to categories"
        ));

        inv.setItem(47, buildItem(Material.BARRIER, "&cClose"));

        player.openInventory(inv);
    }

    private Material getCategoryIcon(String category) {
        return switch (category.toLowerCase()) {
            case "minerals" -> Material.DIAMOND;
            case "building" -> Material.BRICKS;
            case "farming" -> Material.WHEAT;
            case "mob_drops" -> Material.BONE;
            case "tools" -> Material.IRON_PICKAXE;
            case "armor" -> Material.IRON_CHESTPLATE;
            case "wood" -> Material.OAK_LOG;
            case "potions" -> Material.POTION;
            case "misc" -> Material.CHEST;
            default -> Material.GRASS_BLOCK;
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase()).append(" ");
            }
        }
        return sb.toString().trim();
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
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String l : loreLines) {
            if (l.isEmpty()) lore.add(Component.empty());
            else lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(l)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        }
        if (!lore.isEmpty()) meta.lore(lore);

        for (ItemFlag f : ItemFlag.values()) {
            try { meta.addItemFlags(f); } catch (Exception ignored) {}
        }
        item.setItemMeta(meta);
        return item;
    }

    public MarketItem getItemByMaterial(Material mat) {
        for (MarketItem i : items) {
            if (i.material == mat) return i;
        }
        return null;
    }

    public MarketItem getItemById(String id) {
        for (MarketItem i : items) {
            if (i.id.equalsIgnoreCase(id)) return i;
        }
        return null;
    }

    public List<MarketItem> getItems() { return items; }
    public List<String> getCategories() { return categories; }

    public void save() {
        if (marketFile == null || marketConfig == null) return;
        for (String k : marketConfig.getKeys(false)) marketConfig.set(k, null);
        for (MarketItem i : items) {
            marketConfig.set(i.id + ".material", i.material.name());
            marketConfig.set(i.id + ".category", i.category);
            marketConfig.set(i.id + ".buy", i.buyPrice);
            marketConfig.set(i.id + ".sell", i.sellPrice);
        }
        try {
            marketConfig.save(marketFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save market.yml: " + e.getMessage());
        }
    }

    public void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::save);
    }
}
