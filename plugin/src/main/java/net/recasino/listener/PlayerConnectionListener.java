package net.recasino.listener;

import net.recasino.service.ProfileService;
import net.recasino.service.CasinoService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerConnectionListener implements Listener {

    private final ProfileService profileService;
    private final CasinoService casinoService;

    public PlayerConnectionListener(ProfileService profileService, CasinoService casinoService) {
        this.profileService = profileService;
        this.casinoService = casinoService;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        profileService.getProfile(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        casinoService.clearPlayerState(event.getPlayer(), profileService.getProfile(event.getPlayer()));
        profileService.unload(event.getPlayer());
    }
}

