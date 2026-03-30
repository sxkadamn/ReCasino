package net.recasino.model;

public final class MinerSession {

    private final CurrencyType currencyType;
    private final double stake;
    private final boolean[] mines;
    private final boolean[] revealed;
    private int safeRevealed;
    private boolean lost;

    public MinerSession(CurrencyType currencyType, double stake, boolean[] mines) {
        this.currencyType = currencyType;
        this.stake = stake;
        this.mines = mines;
        this.revealed = new boolean[mines.length];
        this.safeRevealed = 0;
        this.lost = false;
    }

    public CurrencyType getCurrencyType() {
        return currencyType;
    }

    public double getStake() {
        return stake;
    }

    public boolean isMine(int index) {
        return mines[index];
    }

    public boolean isRevealed(int index) {
        return revealed[index];
    }

    public boolean reveal(int index) {
        if (revealed[index]) {
            return !mines[index];
        }

        revealed[index] = true;
        if (!mines[index]) {
            safeRevealed++;
            return true;
        }
        return false;
    }

    public int getSafeRevealed() {
        return safeRevealed;
    }

    public boolean isLost() {
        return lost;
    }

    public void markLost() {
        this.lost = true;
    }

    public void revealAllMines() {
        for (int index = 0; index < mines.length; index++) {
            if (mines[index]) {
                revealed[index] = true;
            }
        }
    }

    public int getMineCount() {
        int minesTotal = 0;
        for (boolean mine : mines) {
            if (mine) {
                minesTotal++;
            }
        }
        return minesTotal;
    }

    public int getSafeCellsTotal() {
        return mines.length - getMineCount();
    }
}

