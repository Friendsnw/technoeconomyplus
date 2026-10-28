package com.techno.economy.vault;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.clan.Clan;
import com.techno.economy.util.Format;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class VaultEconomyHook implements Economy {

    private final TechnoEconomyPlus plugin;

    public VaultEconomyHook(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isEnabled() {
        return plugin != null && plugin.isEnabled();
    }

    @Override
    public String getName() {
        return "technoEconomyPlus";
    }

    @Override
    public boolean hasBankSupport() {
        return true;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        return Format.money(amount, symbol);
    }

    @Override
    public String currencyNamePlural() {
        return plugin.getConfig().getString("economy.currency-plural", "Dollars");
    }

    @Override
    public String currencyNameSingular() {
        return plugin.getConfig().getString("economy.currency-singular", "Dollar");
    }

    @Override
    public boolean hasAccount(String playerName) {
        if (playerName == null || playerName.isBlank()) return false;
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return hasAccount(op);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return player != null;
    }

    @Override
    public boolean hasAccount(String playerName, String worldName) {
        return hasAccount(playerName);
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player);
    }

    @Override
    public double getBalance(String playerName) {
        if (playerName == null || playerName.isBlank()) return 0.0;
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return getBalance(op);
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        if (player == null) return 0.0;
        return plugin.getBalanceManager().getBalance(player.getUniqueId());
    }

    @Override
    public double getBalance(String playerName, String world) {
        return getBalance(playerName);
    }

    @Override
    public double getBalance(OfflinePlayer player, String world) {
        return getBalance(player);
    }

    @Override
    public boolean has(String playerName, double amount) {
        if (playerName == null || playerName.isBlank()) return false;
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return has(op, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        if (player == null || amount < 0) return false;
        return plugin.getBalanceManager().has(player.getUniqueId(), amount);
    }

    @Override
    public boolean has(String playerName, String worldName, double amount) {
        return has(playerName, amount);
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        if (playerName == null || playerName.isBlank()) {
            return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player name is invalid.");
        }
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return withdrawPlayer(op, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        if (player == null) {
            return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player cannot be null.");
        }
        if (amount < 0) {
            return new EconomyResponse(0.0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Cannot withdraw negative amount.");
        }

        UUID uuid = player.getUniqueId();
        double current = plugin.getBalanceManager().getBalance(uuid);
        if (current < amount) {
            return new EconomyResponse(0.0, current, EconomyResponse.ResponseType.FAILURE, "Insufficient funds.");
        }

        boolean ok = plugin.getBalanceManager().withdraw(uuid, amount);
        if (!ok) {
            return new EconomyResponse(0.0, plugin.getBalanceManager().getBalance(uuid), EconomyResponse.ResponseType.FAILURE, "Withdrawal failed.");
        }

        double updated = plugin.getBalanceManager().getBalance(uuid);
        return new EconomyResponse(amount, updated, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
        return withdrawPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        if (playerName == null || playerName.isBlank()) {
            return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player name is invalid.");
        }
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return depositPlayer(op, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        if (player == null) {
            return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player cannot be null.");
        }
        if (amount < 0) {
            return new EconomyResponse(0.0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Cannot deposit negative amount.");
        }

        UUID uuid = player.getUniqueId();
        plugin.getBalanceManager().deposit(uuid, amount);
        double updated = plugin.getBalanceManager().getBalance(uuid);
        return new EconomyResponse(amount, updated, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
        return depositPlayer(playerName, amount);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount);
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        if (name == null || name.isBlank()) {
            return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Bank name cannot be empty.");
        }
        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return createBank(name, player != null ? player.getName() : "");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        if (name == null || name.isBlank()) {
            return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Invalid bank name.");
        }
        Clan clan = plugin.getClanManager().getClan(name);
        if (clan != null) {
            return new EconomyResponse(0.0, clan.getBankBalance(), EconomyResponse.ResponseType.SUCCESS, null);
        }
        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        if (op.hasPlayedBefore() || op.isOnline()) {
            double bal = plugin.getBankManager().getBankBalance(op.getUniqueId());
            return new EconomyResponse(0.0, bal, EconomyResponse.ResponseType.SUCCESS, null);
        }
        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Bank not found.");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        EconomyResponse resp = bankBalance(name);
        if (!resp.transactionSuccess()) return resp;
        if (resp.balance >= amount) {
            return new EconomyResponse(0.0, resp.balance, EconomyResponse.ResponseType.SUCCESS, null);
        }
        return new EconomyResponse(0.0, resp.balance, EconomyResponse.ResponseType.FAILURE, "Bank does not have enough funds.");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        if (name == null || name.isBlank() || amount < 0) {
            return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Invalid arguments.");
        }
        Clan clan = plugin.getClanManager().getClan(name);
        if (clan != null) {
            if (clan.getBankBalance() < amount) {
                return new EconomyResponse(0.0, clan.getBankBalance(), EconomyResponse.ResponseType.FAILURE, "Insufficient bank balance.");
            }
            clan.setBankBalance(clan.getBankBalance() - amount);
            plugin.getClanManager().markDirty(clan);
            return new EconomyResponse(amount, clan.getBankBalance(), EconomyResponse.ResponseType.SUCCESS, null);
        }
        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        if (op.hasPlayedBefore() || op.isOnline()) {
            double cur = plugin.getBankManager().getBankBalance(op.getUniqueId());
            if (cur < amount) {
                return new EconomyResponse(0.0, cur, EconomyResponse.ResponseType.FAILURE, "Insufficient bank balance.");
            }
            plugin.getBankManager().withdraw(op.getUniqueId(), amount);
            double updated = plugin.getBankManager().getBankBalance(op.getUniqueId());
            return new EconomyResponse(amount, updated, EconomyResponse.ResponseType.SUCCESS, null);
        }
        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Bank not found.");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        if (name == null || name.isBlank() || amount < 0) {
            return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Invalid arguments.");
        }
        Clan clan = plugin.getClanManager().getClan(name);
        if (clan != null) {
            clan.setBankBalance(clan.getBankBalance() + amount);
            plugin.getClanManager().markDirty(clan);
            return new EconomyResponse(amount, clan.getBankBalance(), EconomyResponse.ResponseType.SUCCESS, null);
        }
        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        if (op.hasPlayedBefore() || op.isOnline()) {
            plugin.getBankManager().deposit(op.getUniqueId(), amount);
            double updated = plugin.getBankManager().getBankBalance(op.getUniqueId());
            return new EconomyResponse(amount, updated, EconomyResponse.ResponseType.SUCCESS, null);
        }
        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Bank not found.");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        if (playerName == null) return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player is null.");
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return isBankOwner(name, op);
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        if (name == null || player == null) {
            return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Invalid arguments.");
        }
        Clan clan = plugin.getClanManager().getClan(name);
        if (clan != null && clan.getLeaderUuid().equals(player.getUniqueId())) {
            return new EconomyResponse(0.0, clan.getBankBalance(), EconomyResponse.ResponseType.SUCCESS, null);
        }
        if (name.equalsIgnoreCase(player.getName())) {
            return new EconomyResponse(0.0, plugin.getBankManager().getBankBalance(player.getUniqueId()), EconomyResponse.ResponseType.SUCCESS, null);
        }
        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Not bank owner.");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        if (playerName == null) return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Player is null.");
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerName);
        return isBankMember(name, op);
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        if (name == null || player == null) {
            return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Invalid arguments.");
        }
        Clan clan = plugin.getClanManager().getClan(name);
        if (clan != null && clan.isMember(player.getUniqueId())) {
            return new EconomyResponse(0.0, clan.getBankBalance(), EconomyResponse.ResponseType.SUCCESS, null);
        }
        if (name.equalsIgnoreCase(player.getName())) {
            return new EconomyResponse(0.0, plugin.getBankManager().getBankBalance(player.getUniqueId()), EconomyResponse.ResponseType.SUCCESS, null);
        }
        return new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Not bank member.");
    }

    @Override
    public List<String> getBanks() {
        List<String> list = new ArrayList<>();
        for (Clan c : plugin.getClanManager().getAllClans()) {
            list.add(c.getName());
        }
        return Collections.unmodifiableList(list);
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String worldName) {
        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return true;
    }
}
