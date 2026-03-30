package net.recasino.api.player;

import net.recasino.model.CurrencyType;

public interface CasinoPlayerProfile {

    String getPlayerName();

    void setPlayerName(String playerName);

    double getBet(CurrencyType currencyType);

    void setBet(CurrencyType currencyType, double value);

    double getRillikBalance();

    void setRillikBalance(double rillikBalance);

    CurrencyType getMinerCurrency();

    void setMinerCurrency(CurrencyType minerCurrency);

    void toggleMinerCurrency();

    int getTotalGames();

    int getTotalWins();

    int getTotalLosses();

    double getTotalWagered();

    double getTotalWon();

    double getBestWin();

    double getBestCrashMultiplier();

    double getNetProfit();

    double getWinRate();

    void recordGameStart(double stake);

    void cancelGameStart(double stake);

    void recordGameResult(double payout);

    void recordCrashCashout(double multiplier);
}
