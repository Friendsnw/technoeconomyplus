package com.techno.economy.menu;

import com.techno.economy.TechnoEconomyPlus;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuLoader {

    private final TechnoEconomyPlus plugin;
    private final Map<String, MenuDefinition> menus = new HashMap<>();

    public MenuLoader(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    public void load() {
        menus.clear();
        File menuDir = new File(plugin.getDataFolder(), "menus");
        if (!menuDir.exists()) {
            menuDir.mkdirs();
        }

        String[] defaults = new String[]{
                "bank.yml", "daily.yml", "stock.yml", "company.yml", "check.yml", "duel.yml", "jobs.yml", "trash.yml", "sell.yml", "casino.yml", "auctionhouse.yml", "chestshop.yml"
        };

        for (String def : defaults) {
            File target = new File(menuDir, def);
            if (!target.exists()) {
                try (InputStream in = plugin.getResource("menus/" + def)) {
                    if (in != null) {
                        Files.copy(in, target.toPath());
                    }
                } catch (Exception ignored) {}
            }
        }

        File[] files = menuDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File f : files) {
            try {
                String menuKey = f.getName().substring(0, f.getName().length() - 4).toLowerCase();
                YamlConfiguration yaml;
                try (InputStreamReader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
                    yaml = YamlConfiguration.loadConfiguration(r);
                } catch (Exception ex) {
                    yaml = YamlConfiguration.loadConfiguration(f);
                }
                String title = yaml.getString("menu_title", "&8Menu");
                int size = yaml.getInt("size", 27);
                String openCmd = yaml.getString("open_command", "");

                MenuDefinition def = new MenuDefinition(menuKey, title, size, openCmd);

                ConfigurationSection itemsSec = yaml.getConfigurationSection("items");
                if (itemsSec != null) {
                    for (String itemKey : itemsSec.getKeys(false)) {
                        ConfigurationSection is = itemsSec.getConfigurationSection(itemKey);
                        if (is == null) continue;

                        String matName = is.getString("material", "STONE");
                        Material mat = Material.matchMaterial(matName);
                        if (mat == null) mat = Material.STONE;

                        List<Integer> slots = new ArrayList<>();
                        if (is.contains("slot")) {
                            slots.add(is.getInt("slot"));
                        }
                        if (is.contains("slots")) {
                            slots.addAll(is.getIntegerList("slots"));
                        }

                        String displayName = is.getString("display_name", "");
                        List<String> lore = is.getStringList("lore");
                        int cmd = is.getInt("custom_model_data", 0);
                        List<String> clickCommands = is.getStringList("click_commands");

                        MenuItem item = new MenuItem(itemKey, mat, matName, slots, displayName, lore, cmd, clickCommands);
                        def.addItem(item);
                    }
                }
                menus.put(menuKey, def);
            } catch (Exception ignored) {}
        }
    }

    public boolean openMenu(Player player, String menuName) {
        String key = menuName.toLowerCase();
        MenuDefinition def = menus.get(key);
        if (def == null) {
            if (key.equals("borsa")) def = menus.get("stock");
            else if (key.equals("sirket")) def = menus.get("company");
            else if (key.equals("cek")) def = menus.get("check");
            else if (key.equals("duello")) def = menus.get("duel");
            else if (key.equals("job")) def = menus.get("jobs");
            else if (key.equals("meslek")) def = menus.get("jobs");
            else if (key.equals("cop")) def = menus.get("trash");
        }
        if (def == null) return false;

        String rawTitle = def.getTitle();
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            rawTitle = PlaceholderAPI.setPlaceholders(player, rawTitle);
        }
        Component titleComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(rawTitle);

        Inventory inv = Bukkit.createInventory(new MenuHolder(def.getName()), def.getSize(), titleComponent);

        for (MenuItem item : def.getItems()) {
            ItemStack is = createItemStack(player, item);
            for (int slot : item.getSlots()) {
                if (slot >= 0 && slot < def.getSize()) {
                    inv.setItem(slot, is);
                }
            }
        }

        player.openInventory(inv);
        return true;
    }

    private ItemStack createItemStack(Player player, MenuItem item) {
        ItemStack is = null;

        if (Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            try {
                Class<?> csClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
                Method getInstance = csClass.getMethod("getInstance", String.class);
                Object cs = getInstance.invoke(null, item.getRawMaterial());
                if (cs != null) {
                    Method getItemStack = csClass.getMethod("getItemStack");
                    is = (ItemStack) getItemStack.invoke(cs);
                }
            } catch (Exception ignored) {}
        }

        if (is == null) {
            is = new ItemStack(item.getMaterial());
        } else {
            is = is.clone();
        }

        ItemMeta meta = is.getItemMeta();
        if (meta == null) return is;

        if (item.getCustomModelData() > 0) {
            meta.setCustomModelData(item.getCustomModelData());
        }

        if (!item.getDisplayName().isEmpty()) {
            String name = item.getDisplayName();
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                name = PlaceholderAPI.setPlaceholders(player, name);
            }
            meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name)
                    .decoration(TextDecoration.ITALIC, false));
        }

        if (!item.getLore().isEmpty()) {
            List<Component> loreList = new ArrayList<>();
            for (String line : item.getLore()) {
                if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    line = PlaceholderAPI.setPlaceholders(player, line);
                }
                loreList.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line)
                        .decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(loreList);
        }

        is.setItemMeta(meta);
        return is;
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();

        MenuDefinition def = menus.get(holder.getMenuName().toLowerCase());
        if (def == null) return;

        MenuItem clickedItem = def.getItemBySlot(slot);
        if (clickedItem == null) return;

        for (String cmd : clickedItem.getClickCommands()) {
            executeCommand(player, cmd);
        }
    }

    private void executeCommand(Player player, String rawCmd) {
        String cmd = rawCmd.replace("%player%", player.getName());
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            cmd = PlaceholderAPI.setPlaceholders(player, cmd);
        }

        if (cmd.startsWith("[player] ")) {
            String toExec = cmd.substring(9).trim();
            player.performCommand(toExec);
        } else if (cmd.startsWith("[console] ")) {
            String toExec = cmd.substring(10).trim();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), toExec);
        } else if (cmd.startsWith("[close]")) {
            Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
        } else if (cmd.startsWith("[message] ")) {
            String msg = cmd.substring(10).trim();
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
        } else if (cmd.startsWith("[open_menu] ")) {
            String target = cmd.substring(12).trim();
            openMenu(player, target);
        } else if (cmd.startsWith("[open_checks]")) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getCheckManager().openPlayerChecksMenu(player));
        } else if (cmd.startsWith("[casino_custom_bet]")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                plugin.getSignInputManager().openSignInput(player, input -> {
                    double bet;
                    try {
                        bet = Double.parseDouble(input.trim().replace("$", "").replace(",", "."));
                        if (bet <= 0) throw new IllegalArgumentException();
                    } catch (Exception e) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.invalid-amount", Map.of("amount", input)));
                        Bukkit.getScheduler().runTask(plugin, () -> openMenu(player, "casino"));
                        return;
                    }
                    if (!plugin.getBalanceManager().has(player.getUniqueId(), bet)) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.insufficient-funds"));
                        Bukkit.getScheduler().runTask(plugin, () -> openMenu(player, "casino"));
                        return;
                    }
                    plugin.getBalanceManager().removeBalance(player.getUniqueId(), bet);
                    plugin.getTransactionLogManager().log(player.getUniqueId(), player.getName(), "CASINO", -bet,
                            plugin.getBalanceManager().getBalance(player.getUniqueId()), "Slots bet placed");
                    Bukkit.getScheduler().runTask(plugin, () -> plugin.getCasinoManager().openSlots(player, bet));
                }, () -> Bukkit.getScheduler().runTask(plugin, () -> openMenu(player, "casino")));
            });
        } else if (cmd.startsWith("[bank_deposit]")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                plugin.getSignInputManager().openSignInput(player, input -> {
                    double amount;
                    try {
                        amount = Double.parseDouble(input.trim().replace("$", "").replace(",", "."));
                        if (amount <= 0) throw new IllegalArgumentException();
                    } catch (Exception e) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.invalid-amount", Map.of("amount", input)));
                        Bukkit.getScheduler().runTask(plugin, () -> openMenu(player, "bank"));
                        return;
                    }
                    if (!plugin.getBalanceManager().has(player.getUniqueId(), amount)) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.insufficient-funds"));
                        Bukkit.getScheduler().runTask(plugin, () -> openMenu(player, "bank"));
                        return;
                    }
                    plugin.getBalanceManager().withdraw(player.getUniqueId(), amount);
                    plugin.getBankManager().deposit(player.getUniqueId(), amount);
                    String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
                    player.sendMessage(plugin.getLanguageManager().getMessage("messages.bank-deposit-success", Map.of("amount", String.valueOf(amount), "symbol", symbol)));
                    Bukkit.getScheduler().runTask(plugin, () -> openMenu(player, "bank"));
                }, () -> Bukkit.getScheduler().runTask(plugin, () -> openMenu(player, "bank")));
            });
        } else if (cmd.startsWith("[bank_withdraw]")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                plugin.getSignInputManager().openSignInput(player, input -> {
                    double amount;
                    try {
                        amount = Double.parseDouble(input.trim().replace("$", "").replace(",", "."));
                        if (amount <= 0) throw new IllegalArgumentException();
                    } catch (Exception e) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.invalid-amount", Map.of("amount", input)));
                        Bukkit.getScheduler().runTask(plugin, () -> openMenu(player, "bank"));
                        return;
                    }
                    if (plugin.getBankManager().getBankBalance(player.getUniqueId()) < amount) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.bank-insufficient"));
                        Bukkit.getScheduler().runTask(plugin, () -> openMenu(player, "bank"));
                        return;
                    }
                    plugin.getBankManager().withdraw(player.getUniqueId(), amount);
                    plugin.getBalanceManager().addBalance(player.getUniqueId(), amount);
                    String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
                    player.sendMessage(plugin.getLanguageManager().getMessage("messages.bank-withdraw-success", Map.of("amount", String.valueOf(amount), "symbol", symbol)));
                    Bukkit.getScheduler().runTask(plugin, () -> openMenu(player, "bank"));
                }, () -> Bukkit.getScheduler().runTask(plugin, () -> openMenu(player, "bank")));
            });
        } else if (cmd.startsWith("[check_write]")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                plugin.getSignInputManager().openSignInput(player, input -> {
                    double amount;
                    try {
                        amount = Double.parseDouble(input.trim().replace("$", "").replace(",", "."));
                    } catch (Exception e) {
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.invalid-amount",
                                Map.of("amount", input)));
                        Bukkit.getScheduler().runTask(plugin, () -> openMenu(player, "check"));
                        return;
                    }
                    ItemStack checkItem = plugin.getCheckManager().createCheck(player, amount);
                    if (checkItem != null) {
                        player.getInventory().addItem(checkItem);
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.check-created",
                                Map.of("amount", String.valueOf(amount))));
                    } else {
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.check-create-failed"));
                    }
                    Bukkit.getScheduler().runTask(plugin, () -> openMenu(player, "check"));
                }, () -> {
                    player.sendMessage(plugin.getLanguageManager().getMessage("messages.check-write-cancel"));
                    Bukkit.getScheduler().runTask(plugin, () -> openMenu(player, "check"));
                });
            });
        }
    }

    public static class MenuDefinition {
        private final String name;
        private final String title;
        private final int size;
        private final String openCommand;
        private final List<MenuItem> items = new ArrayList<>();

        public MenuDefinition(String name, String title, int size, String openCommand) {
            this.name = name;
            this.title = title;
            this.size = size;
            this.openCommand = openCommand;
        }

        public void addItem(MenuItem item) {
            items.add(item);
        }

        public MenuItem getItemBySlot(int slot) {
            for (MenuItem item : items) {
                if (item.getSlots().contains(slot)) return item;
            }
            return null;
        }

        public String getName() { return name; }
        public String getTitle() { return title; }
        public int getSize() { return size; }
        public String getOpenCommand() { return openCommand; }
        public List<MenuItem> getItems() { return items; }
    }

    public static class MenuItem {
        private final String key;
        private final Material material;
        private final String rawMaterial;
        private final List<Integer> slots;
        private final String displayName;
        private final List<String> lore;
        private final int customModelData;
        private final List<String> clickCommands;

        public MenuItem(String key, Material material, String rawMaterial, List<Integer> slots, String displayName, List<String> lore, int customModelData, List<String> clickCommands) {
            this.key = key;
            this.material = material;
            this.rawMaterial = rawMaterial;
            this.slots = slots;
            this.displayName = displayName;
            this.lore = lore;
            this.customModelData = customModelData;
            this.clickCommands = clickCommands;
        }

        public String getKey() { return key; }
        public Material getMaterial() { return material; }
        public String getRawMaterial() { return rawMaterial; }
        public List<Integer> getSlots() { return slots; }
        public String getDisplayName() { return displayName; }
        public List<String> getLore() { return lore; }
        public int getCustomModelData() { return customModelData; }
        public List<String> getClickCommands() { return clickCommands; }
    }
}

