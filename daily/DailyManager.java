package com.techno.economy.daily;

import com.techno.economy.TechnoEconomyPlus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DailyManager {

    private final TechnoEconomyPlus plugin;
    private final ConcurrentHashMap<UUID, DailyData> dailyDataMap = new ConcurrentHashMap<>();

    public static class DailyData {
        public LocalDate lastClaim;
        public int streak;

        public DailyData(LocalDate lastClaim, int streak) {
            this.lastClaim = lastClaim;
            this.streak = streak;
        }
    }

    public DailyManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        dailyDataMap.clear();
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn != null) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT uuid, streak, last_claim FROM techno_daily;");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            UUID uuid = UUID.fromString(rs.getString("uuid"));
                            int streak = rs.getInt("streak");
                            long lastClaimEpoch = rs.getLong("last_claim");
                            LocalDate lastClaim = lastClaimEpoch > 0 ? LocalDate.ofEpochDay(lastClaimEpoch) : null;
                            dailyDataMap.put(uuid, new DailyData(lastClaim, streak));
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public boolean canClaim(UUID uuid) {
        DailyData data = dailyDataMap.get(uuid);
        if (data == null || data.lastClaim == null) return true;
        return !data.lastClaim.equals(LocalDate.now());
    }

    public double claim(UUID uuid) {
        DailyData data = dailyDataMap.computeIfAbsent(uuid, k -> new DailyData(null, 0));
        LocalDate today = LocalDate.now();

        if (data.lastClaim != null && data.lastClaim.plusDays(1).equals(today)) {
            data.streak++;
        } else if (data.lastClaim == null || !data.lastClaim.equals(today)) {
            data.streak = 1;
        }

        int maxStreak = plugin.getConfig().getInt("daily.max-streak", 7);
        if (data.streak > maxStreak) {
            data.streak = 1;
        }

        data.lastClaim = today;

        double base = plugin.getConfig().getDouble("daily.base-reward", 50.0);
        double bonus = plugin.getConfig().getDouble("daily.streak-bonus", 10.0);
        double reward = base + ((data.streak - 1) * bonus);

        saveAsync();
        return reward;
    }

    public int getStreak(UUID uuid) {
        DailyData data = dailyDataMap.get(uuid);
        return data != null ? data.streak : 0;
    }

    public void save() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return;

            boolean isRemote = plugin.getDatabaseManager().isRemote();
            String sql = isRemote
                    ? "INSERT INTO techno_daily (uuid, streak, last_claim) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE streak = VALUES(streak), last_claim = VALUES(last_claim);"
                    : "INSERT OR REPLACE INTO techno_daily (uuid, streak, last_claim) VALUES (?, ?, ?);";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map.Entry<UUID, DailyData> entry : dailyDataMap.entrySet()) {
                    ps.setString(1, entry.getKey().toString());
                    ps.setInt(2, entry.getValue().streak);
                    ps.setLong(3, entry.getValue().lastClaim != null ? entry.getValue().lastClaim.toEpochDay() : 0);
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
}
