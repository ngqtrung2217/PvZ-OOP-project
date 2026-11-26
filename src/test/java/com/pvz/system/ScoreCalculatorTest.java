package com.pvz.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.pvz.system.event.ZombieDeathEvent.DeathCause;
import com.pvz.system.score.RunStats;
import com.pvz.system.score.ScoreCalculator;
import com.pvz.system.score.ScoreSpec;

/**
 * Unit tests for the score formula of README 3.7, using the real score_spec.json.
 */
class ScoreCalculatorTest {
    private ScoreCalculator calculator;
    private RunStats stats;

    @BeforeEach
    void setUp() {
        calculator = new ScoreCalculator(ScoreSpec.load());
        stats = new RunStats();
    }

    @Test
    @DisplayName("Kill points follow the README table")
    void testKillPoints() {
        ScoreSpec spec = ScoreSpec.load();
        assertEquals(10, spec.killPoints("NORMAL"));
        assertEquals(15, spec.killPoints("WATER"));
        assertEquals(20, spec.killPoints("CONEHEAD"));
        assertEquals(25, spec.killPoints("POLE_VAULTING"));
        assertEquals(40, spec.killPoints("BUCKETHEAD"));
        assertEquals(0, spec.killPoints("DRAGON"));
        assertSame(spec, ScoreSpec.load(), "The spec is read once");
    }

    @Test
    @DisplayName("A drowned zombie scores half; a mowed one scores in full")
    void testDrownedHalf() {
        stats.recordDeath("BUCKETHEAD", DeathCause.KILLED);
        stats.recordDeath("CONEHEAD", DeathCause.MOWED);
        stats.recordDeath("CONEHEAD", DeathCause.DROWNED);

        assertEquals(40 + 20 + 10, calculator.killPoints(stats), 1e-9);
        assertEquals(2, stats.getKillCount());
        assertEquals(1, stats.getDrownedCount());
    }

    @Test
    @DisplayName("Time scores one point per whole second")
    void testTimePoints() {
        stats.addTime(59.9);
        stats.addTime(-5); // ignored
        assertEquals(59, calculator.timePoints(stats), 1e-9);
    }

    @Test
    @DisplayName("Sun multiplier: 50 sun ×2, 150 sun ×1, 300 and more ×0.5, 0 sun ×2")
    void testSunMultiplier() {
        assertEquals(2.0, calculator.sunMultiplier(50), 1e-9);
        assertEquals(1.0, calculator.sunMultiplier(150), 1e-9);
        assertEquals(0.75, calculator.sunMultiplier(200), 1e-9);
        assertEquals(0.5, calculator.sunMultiplier(300), 1e-9);
        assertEquals(0.5, calculator.sunMultiplier(1000), 1e-9);
        assertEquals(2.0, calculator.sunMultiplier(0), 1e-9);
    }

    @Test
    @DisplayName("The win bonus needs a win and a turn limit, plus 100 per mower left")
    void testWinBonus() {
        stats.setMowersLeft(3);
        assertEquals(0, calculator.winBonus(stats, true), 1e-9);

        stats.setWon(true);
        assertEquals(500 + 300, calculator.winBonus(stats, true), 1e-9);
        assertEquals(0, calculator.winBonus(stats, false), "Endless maps cannot be won");
    }

    @Test
    @DisplayName("The full formula adds every part, then applies the sun multiplier")
    void testFullScore() {
        stats.recordDeath("NORMAL", DeathCause.KILLED);   // 10
        stats.recordDeath("BUCKETHEAD", DeathCause.KILLED); // 40
        stats.addTime(120.5);                              // 120
        stats.setTurnsCleared(3);                          // 300
        stats.setMowersLeft(2);
        stats.setWon(true);                                // 500 + 200

        assertEquals(1170, calculator.score(stats, 150, true));
        assertEquals(2340, calculator.score(stats, 50, true));
        assertEquals(470 / 2, calculator.score(stats, 300, false), "No win bonus when endless");
    }
}
