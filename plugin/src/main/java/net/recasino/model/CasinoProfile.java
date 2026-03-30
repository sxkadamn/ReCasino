package net.recasino.model;

import net.recasino.api.player.CasinoPlayerProfile;
import net.recasino.util.ColorUtil;

public final class CasinoProfile implements CasinoPlayerProfile {

    private String playerName;
    private double moneyBet;
    private double rillikBet;
    private double rillikBalance;
    private CurrencyType minerCurrency;
    private int totalGames;
    private int totalWins;
    private int totalLosses;
    private double totalWagered;
    private double totalWon;
    private double bestWin;
    private double bestCrashMultiplier;

    public CasinoProfile(
            String playerName,
            double moneyBet,
            double rillikBet,
            double rillikBalance,
            CurrencyType minerCurrency,
            int totalGames,
            int totalWins,
            int totalLosses,
            double totalWagered,
            double totalWon,
            double bestWin,
            double bestCrashMultiplier
    ) {
        this.playerName = playerName;
        this.moneyBet = moneyBet;
        this.rillikBet = rillikBet;
        this.rillikBalance = rillikBalance;
        this.minerCurrency = minerCurrency;
        this.totalGames = totalGames;
        this.totalWins = totalWins;
        this.totalLosses = totalLosses;
        this.totalWagered = totalWagered;
        this.totalWon = totalWon;
        this.bestWin = bestWin;
        this.bestCrashMultiplier = bestCrashMultiplier;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public double getMoneyBet() {
        return moneyBet;
    }

    public void setMoneyBet(double moneyBet) {
        this.moneyBet = moneyBet;
    }

    public double getRillikBet() {
        return rillikBet;
    }

    public void setRillikBet(double rillikBet) {
        this.rillikBet = rillikBet;
    }

    public double getRillikBalance() {
        return rillikBalance;
    }

    public void setRillikBalance(double rillikBalance) {
        this.rillikBalance = rillikBalance;
    }

    public CurrencyType getMinerCurrency() {
        return minerCurrency;
    }

    public void setMinerCurrency(CurrencyType minerCurrency) {
        this.minerCurrency = minerCurrency;
    }

    public void toggleMinerCurrency() {
        minerCurrency = minerCurrency == CurrencyType.MONEY ? CurrencyType.RILLIK : CurrencyType.MONEY;
    }

    public double getBet(CurrencyType currencyType) {
        return currencyType == CurrencyType.MONEY ? moneyBet : rillikBet;
    }

    public void setBet(CurrencyType currencyType, double value) {
        if (currencyType == CurrencyType.MONEY) {
            moneyBet = value;
        } else {
            rillikBet = value;
        }
    }

    public int getTotalGames() {
        return totalGames;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public int getTotalLosses() {
        return totalLosses;
    }

    public double getTotalWagered() {
        return totalWagered;
    }

    public double getTotalWon() {
        return totalWon;
    }

    public double getBestWin() {
        return bestWin;
    }

    public double getBestCrashMultiplier() {
        return bestCrashMultiplier;
    }

    public double getNetProfit() {
        return totalWon - totalWagered;
    }

    public double getWinRate() {
        if (totalGames <= 0) {
            return 0.0D;
        }
        return ((double) totalWins / (double) totalGames) * 100.0D;
    }

    public void recordGameStart(double stake) {
        totalGames++;
        totalWagered += stake;
    }

    public void cancelGameStart(double stake) {
        if (totalGames > 0) {
            totalGames--;
        }
        totalWagered = Math.max(0.0D, totalWagered - stake);
    }

    public void recordGameResult(double payout) {
        if (payout > 0.0D) {
            totalWins++;
            totalWon += payout;
            if (payout > bestWin) {
                bestWin = payout;
            }
        } else {
            totalLosses++;
        }
    }

    public void recordCrashCashout(double multiplier) {
        if (multiplier > bestCrashMultiplier) {
            bestCrashMultiplier = multiplier;
        }
    }

    public String getMoneyBetFormatted() {
        return ColorUtil.formatNumber(moneyBet);
    }

    public String getRillikBetFormatted() {
        return ColorUtil.formatNumber(rillikBet);
    }

    public String getRillikBalanceFormatted() {
        return ColorUtil.formatNumber(rillikBalance);
    }
}

