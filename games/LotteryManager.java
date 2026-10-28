package com.techno.economy.games;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.*;

public class LotteryManager {

    private final TechnoEconomyPlus plugin;
    private final List<UUID> tickets = new ArrayList<>();
    private BukkitTask drawTask;
    private long nextDrawTimestamp = 0;

    public LotteryManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (drawTask != null) {
            drawTask.cancel();
        }
        int intervalMinutes = plugin.getConfig().getInt("lottery.draw-interval-minutes", 60);
        long ticks = intervalMinutes * 60L * 20L;
        nextDrawTimestamp = Instant.now().getEpochSecond() + (intervalMinutes * 60L);

        drawTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            drawWinner();
            nextDrawTimestamp = Instant.now().getEpochSecond() + (intervalMinutes * 60L);
        }, ticks, ticks);
    }

    public void buyTickets(UUID uuid, int count) {
        for (int i = 0; i < count; i++) {
            tickets.add(uuid);
        }
    }

    public int getPlayerTickets(UUID uuid) {
        int c = 0;
        for (UUID u : tickets) {
            if (u.equals(uuid)) c++;
        }
        return c;
    }

    public int getTotalTickets() {
        return tickets.size();
    }

    public double getPot() {
        double price = plugin.getConfig().getDouble("lottery.ticket-price", 20.0);
        return tickets.size() * price;
    }

    public void drawWinner() {
        if (tickets.isEmpty()) {
            return;
        }

        Random rng = new Random();
        UUID winnerUuid = tickets.get(rng.nextInt(tickets.size()));
        double pot = getPot();
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");

        plugin.getBalanceManager().addBalance(winnerUuid, pot);
        Player winnerPlayer = Bukkit.getPlayer(winnerUuid);
        String winnerName = winnerPlayer != null ? winnerPlayer.getName() : winnerUuid.toString();

        plugin.getTransactionLogManager().log(winnerUuid, winnerName, "LOTTERY", pot,
                plugin.getBalanceManager().getBalance(winnerUuid), "Lottery jackpot winner");

        Bukkit.broadcast(plugin.getLanguageManager().getMessage("messages.lottery-winner-broadcast",
                Map.of("player", winnerName, "amount", String.valueOf(pot), "symbol", symbol)));

        tickets.clear();
    }

    public long getNextDrawTimestamp() {
        return nextDrawTimestamp;
    }

    public void stop() {
        if (drawTask != null) {
            drawTask.cancel();
            drawTask = null;
        }
    }
}
