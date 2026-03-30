package net.recasino.service;

import net.recasino.config.CasinoConfig;
import net.recasino.model.CasinoProfile;
import net.recasino.model.CurrencyType;
import net.recasino.model.LeaderboardEntry;
import net.recasino.storage.ProfileStorage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

public final class ProfileService {

    private final ProfileStorage storage;
    private final CasinoConfig config;
    private final Map<UUID, CasinoProfile> profiles;

    public ProfileService(ProfileStorage storage, CasinoConfig config) {
        this.storage = storage;
        this.config = config;
        this.profiles = new HashMap<UUID, CasinoProfile>();
    }

    public CasinoProfile getProfile(Player player) {
        CasinoProfile cached = profiles.get(player.getUniqueId());
        if (cached != null) {
            cached.setPlayerName(player.getName());
            return cached;
        }

        CasinoProfile loaded = storage.load(player.getUniqueId());
        if (loaded == null) {
            loaded = new CasinoProfile(
                    player.getName(),
                    config.getStartingMoneyBet(),
                    config.getStartingRillikBet(),
                    config.getStartingRillikBalance(),
                    CurrencyType.MONEY,
                    0,
                    0,
                    0,
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D
            );
        } else {
            loaded.setPlayerName(player.getName());
        }

        profiles.put(player.getUniqueId(), loaded);
        return loaded;
    }

    public void unload(Player player) {
        UUID uniqueId = player.getUniqueId();
        CasinoProfile profile = profiles.remove(uniqueId);
        if (profile != null) {
            profile.setPlayerName(player.getName());
            storage.save(uniqueId, profile);
        }
    }

    public void saveAll() {
        for (Map.Entry<UUID, CasinoProfile> entry : profiles.entrySet()) {
            storage.save(entry.getKey(), entry.getValue());
        }
    }

    public void markDirty(UUID uniqueId) {
        CasinoProfile profile = profiles.get(uniqueId);
        if (profile != null) {
            storage.save(uniqueId, profile);
        }
    }

    public List<LeaderboardEntry> getTopProfiles(ToDoubleFunction<CasinoProfile> extractor, int limit) {
        Map<UUID, CasinoProfile> all = storage.loadAll();
        all.putAll(profiles);

        List<LeaderboardEntry> result = new ArrayList<LeaderboardEntry>();
        all.values().stream()
                .sorted(Comparator.comparingDouble(extractor).reversed())
                .limit(limit)
                .forEach(profile -> result.add(new LeaderboardEntry(profile.getPlayerName(), extractor.applyAsDouble(profile))));
        return result;
    }
}

