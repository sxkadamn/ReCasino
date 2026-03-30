package net.recasino.service;

import net.recasino.ReCasino;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class EconomyService {

    private final ReCasino plugin;
    private Economy economy;

    public EconomyService(ReCasino plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> provider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            return false;
        }

        economy = provider.getProvider();
        return economy != null;
    }

    public boolean has(Player player, double amount) {
        return economy.has(player, amount);
    }

    public double getBalance(Player player) {
        return economy.getBalance(player);
    }

    public void withdraw(Player player, double amount) {
        economy.withdrawPlayer(player, amount);
    }

    public void deposit(Player player, double amount) {
        economy.depositPlayer(player, amount);
    }
}

