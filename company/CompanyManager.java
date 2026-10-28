package com.techno.economy.company;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CompanyManager {

    private final TechnoEconomyPlus plugin;
    private BukkitTask taxTask;
    private volatile double taxRate = 0.05;
    private volatile double taxPenalty = 500.0;
    private volatile double creationFee = 5000.0;

    public CompanyManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void start() {
        taxRate = plugin.getConfig().getDouble("sirket.tax-rate", 0.05);
        taxPenalty = plugin.getConfig().getDouble("sirket.tax-penalty", 500.0);
        creationFee = plugin.getConfig().getDouble("sirket.creation-fee", 5000.0);

        int intervalMinutes = plugin.getConfig().getInt("sirket.tax-interval-minutes", 60);
        taxTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::processTaxes, intervalMinutes * 60 * 20L, intervalMinutes * 60 * 20L);
    }

    public void stop() {
        if (taxTask != null) taxTask.cancel();
    }

    public boolean createCompany(UUID ownerUuid, String ownerName, String name) {
        if (name == null || name.length() < 3 || name.length() > 32) return false;
        if (!plugin.getBalanceManager().has(ownerUuid, creationFee)) return false;

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return false;

            try (PreparedStatement ps = conn.prepareStatement("SELECT name FROM techno_sirketler WHERE owner_uuid = ? LIMIT 1;")) {
                ps.setString(1, ownerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return false;
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO techno_sirketler (name, owner_uuid, owner_name, balance, tax_debt, created_at) VALUES (?, ?, ?, 0.0, 0.0, ?);")) {
                ps.setString(1, name);
                ps.setString(2, ownerUuid.toString());
                ps.setString(3, ownerName);
                ps.setLong(4, System.currentTimeMillis());
                ps.executeUpdate();
            }

            plugin.getBalanceManager().withdraw(ownerUuid, creationFee);
            plugin.getTransactionLogManager().log(ownerUuid, "COMPANY_CREATE", creationFee, "Created company: " + name);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public CompanyData getCompany(String name) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return null;

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM techno_sirketler WHERE name = ? LIMIT 1;")) {
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new CompanyData(
                                rs.getString("name"),
                                UUID.fromString(rs.getString("owner_uuid")),
                                rs.getString("owner_name"),
                                rs.getDouble("balance"),
                                rs.getDouble("tax_debt"),
                                rs.getLong("created_at")
                        );
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public CompanyData getPlayerCompany(UUID playerUuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return null;

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM techno_sirketler WHERE owner_uuid = ? LIMIT 1;")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new CompanyData(
                                rs.getString("name"),
                                UUID.fromString(rs.getString("owner_uuid")),
                                rs.getString("owner_name"),
                                rs.getDouble("balance"),
                                rs.getDouble("tax_debt"),
                                rs.getLong("created_at")
                        );
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public boolean addWorker(String companyName, UUID workerUuid, String workerName, double salary) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return false;

            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO techno_sirket_isciler (sirket_name, worker_uuid, worker_name, salary) VALUES (?, ?, ?, ?);")) {
                ps.setString(1, companyName);
                ps.setString(2, workerUuid.toString());
                ps.setString(3, workerName);
                ps.setDouble(4, salary);
                ps.executeUpdate();
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean removeWorker(String companyName, UUID workerUuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return false;

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM techno_sirket_isciler WHERE sirket_name = ? AND worker_uuid = ?;")) {
                ps.setString(1, companyName);
                ps.setString(2, workerUuid.toString());
                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public List<WorkerData> getWorkers(String companyName) {
        List<WorkerData> list = new ArrayList<>();
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return list;

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM techno_sirket_isciler WHERE sirket_name = ?;")) {
                ps.setString(1, companyName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new WorkerData(
                                rs.getInt("id"),
                                rs.getString("sirket_name"),
                                UUID.fromString(rs.getString("worker_uuid")),
                                rs.getString("worker_name"),
                                rs.getDouble("salary")
                        ));
                    }
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    public boolean deposit(String companyName, double amount) {
        if (amount <= 0) return false;
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return false;

            try (PreparedStatement ps = conn.prepareStatement("UPDATE techno_sirketler SET balance = balance + ? WHERE name = ?;")) {
                ps.setDouble(1, amount);
                ps.setString(2, companyName);
                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean withdraw(String companyName, double amount) {
        if (amount <= 0) return false;
        CompanyData data = getCompany(companyName);
        if (data == null || data.getBalance() < amount) return false;

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return false;

            try (PreparedStatement ps = conn.prepareStatement("UPDATE techno_sirketler SET balance = balance - ? WHERE name = ?;")) {
                ps.setDouble(1, amount);
                ps.setString(2, companyName);
                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public boolean payTaxes(String companyName, double amount) {
        if (amount <= 0) return false;
        CompanyData data = getCompany(companyName);
        if (data == null || data.getBalance() < amount) return false;

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return false;

            try (PreparedStatement ps = conn.prepareStatement("UPDATE techno_sirketler SET balance = balance - ?, tax_debt = MAX(0.0, tax_debt - ?) WHERE name = ?;")) {
                ps.setDouble(1, amount);
                ps.setDouble(2, amount);
                ps.setString(3, companyName);
                return ps.executeUpdate() > 0;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public double getMarketCap(String companyName) {
        CompanyData data = getCompany(companyName);
        if (data == null) return 0.0;
        int workerCount = getWorkers(companyName).size();
        return data.getBalance() + (workerCount * 500.0);
    }

    private void processTaxes() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return;

            try (PreparedStatement ps = conn.prepareStatement("SELECT name, balance, tax_debt FROM techno_sirketler;");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    double bal = rs.getDouble("balance");
                    double existingDebt = rs.getDouble("tax_debt");

                    double currentTax = Math.round(bal * taxRate * 100.0) / 100.0;
                    double newDebt = existingDebt + currentTax;
                    if (existingDebt > 0) {
                        newDebt += penaltyDebt();
                    }

                    try (PreparedStatement ups = conn.prepareStatement("UPDATE techno_sirketler SET tax_debt = ? WHERE name = ?;")) {
                        ups.setDouble(1, newDebt);
                        ups.setString(2, name);
                        ups.executeUpdate();
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private double penaltyDebt() {
        return taxPenalty;
    }

    public static class CompanyData {
        private final String name;
        private final UUID ownerUuid;
        private final String ownerName;
        private final double balance;
        private final double taxDebt;
        private final long createdAt;

        public CompanyData(String name, UUID ownerUuid, String ownerName, double balance, double taxDebt, long createdAt) {
            this.name = name;
            this.ownerUuid = ownerUuid;
            this.ownerName = ownerName;
            this.balance = balance;
            this.taxDebt = taxDebt;
            this.createdAt = createdAt;
        }

        public String getName() { return name; }
        public UUID getOwnerUuid() { return ownerUuid; }
        public String getOwnerName() { return ownerName; }
        public double getBalance() { return balance; }
        public double getTaxDebt() { return taxDebt; }
        public long getCreatedAt() { return createdAt; }
    }

    public static class WorkerData {
        private final int id;
        private final String companyName;
        private final UUID workerUuid;
        private final String workerName;
        private final double salary;

        public WorkerData(int id, String companyName, UUID workerUuid, String workerName, double salary) {
            this.id = id;
            this.companyName = companyName;
            this.workerUuid = workerUuid;
            this.workerName = workerName;
            this.salary = salary;
        }

        public int getId() { return id; }
        public String getCompanyName() { return companyName; }
        public UUID getWorkerUuid() { return workerUuid; }
        public String getWorkerName() { return workerName; }
        public double getSalary() { return salary; }
    }
}
