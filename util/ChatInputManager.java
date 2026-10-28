package com.techno.economy.util;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatInputManager {

    private final TechnoEconomyPlus plugin;
    private final Map<UUID, PendingInput> pending = new ConcurrentHashMap<>();

    public ChatInputManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void awaitInput(Player player, Consumer<String> callback, Runnable onCancel) {
        pending.put(player.getUniqueId(), new PendingInput(callback, onCancel,
                System.currentTimeMillis() + 30_000L));
        player.sendMessage("§ePlease type the amount or price in chat (Type 'cancel' to abort):");

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            PendingInput pi = pending.remove(player.getUniqueId());
            if (pi != null && pi.onCancel != null) pi.onCancel.run();
        }, 20L * 30);
    }

    public boolean hasPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public void handleInput(UUID uuid, String input) {
        PendingInput pi = pending.remove(uuid);
        if (pi != null) {
            if (input.trim().equalsIgnoreCase("iptal") || input.trim().equalsIgnoreCase("cancel")) {
                if (pi.onCancel != null) pi.onCancel.run();
                return;
            }
            pi.callback.accept(input);
        }
    }

    public void cancel(UUID uuid) {
        PendingInput pi = pending.remove(uuid);
        if (pi != null && pi.onCancel != null) pi.onCancel.run();
    }

    public static class PendingInput {
        final Consumer<String> callback;
        final Runnable onCancel;
        final long expireAt;

        PendingInput(Consumer<String> callback, Runnable onCancel, long expireAt) {
            this.callback = callback;
            this.onCancel = onCancel;
            this.expireAt = expireAt;
        }
    }
}
