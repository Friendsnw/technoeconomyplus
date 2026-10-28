package com.techno.economy.jobs;

import com.techno.economy.TechnoEconomyPlus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Fish;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.Map;

public class JobsListener implements Listener {

    private final TechnoEconomyPlus plugin;

    public JobsListener(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.getFeatureManager().isEnabled("jobs")) return;
        Player player = event.getPlayer();
        String job = plugin.getJobsManager().getJob(player.getUniqueId());
        if (job.equals("none")) return;
        if (!plugin.getJobsManager().canPerformAction(player.getUniqueId())) return;

        double reward = plugin.getJobsManager().getBlockReward(job, event.getBlock().getType());
        if (reward > 0) payJobReward(player, reward, job);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.getFeatureManager().isEnabled("jobs")) return;
        Player player = event.getPlayer();
        String job = plugin.getJobsManager().getJob(player.getUniqueId());
        if (!job.equals("builder")) return;
        if (!plugin.getJobsManager().canPerformAction(player.getUniqueId())) return;

        double reward = plugin.getJobsManager().getPlaceReward(job);
        if (reward > 0) payJobReward(player, reward, job);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!plugin.getFeatureManager().isEnabled("jobs")) return;
        Player killer = event.getEntity().getKiller();

        if (event.getEntity() instanceof Player victim) {
            if (killer == null) return;
            double bounty = plugin.getBountyManager().claimBounty(victim.getUniqueId());
            if (bounty > 0) {
                plugin.getBalanceManager().addBalance(killer.getUniqueId(), bounty);
                String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
                plugin.getTransactionLogManager().log(killer.getUniqueId(), killer.getName(), "BOUNTY_CLAIM", bounty,
                        plugin.getBalanceManager().getBalance(killer.getUniqueId()), "Claimed bounty on " + victim.getName());
                killer.sendMessage(plugin.getLanguageManager().getMessage("messages.bounty-claimed",
                        Map.of("target", victim.getName(), "amount", symbol + String.format("%.2f", bounty))));
            }
            return;
        }

        if (killer == null) return;
        String job = plugin.getJobsManager().getJob(killer.getUniqueId());
        if (!job.equals("hunter")) return;
        if (!plugin.getJobsManager().canPerformAction(killer.getUniqueId())) return;

        double reward = plugin.getJobsManager().getEntityReward(job, event.getEntity().getType());
        if (reward > 0) payJobReward(killer, reward, job);
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (!plugin.getFeatureManager().isEnabled("jobs")) return;
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Player player = event.getPlayer();
        String job = plugin.getJobsManager().getJob(player.getUniqueId());
        if (!job.equals("fisherman")) return;
        if (!plugin.getJobsManager().canPerformAction(player.getUniqueId())) return;

        if (event.getCaught() instanceof org.bukkit.entity.Item caught) {
            String fishName = caught.getItemStack().getType().name();
            double reward = plugin.getJobsManager().getCatchReward(job, fishName);
            if (reward > 0) payJobReward(player, reward, job);
        }
    }

    private void payJobReward(Player player, double amount, String job) {
        plugin.getBalanceManager().addBalance(player.getUniqueId(), amount);
        plugin.getTransactionLogManager().log(player.getUniqueId(), player.getName(), "JOB", amount,
                plugin.getBalanceManager().getBalance(player.getUniqueId()), "Job: " + job);

        String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
        String formattedAmount = String.format("%.2f", amount);
        Component msg = LegacyComponentSerializer.legacyAmpersand()
                .deserialize("&a+" + symbol + formattedAmount + " &7(" + job + ")");
        player.sendActionBar(msg);
    }
}
