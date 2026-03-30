package net.recasino.addon;

import net.recasino.ReCasino;
import net.recasino.api.animation.RouletteAnimationContext;
import net.recasino.api.player.CasinoPlayerProfile;
import net.recasino.model.CasinoProfile;
import net.recasino.model.CurrencyType;
import net.recasino.model.GameMode;
import net.recasino.model.Prize;
import net.recasino.model.SpinResult;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public final class ApiRouletteAnimationContext implements RouletteAnimationContext {

    private final ReCasino plugin;
    private final Player player;
    private final CasinoProfile profile;
    private final CurrencyType currencyType;
    private final SpinResult result;
    private final Inventory inventory;
    private final double bet;
    private final double balance;
    private final int stepCount;
    private boolean finished;

    public ApiRouletteAnimationContext(ReCasino plugin, Player player, CasinoProfile profile, CurrencyType currencyType, SpinResult result, Inventory inventory) {
        this.plugin = plugin;
        this.player = player;
        this.profile = profile;
        this.currencyType = currencyType;
        this.result = result;
        this.inventory = inventory;
        this.bet = profile.getBet(currencyType);
        this.balance = currencyType == CurrencyType.MONEY ? plugin.getEconomyService().getBalance(player) : profile.getRillikBalance();
        this.stepCount = plugin.getCasinoConfig().getAnimationSteps(result.getGameMode());
        this.finished = false;
    }

    @Override
    public net.recasino.api.ReCasinoApi getApi() {
        return plugin.getAddonRegistry();
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    @Override
    public CasinoPlayerProfile getProfile() {
        return profile;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public CurrencyType getCurrencyType() {
        return currencyType;
    }

    @Override
    public GameMode getGameMode() {
        return result.getGameMode();
    }

    @Override
    public SpinResult getResult() {
        return result;
    }

    @Override
    public double getBet() {
        return bet;
    }

    @Override
    public double getBalance() {
        return balance;
    }

    @Override
    public int getStepCount() {
        return stepCount;
    }

    @Override
    public Prize nextPreviewPrize() {
        return plugin.getCasinoService().generateRoulettePreview(result.getGameMode());
    }

    @Override
    public List<Prize> createInitialStrip() {
        List<Prize> strip = new ArrayList<Prize>();
        for (int i = 0; i < 7; i++) {
            strip.add(nextPreviewPrize());
        }
        return strip;
    }

    @Override
    public long getStepDelay(int step, int maxSteps) {
        return plugin.getCasinoService().resolveRouletteStepDelay(result.getGameMode(), step, maxSteps);
    }

    @Override
    public void render(List<Prize> strip, Prize centerPrize, boolean finalFrame) {
        plugin.getMenuFactory().updateRoulettePreview(inventory, currencyType, bet, balance, strip, centerPrize, finalFrame);
    }

    @Override
    public void finish() {
        if (finished) {
            return;
        }

        finished = true;
        plugin.getCasinoService().finishRoulette(player, profile, result);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    return;
                }
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof net.recasino.gui.CasinoMenuHolder) {
                    plugin.getMenuFactory().openRoulette(player, profile, currencyType);
                }
            }
        }.runTaskLater(plugin, plugin.getCasinoConfig().getRouletteResultDisplayTicks());
    }

    @Override
    public boolean isViewerStillPresent() {
        return player.isOnline() && player.getOpenInventory().getTopInventory().equals(inventory);
    }
}
