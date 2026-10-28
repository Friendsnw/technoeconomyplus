package com.techno.economy.gui;

import com.techno.economy.TechnoEconomyPlus;
import org.bukkit.entity.Player;

public class DuelGui {

    private final TechnoEconomyPlus plugin;

    public DuelGui(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        plugin.getMenuLoader().openMenu(player, "duel");
    }
}
