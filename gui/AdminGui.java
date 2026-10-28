package com.techno.economy.gui;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.menu.MenuHolder;
import com.techno.economy.util.ItemBuilder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class AdminGui {

    private final TechnoEconomyPlus plugin;

    public AdminGui(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        var lang = plugin.getLanguageManager();

        String rawTitle = lang.getRawMessage("gui.admin.title");
        if (rawTitle.startsWith("gui.")) rawTitle = "&c&lTechnoEconomy &8- &4Admin Control Panel";
        String title = LegacyComponentSerializer.legacySection().serialize(
                LegacyComponentSerializer.legacyAmpersand().deserialize(rawTitle));

        Inventory inv = Bukkit.createInventory(new MenuHolder("admin_main"), 54, title);

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

        ItemStack titleItem = new ItemBuilder(Material.NETHER_STAR)
                .name(lang.getRawMessage("gui.admin.header-name"))
                .lore(
                        lang.getRawMessage("gui.admin.header-lore1"),
                        lang.getRawMessage("gui.admin.header-lore2")
                ).build();
        inv.setItem(4, titleItem);

        int[] cmdSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        ItemStack[] items = {
                createCmdItem(Material.GOLD_INGOT, "/balance", "technoeconomy.use", "gui.admin.cmd.balance", "gui.admin.action.click-view"),
                createCmdItem(Material.EMERALD, "/pay <player> <amount>", "technoeconomy.use", "gui.admin.cmd.pay", "gui.admin.action.command-only"),
                createCmdItem(Material.CHEST, "/bank", "technoeconomy.bank", "gui.admin.cmd.bank", "gui.admin.action.click-open"),
                createCmdItem(Material.GOLD_BLOCK, "/baltop", "technoeconomy.use", "gui.admin.cmd.baltop", "gui.admin.action.click-open"),
                createCmdItem(Material.PAPER, "/check", "technoeconomy.check", "gui.admin.cmd.check", "gui.admin.action.click-open"),
                createCmdItem(Material.TARGET, "/bounty", "technoeconomy.use", "gui.admin.cmd.bounty", "gui.admin.action.click-open"),
                createCmdItem(Material.COMPASS, "/market", "technoeconomy.market", "gui.admin.cmd.market", "gui.admin.action.click-open"),

                createCmdItem(Material.DISPENSER, "/shop", "technoeconomy.shop", "gui.admin.cmd.shop", "gui.admin.action.click-open"),
                createCmdItem(Material.NETHER_STAR, "/ah", "technoeconomy.ah", "gui.admin.cmd.ah", "gui.admin.action.click-open"),
                createCmdItem(Material.TRAPPED_CHEST, "/chestshop", "technoeconomy.chestshop", "gui.admin.cmd.chestshop", "gui.admin.action.command-only"),
                createCmdItem(Material.HOPPER, "/sell & /sellwand", "technoeconomy.sell", "gui.admin.cmd.sell", "gui.admin.action.command-only"),
                createCmdItem(Material.SPYGLASS, "/worth", "technoeconomy.use", "gui.admin.cmd.worth", "gui.admin.action.command-only"),
                createCmdItem(Material.DIAMOND, "/stock & /borsa", "technoeconomy.stock", "gui.admin.cmd.stock", "gui.admin.action.click-open"),
                createCmdItem(Material.BOOKSHELF, "/company & /sirket", "technoeconomy.sirket", "gui.admin.cmd.company", "gui.admin.action.command-only"),

                createCmdItem(Material.JUKEBOX, "/casino", "technoeconomy.games", "gui.admin.cmd.casino", "gui.admin.action.click-open"),
                createCmdItem(Material.SUNFLOWER, "/coinflip <amount> <h/t>", "technoeconomy.games", "gui.admin.cmd.coinflip", "gui.admin.action.command-only"),
                createCmdItem(Material.EXPERIENCE_BOTTLE, "/lottery", "technoeconomy.games", "gui.admin.cmd.lottery", "gui.admin.action.command-only"),
                createCmdItem(Material.DIAMOND_SWORD, "/duel <player> <amount>", "technoeconomy.duel", "gui.admin.cmd.duel", "gui.admin.action.click-open"),
                createCmdItem(Material.BEACON, "/clan", "technoeconomy.clan", "gui.admin.cmd.clan", "gui.admin.action.click-open"),
                createCmdItem(Material.NETHERITE_SWORD, "/clanadmin", "technoeconomy.clan.admin", "gui.admin.cmd.clanadmin", "gui.admin.action.command-only"),
                createCmdItem(Material.CLOCK, "/daily", "technoeconomy.use", "gui.admin.cmd.daily", "gui.admin.action.click-open"),

                createCmdItem(Material.WRITABLE_BOOK, "/payday", "technoeconomy.payday", "gui.admin.cmd.payday", "gui.admin.action.command-only"),
                createCmdItem(Material.IRON_PICKAXE, "/jobs", "technoeconomy.jobs", "gui.admin.cmd.jobs", "gui.admin.action.click-open"),
                createCmdItem(Material.ITEM_FRAME, "/hud", "technoeconomy.use", "gui.admin.cmd.hud", "gui.admin.action.command-only"),
                createCmdItem(Material.LIME_DYE, "/eco give <player> <amount>", "technoeconomy.admin", "gui.admin.cmd.eco-give", "gui.admin.action.admin-only"),
                createCmdItem(Material.RED_DYE, "/eco take <player> <amount>", "technoeconomy.admin", "gui.admin.cmd.eco-take", "gui.admin.action.admin-only"),
                createCmdItem(Material.YELLOW_DYE, "/eco set <player> <amount>", "technoeconomy.admin", "gui.admin.cmd.eco-set", "gui.admin.action.admin-only"),
                createCmdItem(Material.GRAY_DYE, "/eco reset <player>", "technoeconomy.admin", "gui.admin.cmd.eco-reset", "gui.admin.action.admin-only")
        };

        for (int i = 0; i < items.length && i < cmdSlots.length; i++) {
            inv.setItem(cmdSlots[i], items[i]);
        }

        ItemStack reloadBtn = new ItemBuilder(Material.REDSTONE_TORCH)
                .name(lang.getRawMessage("gui.admin.reload-name"))
                .lore("&7" + lang.getRawMessage("gui.admin.reload-lore"))
                .build();
        inv.setItem(46, reloadBtn);

        ItemStack backupBtn = new ItemBuilder(Material.ENDER_CHEST)
                .name(lang.getRawMessage("gui.admin.backup-name"))
                .lore("&7" + lang.getRawMessage("gui.admin.backup-lore"))
                .build();
        inv.setItem(47, backupBtn);

        ItemStack close = new ItemBuilder(Material.BARRIER)
                .name(lang.getRawMessage("gui.admin.close-name"))
                .build();
        inv.setItem(49, close);

        ItemStack licenseBtn = new ItemBuilder(Material.KNOWLEDGE_BOOK)
                .name(lang.getRawMessage("gui.admin.license-name"))
                .lore("&7" + lang.getRawMessage("gui.admin.license-lore"))
                .build();
        inv.setItem(51, licenseBtn);

        ItemStack engineInfo = new ItemBuilder(Material.COMMAND_BLOCK)
                .name("&e&lTechnoEconomy Engine")
                .lore(
                        "&7Version: &f" + plugin.getDescription().getVersion(),
                        "&7Paper / Spigot / Folia Compatible",
                        "&aOperational"
                ).build();
        inv.setItem(52, engineInfo);

        player.openInventory(inv);
    }

    private ItemStack createCmdItem(Material mat, String syntax, String perm, String descKey, String actionKey) {
        var lang = plugin.getLanguageManager();
        return new ItemBuilder(mat)
                .name("&6&l" + syntax)
                .lore(
                        "&7" + lang.getRawMessage(descKey),
                        "&8Permission: &f" + perm,
                        "",
                        "&e" + lang.getRawMessage(actionKey)
                ).build();
    }
}
