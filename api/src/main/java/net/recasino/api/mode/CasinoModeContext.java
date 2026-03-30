package net.recasino.api.mode;

import net.recasino.api.ReCasinoApi;
import net.recasino.api.player.CasinoPlayerProfile;
import net.recasino.model.CurrencyType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public interface CasinoModeContext {

    ReCasinoApi getApi();

    Plugin getPlugin();

    Player getPlayer();

    CasinoPlayerProfile getProfile();

    Inventory getInventory();

    double getBalance(CurrencyType currencyType);

    boolean has(CurrencyType currencyType, double amount);

    boolean withdraw(CurrencyType currencyType, double amount);

    void deposit(CurrencyType currencyType, double amount);

    void markProfileDirty();

    void openMainMenu();

    void reopen();
}
