package com.pvz.state.game;

import com.pvz.core.GameManager;
import com.pvz.view.ResultView;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/**
 * State shown when zombies breach the lawn and enter the player's house.
 */
public class GameOverState implements GameState {
    private final RunResult result;
    /** The editor to go back to after a play-test; null in a normal game. */
    private final GameState practiceReturn;
    private final ResultView view = new ResultView();
    /** Filled once on enter(); the record is saved only then. */
    private RecordSubmission records;

    public GameOverState(RunResult result) {
        this(result, null);
    }

    public GameOverState(RunResult result, GameState practiceReturn) {
        if (result == null) {
            throw new IllegalArgumentException("Result cannot be null");
        }
        this.result = result;
        this.practiceReturn = practiceReturn;
    }

    @Override
    public void enter() {
        // Play game over music
        if (records == null) {
            records = RecordSubmission.submit(result,
                    GameManager.getInstance().getRecordRepository());
        }
    }

    @Override
    public void update(double deltaTime) {}

    @Override
    public void render(GraphicsContext gc) {
        view.render(gc, result, records, "[ENTER] Chơi lại",
                practiceReturn != null ? "[ESC] Về editor" : "[ESC] Về menu");
    }

    @Override
    public void handleMouseClick(MouseEvent event) {
        retry();
    }

    @Override
    public void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            retry();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            GameManager.getInstance().changeState(
                    practiceReturn != null ? practiceReturn : new MenuState());
        }
    }

    /** Plays the lost map again on a fresh board with its starting sun. */
    private void retry() {
        if (practiceReturn != null) {
            GameManager.getInstance().startPracticeGame(result.map(), practiceReturn);
        } else {
            GameManager.getInstance().startNewGame(result.map());
        }
    }

    @Override
    public void exit() {}

    public RunResult getResult() {
        return result;
    }

    /** Rank and top table, or null before the state has been entered. */
    public RecordSubmission getRecords() {
        return records;
    }
}
