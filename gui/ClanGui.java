package com.techno.economy.gui;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.clan.Clan;
import com.techno.economy.clan.ClanMember;
import com.techno.economy.clan.ClanRank;
import com.techno.economy.clan.ClanWar;
import com.techno.economy.menu.MenuHolder;
import com.techno.economy.util.Format;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.*;

public class ClanGui {

    private final TechnoEconomyPlus plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public ClanGui(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            openNoClanMenu(player);
            return;
        }

        String titleRaw = plugin.getLanguageManager().getRawMessage("gui.clan.main.title");
        if (titleRaw.startsWith("&cMissing message")) {
            titleRaw = "&8Clan: &6" + clan.getName();
        }
        titleRaw = titleRaw.replace("{clan}", clan.getName()).replace("{tag}", clan.getTag());
        Inventory inv = Bukkit.createInventory(new MenuHolder("clan_main"), 54, deserialize(titleRaw));

        fillBorders(inv, 54);

        String currency = plugin.getConfig().getString("economy.currency-symbol", "$");
        long requiredXp = plugin.getClanManager().getXpRequiredForLevel(clan.getLevel());
        double xpProgress = requiredXp > 0 ? (double) clan.getXp() / (double) requiredXp * 100.0 : 100.0;
        int maxMembers = plugin.getClanManager().getMaxMembers(clan);
        int maxAllies = plugin.getClanManager().getMaxAllies(clan);
        double bankLimit = plugin.getClanManager().getBankLimit(clan);

        OfflinePlayer leaderPlayer = Bukkit.getOfflinePlayer(clan.getLeaderUuid());
        String leaderName = leaderPlayer.getName() != null ? leaderPlayer.getName() : "Unknown";

        List<String> infoLore = new ArrayList<>();
        infoLore.add("&7Tag: &e[" + clan.getTag() + "&e]");
        infoLore.add("&7Leader: &f" + leaderName);
        infoLore.add("&7Level: &b" + clan.getLevel() + " &8(" + String.format("%.1f", xpProgress) + "%)");
        infoLore.add("&7XP: &f" + clan.getXp() + " &7/ &f" + requiredXp);
        infoLore.add("&7Members: &a" + clan.getMembers().size() + "&7/&2" + maxMembers);
        infoLore.add("&7Bank: &6" + Format.money(clan.getBankBalance(), currency) + " &7/ " + Format.money(bankLimit, currency));
        infoLore.add("&7KDR: &c" + clan.getKills() + "&7/&4" + clan.getDeaths() + " &8(&f" + String.format("%.2f", clan.getKdr()) + "&8)");
        infoLore.add("&7Wars Won: &a" + clan.getWarWins() + " &7Lost: &c" + clan.getWarLosses());
        inv.setItem(4, createItem(Material.SHIELD, "&6&l" + clan.getName(), infoLore));

        inv.setItem(19, createItem(Material.PLAYER_HEAD, "&b&lMembers", List.of(
                "&7View and manage clan members,",
                "&7promote, demote or kick members.",
                "",
                "&eClick to view members (" + clan.getMembers().size() + ")"
        )));

        inv.setItem(21, createItem(Material.GOLD_BLOCK, "&e&lClan Bank", List.of(
                "&7Balance: &6" + Format.money(clan.getBankBalance(), currency),
                "&7Limit: &7" + Format.money(bankLimit, currency),
                "",
                "&eClick to deposit/withdraw coins"
        )));

        Material warMat = clan.getActiveWar() != null ? Material.NETHERITE_SWORD : Material.IRON_SWORD;
        String warStatus = clan.getActiveWar() != null ? "&c&lWAR IN PROGRESS!" : "&7No active war";
        inv.setItem(23, createItem(warMat, "&c&lClan War", List.of(
                "&7Status: " + warStatus,
                "&7Wins: &a" + clan.getWarWins() + " &7Losses: &c" + clan.getWarLosses(),
                "",
                "&eClick to open Clan War menu"
        )));

        inv.setItem(25, createItem(Material.BEACON, "&d&lRelations", List.of(
                "&7Allies: &a" + clan.getAllies().size() + "&7/&2" + maxAllies,
                "&7Enemies: &c" + clan.getEnemies().size(),
                "",
                "&eClick to view diplomacy & relations"
        )));

