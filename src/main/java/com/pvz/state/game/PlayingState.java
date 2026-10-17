package com.pvz.state.game;

import com.pvz.core.GameManager;
import com.pvz.model.board.GameBoard;
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
 */
public class PlayingState implements GameState {
    private final GameBoard gameBoard;
    private int selectedRow = -1;
    private int selectedCol = -1;

    public PlayingState() {
        this.gameBoard = new GameBoard();
    }

    @Override
    public void enter() {
        // Lifecycle only: the session is reset by GameManager.startNewGame().
        // PauseState resumes by re-entering this same instance, so resetting here
        // would silently wipe the player's sun on every pause/resume cycle.
    }

    @Override
    public void update(double deltaTime) {
        gameBoard.update(deltaTime);
        // Additional systems (Plants, Zombies, Projectiles) will be updated here
    }

    @Override
    public void render(GraphicsContext gc) {
        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();

        // Clear canvas with lawn-surround background
        gc.setFill(Color.web("#1E3A1E"));
        gc.fillRect(0, 0, width, height);

        // Render the 5x9 tile grid
        gameBoard.render(gc);

        // Highlight selected tile if any
        if (selectedRow >= 0 && selectedCol >= 0) {
            double ox = gameBoard.getCellOriginX(selectedCol);
            double oy = gameBoard.getCellOriginY(selectedRow);
            gc.setStroke(Color.web("#FFEB3B"));
            gc.setLineWidth(3.0);
            gc.strokeRect(ox, oy, GameBoard.CELL_WIDTH, GameBoard.CELL_HEIGHT);
        }

        // Render HUD (Sun count, Score, Controls)
        renderHUD(gc);
    }

    private void renderHUD(GraphicsContext gc) {
        // Sun counter panel
        gc.setFill(Color.web("#795548"));
        gc.fillRoundRect(20, 20, 160, 50, 10, 10);
        gc.setStroke(Color.web("#5D4037"));
        gc.setLineWidth(2.0);
        gc.strokeRoundRect(20, 20, 160, 50, 10, 10);

        // Sun icon (yellow circle)
        gc.setFill(Color.web("#FDD835"));
        gc.fillOval(30, 28, 34, 34);

        // Sun text
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        gc.fillText(String.valueOf(GameManager.getInstance().getSun()), 75, 53);

        // Controls info hint
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        gc.setFill(Color.web("#D1D5DB"));
        gc.fillText("Press [P] to Pause | [ESC] for Menu", gc.getCanvas().getWidth() - 20, 40);
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
            GameManager.getInstance().changeState(new MenuState());
        }
    }

    @Override
    public void exit() {
        // Stop battle bgm or clean up temporary resources
    }

    public GameBoard getGameBoard() {
        return gameBoard;
    }
}
