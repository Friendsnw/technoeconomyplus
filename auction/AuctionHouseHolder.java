package com.techno.economy.auction;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class AuctionHouseHolder implements InventoryHolder {

    public enum Type {
        BROWSE, CONFIRM, MY_LISTINGS
    }

    private final Type type;
    private final int page;
    private final String listingId;

    public AuctionHouseHolder(Type type, int page) {
        this.type = type;
        this.page = page;
        this.listingId = null;
    }

    public AuctionHouseHolder(Type type, int page, String listingId) {
        this.type = type;
        this.page = page;
        this.listingId = listingId;
    }

    public Type getType() { return type; }
    public int getPage() { return page; }
    public String getListingId() { return listingId; }

    @Override
    public Inventory getInventory() { return null; }
}
