package com.techno.economy.listeners;

import com.techno.economy.TechnoEconomyPlus;
import com.techno.economy.shop.ChestShopHolder;
import com.techno.economy.shop.ChestShopManager;
import com.techno.economy.shop.ChestShopManager.ChestShopData;
import com.techno.economy.shop.ChestShopManager.ShopProduct;
import com.techno.economy.util.Format;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class ChestShopListener implements Listener {

    private final TechnoEconomyPlus plugin;

    public ChestShopListener(TechnoEconomyPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!plugin.getChestShopManager().isShop(block.getLocation())) return;

        Player player = event.getPlayer();
        ChestShopData shop = plugin.getChestShopManager().getShop(block.getLocation());
        if (shop == null) return;

        boolean isOwner = player.getUniqueId().equals(shop.owner);
        boolean isAdmin = player.hasPermission("technoeconomy.admin") || player.isOp();

        if (!isOwner && !isAdmin) {
            event.setCancelled(true);
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.chestshop-break-denied"));
        } else {
            plugin.getChestShopManager().removeShop(block.getLocation());
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.chestshop-removed"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Material blockType = block.getType();
        if (blockType != Material.CHEST && blockType != Material.TRAPPED_CHEST && blockType != Material.BARREL) return;

        Player player = event.getPlayer();
        ChestShopManager mgr = plugin.getChestShopManager();
        String locKey = mgr.locToString(block.getLocation());

        if (!mgr.isShop(block.getLocation())) {
            if (!player.isSneaking()) return;
            event.setCancelled(true);
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getSignInputManager().openSignInput(player,
                        lines -> {
                            String name = lines.trim();
                            if (name.isEmpty()) {
                                player.sendMessage(plugin.getLanguageManager().getMessage("messages.chestshop-name-empty"));
                                return;
                            }
                            mgr.createShop(block.getLocation(), player.getUniqueId(), name);

                            ItemStack hand = player.getInventory().getItemInMainHand();
                            if (hand.getType() != Material.AIR) {
                                String pid = UUID.randomUUID().toString().substring(0, 8);
                                int amount = hand.getAmount();
                                ItemStack product = hand.clone();
                                product.setAmount(1);
                                player.getInventory().setItemInMainHand(null);
                                ShopProduct sp = new ShopProduct(pid, product, 0.0, amount);
                                mgr.addProduct(locKey, sp);
                                promptProductPrice(player, locKey, sp);
                            } else {
                                player.sendMessage(plugin.getLanguageManager().getMessage("messages.chestshop-created-no-item"));
                            }
                        },
                        () -> player.sendMessage(plugin.getLanguageManager().getMessage("messages.chestshop-creation-cancelled"))
                );
            }, 2L);
            return;
        }

        ChestShopData shop = mgr.getShop(block.getLocation());
        if (shop == null) return;

        event.setCancelled(true);
        boolean isOwner = player.getUniqueId().equals(shop.owner);
        boolean isAdmin = player.hasPermission("technoeconomy.admin") || player.isOp();

        if (isOwner || isAdmin) {
            Bukkit.getScheduler().runTask(plugin, () -> mgr.openAdminMenu(player, locKey, shop));
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> mgr.openShopMenu(player, locKey, shop, 0));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof ChestShopHolder holder)) return;

        ChestShopManager mgr = plugin.getChestShopManager();
        String locKey = holder.getLocKey();
        ChestShopData shop = mgr.getShopByKey(locKey);

        if (holder.getType() != ChestShopHolder.Type.ADD_PRODUCT) {
            event.setCancelled(true);
        }

        switch (holder.getType()) {
            case ADMIN -> handleAdminClick(event, player, mgr, locKey, shop);
            case BROWSE -> handleBrowseClick(event, player, mgr, locKey, shop, holder.getPage());
            case CONFIRM -> handleConfirmClick(event, player, mgr, locKey, shop, holder.getProductId());
            case ADD_PRODUCT -> handleAddProductClick(event, player, mgr, locKey, shop);
        }
    }

    private void handleAdminClick(InventoryClickEvent event, Player player, ChestShopManager mgr,
                                   String locKey, ChestShopData shop) {
        if (shop == null) { closeInv(player); return; }
        int slot = event.getRawSlot();

        if (slot == 19) {
            Bukkit.getScheduler().runTask(plugin, () -> mgr.openAddProductMenu(player, locKey, shop));
            return;
        }

        if (slot == 25) {
            Location loc = mgr.stringToLoc(locKey);
            if (loc != null) mgr.removeShop(loc);
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.chestshop-removed"));
            closeInv(player);
            return;
        }

        if (slot == 49) { closeInv(player); return; }

        if (slot >= 27 && slot < 45) {
            int productIdx = slot - 27;
            if (productIdx >= shop.products.size()) return;
            ShopProduct p = shop.products.get(productIdx);

            if (event.getClick() == ClickType.RIGHT) {
                if (p.stock > 0) {
                    ItemStack returnItem = p.item.clone();
                    returnItem.setAmount(p.stock);
                    player.getInventory().addItem(returnItem);
                }
                mgr.removeProduct(locKey, p.id);
                Bukkit.getScheduler().runTask(plugin, () -> mgr.openAdminMenu(player, locKey, shop));
            } else if (event.getClick() == ClickType.MIDDLE) {
                promptProductPrice(player, locKey, p);
            }
        }
    }

    private void handleBrowseClick(InventoryClickEvent event, Player player, ChestShopManager mgr,
                                    String locKey, ChestShopData shop, int scrollIndex) {
        if (shop == null) { closeInv(player); return; }
        int slot = event.getRawSlot();

        if (slot == 22) { closeInv(player); return; }

        if (slot == 9 && scrollIndex > 0) {
            Bukkit.getScheduler().runTask(plugin, () -> mgr.openShopMenu(player, locKey, shop, scrollIndex - 1));
            return;
        }
        if (slot == 17) {
            int maxVisible = 7;
            if (scrollIndex + maxVisible < shop.products.size()) {
                Bukkit.getScheduler().runTask(plugin, () -> mgr.openShopMenu(player, locKey, shop, scrollIndex + 1));
            }
            return;
        }

        if (slot >= 10 && slot <= 16) {
            int idx = scrollIndex + (slot - 10);
            if (idx >= shop.products.size()) return;
            ShopProduct p = shop.products.get(idx);
            Bukkit.getScheduler().runTask(plugin, () -> mgr.openConfirmMenu(player, locKey, shop, p));
        }
    }

    private void handleConfirmClick(InventoryClickEvent event, Player player, ChestShopManager mgr,
                                     String locKey, ChestShopData shop, String productId) {
        if (shop == null) { closeInv(player); return; }
        int slot = event.getRawSlot();

        if (slot == 15) {
            Bukkit.getScheduler().runTask(plugin, () -> mgr.openShopMenu(player, locKey, shop, 0));
            return;
        }

        if (slot == 11) {
            ShopProduct p = shop.getProduct(productId);
            if (p == null) { closeInv(player); return; }

            if (p.stock <= 0) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.chestshop-empty"));
                closeInv(player);
                return;
            }

            if (!plugin.getBalanceManager().has(player.getUniqueId(), p.price)) {
                player.sendMessage(plugin.getLanguageManager().getMessage("messages.insufficient-funds"));
                closeInv(player);
                return;
            }

            plugin.getBalanceManager().removeBalance(player.getUniqueId(), p.price);
            plugin.getBalanceManager().addBalance(shop.owner, p.price);

            p.stock--;
            mgr.saveProductAsync(locKey, p);

            ItemStack bought = p.item.clone();
            bought.setAmount(1);
            player.getInventory().addItem(bought);

            String symbol = plugin.getConfig().getString("economy.currency-symbol", "$");
            player.sendMessage(plugin.getLanguageManager().getMessage("messages.chestshop-purchased", Map.of(
                    "item", mgr.getItemDisplayName(p.item),
                    "price", Format.money(p.price, symbol)
            )));

            Player ownerPlayer = Bukkit.getPlayer(shop.owner);
            if (ownerPlayer != null && ownerPlayer.isOnline()) {
                ownerPlayer.sendMessage(plugin.getLanguageManager().getMessage("messages.chestshop-sold-notify", Map.of(
                        "item", mgr.getItemDisplayName(p.item),
                        "buyer", player.getName(),
                        "price", Format.money(p.price, symbol)
                )));
            }

            plugin.getTransactionLogManager().log(player.getUniqueId(), player.getName(),
                    "CHESTSHOP_BUY", -p.price,
                    plugin.getBalanceManager().getBalance(player.getUniqueId()),
                    "Bought " + p.item.getType().name() + " from chest shop");
            plugin.getTransactionLogManager().log(shop.owner, "ShopOwner",
                    "CHESTSHOP_SALE", p.price,
                    plugin.getBalanceManager().getBalance(shop.owner),
                    "Sold " + p.item.getType().name() + " via chest shop");

            Location loc = mgr.stringToLoc(locKey);
            if (loc != null) mgr.spawnHolograms(locKey, loc, shop);

            closeInv(player);
        }
    }

    private void handleAddProductClick(InventoryClickEvent event, Player player, ChestShopManager mgr,
                                        String locKey, ChestShopData shop) {
        if (shop == null) { closeInv(player); return; }
        int rawSlot = event.getRawSlot();

        if (rawSlot == 11) {
            event.setCancelled(true);
            ItemStack inSlot = event.getInventory().getItem(13);
            if (inSlot != null && inSlot.getType() != Material.AIR) {
                player.getInventory().addItem(inSlot);
                event.getInventory().setItem(13, null);
            }
            Bukkit.getScheduler().runTask(plugin, () -> mgr.openAdminMenu(player, locKey, shop));
            return;
        }

        if (rawSlot == 15) {
            event.setCancelled(true);
            ItemStack placed = event.getInventory().getItem(13);
            if (placed == null || placed.getType() == Material.AIR) {
                player.sendMessage("§cPlease place an item in the center slot first!");
                return;
            }
            int amount = placed.getAmount();
            ItemStack productItem = placed.clone();
            productItem.setAmount(1);
            event.getInventory().setItem(13, null);

            String pid = UUID.randomUUID().toString().substring(0, 8);
            ShopProduct sp = new ShopProduct(pid, productItem, 0.0, amount);
            mgr.addProduct(locKey, sp);
            promptProductPrice(player, locKey, sp);
            return;
        }

        if (rawSlot >= 0 && rawSlot < 27 && rawSlot != 13) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof ChestShopHolder holder)) return;

        if (holder.getType() == ChestShopHolder.Type.ADD_PRODUCT) {
            ItemStack inSlot = event.getInventory().getItem(13);
            if (inSlot != null && inSlot.getType() != Material.AIR) {
                player.getInventory().addItem(inSlot);
                event.getInventory().setItem(13, null);
            }
        }
    }

    private void promptProductPrice(Player player, String locKey, ShopProduct product) {
        player.closeInventory();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getSignInputManager().openSignInput(player,
                    input -> {
                        double price;
                        try {
                            price = Double.parseDouble(input.trim().replace(",", ".").replace("$", ""));
                            if (price < 0) throw new NumberFormatException();
                        } catch (NumberFormatException e) {
                            player.sendMessage(plugin.getLanguageManager().getMessage(
                                    "messages.invalid-amount", Map.of("amount", input)));
                            return;
                        }
                        product.price = price;
                        plugin.getChestShopManager().updateProduct(locKey, product);
                        ChestShopData shop = plugin.getChestShopManager().getShopByKey(locKey);
                        player.sendMessage(plugin.getLanguageManager().getMessage("messages.chestshop-product-added",
                                Map.of("item", plugin.getChestShopManager().getItemDisplayName(product.item),
                                        "price", String.valueOf(price))));
                        if (shop != null) {
                            Bukkit.getScheduler().runTask(plugin, () ->
                                    plugin.getChestShopManager().openAdminMenu(player, locKey, shop));
                        }
                    },
                    () -> {
                        ChestShopData shop = plugin.getChestShopManager().getShopByKey(locKey);
                        if (shop != null) {
                            Bukkit.getScheduler().runTask(plugin, () ->
                                    plugin.getChestShopManager().openAdminMenu(player, locKey, shop));
                        }
                    }
            );
        }, 2L);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ChestShopHolder holder) {
            if (holder.getType() != ChestShopHolder.Type.ADD_PRODUCT) {
                event.setCancelled(true);
            } else {
                for (int slot : event.getRawSlots()) {
                    if (slot < 27 && slot != 13) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    private void closeInv(Player player) {
        Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
    }
}
