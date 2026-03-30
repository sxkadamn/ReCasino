package net.recasino.addon;

import net.recasino.ReCasino;
import net.recasino.api.mode.CasinoMode;
import net.recasino.api.mode.CasinoModeContext;
import net.recasino.api.player.CasinoPlayerProfile;
import net.recasino.model.CasinoProfile;
import net.recasino.model.CurrencyType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public final class ApiModeContext implements CasinoModeContext {

    private final ReCasino plugin;
    private final CasinoMode mode;
    private final Player player;
    private final CasinoProfile profile;
    private final Inventory inventory;

    public ApiModeContext(ReCasino plugin, CasinoMode mode, Player player, CasinoProfile profile, Inventory inventory) {
        this.plugin = plugin;
        this.mode = mode;
        this.player = player;
        this.profile = profile;
        this.inventory = inventory;
    }

    @Override
    public net.recasino.api.ReCasinoApi getApi() {
        return plugin.getAddonRegistry();
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public CasinoPlayerProfile getProfile() {
        return profile;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public double getBalance(CurrencyType currencyType) {
        return currencyType == CurrencyType.MONEY ? plugin.getEconomyService().getBalance(player) : profile.getRillikBalance();
    }

    @Override
    public boolean has(CurrencyType currencyType, double amount) {
        return currencyType == CurrencyType.MONEY ? plugin.getEconomyService().has(player, amount) : profile.getRillikBalance() >= amount;
    }

    @Override
    public boolean withdraw(CurrencyType currencyType, double amount) {
        if (!has(currencyType, amount)) {
            return false;
        }
        if (currencyType == CurrencyType.MONEY) {
            plugin.getEconomyService().withdraw(player, amount);
        } else {
            profile.setRillikBalance(profile.getRillikBalance() - amount);
            plugin.getProfileService().markDirty(player.getUniqueId());
        }
        return true;
    }

    @Override
    public void deposit(CurrencyType currencyType, double amount) {
        if (currencyType == CurrencyType.MONEY) {
            plugin.getEconomyService().deposit(player, amount);
        } else {
            profile.setRillikBalance(profile.getRillikBalance() + amount);
            plugin.getProfileService().markDirty(player.getUniqueId());
        }
    }

    @Override
    public void markProfileDirty() {
        plugin.getProfileService().markDirty(player.getUniqueId());
    }

    @Override
    public void openMainMenu() {
        plugin.getMenuFactory().openMain(player);
    }

    @Override
    public void reopen() {
        plugin.getMenuFactory().openAddonMode(player, profile, mode);
    }
}
