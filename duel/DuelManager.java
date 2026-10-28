package com.techno.economy.duel;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DuelManager {

    private final TechnoEconomyPlus plugin;
    private final Map<UUID, DuelOffer> pendingOffers = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> activePvPDuels = new ConcurrentHashMap<>();
    private final Map<UUID, Double> activePvPWagers = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public DuelManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public boolean sendOffer(Player sender, Player target, double amount, String type) {
        if (sender.equals(target)) return false;
        if (amount <= 0) return false;
        if (!plugin.getBalanceManager().has(sender.getUniqueId(), amount)) return false;
        if (!plugin.getBalanceManager().has(target.getUniqueId(), amount)) return false;

        UUID offerId = UUID.randomUUID();
        long now = System.currentTimeMillis();
        DuelOffer offer = new DuelOffer(offerId, sender.getUniqueId(), target.getUniqueId(), amount, type, now);
        pendingOffers.put(target.getUniqueId(), offer);

        target.sendMessage(plugin.getLanguageManager().getMessage("messages.duel-offer-header", Map.of("player", sender.getName())));
        target.sendMessage(plugin.getLanguageManager().getMessage("messages.duel-offer-wager", Map.of("amount", String.valueOf(amount))));
        target.sendMessage(plugin.getLanguageManager().getMessage("messages.duel-offer-type", Map.of("type", type)));
        target.sendMessage(plugin.getLanguageManager().getMessage("messages.duel-offer-accept-hint", Map.of("player", sender.getName())));

        return true;
    }

    public boolean acceptOffer(Player target, Player sender) {
        DuelOffer offer = pendingOffers.get(target.getUniqueId());
        if (offer == null || !offer.getSenderUuid().equals(sender.getUniqueId())) return false;

        long timeout = plugin.getConfig().getInt("duello.timeout-seconds", 60) * 1000L;
        if (System.currentTimeMillis() - offer.getCreatedAt() > timeout) {
            pendingOffers.remove(target.getUniqueId());
            return false;
        }

        double amount = offer.getAmount();
        if (!plugin.getBalanceManager().has(sender.getUniqueId(), amount)) return false;
        if (!plugin.getBalanceManager().has(target.getUniqueId(), amount)) return false;

        plugin.getBalanceManager().withdraw(sender.getUniqueId(), amount);
        plugin.getBalanceManager().withdraw(target.getUniqueId(), amount);

        pendingOffers.remove(target.getUniqueId());

        if (offer.getType().equalsIgnoreCase("COINFLIP")) {
            boolean senderWins = random.nextBoolean();
            Player winner = senderWins ? sender : target;
            Player loser = senderWins ? target : sender;
            resolveWinner(winner, loser, amount, "COINFLIP");
            return true;
        }

        activePvPDuels.put(sender.getUniqueId(), target.getUniqueId());
        activePvPDuels.put(target.getUniqueId(), sender.getUniqueId());
        activePvPWagers.put(sender.getUniqueId(), amount);
        activePvPWagers.put(target.getUniqueId(), amount);

        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.duel-started"));
        target.sendMessage(plugin.getLanguageManager().getMessage("messages.duel-started"));
        sender.playSound(sender.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        target.playSound(target.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);

        return true;
    }

    public void handlePvPEnd(Player loser, String reason) {
        UUID loserUuid = loser.getUniqueId();
        UUID winnerUuid = activePvPDuels.remove(loserUuid);
        if (winnerUuid == null) return;
        activePvPDuels.remove(winnerUuid);

        Double amount = activePvPWagers.remove(loserUuid);
        activePvPWagers.remove(winnerUuid);
        if (amount == null) amount = 100.0;

        Player winner = Bukkit.getPlayer(winnerUuid);
        if (winner != null && winner.isOnline()) {
            resolveWinner(winner, loser, amount, "PVP");
        }
    }

    private void resolveWinner(Player winner, Player loser, double singleWager, String type) {
        double pot = singleWager * 2.0;
        double commPercent = plugin.getConfig().getDouble("duello.commission-percent", 2.0);
        double fee = Math.round((pot * (commPercent / 100.0)) * 100.0) / 100.0;
        double prize = pot - fee;

        plugin.getBalanceManager().deposit(winner.getUniqueId(), prize);
        plugin.getTransactionLogManager().log(winner.getUniqueId(), "DUEL_WIN", prize, "Opponent: " + loser.getName() + " Type: " + type);

        UUID winUuid = winner.getUniqueId();
        UUID loseUuid = loser.getUniqueId();
        long now = System.currentTimeMillis();

        Runnable insertTask = () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                if (conn != null) {
                    try (PreparedStatement ps = conn.prepareStatement("INSERT INTO techno_duellolar (id, sender_uuid, target_uuid, amount, duel_type, status, winner_uuid, created_at) VALUES (?, ?, ?, ?, ?, 'FINISHED', ?, ?);")) {
                        ps.setString(1, UUID.randomUUID().toString());
                        ps.setString(2, winUuid.toString());
                        ps.setString(3, loseUuid.toString());
                        ps.setDouble(4, singleWager);
                        ps.setString(5, type);
                        ps.setString(6, winUuid.toString());
                        ps.setLong(7, now);
                        ps.executeUpdate();
                    }
                }
            } catch (Exception ignored) {}
        };

        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, insertTask);
        } else {
            insertTask.run();
        }

        winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        winner.sendMessage(plugin.getLanguageManager().getMessage("messages.duel-won", Map.of("prize", String.valueOf(prize))));
        loser.sendMessage(plugin.getLanguageManager().getMessage("messages.duel-lost", Map.of("amount", String.valueOf(singleWager))));
    }

    public boolean isInDuel(UUID playerUuid) {
        return activePvPDuels.containsKey(playerUuid);
    }

    public int getWins(UUID playerUuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return 0;

            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS cnt FROM techno_duellolar WHERE winner_uuid = ?;")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("cnt");
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public int getLosses(UUID playerUuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return 0;

            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS cnt FROM techno_duellolar WHERE (sender_uuid = ? OR target_uuid = ?) AND winner_uuid != ?;")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, playerUuid.toString());
                ps.setString(3, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt("cnt");
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public double getTotalWinnings(UUID playerUuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return 0.0;

            try (PreparedStatement ps = conn.prepareStatement("SELECT SUM(amount) AS total FROM techno_duellolar WHERE winner_uuid = ?;")) {
                ps.setString(1, playerUuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return Math.round(rs.getDouble("total") * 100.0) / 100.0;
                }
            }
        } catch (Exception ignored) {}
        return 0.0;
    }

    public static class DuelOffer {
        private final UUID id;
        private final UUID senderUuid;
        private final UUID targetUuid;
        private final double amount;
        private final String type;
        private final long createdAt;

        public DuelOffer(UUID id, UUID senderUuid, UUID targetUuid, double amount, String type, long createdAt) {
            this.id = id;
            this.senderUuid = senderUuid;
            this.targetUuid = targetUuid;
            this.amount = amount;
            this.type = type;
            this.createdAt = createdAt;
        }

        public UUID getId() { return id; }
        public UUID getSenderUuid() { return senderUuid; }
        public UUID getTargetUuid() { return targetUuid; }
        public double getAmount() { return amount; }
        public String getType() { return type; }
        public long getCreatedAt() { return createdAt; }
    }
}
