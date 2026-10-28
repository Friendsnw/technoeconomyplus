package com.techno.economy.logging;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TransactionLogManager {

    private final TechnoEconomyPlus plugin;
    private final File logsFolder;
    private final ConcurrentHashMap<UUID, List<String>> memoryHistory = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private BukkitTask flushTask;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TransactionLogManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
        this.logsFolder = new File(plugin.getDataFolder(), "logs");
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }
        startFlushTask();
    }

    private void startFlushTask() {
        flushTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::flushLogs, 20L, 20L);
    }

    public void log(UUID uuid, String playerName, String type, double amount, double finalBalance, String details) {
        String nowStr = LocalDateTime.now().format(TIME_FORMAT);
        String logLine = String.format("[%s] [%s] %s (%s): %s%.2f -> Balance: %.2f | %s",
                nowStr, type, playerName, uuid.toString(), (amount >= 0 ? "+" : ""), amount, finalBalance, details);

        List<String> list = memoryHistory.computeIfAbsent(uuid, k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            list.add(0, logLine);
            if (list.size() > 50) {
                list.remove(list.size() - 1);
            }
        }

        logQueue.add(logLine);
    }

    public void log(UUID uuid, String type, double amount, String details) {
        double balance = plugin.getBalanceManager().getBalance(uuid);
        Player online = plugin.getServer().getPlayer(uuid);
        String playerName = online != null ? online.getName() : uuid.toString().substring(0, 8);
        log(uuid, playerName, type, amount, balance, details);
    }

    public synchronized void flushLogs() {
        if (logQueue.isEmpty()) return;
        List<String> batch = new ArrayList<>();
        String line;
        while ((line = logQueue.poll()) != null) {
            batch.add(line);
        }
        if (batch.isEmpty()) return;

        String todayFile = "transactions-" + LocalDate.now().format(DATE_FORMAT) + ".log";
        File target = new File(logsFolder, todayFile);
        try (FileWriter fw = new FileWriter(target, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            for (String l : batch) {
                bw.write(l);
                bw.newLine();
            }
            bw.flush();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write transaction logs: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }
        flushLogs();
    }

    public List<String> getHistory(UUID uuid) {
        List<String> list = memoryHistory.get(uuid);
        if (list == null) return new ArrayList<>();
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }
}
