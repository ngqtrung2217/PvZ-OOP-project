package com.pvz.system.score;

import java.util.Map;

/**
 * Score formula from README 3.7:
 * <pre>
 * Score = (kill points + time points + turn bonus + win bonus) × sun multiplier
 * </pre>
 * Every constant comes from {@link ScoreSpec}.
 */
public final class ScoreCalculator {
    private final ScoreSpec spec;

    public ScoreCalculator(ScoreSpec spec) {
        if (spec == null) {
            throw new IllegalArgumentException("Spec cannot be null");
        }
        this.spec = spec;
    }

    /** A calculator using the game's score_spec.json. */
    public static ScoreCalculator createDefault() {
        return new ScoreCalculator(ScoreSpec.load());
    }

    /**
     * @param startingSun  the map's starting sun, for the multiplier
     * @param hasTurnLimit false for an endless custom map, which can never be won
     * @return the final score, rounded to a whole number
     */
    public int score(RunStats stats, int startingSun, boolean hasTurnLimit) {
        double total = killPoints(stats)
                + timePoints(stats)
                + turnBonus(stats)
                + winBonus(stats, hasTurnLimit);
        return (int) Math.round(total * sunMultiplier(startingSun));
    }

    /** Full points per kill; a drowned zombie scores only drownedKillFactor of that. */
    public double killPoints(RunStats stats) {
        double points = 0;
        for (Map.Entry<String, Integer> kill : stats.getKills().entrySet()) {
            points += spec.killPoints(kill.getKey()) * kill.getValue();
        }
        for (Map.Entry<String, Integer> drown : stats.getDrowned().entrySet()) {
            points += spec.killPoints(drown.getKey()) * drown.getValue() * spec.getDrownedKillFactor();
        }
        return points;
    }

    /** One point per whole second survived (by default). */
    public double timePoints(RunStats stats) {
        return Math.floor(stats.getSurvivalSeconds()) * spec.getPointsPerSecond();
    }

    public double turnBonus(RunStats stats) {
        return (double) spec.getTurnBonus() * stats.getTurnsCleared();
    }

    /** Only a won game with a turn limit earns it, plus a bonus per unused lawn mower. */
    public double winBonus(RunStats stats, boolean hasTurnLimit) {
        if (!hasTurnLimit || !stats.isWon()) {
            return 0;
        }
        return spec.getWinBonus() + (double) spec.getWinBonusPerMower() * stats.getMowersLeft();
    }

    /**
     * clamp(base / sun, min, max): less starting sun scores more. With 0 sun the division
     * gives Infinity, which the clamp turns into the maximum.
     */
    public double sunMultiplier(int startingSun) {
        double factor = spec.getSunFactorBase() / startingSun;
        return Math.max(spec.getSunFactorMin(), Math.min(spec.getSunFactorMax(), factor));
    }
}
