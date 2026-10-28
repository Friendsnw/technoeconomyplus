package com.techno.economy.feature;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

public class FeatureManager {

    private final TechnoEconomyPlus plugin;
    private final Map<String, Boolean> features = new LinkedHashMap<>();

    public FeatureManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        features.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("features");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                features.put(key, sec.getBoolean(key, true));
            }
        }

        boolean tr = "tr".equalsIgnoreCase(plugin.getConfig().getString("language", "en"));
        plugin.getLogger().info(tr ? "--- Özellikler ---" : "--- Features ---");
        for (Map.Entry<String, Boolean> entry : features.entrySet()) {
            String state = entry.getValue()
                    ? (tr ? "AKTIF" : "ENABLED")
                    : (tr ? "PASIF" : "DISABLED");
            plugin.getLogger().info("  " + entry.getKey() + " → " + state);
        }
    }

    public boolean isEnabled(String feature) {
        return features.getOrDefault(feature, true);
    }

    public Map<String, Boolean> getAll() {
        return features;
    }
}
