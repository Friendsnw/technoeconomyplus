package com.techno.economy.shop;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class WorthManager {

    private final TechnoEconomyPlus plugin;
    private final Map<Material, Double> buyPrices = new HashMap<>();
    private final Map<Material, Double> sellPrices = new HashMap<>();

    public WorthManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        buyPrices.clear();
        sellPrices.clear();

        File dir = new File(plugin.getDataFolder(), "settings");
        if (!dir.exists()) dir.mkdirs();

        File worthsFile = new File(dir, "worths.yml");
        if (!worthsFile.exists()) {
            try (InputStream in = plugin.getResource("settings/worths.yml")) {
                if (in != null) Files.copy(in, worthsFile.toPath());
            } catch (Exception ignored) {}
        }

        if (worthsFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(worthsFile);
            for (String key : yaml.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(key.toUpperCase());
                    double buy = yaml.getDouble(key + ".buy", 0.0);
                    double sell = yaml.getDouble(key + ".sell", 0.0);
                    if (buy > 0) buyPrices.put(mat, buy);
                    if (sell > 0) sellPrices.put(mat, sell);
                } catch (IllegalArgumentException ignored) {}
            }
        }

        if (sellPrices.isEmpty()) {
            loadFallbackPrices();
        }

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("worth");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                try {
                    Material mat = Material.valueOf(key.toUpperCase());
                    double buy = sec.getDouble(key + ".buy", 0.0);
                    double sell = sec.getDouble(key + ".sell", 0.0);
                    if (buy > 0) buyPrices.put(mat, buy);
                    if (sell > 0) sellPrices.put(mat, sell);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    private void loadFallbackPrices() {
        sellPrices.put(Material.DIAMOND, 100.0); buyPrices.put(Material.DIAMOND, 200.0);
        sellPrices.put(Material.EMERALD, 50.0); buyPrices.put(Material.EMERALD, 100.0);
        sellPrices.put(Material.GOLD_INGOT, 25.0); buyPrices.put(Material.GOLD_INGOT, 50.0);
        sellPrices.put(Material.IRON_INGOT, 10.0); buyPrices.put(Material.IRON_INGOT, 20.0);
        sellPrices.put(Material.COAL, 3.0); buyPrices.put(Material.COAL, 6.0);
        sellPrices.put(Material.OAK_LOG, 2.0); buyPrices.put(Material.OAK_LOG, 4.0);
        sellPrices.put(Material.WHEAT, 1.5); buyPrices.put(Material.WHEAT, 3.0);
    }

    public double getBuyPrice(Material material) {
        return buyPrices.getOrDefault(material, 0.0);
    }

    public double getSellPrice(Material material) {
        return sellPrices.getOrDefault(material, 0.0);
    }

    public boolean isSellable(Material material) {
        return sellPrices.getOrDefault(material, 0.0) > 0;
    }
}
