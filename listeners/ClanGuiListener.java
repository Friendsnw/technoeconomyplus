package com.techno.economy.listeners;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.clan.Clan;
import com.techno.economy.clan.ClanMember;
import com.techno.economy.clan.ClanRank;
import com.techno.economy.menu.MenuHolder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClanGuiListener implements Listener {

    private final TechnoEconomyPlus plugin;

    public ClanGuiListener(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) return;

        String menuName = holder.getMenuName();
        if (menuName == null || !menuName.startsWith("clan_")) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);

        if (menuName.equals("clan_main")) {
            handleMainMenu(player, slot);
        } else if (menuName.equals("clan_none")) {
            handleNoClanMenu(player, slot);
        } else if (menuName.startsWith("clan_members:")) {
            int page = 0;
            try {
                page = Integer.parseInt(menuName.substring("clan_members:".length()));
            } catch (Exception ignored) {}
            handleMembersMenu(player, slot, page);
        } else if (menuName.startsWith("clan_member_action:")) {
            String uuidStr = menuName.substring("clan_member_action:".length());
            try {
                UUID targetUuid = UUID.fromString(uuidStr);
                handleMemberActionMenu(player, slot, targetUuid);
            } catch (Exception ignored) {}
        } else if (menuName.equals("clan_bank")) {
            handleBankMenu(player, slot);
        } else if (menuName.equals("clan_war")) {
            handleWarMenu(player, slot);
        } else if (menuName.equals("clan_allies")) {
            handleAlliesMenu(player, slot);
        } else if (menuName.equals("clan_bases")) {
            handleBasesMenu(player, slot, event.isRightClick());
        } else if (menuName.startsWith("clan_top:")) {
            String filter = menuName.substring("clan_top:".length());
            handleTopMenu(player, slot, filter);
        } else if (menuName.startsWith("clan_confirm:")) {
            String[] parts = menuName.substring("clan_confirm:".length()).split(":", 2);
            String action = parts[0];
            String data = parts.length > 1 ? parts[1] : "";
            handleConfirmMenu(player, slot, action, data);
        }
    }

    private void handleMainMenu(Player player, int slot) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            plugin.getClanGui().openNoClanMenu(player);
            return;
        }

        switch (slot) {
            case 19 -> plugin.getClanGui().openMembersMenu(player, 0);
            case 21 -> plugin.getClanGui().openBankMenu(player);
            case 23 -> plugin.getClanGui().openWarMenu(player);
            case 25 -> plugin.getClanGui().openRelationsMenu(player);
            case 29 -> plugin.getClanGui().openBasesMenu(player);
            case 31 -> plugin.getClanGui().openTopClansMenu(player, "level");
            case 33 -> {
                ClanRank rank = clan.getMemberRank(player.getUniqueId());
                if (rank == ClanRank.LEADER) {
                    plugin.getClanGui().openConfirmMenu(player, "disband", "", "&4&lDisband Clan", "Disband " + clan.getName() + " permanently?");
                } else {
                    plugin.getClanGui().openConfirmMenu(player, "leave", "", "&c&lLeave Clan", "Leave " + clan.getName() + "?");
                }
            }
            case 49 -> player.closeInventory();
        }
    }

    private void handleNoClanMenu(Player player, int slot) {
        switch (slot) {
            case 13 -> plugin.getClanGui().openTopClansMenu(player, "level");
            case 31 -> player.closeInventory();
        }
    }

    private void handleMembersMenu(Player player, int slot, int page) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            plugin.getClanGui().openNoClanMenu(player);
            return;
        }

        if (slot == 45 && page > 0) {
            plugin.getClanGui().openMembersMenu(player, page - 1);
            return;
        }
        if (slot == 49) {
            plugin.getClanGui().openMainMenu(player);
            return;
        }

        int[] memberSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        List<ClanMember> members = new ArrayList<>(clan.getMembers().values());
        members.sort((a, b) -> Integer.compare(b.getRank().getWeight(), a.getRank().getWeight()));

        int perPage = 28;
        if (slot == 53 && (page + 1) * perPage < members.size()) {
            plugin.getClanGui().openMembersMenu(player, page + 1);
            return;
        }

        int clickedIdx = -1;
        for (int i = 0; i < memberSlots.length; i++) {
            if (memberSlots[i] == slot) {
                clickedIdx = i;
                break;
            }
        }

        if (clickedIdx != -1) {
            int targetIdx = page * perPage + clickedIdx;
            if (targetIdx < members.size()) {
                ClanMember cm = members.get(targetIdx);
                ClanRank viewerRank = clan.getMemberRank(player.getUniqueId());
                if (viewerRank != null && (viewerRank.getWeight() > cm.getRank().getWeight() || viewerRank == ClanRank.LEADER)) {
                    plugin.getClanGui().openMemberActionMenu(player, cm.getPlayerUuid());
                }
            }
        }
    }

    private void handleMemberActionMenu(Player player, int slot, UUID targetUuid) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            plugin.getClanGui().openNoClanMenu(player);
            return;
        }

        ClanMember targetMember = clan.getMember(targetUuid);
        if (targetMember == null) {
            plugin.getClanGui().openMembersMenu(player, 0);
            return;
        }

        switch (slot) {
            case 11 -> {
                plugin.getClanManager().promotePlayer(clan, player, targetUuid);
                plugin.getClanGui().openMemberActionMenu(player, targetUuid);
            }
            case 13 -> {
                plugin.getClanManager().demotePlayer(clan, player, targetUuid);
                plugin.getClanGui().openMemberActionMenu(player, targetUuid);
            }
            case 15 -> {
                OfflinePlayer op = Bukkit.getOfflinePlayer(targetUuid);
                String tName = op.getName() != null ? op.getName() : targetMember.getPlayerName();
                plugin.getClanGui().openConfirmMenu(player, "kick", targetUuid.toString(), "&c&lKick Member", "Kick " + tName + " from clan?");
            }
            case 17 -> {
                if (clan.getMemberRank(player.getUniqueId()) == ClanRank.LEADER) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(targetUuid);
                    String tName = op.getName() != null ? op.getName() : targetMember.getPlayerName();
                    plugin.getClanGui().openConfirmMenu(player, "transfer", targetUuid.toString(), "&4&lTransfer Ownership", "Transfer leadership to " + tName + "?");
                }
            }
            case 22 -> plugin.getClanGui().openMembersMenu(player, 0);
        }
    }

    private void handleBankMenu(Player player, int slot) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            plugin.getClanGui().openNoClanMenu(player);
            return;
        }

        switch (slot) {
            case 19 -> {
                plugin.getClanManager().depositBank(player, 100);
                plugin.getClanGui().openBankMenu(player);
            }
            case 20 -> {
                plugin.getClanManager().depositBank(player, 1000);
                plugin.getClanGui().openBankMenu(player);
            }
            case 21 -> {
                plugin.getClanManager().depositBank(player, 10000);
                plugin.getClanGui().openBankMenu(player);
            }
            case 22 -> {
                double bal = plugin.getBalanceManager().getBalance(player.getUniqueId());
                if (bal > 0) plugin.getClanManager().depositBank(player, bal);
                plugin.getClanGui().openBankMenu(player);
            }
            case 23 -> {
                plugin.getClanManager().withdrawBank(player, 100);
                plugin.getClanGui().openBankMenu(player);
            }
            case 24 -> {
                plugin.getClanManager().withdrawBank(player, 1000);
                plugin.getClanGui().openBankMenu(player);
            }
            case 25 -> {
                plugin.getClanManager().withdrawBank(player, 10000);
                plugin.getClanGui().openBankMenu(player);
            }
            case 26 -> {
                double cbal = clan.getBankBalance();
                if (cbal > 0) plugin.getClanManager().withdrawBank(player, cbal);
                plugin.getClanGui().openBankMenu(player);
            }
            case 40 -> plugin.getClanGui().openMainMenu(player);
        }
    }

    private void handleWarMenu(Player player, int slot) {
        if (slot == 31) {
            plugin.getClanGui().openMainMenu(player);
        }
    }

    private void handleAlliesMenu(Player player, int slot) {
        if (slot == 49) {
            plugin.getClanGui().openMainMenu(player);
        }
    }

    private void handleBasesMenu(Player player, int slot, boolean isRightClick) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            plugin.getClanGui().openNoClanMenu(player);
            return;
        }

        if (slot == 11) {
            if (isRightClick || clan.getHomeLocation() == null) {
                plugin.getClanManager().setHome(clan, player, player.getLocation());
                plugin.getClanGui().openBasesMenu(player);
            } else {
                player.closeInventory();
                plugin.getClanManager().teleportHome(player);
            }
            return;
        }

        int[] baseSlots = {13, 14, 15};
        List<String> baseNames = new ArrayList<>(clan.getBases().keySet());
        for (int i = 0; i < baseSlots.length; i++) {
            if (baseSlots[i] == slot && i < baseNames.size()) {
                String bName = baseNames.get(i);
                player.closeInventory();
                plugin.getClanManager().teleportBase(player, bName);
                return;
            }
        }

        if (slot == 31) {
            plugin.getClanGui().openMainMenu(player);
        }
    }

    private void handleTopMenu(Player player, int slot, String currentFilter) {
        switch (slot) {
            case 2 -> plugin.getClanGui().openTopClansMenu(player, "level");
            case 3 -> plugin.getClanGui().openTopClansMenu(player, "bank");
            case 4 -> plugin.getClanGui().openTopClansMenu(player, "kills");
            case 5 -> plugin.getClanGui().openTopClansMenu(player, "war_wins");
            case 49 -> plugin.getClanGui().openMainMenu(player);
        }
    }

    private void handleConfirmMenu(Player player, int slot, String action, String data) {
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());

        if (slot == 11) {
            if ("disband".equalsIgnoreCase(action)) {
                if (clan != null) plugin.getClanManager().disbandClan(clan, player);
                player.closeInventory();
            } else if ("leave".equalsIgnoreCase(action)) {
                plugin.getClanManager().leaveClan(player);
                player.closeInventory();
            } else if ("kick".equalsIgnoreCase(action)) {
                if (clan != null && !data.isEmpty()) {
                    try {
                        plugin.getClanManager().kickPlayer(clan, player, UUID.fromString(data));
                    } catch (Exception ignored) {}
                }
                plugin.getClanGui().openMembersMenu(player, 0);
            } else if ("transfer".equalsIgnoreCase(action)) {
                if (clan != null && !data.isEmpty()) {
                    try {
                        OfflinePlayer target = Bukkit.getOfflinePlayer(UUID.fromString(data));
                        if (target.isOnline()) {
                            plugin.getClanManager().transferOwnership(clan, player, target.getPlayer());
                            plugin.getClanGui().openMainMenu(player);
                        } else {
                            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-player-not-online"));
                            plugin.getClanGui().openMainMenu(player);
                        }
                    } catch (Exception ignored) {
                        plugin.getClanGui().openMainMenu(player);
                    }
                }
            }
        } else if (slot == 15) {
            plugin.getClanGui().openMainMenu(player);
        }
    }
}
