package com.techno.economy.balance;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BalanceManager {

    private final TechnoEconomyPlus plugin;
    private final ConcurrentHashMap<UUID, Double> balances = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private BukkitTask autoSaveTask;
    private BukkitTask baltopTask;
    private volatile List<Map.Entry<UUID, Double>> cachedBaltop = new ArrayList<>();
    private volatile double startingBalance = 100.0;

    public BalanceManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        balances.clear();
        dirty.clear();
        startingBalance = plugin.getConfig().getDouble("economy.starting-balance", 100.0);

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn != null) {
                try (PreparedStatement ps = conn.prepareStatement("SELECT uuid, balance FROM techno_balances;");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            UUID uuid = UUID.fromString(rs.getString("uuid"));
                            balances.put(uuid, rs.getDouble("balance"));
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
        refreshBaltop();
    }

    public void scheduleAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        long ticks = 60L * 20L;
        autoSaveTask = plugin.getServer().getScheduler()
                .runTaskTimerAsynchronously(plugin, () -> {
                    saveDirty();
                    refreshBaltop();
                }, ticks, ticks);

        if (baltopTask != null) {
            baltopTask.cancel();
        }
        baltopTask = plugin.getServer().getScheduler()
                .runTaskTimerAsynchronously(plugin, this::refreshBaltop, 20L * 60L, 20L * 60L);
    }

    public void refreshBaltop() {
        List<Map.Entry<UUID, Double>> list = new ArrayList<>(balances.entrySet());
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        cachedBaltop = Collections.unmodifiableList(list);
    }

    public List<Map.Entry<UUID, Double>> getCachedBaltop() {
        return cachedBaltop;
    }

    public void saveDirty() {
        if (dirty.isEmpty()) return;
        Set<UUID> toSave = new HashSet<>(dirty);
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return;
            boolean isRemote = plugin.getDatabaseManager().isRemote();
            String sql = isRemote
                    ? "INSERT INTO techno_balances (uuid, balance) VALUES (?, ?) ON DUPLICATE KEY UPDATE balance = VALUES(balance);"
                    : "INSERT OR REPLACE INTO techno_balances (uuid, balance) VALUES (?, ?);";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (UUID u : toSave) {
                    Double val = balances.get(u);
                    if (val == null) continue;
                    ps.setString(1, u.toString());
                    ps.setDouble(2, val);
                    ps.addBatch();
                }
                ps.executeBatch();
                dirty.removeAll(toSave);
            }
        } catch (Exception ignored) {}
    }

    public void save() {
        saveDirty();
    }

    public void saveAsync() {
        if (!plugin.isEnabled()) {
            saveDirty();
            refreshBaltop();
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            saveDirty();
            refreshBaltop();
        });
    }

    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        if (baltopTask != null) {
            baltopTask.cancel();
            baltopTask = null;
        }
        saveDirty();
    }

    public double getBalance(UUID uuid) {
        return balances.computeIfAbsent(uuid, k -> startingBalance);
    }

    public void setBalance(UUID uuid, double amount) {
        double finalAmount = Math.max(0, amount);
        balances.put(uuid, finalAmount);
        dirty.add(uuid);
    }

    public void addBalance(UUID uuid, double amount) {
        balances.merge(uuid, amount, Double::sum);
        dirty.add(uuid);
    }

    public boolean removeBalance(UUID uuid, double amount) {
        double current = getBalance(uuid);
        if (current < amount) {
            return false;
        }
        balances.put(uuid, current - amount);
        dirty.add(uuid);
        return true;
    }

    public boolean has(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }

    public boolean withdraw(UUID uuid, double amount) {
        return removeBalance(uuid, amount);
    }

    public void deposit(UUID uuid, double amount) {
        addBalance(uuid, amount);
    }

    public ConcurrentHashMap<UUID, Double> getAllBalances() {
        return balances;
    }
}
