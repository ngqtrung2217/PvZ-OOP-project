package com.pvz.model.economy;

/**
 * Delta-time based cooldown tracker for seed cards.
 * Only advances when updated in PlayingState so pausing freezes cooldowns accurately.
 */
public class CooldownTimer {
    private final double duration;
    private double remaining = 0.0;

    public CooldownTimer(double duration) {
        if (duration < 0) {
            throw new IllegalArgumentException("Cooldown duration must be non-negative");
        }
        this.duration = duration;
    }

    /**
     * Starts or restarts the cooldown period.
     */
    public void start() {
        this.remaining = duration;
    }

    /**
     * Advances the cooldown by deltaTime seconds.
     */
    public void update(double deltaTime) {
        if (deltaTime <= 0 || remaining <= 0) {
            return;
        }
        remaining = Math.max(0.0, remaining - deltaTime);
    }

    public boolean isReady() {
        return remaining <= 0.0;
    }

    public double getRemaining() {
        return remaining;
    }

    public double getDuration() {
        return duration;
    }

    /**
     * Normalized remaining cooldown ratio in [0.0, 1.0].
     * 1.0 means just triggered (full overlay), 0.0 means ready to plant.
     */
    public double getProgress() {
        if (duration <= 0.0) {
            return 0.0;
        }
        return remaining / duration;
    }

    public void reset() {
        this.remaining = 0.0;
    }
}
