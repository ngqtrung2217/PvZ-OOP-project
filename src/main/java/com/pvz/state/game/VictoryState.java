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
 * State displayed when player defeats all waves and wins the level.
 */
public class VictoryState implements GameState {

    @Override
    public void enter() {
        // Play victory jingle
    }

    @Override
    public void update(double deltaTime) {}

    @Override
    public void render(GraphicsContext gc) {
        double width = gc.getCanvas().getWidth();
        double height = gc.getCanvas().getHeight();

        gc.setFill(Color.web("#064E3B"));
        gc.fillRect(0, 0, width, height);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#34D399"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 56));
        gc.fillText("VICTORY!", width / 2.0, height / 2.0 - 50);

        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 22));
        gc.setFill(Color.WHITE);
        gc.fillText("You successfully defended your lawn!", width / 2.0, height / 2.0 + 10);

        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 18));
        gc.setFill(Color.web("#A7F3D0"));
        gc.fillText("Press [ENTER] or [ESC] to Return to Menu", width / 2.0, height / 2.0 + 70);
    }

    @Override
    public void handleMouseClick(MouseEvent event) {
        GameManager.getInstance().changeState(new MenuState());
    }

    @Override
    public void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.ESCAPE) {
            GameManager.getInstance().changeState(new MenuState());
        }
    }

    @Override
    public void exit() {}
}
