package com.pvz.state.game;

import com.pvz.core.GameManager;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * State shown when zombies breach the lawn and enter the player's house.
 */
public class GameOverState implements GameState {

    @Override
    public void enter() {
        // Play game over scream / music
    }

    @Override
    public void update(double deltaTime) {}

    @Override
    public void render(GraphicsContext gc) {
        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();

        gc.setFill(Color.web("#0F172A"));
        gc.fillRect(0, 0, width, height);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#EF4444"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 54));
        gc.fillText("THE ZOMBIES ATE YOUR BRAINS!", width / 2.0, height / 2.0 - 50);

        gc.setFill(Color.web("#E2E8F0"));
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 22));
        gc.fillText("Press [ENTER] to Try Again", width / 2.0, height / 2.0 + 30);
        gc.fillText("Press [ESC] for Main Menu", width / 2.0, height / 2.0 + 70);
    }

    @Override
    public void handleMouseClick(MouseEvent event) {
        GameManager.getInstance().startNewGame(new PlayingState());
    }

    @Override
    public void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
            GameManager.getInstance().startNewGame(new PlayingState());
        } else if (event.getCode() == KeyCode.ESCAPE) {
            GameManager.getInstance().changeState(new MenuState());
        }
    }

    @Override
    public void exit() {}
}
