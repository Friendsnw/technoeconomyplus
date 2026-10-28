package com.techno.economy.clan;

import org.bukkit.Location;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Clan {

    private final String name;
    private String tag;
    private UUID leaderUuid;
    private int level;
    private long xp;
    private double bankBalance;
    private Location home;
    private boolean friendlyFire;
    private final long createdAt;

    private final Map<UUID, ClanMember> members = new ConcurrentHashMap<>();
    private final Set<String> allies = ConcurrentHashMap.newKeySet();
    private final Set<String> enemies = ConcurrentHashMap.newKeySet();
    private final Map<String, Location> bases = new ConcurrentHashMap<>();
    private final Set<UUID> pendingInvites = ConcurrentHashMap.newKeySet();
    private ClanWar activeWar;

    private long kills;
    private long deaths;
    private int warWins;
    private int warLosses;

    public Clan(String name, String tag, UUID leaderUuid, int level, long xp, double bankBalance, Location home, boolean friendlyFire, long createdAt) {
        this.name = name;
        this.tag = tag;
        this.leaderUuid = leaderUuid;
        this.level = Math.max(1, level);
        this.xp = Math.max(0, xp);
        this.bankBalance = Math.max(0.0, bankBalance);
        this.home = home;
        this.friendlyFire = friendlyFire;
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
    }

    public Clan(String name, String tag, UUID leaderUuid, int level, long xp, double bankBalance, Location home, boolean friendlyFire, long createdAt, long kills, long deaths, int warWins, int warLosses) {
        this(name, tag, leaderUuid, level, xp, bankBalance, home, friendlyFire, createdAt);
        this.kills = Math.max(0, kills);
        this.deaths = Math.max(0, deaths);
        this.warWins = Math.max(0, warWins);
        this.warLosses = Math.max(0, warLosses);
    }

    public String getName() {
        return name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public UUID getLeaderUuid() {
        return leaderUuid;
    }

    public void setLeaderUuid(UUID leaderUuid) {
        this.leaderUuid = leaderUuid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public long getXp() {
        return xp;
    }

    public void setXp(long xp) {
        this.xp = Math.max(0, xp);
    }

    public void addXp(long amount) {
        this.xp = Math.max(0, this.xp + amount);
    }

    public double getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(double bankBalance) {
        this.bankBalance = Math.max(0.0, bankBalance);
    }

    public void depositBank(double amount) {
        this.bankBalance += amount;
    }

    public boolean withdrawBank(double amount) {
        if (amount > this.bankBalance) return false;
        this.bankBalance -= amount;
        return true;
    }

    public Location getHome() {
        return home != null ? home.clone() : null;
    }

    public void setHome(Location home) {
        this.home = home != null ? home.clone() : null;
    }

    public boolean isFriendlyFire() {
        return friendlyFire;
    }

    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Map<UUID, ClanMember> getMembers() {
        return members;
    }

    public Collection<ClanMember> getMemberList() {
        return members.values();
    }

    public int getMemberCount() {
        return members.size();
    }

    public void addMember(ClanMember member) {
        if (member != null) {
            members.put(member.getPlayerUuid(), member);
        }
    }

    public void removeMember(UUID uuid) {
        if (uuid != null) {
            members.remove(uuid);
        }
    }

    public boolean isMember(UUID uuid) {
        return uuid != null && members.containsKey(uuid);
    }

    public ClanMember getMember(UUID uuid) {
        return uuid != null ? members.get(uuid) : null;
    }

    public ClanRank getRank(UUID uuid) {
        ClanMember m = getMember(uuid);
        return m != null ? m.getRank() : null;
    }

    public Set<String> getAllies() {
        return allies;
    }

    public void addAlly(String clanName) {
        if (clanName != null && !clanName.equalsIgnoreCase(this.name)) {
            allies.add(clanName);
            enemies.remove(clanName);
        }
    }

    public void removeAlly(String clanName) {
        if (clanName != null) {
            allies.remove(clanName);
        }
    }

    public boolean isAlly(String clanName) {
        return clanName != null && allies.contains(clanName);
    }

    public Set<String> getEnemies() {
        return enemies;
    }

    public void addEnemy(String clanName) {
        if (clanName != null && !clanName.equalsIgnoreCase(this.name)) {
            enemies.add(clanName);
            allies.remove(clanName);
        }
    }

    public void removeEnemy(String clanName) {
        if (clanName != null) {
            enemies.remove(clanName);
        }
    }

    public boolean isEnemy(String clanName) {
        return clanName != null && enemies.contains(clanName);
    }

    public Map<String, Location> getBases() {
        return bases;
    }

    public Location getBase(String name) {
        if (name == null) return null;
        Location loc = bases.get(name.toLowerCase());
        return loc != null ? loc.clone() : null;
    }

    public void setBase(String name, Location location) {
        if (name != null && location != null) {
            bases.put(name.toLowerCase(), location.clone());
        }
    }

    public void removeBase(String name) {
        if (name != null) {
            bases.remove(name.toLowerCase());
        }
    }

    public Set<UUID> getPendingInvites() {
        return pendingInvites;
    }

    public void addInvite(UUID playerUuid) {
        if (playerUuid != null) {
            pendingInvites.add(playerUuid);
        }
    }

    public void removeInvite(UUID playerUuid) {
        if (playerUuid != null) {
            pendingInvites.remove(playerUuid);
        }
    }

    public boolean hasInvited(UUID playerUuid) {
        return playerUuid != null && pendingInvites.contains(playerUuid);
    }

    public ClanWar getActiveWar() {
        return activeWar;
    }

    public void setActiveWar(ClanWar activeWar) {
        this.activeWar = activeWar;
    }

    public boolean isInWar() {
        return activeWar != null && activeWar.getStatus() == ClanWar.WarStatus.ACTIVE && !activeWar.isExpired();
    }

    public ClanRank getMemberRank(UUID uuid) {
        ClanMember m = getMember(uuid);
        return m != null ? m.getRank() : null;
    }

    public long getKills() {
        return kills;
    }

    public void setKills(long kills) {
        this.kills = Math.max(0, kills);
    }

    public void incrementKills() {
        this.kills++;
    }

    public long getDeaths() {
        return deaths;
    }

    public void setDeaths(long deaths) {
        this.deaths = Math.max(0, deaths);
    }

    public void incrementDeaths() {
        this.deaths++;
    }

    public double getKdr() {
        if (deaths == 0) return kills;
        return (double) kills / (double) deaths;
    }

    public int getWarWins() {
        return warWins;
    }

    public void setWarWins(int warWins) {
        this.warWins = Math.max(0, warWins);
    }

    public void incrementWarWins() {
        this.warWins++;
    }

    public int getWarLosses() {
        return warLosses;
    }

    public void setWarLosses(int warLosses) {
        this.warLosses = Math.max(0, warLosses);
    }

    public void incrementWarLosses() {
        this.warLosses++;
    }

    public Location getHomeLocation() {
        return home != null ? home.clone() : null;
    }

    public Location getBaseLocation(String name) {
        return getBase(name);
    }
}
