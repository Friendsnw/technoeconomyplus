package com.techno.economy.listeners;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.util.SignInputManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class SignInputListener implements Listener {

    private final TechnoEconomyPlus plugin;

    public SignInputListener(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getSignInputManager().hasSession(player.getUniqueId())) return;

        SignInputManager.SignSession session = plugin.getSignInputManager().removeSession(player.getUniqueId());
        if (session == null) return;

        event.setCancelled(true);
        session.originalState.update(true, false);

        String entered = "";
        for (int i = 0; i < 4; i++) {
            var lineComp = event.line(i);
            if (lineComp != null) {
                String plain = PlainTextComponentSerializer.plainText().serialize(lineComp).trim();
                if (!plain.isEmpty() && !plain.contains("^^^^") && !plain.equalsIgnoreCase("Miktar Giriniz") && !plain.equalsIgnoreCase("Enter Amount")) {
                    entered = plain;
                    break;
                }
            }
        }

        final String result = entered;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (result.isEmpty()) {
                if (session.onCancel != null) session.onCancel.run();
            } else {
                session.callback.accept(result);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        SignInputManager.SignSession s = plugin.getSignInputManager().removeSession(event.getPlayer().getUniqueId());
        if (s != null) {
            s.originalState.update(true, false);
        }
    }
}
