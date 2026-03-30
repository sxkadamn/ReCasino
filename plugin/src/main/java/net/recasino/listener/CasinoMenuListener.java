package net.recasino.listener;

import net.recasino.ReCasino;
import net.recasino.addon.ApiModeContext;
import net.recasino.addon.ApiRouletteAnimationContext;
import net.recasino.api.animation.RouletteAnimation;
import net.recasino.api.mode.CasinoMode;
import net.recasino.gui.CasinoMenuHolder;
import net.recasino.gui.MenuType;
import net.recasino.model.CasinoProfile;
import net.recasino.model.CrashSession;
import net.recasino.model.CurrencyType;
import net.recasino.model.GameMode;
import net.recasino.model.HorseRacingSession;
import net.recasino.model.JackpotSession;
import net.recasino.model.LeaderboardEntry;
import net.recasino.model.MinerSession;
import net.recasino.model.Prize;
import net.recasino.model.SpinResult;
import net.recasino.util.ColorUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public final class CasinoMenuListener implements Listener {

    private static final int[] MINER_BOARD_SLOTS = {11, 12, 13, 20, 21, 22, 29, 30, 31};

    private final ReCasino plugin;

    public CasinoMenuListener(ReCasino plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(event.getView().getTopInventory().getHolder() instanceof CasinoMenuHolder holder)) {
            return;
        }

        event.setCancelled(true);
        CasinoProfile profile = plugin.getProfileService().getProfile(player);
        MenuType menuType = holder.getMenuType();

        switch (menuType) {
            case MAIN -> handleMain(player, profile, event.getRawSlot());
            case MONEY_ROULETTE -> handleRoulette(player, profile, CurrencyType.MONEY, event.getRawSlot(), event.getClick());
            case RILLIK_ROULETTE -> handleRoulette(player, profile, CurrencyType.RILLIK, event.getRawSlot(), event.getClick());
            case MINER -> handleMiner(player, profile, event.getRawSlot(), event.getClick());
            case HORSE_SELECT -> handleHorseSelect(player, profile, event.getRawSlot(), event.getClick());
            case HORSE_RACING -> handleHorseRacing(player, profile, event.getRawSlot());
            case CRASH -> handleCrash(player, profile, event.getRawSlot(), event.getClick());
            case JACKPOT -> handleJackpot(player, profile, event.getRawSlot(), event.getClick());
            case JACKPOT_ANIMATION -> {
            }
            case ADDON_MODE -> handleAddonMode(player, profile, holder, event);
            case STATS -> handleSimpleBack(player, event.getRawSlot(), 31);
            case LEADERBOARD -> handleSimpleBack(player, event.getRawSlot(), 40);
            case ADMIN_SETTINGS -> handleAdmin(player, event.getRawSlot(), event.getClick());
        }
    }

    private void handleMain(Player player, CasinoProfile profile, int slot) {
        if (slot == 11) {
            plugin.getMenuFactory().openRoulette(player, profile, CurrencyType.MONEY);
        } else if (slot == 13) {
            plugin.getMenuFactory().openRoulette(player, profile, CurrencyType.RILLIK);
        } else if (slot == 15) {
            plugin.getMenuFactory().openMiner(player, profile);
        } else if (slot == 29) {
            HorseRacingSession session = plugin.getCasinoService().getHorseRacingSession(player.getUniqueId());
            if (session != null) {
                plugin.getMenuFactory().openHorseRacing(player, session);
            } else {
                plugin.getMenuFactory().openHorseSelect(player, profile);
            }
        } else if (slot == 31) {
            plugin.getMenuFactory().openCrash(player, profile);
        } else if (slot == 33) {
            plugin.getMenuFactory().openJackpot(player, profile);
            startJackpotMenuUpdates(player, profile);
        } else if (slot == 40 && player.hasPermission("recasino.admin")) {
            plugin.getMenuFactory().openAdminSettings(player);
        } else {
            CasinoMode mode = plugin.getAddonRegistry().resolveMainMenuModes().get(slot);
            if (mode != null) {
                plugin.getMenuFactory().openAddonMode(player, profile, mode);
            }
        }
    }

    private void handleAddonMode(Player player, CasinoProfile profile, CasinoMenuHolder holder, InventoryClickEvent event) {
        CasinoMode mode = plugin.getAddonRegistry().getMode(holder.getAddonModeId());
        if (mode == null) {
            plugin.getMenuFactory().openMain(player);
            return;
        }
        mode.onClick(new ApiModeContext(plugin, mode, player, profile, event.getView().getTopInventory()), event);
    }

    private void handleJackpot(Player player, CasinoProfile profile, int slot, ClickType clickType) {
        JackpotSession session = plugin.getCasinoService().getJackpotSession();
        if (slot == 20) {
            if (!session.hasParticipant(player.getUniqueId()) && !session.isAnimating() && !session.isShowingResult()) {
                plugin.getCasinoService().changeBet(profile, CurrencyType.MONEY, -plugin.getCasinoService().resolveBetDelta(CurrencyType.MONEY, clickType));
            }
            plugin.getMenuFactory().updateJackpot(player.getOpenInventory().getTopInventory(), player, profile);
            return;
        }
        if (slot == 24) {
            if (!session.hasParticipant(player.getUniqueId()) && !session.isAnimating() && !session.isShowingResult()) {
                plugin.getCasinoService().changeBet(profile, CurrencyType.MONEY, plugin.getCasinoService().resolveBetDelta(CurrencyType.MONEY, clickType));
            }
            plugin.getMenuFactory().updateJackpot(player.getOpenInventory().getTopInventory(), player, profile);
            return;
        }
        if (slot == 31) {
            String error = plugin.getCasinoService().joinJackpot(player, profile);
            if (error != null) {
                player.sendMessage(ColorUtil.color(error));
            }
            plugin.getMenuFactory().updateJackpot(player.getOpenInventory().getTopInventory(), player, profile);
            return;
        }
        if (slot == 36) {
            plugin.getMenuFactory().openMain(player);
        }
    }

    private void handleRoulette(Player player, CasinoProfile profile, CurrencyType currencyType, int slot, ClickType clickType) {
        if (slot == 20) {
            plugin.getCasinoService().changeBet(profile, currencyType, -plugin.getCasinoService().resolveBetDelta(currencyType, clickType));
            plugin.getMenuFactory().openRoulette(player, profile, currencyType);
            return;
        }
        if (slot == 24) {
            plugin.getCasinoService().changeBet(profile, currencyType, plugin.getCasinoService().resolveBetDelta(currencyType, clickType));
            plugin.getMenuFactory().openRoulette(player, profile, currencyType);
            return;
        }
        if (slot == 31) {
            SpinResult result = plugin.getCasinoService().startRoulette(player, profile, currencyType);
            if (!result.isAccepted()) {
                player.sendMessage(ColorUtil.color(result.getMessage()));
                return;
            }

            player.sendMessage(ColorUtil.color(plugin.getCasinoConfig().getString("messages.spin-start")));
            animateRoulette(player, profile, currencyType, result);
            return;
        }
        if (slot == 36) {
            plugin.getMenuFactory().openMain(player);
        }
    }

    private void handleMiner(Player player, CasinoProfile profile, int slot, ClickType clickType) {
        MinerSession session = plugin.getCasinoService().getMinerSession(player.getUniqueId());

        if (slot == 23) {
            if (session != null) {
                player.sendMessage(ColorUtil.color("&cНельзя менять валюту во время раунда."));
                return;
            }
            profile.toggleMinerCurrency();
            plugin.getMenuFactory().openMiner(player, profile);
            return;
        }

        if (slot == 24 || slot == 32) {
            if (session != null) {
                player.sendMessage(ColorUtil.color("&cНельзя менять ставку во время раунда."));
                return;
            }
            CurrencyType currencyType = profile.getMinerCurrency();
            double delta = plugin.getCasinoService().resolveBetDelta(currencyType, clickType);
            plugin.getCasinoService().changeBet(profile, currencyType, slot == 24 ? delta : -delta);
            plugin.getMenuFactory().openMiner(player, profile);
            return;
        }

        if (slot == 33) {
            String error = plugin.getCasinoService().cashOutMiner(player, profile);
            if (error != null) {
                player.sendMessage(ColorUtil.color(error));
            }
            plugin.getMenuFactory().openMiner(player, profile);
            return;
        }

        if (slot == 36) {
            plugin.getMenuFactory().openMain(player);
            return;
        }

        int cellIndex = resolveMinerCellIndex(slot);
        if (cellIndex < 0) {
            return;
        }

        String error = session == null
                ? plugin.getCasinoService().startMiner(player, profile, cellIndex)
                : plugin.getCasinoService().revealMinerCell(player, profile, cellIndex);
        if (error != null) {
            player.sendMessage(ColorUtil.color(error));
        }

        MinerSession updatedSession = plugin.getCasinoService().getMinerSession(player.getUniqueId());
        plugin.getMenuFactory().openMiner(player, profile);
        if (updatedSession != null && updatedSession.isLost()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        return;
                    }
                    plugin.getCasinoService().clearMinerSession(player.getUniqueId());
                    if (player.getOpenInventory().getTopInventory().getHolder() instanceof CasinoMenuHolder topHolder && topHolder.getMenuType() == MenuType.MINER) {
                        plugin.getMenuFactory().openMiner(player, profile);
                    }
                }
            }.runTaskLater(plugin, plugin.getCasinoConfig().getMinerLoseDisplayTicks());
        }
    }

    private void handleHorseSelect(Player player, CasinoProfile profile, int slot, ClickType clickType) {
        if (slot == 20) {
            plugin.getCasinoService().changeBet(profile, CurrencyType.MONEY, -plugin.getCasinoService().resolveBetDelta(CurrencyType.MONEY, clickType));
            plugin.getMenuFactory().openHorseSelect(player, profile);
            return;
        }
        if (slot == 24) {
            plugin.getCasinoService().changeBet(profile, CurrencyType.MONEY, plugin.getCasinoService().resolveBetDelta(CurrencyType.MONEY, clickType));
            plugin.getMenuFactory().openHorseSelect(player, profile);
            return;
        }
        if (slot == 36) {
            plugin.getMenuFactory().openMain(player);
            return;
        }

        HorseRacingSession.Horse selectedHorse = resolveHorse(slot);
        if (selectedHorse == null) {
            return;
        }

        String error = plugin.getCasinoService().startHorseRacing(player, profile, selectedHorse);
        if (error != null) {
            player.sendMessage(ColorUtil.color(error));
            return;
        }

        HorseRacingSession session = plugin.getCasinoService().getHorseRacingSession(player.getUniqueId());
        plugin.getMenuFactory().openHorseRacing(player, session);
        animateHorseRacing(player, session);
    }

    private void handleHorseRacing(Player player, CasinoProfile profile, int slot) {
        HorseRacingSession session = plugin.getCasinoService().getHorseRacingSession(player.getUniqueId());
        if (session == null) {
            plugin.getMenuFactory().openMain(player);
            return;
        }

        if (slot == 49 && session.getState() == HorseRacingSession.State.REWARD) {
            double payout = plugin.getCasinoService().claimHorseRacingReward(player, profile);
            String suffix = payout > 0.0D ? "&aНаграда получена: &f" + ColorUtil.formatNumber(payout) : "&cЗаезд завершён без выигрыша.";
            player.sendMessage(ColorUtil.color(plugin.getCasinoConfig().getString("messages.result-prefix") + suffix));
            plugin.getMenuFactory().updateHorseRacing(player.getOpenInventory().getTopInventory(), session);
            return;
        }

        if (slot == 50 && session.getState() == HorseRacingSession.State.TAKEN_REWARD) {
            plugin.getCasinoService().clearHorseRacingSession(player.getUniqueId());
            plugin.getMenuFactory().openHorseSelect(player, profile);
            return;
        }

        if (slot == 53 && session.getState() != HorseRacingSession.State.RUNNING) {
            if (session.getState() == HorseRacingSession.State.TAKEN_REWARD) {
                plugin.getCasinoService().clearHorseRacingSession(player.getUniqueId());
            }
            plugin.getMenuFactory().openMain(player);
        }
    }

    private void handleCrash(Player player, CasinoProfile profile, int slot, ClickType clickType) {
        CrashSession session = plugin.getCasinoService().getCrashSession(player.getUniqueId());

        if (slot == 23) {
            if (session != null && session.getState() == CrashSession.State.RUNNING) {
                player.sendMessage(ColorUtil.color("&cНельзя менять валюту во время Crash."));
                return;
            }
            profile.toggleMinerCurrency();
            plugin.getMenuFactory().openCrash(player, profile);
            return;
        }

        if (slot == 20 || slot == 24) {
            if (session != null && session.getState() == CrashSession.State.RUNNING) {
                player.sendMessage(ColorUtil.color("&cНельзя менять ставку во время Crash."));
                return;
            }
            CurrencyType currencyType = profile.getMinerCurrency();
            double delta = plugin.getCasinoService().resolveBetDelta(currencyType, clickType);
            plugin.getCasinoService().changeBet(profile, currencyType, slot == 24 ? delta : -delta);
            plugin.getMenuFactory().openCrash(player, profile);
            return;
        }

        if (slot == 31) {
            if (session == null) {
                String error = plugin.getCasinoService().startCrash(player, profile, profile.getMinerCurrency());
                if (error != null) {
                    player.sendMessage(ColorUtil.color(error));
                    return;
                }
                plugin.getMenuFactory().openCrash(player, profile);
                animateCrash(player, profile);
                return;
            }

            if (session.getState() == CrashSession.State.RUNNING) {
                double payout = plugin.getCasinoService().cashOutCrash(player, profile);
                player.sendMessage(ColorUtil.color(plugin.getCasinoConfig().getString("messages.result-prefix") + "&aВы забрали: &f" + ColorUtil.formatNumber(payout)));
                plugin.getMenuFactory().updateCrash(player.getOpenInventory().getTopInventory(), profile, session, plugin.getEconomyService().getBalance(player));
                return;
            }

            plugin.getCasinoService().clearCrashSession(player.getUniqueId());
            plugin.getMenuFactory().openCrash(player, profile);
            return;
        }

        if (slot == 36) {
            if (session == null || session.getState() != CrashSession.State.RUNNING) {
                plugin.getCasinoService().clearCrashSession(player.getUniqueId());
                plugin.getMenuFactory().openMain(player);
            }
        }
    }

    private void handleAdmin(Player player, int slot, ClickType clickType) {
        if (!player.hasPermission("recasino.admin")) {
            player.sendMessage(ColorUtil.color(plugin.getCasinoConfig().getString("messages.no-permission")));
            plugin.getMenuFactory().openMain(player);
            return;
        }

        if (slot == 49) {
            plugin.getMenuFactory().openMain(player);
            return;
        }

        int delta = resolveAdminDelta(clickType);
        if (updatePrizeWeightBySlot(slot, delta)) {
            plugin.getMenuFactory().openAdminSettings(player);
            return;
        }

        if (slot == 16) {
            plugin.getCasinoConfig().updateCrashSetting("growth-per-tick", resolveDecimalDelta(clickType, 0.01D, 0.05D), 0.01D, 1.0D);
        } else if (slot == 25) {
            plugin.getCasinoConfig().updateCrashSetting("min-multiplier", resolveDecimalDelta(clickType, 0.1D, 0.5D), 1.1D, 100.0D);
        } else if (slot == 34) {
            plugin.getCasinoConfig().updateCrashSetting("instant-crash-chance", resolveDecimalDelta(clickType, 0.01D, 0.05D), 0.0D, 0.95D);
        } else if (slot == 43) {
            plugin.getCasinoConfig().updateCrashSetting("max-multiplier", resolveDecimalDelta(clickType, 0.1D, 0.5D), plugin.getCasinoConfig().getCrashMinMultiplier(), 100.0D);
        }

        plugin.getMenuFactory().openAdminSettings(player);
    }

    private boolean updatePrizeWeightBySlot(int slot, int delta) {
        return updatePrizeGroup(slot, 10, GameMode.MONEY_ROULETTE, delta)
                || updatePrizeGroup(slot, 19, GameMode.RILLIK_ROULETTE, delta)
                || updatePrizeGroup(slot, 28, GameMode.MINER, delta);
    }

    private boolean updatePrizeGroup(int slot, int startSlot, GameMode mode, int delta) {
        List<Prize> prizes = plugin.getCasinoConfig().getPrizes(mode);
        int index = slot - startSlot;
        if (index < 0 || index >= prizes.size() || index >= 5) {
            return false;
        }
        plugin.getCasinoConfig().updatePrizeWeight(mode, prizes.get(index).getKey(), delta);
        return true;
    }

    private int resolveAdminDelta(ClickType clickType) {
        int base = clickType.isShiftClick() ? 5 : 1;
        return clickType.isRightClick() ? -base : base;
    }

    private double resolveDecimalDelta(ClickType clickType, double base, double shifted) {
        double value = clickType.isShiftClick() ? shifted : base;
        return clickType.isRightClick() ? -value : value;
    }

    private void handleSimpleBack(Player player, int slot, int backSlot) {
        if (slot == backSlot) {
            plugin.getMenuFactory().openMain(player);
        }
    }

    private void animateRoulette(Player player, CasinoProfile profile, CurrencyType currencyType, SpinResult result) {
        RouletteAnimation animation = plugin.getAddonRegistry().getActiveRouletteAnimation();
        animation.play(new ApiRouletteAnimationContext(plugin, player, profile, currencyType, result, player.getOpenInventory().getTopInventory()));
    }

    private void animateHorseRacing(Player player, HorseRacingSession session) {
        new BukkitRunnable() {
            @Override
            public void run() {
                HorseRacingSession currentSession = plugin.getCasinoService().getHorseRacingSession(player.getUniqueId());
                if (!player.isOnline() || currentSession == null || currentSession != session) {
                    cancel();
                    return;
                }

                boolean finished = currentSession.tick(plugin.getCasinoConfig().getHorseRacingMoveChance());
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof CasinoMenuHolder holder && holder.getMenuType() == MenuType.HORSE_RACING) {
                    plugin.getMenuFactory().updateHorseRacing(player.getOpenInventory().getTopInventory(), currentSession);
                }

                if (finished) {
                    plugin.getCasinoService().finishHorseRacing(currentSession);
                    if (player.getOpenInventory().getTopInventory().getHolder() instanceof CasinoMenuHolder holder && holder.getMenuType() == MenuType.HORSE_RACING) {
                        plugin.getMenuFactory().updateHorseRacing(player.getOpenInventory().getTopInventory(), currentSession);
                    }
                    player.playSound(player.getLocation(), currentSession.getPlayerPlace() == 0 ? Sound.ENTITY_PLAYER_LEVELUP : Sound.BLOCK_ANVIL_LAND, 0.9F, 1.0F);
                    cancel();
                    return;
                }

                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3F, 1.6F);
            }
        }.runTaskTimer(plugin, 0L, plugin.getCasinoConfig().getHorseRacingTickInterval());
    }

    private void animateCrash(Player player, CasinoProfile profile) {
        new BukkitRunnable() {
            @Override
            public void run() {
                CrashSession session = plugin.getCasinoService().getCrashSession(player.getUniqueId());
                if (!player.isOnline() || session == null) {
                    cancel();
                    return;
                }

                if (session.getState() != CrashSession.State.RUNNING) {
                    if (player.getOpenInventory().getTopInventory().getHolder() instanceof CasinoMenuHolder holder && holder.getMenuType() == MenuType.CRASH) {
                        plugin.getMenuFactory().updateCrash(player.getOpenInventory().getTopInventory(), profile, session, plugin.getEconomyService().getBalance(player));
                    }
                    cancel();
                    return;
                }

                boolean finished = session.tick(plugin.getCasinoConfig().getCrashGrowthPerTick());
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof CasinoMenuHolder holder && holder.getMenuType() == MenuType.CRASH) {
                    plugin.getMenuFactory().updateCrash(player.getOpenInventory().getTopInventory(), profile, session, plugin.getEconomyService().getBalance(player));
                }

                if (finished) {
                    plugin.getCasinoService().finishCrashAsLose(player, profile);
                    player.sendMessage(ColorUtil.color(plugin.getCasinoConfig().getString("messages.result-prefix") + "&cCrash на x" + ColorUtil.formatNumber(session.getCrashPoint())));
                    player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8F, 1.0F);
                    cancel();
                    return;
                }

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.2F, 1.8F);
            }
        }.runTaskTimer(plugin, 0L, plugin.getCasinoConfig().getCrashTickInterval());
    }

    private void startJackpotMenuUpdates(Player player, CasinoProfile profile) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof CasinoMenuHolder holder) || holder.getMenuType() != MenuType.JACKPOT) {
                    cancel();
                    return;
                }

                plugin.getMenuFactory().updateJackpot(player.getOpenInventory().getTopInventory(), player, profile);
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private int resolveMinerCellIndex(int slot) {
        for (int index = 0; index < MINER_BOARD_SLOTS.length; index++) {
            if (MINER_BOARD_SLOTS[index] == slot) {
                return index;
            }
        }
        return -1;
    }

    private HorseRacingSession.Horse resolveHorse(int slot) {
        if (slot == 11) {
            return HorseRacingSession.Horse.RED;
        }
        if (slot == 13) {
            return HorseRacingSession.Horse.BLUE;
        }
        if (slot == 15) {
            return HorseRacingSession.Horse.GREEN;
        }
        if (slot == 31) {
            return HorseRacingSession.Horse.YELLOW;
        }
        return null;
    }

}

