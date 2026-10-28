package com.techno.economy.bank;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BankManager {

    private final TechnoEconomyPlus plugin;
    private final ConcurrentHashMap<UUID, Double> bankBalances = new ConcurrentHashMap<>();
    private BukkitTask interestTask;
    private long nextInterestTimestamp = 0;
    private volatile double interestRate = 0.02;

    public BankManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        bankBalances.clear();
        interestRate = plugin.getConfig().getDouble("bank.interest-rate", 0.02);

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn != null) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT uuid, balance FROM techno_banks;");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            UUID uuid = UUID.fromString(rs.getString("uuid"));
                            bankBalances.put(uuid, rs.getDouble("balance"));
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
        scheduleInterestTask();
    }

    public void scheduleInterestTask() {
        if (interestTask != null) {
            interestTask.cancel();
        }
        int intervalMinutes = plugin.getConfig().getInt("bank.interest-interval-minutes", 60);
        long ticks = intervalMinutes * 60L * 20L;
        nextInterestTimestamp = Instant.now().getEpochSecond() + (intervalMinutes * 60L);

        interestTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            payoutInterest();
            nextInterestTimestamp = Instant.now().getEpochSecond() + (intervalMinutes * 60L);
        }, ticks, ticks);
    }

    private void payoutInterest() {
        for (var entry : bankBalances.entrySet()) {
            double current = entry.getValue();
            if (current > 0) {
                double interest = Math.round(current * interestRate * 100.0) / 100.0;
                entry.setValue(current + interest);
            }
        }
        saveAsync();
    }

    public double getBankBalance(UUID uuid) {
        return bankBalances.getOrDefault(uuid, 0.0);
    }

    public double getBalance(UUID uuid) {
        return getBankBalance(uuid);
    }

    public void deposit(UUID uuid, double amount) {
        bankBalances.merge(uuid, amount, Double::sum);
        saveAsync();
    }

    public boolean withdraw(UUID uuid, double amount) {
        double current = getBankBalance(uuid);
        if (current < amount) return false;
        bankBalances.put(uuid, current - amount);
        saveAsync();
        return true;
    }

    public void save() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return;
            boolean isRemote = plugin.getDatabaseManager().isRemote();
            String sql = isRemote
                    ? "INSERT INTO techno_banks (uuid, balance, last_interest) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE balance = VALUES(balance);"
                    : "INSERT OR REPLACE INTO techno_banks (uuid, balance, last_interest) VALUES (?, ?, ?);";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                long now = System.currentTimeMillis();
                for (Map.Entry<UUID, Double> entry : bankBalances.entrySet()) {
                    ps.setString(1, entry.getKey().toString());
                    ps.setDouble(2, entry.getValue());
                    ps.setLong(3, now);
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
        if (interestTask != null) {
            interestTask.cancel();
            interestTask = null;
        }
        save();
    }

    public long getNextInterestTimestamp() {
        return nextInterestTimestamp;
    }
}
