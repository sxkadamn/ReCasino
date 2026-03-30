package net.recasino.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class CasinoMenuHolder implements InventoryHolder {

    private final MenuType menuType;
    private final String addonModeId;

    public CasinoMenuHolder(MenuType menuType) {
        this(menuType, null);
    }

    public CasinoMenuHolder(MenuType menuType, String addonModeId) {
        this.menuType = menuType;
        this.addonModeId = addonModeId;
    }

    public MenuType getMenuType() {
        return menuType;
    }

    public String getAddonModeId() {
        return addonModeId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}

