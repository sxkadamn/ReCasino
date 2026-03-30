package net.recasino.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class CasinoMenuHolder implements InventoryHolder {

    private final MenuType menuType;
    private final String addonModeId;
    private final String contextId;

    public CasinoMenuHolder(MenuType menuType) {
        this(menuType, null, null);
    }

    public CasinoMenuHolder(MenuType menuType, String addonModeId) {
        this(menuType, addonModeId, null);
    }

    public CasinoMenuHolder(MenuType menuType, String addonModeId, String contextId) {
        this.menuType = menuType;
        this.addonModeId = addonModeId;
        this.contextId = contextId;
    }

    public MenuType getMenuType() {
        return menuType;
    }

    public String getAddonModeId() {
        return addonModeId;
    }

    public String getContextId() {
        return contextId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}

