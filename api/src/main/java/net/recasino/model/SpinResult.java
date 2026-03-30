package net.recasino.model;

public final class SpinResult {

    private final boolean accepted;
    private final String message;
    private final GameMode gameMode;
    private final CurrencyType currencyType;
    private final double stake;
    private final Prize prize;

    private SpinResult(boolean accepted, String message, GameMode gameMode, CurrencyType currencyType, double stake, Prize prize) {
        this.accepted = accepted;
        this.message = message;
        this.gameMode = gameMode;
        this.currencyType = currencyType;
        this.stake = stake;
        this.prize = prize;
    }

    public static SpinResult rejected(String message) {
        return new SpinResult(false, message, null, null, 0.0D, null);
    }

    public static SpinResult accepted(GameMode gameMode, CurrencyType currencyType, double stake, Prize prize) {
        return new SpinResult(true, "", gameMode, currencyType, stake, prize);
    }

    public boolean isAccepted() {
        return accepted;
    }

    public String getMessage() {
        return message;
    }

    public GameMode getGameMode() {
        return gameMode;
    }

    public CurrencyType getCurrencyType() {
        return currencyType;
    }

    public double getStake() {
        return stake;
    }

    public Prize getPrize() {
        return prize;
    }
}
