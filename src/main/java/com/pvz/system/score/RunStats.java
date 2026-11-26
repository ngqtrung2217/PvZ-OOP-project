package com.pvz.system.score;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.pvz.system.event.ZombieDeathEvent;

/**
 * Counters for one game, filled by {@link ScoreTracker} from events and by the playing
 * state for time, turns and mowers. {@link ScoreCalculator} turns them into a score.
 */
public final class RunStats {
    /** Zombies killed by plants or lawn mowers, by type. */
    private final Map<String, Integer> kills = new HashMap<>();
    /** Zombies that drowned, by type; they score only part of a kill. */
    private final Map<String, Integer> drowned = new HashMap<>();
    private double survivalSeconds;
    private int turnsCleared;
    private int mowersLeft;
    private boolean won;

    public void recordDeath(String zombieType, ZombieDeathEvent.DeathCause cause) {
        Map<String, Integer> target = cause == ZombieDeathEvent.DeathCause.DROWNED ? drowned : kills;
        target.merge(zombieType, 1, Integer::sum);
    }

    /** Adds play time. Only the playing state calls this, so pauses never count. */
    public void addTime(double seconds) {
        if (seconds > 0) {
            survivalSeconds += seconds;
        }
    }

    public void setTurnsCleared(int turnsCleared) {
        this.turnsCleared = Math.max(0, turnsCleared);
    }

    public void setMowersLeft(int mowersLeft) {
        this.mowersLeft = Math.max(0, mowersLeft);
    }

    public void setWon(boolean won) {
        this.won = won;
    }

    /** Read-only view of kills by type, not counting drownings. */
    public Map<String, Integer> getKills() {
        return Collections.unmodifiableMap(kills);
    }

    /** Read-only view of drownings by type. */
    public Map<String, Integer> getDrowned() {
        return Collections.unmodifiableMap(drowned);
    }

    public int getKillCount() {
        return kills.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getDrownedCount() {
        return drowned.values().stream().mapToInt(Integer::intValue).sum();
    }

    public double getSurvivalSeconds() {
        return survivalSeconds;
    }

    public int getTurnsCleared() {
        return turnsCleared;
    }

    public int getMowersLeft() {
        return mowersLeft;
    }

    public boolean isWon() {
        return won;
    }
}
