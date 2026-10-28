package com.techno.economy.util;

import com.techno.economy.TechnoEconomyPlus;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SignInputManager {

    private final TechnoEconomyPlus plugin;
    private final Map<UUID, SignSession> sessions = new ConcurrentHashMap<>();

    public static class SignSession {
        public final Location loc;
        public final BlockState originalState;
        public final Consumer<String> callback;
        public final Runnable onCancel;

        public SignSession(Location loc, BlockState originalState, Consumer<String> callback, Runnable onCancel) {
            this.loc = loc;
            this.originalState = originalState;
            this.callback = callback;
            this.onCancel = onCancel;
        }
    }

    public SignInputManager(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void openSignInput(Player player, Consumer<String> callback, Runnable onCancel) {
        try {
            Location loc = player.getLocation().clone().add(0, 3, 0);
            Block block = loc.getBlock();
            BlockState original = block.getState();

            sessions.put(player.getUniqueId(), new SignSession(loc, original, callback, onCancel));

            block.setType(Material.OAK_SIGN, false);
            Sign sign = (Sign) block.getState();
            sign.line(0, Component.text(""));
            sign.line(1, Component.text("^^^^^^^^^^^^^^^"));
            sign.line(2, Component.text("Enter Amount"));
            sign.update(true, false);

            player.closeInventory();
            player.openSign(sign);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                SignSession s = sessions.remove(player.getUniqueId());
                if (s != null) {
                    s.originalState.update(true, false);
                    if (s.onCancel != null) s.onCancel.run();
                }
            }, 20L * 45);
        } catch (Exception e) {
            plugin.getChatInputManager().awaitInput(player, callback, onCancel);
        }
    }

    public boolean hasSession(UUID uuid) {
        return sessions.containsKey(uuid);
    }

    public SignSession getSession(UUID uuid) {
        return sessions.get(uuid);
    }

    public SignSession removeSession(UUID uuid) {
        return sessions.remove(uuid);
    }
}
