package net.recasino.api;

import net.recasino.api.animation.RouletteAnimation;
import net.recasino.api.mode.CasinoMode;
import net.recasino.api.player.CasinoPlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.UUID;

public interface ReCasinoApi {

    void registerMode(Plugin owner, CasinoMode mode);

    void unregisterModes(Plugin owner);

    Collection<CasinoMode> getModes();

    CasinoMode getMode(String id);

    void registerRouletteAnimation(Plugin owner, RouletteAnimation animation);

    void unregisterRouletteAnimations(Plugin owner);

    Collection<RouletteAnimation> getRouletteAnimations();

    RouletteAnimation getRouletteAnimation(String id);

    CasinoPlayerProfile getProfile(Player player);

    void markProfileDirty(UUID uniqueId);

    void openMainMenu(Player player);
}
