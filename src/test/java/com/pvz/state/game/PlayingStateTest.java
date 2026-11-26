package com.pvz.state.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.pvz.builder.CustomMapBuilder;
import com.pvz.builder.GameMap;
import com.pvz.builder.SpawnConfig;
import com.pvz.builder.WaveConfig;
import com.pvz.core.GameManager;
import com.pvz.io.RecordRepository;
import com.pvz.model.zombie.Zombie;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Unit tests for PlayingState: survival time and the two ways a game ends.
 * Runs without a JavaFX toolkit; render is never called.
 */
class PlayingStateTest {
    private static final double STEP = 0.1;
    /** Long enough for the slowest zombie to cross the whole lawn. */
    private static final int MAX_STEPS = 2000;

    private GameManager manager;

    /** End screens save records here instead of ~/.pvz. */
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        manager = GameManager.getInstance();
        manager.setRecordRepository(new RecordRepository(tempDir.resolve("records.json")));
        manager.startNewGame(new CustomMapBuilder(Set.of()).build());
    }

    private static GameMap campaignOf(WaveConfig... waves) {
        CustomMapBuilder builder = new CustomMapBuilder(Set.of());
        builder.setName("Test");
        for (WaveConfig wave : waves) {
            builder.addWave(wave);
        }
        return builder.build();
    }

    private static WaveConfig oneZombie(int lane, double delay) {
        return new WaveConfig(List.of(new SpawnConfig("NORMAL", lane, delay)), false);
    }

    private static KeyEvent key(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false);
    }

    /** Updates the current state until it is no longer the given playing state. */
    private void runUntilLeaves(PlayingState playing) {
        for (int i = 0; i < MAX_STEPS && manager.getCurrentState() == playing; i++) {
            playing.update(STEP);
        }
    }

    private static WaveConfig twoZombies(int lane, double firstDelay, double secondDelay) {
        return new WaveConfig(List.of(new SpawnConfig("NORMAL", lane, firstDelay),
                new SpawnConfig("NORMAL", lane, secondDelay)), false);
    }

    @Test
    @DisplayName("The lawn mower stops the first zombie; the next one in that lane loses")
    void testZombieReachingHouseLoses() {
        // The first zombie reaches the mower after about 42 s and the mower sweeps the
        // whole lane, so the second must arrive after the mower has gone
        manager.startNewGame(campaignOf(twoZombies(2, 0.0, 50.0)));
        PlayingState playing = (PlayingState) manager.getCurrentState();

        runUntilLeaves(playing);

        GameOverState over = assertInstanceOf(GameOverState.class, manager.getCurrentState());
        RunResult result = over.getResult();
        assertEquals(RunResult.Outcome.LOST, result.outcome());
        assertSame(playing.getMap(), result.map());
        assertEquals(0, result.wavesCleared());
        assertTrue(result.survivalSeconds() > 30, "Crossing the lawn takes tens of seconds");
        assertEquals(1, result.kills(), "The mower's kill is counted");
        assertEquals(4, result.mowersLeft());
        // 10 points for the mowed Normal + 1 per second, ×3 for 50 sun (capped at ×2)
        int expected = (int) Math.round((10 + Math.floor(result.survivalSeconds())) * 2.0);
        assertEquals(expected, result.score());
    }

    @Test
    @DisplayName("A lone zombie is mowed down, so the level is won with one mower used")
    void testMowerSavesTheLane() {
        manager.startNewGame(campaignOf(oneZombie(3, 0.0)));
        PlayingState playing = (PlayingState) manager.getCurrentState();

        runUntilLeaves(playing);

        VictoryState victory = assertInstanceOf(VictoryState.class, manager.getCurrentState());
        assertEquals(1, victory.getResult().kills());
        assertEquals(4, victory.getResult().mowersLeft());
        assertTrue(playing.getMowers().get(3).isUsed() || playing.getMowers().get(3).isActive());
    }

    @Test
    @DisplayName("Kills are not counted after the game has ended")
    void testTrackerStopsAfterFinish() {
        manager.startNewGame(campaignOf(oneZombie(3, 0.0)));
        PlayingState playing = (PlayingState) manager.getCurrentState();
        runUntilLeaves(playing);
        int kills = playing.getStats().getKillCount();

        com.pvz.system.event.CombatEventBus.getInstance().publish(
                new com.pvz.system.event.ZombieDeathEvent(null));

        assertEquals(kills, playing.getStats().getKillCount());
    }

    @Test
    @DisplayName("Clearing every wave of a campaign level wins it")
    void testClearingAllWavesWins() {
        manager.startNewGame(campaignOf(oneZombie(0, 0.0), oneZombie(4, 0.5)));
        PlayingState playing = (PlayingState) manager.getCurrentState();

        // Stand in for plants: kill each zombie as soon as it appears
        for (int i = 0; i < MAX_STEPS && manager.getCurrentState() == playing; i++) {
            playing.update(STEP);
            for (Zombie zombie : playing.getWaveManager().getActiveZombies()) {
                if (zombie.isAlive()) {
                    zombie.takeDamage(10_000);
                }
            }
        }

        VictoryState victory = assertInstanceOf(VictoryState.class, manager.getCurrentState());
        assertEquals(RunResult.Outcome.WON, victory.getResult().outcome());
        assertEquals(2, victory.getResult().wavesCleared());
    }

    @Test
    @DisplayName("A custom map without waves is not won instantly")
    void testCustomMapIsNotWonInstantly() {
        manager.startNewGame(new CustomMapBuilder(Set.of()).setMaxTurns(3).build());
        PlayingState playing = (PlayingState) manager.getCurrentState();

        for (int i = 0; i < 50; i++) {
            playing.update(STEP);
        }

        assertSame(playing, manager.getCurrentState());
    }

    @Test
    @DisplayName("Survival time adds up while playing and stops while paused")
    void testSurvivalTimeSkipsPause() {
        PlayingState playing = (PlayingState) manager.getCurrentState();
        for (int i = 0; i < 20; i++) {
            playing.update(STEP);
        }
        double beforePause = playing.getSurvivalTime();
        assertEquals(2.0, beforePause, 1e-9);

        playing.handleKeyPressed(key(KeyCode.P));
        GameState pause = manager.getCurrentState();
        for (int i = 0; i < 50; i++) {
            pause.update(STEP);
        }
        pause.handleKeyPressed(key(KeyCode.P));

        assertSame(playing, manager.getCurrentState());
        assertEquals(beforePause, playing.getSurvivalTime(), 1e-9);
    }

    @Test
    @DisplayName("Updates after the game has ended do nothing")
    void testNoUpdatesAfterFinish() {
        manager.startNewGame(campaignOf(oneZombie(1, 0.0)));
        PlayingState playing = (PlayingState) manager.getCurrentState();
        runUntilLeaves(playing);
        GameState end = manager.getCurrentState();
        double time = playing.getSurvivalTime();

        playing.update(STEP);

        assertSame(end, manager.getCurrentState());
        assertEquals(time, playing.getSurvivalTime(), 1e-9);
    }

    @Test
    @DisplayName("Time is shown as m:ss")
    void testFormatTime() {
        assertEquals("0:00", PlayingState.formatTime(0));
        assertEquals("0:59", PlayingState.formatTime(59.9));
        assertEquals("2:05", PlayingState.formatTime(125));
    }
}