        inv.setItem(29, createItem(Material.CAMPFIRE, "&a&lBases & Home", List.of(
                "&7Home: " + (clan.getHomeLocation() != null ? "&aSet" : "&cNot set"),
                "&7Bases: &b" + clan.getBases().size() + "&7/&3" + plugin.getConfig().getInt("clan.limits.max-bases", 3),
                "",
                "&eClick to teleport or manage bases"
        )));

        inv.setItem(31, createItem(Material.NETHER_STAR, "&6&lTop Clans", List.of(
                "&7View leaderboard rankings across",
                "&7clan level, bank, kills and wars.",
                "",
                "&eClick to open Leaderboards"
        )));

        ClanRank rank = clan.getMemberRank(player.getUniqueId());
        if (rank == ClanRank.LEADER) {
            inv.setItem(33, createItem(Material.BARRIER, "&4&lDisband Clan", List.of(
                    "&cPermanently delete and disband this clan.",
                    "&7Requires confirmation.",
                    "",
                    "&cClick to disband clan"
            )));
        } else {
            inv.setItem(33, createItem(Material.REDSTONE, "&c&lLeave Clan", List.of(
                    "&7Leave " + clan.getName() + " permanently.",
                    "",
                    "&cClick to leave clan"
            )));
        }

        inv.setItem(49, createItem(Material.IRON_DOOR, "&7&lClose Menu", List.of("&8Click to close")));

