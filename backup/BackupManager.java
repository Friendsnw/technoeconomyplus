package com.techno.economy.backup;

import com.techno.economy.TechnoEconomyPlus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;

public class BackupManager {

    private final TechnoEconomyPlus plugin;
    private final File backupDir;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final int MAX_BACKUPS = 10;

    public BackupManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
        this.backupDir = new File(plugin.getDataFolder(), "backups");
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
    }

    public void prepareBalancesFileOnStartup() {
    }

    public boolean backupDatabase() {
        String dbName = plugin.getConfig().getString("database.local.file", "plugindatabase.sql");
        File source = new File(plugin.getDataFolder(), dbName);
        if (!source.exists()) return true;
        return backupFile(source, "database");
    }

    public boolean backupAll() {
        boolean ok = backupDatabase();
        ok &= backupFile(new File(plugin.getDataFolder(), "config.yml"), "config");

        File langDir = new File(plugin.getDataFolder(), "languages");
        if (langDir.exists()) {
            File[] langFiles = langDir.listFiles((d, n) -> n.endsWith(".yml"));
            if (langFiles != null) {
                for (File lf : langFiles) {
                    ok &= backupFile(lf, "lang-" + lf.getName().replace(".yml", ""));
                }
            }
        }

        File menuDir = new File(plugin.getDataFolder(), "menus");
        if (menuDir.exists()) {
            File[] menuFiles = menuDir.listFiles((d, n) -> n.endsWith(".yml"));
            if (menuFiles != null) {
                for (File mf : menuFiles) {
                    ok &= backupFile(mf, "menu-" + mf.getName().replace(".yml", ""));
                }
            }
        }

        File settingsDir = new File(plugin.getDataFolder(), "settings");
        if (settingsDir.exists()) {
            File[] setFiles = settingsDir.listFiles((d, n) -> n.endsWith(".yml"));
            if (setFiles != null) {
                for (File sf : setFiles) {
                    ok &= backupFile(sf, "settings-" + sf.getName().replace(".yml", ""));
                }
            }
        }

        pruneOldBackups();
        return ok;
    }

    private boolean backupFile(File source, String prefix) {
        if (!source.exists()) return true;
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String ext = source.getName().endsWith(".sql") ? ".sql" : ".yml";
        File dest = new File(backupDir, prefix + "-" + timestamp + ext);
        try {
            Files.copy(source.toPath(), dest.toPath());
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Backup failed for " + source.getName() + ": " + e.getMessage());
            return false;
        }
    }

    public void pruneOldBackups() {
        for (String prefix : new String[]{"database", "config", "lang-", "menu-"}) {
            pruneByPrefix(prefix);
        }
    }

    private void pruneByPrefix(String prefix) {
        File[] files = backupDir.listFiles((d, n) -> n.startsWith(prefix));
        if (files == null || files.length <= MAX_BACKUPS) return;
        Arrays.sort(files, Comparator.comparing(File::lastModified));
        for (int i = 0; i < files.length - MAX_BACKUPS; i++) {
            files[i].delete();
        }
    }
}
