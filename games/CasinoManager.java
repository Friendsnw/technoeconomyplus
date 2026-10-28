package com.techno.economy.games;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.util.ItemBuilder;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class CasinoManager {

    private final TechnoEconomyPlus plugin;
    private final Random random = new Random();
    private static final Material[] SLOT_SYMBOLS = {
            Material.DIAMOND,
            Material.EMERALD,
            Material.GOLD_INGOT,
            Material.IRON_INGOT,
            Material.REDSTONE,
            Material.NETHERITE_SCRAP
    };

    public CasinoManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void openSlots(Player player, double bet) {
        String title = "\u00a76\u00a7lTechnoCasino \u00a78- \u00a7eSlots";
        Inventory inv = Bukkit.createInventory(null, 27, LegacyComponentSerializer.legacySection().deserialize(title));

        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        player.openInventory(inv);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(inv)) {
                    cancel();
                    return;
                }

                ticks++;
                Material s1 = SLOT_SYMBOLS[random.nextInt(SLOT_SYMBOLS.length)];
                Material s2 = SLOT_SYMBOLS[random.nextInt(SLOT_SYMBOLS.length)];
                Material s3 = SLOT_SYMBOLS[random.nextInt(SLOT_SYMBOLS.length)];

                inv.setItem(12, new ItemBuilder(s1).name("\u00a7e?").build());
                inv.setItem(13, new ItemBuilder(s2).name("\u00a7e?").build());
                inv.setItem(14, new ItemBuilder(s3).name("\u00a7e?").build());
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.5f);

                if (ticks >= 15) {
                    cancel();
                    evaluateResults(player, inv, bet, s1, s2, s3);
                }
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    private void evaluateResults(Player player, Inventory inv, double bet, Material s1, Material s2, Material s3) {
        Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        java.util.UUID uuid = player.getUniqueId();
        if (s1 == s2 && s2 == s3) {
            double multiplier = (s1 == Material.NETHERITE_SCRAP) ? 10.0 : 5.0;
            double win = bet * multiplier;
            plugin.getBalanceManager().addBalance(uuid, win);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    plugin.getDatabaseManager().recordCasinoGame(uuid, true, win - bet, "JACKPOT (" + s1.name() + ")"));
            plugin.getTransactionLogManager().log(uuid, player.getName(), "CASINO", win,
                    plugin.getBalanceManager().getBalance(uuid), "Slots Jackpot 3x (" + s1.name() + ")");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.slots-win",
                    java.util.Map.of("amount", String.valueOf(win), "symbol", symbol)));
        } else if (s1 == s2 || s2 == s3 || s1 == s3) {
            double win = bet * 1.5;
            plugin.getBalanceManager().addBalance(uuid, win);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    plugin.getDatabaseManager().recordCasinoGame(uuid, true, win - bet, "MATCH 2x"));
            plugin.getTransactionLogManager().log(uuid, player.getName(), "CASINO", win,
                    plugin.getBalanceManager().getBalance(uuid), "Slots 2x match");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.slots-win",
                    java.util.Map.of("amount", String.valueOf(win), "symbol", symbol)));
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    plugin.getDatabaseManager().recordCasinoGame(uuid, false, bet, "LOST"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f);
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.slots-lose"));
        }
    }

    public boolean playCoinflip(Player player, double bet, String guess) {
        boolean heads = random.nextBoolean();
        String result = heads ? "heads" : "tails";
        boolean won = result.equalsIgnoreCase(guess);
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        java.util.UUID uuid = player.getUniqueId();

        if (won) {
            double win = bet * 2.0;
            plugin.getBalanceManager().addBalance(uuid, win);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    plugin.getDatabaseManager().recordCasinoGame(uuid, true, bet, "COINFLIP WIN"));
            plugin.getTransactionLogManager().log(uuid, player.getName(), "COINFLIP", win,
                    plugin.getBalanceManager().getBalance(uuid), "Won coinflip (" + result + ")");
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.coinflip-win",
                    java.util.Map.of("result", result, "amount", String.valueOf(win), "symbol", symbol)));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            return true;
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    plugin.getDatabaseManager().recordCasinoGame(uuid, false, bet, "COINFLIP LOSE"));
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.coinflip-lose",
                    java.util.Map.of("result", result)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }
    }

    public void openCasinoHistoryMenu(Player player) {
        String title = "\u00268\u0026lCasino Spin History";
        java.util.UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            var history = plugin.getDatabaseManager().getCasinoHistory(uuid, 45);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                Inventory inv = Bukkit.createInventory(new com.techno.economy.menu.MenuHolder("casino_history"), 54,
                        LegacyComponentSerializer.legacyAmpersand().deserialize(title));
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm");
                for (int i = 0; i < Math.min(history.size(), 45); i++) {
                    var entry = history.get(i);
                    boolean isWon = !entry.outcome.equalsIgnoreCase("LOST") && !entry.outcome.contains("LOSE");
                    ItemStack is = new ItemStack(isWon ? Material.EMERALD : Material.REDSTONE);
                    var meta = is.getItemMeta();
                    if (meta != null) {
                        String name = (isWon ? "\u0026a\u0026l" : "\u0026c\u0026l") + entry.outcome + " " + (isWon ? "+$" : "-$") + entry.amount;
                        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name)
                                .decoration(TextDecoration.ITALIC, false));
                        java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
                        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("\u00267Outcome: \u0026f" + entry.outcome)
                                .decoration(TextDecoration.ITALIC, false));
                        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("\u00267Amount: \u0026f$" + entry.amount)
                                .decoration(TextDecoration.ITALIC, false));
                        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("\u00267Date: \u0026f" + sdf.format(new java.util.Date(entry.timestamp)))
                                .decoration(TextDecoration.ITALIC, false));
                        meta.lore(lore);
                        is.setItemMeta(meta);
                    }
                    inv.setItem(i, is);
                }

                ItemStack back = new ItemStack(Material.ARROW);
                var bm = back.getItemMeta();
                if (bm != null) {
                    bm.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("\u0026e\u2190 Back to Casino Menu")
                            .decoration(TextDecoration.ITALIC, false));
                    back.setItemMeta(bm);
                }
                inv.setItem(49, back);
                player.openInventory(inv);
            });
        });
    }
}
