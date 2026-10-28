package com.techno.economy.hud;

import com.techno.economy.TechnoEconomyPlus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HudManager {

    private final TechnoEconomyPlus plugin;
    private BukkitTask task;
    private volatile boolean globalEnabled = true;
    private volatile boolean showHealth = true;
    private volatile boolean showFood = true;
    private volatile boolean showTime = true;
    private final ConcurrentHashMap<UUID, Boolean> playerHudState = new ConcurrentHashMap<>();

    public HudManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void start() {
        loadConfigValues();
        if (!globalEnabled) return;
        int interval = plugin.getConfig().getInt("hud.update-interval-ticks", 20);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }

    public void loadConfigValues() {
        globalEnabled = plugin.getConfig().getBoolean("hud.enabled", true);
        showHealth = plugin.getConfig().getBoolean("hud.show-health", true);
        showFood = plugin.getConfig().getBoolean("hud.show-food", true);
        showTime = plugin.getConfig().getBoolean("hud.show-time", true);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void reload() {
        stop();
        start();
    }

    public boolean isGlobalEnabled() {
        return globalEnabled;
    }

    public void setGlobalHudEnabled(boolean enabled) {
        this.globalEnabled = enabled;
        if (Bukkit.isPrimaryThread()) {
            plugin.getConfig().set("hud.enabled", enabled);
            plugin.saveConfig();
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getConfig().set("hud.enabled", enabled);
                plugin.saveConfig();
            });
        }
        if (!enabled) {
            stop();
        } else if (task == null) {
            start();
        }
    }

    private void tick() {
        if (!globalEnabled) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isHudEnabled(player.getUniqueId())) continue;
            sendHud(player);
        }
    }

    private void sendHud(Player player) {
        StringBuilder sb = new StringBuilder();

        if (showHealth) {
            double hp = Math.round(player.getHealth() * 10.0) / 10.0;
            double maxHp = 20.0;
            var attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attr != null) maxHp = Math.round(attr.getValue() * 10.0) / 10.0;
            sb.append("§c❤ ").append(hp).append("§7/§c").append((int) maxHp);
        }

        if (showFood) {
            if (sb.length() > 0) sb.append("  ");
            sb.append("§e🍖 ").append(player.getFoodLevel()).append("§7/§e20");
        }

        if (showTime) {
            if (sb.length() > 0) sb.append("  ");
            long time = player.getWorld().getTime();
            int hours = (int) ((time / 1000 + 6) % 24);
            int minutes = (int) ((time % 1000) * 60 / 1000);
            sb.append("§b⏱ ").append(String.format("%02d:%02d", hours, minutes));
        }

        if (sb.length() > 0) {
            Component actionbar = LegacyComponentSerializer.legacySection().deserialize(sb.toString());
            player.sendActionBar(actionbar);
        }
    }

    public boolean isHudEnabled(UUID uuid) {
        if (!globalEnabled) return false;
        return playerHudState.getOrDefault(uuid, true);
    }

    public void setHudEnabled(UUID uuid, boolean enabled) {
        playerHudState.put(uuid, enabled);
    }
}
