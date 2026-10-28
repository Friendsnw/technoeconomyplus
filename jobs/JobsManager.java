package com.techno.economy.jobs;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JobsManager {

    private final TechnoEconomyPlus plugin;
    private final ConcurrentHashMap<UUID, String> playerJobs = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> actionCooldowns = new ConcurrentHashMap<>();
    private final Map<String, Map<Material, Double>> blockRewards = new HashMap<>();
    private final Map<String, Map<EntityType, Double>> entityRewards = new HashMap<>();
    private final Map<String, Map<String, Double>> catchRewards = new HashMap<>();
    private final Map<String, Double> placeRewards = new HashMap<>();

    public JobsManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        loadJobsFromDB();
        loadJobDefinitions();
    }

    private void loadJobsFromDB() {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return;
            try (PreparedStatement ps = conn.prepareStatement("SELECT player_uuid, job FROM techno_jobs;");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        playerJobs.put(UUID.fromString(rs.getString("player_uuid")), rs.getString("job"));
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    public void loadJobDefinitions() {
        blockRewards.clear();
        entityRewards.clear();
        catchRewards.clear();
        placeRewards.clear();

        File dir = new File(plugin.getDataFolder(), "settings");
        if (!dir.exists()) dir.mkdirs();

        File jobsFile = new File(dir, "jobs.yml");
        if (!jobsFile.exists()) {
            try (InputStream in = plugin.getResource("settings/jobs.yml")) {
                if (in != null) Files.copy(in, jobsFile.toPath());
            } catch (Exception ignored) {}
        }

        if (!jobsFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(jobsFile);
        ConfigurationSection jobs = yaml.getConfigurationSection("jobs");
        if (jobs == null) return;

        for (String jobName : jobs.getKeys(false)) {
            ConfigurationSection jobSec = jobs.getConfigurationSection(jobName);
            if (jobSec == null) continue;

            ConfigurationSection blockSec = jobSec.getConfigurationSection("block-rewards");
            if (blockSec != null) {
                Map<Material, Double> bMap = new HashMap<>();
                for (String matName : blockSec.getKeys(false)) {
                    try {
                        Material mat = Material.valueOf(matName.toUpperCase());
                        bMap.put(mat, blockSec.getDouble(matName));
                    } catch (Exception ignored) {}
                }
                blockRewards.put(jobName, bMap);
            }

            ConfigurationSection entSec = jobSec.getConfigurationSection("entity-rewards");
            if (entSec != null) {
                Map<EntityType, Double> eMap = new HashMap<>();
                for (String entName : entSec.getKeys(false)) {
                    try {
                        EntityType et = EntityType.valueOf(entName.toUpperCase());
                        eMap.put(et, entSec.getDouble(entName));
                    } catch (Exception ignored) {}
                }
                entityRewards.put(jobName, eMap);
            }

            ConfigurationSection catchSec = jobSec.getConfigurationSection("catch-rewards");
            if (catchSec != null) {
                Map<String, Double> cMap = new HashMap<>();
                for (String fishName : catchSec.getKeys(false)) {
                    cMap.put(fishName.toUpperCase(), catchSec.getDouble(fishName));
                }
                catchRewards.put(jobName, cMap);
            }

            if (jobSec.contains("place-reward")) {
                placeRewards.put(jobName, jobSec.getDouble("place-reward"));
            }
        }
    }

    public String getJob(UUID uuid) {
        return playerJobs.getOrDefault(uuid, "none");
    }

    public void setJob(UUID uuid, String job) {
        playerJobs.put(uuid, job.toLowerCase());
        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveJob(uuid, job.toLowerCase()));
        } else {
            saveJob(uuid, job.toLowerCase());
        }
    }

    private void saveJob(UUID uuid, String job) {
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return;
            boolean isRemote = plugin.getDatabaseManager().isRemote();
            String sql = isRemote
                    ? "INSERT INTO techno_jobs (player_uuid, job) VALUES (?, ?) ON DUPLICATE KEY UPDATE job = VALUES(job);"
                    : "INSERT OR REPLACE INTO techno_jobs (player_uuid, job) VALUES (?, ?);";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, job);
                ps.executeUpdate();
            }
        } catch (Exception ignored) {}
    }

    public double getBlockReward(String job, Material block) {
        Map<Material, Double> map = blockRewards.get(job);
        if (map == null) return 0.0;
        return map.getOrDefault(block, 0.0);
    }

    public double getEntityReward(String job, EntityType type) {
        Map<EntityType, Double> map = entityRewards.get(job);
        if (map == null) return 0.0;
        return map.getOrDefault(type, 0.0);
    }

    public double getPlaceReward(String job) {
        return placeRewards.getOrDefault(job, 0.0);
    }

    public double getCatchReward(String job, String fishName) {
        Map<String, Double> map = catchRewards.get(job);
        if (map == null) return 0.0;
        return map.getOrDefault(fishName.toUpperCase(), 0.0);
    }

    public boolean canPerformAction(UUID uuid) {
        long now = System.currentTimeMillis();
        long last = actionCooldowns.getOrDefault(uuid, 0L);
        long cooldownMs = plugin.getConfig().getLong("jobs.action-cooldown-ms", 250);
        if (now - last < cooldownMs) return false;
        actionCooldowns.put(uuid, now);
        return true;
    }

    public void save() {
        for (Map.Entry<UUID, String> entry : playerJobs.entrySet()) {
            saveJob(entry.getKey(), entry.getValue());
        }
    }
}
