package com.techno.economy.listeners;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatInputListener implements Listener {

    private final TechnoEconomyPlus plugin;

    public ChatInputListener(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getChatInputManager().hasPending(player.getUniqueId())) return;

        event.setCancelled(true);
        String input = event.getMessage();

        plugin.getServer().getScheduler().runTask(plugin, () ->
                plugin.getChatInputManager().handleInput(player.getUniqueId(), input));
    }
}
