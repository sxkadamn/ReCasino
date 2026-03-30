package net.recasino.service;

import net.recasino.ReCasino;
import net.recasino.config.CasinoConfig;
import net.recasino.model.CasinoProfile;
import net.recasino.model.CrashSession;
import net.recasino.model.CurrencyType;
import net.recasino.model.GameMode;
import net.recasino.model.HorseRacingSession;
import net.recasino.model.JackpotSession;
import net.recasino.model.MinerSession;
import net.recasino.model.Prize;
import net.recasino.model.SpinResult;
import net.recasino.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class CasinoService {

    private static final int MINER_BOARD_SIZE = 9;
    private static final long JACKPOT_RESULT_SHOW_MILLIS = 5000L;
    private static final int JACKPOT_STRIP_SIZE = 7;

    private final ReCasino plugin;
    private final CasinoConfig config;
    private final EconomyService economyService;
    private final ProfileService profileService;
    private final Random random;
    private final Set<UUID> activeSpins;
    private final Map<UUID, MinerSession> minerSessions;
    private final Map<UUID, HorseRacingSession> horseRacingSessions;
    private final Map<UUID, CrashSession> crashSessions;
    private final JackpotSession jackpotSession;
    private BukkitTask jackpotTask;
    private BukkitTask jackpotAnimationTask;

    public CasinoService(ReCasino plugin, CasinoConfig config, EconomyService economyService, ProfileService profileService) {
        this.plugin = plugin;
        this.config = config;
        this.economyService = economyService;
        this.profileService = profileService;
        this.random = new Random();
        this.activeSpins = new HashSet<UUID>();
        this.minerSessions = new HashMap<UUID, MinerSession>();
        this.horseRacingSessions = new HashMap<UUID, HorseRacingSession>();
        this.crashSessions = new HashMap<UUID, CrashSession>();
        this.jackpotSession = new JackpotSession();
    }

    public void initialize() {
        shutdown();
        jackpotTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (jackpotSession.getState() == JackpotSession.State.COUNTDOWN
                        && jackpotSession.getParticipantCount() >= 2
                        && jackpotSession.getRemainingSeconds() <= 0L) {
                    startJackpotAnimation();
                }

                if (jackpotSession.getState() == JackpotSession.State.RESULT
                        && jackpotSession.getResultRemainingSeconds() <= 0L) {
                    jackpotSession.clearParticipants();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void shutdown() {
        if (jackpotTask != null) {
            jackpotTask.cancel();
            jackpotTask = null;
        }
        if (jackpotAnimationTask != null) {
            jackpotAnimationTask.cancel();
            jackpotAnimationTask = null;
        }
    }

    public void changeBet(CasinoProfile profile, CurrencyType currencyType, double delta) {
        double current = profile.getBet(currencyType);
        double max = config.getMaxBet(currencyType);
        profile.setBet(currencyType, Math.max(1.0D, Math.min(max, current + delta)));
    }

    public double resolveBetDelta(CurrencyType currencyType, ClickType clickType) {
        if (currencyType == CurrencyType.MONEY) {
            if (clickType == ClickType.SHIFT_RIGHT) {
                return 100000.0D;
            }
            if (clickType == ClickType.SHIFT_LEFT) {
                return 50000.0D;
            }
            if (clickType == ClickType.RIGHT) {
                return 10000.0D;
            }
            return 5000.0D;
        }
        if (clickType == ClickType.SHIFT_RIGHT) {
            return 5000.0D;
        }
        if (clickType == ClickType.SHIFT_LEFT) {
            return 1000.0D;
        }
        if (clickType == ClickType.RIGHT) {
            return 500.0D;
        }
        return 100.0D;
    }

    public SpinResult startRoulette(Player player, CasinoProfile profile, CurrencyType currencyType) {
        GameMode gameMode = currencyType == CurrencyType.MONEY ? GameMode.MONEY_ROULETTE : GameMode.RILLIK_ROULETTE;
        String error = reserveStake(player, profile, currencyType);
        if (error != null) {
            return SpinResult.rejected(error);
        }
        return SpinResult.accepted(gameMode, currencyType, profile.getBet(currencyType), roll(config.getPrizes(gameMode)));
    }

    public void finishRoulette(Player player, CasinoProfile profile, SpinResult result) {
        if (!result.isAccepted()) {
            return;
        }
        double payout = settleGame(player, profile, result.getCurrencyType(), result.getStake(), result.getStake() * result.getPrize().getMultiplier());
        player.sendMessage(buildResultMessage(result, payout));
    }

    public Prize generateRoulettePreview(GameMode gameMode) {
        return roll(config.getPrizes(gameMode));
    }

    public long resolveRouletteStepDelay(GameMode mode, int step, int maxSteps) {
        long start = plugin.getCasinoConfig().getAnimationStartIntervalTicks(mode);
        long end = plugin.getCasinoConfig().getAnimationEndIntervalTicks(mode);
        if (maxSteps <= 1 || start == end) {
            return start;
        }

        double progress = (double) step / (double) (maxSteps - 1);
        double eased = progress * progress;
        return Math.max(1L, Math.round(start + (end - start) * eased));
    }

    public MinerSession getMinerSession(UUID playerId) {
        return minerSessions.get(playerId);
    }

    public void clearMinerSession(UUID playerId) {
        minerSessions.remove(playerId);
    }

    public String startMiner(Player player, CasinoProfile profile, int firstCell) {
        UUID playerId = player.getUniqueId();
        if (minerSessions.containsKey(playerId)) {
            return colorPrefix() + ColorUtil.color("&cРаунд уже активен.");
        }

        CurrencyType currencyType = profile.getMinerCurrency();
        String error = reserveStake(player, profile, currencyType);
        if (error != null) {
            return error;
        }

        MinerSession session = new MinerSession(currencyType, profile.getBet(currencyType), createMinerBoard(firstCell));
        minerSessions.put(playerId, session);
        revealSafeCell(player, profile, session, firstCell);
        return null;
    }

    public String revealMinerCell(Player player, CasinoProfile profile, int cellIndex) {
        MinerSession session = minerSessions.get(player.getUniqueId());
        if (session == null) {
            return colorPrefix() + ColorUtil.color("&cНет активного раунда.");
        }
        if (session.isLost()) {
            return colorPrefix() + ColorUtil.color("&cРаунд уже проигран.");
        }
        if (session.isRevealed(cellIndex)) {
            return colorPrefix() + ColorUtil.color("&eЭта клетка уже открыта.");
        }

        if (session.isMine(cellIndex)) {
            session.reveal(cellIndex);
            session.revealAllMines();
            session.markLost();
            activeSpins.remove(player.getUniqueId());
            profile.recordGameResult(0.0D);
            profileService.markDirty(player.getUniqueId());
            player.sendMessage(colorPrefix() + ColorUtil.color("&cМина. Ставка проиграна."));
            return null;
        }

        revealSafeCell(player, profile, session, cellIndex);
        return null;
    }

    public String cashOutMiner(Player player, CasinoProfile profile) {
        MinerSession session = minerSessions.remove(player.getUniqueId());
        if (session == null) {
            return colorPrefix() + ColorUtil.color("&cНет активного раунда.");
        }
        if (session.isLost()) {
            minerSessions.put(player.getUniqueId(), session);
            return colorPrefix() + ColorUtil.color("&cРаунд уже проигран.");
        }
        if (session.getSafeRevealed() <= 0) {
            minerSessions.put(player.getUniqueId(), session);
            return colorPrefix() + ColorUtil.color("&cСначала откройте безопасную клетку.");
        }

        double multiplier = config.getMinerMultiplier(session.getSafeRevealed());
        double payout = settleGame(player, profile, session.getCurrencyType(), session.getStake(), session.getStake() * multiplier);
        player.sendMessage(colorPrefix() + ColorUtil.color("&aВы забрали: &f" + ColorUtil.formatNumber(payout) + " &7(x" + ColorUtil.formatNumber(multiplier) + ")"));
        return null;
    }

    public HorseRacingSession getHorseRacingSession(UUID playerId) {
        return horseRacingSessions.get(playerId);
    }

    public String startHorseRacing(Player player, CasinoProfile profile, HorseRacingSession.Horse horse) {
        if (horseRacingSessions.containsKey(player.getUniqueId())) {
            return colorPrefix() + ColorUtil.color("&cЗаезд уже запущен.");
        }

        String error = reserveStake(player, profile, CurrencyType.MONEY);
        if (error != null) {
            return error;
        }

        horseRacingSessions.put(player.getUniqueId(), new HorseRacingSession(profile.getBet(CurrencyType.MONEY), horse));
        return null;
    }

    public void finishHorseRacing(HorseRacingSession session) {
        if (session == null || session.getPlayerPlace() < 0) {
            return;
        }
        double multiplier = config.getHorseRacingMultiplier(session.getPlayerPlace() + 1);
        session.setReward(multiplier, session.getStake() * multiplier);
    }

    public double claimHorseRacingReward(Player player, CasinoProfile profile) {
        HorseRacingSession session = horseRacingSessions.get(player.getUniqueId());
        if (session == null || session.getState() != HorseRacingSession.State.REWARD) {
            return 0.0D;
        }

        double payout = settleGame(player, profile, CurrencyType.MONEY, session.getStake(), session.getRewardAmount());
        session.markRewardTaken();
        return payout;
    }

    public void clearHorseRacingSession(UUID playerId) {
        HorseRacingSession session = horseRacingSessions.remove(playerId);
        if (session != null && session.getState() == HorseRacingSession.State.TAKEN_REWARD) {
            activeSpins.remove(playerId);
        }
    }

    public CrashSession getCrashSession(UUID playerId) {
        return crashSessions.get(playerId);
    }

    public String startCrash(Player player, CasinoProfile profile, CurrencyType currencyType) {
        UUID playerId = player.getUniqueId();
        if (crashSessions.containsKey(playerId)) {
            return colorPrefix() + ColorUtil.color("&cCrash уже запущен.");
        }

        String error = reserveStake(player, profile, currencyType);
        if (error != null) {
            return error;
        }

        crashSessions.put(playerId, new CrashSession(profile.getBet(currencyType), currencyType, rollCrashPoint()));
        return null;
    }

    public double cashOutCrash(Player player, CasinoProfile profile) {
        CrashSession session = crashSessions.get(player.getUniqueId());
        if (session == null || session.getState() != CrashSession.State.RUNNING) {
            return 0.0D;
        }

        session.cashOut();
        double payout = settleGame(player, profile, session.getCurrencyType(), session.getStake(), session.getStake() * session.getCurrentMultiplier());
        profile.recordCrashCashout(session.getCurrentMultiplier());
        profileService.markDirty(player.getUniqueId());
        return payout;
    }

    public void finishCrashAsLose(Player player, CasinoProfile profile) {
        CrashSession session = crashSessions.get(player.getUniqueId());
        if (session == null || session.getState() != CrashSession.State.CRASHED) {
            return;
        }
        activeSpins.remove(player.getUniqueId());
        profile.recordGameResult(0.0D);
        profileService.markDirty(player.getUniqueId());
    }

    public void clearCrashSession(UUID playerId) {
        crashSessions.remove(playerId);
        activeSpins.remove(playerId);
    }

    public JackpotSession getJackpotSession() {
        return jackpotSession;
    }

    public String joinJackpot(Player player, CasinoProfile profile) {
        UUID playerId = player.getUniqueId();
        if (jackpotSession.hasParticipant(playerId)) {
            return colorPrefix() + ColorUtil.color("&cВы уже участвуете в Jackpot.");
        }
        if (jackpotSession.isAnimating() || jackpotSession.isShowingResult()) {
            return colorPrefix() + ColorUtil.color("&cРозыгрыш уже идёт. Дождитесь следующего раунда.");
        }

        String error = reserveStake(player, profile, CurrencyType.MONEY);
        if (error != null) {
            return error;
        }

        jackpotSession.addParticipant(playerId, profile.getBet(CurrencyType.MONEY));
        if (jackpotSession.getParticipantCount() >= 2 && !jackpotSession.hasCountdown()) {
            jackpotSession.startCountdown(config.getJackpotCountdownSeconds() * 1000L);
            Bukkit.broadcastMessage(colorPrefix() + ColorUtil.color("&6Jackpot запущен. До розыгрыша: &f" + config.getJackpotCountdownSeconds() + " сек."));
        }
        return null;
    }

    public void leaveJackpot(Player player, CasinoProfile profile) {
        Double stake = jackpotSession.removeParticipant(player.getUniqueId());
        if (stake == null) {
            return;
        }

        economyService.deposit(player, stake);
        activeSpins.remove(player.getUniqueId());
        profile.cancelGameStart(stake);
        profileService.markDirty(player.getUniqueId());
        if (jackpotSession.getParticipantCount() < 2 && jackpotSession.getState() == JackpotSession.State.COUNTDOWN) {
            jackpotSession.resetCountdown();
        }
        if (jackpotSession.getParticipantCount() == 0 && jackpotSession.getState() != JackpotSession.State.ANIMATING) {
            jackpotSession.clearParticipants();
        }
    }

    public void clearPlayerState(Player player, CasinoProfile profile) {
        leaveJackpot(player, profile);
        minerSessions.remove(player.getUniqueId());
        horseRacingSessions.remove(player.getUniqueId());
        crashSessions.remove(player.getUniqueId());
        activeSpins.remove(player.getUniqueId());
    }

    private void startJackpotAnimation() {
        if (jackpotSession.getParticipantCount() < 2) {
            jackpotSession.resetCountdown();
            return;
        }
        if (jackpotAnimationTask != null) {
            jackpotAnimationTask.cancel();
            jackpotAnimationTask = null;
        }

        double totalPot = jackpotSession.getTotalPot();
        double ticket = random.nextDouble() * totalPot;
        double current = 0.0D;
        UUID winnerId = null;

        for (Map.Entry<UUID, Double> entry : jackpotSession.getParticipants().entrySet()) {
            current += entry.getValue();
            if (ticket <= current) {
                winnerId = entry.getKey();
                break;
            }
        }

        if (winnerId == null) {
            winnerId = jackpotSession.getParticipants().keySet().iterator().next();
        }

        jackpotSession.beginAnimation(winnerId, jackpotSession.getParticipants().getOrDefault(winnerId, 0.0D));
        Bukkit.broadcastMessage(colorPrefix() + ColorUtil.color("&6Jackpot начался. Идёт анимация розыгрыша..."));
        openJackpotAnimationForParticipants();

        List<UUID> cycle = new ArrayList<UUID>(jackpotSession.getParticipants().keySet());
        List<UUID> strip = buildInitialJackpotStrip(cycle);
        jackpotSession.setAnimationDisplayIds(strip);
        jackpotSession.setHighlightedParticipantId(strip.get(JACKPOT_STRIP_SIZE / 2));
        updateJackpotAnimationViewers();

        int totalSteps = Math.max(30, cycle.size() * 12);
        scheduleJackpotAnimationFrame(cycle, strip, 0, totalSteps);
    }

    private void finishJackpotDraw() {
        UUID winnerId = jackpotSession.getWinnerId();
        if (winnerId == null) {
            jackpotSession.clearParticipants();
            return;
        }

        double totalPot = jackpotSession.getTotalPot();
        Player winner = Bukkit.getPlayer(winnerId);
        String winnerName = winner != null ? winner.getName() : jackpotSession.getLastWinnerName();
        jackpotSession.setLastResult(winnerName, totalPot);
        jackpotSession.showResult(JACKPOT_RESULT_SHOW_MILLIS);
        jackpotSession.setAnimationDisplayIds(buildFinalJackpotStrip(winnerId));
        updateJackpotAnimationViewers();

        for (Map.Entry<UUID, Double> entry : jackpotSession.getParticipants().entrySet()) {
            UUID uniqueId = entry.getKey();
            activeSpins.remove(uniqueId);
            Player participant = Bukkit.getPlayer(uniqueId);
            if (participant == null || !participant.isOnline()) {
                continue;
            }

            CasinoProfile profile = profileService.getProfile(participant);
            if (uniqueId.equals(winnerId)) {
                economyService.deposit(participant, totalPot);
                profile.recordGameResult(totalPot);
                participant.sendMessage(colorPrefix() + ColorUtil.color("&aВы выиграли Jackpot: &f" + ColorUtil.formatNumber(totalPot)));
                participant.playSound(participant.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.05F);
            } else {
                profile.recordGameResult(0.0D);
                participant.sendMessage(colorPrefix() + ColorUtil.color("&cВы проиграли розыгрыш Jackpot."));
                participant.playSound(participant.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.7F, 1.0F);
            }
            profileService.markDirty(uniqueId);
        }

        Bukkit.broadcastMessage(colorPrefix() + ColorUtil.color("&6Jackpot выиграл игрок &f" + winnerName + " &6и забрал &f" + ColorUtil.formatNumber(totalPot)));
    }

    private void cancelAnimationTask() {
        if (jackpotAnimationTask != null) {
            jackpotAnimationTask.cancel();
            jackpotAnimationTask = null;
        }
    }

    private void scheduleJackpotAnimationFrame(List<UUID> cycle, List<UUID> strip, int step, int totalSteps) {
        long delay = resolveJackpotAnimationDelay(step, totalSteps);
        jackpotAnimationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (jackpotSession.getParticipantCount() < 2 || jackpotSession.getWinnerId() == null || cycle.isEmpty()) {
                    jackpotSession.clearParticipants();
                    cancelAnimationTask();
                    cancel();
                    return;
                }

                shiftJackpotStrip(strip, cycle, step >= totalSteps - 1 ? jackpotSession.getWinnerId() : null, step);
                jackpotSession.setAnimationDisplayIds(strip);
                jackpotSession.setHighlightedParticipantId(strip.get(JACKPOT_STRIP_SIZE / 2));
                playJackpotAnimationSound(step, totalSteps);
                updateJackpotAnimationViewers();

                if (step >= totalSteps - 1) {
                    finishJackpotDraw();
                    cancelAnimationTask();
                    cancel();
                    return;
                }

                cancel();
                scheduleJackpotAnimationFrame(cycle, strip, step + 1, totalSteps);
            }
        }.runTaskLater(plugin, delay);
    }

    private long resolveJackpotAnimationDelay(int step, int totalSteps) {
        if (totalSteps <= 1) {
            return 1L;
        }
        double progress = (double) step / (double) (totalSteps - 1);
        double eased = progress * progress;
        return Math.max(1L, Math.round(1.0D + eased * 7.0D));
    }

    private List<UUID> buildInitialJackpotStrip(List<UUID> cycle) {
        List<UUID> strip = new ArrayList<UUID>(JACKPOT_STRIP_SIZE);
        for (int i = 0; i < JACKPOT_STRIP_SIZE; i++) {
            strip.add(cycle.get(i % cycle.size()));
        }
        return strip;
    }

    private List<UUID> buildFinalJackpotStrip(UUID winnerId) {
        List<UUID> strip = new ArrayList<UUID>(JACKPOT_STRIP_SIZE);
        for (int i = 0; i < JACKPOT_STRIP_SIZE; i++) {
            strip.add(winnerId);
        }
        return strip;
    }

    private void shiftJackpotStrip(List<UUID> strip, List<UUID> cycle, UUID forcedWinnerId, int step) {
        for (int i = 0; i < strip.size() - 1; i++) {
            strip.set(i, strip.get(i + 1));
        }
        if (forcedWinnerId != null) {
            strip.set(strip.size() - 1, forcedWinnerId);
            return;
        }
        strip.set(strip.size() - 1, cycle.get(step % cycle.size()));
    }

    private void openJackpotAnimationForParticipants() {
        for (UUID participantId : jackpotSession.getParticipants().keySet()) {
            Player participant = Bukkit.getPlayer(participantId);
            if (participant == null || !participant.isOnline()) {
                continue;
            }
            plugin.getMenuFactory().openJackpotAnimation(participant);
        }
    }

    private void updateJackpotAnimationViewers() {
        for (UUID participantId : jackpotSession.getParticipants().keySet()) {
            Player participant = Bukkit.getPlayer(participantId);
            if (participant == null || !participant.isOnline()) {
                continue;
            }
            if (participant.getOpenInventory().getTopInventory().getHolder() instanceof net.recasino.gui.CasinoMenuHolder holder
                    && holder.getMenuType() == net.recasino.gui.MenuType.JACKPOT_ANIMATION) {
                plugin.getMenuFactory().updateJackpotAnimation(participant.getOpenInventory().getTopInventory());
            }
        }
    }

    private void playJackpotAnimationSound(int step, int totalSteps) {
        float progress = totalSteps <= 1 ? 1.0F : (float) step / (float) (totalSteps - 1);
        float pitch = 1.9F - (progress * 1.0F);
        for (UUID participantId : jackpotSession.getParticipants().keySet()) {
            Player participant = Bukkit.getPlayer(participantId);
            if (participant != null && participant.isOnline()) {
                participant.playSound(participant.getLocation(), Sound.UI_BUTTON_CLICK, 0.45F, Math.max(0.8F, pitch));
            }
        }
    }

    private void revealSafeCell(Player player, CasinoProfile profile, MinerSession session, int cellIndex) {
        session.reveal(cellIndex);
        if (session.getSafeRevealed() >= session.getSafeCellsTotal()) {
            double multiplier = config.getMinerMultiplier(session.getSafeRevealed());
            minerSessions.remove(player.getUniqueId());
            double payout = settleGame(player, profile, session.getCurrencyType(), session.getStake(), session.getStake() * multiplier);
            player.sendMessage(colorPrefix() + ColorUtil.color("&aВсе безопасные клетки открыты. Выплата: &f" + ColorUtil.formatNumber(payout)));
            return;
        }
        player.sendMessage(colorPrefix() + ColorUtil.color("&aБезопасно. Текущий cash out: &fx" + ColorUtil.formatNumber(config.getMinerMultiplier(session.getSafeRevealed()))));
    }

    private boolean[] createMinerBoard(int safeIndex) {
        int minesCount = Math.min(config.getMinerMinesCount(), MINER_BOARD_SIZE - 1);
        boolean[] mines = new boolean[MINER_BOARD_SIZE];
        int placed = 0;
        while (placed < minesCount) {
            int index = random.nextInt(MINER_BOARD_SIZE);
            if (index == safeIndex || mines[index]) {
                continue;
            }
            mines[index] = true;
            placed++;
        }
        return mines;
    }

    private double rollCrashPoint() {
        if (random.nextDouble() <= config.getCrashInstantChance()) {
            return config.getCrashMinMultiplier();
        }
        return config.getCrashMinMultiplier() + (config.getCrashMaxMultiplier() - config.getCrashMinMultiplier()) * random.nextDouble();
    }

    private String reserveStake(Player player, CasinoProfile profile, CurrencyType currencyType) {
        UUID playerId = player.getUniqueId();
        if (activeSpins.contains(playerId)) {
            return config.getString("messages.already-spinning");
        }

        double stake = profile.getBet(currencyType);
        if (stake <= 0.0D || stake > config.getMaxBet(currencyType)) {
            return config.getString("messages.invalid-bet");
        }

        if (currencyType == CurrencyType.MONEY) {
            if (!economyService.has(player, stake)) {
                return config.getString("messages.not-enough-money");
            }
            economyService.withdraw(player, stake);
        } else {
            if (profile.getRillikBalance() < stake) {
                return config.getString("messages.not-enough-rillik");
            }
            profile.setRillikBalance(profile.getRillikBalance() - stake);
        }

        activeSpins.add(playerId);
        profile.recordGameStart(stake);
        profileService.markDirty(playerId);
        return null;
    }

    private double settleGame(Player player, CasinoProfile profile, CurrencyType currencyType, double stake, double payout) {
        activeSpins.remove(player.getUniqueId());
        if (currencyType == CurrencyType.MONEY) {
            if (payout > 0.0D) {
                economyService.deposit(player, payout);
            }
        } else if (payout > 0.0D) {
            profile.setRillikBalance(profile.getRillikBalance() + payout);
        }

        profile.recordGameResult(payout);
        profileService.markDirty(player.getUniqueId());
        return payout;
    }

    private Prize roll(List<Prize> prizes) {
        int totalWeight = 0;
        for (Prize prize : prizes) {
            totalWeight += prize.getWeight();
        }

        int ticket = random.nextInt(Math.max(1, totalWeight)) + 1;
        int current = 0;
        for (Prize prize : prizes) {
            current += prize.getWeight();
            if (ticket <= current) {
                return prize;
            }
        }
        return prizes.get(prizes.size() - 1);
    }

    private String buildResultMessage(SpinResult result, double payout) {
        String prefix = ColorUtil.color(plugin.getCasinoConfig().getString("messages.result-prefix"));
        if (result.getPrize().getMultiplier() <= 0.0D) {
            return prefix + ColorUtil.color(plugin.getCasinoConfig().getString("messages.result-lose"));
        }
        return prefix + ColorUtil.color(plugin.getCasinoConfig().getString("messages.result-win")
                .replace("{multiplier}", ColorUtil.formatNumber(result.getPrize().getMultiplier()))
                .replace("{payout}", ColorUtil.formatNumber(payout))
                .replace("{prize}", result.getPrize().getDisplayName()));
    }

    private String colorPrefix() {
        return ColorUtil.color(plugin.getCasinoConfig().getString("messages.result-prefix"));
    }
}

