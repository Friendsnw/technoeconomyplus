package com.techno.economy.check;

import com.techno.economy.TechnoEconomyPlus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CheckManager {

    private final TechnoEconomyPlus plugin;
    private final NamespacedKey checkKey;

    public CheckManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
        this.checkKey = new NamespacedKey(plugin, "check_uuid");
    }

    public ItemStack createCheck(Player player, double amount) {
        double min = plugin.getConfig().getDouble("cek.min-amount", 10.0);
        double max = plugin.getConfig().getDouble("cek.max-amount", 10000000.0);
        if (amount < min || amount > max) return null;

        double commPercent = plugin.getConfig().getDouble("cek.commission-percent", 1.0);
        double fee = Math.round((amount * (commPercent / 100.0)) * 100.0) / 100.0;
        double totalCost = amount + fee;

        if (!plugin.getBalanceManager().has(player.getUniqueId(), totalCost)) return null;

        UUID checkId = UUID.randomUUID();
        long now = System.currentTimeMillis();

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return null;

            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO techno_cekler (id, issuer_uuid, issuer_name, amount, created_at, redeemed) VALUES (?, ?, ?, ?, ?, 0);")) {
                ps.setString(1, checkId.toString());
                ps.setString(2, player.getUniqueId().toString());
                ps.setString(3, player.getName());
                ps.setDouble(4, amount);
                ps.setLong(5, now);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            return null;
        }

        plugin.getBalanceManager().withdraw(player.getUniqueId(), totalCost);
        plugin.getTransactionLogManager().log(player.getUniqueId(), "CHECK_CREATE", totalCost, "Check: " + checkId + " Amount: " + amount);

        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        meta.getPersistentDataContainer().set(checkKey, PersistentDataType.STRING, checkId.toString());

        String dateStr = new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(now));

        meta.displayName(plugin.getLanguageManager().getMessage("messages.check-item-name", Map.of("amount", String.valueOf(amount))));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.getLanguageManager().getMessage("messages.check-item-lore-amount", Map.of("amount", String.valueOf(amount))));
        lore.add(plugin.getLanguageManager().getMessage("messages.check-item-lore-issuer", Map.of("issuer", player.getName())));
        lore.add(plugin.getLanguageManager().getMessage("messages.check-item-lore-date", Map.of("date", dateStr)));
        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&8ID: " + checkId));
        lore.add(Component.empty());
        lore.add(plugin.getLanguageManager().getMessage("messages.check-item-lore-click"));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public boolean redeemCheck(Player player, ItemStack item) {
        if (item == null || item.getType() != Material.PAPER) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        String idStr = meta.getPersistentDataContainer().get(checkKey, PersistentDataType.STRING);
        if (idStr == null) return false;

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return false;

            double amount = 0.0;
            int redeemed = 1;

            try (PreparedStatement ps = conn.prepareStatement("SELECT amount, redeemed FROM techno_cekler WHERE id = ? LIMIT 1;")) {
                ps.setString(1, idStr);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        amount = rs.getDouble("amount");
                        redeemed = rs.getInt("redeemed");
                    }
                }
            }

            if (redeemed != 0 || amount <= 0.0) {
                return false;
            }

            try (PreparedStatement ps = conn.prepareStatement("UPDATE techno_cekler SET redeemed = 1 WHERE id = ? AND redeemed = 0;")) {
                ps.setString(1, idStr);
                int updated = ps.executeUpdate();
                if (updated <= 0) return false;
            }

            item.setAmount(item.getAmount() - 1);
            plugin.getBalanceManager().deposit(player.getUniqueId(), amount);
            plugin.getTransactionLogManager().log(player.getUniqueId(), "CHECK_REDEEM", amount, "Check ID: " + idStr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int getPlayerCheckCount(UUID playerUuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return 0;

            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS cnt FROM techno_cekler WHERE issuer_uuid = ?;")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("cnt");
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public double getLastCheckAmount(UUID playerUuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return 0.0;

            try (PreparedStatement ps = conn.prepareStatement("SELECT amount FROM techno_cekler WHERE issuer_uuid = ? ORDER BY created_at DESC LIMIT 1;")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getDouble("amount");
                }
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    public NamespacedKey getCheckKey() {
        return checkKey;
    }

    public static class CheckRecord {
        public final String id;
        public final double amount;
        public final long createdAt;
        public final boolean redeemed;

        public CheckRecord(String id, double amount, long createdAt, boolean redeemed) {
            this.id = id;
            this.amount = amount;
            this.createdAt = createdAt;
            this.redeemed = redeemed;
        }
    }

    public List<CheckRecord> getPlayerChecks(UUID uuid) {
        List<CheckRecord> list = new ArrayList<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return list;
            try (PreparedStatement ps = conn.prepareStatement("SELECT id, amount, created_at, redeemed FROM techno_cekler WHERE issuer_uuid = ? ORDER BY created_at DESC LIMIT 45;")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new CheckRecord(
                                rs.getString("id"),
                                rs.getDouble("amount"),
                                rs.getLong("created_at"),
                                rs.getInt("redeemed") == 1
                        ));
                    }
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    public void openPlayerChecksMenu(Player player) {
        String title = "&8&lMy Active & Past Checks";
        org.bukkit.inventory.Inventory inv = org.bukkit.Bukkit.createInventory(
                new com.techno.economy.menu.MenuHolder("checks_list"), 54,
                LegacyComponentSerializer.legacyAmpersand().deserialize(title));

        List<CheckRecord> checks = getPlayerChecks(player.getUniqueId());
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

        for (int i = 0; i < Math.min(checks.size(), 45); i++) {
            CheckRecord c = checks.get(i);
            ItemStack is = new ItemStack(Material.PAPER);
            ItemMeta meta = is.getItemMeta();
            if (meta != null) {
                String name = (c.redeemed ? "&c&l" : "&a&l") + "$" + c.amount + (c.redeemed ? " &7(Redeemed)" : " &a(Active)");
                meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name)
                        .decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&7Value: &f$" + c.amount)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&7Date: &f" + sdf.format(new Date(c.createdAt)))
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&7Status: " + (c.redeemed ? "&cRedeemed (Used)" : "&aActive (Unclaimed)"))
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize("&8ID: " + c.id)
                        .decoration(TextDecoration.ITALIC, false));
                meta.lore(lore);
                is.setItemMeta(meta);
            }
            inv.setItem(i, is);
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        if (bm != null) {
            bm.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&e← Back to Check Menu")
                    .decoration(TextDecoration.ITALIC, false));
            back.setItemMeta(bm);
        }
        inv.setItem(49, back);

        player.openInventory(inv);
    }
}
