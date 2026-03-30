package net.recasino.service;

import net.recasino.ReCasino;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HologramService {

    public enum HologramType {
        TOP_PROFIT("top-profit", "lc_top_profit"),
        TOP_WINS("top-wins", "lc_top_wins"),
        PERSONAL_STATS("personal-stats", "lc_personal_stats");

        private final String configKey;
        private final String hologramId;

        HologramType(String configKey, String hologramId) {
            this.configKey = configKey;
            this.hologramId = hologramId;
        }

        public String getConfigKey() {
            return configKey;
        }

        public String getHologramId() {
            return hologramId;
        }
    }

    private final ReCasino plugin;
    private final Map<HologramType, Object> hdHolograms;
    private Provider provider;
    private long nextRefreshAtMillis;
    private BukkitTask countdownTask;

    public HologramService(ReCasino plugin) {
        this.plugin = plugin;
        this.hdHolograms = new HashMap<HologramType, Object>();
        this.provider = Provider.NONE;
    }

    public void initialize() {
        shutdown();
        provider = detectProvider();
        if (provider == Provider.DECENT_HOLOGRAMS) {
            cleanupLegacyDecentFiles();
        }
        nextRefreshAtMillis = System.currentTimeMillis() + Duration.ofHours(plugin.getCasinoConfig().getHologramRefreshHours()).toMillis();
        refreshAll();
        startCountdownTask();
    }

    public void shutdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }

        if (provider == Provider.HOLOGRAPHIC_DISPLAYS) {
            for (Object hologram : hdHolograms.values()) {
                invokeNoArgs(hologram, "delete");
            }
            hdHolograms.clear();
        }

        if (provider == Provider.DECENT_HOLOGRAMS) {
            deleteDecentHolograms();
        }
    }

    public Provider getProvider() {
        return provider;
    }

    public boolean setHologramLocation(HologramType type, Location location) {
        String path = "holograms." + type.getConfigKey() + ".location.";
        plugin.getConfig().set(path + "world", location.getWorld() != null ? location.getWorld().getName() : null);
        plugin.getConfig().set(path + "x", location.getX());
        plugin.getConfig().set(path + "y", location.getY());
        plugin.getConfig().set(path + "z", location.getZ());
        plugin.getConfig().set(path + "yaw", location.getYaw());
        plugin.getConfig().set(path + "pitch", location.getPitch());
        plugin.saveConfig();
        plugin.reloadConfig();
        plugin.getCasinoConfig().reload();
        return refresh(type);
    }

    public void refreshAll() {
        for (HologramType type : HologramType.values()) {
            refresh(type);
        }
    }

    public boolean refresh(HologramType type) {
        Location location = readLocation(type);
        if (location == null) {
            return false;
        }

        String timerLine = "&7Обновление через: &f" + formatRemaining();
        List<String> lines = switch (type) {
            case TOP_PROFIT -> List.of(
                    "&#F5C542&lТоп Профита",
                    timerLine,
                    "&71. &f%recasino_top_profit_1_name% &8- &a%recasino_top_profit_1_value%",
                    "&72. &f%recasino_top_profit_2_name% &8- &a%recasino_top_profit_2_value%",
                    "&73. &f%recasino_top_profit_3_name% &8- &a%recasino_top_profit_3_value%",
                    "&74. &f%recasino_top_profit_4_name% &8- &a%recasino_top_profit_4_value%",
                    "&75. &f%recasino_top_profit_5_name% &8- &a%recasino_top_profit_5_value%"
            );
            case TOP_WINS -> List.of(
                    "&#43D17A&lТоп Побед",
                    timerLine,
                    "&71. &f%recasino_top_wins_1_name% &8- &a%recasino_top_wins_1_value%",
                    "&72. &f%recasino_top_wins_2_name% &8- &a%recasino_top_wins_2_value%",
                    "&73. &f%recasino_top_wins_3_name% &8- &a%recasino_top_wins_3_value%",
                    "&74. &f%recasino_top_wins_4_name% &8- &a%recasino_top_wins_4_value%",
                    "&75. &f%recasino_top_wins_5_name% &8- &a%recasino_top_wins_5_value%"
            );
            case PERSONAL_STATS -> List.of(
                    "&#66D9EF&lВаша Статистика",
                    timerLine,
                    "&7Игрок: &f%player_name%",
                    "&7Игр: &f%recasino_games%",
                    "&7Побед: &f%recasino_wins%",
                    "&7Профит: &a%recasino_profit%",
                    "&7Лучший выигрыш: &f%recasino_best_win%",
                    "&7Лучший Crash: &fx%recasino_best_crash%"
            );
        };

        if (provider == Provider.DECENT_HOLOGRAMS) {
            return upsertDecentHologram(type.getHologramId(), location, lines);
        }
        if (provider == Provider.HOLOGRAPHIC_DISPLAYS) {
            return upsertHdHologram(type, location, lines);
        }
        return false;
    }

    private void startCountdownTask() {
        if (provider == Provider.NONE) {
            return;
        }

        long periodTicks = plugin.getCasinoConfig().getHologramCountdownUpdateSeconds() * 20L;
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() >= nextRefreshAtMillis) {
                    nextRefreshAtMillis = System.currentTimeMillis() + Duration.ofHours(plugin.getCasinoConfig().getHologramRefreshHours()).toMillis();
                }
                refreshAll();
            }
        }, periodTicks, periodTicks);
    }

    private String formatRemaining() {
        long remainingMillis = Math.max(0L, nextRefreshAtMillis - System.currentTimeMillis());
        long totalSeconds = remainingMillis / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        if (hours > 0L) {
            return hours + "ч " + minutes + "м";
        }
        return minutes + "м";
    }

    private Provider detectProvider() {
        Plugin dh = Bukkit.getPluginManager().getPlugin("DecentHolograms");
        if (dh != null && dh.isEnabled()) {
            return Provider.DECENT_HOLOGRAMS;
        }
        Plugin hd = Bukkit.getPluginManager().getPlugin("HolographicDisplays");
        if (hd != null && hd.isEnabled()) {
            return Provider.HOLOGRAPHIC_DISPLAYS;
        }
        return Provider.NONE;
    }

    private Location readLocation(HologramType type) {
        String path = "holograms." + type.getConfigKey() + ".location.";
        String worldName = plugin.getConfig().getString(path + "world");
        if (worldName == null || Bukkit.getWorld(worldName) == null) {
            return null;
        }

        return new Location(
                Bukkit.getWorld(worldName),
                plugin.getConfig().getDouble(path + "x"),
                plugin.getConfig().getDouble(path + "y"),
                plugin.getConfig().getDouble(path + "z"),
                (float) plugin.getConfig().getDouble(path + "yaw"),
                (float) plugin.getConfig().getDouble(path + "pitch")
        );
    }

    private boolean upsertDecentHologram(String hologramId, Location location, List<String> lines) {
        try {
            Class<?> dhapiClass = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
            Method getHologram = dhapiClass.getMethod("getHologram", String.class);
            Object hologram = getHologram.invoke(null, hologramId);
            if (hologram != null) {
                invokeNoArgs(hologram, "delete");
            }
            Method create = dhapiClass.getMethod("createHologram", String.class, Location.class, boolean.class, List.class);
            create.invoke(null, hologramId, location, false, lines);
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to update DecentHolograms hologram " + hologramId + ": " + exception.getMessage());
            return false;
        }
    }

    private boolean upsertHdHologram(HologramType type, Location location, List<String> lines) {
        try {
            Object previous = hdHolograms.remove(type);
            if (previous != null) {
                invokeNoArgs(previous, "delete");
            }

            Class<?> apiClass = Class.forName("me.filoghost.holographicdisplays.api.HolographicDisplaysAPI");
            Method get = apiClass.getMethod("get", Plugin.class);
            Object api = get.invoke(null, plugin);
            Method create = apiClass.getMethod("createHologram", Location.class);
            Object hologram = create.invoke(api, location);

            Object linesContainer = hologram.getClass().getMethod("getLines").invoke(hologram);
            Method appendText = linesContainer.getClass().getMethod("appendText", String.class);
            for (String line : lines) {
                appendText.invoke(linesContainer, line);
            }

            hdHolograms.put(type, hologram);
            return true;
        } catch (Exception exception) {
            plugin.getLogger().warning("Failed to update HolographicDisplays hologram " + type.getHologramId() + ": " + exception.getMessage());
            return false;
        }
    }

    private void deleteDecentHolograms() {
        for (HologramType type : HologramType.values()) {
            try {
                Class<?> dhapiClass = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
                Method getHologram = dhapiClass.getMethod("getHologram", String.class);
                Object hologram = getHologram.invoke(null, type.getHologramId());
                if (hologram != null) {
                    invokeNoArgs(hologram, "delete");
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void cleanupLegacyDecentFiles() {
        Plugin decentPlugin = Bukkit.getPluginManager().getPlugin("DecentHolograms");
        if (decentPlugin == null) {
            return;
        }

        File hologramsFolder = new File(decentPlugin.getDataFolder(), "holograms");
        if (!hologramsFolder.isDirectory()) {
            return;
        }

        for (HologramType type : HologramType.values()) {
            File file = new File(hologramsFolder, type.getHologramId() + ".yml");
            if (file.isFile() && !file.delete()) {
                plugin.getLogger().warning("Failed to delete legacy DecentHolograms file: " + file.getName());
            }
        }
    }

    private void invokeNoArgs(Object target, String method) {
        try {
            target.getClass().getMethod(method).invoke(target);
        } catch (Exception ignored) {
        }
    }

    public enum Provider {
        NONE,
        DECENT_HOLOGRAMS,
        HOLOGRAPHIC_DISPLAYS
    }
}

