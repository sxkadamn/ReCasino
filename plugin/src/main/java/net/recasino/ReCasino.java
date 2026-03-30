package net.recasino;

import net.recasino.addon.AddonLifecycleListener;
import net.recasino.addon.AddonRegistry;
import net.recasino.addon.DefaultRouletteAnimation;
import net.recasino.api.ReCasinoApi;
import net.recasino.command.CasinoCommand;
import net.recasino.config.CasinoConfig;
import net.recasino.gui.MenuFactory;
import net.recasino.listener.CasinoMenuListener;
import net.recasino.listener.PlayerConnectionListener;
import net.recasino.placeholder.CasinoPlaceholderExpansion;
import net.recasino.service.CasinoService;
import net.recasino.service.EconomyService;
import net.recasino.service.HologramService;
import net.recasino.service.ProfileService;
import net.recasino.storage.ProfileStorage;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class ReCasino extends JavaPlugin {

    private CasinoConfig casinoConfig;
    private EconomyService economyService;
    private ProfileStorage profileStorage;
    private ProfileService profileService;
    private CasinoService casinoService;
    private HologramService hologramService;
    private MenuFactory menuFactory;
    private AddonRegistry addonRegistry;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        casinoConfig = new CasinoConfig(this);
        economyService = new EconomyService(this);
        if (!economyService.setup()) {
            getLogger().severe("Vault economy provider was not found. Plugin will be disabled.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        profileStorage = new ProfileStorage(this);
        profileService = new ProfileService(profileStorage, casinoConfig);
        casinoService = new CasinoService(this, casinoConfig, economyService, profileService);
        hologramService = new HologramService(this);
        addonRegistry = new AddonRegistry(this);
        addonRegistry.registerRouletteAnimation(this, new DefaultRouletteAnimation());
        menuFactory = new MenuFactory(this, casinoConfig, economyService, casinoService, addonRegistry);
        casinoService.initialize();
        getServer().getServicesManager().register(ReCasinoApi.class, addonRegistry, this, ServicePriority.Normal);

        registerListeners();
        registerCommands();
        registerPlaceholders();
        hologramService.initialize();
    }

    @Override
    public void onDisable() {
        if (profileService != null) {
            profileService.saveAll();
        }
        if (hologramService != null) {
            hologramService.shutdown();
        }
        if (casinoService != null) {
            casinoService.shutdown();
        }
        getServer().getServicesManager().unregister(ReCasinoApi.class, addonRegistry);
    }

    public void reloadPlugin() {
        reloadConfig();
        casinoConfig.reload();
        if (profileService != null) {
            profileService.saveAll();
        }
        if (casinoService != null) {
            casinoService.initialize();
        }
        if (hologramService != null) {
            hologramService.initialize();
        }
    }

    public CasinoConfig getCasinoConfig() {
        return casinoConfig;
    }

    public EconomyService getEconomyService() {
        return economyService;
    }

    public ProfileService getProfileService() {
        return profileService;
    }

    public CasinoService getCasinoService() {
        return casinoService;
    }

    public HologramService getHologramService() {
        return hologramService;
    }

    public MenuFactory getMenuFactory() {
        return menuFactory;
    }

    public AddonRegistry getAddonRegistry() {
        return addonRegistry;
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(profileService, casinoService), this);
        getServer().getPluginManager().registerEvents(new CasinoMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new AddonLifecycleListener(this), this);
    }

    private void registerCommands() {
        PluginCommand command = getCommand("casino");
        if (command == null) {
            throw new IllegalStateException("Command 'casino' is not defined in plugin.yml");
        }

        CasinoCommand executor = new CasinoCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new CasinoPlaceholderExpansion(this).register();
        }
    }
}

