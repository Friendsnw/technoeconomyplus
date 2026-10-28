package com.techno.economy.gui;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.entity.Player;

public class CheckGui {

    private final TechnoEconomyPlus plugin;

    public CheckGui(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        plugin.getMenuLoader().openMenu(player, "check");
    }
}
