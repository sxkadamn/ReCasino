package net.recasino.gui;

import net.recasino.addon.AddonRegistry;
import net.recasino.addon.ApiModeContext;
import net.recasino.api.mode.CasinoMode;
import net.recasino.ReCasino;
import net.recasino.config.CasinoConfig;
import net.recasino.model.BetTableDefinition;
import net.recasino.model.BetTableSession;
import net.recasino.model.CasinoProfile;
import net.recasino.model.CrashSession;
import net.recasino.model.CurrencyType;
import net.recasino.model.GameMode;
import net.recasino.model.HorseRacingSession;
import net.recasino.model.JackpotSession;
import net.recasino.model.LeaderboardEntry;
import net.recasino.model.MinerSession;
import net.recasino.model.Prize;
import net.recasino.service.CasinoService;
import net.recasino.service.EconomyService;
import net.recasino.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MenuFactory {

    private static final int[] ROULETTE_STRIP_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int ROULETTE_POINTER_SLOT = 4;
    private static final int[] MINER_BOARD_SLOTS = {11, 12, 13, 20, 21, 22, 29, 30, 31};
    private static final int[] HORSE_SELECT_SLOTS = {11, 13, 15, 31};
    private static final int[] JACKPOT_STRIP_SLOTS = {19, 20, 21, 22, 23, 24, 25};
    private static final int[] BET_TABLE_SLOTS = {11, 13, 15, 29, 31, 33};

    private final CasinoConfig config;
    private final EconomyService economyService;
    private final CasinoService casinoService;
    private final AddonRegistry addonRegistry;
    private final ReCasino plugin;

    public MenuFactory(ReCasino plugin, CasinoConfig config, EconomyService economyService, CasinoService casinoService, AddonRegistry addonRegistry) {
        this.plugin = plugin;
        this.config = config;
        this.economyService = economyService;
        this.casinoService = casinoService;
        this.addonRegistry = addonRegistry;
    }

    public void openMain(Player player) {
        Inventory inventory = Bukkit.createInventory(new CasinoMenuHolder(MenuType.MAIN), config.getInventorySize("main"), ColorUtil.color(config.getMenuTitle("main")));
        ItemStack base = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack accent = item(Material.BROWN_STAINED_GLASS_PANE, " ");
        fillAllEmpty(inventory, base);
        for (int slot : new int[]{1, 3, 5, 7, 19, 25, 37, 39, 41, 43}) {
            inventory.setItem(slot, accent);
        }

        inventory.setItem(4, item(Material.NETHER_STAR, "&#F5C542&lReCasino", "&7Премиальная подборка режимов казино.", "&8• &7Выберите игру и начните сразу."));
        inventory.setItem(11, item(Material.GOLD_INGOT, "&#F5C542Рулетка монет", "&8• &7Классическая рулетка за деньги", "&8• &7Быстрые множители и мгновенный результат"));
        inventory.setItem(13, item(Material.EMERALD, "&#43D17AРулетка рилликов", "&8• &7Режим за внутреннюю валюту", "&8• &7Отдельные шансы и награды"));
        inventory.setItem(15, item(Material.TNT, "&#E1B75FMiner", "&8• &7Открывайте клетки и избегайте мин", "&8• &7Фиксируйте выигрыш через cash out"));
        inventory.setItem(29, item(Material.SADDLE, "&#D98D43Скачки", "&8• &7Выберите лошадь и следите за заездом", "&8• &7Награда зависит от места"));
        inventory.setItem(31, item(Material.FIRE_CHARGE, "&#FF6A3DCrash", "&8• &7Ловите растущий множитель", "&8• &7Успейте выйти до краша"));
        inventory.setItem(33, item(Material.GOLD_NUGGET, "&#FFB347Jackpot", "&8• &7Общий банк между игроками сервера", "&8• &7Победитель забирает весь банк"));
        inventory.setItem(35, item(Material.CLOCK, "&#7FE7CCСтавки", "&8• &7Столы с общим банком", "&8• &7Победитель выбирается по весу ставки"));
        inventory.setItem(
                40,
                player.hasPermission("recasino.admin")
                        ? item(Material.COMPARATOR, "&#FF6A3DНастройка шансов", "&8• &7Админская панель весов и параметров", "&8• &7Доступно только с правами")
                        : base
        );
        for (Map.Entry<Integer, CasinoMode> entry : addonRegistry.resolveMainMenuModes().entrySet()) {
            CasinoMode mode = entry.getValue();
            ItemStack item = mode.createMainMenuItem(player, addonRegistry.getProfile(player));
            if (item != null) {
                inventory.setItem(entry.getKey(), item);
            }
        }

        player.openInventory(inventory);
    }

    public void openAddonMode(Player player, CasinoProfile profile, CasinoMode mode) {
        int size = normalizeInventorySize(mode.getInventorySize());
        Inventory inventory = Bukkit.createInventory(new CasinoMenuHolder(MenuType.ADDON_MODE, mode.getId()), size, ColorUtil.color(mode.getInventoryTitle(player, profile)));
        mode.onOpen(new ApiModeContext(plugin, mode, player, profile, inventory));
        player.openInventory(inventory);
    }

    public void openRoulette(Player player, CasinoProfile profile, CurrencyType currencyType) {
        String menuKey = currencyType == CurrencyType.MONEY ? "money" : "rillik";
        MenuType menuType = currencyType == CurrencyType.MONEY ? MenuType.MONEY_ROULETTE : MenuType.RILLIK_ROULETTE;
        Inventory inventory = Bukkit.createInventory(new CasinoMenuHolder(menuType), config.getInventorySize(menuKey), ColorUtil.color(config.getMenuTitle(menuKey)));
        fillBorders(inventory);

        inventory.setItem(ROULETTE_POINTER_SLOT, item(Material.SPECTRAL_ARROW, "&#FFCC66Указатель", "&7Центральный слот определяет итог."));
        inventory.setItem(20, item(Material.REDSTONE_BLOCK, "&cУменьшить ставку", changeLore(currencyType, false)));
        inventory.setItem(24, item(Material.EMERALD_BLOCK, "&aУвеличить ставку", changeLore(currencyType, true)));
        inventory.setItem(31, item(Material.ANVIL, "&#FFCC66Крутить", "&7Запустить прокрутку с текущей ставкой."));
        inventory.setItem(36, item(Material.ARROW, "&7Назад", "&7Вернуться в главное меню."));

        setRouletteStatus(
                inventory,
                currencyType,
                profile.getBet(currencyType),
                currencyType == CurrencyType.MONEY ? economyService.getBalance(player) : profile.getRillikBalance(),
                null,
                false
        );
        fillRouletteStrip(inventory, buildIdleRouletteStrip());
        player.openInventory(inventory);
    }

    public void openMiner(Player player, CasinoProfile profile) {
        Inventory inventory = Bukkit.createInventory(new CasinoMenuHolder(MenuType.MINER), config.getInventorySize("miner"), ColorUtil.color(config.getMenuTitle("miner")));
        fillBorders(inventory);

        CurrencyType currencyType = profile.getMinerCurrency();
        double bet = profile.getBet(currencyType);
        double balance = currencyType == CurrencyType.MONEY ? economyService.getBalance(player) : profile.getRillikBalance();
        MinerSession session = casinoService.getMinerSession(player.getUniqueId());

        inventory.setItem(15, item(
                Material.EXPERIENCE_BOTTLE,
                "&#E1B75FMiner",
                "&7Валюта: &f" + currencyType.getDisplayName(),
                "&7Текущая ставка: &f" + ColorUtil.formatNumber(bet),
                "&7Доступный баланс: &f" + ColorUtil.formatNumber(balance),
                session == null ? "&7Нажмите на закрытую клетку, чтобы начать." : "&7Открыто безопасных: &f" + session.getSafeRevealed() + "&7/&f" + session.getSafeCellsTotal()
        ));
        inventory.setItem(23, item(Material.COMPASS, "&#66D9EFСменить валюту", session == null ? "&7Текущий режим: &f" + currencyType.getDisplayName() : "&cНедоступно во время раунда"));
        inventory.setItem(24, item(Material.EMERALD_BLOCK, "&aУвеличить ставку", session == null ? changeLore(currencyType, true) : new String[]{"&cНедоступно во время раунда"}));
        inventory.setItem(32, item(Material.REDSTONE_BLOCK, "&cУменьшить ставку", session == null ? changeLore(currencyType, false) : new String[]{"&cНедоступно во время раунда"}));
        inventory.setItem(33, buildMinerCashoutItem(session));
        inventory.setItem(36, item(Material.ARROW, "&7Назад", "&7Вернуться в главное меню."));
        fillMinerBoard(inventory, session);

        player.openInventory(inventory);
    }

    public void openHorseSelect(Player player, CasinoProfile profile) {
        Inventory inventory = Bukkit.createInventory(new CasinoMenuHolder(MenuType.HORSE_SELECT), config.getInventorySize("horse-select"), ColorUtil.color(config.getMenuTitle("horse-select")));
        fillBorders(inventory);

        double bet = profile.getBet(CurrencyType.MONEY);
        double balance = economyService.getBalance(player);
        inventory.setItem(20, item(Material.REDSTONE_BLOCK, "&cУменьшить ставку", changeLore(CurrencyType.MONEY, false)));
        inventory.setItem(22, item(Material.SADDLE, "&#D98D43Скачки", "&7Ставка: &f" + ColorUtil.formatNumber(bet), "&7Баланс: &f" + ColorUtil.formatNumber(balance), "&7Выберите лошадь для старта."));
        inventory.setItem(24, item(Material.EMERALD_BLOCK, "&aУвеличить ставку", changeLore(CurrencyType.MONEY, true)));
        inventory.setItem(36, item(Material.ARROW, "&7Назад", "&7Вернуться в главное меню."));

        HorseRacingSession.Horse[] horses = HorseRacingSession.Horse.values();
        for (int index = 0; index < horses.length; index++) {
            HorseRacingSession.Horse horse = horses[index];
            inventory.setItem(HORSE_SELECT_SLOTS[index], item(horse.getHorseMaterial(), "&#D98D43" + horse.getDisplayName(), "&7Нажмите, чтобы поставить на эту лошадь."));
        }

        fillAllEmpty(inventory, item(Material.BROWN_STAINED_GLASS_PANE, " "));
        player.openInventory(inventory);
    }

    public void openHorseRacing(Player player, HorseRacingSession session) {
        Inventory inventory = Bukkit.createInventory(new CasinoMenuHolder(MenuType.HORSE_RACING), config.getInventorySize("horse-racing"), ColorUtil.color(config.getMenuTitle("horse-racing")));
        updateHorseRacing(inventory, session);
        player.openInventory(inventory);
    }

    public void updateHorseRacing(Inventory inventory, HorseRacingSession session) {
        clearInventory(inventory);
        fillAllEmpty(inventory, item(Material.BLACK_STAINED_GLASS_PANE, " "));

        for (HorseRacingSession.Racer racer : session.getRacers()) {
            renderHorseTrack(inventory, racer, session.getSelectedHorse() == racer.getHorse());
        }

        inventory.setItem(48, buildHorseStatus(session));
        inventory.setItem(49, buildHorseRewardButton(session));
        inventory.setItem(50, buildHorseNewGameButton(session));
        inventory.setItem(53, item(Material.ARROW, "&7Назад", "&7Вернуться в главное меню после завершения."));
    }

    public void openCrash(Player player, CasinoProfile profile) {
        Inventory inventory = Bukkit.createInventory(new CasinoMenuHolder(MenuType.CRASH), config.getInventorySize("crash"), ColorUtil.color(config.getMenuTitle("crash")));
        updateCrash(inventory, profile, casinoService.getCrashSession(player.getUniqueId()), economyService.getBalance(player));
        player.openInventory(inventory);
    }

    public void openJackpot(Player player, CasinoProfile profile) {
        Inventory inventory = Bukkit.createInventory(new CasinoMenuHolder(MenuType.JACKPOT), config.getInventorySize("crash"), ColorUtil.color("&#FFB347Jackpot"));
        updateJackpot(inventory, player, profile);
        player.openInventory(inventory);
    }

    public void openBetTables(Player player, CasinoProfile profile) {
        Inventory inventory = Bukkit.createInventory(new CasinoMenuHolder(MenuType.BET_TABLES), config.getInventorySize("bet-tables"), ColorUtil.color(config.getMenuTitle("bet-tables")));
        updateBetTables(inventory, player, profile);
        player.openInventory(inventory);
    }

    public void updateBetTables(Inventory inventory, Player player, CasinoProfile profile) {
        clearInventory(inventory);
        fillBorders(inventory);
        inventory.setItem(4, item(Material.CLOCK, "&#7FE7CCСтолы ставок", "&7Текущая ставка: &f" + ColorUtil.formatNumber(profile.getBet(CurrencyType.MONEY)), "&7Баланс: &f" + ColorUtil.formatNumber(economyService.getBalance(player))));
        inventory.setItem(20, item(Material.REDSTONE_BLOCK, "&cУменьшить ставку", changeLore(CurrencyType.MONEY, false)));
        inventory.setItem(24, item(Material.EMERALD_BLOCK, "&aУвеличить ставку", changeLore(CurrencyType.MONEY, true)));
        inventory.setItem(40, item(Material.ARROW, "&7Назад", "&7Вернуться в главное меню."));

        List<BetTableSession> sessions = casinoService.getBetTableSessions();
        for (int i = 0; i < sessions.size() && i < BET_TABLE_SLOTS.length; i++) {
            BetTableSession session = sessions.get(i);
            BetTableDefinition definition = session.getDefinition();
            inventory.setItem(BET_TABLE_SLOTS[i], item(
                    definition.getMaterial(),
                    definition.getDisplayName(),
                    "&7Банк: &f" + ColorUtil.formatNumber(session.getTotalPot()),
                    "&7Игроков: &f" + session.getParticipantCount(),
                    "&7Лимит: &f" + ColorUtil.formatNumber(definition.getMaxBet()),
                    session.getState() == BetTableSession.State.COUNTDOWN ? "&7Старт через: &f" + session.getRemainingSeconds() + " сек." : session.getState() == BetTableSession.State.RESULT ? "&7Последний победитель: &f" + session.getLastWinnerName() : "&7Ожидание игроков"
            ));
        }

        fillAllEmpty(inventory, item(Material.BLACK_STAINED_GLASS_PANE, " "));
    }

    public void openBetTable(Player player, CasinoProfile profile, BetTableSession session) {
        Inventory inventory = Bukkit.createInventory(new CasinoMenuHolder(MenuType.BET_TABLE, null, session.getDefinition().getId()), config.getInventorySize("bet-table"), ColorUtil.color(config.getMenuTitle("bet-table")));
        updateBetTable(inventory, player, profile, session);
        player.openInventory(inventory);
    }

    public void updateBetTable(Inventory inventory, Player player, CasinoProfile profile, BetTableSession session) {
        clearInventory(inventory);
        fillBorders(inventory);

        double bet = profile.getBet(CurrencyType.MONEY);
        Double joinedStake = session.getStake(player.getUniqueId());
        inventory.setItem(4, item(session.getDefinition().getMaterial(), session.getDefinition().getDisplayName(), "&7Банк: &f" + ColorUtil.formatNumber(session.getTotalPot()), "&7Игроков: &f" + session.getParticipantCount(), "&7Лимит стола: &f" + ColorUtil.formatNumber(session.getDefinition().getMaxBet())));
        inventory.setItem(20, item(Material.REDSTONE_BLOCK, "&cУменьшить ставку", joinedStake == null ? changeLore(CurrencyType.MONEY, false) : new String[]{"&cНедоступно после входа"}));
        inventory.setItem(24, item(Material.EMERALD_BLOCK, "&aУвеличить ставку", joinedStake == null ? changeLore(CurrencyType.MONEY, true) : new String[]{"&cНедоступно после входа"}));
        inventory.setItem(22, item(Material.PAPER, "&#7FE7CCИнформация", "&7Ваша ставка: &f" + ColorUtil.formatNumber(joinedStake == null ? bet : joinedStake), "&7Ваш шанс: &f" + ColorUtil.formatNumber(resolveBetTableChance(session, player.getUniqueId())) + "%", "&7Баланс: &f" + ColorUtil.formatNumber(economyService.getBalance(player))));
        inventory.setItem(31, joinedStake == null ? item(Material.LIME_CONCRETE, "&aВойти за стол", "&7Использовать текущую денежную ставку.") : item(Material.BARRIER, "&cВыйти со стола", "&7Вернуть ставку до старта розыгрыша."));
        inventory.setItem(36, item(Material.ARROW, "&7К списку столов", "&7Вернуться назад."));

        int slot = 10;
        for (Map.Entry<UUID, Double> entry : session.getParticipants().entrySet()) {
            if (slot > 16) {
                break;
            }
            String name = resolveParticipantName(entry.getKey());
            inventory.setItem(slot++, item(Material.PLAYER_HEAD, "&#7FE7CC" + name, "&7Вклад: &f" + ColorUtil.formatNumber(entry.getValue()), "&7Шанс: &f" + ColorUtil.formatNumber(resolveBetTableChance(session, entry.getKey())) + "%"));
        }

        fillAllEmpty(inventory, item(Material.BLACK_STAINED_GLASS_PANE, " "));
    }

    public void openJackpotAnimation(Player player) {
        Inventory inventory = Bukkit.createInventory(new CasinoMenuHolder(MenuType.JACKPOT_ANIMATION), 45, ColorUtil.color("&#FFB347Розыгрыш Jackpot"));
        updateJackpotAnimation(inventory);
        player.openInventory(inventory);
    }

    public void updateJackpot(Inventory inventory, Player player, CasinoProfile profile) {
        clearInventory(inventory);
        fillBorders(inventory);

        JackpotSession session = casinoService.getJackpotSession();
        UUID playerId = player.getUniqueId();
        double bet = profile.getBet(CurrencyType.MONEY);
        boolean joined = session.hasParticipant(playerId);
        boolean locked = joined || session.isAnimating() || session.isShowingResult();

        inventory.setItem(20, item(Material.REDSTONE_BLOCK, "&cУменьшить ставку", locked ? new String[]{"&cНедоступно в текущей стадии"} : changeLore(CurrencyType.MONEY, false)));
        inventory.setItem(24, item(Material.EMERALD_BLOCK, "&aУвеличить ставку", locked ? new String[]{"&cНедоступно в текущей стадии"} : changeLore(CurrencyType.MONEY, true)));
        inventory.setItem(22, buildJackpotStateItem(session, bet));
        inventory.setItem(31, buildJackpotMainButton(session, playerId));
        inventory.setItem(36, item(Material.ARROW, "&7Назад", "&7Вернуться в главное меню."));

        int slot = 10;
        for (Map.Entry<UUID, Double> entry : session.getParticipants().entrySet()) {
            if (slot > 16) {
                break;
            }
            inventory.setItem(slot++, buildJackpotParticipantItem(entry.getKey(), entry.getValue(), session));
        }

        fillAllEmpty(inventory, item(Material.BLACK_STAINED_GLASS_PANE, " "));
    }

    public void updateJackpotAnimation(Inventory inventory) {
        clearInventory(inventory);
        fillAllEmpty(inventory, item(Material.BLACK_STAINED_GLASS_PANE, " "));

        JackpotSession session = casinoService.getJackpotSession();
        inventory.setItem(4, item(Material.SPECTRAL_ARROW, "&#FFCC66Указатель", "&7Победитель остановится по центру."));
        inventory.setItem(13, item(Material.GOLD_NUGGET, "&#FFB347Общий банк", "&7Банк: &f" + ColorUtil.formatNumber(session.getTotalPot()), "&7Игроков: &f" + session.getParticipantCount()));
        inventory.setItem(31, buildJackpotAnimationStatus(session));
        inventory.setItem(40, item(Material.BLACK_STAINED_GLASS_PANE, " "));

        List<UUID> displayIds = session.getAnimationDisplayIds();
        for (int i = 0; i < JACKPOT_STRIP_SLOTS.length; i++) {
            UUID participantId = i < displayIds.size() ? displayIds.get(i) : null;
            inventory.setItem(JACKPOT_STRIP_SLOTS[i], buildJackpotAnimationSlot(participantId, session, i == JACKPOT_STRIP_SLOTS.length / 2));
        }
    }

    public void updateCrash(Inventory inventory, CasinoProfile profile, CrashSession session, double moneyBalance) {
        clearInventory(inventory);
        fillBorders(inventory);

        CurrencyType currencyType = profile.getMinerCurrency();
        double balance = currencyType == CurrencyType.MONEY ? moneyBalance : profile.getRillikBalance();
        inventory.setItem(20, item(Material.REDSTONE_BLOCK, "&cУменьшить ставку", session == null ? changeLore(currencyType, false) : new String[]{"&cНедоступно во время раунда"}));
        inventory.setItem(24, item(Material.EMERALD_BLOCK, "&aУвеличить ставку", session == null ? changeLore(currencyType, true) : new String[]{"&cНедоступно во время раунда"}));
        inventory.setItem(22, item(
                Material.FIRE_CHARGE,
                "&#FF6A3DCrash",
                "&7Валюта: &f" + currencyType.getDisplayName(),
                "&7Ставка: &f" + ColorUtil.formatNumber(profile.getBet(currencyType)),
                "&7Баланс: &f" + ColorUtil.formatNumber(balance),
                session == null ? "&7Нажмите Старт, чтобы начать." : "&7Текущий множитель: &f" + ColorUtil.formatNumber(session.getCurrentMultiplier())
        ));
        inventory.setItem(31, buildCrashMainButton(session));
        inventory.setItem(23, item(Material.COMPASS, "&#66D9EFСменить валюту", session == null ? "&7Переключить между монетами и рилликами." : "&cНедоступно во время раунда"));
        inventory.setItem(36, item(Material.ARROW, "&7Назад", "&7Вернуться в главное меню."));
    }

    public void openStats(Player player, CasinoProfile profile) {
        Inventory inventory = Bukkit.createInventory(new CasinoMenuHolder(MenuType.STATS), config.getInventorySize("stats"), ColorUtil.color(config.getMenuTitle("stats")));
        fillAllEmpty(inventory, item(Material.BLUE_STAINED_GLASS_PANE, " "));
        inventory.setItem(11, item(Material.PAPER, "&#66D9EFОбщая статистика", "&7Всего игр: &f" + profile.getTotalGames(), "&7Побед: &f" + profile.getTotalWins(), "&7Поражений: &f" + profile.getTotalLosses(), "&7Винрейт: &f" + ColorUtil.formatNumber(profile.getWinRate()) + "%"));
        inventory.setItem(13, item(Material.GOLD_INGOT, "&#F5C542Финансы", "&7Поставлено: &f" + ColorUtil.formatNumber(profile.getTotalWagered()), "&7Выиграно: &f" + ColorUtil.formatNumber(profile.getTotalWon()), "&7Профит: &f" + ColorUtil.formatNumber(profile.getNetProfit())));
        inventory.setItem(15, item(Material.DIAMOND, "&#66D9EFРекорды", "&7Лучший выигрыш: &f" + ColorUtil.formatNumber(profile.getBestWin()), "&7Лучший Crash: &fx" + ColorUtil.formatNumber(profile.getBestCrashMultiplier())));
        inventory.setItem(31, item(Material.ARROW, "&7Назад", "&7Вернуться в главное меню."));
        player.openInventory(inventory);
    }

    public void openLeaderboard(Player player, List<LeaderboardEntry> topProfit, List<LeaderboardEntry> topWins) {
        Inventory inventory = Bukkit.createInventory(new CasinoMenuHolder(MenuType.LEADERBOARD), config.getInventorySize("leaderboard"), ColorUtil.color(config.getMenuTitle("leaderboard")));
        fillAllEmpty(inventory, item(Material.YELLOW_STAINED_GLASS_PANE, " "));

        for (int index = 0; index < Math.min(5, topProfit.size()); index++) {
            LeaderboardEntry entry = topProfit.get(index);
            inventory.setItem(10 + index, item(Material.GOLD_BLOCK, "&#F5C542Топ профита #" + (index + 1), "&7Игрок: &f" + entry.getPlayerName(), "&7Профит: &f" + ColorUtil.formatNumber(entry.getValue())));
        }

        for (int index = 0; index < Math.min(5, topWins.size()); index++) {
            LeaderboardEntry entry = topWins.get(index);
            inventory.setItem(28 + index, item(Material.EMERALD_BLOCK, "&#43D17AТоп побед #" + (index + 1), "&7Игрок: &f" + entry.getPlayerName(), "&7Побед: &f" + ColorUtil.formatNumber(entry.getValue())));
        }

        inventory.setItem(40, item(Material.ARROW, "&7Назад", "&7Вернуться в главное меню."));
        player.openInventory(inventory);
    }

    public void openAdminSettings(Player player) {
        Inventory inventory = Bukkit.createInventory(new CasinoMenuHolder(MenuType.ADMIN_SETTINGS), config.getInventorySize("admin"), ColorUtil.color(config.getMenuTitle("admin")));
        clearInventory(inventory);
        fillAllEmpty(inventory, item(Material.BLACK_STAINED_GLASS_PANE, " "));

        placePrizeGroup(inventory, 10, GameMode.MONEY_ROULETTE, "Монеты");
        placePrizeGroup(inventory, 19, GameMode.RILLIK_ROULETTE, "Риллики");
        placePrizeGroup(inventory, 28, GameMode.MINER, "Miner");

        inventory.setItem(16, item(Material.FIRE_CHARGE, "&#FF6A3DCrash: рост", "&7Сейчас: &f" + ColorUtil.formatNumber(config.getCrashGrowthPerTick()), "&7ЛКМ/ПКМ: +/- 0.01", "&7Shift: +/- 0.05"));
        inventory.setItem(25, item(Material.CLOCK, "&#FF6A3DCrash: минимум", "&7Сейчас: &f" + ColorUtil.formatNumber(config.getCrashMinMultiplier()), "&7ЛКМ/ПКМ: +/- 0.1", "&7Shift: +/- 0.5"));
        inventory.setItem(34, item(Material.TNT, "&#FF6A3DCrash: шанс мгновенного краша", "&7Сейчас: &f" + ColorUtil.formatNumber(config.getCrashInstantChance()), "&7ЛКМ/ПКМ: +/- 0.01", "&7Shift: +/- 0.05"));
        inventory.setItem(43, item(Material.BLAZE_POWDER, "&#FF6A3DCrash: максимум", "&7Сейчас: &f" + ColorUtil.formatNumber(config.getCrashMaxMultiplier()), "&7ЛКМ/ПКМ: +/- 0.1", "&7Shift: +/- 0.5"));
        inventory.setItem(49, item(Material.ARROW, "&7Назад", "&7Вернуться в главное меню."));
        player.openInventory(inventory);
    }

    public void updateRoulettePreview(Inventory inventory, CurrencyType currencyType, double bet, double balance, List<Prize> strip, Prize centerPrize, boolean finalFrame) {
        setRouletteStatus(inventory, currencyType, bet, balance, centerPrize, true);
        fillRouletteStrip(inventory, strip);
        if (finalFrame) {
            inventory.setItem(31, item(centerPrize.getMaterial(), "&aРезультат готов", "&7В центре остановился: &f" + centerPrize.getDisplayName()));
        }
    }

    private void placePrizeGroup(Inventory inventory, int startSlot, GameMode mode, String title) {
        List<Prize> prizes = config.getPrizes(mode);
        for (int index = 0; index < prizes.size() && index < 5; index++) {
            Prize prize = prizes.get(index);
            inventory.setItem(startSlot + index, item(prize.getMaterial(), "&#F5C542" + title + ": " + prize.getDisplayName(), "&7Вес: &f" + prize.getWeight(), "&7ЛКМ/ПКМ: +/- 1", "&7Shift: +/- 5"));
        }
    }

    private ItemStack buildJackpotStateItem(JackpotSession session, double bet) {
        if (session.getState() == JackpotSession.State.ANIMATING) {
            String name = resolveParticipantName(session.getHighlightedParticipantId());
            return item(Material.CLOCK, "&#FFB347Jackpot", "&7Ваша ставка: &f" + ColorUtil.formatNumber(bet), "&7Общий банк: &f" + ColorUtil.formatNumber(session.getTotalPot()), "&7Игроков в банке: &f" + session.getParticipantCount(), "&6Идёт прокрутка...", "&7Сейчас на: &f" + name);
        }
        if (session.getState() == JackpotSession.State.RESULT) {
            return item(Material.FIREWORK_STAR, "&#FFB347Jackpot", "&7Победитель: &f" + session.getLastWinnerName(), "&7Выигрыш: &f" + ColorUtil.formatNumber(session.getLastPayout()), "&7Новый раунд через: &f" + session.getResultRemainingSeconds() + " сек.");
        }
        return item(Material.GOLD_NUGGET, "&#FFB347Jackpot", "&7Ваша ставка: &f" + ColorUtil.formatNumber(bet), "&7Общий банк: &f" + ColorUtil.formatNumber(session.getTotalPot()), "&7Игроков в банке: &f" + session.getParticipantCount(), session.hasCountdown() ? "&7До розыгрыша: &f" + session.getRemainingSeconds() + " сек." : "&7Ожидание второго игрока");
    }

    private ItemStack buildJackpotMainButton(JackpotSession session, UUID viewerId) {
        if (session.getState() == JackpotSession.State.ANIMATING) {
            return item(Material.GLOWSTONE_DUST, "&6Прокрутка...", "&7Система выбирает победителя.", "&7Текущий слот: &f" + resolveParticipantName(session.getHighlightedParticipantId()));
        }
        if (session.getState() == JackpotSession.State.RESULT) {
            return item(Material.GOLD_BLOCK, "&aПобедитель определён", "&7Игрок: &f" + session.getLastWinnerName(), "&7Банк: &f" + ColorUtil.formatNumber(session.getLastPayout()));
        }
        if (session.hasParticipant(viewerId)) {
            return item(Material.GOLD_BLOCK, "&aВы участвуете", "&7Последний победитель: &f" + session.getLastWinnerName(), "&7Последний выигрыш: &f" + ColorUtil.formatNumber(session.getLastPayout()));
        }
        return item(Material.GOLD_BLOCK, "&#FFB347Войти в Jackpot", "&7Последний победитель: &f" + session.getLastWinnerName(), "&7Последний выигрыш: &f" + ColorUtil.formatNumber(session.getLastPayout()));
    }

    private ItemStack buildJackpotParticipantItem(UUID participantId, double stake, JackpotSession session) {
        boolean highlighted = participantId.equals(session.getHighlightedParticipantId());
        boolean winner = session.isShowingResult() && participantId.equals(session.getWinnerId());
        Material material = winner ? Material.TOTEM_OF_UNDYING : highlighted ? Material.GLOWSTONE_DUST : Material.PLAYER_HEAD;
        double chance = session.getTotalPot() <= 0.0D ? 0.0D : stake / session.getTotalPot() * 100.0D;
        String name = resolveParticipantName(participantId);
        String title = winner ? "&a" + name + " &7[победитель]" : highlighted ? "&6" + name + " &7[выбор]" : "&#FFB347" + name;
        return item(material, title, "&7Вклад: &f" + ColorUtil.formatNumber(stake), "&7Шанс: &f" + ColorUtil.formatNumber(chance) + "%");
    }

    private ItemStack buildJackpotAnimationStatus(JackpotSession session) {
        if (session.isShowingResult()) {
            return item(Material.GOLD_BLOCK, "&aПобедитель найден", "&7Игрок: &f" + session.getLastWinnerName(), "&7Забрал: &f" + ColorUtil.formatNumber(session.getLastPayout()), "&7Окно закроется после показа результата.");
        }
        return item(Material.CLOCK, "&#FFB347Прокрутка...", "&7Лента замедляется перед выбором победителя.", "&7Следите за центральным слотом.");
    }

    private ItemStack buildJackpotAnimationSlot(UUID participantId, JackpotSession session, boolean center) {
        if (participantId == null) {
            return item(Material.BLACK_STAINED_GLASS_PANE, " ");
        }

        String name = resolveParticipantName(participantId);
        boolean highlighted = participantId.equals(session.getHighlightedParticipantId());
        boolean winner = session.isShowingResult() && participantId.equals(session.getWinnerId());
        Material material = winner ? Material.TOTEM_OF_UNDYING : center ? Material.GOLD_INGOT : highlighted ? Material.GLOWSTONE_DUST : Material.PLAYER_HEAD;
        String title = winner ? "&a" + name + " &7[победитель]" : center ? "&#FFCC66> " + name + " &#FFCC66<" : highlighted ? "&6" + name : "&#FFB347" + name;
        double stake = session.getParticipants().getOrDefault(participantId, 0.0D);
        double chance = session.getTotalPot() <= 0.0D ? 0.0D : stake / session.getTotalPot() * 100.0D;
        return item(material, title, "&7Вклад: &f" + ColorUtil.formatNumber(stake), "&7Шанс: &f" + ColorUtil.formatNumber(chance) + "%");
    }

    private String resolveParticipantName(UUID participantId) {
        if (participantId == null) {
            return "Игрок";
        }
        Player player = Bukkit.getPlayer(participantId);
        return player != null ? player.getName() : participantId.toString().substring(0, 8);
    }

    private double resolveBetTableChance(BetTableSession session, UUID playerId) {
        double stake = session.getParticipants().getOrDefault(playerId, 0.0D);
        return session.getTotalPot() <= 0.0D ? 0.0D : stake / session.getTotalPot() * 100.0D;
    }

    private ItemStack buildCrashMainButton(CrashSession session) {
        if (session == null) {
            return item(Material.ANVIL, "&#FF6A3DСтарт", "&7Начать раунд Crash.");
        }
        if (session.getState() == CrashSession.State.RUNNING) {
            return item(Material.GOLD_INGOT, "&aЗабрать", "&7Текущий множитель: &fx" + ColorUtil.formatNumber(session.getCurrentMultiplier()), "&7Нажмите, чтобы зафиксировать выигрыш.");
        }
        if (session.getState() == CrashSession.State.CASHED_OUT) {
            return item(Material.EMERALD_BLOCK, "&aВы забрали выигрыш", "&7Откройте новую игру.");
        }
        return item(Material.TNT, "&cКраш", "&7Раунд уже завершился.");
    }

    private List<Prize> buildIdleRouletteStrip() {
        List<Prize> strip = new ArrayList<Prize>();
        for (int i = 0; i < ROULETTE_STRIP_SLOTS.length; i++) {
            strip.add(new Prize("idle", "&#FFCC66?", Material.GLOWSTONE_DUST, 1, 0.0D));
        }
        return strip;
    }

    private void fillRouletteStrip(Inventory inventory, List<Prize> strip) {
        for (int i = 0; i < ROULETTE_STRIP_SLOTS.length; i++) {
            Prize prize = strip.get(i);
            boolean center = i == ROULETTE_STRIP_SLOTS.length / 2;
            inventory.setItem(ROULETTE_STRIP_SLOTS[i], item(prize.getMaterial(), center ? "&#FFCC66> " + prize.getDisplayName() + " &#FFCC66<" : prize.getDisplayName(), "&7Множитель: &f" + ColorUtil.formatNumber(prize.getMultiplier()), center ? "&7Этот слот участвует в результате." : "&8Прокрутка ленты"));
        }
    }

    private void fillMinerBoard(Inventory inventory, MinerSession session) {
        for (int index = 0; index < MINER_BOARD_SLOTS.length; index++) {
            inventory.setItem(MINER_BOARD_SLOTS[index], buildMinerCell(session, index));
        }
    }

    private ItemStack buildMinerCell(MinerSession session, int index) {
        if (session == null) {
            return item(Material.TRAPPED_CHEST, "&6Закрытая клетка", "&7Нажмите сюда, чтобы начать раунд.");
        }
        if (!session.isRevealed(index)) {
            return item(Material.TRAPPED_CHEST, "&6Закрытая клетка", "&7Нажмите, чтобы открыть клетку.");
        }
        if (session.isMine(index)) {
            return item(Material.TNT, "&cМина", "&7Раунд проигран.");
        }
        return item(Material.DIAMOND, "&aБезопасная клетка", "&7Найдено безопасных: &f" + session.getSafeRevealed(), "&7Cash out: &fx" + ColorUtil.formatNumber(config.getMinerMultiplier(session.getSafeRevealed())));
    }

    private ItemStack buildMinerCashoutItem(MinerSession session) {
        if (session == null || session.getSafeRevealed() <= 0 || session.isLost()) {
            return item(Material.BARRIER, "&cЗабрать выигрыш", "&7Сначала откройте безопасную клетку.");
        }
        double multiplier = config.getMinerMultiplier(session.getSafeRevealed());
        double payout = session.getStake() * multiplier;
        return item(Material.GOLD_INGOT, "&aЗабрать выигрыш", "&7Открыто безопасных: &f" + session.getSafeRevealed(), "&7Множитель: &fx" + ColorUtil.formatNumber(multiplier), "&7Выплата: &f" + ColorUtil.formatNumber(payout));
    }

    private void renderHorseTrack(Inventory inventory, HorseRacingSession.Racer racer, boolean selected) {
        for (int column = 0; column < 9; column++) {
            int slot = racer.getRow() * 9 + column;
            inventory.setItem(slot, item(column < racer.getPosition() ? racer.getHorse().getPathMaterial() : Material.BLACK_STAINED_GLASS_PANE, column < racer.getPosition() ? (selected ? "&eВаш след" : "&7След") : " "));
        }

        int horseSlot = racer.getRow() * 9 + racer.getPosition();
        inventory.setItem(horseSlot, item(racer.getHorse().getHorseMaterial(), (selected ? "&e" : "&f") + racer.getHorse().getDisplayName(), racer.isFinished() ? "&7Место: &f" + (racer.getPlace() + 1) : selected ? "&7Это ваша лошадь." : "&7Соперник"));
    }

    private ItemStack buildHorseStatus(HorseRacingSession session) {
        String place = session.getPlayerPlace() >= 0 ? String.valueOf(session.getPlayerPlace() + 1) : "-";
        return item(Material.EXPERIENCE_BOTTLE, "&#D98D43Статус заезда", "&7Ваша лошадь: &f" + session.getSelectedHorse().getDisplayName(), "&7Ставка: &f" + ColorUtil.formatNumber(session.getStake()), "&7Место: &f" + place, session.getState() == HorseRacingSession.State.RUNNING ? "&7Заезд продолжается..." : "&7Множитель: &f" + ColorUtil.formatNumber(session.getRewardMultiplier()));
    }

    private ItemStack buildHorseRewardButton(HorseRacingSession session) {
        if (session.getState() != HorseRacingSession.State.REWARD) {
            return item(Material.BARRIER, "&cЗабрать награду", "&7Доступно после финиша.");
        }
        return item(Material.GOLD_INGOT, "&aЗабрать награду", "&7Место: &f" + (session.getPlayerPlace() + 1), "&7Множитель: &fx" + ColorUtil.formatNumber(session.getRewardMultiplier()), "&7Выплата: &f" + ColorUtil.formatNumber(session.getRewardAmount()));
    }

    private ItemStack buildHorseNewGameButton(HorseRacingSession session) {
        if (session.getState() != HorseRacingSession.State.TAKEN_REWARD) {
            return item(Material.GRAY_DYE, "&7Новая игра", "&7Сначала завершите текущий заезд.");
        }
        return item(Material.LIME_DYE, "&aНовая игра", "&7Вернуться к выбору лошади.");
    }

    private void fillBorders(Inventory inventory) {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null && (slot < 9 || slot >= inventory.getSize() - 9 || slot % 9 == 0 || slot % 9 == 8)) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private void fillAllEmpty(Inventory inventory, ItemStack filler) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private void clearInventory(Inventory inventory) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, null);
        }
    }

    private int normalizeInventorySize(int size) {
        int normalized = Math.max(9, Math.min(54, size));
        int remainder = normalized % 9;
        return remainder == 0 ? normalized : normalized + (9 - remainder);
    }

    private String[] changeLore(CurrencyType currencyType, boolean increase) {
        if (currencyType == CurrencyType.MONEY) {
            return increase
                    ? new String[]{"&7ЛКМ: &f+5,000", "&7ПКМ: &f+10,000", "&7Shift + ЛКМ: &f+50,000", "&7Shift + ПКМ: &f+100,000"}
                    : new String[]{"&7ЛКМ: &f-5,000", "&7ПКМ: &f-10,000", "&7Shift + ЛКМ: &f-50,000", "&7Shift + ПКМ: &f-100,000"};
        }
        return increase
                ? new String[]{"&7ЛКМ: &f+100", "&7ПКМ: &f+500", "&7Shift + ЛКМ: &f+1,000", "&7Shift + ПКМ: &f+5,000"}
                : new String[]{"&7ЛКМ: &f-100", "&7ПКМ: &f-500", "&7Shift + ЛКМ: &f-1,000", "&7Shift + ПКМ: &f-5,000"};
    }

    private void setRouletteStatus(Inventory inventory, CurrencyType currencyType, double bet, double balance, Prize previewPrize, boolean spinning) {
        Material material = previewPrize == null ? Material.EXPERIENCE_BOTTLE : previewPrize.getMaterial();
        String title = currencyType == CurrencyType.MONEY ? "&#F5C542Рулетка монет" : "&#43D17AРулетка рилликов";
        if (!spinning) {
            inventory.setItem(22, item(material, title, "&7Текущая ставка: &f" + ColorUtil.formatNumber(bet), "&7Доступный баланс: &f" + ColorUtil.formatNumber(balance), "&7Лента остановится по центру."));
            return;
        }
        inventory.setItem(22, item(material, title, "&7Прокрутка...", "&7Ставка: &f" + ColorUtil.formatNumber(bet), "&7Центральный приз: &f" + previewPrize.getDisplayName(), "&7Множитель: &f" + ColorUtil.formatNumber(previewPrize.getMultiplier())));
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.color(name));
            List<String> coloredLore = new ArrayList<String>();
            for (String line : lore) {
                coloredLore.add(ColorUtil.color(line));
            }
            meta.setLore(coloredLore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
}

