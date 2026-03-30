package net.recasino.model;

public final class CrashSession {

    public enum State {
        RUNNING,
        CASHED_OUT,
        CRASHED
    }

    private final double stake;
    private final CurrencyType currencyType;
    private final double crashPoint;
    private State state;
    private double currentMultiplier;

    public CrashSession(double stake, CurrencyType currencyType, double crashPoint) {
        this.stake = stake;
        this.currencyType = currencyType;
        this.crashPoint = crashPoint;
        this.state = State.RUNNING;
        this.currentMultiplier = 1.0D;
    }

    public boolean tick(double growthPerTick) {
        if (state != State.RUNNING) {
            return true;
        }

        currentMultiplier += growthPerTick;
        if (currentMultiplier >= crashPoint) {
            currentMultiplier = crashPoint;
            state = State.CRASHED;
            return true;
        }
        return false;
    }

    public double getStake() {
        return stake;
    }

    public CurrencyType getCurrencyType() {
        return currencyType;
    }

    public double getCrashPoint() {
        return crashPoint;
    }

    public State getState() {
        return state;
    }

    public double getCurrentMultiplier() {
        return currentMultiplier;
    }

    public void cashOut() {
        if (state == State.RUNNING) {
            state = State.CASHED_OUT;
        }
    }
}

