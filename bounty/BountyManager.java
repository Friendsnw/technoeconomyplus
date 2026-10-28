package com.techno.economy.bounty;

import com.techno.economy.TechnoEconomyPlus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BountyManager {

    private final TechnoEconomyPlus plugin;
    private final ConcurrentHashMap<UUID, Double> bounties = new ConcurrentHashMap<>();

    public BountyManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        bounties.clear();
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn != null) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT target_uuid, amount FROM techno_bounties;");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            UUID uuid = UUID.fromString(rs.getString("target_uuid"));
                            bounties.put(uuid, rs.getDouble("amount"));
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    public double getBounty(UUID target) {
        return bounties.getOrDefault(target, 0.0);
    }

    public void addBounty(UUID target, double amount) {
        bounties.merge(target, amount, Double::sum);
        saveAsync();
    }

    public double claimBounty(UUID target) {
        Double val = bounties.remove(target);
        if (val != null) {
            saveAsync();
            return val;
        }
        return 0.0;
    }

    public Map<UUID, Double> getAllBounties() {
        return bounties;
    }

    public void save() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return;

            try (PreparedStatement del = conn.prepareStatement("DELETE FROM techno_bounties;")) {
                del.executeUpdate();
            }

            boolean isRemote = plugin.getDatabaseManager().isRemote();
            String sql = isRemote
                    ? "INSERT INTO techno_bounties (target_uuid, amount) VALUES (?, ?) ON DUPLICATE KEY UPDATE amount = VALUES(amount);"
                    : "INSERT OR REPLACE INTO techno_bounties (target_uuid, amount) VALUES (?, ?);";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map.Entry<UUID, Double> entry : bounties.entrySet()) {
                    ps.setString(1, entry.getKey().toString());
                    ps.setDouble(2, entry.getValue());
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
