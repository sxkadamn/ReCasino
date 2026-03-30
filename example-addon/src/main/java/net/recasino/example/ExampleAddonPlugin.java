package net.recasino.example;

import net.recasino.api.ReCasinoApi;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ExampleAddonPlugin extends JavaPlugin {

    private ReCasinoApi api;

    @Override
    public void onEnable() {
        api = Bukkit.getServicesManager().load(ReCasinoApi.class);
        if (api == null) {
            getLogger().severe("ReCasino API недоступен. Убедитесь, что ReCasino установлен и включен.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        api.registerMode(this, new CoinFlipMode());
        api.registerRouletteAnimation(this, new PulseRouletteAnimation());
        getLogger().info("Зарегистрированы режим Коинфлип и пример анимации рулетки.");
    }

    @Override
    public void onDisable() {
        if (api == null) {
            return;
        }
        api.unregisterModes(this);
        api.unregisterRouletteAnimations(this);
    }
}
