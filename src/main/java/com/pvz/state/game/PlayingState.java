package com.pvz.state.game;

import com.pvz.builder.GameMap;
import com.pvz.builder.MapMode;
import com.pvz.core.GameManager;
import com.pvz.model.board.GameBoard;
import com.pvz.model.wave.WaveManager;
import com.pvz.model.zombie.Zombie;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * State active during active combat / lawn defense gameplay.
 * <p>
 * Counts survival time, runs the waves and checks how the game ends (README 3.6):
 * a living zombie reaching the house loses; a campaign level is won once every wave has
 * spawned and no zombie is left.
 */
public class PlayingState implements GameState {
    /** A living zombie whose left edge passes this x has reached the house. */
    public static final double HOUSE_X = GameBoard.START_X - GameBoard.CELL_WIDTH / 2.0;

    private final GameMap map;
    /** The editor to go back to after a play-test; null in a normal game. */
    private final GameState practiceReturn;
    private final GameBoard gameBoard;
    private final WaveManager waveManager;
    private double survivalTime;
    private boolean finished;
    private int selectedRow = -1;
    private int selectedCol = -1;

    /**
     * Use {@link GameManager#startNewGame(GameMap)} rather than creating this directly, so
     * the session's sun is set from the same map.
     */
    public PlayingState(GameMap map) {
        this(map, null);
    }

    /**
     * @param practiceReturn screen to return to after an editor play-test, or null for a
     *                       normal game
     */
    public PlayingState(GameMap map, GameState practiceReturn) {
        if (map == null) {
            throw new IllegalArgumentException("Map cannot be null");
        }
        this.map = map;
        this.practiceReturn = practiceReturn;
        this.gameBoard = map.createBoard();
        // TODO(Ngoc Minh): custom maps have no waves; they need EscalatingRandomSpawnPolicy
        this.waveManager = new WaveManager(gameBoard, map.createWaves());
    }

    @Override
    public void enter() {
        // Lifecycle only: the session is reset by GameManager.startNewGame().
        // PauseState resumes by re-entering this same instance, so resetting here
        // would silently wipe the player's sun on every pause/resume cycle.
    }

    @Override
    public void update(double deltaTime) {
        if (finished) {
            return;
        }
        // Only counted here, so time spent paused never adds up (README rule 6)
        survivalTime += deltaTime;
        gameBoard.update(deltaTime);
        waveManager.update(deltaTime);
        // TODO(An Nguyen, Tuan Minh): update plants, projectiles and collisions here

        if (zombieReachedHouse()) {
            finish(RunResult.Outcome.LOST);
        } else if (isCampaignWon()) {
            finish(RunResult.Outcome.WON);
        }
    }

    private boolean zombieReachedHouse() {
        for (Zombie zombie : waveManager.getActiveZombies()) {
            if (zombie.isAlive() && zombie.getX() < HOUSE_X) {
                return true;
            }
        }
        return false;
    }

    /**
     * A custom map has no scripted waves, so an empty wave list must not count as won.
     * TODO(Ngoc Minh): win a custom map with maxTurns once its spawn policy reports the
     * last turn as finished.
     */
    private boolean isCampaignWon() {
        return map.getMode() == MapMode.CAMPAIGN && waveManager.isAllWavesCleared();
    }

    private void finish(RunResult.Outcome outcome) {
        finished = true;
        RunResult result = new RunResult(map, outcome, survivalTime,
                waveManager.getCurrentWaveIndex(), isPractice());
        GameState next = outcome == RunResult.Outcome.WON
                ? new VictoryState(result, practiceReturn)
                : new GameOverState(result, practiceReturn);
        GameManager.getInstance().changeState(next);
    }

