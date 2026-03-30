package net.recasino.api.mode;

import net.recasino.api.player.CasinoPlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public interface CasinoMode {

    String getId();

    default int getPreferredMainMenuSlot() {
        return -1;
    }

    ItemStack createMainMenuItem(Player player, CasinoPlayerProfile profile);

    int getInventorySize();

    String getInventoryTitle(Player player, CasinoPlayerProfile profile);

    void onOpen(CasinoModeContext context);

    void onClick(CasinoModeContext context, InventoryClickEvent event);
}
