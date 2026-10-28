package com.techno.economy.stock;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StockManager {

    private final TechnoEconomyPlus plugin;
    private final Map<Material, Double> orePrices = new ConcurrentHashMap<>();
    private final Map<Material, Double> basePrices = new HashMap<>();
    private final Random random = new Random();
    private BukkitTask fluctuationTask;
    private volatile double fluctuationRate = 0.05;
    private volatile double minPrice = 1.0;
    private volatile double maxPrice = 10000.0;

    public StockManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
        initBasePrices();
    }

    private void initBasePrices() {
        basePrices.put(Material.DIAMOND, 150.0);
        basePrices.put(Material.EMERALD, 100.0);
        basePrices.put(Material.GOLD_INGOT, 50.0);
        basePrices.put(Material.IRON_INGOT, 20.0);
        basePrices.put(Material.NETHERITE_INGOT, 1200.0);
        basePrices.put(Material.COPPER_INGOT, 5.0);
        basePrices.put(Material.LAPIS_LAZULI, 10.0);
        basePrices.put(Material.REDSTONE, 8.0);

        orePrices.putAll(basePrices);
    }

    public void start() {
        fluctuationRate = plugin.getConfig().getDouble("borsa.fluctuation-rate", 0.05);
        minPrice = plugin.getConfig().getDouble("borsa.min-price", 1.0);
        maxPrice = plugin.getConfig().getDouble("borsa.max-price", 10000.0);

        int intervalSeconds = plugin.getConfig().getInt("borsa.fluctuation-interval-seconds", 300);
        fluctuationTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::fluctuatePrices, intervalSeconds * 20L, intervalSeconds * 20L);
    }

    public void stop() {
        if (fluctuationTask != null) {
            fluctuationTask.cancel();
        }
    }

    private void fluctuatePrices() {
        for (Map.Entry<Material, Double> entry : orePrices.entrySet()) {
            double current = entry.getValue();
            double changePercent = (random.nextDouble() * 2.0 - 1.0) * fluctuationRate;
            double next = current * (1.0 + changePercent);
            if (next < minPrice) next = minPrice;
            if (next > maxPrice) next = maxPrice;
            entry.setValue(Math.round(next * 100.0) / 100.0);
        }
    }

    public double getOrePrice(Material mat) {
        return orePrices.getOrDefault(mat, 10.0);
    }

    public boolean buyOre(UUID playerUuid, Material mat, int amount) {
        if (amount <= 0 || !orePrices.containsKey(mat)) return false;
        double unitPrice = getOrePrice(mat);
        double totalCost = unitPrice * amount;
        if (!plugin.getBalanceManager().has(playerUuid, totalCost)) return false;
        plugin.getBalanceManager().withdraw(playerUuid, totalCost);
        plugin.getTransactionLogManager().log(playerUuid, "STOCK_ORE_BUY", totalCost, "Commodity: " + mat.name() + " Amount: " + amount);
        return true;
    }

    public boolean sellOre(UUID playerUuid, Material mat, int amount) {
        if (amount <= 0 || !orePrices.containsKey(mat)) return false;
        double unitPrice = getOrePrice(mat);
        double totalReward = unitPrice * amount;
        plugin.getBalanceManager().deposit(playerUuid, totalReward);
        plugin.getTransactionLogManager().log(playerUuid, "STOCK_ORE_SELL", totalReward, "Commodity: " + mat.name() + " Amount: " + amount);
        return true;
    }

    public java.util.List<String> getAllStockCompanies() {
        java.util.List<String> list = new java.util.ArrayList<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return list;
            try (PreparedStatement ps = conn.prepareStatement("SELECT sirket_name FROM techno_borsa_hisseler ORDER BY share_price DESC LIMIT 14;");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("sirket_name"));
                }
            }
            if (list.isEmpty()) {
                try (PreparedStatement ps2 = conn.prepareStatement("SELECT name FROM techno_sirketler ORDER BY balance DESC LIMIT 14;");
                     ResultSet rs2 = ps2.executeQuery()) {
                    while (rs2.next()) {
                        list.add(rs2.getString("name"));
                    }
                }
            }
        } catch (Exception ignored) {}
        if (list.isEmpty()) {
            list.add("TechnoCorp");
            list.add("MineIndustries");
            list.add("GlobalHoldings");
        }
        return list;
    }

    public double getOrePrice(String name) {
        try {
            Material mat = Material.matchMaterial(name.toUpperCase());
            if (mat != null && orePrices.containsKey(mat)) {
                return orePrices.get(mat);
            }
        } catch (Exception ignored) {}
        return 10.0;
    }

    public double getCompanySharePrice(String companyName) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return 10.0;

            try (PreparedStatement ps = conn.prepareStatement("SELECT balance FROM techno_sirketler WHERE name = ? LIMIT 1;")) {
                ps.setString(1, companyName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        double balance = rs.getDouble("balance");
                        double calculated = 10.0 + (balance / 500.0);
                        return Math.max(1.0, Math.round(calculated * 100.0) / 100.0);
                    }
                }
            }
        } catch (Exception ignored) {}
        return 10.0;
    }

    public int getAvailableShares(String companyName) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return 0;
            return getOrCreateAvailableShares(conn, companyName);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int getOrCreateAvailableShares(Connection conn, String companyName) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT available_shares FROM techno_borsa_hisseler WHERE sirket_name = ? LIMIT 1;")) {
            ps.setString(1, companyName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("available_shares");
                }
            }
        }
        boolean isRemote = plugin.getDatabaseManager().isRemote();
        String insertSql = isRemote
                ? "INSERT INTO techno_borsa_hisseler (sirket_name, share_price, total_shares, available_shares) VALUES (?, 10.0, 1000, 1000) ON DUPLICATE KEY UPDATE total_shares=VALUES(total_shares);"
                : "INSERT OR IGNORE INTO techno_borsa_hisseler (sirket_name, share_price, total_shares, available_shares) VALUES (?, 10.0, 1000, 1000);";
        try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
            ins.setString(1, companyName);
            ins.executeUpdate();
        }
        return 1000;
    }

    public boolean buyCompanyShares(UUID playerUuid, String companyName, int amount) {
        if (amount <= 0) return false;
        double price = getCompanySharePrice(companyName);
        double totalCost = price * amount;

        if (!plugin.getBalanceManager().has(playerUuid, totalCost)) return false;

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return false;

            int available = getOrCreateAvailableShares(conn, companyName);
            if (available < amount) return false;

            plugin.getBalanceManager().withdraw(playerUuid, totalCost);
            plugin.getTransactionLogManager().log(playerUuid, "STOCK_BUY", totalCost, "Company: " + companyName + " Shares: " + amount);

            try (PreparedStatement dec = conn.prepareStatement("UPDATE techno_borsa_hisseler SET available_shares = available_shares - ? WHERE sirket_name = ?;")) {
                dec.setInt(1, amount);
                dec.setString(2, companyName);
                dec.executeUpdate();
            }

            int existingShares = 0;
            double existingBuyPrice = price;
            int recordId = -1;

            try (PreparedStatement ps = conn.prepareStatement("SELECT id, shares_owned, buy_price FROM techno_borsa_portfoy WHERE player_uuid = ? AND sirket_name = ? LIMIT 1;")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, companyName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        recordId = rs.getInt("id");
                        existingShares = rs.getInt("shares_owned");
                        existingBuyPrice = rs.getDouble("buy_price");
                    }
                }
            }

            if (recordId != -1) {
                int newShares = existingShares + amount;
                double newAvgBuy = ((existingShares * existingBuyPrice) + (amount * price)) / newShares;
                try (PreparedStatement ps = conn.prepareStatement("UPDATE techno_borsa_portfoy SET shares_owned = ?, buy_price = ? WHERE id = ?;")) {
                    ps.setInt(1, newShares);
                    ps.setDouble(2, newAvgBuy);
                    ps.setInt(3, recordId);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO techno_borsa_portfoy (player_uuid, sirket_name, shares_owned, buy_price) VALUES (?, ?, ?, ?);")) {
                    ps.setString(1, playerUuid.toString());
                    ps.setString(2, companyName);
                    ps.setInt(3, amount);
                    ps.setDouble(4, price);
                    ps.executeUpdate();
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean sellCompanyShares(UUID playerUuid, String companyName, int amount) {
        if (amount <= 0) return false;
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return false;

            int recordId = -1;
            int owned = 0;

            try (PreparedStatement ps = conn.prepareStatement("SELECT id, shares_owned FROM techno_borsa_portfoy WHERE player_uuid = ? AND sirket_name = ? LIMIT 1;")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, companyName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        recordId = rs.getInt("id");
                        owned = rs.getInt("shares_owned");
                    }
                }
            }

            if (recordId == -1 || owned < amount) return false;

            double price = getCompanySharePrice(companyName);
            double totalReturn = price * amount;

            if (owned == amount) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM techno_borsa_portfoy WHERE id = ?;")) {
                    ps.setInt(1, recordId);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE techno_borsa_portfoy SET shares_owned = ? WHERE id = ?;")) {
                    ps.setInt(1, owned - amount);
                    ps.setInt(2, recordId);
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement inc = conn.prepareStatement("UPDATE techno_borsa_hisseler SET available_shares = available_shares + ? WHERE sirket_name = ?;")) {
                inc.setInt(1, amount);
                inc.setString(2, companyName);
                inc.executeUpdate();
            }

            plugin.getBalanceManager().deposit(playerUuid, totalReturn);
            plugin.getTransactionLogManager().log(playerUuid, "STOCK_SELL", totalReturn, "Company: " + companyName + " Shares: " + amount);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public double getPortfolioValue(UUID playerUuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return 0.0;
            double total = 0.0;

            try (PreparedStatement ps = conn.prepareStatement("SELECT sirket_name, shares_owned FROM techno_borsa_portfoy WHERE player_uuid = ?;")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String sirket = rs.getString("sirket_name");
                        int shares = rs.getInt("shares_owned");
                        double price = getCompanySharePrice(sirket);
                        total += shares * price;
                    }
                }
            }
            return Math.round(total * 100.0) / 100.0;
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    public Map<String, Integer> getPlayerShares(UUID playerUuid) {
        Map<String, Integer> map = new HashMap<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return map;

            try (PreparedStatement ps = conn.prepareStatement("SELECT sirket_name, shares_owned FROM techno_borsa_portfoy WHERE player_uuid = ?;")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        map.put(rs.getString("sirket_name"), rs.getInt("shares_owned"));
                    }
                }
            }
        } catch (Exception ignored) {}
        return map;
    }

    public Map<String, Integer> getPlayerPortfolio(UUID playerUuid) {
        return getPlayerShares(playerUuid);
    }

    public double getProfitLoss(UUID playerUuid) {
        double currentValue = getPortfolioValue(playerUuid);
        double invested = 0.0;
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return 0.0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT SUM(buy_price * shares_owned) AS total FROM techno_borsa_portfoy WHERE player_uuid = ?;")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        invested = rs.getDouble("total");
                    }
                }
            }
        } catch (Exception ignored) {}
        return Math.round((currentValue - invested) * 100.0) / 100.0;
    }
}
