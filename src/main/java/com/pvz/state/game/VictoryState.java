package com.pvz.state.game;

import com.pvz.core.GameManager;
import com.pvz.io.LevelDataLoader;
import com.pvz.view.LevelSelectView;
import com.pvz.view.ResultView;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

/**
 * State displayed when player defeats all waves and wins the level.
 */
public class VictoryState implements GameState {
    private final RunResult result;
    /** The editor to go back to after a play-test; null in a normal game. */
    private final GameState practiceReturn;
    private final ResultView view = new ResultView();
    /** Filled once on enter(); the record is saved only then. */
    private RecordSubmission records;

    public VictoryState(RunResult result) {
        this(result, null);
    }

    public VictoryState(RunResult result, GameState practiceReturn) {
        if (result == null) {
            throw new IllegalArgumentException("Result cannot be null");
        }
        this.result = result;
        this.practiceReturn = practiceReturn;
    }

    @Override
    public void enter() {
        // Play victory jingle
        if (records == null) {
            records = RecordSubmission.submit(result,
                    GameManager.getInstance().getRecordRepository());
        }
    }

    @Override
    public void update(double deltaTime) {}

    @Override
    public void render(GraphicsContext gc) {
        if (practiceReturn != null) {
            view.render(gc, result, records, "[ENTER] / [ESC] Về editor");
        } else {
            view.render(gc, result, records, "[ENTER] Chọn màn khác", "[ESC] Về menu");
        }
    }

    @Override
    public void handleMouseClick(MouseEvent event) {
        next();
    }

    @Override
    public void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            next();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            GameManager.getInstance().changeState(
                    practiceReturn != null ? practiceReturn : new MenuState());
        }
    }

    /** Back to the editor after a play-test, otherwise on to the level list. */
    private void next() {
        GameManager.getInstance().changeState(practiceReturn != null
                ? practiceReturn
                : new LevelSelectView(LevelDataLoader.createDefault()));
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
