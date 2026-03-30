package net.recasino.storage;

import net.recasino.ReCasino;
import net.recasino.model.CasinoProfile;
import net.recasino.model.CurrencyType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ProfileStorage {

    private final ReCasino plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public ProfileStorage(ReCasino plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), plugin.getCasinoConfig().getProfilesFileName());

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to create profiles storage", exception);
            }
        }

        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public CasinoProfile load(UUID uniqueId) {
        String path = "profiles." + uniqueId + ".";
        if (!yaml.contains(path + "money-bet")) {
            return null;
        }

        return buildProfile(path);
    }

    public Map<UUID, CasinoProfile> loadAll() {
        Map<UUID, CasinoProfile> result = new HashMap<UUID, CasinoProfile>();
        ConfigurationSection section = yaml.getConfigurationSection("profiles");
        if (section == null) {
            return result;
        }

        for (String key : section.getKeys(false)) {
            try {
                UUID uniqueId = UUID.fromString(key);
                CasinoProfile profile = buildProfile("profiles." + key + ".");
                result.put(uniqueId, profile);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }

    public void save(UUID uniqueId, CasinoProfile profile) {
        String path = "profiles." + uniqueId + ".";
        yaml.set(path + "player-name", profile.getPlayerName());
        yaml.set(path + "money-bet", profile.getMoneyBet());
        yaml.set(path + "rillik-bet", profile.getRillikBet());
        yaml.set(path + "rillik-balance", profile.getRillikBalance());
        yaml.set(path + "miner-currency", profile.getMinerCurrency().name());
        yaml.set(path + "stats.total-games", profile.getTotalGames());
        yaml.set(path + "stats.total-wins", profile.getTotalWins());
        yaml.set(path + "stats.total-losses", profile.getTotalLosses());
        yaml.set(path + "stats.total-wagered", profile.getTotalWagered());
        yaml.set(path + "stats.total-won", profile.getTotalWon());
        yaml.set(path + "stats.best-win", profile.getBestWin());
        yaml.set(path + "stats.best-crash-multiplier", profile.getBestCrashMultiplier());

        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to save profiles.yml: " + exception.getMessage());
        }
    }

    private CasinoProfile buildProfile(String path) {
        return new CasinoProfile(
                yaml.getString(path + "player-name", "Unknown"),
                yaml.getDouble(path + "money-bet"),
                yaml.getDouble(path + "rillik-bet"),
                yaml.getDouble(path + "rillik-balance"),
                CurrencyType.fromName(yaml.getString(path + "miner-currency")),
                yaml.getInt(path + "stats.total-games", 0),
                yaml.getInt(path + "stats.total-wins", 0),
                yaml.getInt(path + "stats.total-losses", 0),
                yaml.getDouble(path + "stats.total-wagered", 0.0D),
                yaml.getDouble(path + "stats.total-won", 0.0D),
                yaml.getDouble(path + "stats.best-win", 0.0D),
                yaml.getDouble(path + "stats.best-crash-multiplier", 0.0D)
        );
    }
}

