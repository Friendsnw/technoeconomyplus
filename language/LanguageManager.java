package com.techno.economy.language;

import com.techno.economy.TechnoEconomyPlus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    private final TechnoEconomyPlus plugin;
    private YamlConfiguration langConfig;
    private final Map<String, String> messageCache = new HashMap<>();
    private String prefix = "&bᴛᴇᴄʜɴᴏᴇᴄᴏɴᴏᴍʏ&6ᴘʟᴜꜱ &7» &r";

    public LanguageManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        String lang = plugin.getConfig().getString("language", "en");

        File langDir = new File(plugin.getDataFolder(), "languages");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        for (String bundled : new String[]{"tr.yml", "en.yml", "fr.yml", "de.yml", "ru.yml", "it.yml", "pt.yml"}) {
            File target = new File(langDir, bundled);
            if (!target.exists()) {
                plugin.saveResource("languages/" + bundled, false);
            }
        }

        File langFile = new File(langDir, lang + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file not found: " + lang + ".yml, falling back to en.yml");
            langFile = new File(langDir, "en.yml");
        }

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(langFile), StandardCharsets.UTF_8)) {
            langConfig = YamlConfiguration.loadConfiguration(reader);
        } catch (Exception e) {
            langConfig = YamlConfiguration.loadConfiguration(langFile);
        }

        InputStream defaultStream = plugin.getResource("languages/" + lang + ".yml");
        if (defaultStream == null) {
            defaultStream = plugin.getResource("languages/en.yml");
        }
        if (defaultStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            langConfig.setDefaults(defaults);
        }

        prefix = langConfig.getString("prefix", "&bᴛᴇᴄʜɴᴏᴇᴄᴏɴᴏᴍʏ&6ᴘʟᴜꜱ &7» &r");
        messageCache.clear();
    }

    public String getPrefix() {
        return prefix;
    }

    public String getRawMessage(String key) {
        if (messageCache.containsKey(key)) {
            return messageCache.get(key);
        }
        String msg = langConfig.getString(key, "&cMissing message: " + key);
        if (shouldApplyPrefix(key)) {
            msg = prefix + msg;
        }
        messageCache.put(key, msg);
        return msg;
    }

    private boolean shouldApplyPrefix(String key) {
        if (prefix == null || prefix.isEmpty()) return false;
        if (!key.startsWith("messages.")) return false;
        String sub = key.substring(9).toLowerCase();
        if (sub.endsWith("-header") || sub.endsWith("-entry") || sub.contains("lore") || sub.contains("item-name")) return false;
        if (sub.equals("worth-not-sellable")) return false;
        return true;
    }

    public String getRawMessage(String key, Map<String, String> replacements) {
        String raw = getRawMessage(key);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return raw;
    }

    public Component getMessage(String key) {
        String raw = getRawMessage(key);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
    }

    public Component getMessage(String key, Map<String, String> replacements) {
        String raw = getRawMessage(key, replacements);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
    }

    public String getLegacyMessage(String key, Map<String, String> replacements) {
        return getRawMessage(key, replacements).replace("&", "§");
    }

    public String getCurrentLanguage() {
        return plugin.getConfig().getString("language", "en");
    }

    public boolean isTurkish() {
        return false;
    }
}
