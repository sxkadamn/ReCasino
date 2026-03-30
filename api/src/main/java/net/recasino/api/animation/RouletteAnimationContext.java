package net.recasino.api.animation;

import net.recasino.api.ReCasinoApi;
import net.recasino.api.player.CasinoPlayerProfile;
import net.recasino.model.CurrencyType;
import net.recasino.model.GameMode;
import net.recasino.model.Prize;
import net.recasino.model.SpinResult;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

import java.util.List;

public interface RouletteAnimationContext {

    ReCasinoApi getApi();

    Plugin getPlugin();

    Player getPlayer();

    CasinoPlayerProfile getProfile();

    Inventory getInventory();

    CurrencyType getCurrencyType();

    GameMode getGameMode();

    SpinResult getResult();

    double getBet();

    double getBalance();

    int getStepCount();

    Prize nextPreviewPrize();

    List<Prize> createInitialStrip();

    long getStepDelay(int step, int maxSteps);

    void render(List<Prize> strip, Prize centerPrize, boolean finalFrame);

    void finish();

    boolean isViewerStillPresent();
}
