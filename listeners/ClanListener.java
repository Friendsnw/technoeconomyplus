package com.techno.economy.listeners;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.clan.Clan;
import com.techno.economy.clan.ClanMember;
import com.techno.economy.clan.ClanRank;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ClanListener implements Listener {

    private final TechnoEconomyPlus plugin;
    private final Set<UUID> dailyActiveGiven = new HashSet<>();

    public ClanListener(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker != null && !attacker.getUniqueId().equals(victim.getUniqueId())) {
            Clan attackerClan = plugin.getClanManager().getClanByPlayer(attacker.getUniqueId());
            Clan victimClan = plugin.getClanManager().getClanByPlayer(victim.getUniqueId());

            if (attackerClan != null && victimClan != null) {
                if (attackerClan.getName().equalsIgnoreCase(victimClan.getName())) {
                    boolean disabled = plugin.getConfig().getBoolean("clan.war.friendly-fire-disabled", true);
                    if (disabled) {
                        event.setCancelled(true);
                        attacker.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-friendly-fire-blocked"));
                        return;
                    }
                }

                if (attackerClan.isAlly(victimClan.getName())) {
                    boolean allyDisabled = plugin.getConfig().getBoolean("clan.relations.ally-damage-disabled", true);
                    if (allyDisabled) {
                        event.setCancelled(true);
                        attacker.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-ally-fire-blocked"));
                        return;
                    }
                }
            }
        }

        if (plugin.getConfig().getBoolean("clan.home.cancel-on-damage", true)) {
            if (plugin.getClanManager().isWarmingUp(victim.getUniqueId())) {
                plugin.getClanManager().cancelWarmup(victim);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("clan.home.cancel-on-move", true)) return;

        Player player = event.getPlayer();
        if (!plugin.getClanManager().isWarmingUp(player.getUniqueId())) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            plugin.getClanManager().cancelWarmup(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        Clan victimClan = plugin.getClanManager().getClanByPlayer(victim.getUniqueId());
        Clan killerClan = killer != null ? plugin.getClanManager().getClanByPlayer(killer.getUniqueId()) : null;

        if (victimClan != null) {
            victimClan.incrementDeaths();
            long deathXp = plugin.getConfig().getLong("clan.levels.xp-sources.member-death", -5);
            if (deathXp != 0) {
                plugin.getClanManager().addXp(victimClan, deathXp, "death");
            }
            plugin.getClanManager().markDirty(victimClan);
        }

        if (killerClan != null && killer != null) {
            if (victimClan == null || !victimClan.getName().equalsIgnoreCase(killerClan.getName())) {
                killerClan.incrementKills();
                long killXp = plugin.getConfig().getLong("clan.levels.xp-sources.member-kill", 10);
                if (killXp > 0) {
                    plugin.getClanManager().addXp(killerClan, killXp, "kill");
                }
                plugin.getClanManager().markDirty(killerClan);
            }
        }

        if (killer != null) {
            plugin.getClanManager().handleWarKill(killer, victim);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Clan clan = plugin.getClanManager().getClanByPlayer(player.getUniqueId());
        if (clan == null) return;

        long autoHours = plugin.getConfig().getLong("clan.recruit.auto-promote-hours", 72);
        ClanMember member = clan.getMember(player.getUniqueId());
        if (member != null && member.getRank() == ClanRank.RECRUIT && autoHours > 0) {
            long diffMillis = System.currentTimeMillis() - member.getJoinedAt();
            long diffHours = diffMillis / (1000 * 60 * 60);
            if (diffHours >= autoHours) {
                member.setRank(ClanRank.MEMBER);
                plugin.getClanManager().markDirty(clan);
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-recruit-promoted"));
            }
        }

        long dailyActiveXp = plugin.getConfig().getLong("clan.levels.xp-sources.daily-active", 25);
        if (dailyActiveXp > 0 && !dailyActiveGiven.contains(player.getUniqueId())) {
            dailyActiveGiven.add(player.getUniqueId());
            plugin.getClanManager().addXp(clan, dailyActiveXp, "daily_active");
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.clan-daily-xp-reward", Map.of("xp", String.valueOf(dailyActiveXp))));
        }
    }
}
