package net.recasino.addon;

import net.recasino.ReCasino;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;

public final class AddonLifecycleListener implements Listener {

    private final ReCasino plugin;

    public AddonLifecycleListener(ReCasino plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().equals(plugin)) {
            return;
        }
        plugin.getAddonRegistry().unregisterAll(event.getPlugin());
    }
}
