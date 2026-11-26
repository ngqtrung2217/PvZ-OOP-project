package com.pvz.core;

import com.pvz.builder.CustomMapBuilder;
import com.pvz.builder.GameMap;
import com.pvz.model.tile.TileType;
import com.pvz.model.tile.WaterTile;
import com.pvz.state.game.GameOverState;
import com.pvz.state.game.RunResult;
import com.pvz.state.game.GameState;
import com.pvz.state.game.MenuState;
import com.pvz.state.game.PauseState;
import com.pvz.state.game.PlayingState;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for session bookkeeping and State Pattern transitions.
 * <p>
 * These run without a JavaFX toolkit: states are driven through their input
 * handlers, and rendering is never invoked.
 */
class GameManagerTest {

    private GameManager manager;

    @BeforeEach
    void setUp() {
        manager = GameManager.getInstance();
        manager.startNewGame(defaultMap());
    }

    /** An all-grass map with the default starting sun. */
    private static GameMap defaultMap() {
        return new CustomMapBuilder(Set.of()).build();
    }

    @Test
    @DisplayName("A new game uses the map's starting sun and terrain")
    void testStartNewGameLoadsMap() {
        GameMap map = new CustomMapBuilder(Set.of())
                .setStartingSun(175)
                .setTile(1, 3, TileType.WATER)
                .build();

        manager.startNewGame(map);

        assertEquals(175, manager.getSun());
        assertSame(map, manager.getCurrentMap());
        PlayingState playing = assertInstanceOf(PlayingState.class, manager.getCurrentState());
        assertSame(map, playing.getMap());
        assertInstanceOf(WaterTile.class, playing.getGameBoard().getTile(1, 3));
    }

    @Test
    @DisplayName("Retry after game over replays the same map on a fresh board and sun")
    void testRetryReplaysSameMap() {
        GameMap map = new CustomMapBuilder(Set.of()).setStartingSun(120).build();
        manager.startNewGame(map);
        PlayingState first = (PlayingState) manager.getCurrentState();
        manager.addSun(300);

        manager.changeState(new GameOverState(
                new RunResult(map, RunResult.Outcome.LOST, 12.0, 0)));
        manager.getCurrentState().handleKeyPressed(key(KeyCode.ENTER));

        PlayingState retry = assertInstanceOf(PlayingState.class, manager.getCurrentState());
        assertNotSame(first, retry);
        assertNotSame(first.getGameBoard(), retry.getGameBoard());
        assertSame(map, retry.getMap());
        assertEquals(120, manager.getSun());
    }

    @Test
    @DisplayName("A null map is rejected and leaves the session alone")
    void testNullMapRejected() {
        GameState before = manager.getCurrentState();
        assertThrows(IllegalArgumentException.class, () -> manager.startNewGame(null));
        assertSame(before, manager.getCurrentState());
    }

    /** Builds a synthetic key-press event for the given key. */
    private static KeyEvent key(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code,
                false, false, false, false);
    }

    @Test
    @DisplayName("A new session starts with the standard sun balance")
    void testNewSessionStartsWithStandardSun() {
        assertEquals(GameManager.STARTING_SUN, manager.getSun());
        assertInstanceOf(PlayingState.class, manager.getCurrentState());
    }

    @Test
    @DisplayName("Pause then resume must not wipe the player's sun")
    void testPauseResumeKeepsProgress() {
        manager.addSun(275);
        int earned = manager.getSun();
        assertEquals(GameManager.STARTING_SUN + 275, earned);

        // [P] pauses, [P] again resumes the very same PlayingState instance.
        manager.getCurrentState().handleKeyPressed(key(KeyCode.P));
        assertInstanceOf(PauseState.class, manager.getCurrentState());
        assertEquals(earned, manager.getSun(), "Pausing must not change the sun balance");

        manager.getCurrentState().handleKeyPressed(key(KeyCode.P));
        assertInstanceOf(PlayingState.class, manager.getCurrentState());
        assertEquals(earned, manager.getSun(), "Resuming must not wipe collected sun");
    }

    @Test
    @DisplayName("Resuming restores the identical playing instance, board included")
    void testPauseResumeRestoresSameInstance() {
        PlayingState playing = (PlayingState) manager.getCurrentState();
        manager.getCurrentState().handleKeyPressed(key(KeyCode.P));
        manager.getCurrentState().handleKeyPressed(key(KeyCode.P));

        assertSame(playing, manager.getCurrentState(),
                "Resume must re-enter the paused instance instead of building a new one");
    }

    @Test
    @DisplayName("Starting a new game resets the sun balance")
    void testStartNewGameResetsSession() {
        manager.addSun(500);
        assertNotEquals(GameManager.STARTING_SUN, manager.getSun());

        manager.startNewGame(defaultMap());
        assertEquals(GameManager.STARTING_SUN, manager.getSun());
    }

    @Test
    @DisplayName("changeState runs enter() on the incoming state and exit() on the outgoing one")
    void testChangeStateLifecycleOrder() {
        RecordingState first = new RecordingState();
        RecordingState second = new RecordingState();

        manager.changeState(first);
        assertEquals("[enter]", first.log);

        manager.changeState(second);
        assertEquals("[enter][exit]", first.log, "Outgoing state must be exited");
        assertEquals("[enter]", second.log, "Incoming state must be entered");
    }

    @Test
    @DisplayName("Returning to the menu from pause keeps the session untouched")
    void testPauseToMenuKeepsSun() {
        manager.addSun(100);
        int balance = manager.getSun();

        manager.changeState(new PauseState((PlayingState) manager.getCurrentState()));
        manager.getCurrentState().handleKeyPressed(key(KeyCode.ESCAPE));

        assertInstanceOf(MenuState.class, manager.getCurrentState());
        assertEquals(balance, manager.getSun(), "Leaving to the menu keeps the run's sun");
    }

    @Test
    @DisplayName("spendSun deducts only when affordable")
    void testSpendSunAffordability() {
        manager.addSun(50); // 100 total
        assertEquals(100, manager.getSun());

        assertTrue(manager.spendSun(100));
        assertEquals(0, manager.getSun());
        assertFalse(manager.spendSun(1), "Cannot overspend an empty balance");
        assertEquals(0, manager.getSun());

        manager.addSun(75);
        assertFalse(manager.spendSun(100), "Cannot spend more than the balance");
        assertEquals(75, manager.getSun());

        assertFalse(manager.spendSun(-10), "Negative costs are rejected");
        assertEquals(75, manager.getSun());
    }

    /** Minimal GameState that records lifecycle calls, for order assertions. */
    private static final class RecordingState implements GameState {
        private String log = "";

        @Override public void enter() { log += "[enter]"; }
        @Override public void update(double deltaTime) { }
        @Override public void render(GraphicsContext gc) { }
        @Override public void handleMouseClick(MouseEvent event) { }
        @Override public void handleKeyPressed(KeyEvent event) { }
        @Override public void exit() { log += "[exit]"; }
    }
}
