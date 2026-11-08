package com.frostsmp.parkour.model;

public final class PlayerStats {

    private long bestTimeMs;
    private int completions;
    private int attempts;

    public PlayerStats() {
        this.bestTimeMs = -1L;
        this.completions = 0;
        this.attempts = 0;
    }

    public PlayerStats(long bestTimeMs, int completions, int attempts) {
        this.bestTimeMs = bestTimeMs;
        this.completions = Math.max(0, completions);
        this.attempts = Math.max(0, attempts);
    }

    public long getBestTimeMs() {
        return bestTimeMs;
    }

    public void setBestTimeMs(long bestTimeMs) {
        this.bestTimeMs = bestTimeMs;
    }

    public int getCompletions() {
        return completions;
    }

    public void setCompletions(int completions) {
        this.completions = Math.max(0, completions);
    }

    public void incrementCompletions() {
        this.completions++;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = Math.max(0, attempts);
    }

    public void incrementAttempts() {
        this.attempts++;
    }

    public void mergeFrom(PlayerStats other) {
        if (other == null) {
            return;
        }
        this.completions = Math.max(this.completions, other.completions);
        this.attempts = Math.max(this.attempts, other.attempts);
        if (other.bestTimeMs > 0 && (this.bestTimeMs <= 0 || other.bestTimeMs < this.bestTimeMs)) {
            this.bestTimeMs = other.bestTimeMs;
        }
    }
}
