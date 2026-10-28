package com.techno.economy.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ChestShopHolder implements InventoryHolder {

    public enum Type {
        ADMIN, BROWSE, CONFIRM, ADD_PRODUCT
    }

    private final Type type;
    private final String locKey;
    private final int page;
    private final String productId;

    public ChestShopHolder(Type type, String locKey) {
        this.type = type;
        this.locKey = locKey;
        this.page = 0;
        this.productId = null;
    }

    public ChestShopHolder(Type type, String locKey, int page) {
        this.type = type;
        this.locKey = locKey;
        this.page = page;
        this.productId = null;
    }

    public ChestShopHolder(Type type, String locKey, String productId) {
        this.type = type;
        this.locKey = locKey;
        this.page = 0;
        this.productId = productId;
    }

    public Type getType() { return type; }
    public String getLocKey() { return locKey; }
    public int getPage() { return page; }
    public String getProductId() { return productId; }

    @Override
    public Inventory getInventory() { return null; }
}
