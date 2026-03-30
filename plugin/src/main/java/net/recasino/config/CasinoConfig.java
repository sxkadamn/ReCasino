package net.recasino.config;

import net.recasino.ReCasino;
import net.recasino.model.CurrencyType;
import net.recasino.model.GameMode;
import net.recasino.model.Prize;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public final class CasinoConfig {

    private final ReCasino plugin;
    private FileConfiguration config;

    public CasinoConfig(ReCasino plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        this.config = plugin.getConfig();
    }

    public String getString(String path) {
        return config.getString(path, path);
    }

    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    public double getStartingMoneyBet() {
        return config.getDouble("settings.money.start-bet", 1000.0D);
    }

    public double getMaxMoneyBet() {
        return config.getDouble("settings.money.max-bet", 10000000.0D);
    }

    public double getStartingRillikBet() {
        return config.getDouble("settings.rillik.start-bet", 100.0D);
    }

    public double getMaxRillikBet() {
        return config.getDouble("settings.rillik.max-bet", 100000.0D);
    }

    public double getStartingRillikBalance() {
        return config.getDouble("settings.rillik.start-balance", 5000.0D);
    }

    public String getProfilesFileName() {
        return config.getString("storage.profiles-file", "profiles.yml");
    }

    public long getHologramRefreshHours() {
        return Math.max(1L, config.getLong("holograms.refresh-hours", 3L));
    }

    public long getHologramCountdownUpdateSeconds() {
        return Math.max(10L, config.getLong("holograms.countdown-update-seconds", 60L));
    }

    public int getInventorySize(String key) {
        return Math.max(27, config.getInt("menus." + key + ".size", 45));
    }

    public String getMenuTitle(String key) {
        return getString("menus." + key + ".title");
    }

    public double getMaxBet(CurrencyType currencyType) {
        return currencyType == CurrencyType.MONEY ? getMaxMoneyBet() : getMaxRillikBet();
    }

    public int getAnimationSteps(GameMode mode) {
        return Math.max(3, config.getInt(getAnimationPath(mode) + ".steps", mode == GameMode.MINER ? 14 : 18));
    }

    public long getAnimationStartIntervalTicks(GameMode mode) {
        return Math.max(1L, config.getLong(getAnimationPath(mode) + ".interval-start-ticks", 1L));
    }

    public long getAnimationEndIntervalTicks(GameMode mode) {
        return Math.max(getAnimationStartIntervalTicks(mode), config.getLong(getAnimationPath(mode) + ".interval-end-ticks", mode == GameMode.MINER ? 4L : 5L));
    }

    public long getRouletteResultDisplayTicks() {
        return Math.max(1L, config.getLong("animations.roulette.result-display-ticks", 40L));
    }

    public String getRouletteAnimationProvider() {
        return config.getString("animations.roulette.provider", "default");
    }

    public int getMinerMinesCount() {
        return Math.max(1, Math.min(8, config.getInt("games.miner.mines", 3)));
    }

    public double getMinerMultiplier(int revealedSafe) {
        List<Double> multipliers = config.getDoubleList("games.miner.safe-multipliers");
        if (!multipliers.isEmpty()) {
            int index = Math.max(0, Math.min(revealedSafe - 1, multipliers.size() - 1));
            return multipliers.get(index);
        }

        double[] fallback = {1.18D, 1.44D, 1.87D, 2.55D, 3.75D, 5.90D, 9.40D, 16.0D};
        return fallback[Math.max(0, Math.min(revealedSafe - 1, fallback.length - 1))];
    }

    public long getMinerLoseDisplayTicks() {
        return Math.max(1L, config.getLong("games.miner.lose-display-ticks", 40L));
    }

    public double getHorseRacingMoveChance() {
        return Math.max(0.05D, Math.min(1.0D, config.getDouble("games.horse-racing.move-chance", 0.6D)));
    }

    public long getHorseRacingTickInterval() {
        return Math.max(1L, config.getLong("games.horse-racing.tick-interval-ticks", 2L));
    }

    public double getHorseRacingMultiplier(int place) {
        List<Double> multipliers = config.getDoubleList("games.horse-racing.place-multipliers");
        if (!multipliers.isEmpty()) {
            return multipliers.get(Math.max(0, Math.min(place - 1, multipliers.size() - 1)));
        }

        double[] fallback = {3.0D, 1.5D, 1.0D, 0.0D};
        return fallback[Math.max(0, Math.min(place - 1, fallback.length - 1))];
    }

    public long getCrashTickInterval() {
        return Math.max(1L, config.getLong("games.crash.tick-interval-ticks", 2L));
    }

    public double getCrashGrowthPerTick() {
        return Math.max(0.01D, config.getDouble("games.crash.growth-per-tick", 0.05D));
    }

    public double getCrashMinMultiplier() {
        return Math.max(1.1D, config.getDouble("games.crash.min-multiplier", 1.2D));
    }

    public double getCrashMaxMultiplier() {
        return Math.max(getCrashMinMultiplier(), config.getDouble("games.crash.max-multiplier", 6.0D));
    }

    public double getCrashInstantChance() {
        return Math.max(0.0D, Math.min(0.95D, config.getDouble("games.crash.instant-crash-chance", 0.1D)));
    }

    public long getJackpotCountdownSeconds() {
        return Math.max(10L, config.getLong("games.jackpot.countdown-seconds", 60L));
    }

    public List<Prize> getPrizes(GameMode mode) {
        String sectionPath = getPrizePath(mode);
        ConfigurationSection section = config.getConfigurationSection(sectionPath);
        List<Prize> result = new ArrayList<Prize>();
        if (section == null) {
            result.add(new Prize("fallback", "Fallback", Material.STONE, 1, 0.0D));
            return result;
        }

        for (String key : section.getKeys(false)) {
            String base = sectionPath + "." + key;
            Material material = Material.matchMaterial(config.getString(base + ".material", "STONE"));
            result.add(new Prize(
                    key,
                    config.getString(base + ".name", key),
                    material == null ? Material.STONE : material,
                    Math.max(1, config.getInt(base + ".weight", 1)),
                    config.getDouble(base + ".multiplier", 0.0D)
            ));
        }
        return result;
    }

    public void updatePrizeWeight(GameMode mode, String key, int delta) {
        String path = getPrizePath(mode) + "." + key + ".weight";
        int current = Math.max(1, config.getInt(path, 1));
        config.set(path, Math.max(1, current + delta));
        plugin.saveConfig();
        reload();
    }

    public void updateCrashSetting(String path, double delta, double min, double max) {
        String fullPath = "games.crash." + path;
        double current = config.getDouble(fullPath);
        double updated = Math.max(min, Math.min(max, current + delta));
        config.set(fullPath, updated);
        plugin.saveConfig();
        reload();
    }

    private String getAnimationPath(GameMode mode) {
        if (mode == GameMode.MINER) {
            return "animations.miner";
        }
        return "animations.roulette";
    }

    private String getPrizePath(GameMode mode) {
        if (mode == GameMode.MONEY_ROULETTE) {
            return "games.money-roulette.prizes";
        }
        if (mode == GameMode.RILLIK_ROULETTE) {
            return "games.rillik-roulette.prizes";
        }
        return "games.miner.prizes";
    }
}

