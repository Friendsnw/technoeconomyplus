package com.techno.economy.listeners;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MobHealthListener implements Listener {

    private final TechnoEconomyPlus plugin;
    private final Map<UUID, Long> throttle = new ConcurrentHashMap<>();

    public MobHealthListener(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.getFeatureManager().isEnabled("mob-health-display")) return;
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity instanceof Player) return;

        long now = System.currentTimeMillis();
        Long last = throttle.get(entity.getUniqueId());
        if (last != null && now - last < 150) return;
        throttle.put(entity.getUniqueId(), now);

        updateName(entity);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegen(EntityRegainHealthEvent event) {
        if (!plugin.getFeatureManager().isEnabled("mob-health-display")) return;
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity instanceof Player) return;

        long now = System.currentTimeMillis();
        Long last = throttle.get(entity.getUniqueId());
        if (last != null && now - last < 150) return;
        throttle.put(entity.getUniqueId(), now);

        updateName(entity);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) return;
        throttle.remove(entity.getUniqueId());
        entity.setCustomNameVisible(false);
        entity.customName(null);
    }

    private void updateName(LivingEntity entity) {
        if (!entity.isValid() || entity.isDead()) return;
        double hp = Math.round(entity.getHealth() * 10.0) / 10.0;
        int maxHp = 20;
        var attr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) maxHp = (int) attr.getValue();
        entity.setCustomNameVisible(true);

        net.kyori.adventure.text.Component name = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize("§c❤ " + hp + "§7/§c" + maxHp);
        entity.customName(name);
    }
}
