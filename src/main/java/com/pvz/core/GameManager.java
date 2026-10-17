package com.pvz.core;

import com.pvz.state.game.GameState;

/**
 * Singleton class managing global game state transitions and overall session properties.
 */
public class GameManager {
    /** Sun granted at the beginning of every session. */
    public static final int STARTING_SUN = 50;

    private static volatile GameManager instance;

    private GameState currentState;
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
     * Begins a fresh session: refills sun and activates the first state.
     * <p>
     * The only place session data is reset. Call this from the menu or a game-over
     * screen; use {@link #changeState(GameState)} for pure transitions such as
     * pause/resume, which must keep progress.
     */
    public void startNewGame(GameState playingState) {
        this.sun = STARTING_SUN;
        changeState(playingState);
    }
}