    @Override
    public void render(GraphicsContext gc) {
        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();

        // Clear canvas with lawn-surround background
        gc.setFill(Color.web("#1E3A1E"));
        gc.fillRect(0, 0, width, height);

        // Back to front: tiles, then plants (planned), zombies, projectiles (planned), HUD
        gameBoard.render(gc);

        if (selectedRow >= 0 && selectedCol >= 0) {
            double ox = gameBoard.getCellOriginX(selectedCol);
            double oy = gameBoard.getCellOriginY(selectedRow);
            gc.setStroke(Color.web("#FFEB3B"));
            gc.setLineWidth(3.0);
            gc.strokeRect(ox, oy, GameBoard.CELL_WIDTH, GameBoard.CELL_HEIGHT);
        }

        waveManager.render(gc);
        renderHUD(gc);
    }

    private void renderHUD(GraphicsContext gc) {
        double width = gc.getCanvas().getWidth();

        // Sun counter panel
        gc.setFill(Color.web("#795548"));
        gc.fillRoundRect(20, 20, 160, 50, 10, 10);
        gc.setStroke(Color.web("#5D4037"));
        gc.setLineWidth(2.0);
        gc.strokeRoundRect(20, 20, 160, 50, 10, 10);
        gc.setFill(Color.web("#FDD835"));
        gc.fillOval(30, 28, 34, 34);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        gc.fillText(String.valueOf(GameManager.getInstance().getSun()), 75, 53);

        // Map name, progress and survival time
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        gc.fillText(map.getName(), width / 2.0, 40);
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        gc.setFill(Color.web("#D1D5DB"));
        gc.fillText(progressText() + "   ·   " + formatTime(survivalTime), width / 2.0, 64);

        if (waveManager.isHugeWaveApproaching() && map.getMode() == MapMode.CAMPAIGN) {
            gc.setFill(Color.web("#F87171"));
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 16));
            gc.fillText("Đợt lớn!", width / 2.0, 86);
        }

        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        gc.setFill(Color.web("#D1D5DB"));
        gc.fillText(isPractice() ? "[P] Tạm dừng | [ESC] Về editor" : "[P] Tạm dừng | [ESC] Menu",
                width - 20, 40);
        if (isPractice()) {
            gc.setFill(Color.web("#FBBF24"));
            gc.fillText("Chơi thử – không lưu kỷ lục", width - 20, 62);
        }
    }

    private String progressText() {
        if (map.getMode() == MapMode.CAMPAIGN) {
            int total = waveManager.getTotalWaves();
            int current = Math.min(waveManager.getCurrentWaveIndex() + 1, total);
            return "Wave " + current + "/" + total;
        }
        return map.isEndless() ? "Vô hạn" : "Turn ?/" + map.getMaxTurns();
    }

    /** Formats seconds as m:ss. */
    public static String formatTime(double seconds) {
        int total = (int) seconds;
        return String.format("%d:%02d", total / 60, total % 60);
    }

    @Override
    public void handleMouseClick(MouseEvent event) {
        int col = gameBoard.getColAt(event.getX());
        int row = gameBoard.getRowAt(event.getY());

        if (col != -1 && row != -1) {
            selectedCol = col;
            selectedRow = row;
        } else {
            selectedCol = -1;
            selectedRow = -1;
        }
    }

    @Override
    public void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.P) {
            GameManager.getInstance().changeState(new PauseState(this));
        } else if (event.getCode() == KeyCode.ESCAPE) {
            GameManager.getInstance().changeState(isPractice() ? practiceReturn : new MenuState());
        }
    }

    @Override
    public void exit() {
        // Stop battle bgm or clean up temporary resources
    }

    /** True for an editor play-test. */
    public boolean isPractice() {
        return practiceReturn != null;
    }

    public GameBoard getGameBoard() {
        return gameBoard;
    }

    public GameMap getMap() {
        return map;
    }

    public WaveManager getWaveManager() {
        return waveManager;
    }

    /** Seconds played so far, not counting pauses. */
    public double getSurvivalTime() {
        return survivalTime;
    }
}
