package com.techno.economy.database;

import com.techno.economy.TechnoEconomyPlus;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private final TechnoEconomyPlus plugin;
    private HikariDataSource dataSource;
    private boolean isRemote = false;

    public DatabaseManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void init() {
        boolean remoteEnabled = plugin.getConfig().getBoolean("database.remote.enabled", false);

        if (remoteEnabled) {
            if (connectRemote()) {
                plugin.getServer().getConsoleSender().sendMessage("§a[TechnoEconomyPlus] Your remote database connection was success!");
                return;
            } else {
                plugin.getServer().getConsoleSender().sendMessage("§c[TechnoEconomyPlus] A problem occurred with the database. Default database system will be used.");
            }
        }

        connectLocal();
        plugin.getServer().getConsoleSender().sendMessage("§a[TechnoEconomyPlus] [database] Your server database was loaded successfully.");
    }

    private boolean connectRemote() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String host = plugin.getConfig().getString("database.remote.host", "localhost");
            int port = plugin.getConfig().getInt("database.remote.port", 3306);
            String db = plugin.getConfig().getString("database.remote.database", "technoeconomy");
            String user = plugin.getConfig().getString("database.remote.username", "root");
            String pass = plugin.getConfig().getString("database.remote.password", "");
            boolean ssl = plugin.getConfig().getBoolean("database.remote.ssl", false);

            String url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=" + ssl + "&autoReconnect=true&characterEncoding=utf8";

            HikariConfig config = new HikariConfig();
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(pass);
            config.setMaximumPoolSize(plugin.getConfig().getInt("database.remote.pool-size", 10));
            config.setMinimumIdle(plugin.getConfig().getInt("database.remote.min-idle", 2));
            config.setConnectionTimeout(10000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setPoolName("TechnoEconomy-MySQL");

            if (this.dataSource != null && !this.dataSource.isClosed()) {
                this.dataSource.close();
            }
            this.dataSource = new HikariDataSource(config);
            this.isRemote = true;
            setupRemoteTables();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void connectLocal() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (Exception ignored) {}

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        String fileName = plugin.getConfig().getString("database.local.file", "plugindatabase.sql");
        File dbFile = new File(dataFolder, fileName);

        if (this.dataSource != null && !this.dataSource.isClosed()) {
            try { this.dataSource.close(); } catch (Exception ignored) {}
        }

        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.sqlite.JDBC");
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(10000);
        config.setPoolName("TechnoEconomy-SQLite");
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("busy_timeout", "5000");

        try {
            this.dataSource = new HikariDataSource(config);
            this.isRemote = false;
            try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
                s.execute("PRAGMA journal_mode=WAL;");
                s.execute("PRAGMA busy_timeout=5000;");
            }
            setupLocalTables();
        } catch (Exception fatal) {
            renameCorruptDb(dbFile);
            try {
                this.dataSource = new HikariDataSource(config);
                this.isRemote = false;
                try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
                    s.execute("PRAGMA journal_mode=WAL;");
                    s.execute("PRAGMA busy_timeout=5000;");
                }
                setupLocalTables();
            } catch (Exception ex) {
                throw new RuntimeException("Could not initialize local SQLite database", ex);
            }
        }
    }

    private void renameCorruptDb(File dbFile) {
        if (!dbFile.exists()) return;
        File dataFolder = plugin.getDataFolder();
        int idx = 1;
        File broken = new File(dataFolder, "broken_database_" + idx + ".sql");
        while (broken.exists()) {
            idx++;
            broken = new File(dataFolder, "broken_database_" + idx + ".sql");
        }
        if (dataSource != null && !dataSource.isClosed()) {
            try { dataSource.close(); } catch (Exception ignored) {}
        }
        dbFile.renameTo(broken);
    }

    private void setupRemoteTables() throws SQLException {
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_meta (version INT PRIMARY KEY);");
            st.executeUpdate("INSERT IGNORE INTO techno_meta (version) VALUES (1);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_balances (uuid VARCHAR(36) PRIMARY KEY, player_name VARCHAR(64), balance DOUBLE NOT NULL DEFAULT 0.0);");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_banks (uuid VARCHAR(36) PRIMARY KEY, balance DOUBLE NOT NULL DEFAULT 0.0, last_interest BIGINT NOT NULL DEFAULT 0);");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_daily (uuid VARCHAR(36) PRIMARY KEY, streak INT NOT NULL DEFAULT 0, last_claim BIGINT NOT NULL DEFAULT 0);");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_bounties (target_uuid VARCHAR(36) PRIMARY KEY, amount DOUBLE NOT NULL DEFAULT 0.0);");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_jobs (player_uuid VARCHAR(36) PRIMARY KEY, job VARCHAR(64) NOT NULL DEFAULT 'none');");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_hud_settings (player_uuid VARCHAR(36) PRIMARY KEY, enabled TINYINT NOT NULL DEFAULT 1);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_chestshops (" +
                    "loc VARCHAR(128) PRIMARY KEY, " +
                    "owner_uuid VARCHAR(36) NOT NULL, " +
                    "market_name VARCHAR(128) NOT NULL DEFAULT 'Shop', " +
                    "world VARCHAR(64) NOT NULL DEFAULT 'world', " +
                    "x INT NOT NULL DEFAULT 0, " +
                    "y INT NOT NULL DEFAULT 0, " +
                    "z INT NOT NULL DEFAULT 0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_chestshop_products (" +
                    "loc VARCHAR(128) NOT NULL, " +
                    "product_id VARCHAR(36) NOT NULL, " +
                    "item_base64 MEDIUMTEXT NOT NULL, " +
                    "price DOUBLE NOT NULL DEFAULT 0.0, " +
                    "stock INT NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY (loc, product_id));");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_auctions (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "seller_uuid VARCHAR(36) NOT NULL, " +
                    "seller_name VARCHAR(64) NOT NULL, " +
                    "item_base64 MEDIUMTEXT NOT NULL, " +
                    "price DOUBLE NOT NULL DEFAULT 0.0, " +
                    "highest_bid DOUBLE NOT NULL DEFAULT 0.0, " +
                    "highest_bidder VARCHAR(36) DEFAULT NULL, " +
                    "expires_at BIGINT NOT NULL DEFAULT 0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_sirketler (" +
                    "name VARCHAR(64) PRIMARY KEY, " +
                    "owner_uuid VARCHAR(36) NOT NULL, " +
                    "owner_name VARCHAR(64) NOT NULL, " +
                    "balance DOUBLE NOT NULL DEFAULT 0.0, " +
                    "tax_debt DOUBLE NOT NULL DEFAULT 0.0, " +
                    "created_at BIGINT NOT NULL DEFAULT 0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_sirket_isciler (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "sirket_name VARCHAR(64) NOT NULL, " +
                    "worker_uuid VARCHAR(36) NOT NULL, " +
                    "worker_name VARCHAR(64) NOT NULL, " +
                    "salary DOUBLE NOT NULL DEFAULT 0.0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_borsa_hisseler (" +
                    "sirket_name VARCHAR(64) PRIMARY KEY, " +
                    "share_price DOUBLE NOT NULL DEFAULT 10.0, " +
                    "total_shares INT NOT NULL DEFAULT 1000, " +
                    "available_shares INT NOT NULL DEFAULT 1000);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_borsa_portfoy (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "sirket_name VARCHAR(64) NOT NULL, " +
                    "shares_owned INT NOT NULL DEFAULT 0, " +
                    "buy_price DOUBLE NOT NULL DEFAULT 0.0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_cekler (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "issuer_uuid VARCHAR(36) NOT NULL, " +
                    "issuer_name VARCHAR(64) NOT NULL, " +
                    "amount DOUBLE NOT NULL DEFAULT 0.0, " +
                    "created_at BIGINT NOT NULL DEFAULT 0, " +
                    "redeemed INT NOT NULL DEFAULT 0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_duellolar (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "sender_uuid VARCHAR(36) NOT NULL, " +
                    "target_uuid VARCHAR(36) NOT NULL, " +
                    "amount DOUBLE NOT NULL DEFAULT 0.0, " +
                    "duel_type VARCHAR(32) NOT NULL DEFAULT 'PVP', " +
                    "status VARCHAR(32) NOT NULL DEFAULT 'PENDING', " +
                    "winner_uuid VARCHAR(36) DEFAULT NULL, " +
                    "created_at BIGINT NOT NULL DEFAULT 0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_payday (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "last_claim BIGINT NOT NULL DEFAULT 0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_casino_stats (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "spins INT NOT NULL DEFAULT 0, " +
                    "total_won DOUBLE NOT NULL DEFAULT 0.0, " +
                    "total_lost DOUBLE NOT NULL DEFAULT 0.0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_casino_history (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "uuid VARCHAR(36) NOT NULL, " +
                    "outcome VARCHAR(32) NOT NULL, " +
                    "amount DOUBLE NOT NULL DEFAULT 0.0, " +
                    "created_at BIGINT NOT NULL DEFAULT 0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_clans (" +
                    "name VARCHAR(64) PRIMARY KEY, " +
                    "tag VARCHAR(16) NOT NULL, " +
                    "leader_uuid VARCHAR(36) NOT NULL, " +
                    "level INT NOT NULL DEFAULT 1, " +
                    "xp BIGINT NOT NULL DEFAULT 0, " +
                    "bank_balance DOUBLE NOT NULL DEFAULT 0.0, " +
                    "home_world VARCHAR(64) DEFAULT NULL, " +
                    "home_x DOUBLE DEFAULT 0.0, " +
                    "home_y DOUBLE DEFAULT 0.0, " +
                    "home_z DOUBLE DEFAULT 0.0, " +
                    "home_yaw FLOAT DEFAULT 0.0, " +
                    "home_pitch FLOAT DEFAULT 0.0, " +
                    "pvp_friendly_fire INT NOT NULL DEFAULT 0, " +
                    "kills BIGINT NOT NULL DEFAULT 0, " +
                    "deaths BIGINT NOT NULL DEFAULT 0, " +
                    "war_wins INT NOT NULL DEFAULT 0, " +
                    "war_losses INT NOT NULL DEFAULT 0, " +
                    "created_at BIGINT NOT NULL DEFAULT 0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_clan_members (" +
                    "clan_name VARCHAR(64) NOT NULL, " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "player_name VARCHAR(64) NOT NULL, " +
                    "rank VARCHAR(32) NOT NULL DEFAULT 'RECRUIT', " +
                    "joined_at BIGINT NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY (clan_name, player_uuid));");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_clan_allies (" +
                    "clan_name VARCHAR(64) NOT NULL, " +
                    "ally_name VARCHAR(64) NOT NULL, " +
                    "PRIMARY KEY (clan_name, ally_name));");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_clan_enemies (" +
                    "clan_name VARCHAR(64) NOT NULL, " +
                    "enemy_name VARCHAR(64) NOT NULL, " +
                    "PRIMARY KEY (clan_name, enemy_name));");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_clan_bases (" +
                    "clan_name VARCHAR(64) NOT NULL, " +
                    "base_name VARCHAR(64) NOT NULL, " +
                    "world VARCHAR(64) NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL, " +
                    "yaw FLOAT NOT NULL, " +
                    "pitch FLOAT NOT NULL, " +
                    "PRIMARY KEY (clan_name, base_name));");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_clan_bank_logs (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "clan_name VARCHAR(64) NOT NULL, " +
                    "player_uuid VARCHAR(36) NOT NULL, " +
                    "player_name VARCHAR(64) NOT NULL, " +
                    "action VARCHAR(32) NOT NULL, " +
                    "amount DOUBLE NOT NULL, " +
                    "timestamp BIGINT NOT NULL);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_clan_wars (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "challenger_clan VARCHAR(64) NOT NULL, " +
                    "target_clan VARCHAR(64) NOT NULL, " +
                    "start_time BIGINT NOT NULL, " +
                    "end_time BIGINT NOT NULL, " +
                    "challenger_kills INT NOT NULL DEFAULT 0, " +
                    "target_kills INT NOT NULL DEFAULT 0, " +
                    "winner_clan VARCHAR(64) DEFAULT NULL, " +
                    "status VARCHAR(32) NOT NULL DEFAULT 'PENDING');");
        }
    }

    private void setupLocalTables() throws SQLException {
        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_meta (version INTEGER PRIMARY KEY);");
            st.executeUpdate("INSERT OR IGNORE INTO techno_meta (version) VALUES (1);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_balances (uuid TEXT PRIMARY KEY, player_name TEXT, balance REAL NOT NULL DEFAULT 0.0);");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_banks (uuid TEXT PRIMARY KEY, balance REAL NOT NULL DEFAULT 0.0, last_interest INTEGER NOT NULL DEFAULT 0);");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_daily (uuid TEXT PRIMARY KEY, streak INTEGER NOT NULL DEFAULT 0, last_claim INTEGER NOT NULL DEFAULT 0);");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_bounties (target_uuid TEXT PRIMARY KEY, amount REAL NOT NULL DEFAULT 0.0);");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_jobs (player_uuid TEXT PRIMARY KEY, job TEXT NOT NULL DEFAULT 'none');");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_hud_settings (player_uuid TEXT PRIMARY KEY, enabled INTEGER NOT NULL DEFAULT 1);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_chestshops (" +
                    "loc TEXT PRIMARY KEY, " +
                    "owner_uuid TEXT NOT NULL, " +
                    "market_name TEXT NOT NULL DEFAULT 'Shop', " +
                    "world TEXT NOT NULL DEFAULT 'world', " +
                    "x INTEGER NOT NULL DEFAULT 0, " +
                    "y INTEGER NOT NULL DEFAULT 0, " +
                    "z INTEGER NOT NULL DEFAULT 0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_chestshop_products (" +
                    "loc TEXT NOT NULL, " +
                    "product_id TEXT NOT NULL, " +
                    "item_base64 TEXT NOT NULL, " +
                    "price REAL NOT NULL DEFAULT 0.0, " +
                    "stock INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY (loc, product_id));");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_auctions (" +
                    "id TEXT PRIMARY KEY, " +
                    "seller_uuid TEXT NOT NULL, " +
                    "seller_name TEXT NOT NULL, " +
                    "item_base64 TEXT NOT NULL, " +
                    "price REAL NOT NULL DEFAULT 0.0, " +
                    "highest_bid REAL NOT NULL DEFAULT 0.0, " +
                    "highest_bidder TEXT DEFAULT NULL, " +
                    "expires_at INTEGER NOT NULL DEFAULT 0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_sirketler (" +
                    "name TEXT PRIMARY KEY, " +
                    "owner_uuid TEXT NOT NULL, " +
                    "owner_name TEXT NOT NULL, " +
                    "balance REAL NOT NULL DEFAULT 0.0, " +
                    "tax_debt REAL NOT NULL DEFAULT 0.0, " +
                    "created_at INTEGER NOT NULL DEFAULT 0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_sirket_isciler (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "sirket_name TEXT NOT NULL, " +
                    "worker_uuid TEXT NOT NULL, " +
                    "worker_name TEXT NOT NULL, " +
                    "salary REAL NOT NULL DEFAULT 0.0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_borsa_hisseler (" +
                    "sirket_name TEXT PRIMARY KEY, " +
                    "share_price REAL NOT NULL DEFAULT 10.0, " +
                    "total_shares INTEGER NOT NULL DEFAULT 1000, " +
                    "available_shares INTEGER NOT NULL DEFAULT 1000);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_borsa_portfoy (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "player_uuid TEXT NOT NULL, " +
                    "sirket_name TEXT NOT NULL, " +
                    "shares_owned INTEGER NOT NULL DEFAULT 0, " +
                    "buy_price REAL NOT NULL DEFAULT 0.0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_cekler (" +
                    "id TEXT PRIMARY KEY, " +
                    "issuer_uuid TEXT NOT NULL, " +
                    "issuer_name TEXT NOT NULL, " +
                    "amount REAL NOT NULL DEFAULT 0.0, " +
                    "created_at INTEGER NOT NULL DEFAULT 0, " +
                    "redeemed INTEGER NOT NULL DEFAULT 0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_duellolar (" +
                    "id TEXT PRIMARY KEY, " +
                    "sender_uuid TEXT NOT NULL, " +
                    "target_uuid TEXT NOT NULL, " +
                    "amount REAL NOT NULL DEFAULT 0.0, " +
                    "duel_type TEXT NOT NULL DEFAULT 'PVP', " +
                    "status TEXT NOT NULL DEFAULT 'PENDING', " +
                    "winner_uuid TEXT DEFAULT NULL, " +
                    "created_at INTEGER NOT NULL DEFAULT 0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_payday (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "last_claim INTEGER NOT NULL DEFAULT 0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_casino_stats (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "spins INTEGER NOT NULL DEFAULT 0, " +
                    "total_won REAL NOT NULL DEFAULT 0.0, " +
                    "total_lost REAL NOT NULL DEFAULT 0.0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_casino_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "uuid TEXT NOT NULL, " +
                    "outcome TEXT NOT NULL, " +
                    "amount REAL NOT NULL DEFAULT 0.0, " +
                    "created_at INTEGER NOT NULL DEFAULT 0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_clans (" +
                    "name TEXT PRIMARY KEY, " +
                    "tag TEXT NOT NULL, " +
                    "leader_uuid TEXT NOT NULL, " +
                    "level INTEGER NOT NULL DEFAULT 1, " +
                    "xp INTEGER NOT NULL DEFAULT 0, " +
                    "bank_balance REAL NOT NULL DEFAULT 0.0, " +
                    "home_world TEXT DEFAULT NULL, " +
                    "home_x REAL DEFAULT 0.0, " +
                    "home_y REAL DEFAULT 0.0, " +
                    "home_z REAL DEFAULT 0.0, " +
                    "home_yaw REAL DEFAULT 0.0, " +
                    "home_pitch REAL DEFAULT 0.0, " +
                    "pvp_friendly_fire INTEGER NOT NULL DEFAULT 0, " +
                    "kills INTEGER NOT NULL DEFAULT 0, " +
                    "deaths INTEGER NOT NULL DEFAULT 0, " +
                    "war_wins INTEGER NOT NULL DEFAULT 0, " +
                    "war_losses INTEGER NOT NULL DEFAULT 0, " +
                    "created_at INTEGER NOT NULL DEFAULT 0);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_clan_members (" +
                    "clan_name TEXT NOT NULL, " +
                    "player_uuid TEXT NOT NULL, " +
                    "player_name TEXT NOT NULL, " +
                    "rank TEXT NOT NULL DEFAULT 'RECRUIT', " +
                    "joined_at INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY (clan_name, player_uuid));");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_clan_allies (" +
                    "clan_name TEXT NOT NULL, " +
                    "ally_name TEXT NOT NULL, " +
                    "PRIMARY KEY (clan_name, ally_name));");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_clan_enemies (" +
                    "clan_name TEXT NOT NULL, " +
                    "enemy_name TEXT NOT NULL, " +
                    "PRIMARY KEY (clan_name, enemy_name));");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_clan_bases (" +
                    "clan_name TEXT NOT NULL, " +
                    "base_name TEXT NOT NULL, " +
                    "world TEXT NOT NULL, " +
                    "x REAL NOT NULL, " +
                    "y REAL NOT NULL, " +
                    "z REAL NOT NULL, " +
                    "yaw REAL NOT NULL, " +
                    "pitch REAL NOT NULL, " +
                    "PRIMARY KEY (clan_name, base_name));");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_clan_bank_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "clan_name TEXT NOT NULL, " +
                    "player_uuid TEXT NOT NULL, " +
                    "player_name TEXT NOT NULL, " +
                    "action TEXT NOT NULL, " +
                    "amount REAL NOT NULL, " +
                    "timestamp INTEGER NOT NULL);");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS techno_clan_wars (" +
                    "id TEXT PRIMARY KEY, " +
                    "challenger_clan TEXT NOT NULL, " +
                    "target_clan TEXT NOT NULL, " +
                    "start_time INTEGER NOT NULL, " +
                    "end_time INTEGER NOT NULL, " +
                    "challenger_kills INTEGER NOT NULL DEFAULT 0, " +
                    "target_kills INTEGER NOT NULL DEFAULT 0, " +
                    "winner_clan TEXT DEFAULT NULL, " +
                    "status TEXT NOT NULL DEFAULT 'PENDING');");
        }
    }

    public Connection getConnection() {
        if (dataSource == null || dataSource.isClosed()) {
            return null;
        }
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to obtain connection from pool: " + e.getMessage());
            return null;
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            try {
                dataSource.close();
            } catch (Exception ignored) {}
        }
    }

    public boolean isRemote() {
        return isRemote;
    }

    public static class CasinoStats {
        public final int spins;
        public final double totalWon;
        public final double totalLost;

        public CasinoStats(int spins, double totalWon, double totalLost) {
            this.spins = spins;
            this.totalWon = totalWon;
            this.totalLost = totalLost;
        }
    }

    public static class CasinoHistoryEntry {
        public final String outcome;
        public final double amount;
        public final long timestamp;

        public CasinoHistoryEntry(String outcome, double amount, long timestamp) {
            this.outcome = outcome;
            this.amount = amount;
            this.timestamp = timestamp;
        }
    }

    public long getLastPaydayClaim(UUID uuid) {
        try (Connection conn = getConnection()) {
            if (conn == null) return 0;
            try (PreparedStatement ps = conn.prepareStatement("SELECT last_claim FROM techno_payday WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getLong("last_claim");
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public void setLastPaydayClaim(UUID uuid, long timestamp) {
        try (Connection conn = getConnection()) {
            if (conn == null) return;
            if (isRemote) {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO techno_payday (uuid, last_claim) VALUES (?, ?) ON DUPLICATE KEY UPDATE last_claim = VALUES(last_claim)")) {
                    ps.setString(1, uuid.toString());
                    ps.setLong(2, timestamp);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO techno_payday (uuid, last_claim) VALUES (?, ?)")) {
                    ps.setString(1, uuid.toString());
                    ps.setLong(2, timestamp);
                    ps.executeUpdate();
                }
            }
        } catch (Exception ignored) {}
    }

    public CasinoStats getCasinoStats(UUID uuid) {
        try (Connection conn = getConnection()) {
            if (conn == null) return new CasinoStats(0, 0.0, 0.0);
            try (PreparedStatement ps = conn.prepareStatement("SELECT spins, total_won, total_lost FROM techno_casino_stats WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new CasinoStats(rs.getInt("spins"), rs.getDouble("total_won"), rs.getDouble("total_lost"));
                    }
                }
            }
        } catch (Exception ignored) {}
        return new CasinoStats(0, 0.0, 0.0);
    }

    public void recordCasinoGame(UUID uuid, boolean won, double amount, String outcome) {
        try (Connection conn = getConnection()) {
            if (conn == null) return;
            if (isRemote) {
                String sql = won
                        ? "INSERT INTO techno_casino_stats (uuid, spins, total_won, total_lost) VALUES (?, 1, ?, 0.0) ON DUPLICATE KEY UPDATE spins = spins + 1, total_won = total_won + ?"
                        : "INSERT INTO techno_casino_stats (uuid, spins, total_won, total_lost) VALUES (?, 1, 0.0, ?) ON DUPLICATE KEY UPDATE spins = spins + 1, total_lost = total_lost + ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setDouble(2, amount);
                    ps.setDouble(3, amount);
                    ps.executeUpdate();
                }
            } else {
                String sql = won
                        ? "INSERT INTO techno_casino_stats (uuid, spins, total_won, total_lost) VALUES (?, 1, ?, 0.0) ON CONFLICT(uuid) DO UPDATE SET spins = spins + 1, total_won = total_won + excluded.total_won;"
                        : "INSERT INTO techno_casino_stats (uuid, spins, total_won, total_lost) VALUES (?, 1, 0.0, ?) ON CONFLICT(uuid) DO UPDATE SET spins = spins + 1, total_lost = total_lost + excluded.total_lost;";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setDouble(2, amount);
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO techno_casino_history (uuid, outcome, amount, created_at) VALUES (?, ?, ?, ?);")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, outcome);
                ps.setDouble(3, amount);
                ps.setLong(4, System.currentTimeMillis());
                ps.executeUpdate();
            }
        } catch (Exception ignored) {}
    }

    public List<CasinoHistoryEntry> getCasinoHistory(UUID uuid, int limit) {
        List<CasinoHistoryEntry> list = new ArrayList<>();
        try (Connection conn = getConnection()) {
            if (conn == null) return list;
            try (PreparedStatement ps = conn.prepareStatement("SELECT outcome, amount, created_at FROM techno_casino_history WHERE uuid = ? ORDER BY id DESC LIMIT ?")) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new CasinoHistoryEntry(rs.getString("outcome"), rs.getDouble("amount"), rs.getLong("created_at")));
                    }
                }
            }
        } catch (Exception ignored) {}
        return list;
    }
}
