package net.recasino.model;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class JackpotSession {

    public enum State {
        WAITING,
        COUNTDOWN,
        ANIMATING,
        RESULT
    }

    private final Map<UUID, Double> participants;
    private long endAtMillis;
    private String lastWinnerName;
    private double lastPayout;
    private State state;
    private UUID highlightedParticipantId;
    private UUID winnerId;
    private double winnerStake;
    private long resultUntilMillis;
    private List<UUID> animationDisplayIds;

    public JackpotSession() {
        this.participants = new LinkedHashMap<UUID, Double>();
        this.endAtMillis = 0L;
        this.lastWinnerName = "-";
        this.lastPayout = 0.0D;
        this.state = State.WAITING;
        this.highlightedParticipantId = null;
        this.winnerId = null;
        this.winnerStake = 0.0D;
        this.resultUntilMillis = 0L;
        this.animationDisplayIds = new ArrayList<UUID>();
    }

    public Map<UUID, Double> getParticipants() {
        return participants;
    }

    public boolean hasParticipant(UUID uniqueId) {
        return participants.containsKey(uniqueId);
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

    public long getEndAtMillis() {
        return endAtMillis;
    }

    public void startCountdown(long durationMillis) {
        this.endAtMillis = System.currentTimeMillis() + durationMillis;
        this.state = State.COUNTDOWN;
    }

    public void resetCountdown() {
        this.endAtMillis = 0L;
        if (participants.isEmpty()) {
            this.state = State.WAITING;
        }
    }

    public boolean hasCountdown() {
        return endAtMillis > 0L;
    }

    public long getRemainingSeconds() {
        if (endAtMillis <= 0L) {
            return 0L;
        }
        return Math.max(0L, (endAtMillis - System.currentTimeMillis()) / 1000L);
    }

    public String getLastWinnerName() {
        return lastWinnerName;
    }

    public double getLastPayout() {
        return lastPayout;
    }

    public void setLastResult(String lastWinnerName, double lastPayout) {
        this.lastWinnerName = lastWinnerName;
        this.lastPayout = lastPayout;
    }

    public State getState() {
        return state;
    }

    public boolean isAnimating() {
        return state == State.ANIMATING;
    }

    public boolean isShowingResult() {
        return state == State.RESULT;
    }

    public UUID getHighlightedParticipantId() {
        return highlightedParticipantId;
    }

    public void setHighlightedParticipantId(UUID highlightedParticipantId) {
        this.highlightedParticipantId = highlightedParticipantId;
    }

    public UUID getWinnerId() {
        return winnerId;
    }

    public void beginAnimation(UUID winnerId, double winnerStake) {
        this.state = State.ANIMATING;
        this.endAtMillis = 0L;
        this.winnerId = winnerId;
        this.winnerStake = winnerStake;
        this.resultUntilMillis = 0L;
    }

    public void showResult(long durationMillis) {
        this.state = State.RESULT;
        this.highlightedParticipantId = winnerId;
        this.resultUntilMillis = System.currentTimeMillis() + durationMillis;
    }

    public double getWinnerStake() {
        return winnerStake;
    }

    public long getResultUntilMillis() {
        return resultUntilMillis;
    }

    public long getResultRemainingSeconds() {
        if (resultUntilMillis <= 0L) {
            return 0L;
        }
        long millisLeft = Math.max(0L, resultUntilMillis - System.currentTimeMillis());
        return (millisLeft + 999L) / 1000L;
    }

    public List<UUID> getAnimationDisplayIds() {
        return Collections.unmodifiableList(animationDisplayIds);
    }

    public void setAnimationDisplayIds(List<UUID> animationDisplayIds) {
        this.animationDisplayIds = new ArrayList<UUID>(animationDisplayIds);
    }

    public void clearParticipants() {
        participants.clear();
        resetCountdown();
        state = State.WAITING;
        highlightedParticipantId = null;
        winnerId = null;
        winnerStake = 0.0D;
        resultUntilMillis = 0L;
        animationDisplayIds.clear();
    }
}

