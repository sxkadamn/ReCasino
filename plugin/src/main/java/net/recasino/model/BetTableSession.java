package net.recasino.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class BetTableSession {

    public enum State {
        WAITING,
        COUNTDOWN,
        RESULT
    }

    private final BetTableDefinition definition;
    private final Map<UUID, Double> participants;
    private State state;
    private long remainingSeconds;
    private String lastWinnerName;
    private double lastPayout;

    public BetTableSession(BetTableDefinition definition) {
        this.definition = definition;
        this.participants = new LinkedHashMap<UUID, Double>();
        this.state = State.WAITING;
        this.remainingSeconds = 0L;
        this.lastWinnerName = "Никто";
        this.lastPayout = 0.0D;
    }

    public BetTableDefinition getDefinition() {
        return definition;
    }

    public Map<UUID, Double> getParticipants() {
        return Collections.unmodifiableMap(participants);
    }

    public boolean hasParticipant(UUID uniqueId) {
        return participants.containsKey(uniqueId);
    }

    public Double getStake(UUID uniqueId) {
        return participants.get(uniqueId);
    }

    public void addParticipant(UUID uniqueId, double stake) {
        participants.put(uniqueId, stake);
    }

    public Double removeParticipant(UUID uniqueId) {
        return participants.remove(uniqueId);
    }

    public int getParticipantCount() {
        return participants.size();
    }

    public double getTotalPot() {
        double total = 0.0D;
        for (double value : participants.values()) {
            total += value;
        }
        return total;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }

    public void startCountdown(long remainingSeconds) {
        this.state = State.COUNTDOWN;
        this.remainingSeconds = remainingSeconds;
    }

    public void tickCountdown() {
        if (remainingSeconds > 0L) {
            remainingSeconds--;
        }
    }

    public void resetWaiting() {
        this.state = State.WAITING;
        this.remainingSeconds = 0L;
    }

    public void showResult(String winnerName, double payout, long resultSeconds) {
        this.state = State.RESULT;
        this.lastWinnerName = winnerName;
        this.lastPayout = payout;
        this.remainingSeconds = resultSeconds;
    }

    public void clearAfterResult() {
        participants.clear();
        resetWaiting();
    }

    public String getLastWinnerName() {
        return lastWinnerName;
    }

    public double getLastPayout() {
        return lastPayout;
    }
}
