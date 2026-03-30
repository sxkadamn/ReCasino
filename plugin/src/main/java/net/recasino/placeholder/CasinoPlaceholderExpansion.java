package net.recasino.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.recasino.ReCasino;
import net.recasino.model.CasinoProfile;
import net.recasino.model.LeaderboardEntry;
import net.recasino.util.ColorUtil;
import org.bukkit.OfflinePlayer;

import java.util.List;

public final class CasinoPlaceholderExpansion extends PlaceholderExpansion {

    private final ReCasino plugin;

    public CasinoPlaceholderExpansion(ReCasino plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "recasino";
    }

    @Override
    public String getAuthor() {
        return "OpenAI";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player != null && player.isOnline()) {
            CasinoProfile profile = plugin.getProfileService().getProfile(player.getPlayer());
            if (params.equalsIgnoreCase("games")) {
                return String.valueOf(profile.getTotalGames());
            }
            if (params.equalsIgnoreCase("wins")) {
                return String.valueOf(profile.getTotalWins());
            }
            if (params.equalsIgnoreCase("profit")) {
                return ColorUtil.formatNumber(profile.getNetProfit());
            }
            if (params.equalsIgnoreCase("best_win")) {
                return ColorUtil.formatNumber(profile.getBestWin());
            }
            if (params.equalsIgnoreCase("best_crash")) {
                return ColorUtil.formatNumber(profile.getBestCrashMultiplier());
            }
        }

        if (params.startsWith("top_profit_")) {
            return resolveTop(params.substring("top_profit_".length()), plugin.getProfileService().getTopProfiles(CasinoProfile::getNetProfit, 10));
        }
        if (params.startsWith("top_wins_")) {
            return resolveTop(params.substring("top_wins_".length()), plugin.getProfileService().getTopProfiles(profile -> (double) profile.getTotalWins(), 10));
        }

        return "";
    }

    private String resolveTop(String suffix, List<LeaderboardEntry> entries) {
        String[] parts = suffix.split("_");
        if (parts.length != 2) {
            return "";
        }

        int index;
        try {
            index = Integer.parseInt(parts[0]) - 1;
        } catch (NumberFormatException exception) {
            return "";
        }

        if (index < 0 || index >= entries.size()) {
            return "";
        }

        LeaderboardEntry entry = entries.get(index);
        if (parts[1].equalsIgnoreCase("name")) {
            return entry.getPlayerName();
        }
        if (parts[1].equalsIgnoreCase("value")) {
            return ColorUtil.formatNumber(entry.getValue());
        }
        return "";
    }
}

