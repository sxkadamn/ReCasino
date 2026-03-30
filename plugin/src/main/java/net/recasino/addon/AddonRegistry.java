package net.recasino.addon;

import net.recasino.ReCasino;
import net.recasino.api.ReCasinoApi;
import net.recasino.api.animation.RouletteAnimation;
import net.recasino.api.mode.CasinoMode;
import net.recasino.api.player.CasinoPlayerProfile;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AddonRegistry implements ReCasinoApi {

    private static final int[] MAIN_MENU_SLOTS = {19, 21, 23, 25, 37, 39, 41, 43};

    private final ReCasino plugin;
    private final Map<String, Registered<CasinoMode>> modes;
    private final Map<Plugin, Set<String>> modeOwnership;
    private final Map<String, Registered<RouletteAnimation>> rouletteAnimations;
    private final Map<Plugin, Set<String>> animationOwnership;

    public AddonRegistry(ReCasino plugin) {
        this.plugin = plugin;
        this.modes = new LinkedHashMap<String, Registered<CasinoMode>>();
        this.modeOwnership = new HashMap<Plugin, Set<String>>();
        this.rouletteAnimations = new LinkedHashMap<String, Registered<RouletteAnimation>>();
        this.animationOwnership = new HashMap<Plugin, Set<String>>();
    }

    @Override
    public void registerMode(Plugin owner, CasinoMode mode) {
        String id = normalize(mode.getId());
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Mode id cannot be empty");
        }
        if (modes.containsKey(id)) {
            throw new IllegalArgumentException("Mode '" + id + "' is already registered");
        }

        modes.put(id, new Registered<CasinoMode>(owner, mode));
        modeOwnership.computeIfAbsent(owner, ignored -> new LinkedHashSet<String>()).add(id);
    }

    @Override
    public void unregisterModes(Plugin owner) {
        Set<String> ids = modeOwnership.remove(owner);
        if (ids == null) {
            return;
        }
        for (String id : ids) {
            modes.remove(id);
        }
    }

    @Override
    public Collection<CasinoMode> getModes() {
        return viewOfModes();
    }

    @Override
    public CasinoMode getMode(String id) {
        Registered<CasinoMode> registered = modes.get(normalize(id));
        return registered == null ? null : registered.value();
    }

    @Override
    public void registerRouletteAnimation(Plugin owner, RouletteAnimation animation) {
        String id = normalize(animation.getId());
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Animation id cannot be empty");
        }
        if (rouletteAnimations.containsKey(id)) {
            throw new IllegalArgumentException("Roulette animation '" + id + "' is already registered");
        }

        rouletteAnimations.put(id, new Registered<RouletteAnimation>(owner, animation));
        animationOwnership.computeIfAbsent(owner, ignored -> new LinkedHashSet<String>()).add(id);
    }

    @Override
    public void unregisterRouletteAnimations(Plugin owner) {
        Set<String> ids = animationOwnership.remove(owner);
        if (ids == null) {
            return;
        }
        for (String id : ids) {
            rouletteAnimations.remove(id);
        }
    }

    @Override
    public Collection<RouletteAnimation> getRouletteAnimations() {
        List<RouletteAnimation> result = new ArrayList<RouletteAnimation>();
        for (Registered<RouletteAnimation> value : rouletteAnimations.values()) {
            result.add(value.value());
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public RouletteAnimation getRouletteAnimation(String id) {
        Registered<RouletteAnimation> registered = rouletteAnimations.get(normalize(id));
        return registered == null ? null : registered.value();
    }

    @Override
    public CasinoPlayerProfile getProfile(Player player) {
        return plugin.getProfileService().getProfile(player);
    }

    @Override
    public void markProfileDirty(UUID uniqueId) {
        plugin.getProfileService().markDirty(uniqueId);
    }

    @Override
    public void openMainMenu(Player player) {
        plugin.getMenuFactory().openMain(player);
    }

    public Map<Integer, CasinoMode> resolveMainMenuModes() {
        Map<Integer, CasinoMode> resolved = new LinkedHashMap<Integer, CasinoMode>();
        for (CasinoMode mode : viewOfModes()) {
            int preferred = mode.getPreferredMainMenuSlot();
            if (isAddonSlot(preferred) && !resolved.containsKey(preferred)) {
                resolved.put(preferred, mode);
                continue;
            }

            for (int slot : MAIN_MENU_SLOTS) {
                if (!resolved.containsKey(slot)) {
                    resolved.put(slot, mode);
                    break;
                }
            }
        }
        return resolved;
    }

    public RouletteAnimation getActiveRouletteAnimation() {
        String configuredId = plugin.getCasinoConfig().getRouletteAnimationProvider();
        RouletteAnimation configured = getRouletteAnimation(configuredId);
        if (configured != null) {
            return configured;
        }

        RouletteAnimation fallback = getRouletteAnimation("default");
        if (fallback != null) {
            return fallback;
        }

        throw new IllegalStateException("No roulette animation provider is registered");
    }

    public void unregisterAll(Plugin owner) {
        unregisterModes(owner);
        unregisterRouletteAnimations(owner);
    }

    private boolean isAddonSlot(int slot) {
        for (int value : MAIN_MENU_SLOTS) {
            if (value == slot) {
                return true;
            }
        }
        return false;
    }

    private Collection<CasinoMode> viewOfModes() {
        List<CasinoMode> result = new ArrayList<CasinoMode>();
        for (Registered<CasinoMode> value : modes.values()) {
            result.add(value.value());
        }
        return Collections.unmodifiableList(result);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record Registered<T>(Plugin owner, T value) {
    }
}
