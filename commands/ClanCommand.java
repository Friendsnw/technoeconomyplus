package com.techno.economy.commands;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.clan.Clan;
import com.techno.economy.clan.ClanMember;
import com.techno.economy.clan.ClanRank;
import com.techno.economy.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClanCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public ClanCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.players-only"));
            return true;
        }

        if (!player.hasPermission("technoeconomy.clan")) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.no-permission"));
            return true;
        }

        if (!plugin.getConfig().getBoolean("clan.enabled", true)) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-disabled"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("menu") || args[0].equalsIgnoreCase("gui")) {
            plugin.getClanGui().openMainMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create" -> handleCreate(player, args);
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player, args);
            case "deny" -> handleDeny(player, args);
            case "kick" -> handleKick(player, args);
            case "promote" -> handlePromote(player, args);
            case "demote" -> handleDemote(player, args);
            case "leave" -> handleLeave(player);
            case "disband" -> handleDisband(player);
            case "transfer" -> handleTransfer(player, args);
            case "sethome" -> handleSetHome(player);
            case "home" -> handleHome(player);
            case "setbase" -> handleSetBase(player, args);
            case "base" -> handleBase(player, args);
            case "ally" -> handleAlly(player, args);
            case "unally" -> handleUnally(player, args);
            case "enemy" -> handleEnemy(player, args);
            case "unenemy" -> handleUnenemy(player, args);
            case "war" -> handleWar(player, args);
            case "bank" -> handleBank(player, args);
            case "info" -> handleInfo(player, args);
            case "list", "top" -> handleTop(player, args);
            case "chat" -> handleChat(player, args);
            case "allychat" -> handleAllyChat(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-create-usage"));
            return;
        }
        String tag = args[1];
        String name = args[2];
        plugin.getClanManager().createClan(player, name, tag);
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-invite-usage"));
            return;
        }
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-in-clan"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.player-not-found", Map.of("player", args[1])));
            return;
        }
        plugin.getClanManager().invitePlayer(clan, player, target);
    }

    private void handleAccept(Player player, String[] args) {
        String clanName = args.length > 1 ? args[1] : null;
        plugin.getClanManager().acceptInvite(player, clanName);
    }

    private void handleDeny(Player player, String[] args) {
        String clanName = args.length > 1 ? args[1] : null;
        plugin.getClanManager().denyInvite(player, clanName);
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-kick-usage"));
            return;
        }
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-in-clan"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        plugin.getClanManager().kickPlayer(clan, player, target.getUniqueId());
    }

    private void handlePromote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-promote-usage"));
            return;
        }
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-in-clan"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        plugin.getClanManager().promotePlayer(clan, player, target.getUniqueId());
    }

    private void handleDemote(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-demote-usage"));
            return;
        }
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-in-clan"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        plugin.getClanManager().demotePlayer(clan, player, target.getUniqueId());
    }

    private void handleLeave(Player player) {
        plugin.getClanManager().leaveClan(player);
    }

    private void handleDisband(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-in-clan"));
            return;
        }
        plugin.getClanManager().disbandClan(clan, player);
    }

    private void handleTransfer(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-transfer-usage"));
            return;
        }
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-in-clan"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.player-not-found", Map.of("player", args[1])));
            return;
        }
        plugin.getClanManager().transferOwnership(clan, player, target);
    }

    private void handleSetHome(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-in-clan"));
            return;
        }
        plugin.getClanManager().setHome(clan, player, player.getLocation());
    }

    private void handleHome(Player player) {
        plugin.getClanManager().teleportHome(player);
    }

    private void handleSetBase(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-setbase-usage"));
            return;
        }
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-in-clan"));
            return;
        }
        plugin.getClanManager().setBase(clan, player, args[1], player.getLocation());
    }

    private void handleBase(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-base-usage"));
            return;
        }
        plugin.getClanManager().teleportBase(player, args[1]);
    }

    private void handleAlly(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-ally-usage"));
            return;
        }
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-in-clan"));
            return;
        }
        Clan targetClan = plugin.getClanManager().getClan(args[1]);
        if (targetClan == null) {
            targetClan = plugin.getClanManager().getClanByTag(args[1]);
        }
        if (targetClan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-found"));
            return;
        }
        plugin.getClanManager().requestAlly(clan, targetClan, player);
    }

    private void handleUnally(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-unally-usage"));
            return;
        }
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-in-clan"));
            return;
        }
        Clan targetClan = plugin.getClanManager().getClan(args[1]);
        if (targetClan == null) {
            targetClan = plugin.getClanManager().getClanByTag(args[1]);
        }
        if (targetClan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-found"));
            return;
        }
        plugin.getClanManager().removeAlly(clan, targetClan, player);
    }

    private void handleEnemy(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-enemy-usage"));
            return;
        }
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-in-clan"));
            return;
        }
        Clan targetClan = plugin.getClanManager().getClan(args[1]);
        if (targetClan == null) {
            targetClan = plugin.getClanManager().getClanByTag(args[1]);
        }
        if (targetClan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-found"));
            return;
        }
        plugin.getClanManager().declareEnemy(clan, targetClan, player);
    }

    private void handleUnenemy(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-unenemy-usage"));
            return;
        }
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-in-clan"));
            return;
        }
        Clan targetClan = plugin.getClanManager().getClan(args[1]);
        if (targetClan == null) {
            targetClan = plugin.getClanManager().getClanByTag(args[1]);
        }
        if (targetClan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-found"));
            return;
        }
        plugin.getClanManager().removeEnemy(clan, targetClan, player);
    }

    private void handleWar(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getClanGui().openWarMenu(player);
            return;
        }
        String warAction = args[1].toLowerCase();
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-in-clan"));
            return;
        }

        switch (warAction) {
            case "declare" -> {
                if (args.length < 3) {
                    player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-war-usage"));
                    return;
                }
                Clan targetClan = plugin.getClanManager().getClan(args[2]);
                if (targetClan == null) targetClan = plugin.getClanManager().getClanByTag(args[2]);
                if (targetClan == null) {
                    player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-found"));
                    return;
                }
                plugin.getClanManager().declareWar(clan, targetClan, player);
            }
            case "accept" -> plugin.getClanManager().acceptWar(clan, player);
            case "deny" -> plugin.getClanManager().declineWar(clan, player);
            default -> player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-war-usage"));
        }
    }

    private void handleBank(Player player, String[] args) {
        if (args.length == 1) {
            plugin.getClanGui().openBankMenu(player);
            return;
        }
        if (args.length < 3) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-bank-usage"));
            return;
        }
        String action = args[1].toLowerCase();
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.invalid-amount"));
            return;
        }
        if (action.equals("deposit") || action.equals("yatir")) {
            plugin.getClanManager().depositBank(player, amount);
        } else if (action.equals("withdraw") || action.equals("cek")) {
            plugin.getClanManager().withdrawBank(player, amount);
        } else {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-bank-usage"));
        }
    }

    private void handleInfo(Player player, String[] args) {
        Clan clan;
        if (args.length >= 2) {
            clan = plugin.getClanManager().getClan(args[1]);
            if (clan == null) clan = plugin.getClanManager().getClanByTag(args[1]);
        } else {
            clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        }

        if (clan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-found"));
            return;
        }

        String currency = plugin.getConfig().getString("economy.currency-symbol", "$");
        OfflinePlayer leader = Bukkit.getOfflinePlayer(clan.getLeaderUuid());
        String leaderName = leader.getName() != null ? leader.getName() : "Unknown";

        player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-info-header", Map.of("clan", clan.getName())));
        player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-info-tag", Map.of("tag", clan.getTag())));
        player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-info-leader", Map.of("leader", leaderName)));
        player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-info-level", Map.of("level", String.valueOf(clan.getLevel()), "xp", String.valueOf(clan.getXp()))));
        player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-info-members", Map.of("count", String.valueOf(clan.getMembers().size()), "max", String.valueOf(plugin.getClanManager().getMaxMembers(clan)))));
        player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-info-bank", Map.of("balance", Format.money(clan.getBankBalance(), currency))));
        player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-info-kdr", Map.of("kills", String.valueOf(clan.getKills()), "deaths", String.valueOf(clan.getDeaths()), "kdr", String.format("%.2f", clan.getKdr()))));
        player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-info-allies", Map.of("allies", String.join(", ", clan.getAllies()))));
    }

    private void handleTop(Player player, String[] args) {
        String filter = args.length > 1 ? args[1] : "level";
        plugin.getClanGui().openTopClansMenu(player, filter);
    }

    private void handleChat(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-in-clan"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-chat-usage"));
            return;
        }
        String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.getClanManager().sendClanChat(player, clan, msg);
    }

    private void handleAllyChat(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-in-clan"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-allychat-usage"));
            return;
        }
        String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.getClanManager().sendAllyChat(player, clan, msg);
    }

    private void sendHelp(Player player) {
        player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-help-header"));
        player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-help-create"));
        player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-help-invite"));
        player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-help-members"));
        player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-help-bank"));
        player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-help-war"));
        player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-help-bases"));
        player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-help-relations"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = List.of(
                    "create", "invite", "accept", "deny", "kick", "promote", "demote", "leave",
                    "disband", "transfer", "sethome", "home", "setbase", "base", "ally", "unally",
                    "enemy", "unenemy", "war", "bank", "info", "top", "list", "chat", "allychat", "menu"
            );
            return filter(list, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "invite", "kick", "promote", "demote", "transfer" -> {
                    return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
                }
                case "ally", "unally", "enemy", "unenemy", "info" -> {
                    return filter(plugin.getClanManager().getAllClans().stream().map(Clan::getName).toList(), args[1]);
                }
                case "war" -> {
                    return filter(List.of("declare", "accept", "deny"), args[1]);
                }
                case "bank" -> {
                    return filter(List.of("deposit", "withdraw"), args[1]);
                }
                case "top", "list" -> {
                    return filter(List.of("level", "bank", "kills", "war_wins"), args[1]);
                }
                case "base" -> {
                    if (sender instanceof Player p) {
                        Clan c = plugin.getClanManager().getClanByPlayer(p.getUniqueId());
                        if (c != null) {
                            return filter(new ArrayList<>(c.getBases().keySet()), args[1]);
                        }
                    }
                }
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("war") && args[1].equalsIgnoreCase("declare")) {
            return filter(plugin.getClanManager().getAllClans().stream().map(Clan::getName).toList(), args[2]);
        }

        return Collections.emptyList();
    }

    private List<String> filter(Collection<String> list, String input) {
        String lower = input.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(lower)) {
                result.add(s);
            }
        }
        return result;
    }
}