        player.openInventory(inv);
    }

    public void openNoClanMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new MenuHolder("clan_none"), 36, deserialize("&8Clan Overview"));
        fillBorders(inv, 36);

        String currency = plugin.getConfig().getString("economy.currency-symbol", "$");
        double cost = plugin.getConfig().getDouble("clan.creation.cost", 10000.0);

        inv.setItem(11, createItem(Material.BOOK, "&b&lCreate a Clan", List.of(
                "&7You are not in a clan yet!",
                "&7Creation Cost: &6" + Format.money(cost, currency),
                "",
                "&7Command: &f/clan create <tag> <name>",
                "&8Tag: 2-6 chars, Name: 3-16 chars"
        )));

        inv.setItem(13, createItem(Material.NETHER_STAR, "&6&lTop Clans", List.of(
                "&7View the most powerful clans",
                "&7on the server.",
                "",
                "&eClick to open Leaderboards"
        )));

        inv.setItem(15, createItem(Material.PAPER, "&a&lClan Invites", List.of(
                "&7Check incoming invitations.",
                "&7Commands:",
                "&f/clan accept <clan>",
                "&f/clan deny <clan>"
        )));

        inv.setItem(31, createItem(Material.IRON_DOOR, "&7&lClose Menu", List.of("&8Click to close")));

        player.openInventory(inv);
    }

    public void openMembersMenu(Player player, int page) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            openNoClanMenu(player);
            return;
        }

        Inventory inv = Bukkit.createInventory(new MenuHolder("clan_members:" + page), 54, deserialize("&8Clan Members (Page " + (page + 1) + ")"));
        fillBorders(inv, 54);

        List<ClanMember> members = new ArrayList<>(clan.getMembers().values());
        members.sort((a, b) -> Integer.compare(b.getRank().getWeight(), a.getRank().getWeight()));

        int perPage = 28;
        int startIndex = page * perPage;
        int endIndex = Math.min(startIndex + perPage, members.size());

        int[] memberSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        ClanRank viewerRank = clan.getMemberRank(player.getUniqueId());

        int slotIdx = 0;
        for (int i = startIndex; i < endIndex; i++) {
            ClanMember cm = members.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(cm.getPlayerUuid());
            String pName = op.getName() != null ? op.getName() : cm.getPlayerName();
            boolean isOnline = op.isOnline();

            List<String> lore = new ArrayList<>();
            lore.add("&7Rank: &6" + cm.getRank().name());
            lore.add("&7Joined: &8" + dateFormat.format(new Date(cm.getJoinedAt())));
            lore.add("&7Status: " + (isOnline ? "&aOnline" : "&7Offline"));
            if (viewerRank != null && viewerRank.getWeight() > cm.getRank().getWeight()) {
                lore.add("");
                lore.add("&eClick to manage this member");
            }

            ItemStack skull = createSkull(cm.getPlayerUuid(), (isOnline ? "&a" : "&7") + pName, lore);
            inv.setItem(memberSlots[slotIdx++], skull);
        }

        if (page > 0) {
            inv.setItem(45, createItem(Material.ARROW, "&a« Previous Page", List.of("&7Go to page " + page)));
        }

        inv.setItem(49, createItem(Material.ARROW, "&c« Back to Main Menu", List.of("&7Return to clan overview")));

        if (endIndex < members.size()) {
            inv.setItem(53, createItem(Material.ARROW, "&aNext Page »", List.of("&7Go to page " + (page + 2))));
        }

        player.openInventory(inv);
    }

    public void openMemberActionMenu(Player player, UUID targetUuid) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            openNoClanMenu(player);
            return;
        }

        ClanMember targetMember = clan.getMember(targetUuid);
        if (targetMember == null) {
            openMembersMenu(player, 0);
            return;
        }

        OfflinePlayer op = Bukkit.getOfflinePlayer(targetUuid);
        String tName = op.getName() != null ? op.getName() : targetMember.getPlayerName();

        Inventory inv = Bukkit.createInventory(new MenuHolder("clan_member_action:" + targetUuid), 27, deserialize("&8Manage: &6" + tName));
        fillBorders(inv, 27);

        ItemStack skull = createSkull(targetUuid, "&e" + tName, List.of(
                "&7Rank: &6" + targetMember.getRank().name(),
                "&7Joined: &8" + dateFormat.format(new Date(targetMember.getJoinedAt()))
        ));
        inv.setItem(4, skull);

        inv.setItem(11, createItem(Material.LIME_DYE, "&a&lPromote Member", List.of(
                "&7Increase rank by 1 tier.",
                "&8Next rank: " + (targetMember.getRank().getNextHigher() != null ? targetMember.getRank().getNextHigher().name() : "Max"),
                "",
                "&eClick to promote"
        )));

        inv.setItem(13, createItem(Material.ORANGE_DYE, "&e&lDemote Member", List.of(
                "&7Decrease rank by 1 tier.",
                "&8Next lower: " + (targetMember.getRank().getNextLower() != null ? targetMember.getRank().getNextLower().name() : "Min"),
                "",
                "&eClick to demote"
        )));

        inv.setItem(15, createItem(Material.RED_DYE, "&c&lKick Member", List.of(
                "&7Remove member from the clan.",
                "",
                "&cClick to kick"
        )));

        ClanRank viewerRank = clan.getMemberRank(player.getUniqueId());
        if (viewerRank == ClanRank.LEADER) {
            inv.setItem(17, createItem(Material.DRAGON_EGG, "&4&lTransfer Ownership", List.of(
                    "&cTransfer clan leadership to this member.",
                    "&cYou will become CO_LEADER.",
                    "",
                    "&cClick to transfer ownership"
            )));
        }

        inv.setItem(22, createItem(Material.ARROW, "&c« Back to Members", List.of("&7Return to member list")));

        player.openInventory(inv);
    }

    public void openBankMenu(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            openNoClanMenu(player);
            return;
        }

        String currency = plugin.getConfig().getString("economy.currency-symbol", "$");
        double bankLimit = plugin.getClanManager().getBankLimit(clan);
        double tax = plugin.getConfig().getDouble("clan.bank.tax-percent", 2.0);
        String withdrawRank = plugin.getConfig().getString("clan.bank.withdraw-permission-rank", "CO_LEADER");

        Inventory inv = Bukkit.createInventory(new MenuHolder("clan_bank"), 45, deserialize("&8Clan Bank"));
        fillBorders(inv, 45);

        inv.setItem(4, createItem(Material.CHEST, "&6&lClan Vault", List.of(
                "&7Current Balance: &6" + Format.money(clan.getBankBalance(), currency),
                "&7Vault Capacity: &7" + Format.money(bankLimit, currency),
                "&7Deposit Tax: &e" + tax + "%",
                "&7Withdraw Rank: &b" + withdrawRank
        )));

        inv.setItem(19, createItem(Material.EMERALD, "&aDeposit 100", List.of("&7Deposit &a" + Format.money(100, currency), "", "&eClick to deposit")));
        inv.setItem(20, createItem(Material.EMERALD, "&aDeposit 1,000", List.of("&7Deposit &a" + Format.money(1000, currency), "", "&eClick to deposit")));
        inv.setItem(21, createItem(Material.EMERALD, "&aDeposit 10,000", List.of("&7Deposit &a" + Format.money(10000, currency), "", "&eClick to deposit")));
        inv.setItem(22, createItem(Material.EMERALD_BLOCK, "&aDeposit All", List.of("&7Deposit entire wallet balance", "", "&eClick to deposit")));

        inv.setItem(23, createItem(Material.REDSTONE, "&cWithdraw 100", List.of("&7Withdraw &c" + Format.money(100, currency), "", "&eClick to withdraw")));
        inv.setItem(24, createItem(Material.REDSTONE, "&cWithdraw 1,000", List.of("&7Withdraw &c" + Format.money(1000, currency), "", "&eClick to withdraw")));
        inv.setItem(25, createItem(Material.REDSTONE, "&cWithdraw 10,000", List.of("&7Withdraw &c" + Format.money(10000, currency), "", "&eClick to withdraw")));
        inv.setItem(26, createItem(Material.REDSTONE_BLOCK, "&cWithdraw All", List.of("&7Withdraw full clan vault", "", "&eClick to withdraw")));

        inv.setItem(40, createItem(Material.ARROW, "&c« Back to Main Menu", List.of("&7Return to clan overview")));

        player.openInventory(inv);
    }

    public void openWarMenu(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            openNoClanMenu(player);
            return;
        }

        Inventory inv = Bukkit.createInventory(new MenuHolder("clan_war"), 36, deserialize("&8Clan War"));
        fillBorders(inv, 36);

        ClanWar activeWar = clan.getActiveWar();
        if (activeWar != null) {
            long remainingSec = Math.max(0, (activeWar.getEndTime() - System.currentTimeMillis()) / 1000);
            long minutes = remainingSec / 60;
            long seconds = remainingSec % 60;
            boolean isChallenger = activeWar.getChallengerClan().equalsIgnoreCase(clan.getName());
            int ourKills = isChallenger ? activeWar.getChallengerKills() : activeWar.getTargetKills();
            int theirKills = isChallenger ? activeWar.getTargetKills() : activeWar.getChallengerKills();
            String enemyName = isChallenger ? activeWar.getTargetClan() : activeWar.getChallengerClan();

            inv.setItem(13, createItem(Material.NETHERITE_SWORD, "&c&lACTIVE CLAN WAR", List.of(
                    "&7Opponent: &e" + enemyName,
                    "&7Time Remaining: &f" + String.format("%02d:%02d", minutes, seconds),
                    "&7Score: &a" + ourKills + " &7- &c" + theirKills,
                    "",
                    "&8Defeat opponent members to earn points!"
            )));
        } else {
            inv.setItem(13, createItem(Material.IRON_SWORD, "&e&lWar Statistics", List.of(
                    "&7Status: &aPeace",
                    "&7Victories: &a" + clan.getWarWins(),
                    "&7Defeats: &c" + clan.getWarLosses(),
                    "",
                    "&7Use &f/clan war declare <clan> &7to challenge another clan!"
            )));
        }

        inv.setItem(31, createItem(Material.ARROW, "&c« Back to Main Menu", List.of("&7Return to clan overview")));

        player.openInventory(inv);
    }

    public void openRelationsMenu(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            openNoClanMenu(player);
            return;
        }

        Inventory inv = Bukkit.createInventory(new MenuHolder("clan_allies"), 54, deserialize("&8Clan Diplomacy & Relations"));
        fillBorders(inv, 54);

        int maxAllies = plugin.getClanManager().getMaxAllies(clan);
        inv.setItem(4, createItem(Material.BEACON, "&d&lDiplomatic Status", List.of(
                "&7Allies: &a" + clan.getAllies().size() + " &7/ &2" + maxAllies,
                "&7Enemies: &c" + clan.getEnemies().size(),
                "",
                "&8Commands:",
                "&f/clan ally <clan>",
                "&f/clan enemy <clan>"
        )));

        int[] allySlots = {10, 11, 12, 13, 14, 15, 16};
        int aIdx = 0;
        for (String allyName : clan.getAllies()) {
            if (aIdx >= allySlots.length) break;
            Clan ac = plugin.getClanManager().getClan(allyName);
            String tag = ac != null ? ac.getTag() : "???";
            inv.setItem(allySlots[aIdx++], createItem(Material.LIME_BANNER, "&aAlly: &f" + allyName, List.of(
                    "&7Tag: &e[" + tag + "&e]",
                    "",
                    "&cClick to break alliance (/clan unally " + allyName + ")"
            )));
        }

        int[] enemySlots = {28, 29, 30, 31, 32, 33, 34};
        int eIdx = 0;
        for (String enemyName : clan.getEnemies()) {
            if (eIdx >= enemySlots.length) break;
            Clan ec = plugin.getClanManager().getClan(enemyName);
            String tag = ec != null ? ec.getTag() : "???";
            inv.setItem(enemySlots[eIdx++], createItem(Material.RED_BANNER, "&cEnemy: &f" + enemyName, List.of(
                    "&7Tag: &e[" + tag + "&e]",
                    "",
                    "&eClick to revoke enemy (/clan unenemy " + enemyName + ")"
            )));
        }

        inv.setItem(49, createItem(Material.ARROW, "&c« Back to Main Menu", List.of("&7Return to clan overview")));

        player.openInventory(inv);
    }

    public void openBasesMenu(Player player) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            openNoClanMenu(player);
            return;
        }

        Inventory inv = Bukkit.createInventory(new MenuHolder("clan_bases"), 36, deserialize("&8Clan Bases & Waypoints"));
        fillBorders(inv, 36);

        if (clan.getHomeLocation() != null) {
            inv.setItem(11, createItem(Material.RED_BED, "&a&lClan Home", List.of(
                    "&7World: &f" + (clan.getHomeLocation().getWorld() != null ? clan.getHomeLocation().getWorld().getName() : "world"),
                    "&7Coordinates: &f" + clan.getHomeLocation().getBlockX() + ", " + clan.getHomeLocation().getBlockY() + ", " + clan.getHomeLocation().getBlockZ(),
                    "",
                    "&eLeft-Click to teleport",
                    "&8Right-Click to update home location"
            )));
        } else {
            inv.setItem(11, createItem(Material.GRAY_BED, "&7&lClan Home (Not Set)", List.of(
                    "&7Clan home has not been set yet.",
                    "",
                    "&eClick to set clan home at your position"
            )));
        }

        int[] baseSlots = {13, 14, 15};
        List<String> baseNames = new ArrayList<>(clan.getBases().keySet());
        int maxBases = plugin.getConfig().getInt("clan.limits.max-bases", 3);

        for (int i = 0; i < maxBases && i < baseSlots.length; i++) {
            if (i < baseNames.size()) {
                String bName = baseNames.get(i);
                var loc = clan.getBaseLocation(bName);
                inv.setItem(baseSlots[i], createItem(Material.LODESTONE, "&b&lBase: " + bName, List.of(
                        "&7Coordinates: &f" + (loc != null ? loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() : "Unknown"),
                        "",
                        "&eClick to teleport (/clan base " + bName + ")"
                )));
            } else {
                inv.setItem(baseSlots[i], createItem(Material.BARRIER, "&8Empty Base Slot #" + (i + 1), List.of(
                        "&7Use &f/clan setbase <name>",
                        "&7to create a base waypoint here."
                )));
            }
        }

        inv.setItem(31, createItem(Material.ARROW, "&c« Back to Main Menu", List.of("&7Return to clan overview")));

        player.openInventory(inv);
    }

    public void openTopClansMenu(Player player, String filter) {
        String activeFilter = filter != null ? filter.toLowerCase() : "level";
        Inventory inv = Bukkit.createInventory(new MenuHolder("clan_top:" + activeFilter), 54, deserialize("&8Top Clans Leaderboard"));
        fillBorders(inv, 54);

        inv.setItem(2, createItem(Material.EXPERIENCE_BOTTLE, (activeFilter.equals("level") ? "&a&l" : "&7") + "Rank by Level", List.of("&8Click to sort by Level")));
        inv.setItem(3, createItem(Material.GOLD_BLOCK, (activeFilter.equals("bank") ? "&a&l" : "&7") + "Rank by Bank", List.of("&8Click to sort by Bank")));
        inv.setItem(4, createItem(Material.IRON_SWORD, (activeFilter.equals("kills") ? "&a&l" : "&7") + "Rank by Kills", List.of("&8Click to sort by Kills")));
        inv.setItem(5, createItem(Material.NETHERITE_CHESTPLATE, (activeFilter.equals("war_wins") ? "&a&l" : "&7") + "Rank by War Wins", List.of("&8Click to sort by War Wins")));

        List<Clan> topList = plugin.getClanManager().getTopClans(activeFilter);
        int[] topSlots = {
                19, 20, 21, 22, 23, 24, 25,
                29, 30, 31, 32, 33, 34, 35
        };

        String currency = plugin.getConfig().getString("economy.currency-symbol", "$");
        int rank = 1;
        for (int i = 0; i < topList.size() && i < topSlots.length; i++) {
            Clan c = topList.get(i);
            Material icon = rank == 1 ? Material.GOLD_INGOT : rank == 2 ? Material.IRON_INGOT : rank == 3 ? Material.COPPER_INGOT : Material.PAPER;

            List<String> lore = new ArrayList<>();
            lore.add("&7Tag: &e[" + c.getTag() + "&e]");
            lore.add("&7Level: &b" + c.getLevel() + " &8(" + c.getXp() + " XP)");
            lore.add("&7Members: &f" + c.getMembers().size());
            lore.add("&7Bank: &6" + Format.money(c.getBankBalance(), currency));
            lore.add("&7KDR: &c" + c.getKills() + "&7/&4" + c.getDeaths());
            lore.add("&7War Wins: &a" + c.getWarWins());

            inv.setItem(topSlots[i], createItem(icon, "&6#" + rank + " &f" + c.getName(), lore));
            rank++;
        }

        inv.setItem(49, createItem(Material.ARROW, "&c« Back", List.of("&7Return")));

        player.openInventory(inv);
    }

    public void openConfirmMenu(Player player, String action, String targetData, String title, String prompt) {
        Inventory inv = Bukkit.createInventory(new MenuHolder("clan_confirm:" + action + ":" + targetData), 27, deserialize(title));
        fillBorders(inv, 27);

        inv.setItem(11, createItem(Material.LIME_STAINED_GLASS_PANE, "&a&lConfirm Action", List.of("&7Click to confirm")));
        inv.setItem(13, createItem(Material.PAPER, "&e&lConfirmation", List.of("&7" + prompt)));
        inv.setItem(15, createItem(Material.RED_STAINED_GLASS_PANE, "&c&lCancel Action", List.of("&7Click to abort")));

        player.openInventory(inv);
    }

    public static ItemStack createItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null && !displayName.isEmpty()) {
                Component nameComp = deserialize(displayName).decoration(TextDecoration.ITALIC, false);
                meta.displayName(nameComp);
            }
            if (lore != null && !lore.isEmpty()) {
                List<Component> loreComps = new ArrayList<>();
                for (String line : lore) {
                    loreComps.add(deserialize(line).decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(loreComps);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createSkull(UUID ownerUuid, String displayName, List<String> lore) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerUuid));
            if (displayName != null && !displayName.isEmpty()) {
                Component nameComp = deserialize(displayName).decoration(TextDecoration.ITALIC, false);
                meta.displayName(nameComp);
            }
            if (lore != null && !lore.isEmpty()) {
                List<Component> loreComps = new ArrayList<>();
                for (String line : lore) {
                    loreComps.add(deserialize(line).decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(loreComps);
            }
            skull.setItemMeta(meta);
        }
        return skull;
    }

    private void fillBorders(Inventory inv, int size) {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            border.setItemMeta(meta);
        }

        if (size == 54) {
            int[] borderSlots = {
                    0, 1, 2, 3, 5, 6, 7, 8,
                    9, 17, 18, 26, 27, 35, 36, 44,
                    45, 46, 47, 48, 50, 51, 52, 53
            };
            for (int s : borderSlots) {
                inv.setItem(s, border.clone());
            }
        } else if (size == 45) {
            int[] borderSlots = {
                    0, 1, 2, 3, 5, 6, 7, 8,
                    9, 17, 18, 26, 27, 35,
                    36, 37, 38, 39, 41, 42, 43, 44
            };
            for (int s : borderSlots) {
                inv.setItem(s, border.clone());
            }
        } else if (size == 36) {
            int[] borderSlots = {
                    0, 1, 2, 3, 4, 5, 6, 7, 8,
                    9, 17, 18, 26,
                    27, 28, 29, 30, 32, 33, 34, 35
            };
            for (int s : borderSlots) {
                inv.setItem(s, border.clone());
            }
        } else if (size == 27) {
            int[] borderSlots = {
                    0, 1, 2, 3, 5, 6, 7, 8,
                    9, 10, 12, 14, 16, 17,
                    18, 19, 20, 21, 23, 24, 25, 26
            };
            for (int s : borderSlots) {
                inv.setItem(s, border.clone());
            }
        }
    }

    public static Component deserialize(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
