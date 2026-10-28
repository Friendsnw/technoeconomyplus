package com.techno.economy.clan;

import com.techno.economy.TechnoEconomyPlus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClanManager {

    private final TechnoEconomyPlus plugin;
    private final Map<String, Clan> clans = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerClanMap = new ConcurrentHashMap<>();
    private final Set<String> dirtyClans = ConcurrentHashMap.newKeySet();

    private final Map<UUID, Location> warmupLocations = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> warmupTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> teleportCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> creationCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, String> pendingDisbands = new ConcurrentHashMap<>();

    private final Map<String, String> pendingAllyRequests = new ConcurrentHashMap<>();
    private final Map<String, ClanWar> pendingWarOffers = new ConcurrentHashMap<>();

    private BukkitTask autoSaveTask;
    private BukkitTask recruitTask;
    private BukkitTask warCheckTask;

    public ClanManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void start() {
        loadFromDatabase();

        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveDirty, 6000L, 6000L);
        recruitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkRecruitsPromotion, 1200L, 6000L);
        warCheckTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkActiveWars, 200L, 200L);
    }

    public void stop() {
        if (autoSaveTask != null) autoSaveTask.cancel();
        if (recruitTask != null) recruitTask.cancel();
        if (warCheckTask != null) warCheckTask.cancel();

        for (BukkitTask task : warmupTasks.values()) {
            try { task.cancel(); } catch (Exception ignored) {}
        }
        warmupTasks.clear();
        warmupLocations.clear();

        saveAllSync();
    }

    public void loadFromDatabase() {
        clans.clear();
        playerClanMap.clear();

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return;

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM techno_clans");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    String tag = rs.getString("tag");
                    UUID leader = UUID.fromString(rs.getString("leader_uuid"));
                    int level = rs.getInt("level");
                    long xp = rs.getLong("xp");
                    double bank = rs.getDouble("bank_balance");
                    String hw = rs.getString("home_world");
                    Location home = null;
                    if (hw != null && !hw.isEmpty()) {
                        World w = Bukkit.getWorld(hw);
                        if (w != null) {
                            home = new Location(w, rs.getDouble("home_x"), rs.getDouble("home_y"), rs.getDouble("home_z"),
                                    rs.getFloat("home_yaw"), rs.getFloat("home_pitch"));
                        }
                    }
                    boolean ff = rs.getInt("pvp_friendly_fire") == 1;
                    long createdAt = rs.getLong("created_at");
                    long kills = 0;
                    long deaths = 0;
                    int warWins = 0;
                    int warLosses = 0;
                    try { kills = rs.getLong("kills"); } catch (Exception ignored) {}
                    try { deaths = rs.getLong("deaths"); } catch (Exception ignored) {}
                    try { warWins = rs.getInt("war_wins"); } catch (Exception ignored) {}
                    try { warLosses = rs.getInt("war_losses"); } catch (Exception ignored) {}

                    Clan clan = new Clan(name, tag, leader, level, xp, bank, home, ff, createdAt, kills, deaths, warWins, warLosses);
                    clans.put(name.toLowerCase(), clan);
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM techno_clan_members");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String cName = rs.getString("clan_name");
                    Clan clan = clans.get(cName.toLowerCase());
                    if (clan != null) {
                        UUID u = UUID.fromString(rs.getString("player_uuid"));
                        String pName = rs.getString("player_name");
                        ClanRank rank = ClanRank.fromString(rs.getString("rank"));
                        long joinedAt = rs.getLong("joined_at");
                        ClanMember member = new ClanMember(u, pName, rank, joinedAt);
                        clan.addMember(member);
                        playerClanMap.put(u, cName.toLowerCase());
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM techno_clan_allies");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String cName = rs.getString("clan_name");
                    String ally = rs.getString("ally_name");
                    Clan clan = clans.get(cName.toLowerCase());
                    if (clan != null) {
                        clan.addAlly(ally);
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM techno_clan_enemies");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String cName = rs.getString("clan_name");
                    String enemy = rs.getString("enemy_name");
                    Clan clan = clans.get(cName.toLowerCase());
                    if (clan != null) {
                        clan.addEnemy(enemy);
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM techno_clan_bases");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String cName = rs.getString("clan_name");
                    Clan clan = clans.get(cName.toLowerCase());
                    if (clan != null) {
                        String bName = rs.getString("base_name");
                        String wName = rs.getString("world");
                        World w = Bukkit.getWorld(wName);
                        if (w != null) {
                            Location loc = new Location(w, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                                    rs.getFloat("yaw"), rs.getFloat("pitch"));
                            clan.setBase(bName, loc);
                        }
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM techno_clan_wars WHERE status = 'ACTIVE'");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String ch = rs.getString("challenger_clan");
                    String tg = rs.getString("target_clan");
                    long st = rs.getLong("start_time");
                    long et = rs.getLong("end_time");
                    int ck = rs.getInt("challenger_kills");
                    int tk = rs.getInt("target_kills");
                    String win = rs.getString("winner_clan");
                    ClanWar war = new ClanWar(id, ch, tg, st, et, ck, tk, win, ClanWar.WarStatus.ACTIVE);

                    Clan c1 = clans.get(ch.toLowerCase());
                    Clan c2 = clans.get(tg.toLowerCase());
                    if (c1 != null) c1.setActiveWar(war);
                    if (c2 != null) c2.setActiveWar(war);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load clans from database: " + e.getMessage());
        }
    }

    public void markDirty(Clan clan) {
        if (clan != null) {
            dirtyClans.add(clan.getName().toLowerCase());
        }
    }

    public void saveDirty() {
        if (dirtyClans.isEmpty()) return;
        List<String> list = new ArrayList<>(dirtyClans);
        for (String cName : list) {
            Clan clan = clans.get(cName);
            if (clan != null) {
                saveClanSync(clan);
            }
            dirtyClans.remove(cName);
        }
    }

    public void saveAllSync() {
        for (Clan clan : clans.values()) {
            saveClanSync(clan);
        }
        dirtyClans.clear();
    }

    public void saveClanSync(Clan clan) {
        if (clan == null) return;
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return;

            boolean isRemote = plugin.getDatabaseManager().isRemote();
            String upsertClan = isRemote
                    ? "INSERT INTO techno_clans (name, tag, leader_uuid, level, xp, bank_balance, home_world, home_x, home_y, home_z, home_yaw, home_pitch, pvp_friendly_fire, created_at, kills, deaths, war_wins, war_losses) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE tag=VALUES(tag), leader_uuid=VALUES(leader_uuid), level=VALUES(level), xp=VALUES(xp), bank_balance=VALUES(bank_balance), " +
                    "home_world=VALUES(home_world), home_x=VALUES(home_x), home_y=VALUES(home_y), home_z=VALUES(home_z), home_yaw=VALUES(home_yaw), home_pitch=VALUES(home_pitch), " +
                    "pvp_friendly_fire=VALUES(pvp_friendly_fire), kills=VALUES(kills), deaths=VALUES(deaths), war_wins=VALUES(war_wins), war_losses=VALUES(war_losses);"
                    : "INSERT OR REPLACE INTO techno_clans (name, tag, leader_uuid, level, xp, bank_balance, home_world, home_x, home_y, home_z, home_yaw, home_pitch, pvp_friendly_fire, created_at, kills, deaths, war_wins, war_losses) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

            try (PreparedStatement ps = conn.prepareStatement(upsertClan)) {
                ps.setString(1, clan.getName());
                ps.setString(2, clan.getTag());
                ps.setString(3, clan.getLeaderUuid().toString());
                ps.setInt(4, clan.getLevel());
                ps.setLong(5, clan.getXp());
                ps.setDouble(6, clan.getBankBalance());

                Location h = clan.getHome();
                if (h != null && h.getWorld() != null) {
                    ps.setString(7, h.getWorld().getName());
                    ps.setDouble(8, h.getX());
                    ps.setDouble(9, h.getY());
                    ps.setDouble(10, h.getZ());
                    ps.setFloat(11, h.getYaw());
                    ps.setFloat(12, h.getPitch());
                } else {
                    ps.setString(7, null);
                    ps.setDouble(8, 0.0);
                    ps.setDouble(9, 0.0);
                    ps.setDouble(10, 0.0);
                    ps.setFloat(11, 0.0f);
                    ps.setFloat(12, 0.0f);
                }
                ps.setInt(13, clan.isFriendlyFire() ? 1 : 0);
                ps.setLong(14, clan.getCreatedAt());
                ps.setLong(15, clan.getKills());
                ps.setLong(16, clan.getDeaths());
                ps.setInt(17, clan.getWarWins());
                ps.setInt(18, clan.getWarLosses());
                ps.executeUpdate();
            }

            try (PreparedStatement del = conn.prepareStatement("DELETE FROM techno_clan_members WHERE clan_name = ?")) {
                del.setString(1, clan.getName());
                del.executeUpdate();
            }

            String insertMember = isRemote
                    ? "INSERT INTO techno_clan_members (clan_name, player_uuid, player_name, rank, joined_at) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE rank=VALUES(rank);"
                    : "INSERT OR REPLACE INTO techno_clan_members (clan_name, player_uuid, player_name, rank, joined_at) VALUES (?, ?, ?, ?, ?);";
            try (PreparedStatement ps = conn.prepareStatement(insertMember)) {
                for (ClanMember m : clan.getMemberList()) {
                    ps.setString(1, clan.getName());
                    ps.setString(2, m.getPlayerUuid().toString());
                    ps.setString(3, m.getPlayerName());
                    ps.setString(4, m.getRank().name());
                    ps.setLong(5, m.getJoinedAt());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (PreparedStatement del = conn.prepareStatement("DELETE FROM techno_clan_allies WHERE clan_name = ?")) {
                del.setString(1, clan.getName());
                del.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO techno_clan_allies (clan_name, ally_name) VALUES (?, ?)")) {
                for (String ally : clan.getAllies()) {
                    ps.setString(1, clan.getName());
                    ps.setString(2, ally);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (PreparedStatement del = conn.prepareStatement("DELETE FROM techno_clan_enemies WHERE clan_name = ?")) {
                del.setString(1, clan.getName());
                del.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO techno_clan_enemies (clan_name, enemy_name) VALUES (?, ?)")) {
                for (String enemy : clan.getEnemies()) {
                    ps.setString(1, clan.getName());
                    ps.setString(2, enemy);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (PreparedStatement del = conn.prepareStatement("DELETE FROM techno_clan_bases WHERE clan_name = ?")) {
                del.setString(1, clan.getName());
                del.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO techno_clan_bases (clan_name, base_name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                for (Map.Entry<String, Location> entry : clan.getBases().entrySet()) {
                    Location b = entry.getValue();
                    if (b != null && b.getWorld() != null) {
                        ps.setString(1, clan.getName());
                        ps.setString(2, entry.getKey());
                        ps.setString(3, b.getWorld().getName());
                        ps.setDouble(4, b.getX());
                        ps.setDouble(5, b.getY());
                        ps.setDouble(6, b.getZ());
                        ps.setFloat(7, b.getYaw());
                        ps.setFloat(8, b.getPitch());
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }

            ClanWar war = clan.getActiveWar();
            if (war != null) {
                String upsertWar = isRemote
                        ? "INSERT INTO techno_clan_wars (id, challenger_clan, target_clan, start_time, end_time, challenger_kills, target_kills, winner_clan, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE challenger_kills=VALUES(challenger_kills), target_kills=VALUES(target_kills), winner_clan=VALUES(winner_clan), status=VALUES(status);"
                        : "INSERT OR REPLACE INTO techno_clan_wars (id, challenger_clan, target_clan, start_time, end_time, challenger_kills, target_kills, winner_clan, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
                try (PreparedStatement ps = conn.prepareStatement(upsertWar)) {
                    ps.setString(1, war.getWarId());
                    ps.setString(2, war.getChallengerClan());
                    ps.setString(3, war.getTargetClan());
                    ps.setLong(4, war.getStartTime());
                    ps.setLong(5, war.getEndTime());
                    ps.setInt(6, war.getChallengerKills());
                    ps.setInt(7, war.getTargetKills());
                    ps.setString(8, war.getWinnerClan());
                    ps.setString(9, war.getStatus().name());
                    ps.executeUpdate();
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save clan " + clan.getName() + ": " + e.getMessage());
        }
    }

    public Clan getClan(String name) {
        if (name == null) return null;
        return clans.get(name.toLowerCase());
    }

    public Clan getClanByPlayer(UUID playerUuid) {
        if (playerUuid == null) return null;
        String name = playerClanMap.get(playerUuid);
        return name != null ? clans.get(name) : null;
    }

    public Clan getClanByTag(String tag) {
        if (tag == null) return null;
        for (Clan c : clans.values()) {
            if (c.getTag().equalsIgnoreCase(tag) || LegacyComponentSerializer.legacyAmpersand().serialize(Component.text(c.getTag())).equalsIgnoreCase(tag)) {
                return c;
            }
        }
        return null;
    }

    public boolean isClanNameTaken(String name) {
        return name != null && clans.containsKey(name.toLowerCase());
    }

    public boolean isClanTagTaken(String tag) {
        return getClanByTag(tag) != null;
    }

    public List<Clan> getAllClans() {
        return new ArrayList<>(clans.values());
    }

    public boolean hasPermission(Clan clan, UUID playerUuid, String action) {
        if (clan == null || playerUuid == null) return false;
        if (clan.getLeaderUuid().equals(playerUuid)) return true;

        ClanMember member = clan.getMember(playerUuid);
        if (member == null) return false;

        ClanRank rank = member.getRank();
        List<String> perms = plugin.getConfig().getStringList("clan.ranks.permissions." + rank.name());
        return perms.contains(action.toLowerCase());
    }

    public boolean createClan(Player player, String name, String tag) {
        if (!plugin.getConfig().getBoolean("clan.enabled", true)) {
            sendNotification(player, "messages.feature-disabled", Collections.emptyMap());
            return false;
        }

        if (getClanByPlayer(player.getUniqueId()) != null) {
            sendNotification(player, "messages.clan-already-in-clan", Collections.emptyMap());
            return false;
        }

        int minName = plugin.getConfig().getInt("clan.creation.min-name-length", 3);
        int maxName = plugin.getConfig().getInt("clan.creation.max-name-length", 16);
        if (name == null || name.length() < minName || name.length() > maxName) {
            sendNotification(player, "messages.clan-invalid-name-length", Map.of("min", String.valueOf(minName), "max", String.valueOf(maxName)));
            return false;
        }

        int minTag = plugin.getConfig().getInt("clan.creation.min-tag-length", 2);
        int maxTag = plugin.getConfig().getInt("clan.creation.max-tag-length", 6);
        if (tag == null || tag.length() < minTag || tag.length() > maxTag) {
            sendNotification(player, "messages.clan-invalid-tag-length", Map.of("min", String.valueOf(minTag), "max", String.valueOf(maxTag)));
            return false;
        }

        List<String> nameBlacklist = plugin.getConfig().getStringList("clan.creation.name-blacklist");
        for (String b : nameBlacklist) {
            if (name.toLowerCase().contains(b.toLowerCase())) {
                sendNotification(player, "messages.clan-name-blacklisted", Collections.emptyMap());
                return false;
            }
        }

        List<String> tagBlacklist = plugin.getConfig().getStringList("clan.creation.tag-blacklist");
        for (String b : tagBlacklist) {
            if (tag.toLowerCase().contains(b.toLowerCase())) {
                sendNotification(player, "messages.clan-tag-blacklisted", Collections.emptyMap());
                return false;
            }
        }

        if (isClanNameTaken(name)) {
            sendNotification(player, "messages.clan-name-taken", Map.of("name", name));
            return false;
        }

        if (isClanTagTaken(tag)) {
            sendNotification(player, "messages.clan-tag-taken", Map.of("tag", tag));
            return false;
        }

        long cooldownSeconds = plugin.getConfig().getLong("clan.creation.cooldown-seconds", 300);
        Long lastCreated = creationCooldowns.get(player.getUniqueId());
        if (lastCreated != null) {
            long diff = (System.currentTimeMillis() - lastCreated) / 1000L;
            if (diff < cooldownSeconds) {
                long rem = cooldownSeconds - diff;
                sendNotification(player, "messages.clan-create-cooldown", Map.of("time", String.valueOf(rem)));
                return false;
            }
        }

        double cost = plugin.getConfig().getDouble("clan.creation.cost", 10000.0);
        if (cost > 0) {
            if (!plugin.getBalanceManager().has(player.getUniqueId(), cost)) {
                sendNotification(player, "messages.clan-create-insufficient-funds", Map.of("cost", String.valueOf(cost)));
                return false;
            }
            plugin.getBalanceManager().removeBalance(player.getUniqueId(), cost);
        }

        Clan clan = new Clan(name, tag, player.getUniqueId(), 1, 0, 0.0, null, false, System.currentTimeMillis());
        ClanMember leaderMember = new ClanMember(player.getUniqueId(), player.getName(), ClanRank.LEADER, System.currentTimeMillis());
        clan.addMember(leaderMember);

        clans.put(name.toLowerCase(), clan);
        playerClanMap.put(player.getUniqueId(), name.toLowerCase());
        creationCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        markDirty(clan);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveClanSync(clan));

        sendNotification(player, "messages.clan-created-success", Map.of("name", name, "tag", tag));
        playCustomSound(player, "clan.gui.open-sound", Sound.UI_TOAST_CHALLENGE_COMPLETE);
        return true;
    }

    public boolean disbandClan(Clan clan, Player actor) {
        if (clan == null) return false;
        if (actor != null && !clan.getLeaderUuid().equals(actor.getUniqueId()) && !actor.hasPermission("technoeconomy.clan.admin")) {
            sendNotification(actor, "messages.clan-no-permission-rank", Collections.emptyMap());
            return false;
        }

        String cName = clan.getName();
        for (UUID u : clan.getMembers().keySet()) {
            playerClanMap.remove(u);
            Player p = Bukkit.getPlayer(u);
            if (p != null && p.isOnline()) {
                sendNotification(p, "messages.clan-disbanded-broadcast", Map.of("clan", cName));
            }
        }

        clans.remove(cName.toLowerCase());
        dirtyClans.remove(cName.toLowerCase());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                if (conn == null) return;
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM techno_clans WHERE name = ?")) {
                    ps.setString(1, cName);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM techno_clan_members WHERE clan_name = ?")) {
                    ps.setString(1, cName);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM techno_clan_allies WHERE clan_name = ? OR ally_name = ?")) {
                    ps.setString(1, cName);
                    ps.setString(2, cName);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM techno_clan_enemies WHERE clan_name = ? OR enemy_name = ?")) {
                    ps.setString(1, cName);
                    ps.setString(2, cName);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM techno_clan_bases WHERE clan_name = ?")) {
                    ps.setString(1, cName);
                    ps.executeUpdate();
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to delete clan " + cName + ": " + e.getMessage());
            }
        });

        return true;
    }

    public boolean invitePlayer(Clan clan, Player inviter, Player target) {
        if (clan == null || inviter == null || target == null) return false;

        if (!hasPermission(clan, inviter.getUniqueId(), "invite")) {
            sendNotification(inviter, "messages.clan-no-permission-rank", Collections.emptyMap());
            return false;
        }

        if (getClanByPlayer(target.getUniqueId()) != null) {
            sendNotification(inviter, "messages.clan-target-already-in-clan", Map.of("player", target.getName()));
            return false;
        }

        if (clan.getMemberCount() >= getMaxMembers(clan)) {
            sendNotification(inviter, "messages.clan-members-limit-reached", Collections.emptyMap());
            return false;
        }

        int maxPending = plugin.getConfig().getInt("clan.invite.max-pending-invites", 5);
        if (clan.getPendingInvites().size() >= maxPending) {
            sendNotification(inviter, "messages.clan-invite-max-pending", Collections.emptyMap());
            return false;
        }

        clan.addInvite(target.getUniqueId());

        long timeoutSeconds = plugin.getConfig().getLong("clan.invite.timeout-seconds", 60);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (clan.hasInvited(target.getUniqueId())) {
                clan.removeInvite(target.getUniqueId());
                if (target.isOnline()) {
                    sendNotification(target, "messages.clan-invite-timeout", Map.of("clan", clan.getName()));
                }
            }
        }, timeoutSeconds * 20L);

        sendNotification(inviter, "messages.clan-invite-sent", Map.of("player", target.getName(), "clan", clan.getName()));
        sendNotification(target, "messages.clan-invite-received", Map.of("player", inviter.getName(), "clan", clan.getName()));
        return true;
    }

    public boolean acceptInvite(Player player, String clanName) {
        if (player == null) return false;
        if (getClanByPlayer(player.getUniqueId()) != null) {
            sendNotification(player, "messages.clan-already-in-clan", Collections.emptyMap());
            return false;
        }

        Clan clan = null;
        if (clanName != null && !clanName.isBlank()) {
            clan = getClan(clanName);
        } else {
            for (Clan c : clans.values()) {
                if (c.hasInvited(player.getUniqueId())) {
                    clan = c;
                    break;
                }
            }
        }

        if (clan == null || !clan.hasInvited(player.getUniqueId())) {
            sendNotification(player, "messages.clan-invite-none", Collections.emptyMap());
            return false;
        }

        if (clan.getMemberCount() >= getMaxMembers(clan)) {
            sendNotification(player, "messages.clan-members-limit-reached", Collections.emptyMap());
            clan.removeInvite(player.getUniqueId());
            return false;
        }

        clan.removeInvite(player.getUniqueId());
        ClanMember member = new ClanMember(player.getUniqueId(), player.getName(), ClanRank.RECRUIT, System.currentTimeMillis());
        clan.addMember(member);
        playerClanMap.put(player.getUniqueId(), clan.getName().toLowerCase());

        addXp(clan, plugin.getConfig().getLong("clan.levels.xp-sources.member-join", 50), "member-join");
        markDirty(clan);

        broadcastToClan(clan, "messages.clan-member-joined-broadcast", Map.of("player", player.getName()));
        return true;
    }

    public boolean denyInvite(Player player, String clanName) {
        if (player == null) return false;

        Clan clan = null;
        if (clanName != null && !clanName.isBlank()) {
            clan = getClan(clanName);
        } else {
            for (Clan c : clans.values()) {
                if (c.hasInvited(player.getUniqueId())) {
                    clan = c;
                    break;
                }
            }
        }

        if (clan == null || !clan.hasInvited(player.getUniqueId())) {
            sendNotification(player, "messages.clan-invite-none", Collections.emptyMap());
            return false;
        }

        clan.removeInvite(player.getUniqueId());
        sendNotification(player, "messages.clan-invite-denied", Map.of("clan", clan.getName()));
        return true;
    }

    public boolean kickPlayer(Clan clan, Player actor, UUID targetUuid) {
        if (clan == null || actor == null || targetUuid == null) return false;

        if (!hasPermission(clan, actor.getUniqueId(), "kick")) {
            sendNotification(actor, "messages.clan-no-permission-rank", Collections.emptyMap());
            return false;
        }

        ClanMember targetMember = clan.getMember(targetUuid);
        if (targetMember == null) {
            sendNotification(actor, "messages.clan-member-not-found", Collections.emptyMap());
            return false;
        }

        if (targetUuid.equals(clan.getLeaderUuid())) {
            sendNotification(actor, "messages.clan-cannot-kick-leader", Collections.emptyMap());
            return false;
        }

        ClanMember actorMember = clan.getMember(actor.getUniqueId());
        if (actorMember != null && !actor.getUniqueId().equals(clan.getLeaderUuid())) {
            if (!actorMember.getRank().isHigherThan(targetMember.getRank())) {
                sendNotification(actor, "messages.clan-cannot-kick-higher", Collections.emptyMap());
                return false;
            }
        }

        clan.removeMember(targetUuid);
        playerClanMap.remove(targetUuid);
        markDirty(clan);

        Player tPlayer = Bukkit.getPlayer(targetUuid);
        if (tPlayer != null && tPlayer.isOnline()) {
            sendNotification(tPlayer, "messages.clan-kicked-notice", Map.of("clan", clan.getName(), "actor", actor.getName()));
        }

        broadcastToClan(clan, "messages.clan-kicked-broadcast", Map.of("player", targetMember.getPlayerName(), "actor", actor.getName()));
        return true;
    }

    public boolean promotePlayer(Clan clan, Player actor, UUID targetUuid) {
        if (clan == null || actor == null || targetUuid == null) return false;

        if (!hasPermission(clan, actor.getUniqueId(), "promote")) {
            sendNotification(actor, "messages.clan-no-permission-rank", Collections.emptyMap());
            return false;
        }

        ClanMember targetMember = clan.getMember(targetUuid);
        if (targetMember == null) {
            sendNotification(actor, "messages.clan-member-not-found", Collections.emptyMap());
            return false;
        }

        ClanRank currentRank = targetMember.getRank();
        ClanRank nextRank = currentRank.getPromoteTarget();
        if (nextRank == null || nextRank == ClanRank.LEADER) {
            sendNotification(actor, "messages.clan-cannot-promote-max", Collections.emptyMap());
            return false;
        }

        ClanMember actorMember = clan.getMember(actor.getUniqueId());
        if (!actor.getUniqueId().equals(clan.getLeaderUuid()) && actorMember != null) {
            if (!actorMember.getRank().isHigherThan(nextRank)) {
                sendNotification(actor, "messages.clan-cannot-promote-equal", Collections.emptyMap());
                return false;
            }
        }

        targetMember.setRank(nextRank);
        markDirty(clan);

        broadcastToClan(clan, "messages.clan-promoted-broadcast", Map.of("player", targetMember.getPlayerName(), "rank", nextRank.name()));
        return true;
    }

    public boolean demotePlayer(Clan clan, Player actor, UUID targetUuid) {
        if (clan == null || actor == null || targetUuid == null) return false;

        if (!hasPermission(clan, actor.getUniqueId(), "demote")) {
            sendNotification(actor, "messages.clan-no-permission-rank", Collections.emptyMap());
            return false;
        }

        ClanMember targetMember = clan.getMember(targetUuid);
        if (targetMember == null) {
            sendNotification(actor, "messages.clan-member-not-found", Collections.emptyMap());
            return false;
        }

        ClanRank currentRank = targetMember.getRank();
        ClanRank prevRank = currentRank.getDemoteTarget();
        if (prevRank == null) {
            sendNotification(actor, "messages.clan-cannot-demote-lowest", Collections.emptyMap());
            return false;
        }

        ClanMember actorMember = clan.getMember(actor.getUniqueId());
        if (!actor.getUniqueId().equals(clan.getLeaderUuid()) && actorMember != null) {
            if (!actorMember.getRank().isHigherThan(currentRank)) {
                sendNotification(actor, "messages.clan-cannot-demote-higher", Collections.emptyMap());
                return false;
            }
        }

        targetMember.setRank(prevRank);
        markDirty(clan);

        broadcastToClan(clan, "messages.clan-demoted-broadcast", Map.of("player", targetMember.getPlayerName(), "rank", prevRank.name()));
        return true;
    }

    public boolean transferOwnership(Clan clan, Player leader, Player target) {
        if (clan == null || leader == null || target == null) return false;

        if (!clan.getLeaderUuid().equals(leader.getUniqueId())) {
            sendNotification(leader, "messages.clan-only-leader-transfer", Collections.emptyMap());
            return false;
        }

        if (!clan.isMember(target.getUniqueId())) {
            sendNotification(leader, "messages.clan-member-not-found", Collections.emptyMap());
            return false;
        }

        ClanMember oldLeader = clan.getMember(leader.getUniqueId());
        ClanMember newLeader = clan.getMember(target.getUniqueId());

        if (oldLeader != null) oldLeader.setRank(ClanRank.CO_LEADER);
        if (newLeader != null) newLeader.setRank(ClanRank.LEADER);

        clan.setLeaderUuid(target.getUniqueId());
        markDirty(clan);

        broadcastToClan(clan, "messages.clan-transferred-broadcast", Map.of("old", leader.getName(), "new", target.getName()));
        return true;
    }

    public boolean leaveClan(Player player) {
        if (player == null) return false;
        Clan clan = getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            sendNotification(player, "messages.clan-not-in-clan", Collections.emptyMap());
            return false;
        }

        if (clan.getLeaderUuid().equals(player.getUniqueId())) {
            if (clan.getMemberCount() > 1) {
                sendNotification(player, "messages.clan-leader-cannot-leave", Collections.emptyMap());
                return false;
            } else {
                return disbandClan(clan, player);
            }
        }

        clan.removeMember(player.getUniqueId());
        playerClanMap.remove(player.getUniqueId());
        markDirty(clan);

        sendNotification(player, "messages.clan-left-success", Map.of("clan", clan.getName()));
        broadcastToClan(clan, "messages.clan-member-left-broadcast", Map.of("player", player.getName()));
        return true;
    }

    public boolean setHome(Clan clan, Player player, Location loc) {
        if (clan == null || player == null || loc == null) return false;
        if (!hasPermission(clan, player.getUniqueId(), "sethome")) {
            sendNotification(player, "messages.clan-no-permission-rank", Collections.emptyMap());
            return false;
        }

        clan.setHome(loc);
        markDirty(clan);
        sendNotification(player, "messages.clan-home-set-success", Collections.emptyMap());
        return true;
    }

    public boolean teleportHome(Player player) {
        if (player == null) return false;
        Clan clan = getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            sendNotification(player, "messages.clan-not-in-clan", Collections.emptyMap());
            return false;
        }

        Location home = clan.getHome();
        if (home == null || home.getWorld() == null) {
            sendNotification(player, "messages.clan-home-not-set", Collections.emptyMap());
            return false;
        }

        return executeWarmupTeleport(player, home, "messages.clan-home-teleporting", "messages.clan-home-teleported");
    }

    public boolean setBase(Clan clan, Player player, String baseName, Location loc) {
        if (clan == null || player == null || baseName == null || loc == null) return false;
        if (!hasPermission(clan, player.getUniqueId(), "setbase")) {
            sendNotification(player, "messages.clan-no-permission-rank", Collections.emptyMap());
            return false;
        }

        int maxBases = plugin.getConfig().getInt("clan.limits.max-bases", 3);
        if (!clan.getBases().containsKey(baseName.toLowerCase()) && clan.getBases().size() >= maxBases) {
            sendNotification(player, "messages.clan-base-limit-reached", Map.of("max", String.valueOf(maxBases)));
            return false;
        }

        clan.setBase(baseName, loc);
        markDirty(clan);
        sendNotification(player, "messages.clan-base-set-success", Map.of("base", baseName));
        return true;
    }

    public boolean teleportBase(Player player, String baseName) {
        if (player == null || baseName == null) return false;
        Clan clan = getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            sendNotification(player, "messages.clan-not-in-clan", Collections.emptyMap());
            return false;
        }

        Location base = clan.getBase(baseName);
        if (base == null || base.getWorld() == null) {
            sendNotification(player, "messages.clan-base-not-found", Map.of("base", baseName));
            return false;
        }

        return executeWarmupTeleport(player, base, "messages.clan-base-teleporting", "messages.clan-base-teleported");
    }

    private boolean executeWarmupTeleport(Player player, Location target, String warmupMsgKey, String successMsgKey) {
        long cooldownSeconds = plugin.getConfig().getLong("clan.home.cooldown-seconds", 30);
        Long lastTp = teleportCooldowns.get(player.getUniqueId());
        if (lastTp != null) {
            long diff = (System.currentTimeMillis() - lastTp) / 1000L;
            if (diff < cooldownSeconds) {
                long rem = cooldownSeconds - diff;
                sendNotification(player, "messages.clan-teleport-cooldown", Map.of("time", String.valueOf(rem)));
                return false;
            }
        }

        int warmupSeconds = plugin.getConfig().getInt("clan.home.warmup-seconds", 5);
        if (warmupSeconds <= 0 || player.hasPermission("technoeconomy.clan.admin")) {
            player.teleport(target);
            teleportCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            sendNotification(player, successMsgKey, Collections.emptyMap());
            return true;
        }

        cancelWarmup(player);
        warmupLocations.put(player.getUniqueId(), player.getLocation());

        sendNotification(player, warmupMsgKey, Map.of("seconds", String.valueOf(warmupSeconds)));

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            warmupTasks.remove(player.getUniqueId());
            warmupLocations.remove(player.getUniqueId());
            if (player.isOnline()) {
                player.teleport(target);
                teleportCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                sendNotification(player, successMsgKey, Collections.emptyMap());
                playCustomSound(player, "clan.gui.open-sound", Sound.ENTITY_ENDERMAN_TELEPORT);
            }
        }, warmupSeconds * 20L);

        warmupTasks.put(player.getUniqueId(), task);
        return true;
    }

    public void cancelWarmup(Player player) {
        if (player == null) return;
        BukkitTask t = warmupTasks.remove(player.getUniqueId());
        if (t != null) {
            t.cancel();
            warmupLocations.remove(player.getUniqueId());
            sendNotification(player, "messages.clan-teleport-cancelled", Collections.emptyMap());
        }
    }

    public boolean isWarmingUp(UUID uuid) {
        return uuid != null && warmupTasks.containsKey(uuid);
    }

    public Location getWarmupLocation(UUID uuid) {
        return uuid != null ? warmupLocations.get(uuid) : null;
    }

    public boolean depositBank(Player player, double amount) {
        if (player == null || amount <= 0) return false;
        Clan clan = getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            sendNotification(player, "messages.clan-not-in-clan", Collections.emptyMap());
            return false;
        }

        if (!plugin.getBalanceManager().has(player.getUniqueId(), amount)) {
            sendNotification(player, "messages.insufficient-funds", Collections.emptyMap());
            return false;
        }

        double maxLimit = getBankLimit(clan);
        if (clan.getBankBalance() + amount > maxLimit) {
            sendNotification(player, "messages.clan-bank-limit-reached", Map.of("limit", String.valueOf(maxLimit)));
            return false;
        }

        double taxPercent = plugin.getConfig().getDouble("clan.bank.tax-percent", 2.0);
        double tax = amount * (taxPercent / 100.0);
        double net = amount - tax;

        plugin.getBalanceManager().removeBalance(player.getUniqueId(), amount);
        clan.depositBank(net);
        markDirty(clan);

        long xpDeposit = (long) (amount / 1000.0 * plugin.getConfig().getInt("clan.levels.xp-sources.bank-deposit-per-1000", 1));
        if (xpDeposit > 0) {
            addXp(clan, xpDeposit, "bank-deposit");
        }

        logBankTransaction(clan.getName(), player.getUniqueId(), player.getName(), "DEPOSIT", net);

        sendNotification(player, "messages.clan-bank-deposited", Map.of("amount", String.valueOf(amount), "net", String.valueOf(net), "tax", String.valueOf(tax)));
        broadcastToClan(clan, "messages.clan-bank-deposited-broadcast", Map.of("player", player.getName(), "amount", String.valueOf(net)));
        return true;
    }

    public boolean withdrawBank(Player player, double amount) {
        if (player == null || amount <= 0) return false;
        Clan clan = getClanByPlayer(player.getUniqueId());
        if (clan == null) {
            sendNotification(player, "messages.clan-not-in-clan", Collections.emptyMap());
            return false;
        }

        String reqRankStr = plugin.getConfig().getString("clan.bank.withdraw-permission-rank", "CO_LEADER");
        ClanRank reqRank = ClanRank.fromString(reqRankStr);
        ClanMember member = clan.getMember(player.getUniqueId());
        if (member == null || !member.getRank().isAtLeast(reqRank)) {
            sendNotification(player, "messages.clan-bank-withdraw-no-rank", Map.of("rank", reqRank.name()));
            return false;
        }

        if (clan.getBankBalance() < amount) {
            sendNotification(player, "messages.clan-bank-insufficient-funds", Collections.emptyMap());
            return false;
        }

        clan.withdrawBank(amount);
        plugin.getBalanceManager().addBalance(player.getUniqueId(), amount);
        markDirty(clan);

        logBankTransaction(clan.getName(), player.getUniqueId(), player.getName(), "WITHDRAW", amount);

        sendNotification(player, "messages.clan-bank-withdrawn", Map.of("amount", String.valueOf(amount)));
        broadcastToClan(clan, "messages.clan-bank-withdrawn-broadcast", Map.of("player", player.getName(), "amount", String.valueOf(amount)));
        return true;
    }

    private void logBankTransaction(String clanName, UUID uuid, String pName, String action, double amount) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                if (conn == null) return;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO techno_clan_bank_logs (clan_name, player_uuid, player_name, action, amount, timestamp) VALUES (?, ?, ?, ?, ?, ?)")) {
                    ps.setString(1, clanName);
                    ps.setString(2, uuid.toString());
                    ps.setString(3, pName);
                    ps.setString(4, action);
                    ps.setDouble(5, amount);
                    ps.setLong(6, System.currentTimeMillis());
                    ps.executeUpdate();
                }
            } catch (Exception ignored) {}
        });
    }

    public boolean requestAlly(Clan requester, Clan target, Player player) {
        if (requester == null || target == null || player == null) return false;
        if (requester.getName().equalsIgnoreCase(target.getName())) return false;

        if (!hasPermission(requester, player.getUniqueId(), "ally")) {
            sendNotification(player, "messages.clan-no-permission-rank", Collections.emptyMap());
            return false;
        }

        if (requester.isAlly(target.getName())) {
            sendNotification(player, "messages.clan-already-allies", Map.of("clan", target.getName()));
            return false;
        }

        if (requester.getAllies().size() >= getMaxAllies(requester)) {
            sendNotification(player, "messages.clan-max-allies-reached", Collections.emptyMap());
            return false;
        }

        String pairKey = target.getName().toLowerCase() + ":" + requester.getName().toLowerCase();
        if (pendingAllyRequests.containsKey(pairKey)) {
            pendingAllyRequests.remove(pairKey);
            requester.addAlly(target.getName());
            target.addAlly(requester.getName());
            markDirty(requester);
            markDirty(target);

            broadcastToClan(requester, "messages.clan-allied-broadcast", Map.of("clan", target.getName()));
            broadcastToClan(target, "messages.clan-allied-broadcast", Map.of("clan", requester.getName()));
            return true;
        }

        String myKey = requester.getName().toLowerCase() + ":" + target.getName().toLowerCase();
        pendingAllyRequests.put(myKey, target.getName());
        sendNotification(player, "messages.clan-ally-request-sent", Map.of("clan", target.getName()));

        Player targetLeader = Bukkit.getPlayer(target.getLeaderUuid());
        if (targetLeader != null && targetLeader.isOnline()) {
            sendNotification(targetLeader, "messages.clan-ally-request-received", Map.of("clan", requester.getName()));
        }
        return true;
    }

    public boolean removeAlly(Clan clan, Clan target, Player player) {
        if (clan == null || target == null || player == null) return false;
        if (!hasPermission(clan, player.getUniqueId(), "ally")) {
            sendNotification(player, "messages.clan-no-permission-rank", Collections.emptyMap());
            return false;
        }

        clan.removeAlly(target.getName());
        target.removeAlly(clan.getName());
        markDirty(clan);
        markDirty(target);

        broadcastToClan(clan, "messages.clan-unallied-broadcast", Map.of("clan", target.getName()));
        broadcastToClan(target, "messages.clan-unallied-broadcast", Map.of("clan", clan.getName()));
        return true;
    }

    public boolean declareEnemy(Clan clan, Clan target, Player player) {
        if (clan == null || target == null || player == null) return false;
        if (clan.getName().equalsIgnoreCase(target.getName())) return false;

        if (!hasPermission(clan, player.getUniqueId(), "enemy")) {
            sendNotification(player, "messages.clan-no-permission-rank", Collections.emptyMap());
            return false;
        }

        if (clan.isEnemy(target.getName())) {
            sendNotification(player, "messages.clan-already-enemies", Map.of("clan", target.getName()));
            return false;
        }

        int maxEnemies = plugin.getConfig().getInt("clan.limits.base-max-enemies", 5);
        if (clan.getEnemies().size() >= maxEnemies) {
            sendNotification(player, "messages.clan-max-enemies-reached", Collections.emptyMap());
            return false;
        }

        clan.addEnemy(target.getName());
        markDirty(clan);

        broadcastToClan(clan, "messages.clan-enemy-declared-broadcast", Map.of("clan", target.getName()));
        broadcastToClan(target, "messages.clan-enemy-declared-broadcast", Map.of("clan", clan.getName()));
        return true;
    }

    public boolean removeEnemy(Clan clan, Clan target, Player player) {
        if (clan == null || target == null || player == null) return false;
        if (!hasPermission(clan, player.getUniqueId(), "enemy")) {
            sendNotification(player, "messages.clan-no-permission-rank", Collections.emptyMap());
            return false;
        }

        clan.removeEnemy(target.getName());
        markDirty(clan);

        broadcastToClan(clan, "messages.clan-unenemy-broadcast", Map.of("clan", target.getName()));
        return true;
    }

    public boolean declareWar(Clan challenger, Clan target, Player player) {
        if (challenger == null || target == null || player == null) return false;
        if (!plugin.getConfig().getBoolean("clan.war.enabled", true)) {
            sendNotification(player, "messages.feature-disabled", Collections.emptyMap());
            return false;
        }

        if (!hasPermission(challenger, player.getUniqueId(), "war")) {
            sendNotification(player, "messages.clan-no-permission-rank", Collections.emptyMap());
            return false;
        }

        if (challenger.isInWar() || target.isInWar()) {
            sendNotification(player, "messages.clan-already-in-war", Collections.emptyMap());
            return false;
        }

        int minMembers = plugin.getConfig().getInt("clan.war.min-members", 3);
        if (challenger.getMemberCount() < minMembers || target.getMemberCount() < minMembers) {
            sendNotification(player, "messages.clan-war-min-members", Map.of("min", String.valueOf(minMembers)));
            return false;
        }

        long durationMillis = plugin.getConfig().getLong("clan.war.duration-minutes", 30) * 60000L;
        ClanWar war = new ClanWar(challenger.getName(), target.getName(), durationMillis);
        war.setStatus(ClanWar.WarStatus.PENDING);

        pendingWarOffers.put(target.getName().toLowerCase(), war);

        sendNotification(player, "messages.clan-war-declared-sent", Map.of("clan", target.getName()));
        Player targetLeader = Bukkit.getPlayer(target.getLeaderUuid());
        if (targetLeader != null && targetLeader.isOnline()) {
            sendNotification(targetLeader, "messages.clan-war-declared-received", Map.of("clan", challenger.getName()));
        }
        return true;
    }

    public boolean acceptWar(Clan target, Player player) {
        if (target == null || player == null) return false;
        if (!hasPermission(target, player.getUniqueId(), "war")) {
            sendNotification(player, "messages.clan-no-permission-rank", Collections.emptyMap());
            return false;
        }

        ClanWar war = pendingWarOffers.remove(target.getName().toLowerCase());
        if (war == null) {
            sendNotification(player, "messages.clan-war-no-pending-offers", Collections.emptyMap());
            return false;
        }

        Clan challenger = getClan(war.getChallengerClan());
        if (challenger == null || challenger.isInWar()) {
            sendNotification(player, "messages.clan-war-cancelled", Collections.emptyMap());
            return false;
        }

        war.setStatus(ClanWar.WarStatus.ACTIVE);
        challenger.setActiveWar(war);
        target.setActiveWar(war);

        markDirty(challenger);
        markDirty(target);

        broadcastGlobal("messages.clan-war-started-global", Map.of("clan1", challenger.getName(), "clan2", target.getName()));
        return true;
    }

    public boolean declineWar(Clan target, Player player) {
        if (target == null || player == null) return false;
        ClanWar war = pendingWarOffers.remove(target.getName().toLowerCase());
        if (war == null) {
            sendNotification(player, "messages.clan-war-no-pending-offers", Collections.emptyMap());
            return false;
        }

        sendNotification(player, "messages.clan-war-declined", Map.of("clan", war.getChallengerClan()));
        Clan challenger = getClan(war.getChallengerClan());
        if (challenger != null) {
            broadcastToClan(challenger, "messages.clan-war-declined-other", Map.of("clan", target.getName()));
        }
        return true;
    }

    private void checkActiveWars() {
        for (Clan clan : clans.values()) {
            ClanWar war = clan.getActiveWar();
            if (war != null && war.getStatus() == ClanWar.WarStatus.ACTIVE && war.isExpired()) {
                endWar(war);
            }
        }
    }

    public void endWar(ClanWar war) {
        if (war == null || war.getStatus() == ClanWar.WarStatus.ENDED) return;
        war.setStatus(ClanWar.WarStatus.ENDED);

        Clan c1 = getClan(war.getChallengerClan());
        Clan c2 = getClan(war.getTargetClan());

        String winner = null;
        String loser = null;

        if (war.getChallengerKills() > war.getTargetKills()) {
            winner = war.getChallengerClan();
            loser = war.getTargetClan();
        } else if (war.getTargetKills() > war.getChallengerKills()) {
            winner = war.getTargetClan();
            loser = war.getChallengerClan();
        }

        war.setWinnerClan(winner);

        double winReward = plugin.getConfig().getDouble("clan.war.rewards.winner-coins", 50000.0);
        double losePenalty = plugin.getConfig().getDouble("clan.war.rewards.loser-penalty", 10000.0);
        long winXp = plugin.getConfig().getLong("clan.levels.xp-sources.war-win", 500);
        long loseXp = plugin.getConfig().getLong("clan.levels.xp-sources.war-lose", -100);

        if (winner != null && loser != null) {
            Clan winClan = getClan(winner);
            Clan loseClan = getClan(loser);

            if (winClan != null) {
                winClan.depositBank(winReward);
                winClan.incrementWarWins();
                addXp(winClan, winXp, "war-win");
                markDirty(winClan);
            }
            if (loseClan != null) {
                loseClan.withdrawBank(losePenalty);
                loseClan.incrementWarLosses();
                addXp(loseClan, loseXp, "war-lose");
                markDirty(loseClan);
            }

            broadcastGlobal("messages.clan-war-ended-global", Map.of("winner", winner, "loser", loser, "score1", String.valueOf(war.getChallengerKills()), "score2", String.valueOf(war.getTargetKills())));
        } else {
            broadcastGlobal("messages.clan-war-draw-global", Map.of("clan1", war.getChallengerClan(), "clan2", war.getTargetClan()));
        }

        if (c1 != null) {
            c1.setActiveWar(null);
            markDirty(c1);
        }
        if (c2 != null) {
            c2.setActiveWar(null);
            markDirty(c2);
        }
    }

    public void handleWarKill(Player killer, Player victim) {
        if (killer == null || victim == null) return;
        Clan kClan = getClanByPlayer(killer.getUniqueId());
        Clan vClan = getClanByPlayer(victim.getUniqueId());

        if (kClan != null) {
            addXp(kClan, plugin.getConfig().getLong("clan.levels.xp-sources.member-kill", 10), "kill");
        }
        if (vClan != null) {
            addXp(vClan, plugin.getConfig().getLong("clan.levels.xp-sources.member-death", -5), "death");
        }

        if (kClan != null && vClan != null && kClan.isInWar() && vClan.isInWar()) {
            ClanWar war = kClan.getActiveWar();
            if (war != null && war.involves(vClan.getName())) {
                if (war.getChallengerClan().equalsIgnoreCase(kClan.getName())) {
                    war.addChallengerKill();
                } else {
                    war.addTargetKill();
                }
                broadcastToClan(kClan, "messages.clan-war-kill-notify", Map.of("killer", killer.getName(), "victim", victim.getName()));
                broadcastToClan(vClan, "messages.clan-war-kill-notify", Map.of("killer", killer.getName(), "victim", victim.getName()));
            }
        }
    }

    public void addXp(Clan clan, long amount, String source) {
        if (clan == null || amount == 0) return;
        clan.addXp(amount);

        int maxLevel = plugin.getConfig().getInt("clan.levels.max-level", 50);
        int currentLevel = clan.getLevel();

        while (currentLevel < maxLevel) {
            long req = getXpRequiredForLevel(currentLevel + 1);
            if (clan.getXp() >= req) {
                currentLevel++;
                clan.setLevel(currentLevel);
                broadcastToClan(clan, "messages.clan-level-up-broadcast", Map.of("level", String.valueOf(currentLevel)));
            } else {
                break;
            }
        }
        markDirty(clan);
    }

    public long getXpRequiredForLevel(int level) {
        return (long) (1000.0 * Math.pow(level, 1.5));
    }

    public int getMaxMembers(Clan clan) {
        int base = plugin.getConfig().getInt("clan.limits.base-max-members", 10);
        int perLevel = plugin.getConfig().getInt("clan.limits.max-members-per-level", 2);
        int level = clan != null ? clan.getLevel() : 1;
        return base + (level - 1) * perLevel;
    }

    public int getMaxAllies(Clan clan) {
        int base = plugin.getConfig().getInt("clan.limits.base-max-allies", 3);
        int perLevel = plugin.getConfig().getInt("clan.limits.max-allies-per-level", 1);
        int level = clan != null ? clan.getLevel() : 1;
        return base + (level - 1) * perLevel;
    }

    public double getBankLimit(Clan clan) {
        double base = plugin.getConfig().getDouble("clan.limits.base-bank-limit", 1000000.0);
        double perLevel = plugin.getConfig().getDouble("clan.limits.bank-limit-per-level", 500000.0);
        int level = clan != null ? clan.getLevel() : 1;
        return base + (level - 1) * perLevel;
    }

    private void checkRecruitsPromotion() {
        long hours = plugin.getConfig().getLong("clan.recruit.auto-promote-after-hours", 24);
        if (hours <= 0) return;
        long cutoff = System.currentTimeMillis() - (hours * 3600000L);

        for (Clan clan : clans.values()) {
            boolean changed = false;
            for (ClanMember m : clan.getMemberList()) {
                if (m.getRank() == ClanRank.RECRUIT && m.getJoinedAt() <= cutoff) {
                    m.setRank(ClanRank.MEMBER);
                    changed = true;
                    Player p = Bukkit.getPlayer(m.getPlayerUuid());
                    if (p != null && p.isOnline()) {
                        sendNotification(p, "messages.clan-auto-promoted", Collections.emptyMap());
                    }
                }
            }
            if (changed) {
                markDirty(clan);
            }
        }
    }

    public List<Clan> getTopClans(String type) {
        List<Clan> list = new ArrayList<>(clans.values());
        if ("bank".equalsIgnoreCase(type)) {
            list.sort((c1, c2) -> Double.compare(c2.getBankBalance(), c1.getBankBalance()));
        } else if ("members".equalsIgnoreCase(type)) {
            list.sort((c1, c2) -> Integer.compare(c2.getMemberCount(), c1.getMemberCount()));
        } else if ("kills".equalsIgnoreCase(type)) {
            list.sort((c1, c2) -> Long.compare(c2.getKills(), c1.getKills()));
        } else if ("war_wins".equalsIgnoreCase(type) || "wars".equalsIgnoreCase(type)) {
            list.sort((c1, c2) -> Integer.compare(c2.getWarWins(), c1.getWarWins()));
        } else {
            list.sort((c1, c2) -> {
                int cmp = Integer.compare(c2.getLevel(), c1.getLevel());
                if (cmp != 0) return cmp;
                return Long.compare(c2.getXp(), c1.getXp());
            });
        }
        return list;
    }

    public void sendClanChat(Player sender, Clan clan, String message) {
        if (sender == null || clan == null || message == null) return;
        if (!plugin.getConfig().getBoolean("clan.chat.clan-chat-enabled", true)) {
            sendNotification(sender, "messages.feature-disabled", Collections.emptyMap());
            return;
        }

        ClanMember member = clan.getMember(sender.getUniqueId());
        String rankName = member != null ? member.getRank().name() : "MEMBER";

        String format = plugin.getConfig().getString("clan.chat.clan-chat-format", "&8[&6CLAN&8] &e{rank} &7{player}&8: &f{message}");
        format = format.replace("{rank}", rankName)
                .replace("{player}", sender.getName())
                .replace("{message}", message);

        Component comp = LegacyComponentSerializer.legacyAmpersand().deserialize(format);
        for (UUID u : clan.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && p.isOnline()) {
                p.sendMessage(comp);
            }
        }
    }

    public void sendAllyChat(Player sender, Clan clan, String message) {
        if (sender == null || clan == null || message == null) return;
        if (!plugin.getConfig().getBoolean("clan.chat.ally-chat-enabled", true)) {
            sendNotification(sender, "messages.feature-disabled", Collections.emptyMap());
            return;
        }

        ClanMember member = clan.getMember(sender.getUniqueId());
        String rankName = member != null ? member.getRank().name() : "MEMBER";

        String format = plugin.getConfig().getString("clan.chat.ally-chat-format", "&8[&dALLY&8] &e{clan} &7{player}&8: &f{message}");
        format = format.replace("{clan}", clan.getName())
                .replace("{rank}", rankName)
                .replace("{player}", sender.getName())
                .replace("{message}", message);

        Component comp = LegacyComponentSerializer.legacyAmpersand().deserialize(format);

        for (UUID u : clan.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && p.isOnline()) {
                p.sendMessage(comp);
            }
        }

        for (String allyName : clan.getAllies()) {
            Clan allyClan = getClan(allyName);
            if (allyClan != null) {
                for (UUID u : allyClan.getMembers().keySet()) {
                    Player p = Bukkit.getPlayer(u);
                    if (p != null && p.isOnline()) {
                        p.sendMessage(comp);
                    }
                }
            }
        }
    }

    public void broadcastToClan(Clan clan, String messageKey, Map<String, String> placeholders) {
        if (clan == null) return;
        Component msg = plugin.getLanguageManager().getMessage(messageKey, placeholders);
        for (UUID u : clan.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && p.isOnline()) {
                p.sendMessage(msg);
            }
        }
    }

    public void broadcastGlobal(String messageKey, Map<String, String> placeholders) {
        Component msg = plugin.getLanguageManager().getMessage(messageKey, placeholders);
        Bukkit.broadcast(msg);
    }

    public void sendNotification(Player player, String messageKey, Map<String, String> placeholders) {
        if (player == null) return;
        Component msg = plugin.getLanguageManager().getMessage(messageKey, placeholders);

        String channel = plugin.getConfig().getString("clan.notifications.channel", "CHAT").toUpperCase();
        if ("ACTION_BAR".equals(channel) || "ACTIONBAR".equals(channel)) {
            player.sendActionBar(msg);
        } else if ("TITLE".equals(channel)) {
            player.showTitle(Title.title(msg, Component.empty(), Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(2), Duration.ofMillis(500))));
        } else {
            player.sendMessage(msg);
        }
    }

    public void playCustomSound(Player player, String configPath, Sound fallback) {
        if (player == null) return;
        String soundName = plugin.getConfig().getString(configPath, fallback.name());
        try {
            Sound s = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), s, 1.0f, 1.0f);
        } catch (Exception e) {
            player.playSound(player.getLocation(), fallback, 1.0f, 1.0f);
        }
    }

    public Map<UUID, String> getPendingDisbands() {
        return pendingDisbands;
    }
}
