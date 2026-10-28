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

public class ClanAdminCommand implements CommandExecutor, TabCompleter {

    private final TechnoEconomyPlus plugin;

    public ClanAdminCommand(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("technoeconomy.clan.admin")) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "delete", "disband" -> handleDelete(sender, args);
            case "forcejoin" -> handleForceJoin(sender, args);
            case "forceleave" -> handleForceLeave(sender, args);
            case "setlevel" -> handleSetLevel(sender, args);
            case "addxp" -> handleAddXp(sender, args);
            case "setbank" -> handleSetBank(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleList(CommandSender sender) {
        List<Clan> clans = plugin.getClanManager().getAllClans();
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-list-header", Map.of("count", String.valueOf(clans.size()))));
        for (Clan c : clans) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-list-entry", Map.of(
                    "clan", c.getName(),
                    "tag", c.getTag(),
                    "level", String.valueOf(c.getLevel()),
                    "members", String.valueOf(c.getMembers().size())
            )));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-info-usage"));
            return;
        }
        Clan clan = plugin.getClanManager().getClan(args[1]);
        if (clan == null) clan = plugin.getClanManager().getClanByTag(args[1]);
        if (clan == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-found"));
            return;
        }

        String currency = plugin.getConfig().getString("economy.currency-symbol", "$");
        OfflinePlayer leader = Bukkit.getOfflinePlayer(clan.getLeaderUuid());
        String leaderName = leader.getName() != null ? leader.getName() : "Unknown";

        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-info-header", Map.of("clan", clan.getName())));
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-info-tag", Map.of("tag", clan.getTag())));
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-info-leader", Map.of("leader", leaderName)));
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-info-level", Map.of("level", String.valueOf(clan.getLevel()), "xp", String.valueOf(clan.getXp()))));
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-info-bank", Map.of("balance", Format.money(clan.getBankBalance(), currency))));
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-info-members", Map.of("count", String.valueOf(clan.getMembers().size()), "max", String.valueOf(plugin.getClanManager().getMaxMembers(clan)))));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-delete-usage"));
            return;
        }
        Clan clan = plugin.getClanManager().getClan(args[1]);
        if (clan == null) clan = plugin.getClanManager().getClanByTag(args[1]);
        if (clan == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-found"));
            return;
        }

        plugin.getClanManager().disbandClan(clan, sender instanceof Player p ? p : null);
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-deleted", Map.of("clan", clan.getName())));
    }

    private void handleForceJoin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-forcejoin-usage"));
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.player-not-found", Map.of("player", args[1])));
            return;
        }
        Clan clan = plugin.getClanManager().getClan(args[2]);
        if (clan == null) clan = plugin.getClanManager().getClanByTag(args[2]);
        if (clan == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-found"));
            return;
        }

        Clan existing = plugin.getClanManager().getClanByPlayer(target.getUniqueId());
        if (existing != null) {
            existing.removeMember(target.getUniqueId());
            plugin.getClanManager().markDirty(existing);
        }

        clan.addMember(new ClanMember(target.getUniqueId(), target.getName(), ClanRank.MEMBER, System.currentTimeMillis()));
        plugin.getClanManager().markDirty(clan);
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-forcejoined", Map.of("player", target.getName(), "clan", clan.getName())));
        target.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-target-forcejoined", Map.of("clan", clan.getName())));
    }

    private void handleForceLeave(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-forceleave-usage"));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        Clan clan = plugin.getClanManager().getClanByPlayer(target.getUniqueId());
        if (clan == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-player-not-in-clan"));
            return;
        }

        if (clan.getLeaderUuid().equals(target.getUniqueId())) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-cannot-forceleave-leader"));
            return;
        }

        clan.removeMember(target.getUniqueId());
        plugin.getClanManager().markDirty(clan);
        String tName = target.getName() != null ? target.getName() : args[1];
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-forceleft", Map.of("player", tName, "clan", clan.getName())));
        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(plugin.getLanguageManager().getMessage("messages.clan-kicked", Map.of("clan", clan.getName())));
        }
    }

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-setlevel-usage"));
            return;
        }
        Clan clan = plugin.getClanManager().getClan(args[1]);
        if (clan == null) clan = plugin.getClanManager().getClanByTag(args[1]);
        if (clan == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-found"));
            return;
        }

        try {
            int level = Integer.parseInt(args[2]);
            clan.setLevel(level);
            clan.setXp(0);
            plugin.getClanManager().markDirty(clan);
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-level-set", Map.of("clan", clan.getName(), "level", String.valueOf(level))));
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.invalid-amount"));
        }
    }

    private void handleAddXp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-addxp-usage"));
            return;
        }
        Clan clan = plugin.getClanManager().getClan(args[1]);
        if (clan == null) clan = plugin.getClanManager().getClanByTag(args[1]);
        if (clan == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-found"));
            return;
        }

        try {
            long xp = Long.parseLong(args[2]);
            plugin.getClanManager().addXp(clan, xp, "admin");
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-xp-added", Map.of("clan", clan.getName(), "xp", String.valueOf(xp))));
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.invalid-amount"));
        }
    }

    private void handleSetBank(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-setbank-usage"));
            return;
        }
        Clan clan = plugin.getClanManager().getClan(args[1]);
        if (clan == null) clan = plugin.getClanManager().getClanByTag(args[1]);
        if (clan == null) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-not-found"));
            return;
        }

        try {
            double amount = Double.parseDouble(args[2]);
            clan.setBankBalance(amount);
            plugin.getClanManager().markDirty(clan);
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-bank-set", Map.of("clan", clan.getName(), "amount", String.valueOf(amount))));
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getLanguageManager().getMessage("messages.invalid-amount"));
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getClanManager().loadFromDatabase();
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-reloaded"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-help-header"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-help-list"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-help-info"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-help-delete"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-help-forcejoin"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-help-forceleave"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-help-setlevel"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-help-addxp"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-help-setbank"));
        sender.sendMessage(plugin.getLanguageManager().getMessage("messages.clanadmin-help-reload"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filter(List.of("list", "info", "delete", "forcejoin", "forceleave", "setlevel", "addxp", "setbank", "reload"), args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "info", "delete", "setlevel", "addxp", "setbank" -> {
                    return filter(plugin.getClanManager().getAllClans().stream().map(Clan::getName).toList(), args[1]);
                }
                case "forcejoin", "forceleave" -> {
                    return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
                }
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("forcejoin")) {
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
