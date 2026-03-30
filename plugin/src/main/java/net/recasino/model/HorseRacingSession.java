package net.recasino.model;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class HorseRacingSession {

    public enum State {
        RUNNING,
        REWARD,
        TAKEN_REWARD
    }

    public enum Horse {
        RED("Огненный", Material.RED_WOOL, Material.RED_STAINED_GLASS_PANE),
        BLUE("Шторм", Material.BLUE_WOOL, Material.BLUE_STAINED_GLASS_PANE),
        GREEN("Изумруд", Material.LIME_WOOL, Material.LIME_STAINED_GLASS_PANE),
        YELLOW("Солнце", Material.YELLOW_WOOL, Material.YELLOW_STAINED_GLASS_PANE);

        private final String displayName;
        private final Material horseMaterial;
        private final Material pathMaterial;

        Horse(String displayName, Material horseMaterial, Material pathMaterial) {
            this.displayName = displayName;
            this.horseMaterial = horseMaterial;
            this.pathMaterial = pathMaterial;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getHorseMaterial() {
            return horseMaterial;
        }

        public Material getPathMaterial() {
            return pathMaterial;
        }
    }

    public static final class Racer {
        private final Horse horse;
        private int row;
        private int position;
        private boolean finished;
        private int place;

        private Racer(Horse horse) {
            this.horse = horse;
            this.position = 0;
            this.finished = false;
            this.place = -1;
        }

        public Horse getHorse() {
            return horse;
        }

        public int getRow() {
            return row;
        }

        public int getPosition() {
            return position;
        }

        public boolean isFinished() {
            return finished;
        }

        public int getPlace() {
            return place;
        }
    }

    private final double stake;
    private final Horse selectedHorse;
    private final List<Racer> racers;
    private final Racer playerRacer;
    private State state;
    private int finishCounter;
    private double rewardMultiplier;
    private double rewardAmount;

    public HorseRacingSession(double stake, Horse selectedHorse) {
        this.stake = stake;
        this.selectedHorse = selectedHorse;
        this.racers = new ArrayList<Racer>();

        Racer playerEntry = new Racer(selectedHorse);
        this.racers.add(playerEntry);
        for (Horse horse : Horse.values()) {
            if (horse != selectedHorse) {
                this.racers.add(new Racer(horse));
            }
        }

        Collections.shuffle(this.racers, ThreadLocalRandom.current());
        for (int index = 0; index < this.racers.size(); index++) {
            this.racers.get(index).row = index;
        }

        this.playerRacer = playerEntry;
        this.state = State.RUNNING;
        this.finishCounter = 0;
        this.rewardMultiplier = 0.0D;
        this.rewardAmount = 0.0D;
    }

    public boolean tick(double chanceToMove) {
        if (state != State.RUNNING) {
            return false;
        }

        boolean anyRunning = false;
        for (Racer racer : racers) {
            if (racer.finished) {
                continue;
            }

            anyRunning = true;
            if (ThreadLocalRandom.current().nextDouble() <= chanceToMove) {
                racer.position++;
            }

            if (racer.position >= 8) {
                racer.position = 8;
                racer.finished = true;
                racer.place = finishCounter++;
            }
        }

        if (!anyRunning || finishCounter >= racers.size()) {
            state = State.REWARD;
            return true;
        }

        return false;
    }

    public double getStake() {
        return stake;
    }

    public Horse getSelectedHorse() {
        return selectedHorse;
    }

    public List<Racer> getRacers() {
        return racers;
    }

    public Racer getPlayerRacer() {
        return playerRacer;
    }

    public State getState() {
        return state;
    }

    public int getPlayerPlace() {
        return playerRacer.place;
    }

    public void setReward(double multiplier, double amount) {
        this.rewardMultiplier = multiplier;
        this.rewardAmount = amount;
    }

    public double getRewardMultiplier() {
        return rewardMultiplier;
    }

    public double getRewardAmount() {
        return rewardAmount;
    }

    public void markRewardTaken() {
        this.state = State.TAKEN_REWARD;
    }
}

