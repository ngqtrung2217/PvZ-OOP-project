package com.pvz.core;

import com.pvz.builder.GameMap;
import com.pvz.io.RecordRepository;
import com.pvz.state.game.GameState;
import com.pvz.state.game.PlayingState;

/**
 * Singleton class managing global game state transitions and overall session properties.
 */
public class GameManager {
    /** Default starting sun for a new map; each map sets its own. */
    public static final int STARTING_SUN = 50;

    private static volatile GameManager instance;

    private GameState currentState;
    private GameMap currentMap;
    private RecordRepository recordRepository;
    private int sun = STARTING_SUN;

    private GameManager() {
    }

    /**
     * Retrieves the singleton instance using double-checked locking.
     */
    public static GameManager getInstance() {
        if (instance == null) {
            synchronized (GameManager.class) {
                if (instance == null) {
                    instance = new GameManager();
                }
            }
        }
        return instance;
    }

    /**
     * Switches the active game state to a new state.
     * Triggers exit() on the previous state and enter() on the new one.
     * <p>
     * Never resets session data: PauseState resumes by re-entering the very same
     * PlayingState instance, so a reset here would wipe the player's progress.
     */
    public void changeState(GameState newState) {
        if (currentState != null) {
            currentState.exit();
        }
        this.currentState = newState;
        if (this.currentState != null) {
            this.currentState.enter();
        }
    }

    public GameState getCurrentState() {
        return currentState;
    }

    public int getSun() {
        return sun;
    }

    public void addSun(int amount) {
        if (amount > 0) {
            this.sun += amount;
        }
    }

    /**
     * Deducts sun when the player can afford the cost.
     * @param amount Cost to pay; ignored when negative.
     * @return true if the amount was deducted, false if the balance was insufficient.
     */
    public boolean spendSun(int amount) {
        if (amount < 0 || amount > sun) {
            return false;
        }
        this.sun -= amount;
        return true;
    }

    /**
     * Begins a fresh session on a map: sets the map's starting sun and plays it on a new
     * board.
     * <p>
     * The only place session data is reset. Call this from the menu or a game-over
     * screen; use {@link #changeState(GameState)} for pure transitions such as
     * pause/resume, which must keep progress.
     */
    public void startNewGame(GameMap map) {
        startSession(map, null);
    }

    /**
     * Like {@link #startNewGame(GameMap)}, but for the editor's play-test: no record is
     * kept, and leaving the game or its end screen returns to {@code returnTo}.
     */
    public void startPracticeGame(GameMap map, GameState returnTo) {
        if (returnTo == null) {
            throw new IllegalArgumentException("A practice game needs a screen to return to");
        }
        startSession(map, returnTo);
    }

    private void startSession(GameMap map, GameState practiceReturn) {
        if (map == null) {
            throw new IllegalArgumentException("Map cannot be null");
        }
        this.currentMap = map;
        this.sun = map.getStartingSun();
        changeState(new PlayingState(map, practiceReturn));
    }

    /**
     * @return The map of the current session, so a game-over screen can retry it; null
     *         before the first game.
     */
    public GameMap getCurrentMap() {
        return currentMap;
    }

    /** Where end screens save records; ~/.pvz/records.json unless replaced. */
    public RecordRepository getRecordRepository() {
        if (recordRepository == null) {
            recordRepository = RecordRepository.inUserHome();
        }
        return recordRepository;
    }

    /** Replaces the record store, so tests write to a temporary file instead of ~/.pvz. */
    public void setRecordRepository(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }
}
